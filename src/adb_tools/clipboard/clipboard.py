"""Android clipboard operations via ADB.

Supports multiple backends:
1. `adb shell cmd clipboard` - Android 7.0+ (most devices)
2. `service call clipboard` - fallback for some devices
3. `dumpsys clipboard` - for devices with restricted cmd access

Note: Some devices (OnePlus/ColorOS, MIUI, etc.) block clipboard access via ADB
due to Android 10+ privacy restrictions. In such cases, you may need:
- An app like "Clipper" or "Tasker" running on device
- Or a companion app that exposes clipboard via a local HTTP server
"""

from __future__ import annotations

import re
import subprocess
import warnings
from dataclasses import dataclass
from typing import Optional

# Suppress runtime warning for module execution
warnings.filterwarnings("ignore", category=RuntimeWarning)


@dataclass
class ClipboardResult:
    """Result of a clipboard operation with status information."""
    success: bool
    text: Optional[str] = None
    error: Optional[str] = None
    method_used: Optional[str] = None


def _run_adb(command: list[str], timeout: int = 10) -> tuple[str, str, int]:
    """Run an ADB command and return stdout, stderr, and return code."""
    full_cmd = ["adb"] + command
    result = subprocess.run(
        full_cmd,
        capture_output=True,
        text=True,
        timeout=timeout,
    )
    return result.stdout, result.stderr, result.returncode


def _parse_service_call_output(output: str) -> Optional[str]:
    """Parse hex output from `service call clipboard` into text.

    The output contains UTF-16LE encoded strings in hex format like:
    Result: Parcel( 0x00000000: 00610074 00650073 ... 'a.t.e.s.t.')

    Returns decoded string or None if parsing fails.
    """
    hex_pattern = re.compile(r"0x[0-9a-f]{8}:\s*([0-9a-f]{8})\s*'([^']*)'")
    matches = hex_pattern.findall(output)

    if not matches:
        return None

    hex_bytes = []
    for hex_val, _ascii_part in matches:
        val = int(hex_val, 16)
        low_byte = val & 0xFF
        high_byte = (val >> 8) & 0xFF
        hex_bytes.append(low_byte)
        hex_bytes.append(high_byte)

    try:
        text = bytes(hex_bytes).decode("utf-16-le", errors="ignore")
        text = text.strip("\x00").strip()
        if text:
            return text
    except Exception:
        pass

    return None


def _check_access_denied(output: str) -> bool:
    """Check if clipboard access is denied (Android 10+ restriction)."""
    if "AccessAllowed" in output or "Unknown Source" in output:
        # Check for denied access pattern
        if "AccessAllowed" in output:
            # Look for "AccessAllowed.(UNKNOWN_SOURCE|SELF)" or similar
            denied_patterns = [
                r"AccessAllowed\.\s*\$UNKNOWN",
                r"AccessAllowed\.\s*\$N\.m\.c",
                r"\.N\.m\.c\.l\.i\.p",
            ]
            for pattern in denied_patterns:
                if re.search(pattern, output):
                    return True
    return False


def get_clipboard() -> ClipboardResult:
    """Get text content from Android clipboard.

    Returns:
        ClipboardResult with success status, text content, and method used.
        If success is False, check error field for details.
    """
    # Method 1: cmd clipboard (Android 7.0+)
    stdout, stderr, code = _run_adb(["shell", "cmd", "clipboard", "get", "text"])
    if code == 0 and stdout.strip() and "No shell command" not in stderr:
        return ClipboardResult(
            success=True,
            text=stdout.strip(),
            method_used="cmd clipboard"
        )

    # Method 2: dumpsys clipboard
    stdout, stderr, code = _run_adb(["shell", "dumpsys", "clipboard"])
    if code == 0 and stdout:
        # Check for access denial (OnePlus, MIUI, etc.)
        if _check_access_denied(stdout):
            return ClipboardResult(
                success=False,
                error="Clipboard access denied by device (Android 10+ privacy restriction). "
                      "Use a companion app like Clipper or Tasker.",
                method_used="dumpsys clipboard (access denied)"
            )

        # Look for text content
        text_match = re.search(r'text="([^"]*)"', stdout)
        if text_match:
            return ClipboardResult(
                success=True,
                text=text_match.group(1),
                method_used="dumpsys clipboard"
            )

        # Alternative patterns
        text_match = re.search(r'PrimaryClip.*?text=([^,\s]+)', stdout)
        if text_match:
            val = text_match.group(1).strip()
            if val and val != "null":
                return ClipboardResult(
                    success=True,
                    text=val,
                    method_used="dumpsys clipboard"
                )

    # Method 3: service call clipboard (for some devices)
    stdout, stderr, code = _run_adb(["shell", "service", "call", "clipboard", "5"])
    if code == 0 and stdout:
        if _check_access_denied(stdout):
            return ClipboardResult(
                success=False,
                error="Clipboard access denied by device (Android 10+ privacy restriction). "
                      "Use a companion app like Clipper or Tasker.",
                method_used="service call clipboard (access denied)"
            )

        result = _parse_service_call_output(stdout)
        if result:
            return ClipboardResult(
                success=True,
                text=result,
                method_used="service call clipboard"
            )

    return ClipboardResult(
        success=False,
        error="Clipboard is empty or contains non-text data",
        method_used=None
    )


