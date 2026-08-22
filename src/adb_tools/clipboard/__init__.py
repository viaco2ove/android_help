"""Android clipboard operations via ADB.

Supports multiple providers:
- AndroidClipper: Custom app with Content Provider (recommended)
- Builtin: Native Android clipboard commands
- Clipper: Clipper - Clipboard Manager app
- Tasker: Tasker with HTTP server plugin

Usage:
    from adb_tools.clipboard import get_clipboard

    result = get_clipboard()
    if result.success:
        print(result.text)

    # Or use specific provider
    from adb_tools.clipboard import AndroidClipperProvider, install_clipper

    if not AndroidClipperProvider().is_available():
        install_clipper()
"""

from .clipboard import (
    ClipboardResult,
    get_clipboard as get_clipboard_native,
    set_clipboard as set_clipboard_native,
    clear_clipboard as clear_clipboard_native,
    is_device_connected,
)

# Provider-based API
from .providers import (
    ClipboardProvider,
    ProviderResult,
    get_clipboard_via_provider,
    list_providers,
    ALL_PROVIDERS,
)

# Individual providers
from .providers.builtin import BuiltinProvider
from .providers.android_clipper import (
    AndroidClipperProvider,
    is_android_clipper_available,
    install_clipper,
    launch_clipper,
)
from .providers.clipper import ClipperProvider, is_clipper_available
from .providers.tasker import TaskerProvider, get_tasker_instructions

# For backward compatibility
def get_clipboard() -> ProviderResult:
    """Get clipboard text (tries all providers).

    Returns:
        ProviderResult with success status, text content, and provider name.
    """
    return get_clipboard_via_provider()


def set_clipboard(text: str) -> bool:
    """Set clipboard text (tries all providers)."""
    # Try provider-based set first
    for provider in ALL_PROVIDERS:
        if not provider.is_available():
            continue
        try:
            if provider.set_clipboard(text):
                return True
        except Exception:
            continue

    # Fallback to native implementation
    result = set_clipboard_native(text)
    return result.success


def clear_clipboard() -> bool:
    """Clear clipboard (tries all providers)."""
    # Try provider-based clear first
    for provider in ALL_PROVIDERS:
        if not provider.is_available():
            continue
        try:
            if provider.clear_clipboard():
                return True
        except Exception:
            continue

    # Fallback to native implementation
    result = clear_clipboard_native()
    return result.success


def copy_to_pc() -> bool:
    """Copy Android clipboard to PC clipboard."""
    import subprocess
    import sys
    import platform

    result = get_clipboard()
    if not result.success or not result.text:
        print(f"Failed to get clipboard", file=sys.stderr)
        return False

    system = platform.system()
    try:
        if system == "Windows":
            # Use PowerShell Set-Clipboard for proper UTF-8 support
            ps_script = f'Set-Clipboard -Value "{result.text.replace("\"", "`\"")}"'
            subprocess.run(["powershell", "-Command", ps_script], check=True)
        elif system == "Darwin":
            subprocess.run(["pbcopy"], input=result.text.encode("utf-8"), check=True)
        else:
            subprocess.run(["xclip", "-selection", "clipboard"], input=result.text.encode("utf-8"), check=True)
        return True
    except FileNotFoundError:
        print("Error: clipboard tool not found", file=sys.stderr)
        return False
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return False


def paste_from_pc(text: str) -> bool:
    """Set Android clipboard from PC text (actually just sets clipboard directly).

    Args:
        text: Text to set on Android clipboard.

    Returns:
        True if successful, False otherwise.
    """
    return set_clipboard(text)


__all__ = [
    # Main functions
    "get_clipboard",
    "set_clipboard",
    "clear_clipboard",
    "copy_to_pc",
    "paste_from_pc",
    # Results
    "ClipboardResult",
    "ProviderResult",
    # Providers
    "ClipboardProvider",
    "BuiltinProvider",
    "AndroidClipperProvider",
    "ClipperProvider",
    "TaskerProvider",
    # Utilities
    "get_clipboard_via_provider",
    "list_providers",
    "is_device_connected",
    "is_android_clipper_available",
    "is_clipper_available",
    "install_clipper",
    "launch_clipper",
    "get_tasker_instructions",
    # Legacy
    "get_clipboard_native",
    "set_clipboard_native",
    "clear_clipboard_native",
]