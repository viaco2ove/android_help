"""Built-in ADB clipboard provider.

Uses native Android commands like:
- cmd clipboard (Android 7.0+)
- service call clipboard
- dumpsys clipboard
"""

from __future__ import annotations

import re
import subprocess
from typing import Optional

from .base import ClipboardProvider


def _run_adb(command: list[str], timeout: int = 10) -> tuple[str, str, int]:
    """Run an ADB command and return stdout, stderr, and return code."""
    full_cmd = ["adb"] + command
    result = subprocess.run(
        full_cmd,
        capture_output=True,
        text=True,
        timeout=timeout,
        encoding="utf-8",
        errors="replace",
    )
    return result.stdout, result.stderr, result.returncode


def _parse_service_call_output(output: str) -> Optional[str]:
    """Parse hex output from `service call clipboard` into text."""
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


class BuiltinProvider(ClipboardProvider):
    """Provider using built-in ADB/Android clipboard commands."""

    @property
    def name(self) -> str:
        return "adb_builtin"

    def get_clipboard(self) -> Optional[str]:
        """Get clipboard using native Android commands."""
        # Method 1: cmd clipboard
        stdout, stderr, code = _run_adb(["shell", "cmd", "clipboard", "get", "text"])
        if code == 0 and stdout.strip() and "No shell command" not in stderr:
            return stdout.strip()

        # Method 2: service call clipboard
        stdout, stderr, code = _run_adb(["shell", "service", "call", "clipboard", "5"])
        if code == 0 and stdout:
            # Check for NonNull to confirm clipboard has content
            if "NonNull" in stdout:
                return _parse_service_call_output(stdout)
            # If it contains Null, clipboard is empty

        return None

    def set_clipboard(self, text: str) -> bool:
        """Set clipboard using native Android commands."""
        stdout, stderr, code = _run_adb(["shell", "cmd", "clipboard", "set", text])
        return code == 0 and "No shell command" not in stderr