def set_clipboard(text: str) -> ClipboardResult:
    """Set Android clipboard text.

    Args:
        text: The text to copy to clipboard.

    Returns:
        ClipboardResult with success status.
    """
    # Method 1: cmd clipboard set
    stdout, stderr, code = _run_adb(["shell", "cmd", "clipboard", "set", text])
    if code == 0 and "No shell command" not in stderr:
        return ClipboardResult(
            success=True,
            method_used="cmd clipboard"
        )

    return ClipboardResult(
        success=False,
        error="Failed to set clipboard. cmd clipboard not available.",
        method_used=None
    )


def clear_clipboard() -> ClipboardResult:
    """Clear Android clipboard.

    Returns:
        ClipboardResult with success status.
    """
    stdout, stderr, code = _run_adb(["shell", "cmd", "clipboard", "clear"])
    if code == 0:
        return ClipboardResult(
            success=True,
            method_used="cmd clipboard"
        )

    return ClipboardResult(
        success=False,
        error="Failed to clear clipboard",
        method_used=None
    )


# Legacy API - returns Optional[str] for backward compatibility
def get_clipboard_legacy() -> Optional[str]:
    """Legacy API: Get clipboard text (simple version).

    Returns:
        The clipboard text content, or None if empty/error.
    """
    result = get_clipboard()
    return result.text if result.success else None


def is_device_connected() -> bool:
    """Check if an Android device is connected via ADB.

    Returns:
        True if a device is connected, False otherwise.
    """
    stdout, stderr, code = _run_adb(["devices"])
    if code == 0:
        lines = stdout.strip().split("\n")
        for line in lines[1:]:
            if "device" in line and line.strip():
                return True
    return False


# Convenience function for command line usage
def main():
    """CLI entry point for clipboard operations."""
    import argparse
    import sys

    parser = argparse.ArgumentParser(description="Android Clipboard via ADB")
    parser.add_argument(
        "action",
        choices=["get", "set", "clear", "copy-to-pc"],
        help="Action to perform",
    )
    parser.add_argument(
        "text",
        nargs="?",
        help="Text to set (required for 'set' action)",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Output as JSON for scripting",
    )

    args = parser.parse_args()

    if args.action == "get":
        result = get_clipboard()
        if args.json:
            import json
            print(json.dumps({
                "success": result.success,
                "text": result.text,
                "error": result.error,
                "method": result.method_used
            }))
        elif result.success:
            print(result.text)
        else:
            print(f"Error: {result.error}", file=sys.stderr)
            print(f"Method attempted: {result.method_used}", file=sys.stderr)
            return 1

    elif args.action == "set":
        if not args.text:
            print("Error: text required for 'set' action", file=sys.stderr)
            return 1
        result = set_clipboard(args.text)
        if args.json:
            import json
            print(json.dumps({
                "success": result.success,
                "error": result.error,
                "method": result.method_used
            }))
        elif result.success:
            print("Clipboard set successfully")
        else:
            print(f"Failed to set clipboard: {result.error}", file=sys.stderr)
            return 1

    elif args.action == "clear":
        result = clear_clipboard()
        if args.json:
            import json
            print(json.dumps({
                "success": result.success,
                "error": result.error,
                "method": result.method_used
            }))
        elif result.success:
            print("Clipboard cleared successfully")
        else:
            print(f"Failed to clear clipboard: {result.error}", file=sys.stderr)
            return 1

    elif args.action == "copy-to-pc":
        # Use provider-based get_clipboard from __init__.py
        from . import get_clipboard as provider_get_clipboard
        result = provider_get_clipboard()
        if not result.success or not result.text:
            print(f"Error: {result.error or 'clipboard is empty'}", file=sys.stderr)
            return 1

        import platform
        import subprocess
        system = platform.system()
        try:
            if system == "Windows":
                # Use PowerShell to avoid encoding issues
                import tempfile
                import os
                # Write to temp file with BOM for proper encoding
                with tempfile.NamedTemporaryFile(mode='w', encoding='utf-16-le', suffix='.txt', delete=False) as f:
                    f.write(result.text)
                    temp_path = f.name
                try:
                    subprocess.run(f"cmd /c type \"{temp_path}\" | clip", shell=True, check=True)
                finally:
                    os.unlink(temp_path)
            elif system == "Darwin":
                subprocess.run("pbcopy", input=result.text.encode("utf-8"), check=True)
            else:
                subprocess.run("xclip", "-selection", "clipboard", input=result.text.encode("utf-8"), check=True)
            print(f"Copied to PC clipboard ({len(result.text)} chars)")
        except FileNotFoundError:
            print(f"Error: clipboard tool not found", file=sys.stderr)
            return 1
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())