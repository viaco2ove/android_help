"""Android Clipper app provider."""

from __future__ import annotations

import base64
import subprocess
from typing import Optional

from .base import ClipboardProvider

PACKAGE_NAME = "com.clipper.android_clipper"


def _check_package_installed(package: str) -> bool:
    result = subprocess.run(
        ["adb", "shell", "pm", "list", "packages", package],
        capture_output=True,
    )
    try:
        stdout = result.stdout.decode("utf-8", errors="replace")
    except Exception:
        stdout = str(result.stdout)
    return package in stdout


def _run_adb(command: list) -> tuple:
    result = subprocess.run(command, capture_output=True)
    try:
        stdout = result.stdout.decode("utf-8", errors="replace")
    except Exception:
        stdout = str(result.stdout)
    return stdout, result.returncode


def _read_clipboard_via_content_provider() -> Optional[str]:
    """Read clipboard via content provider (most reliable)."""
    cmd = ["adb", "shell", "content", "query",
           "--uri", f"content://{PACKAGE_NAME}.provider/clipboard",
           "--projection", "text"]
    stdout, code = _run_adb(cmd)
    if code == 0 and stdout.strip():
        # Parse: text=内容 (content might contain spaces, commas, etc.)
        import re
        # Match text= followed by everything until the next column (timestamp=)
        match = re.search(r'text=(.+?)(?:,\s*timestamp=|$)', stdout, re.DOTALL)
        if match:
            text = match.group(1).strip()
            if text:
                return text
    return None


def _read_clipboard_base64() -> Optional[str]:
    """Fallback: read via run-as with hex encoding."""
    cmd = f"run-as {PACKAGE_NAME} cat files/clipboard_data.txt"
    stdout, code = _run_adb(["adb", "shell", cmd])
    if code == 0:
        return stdout if stdout else None
    return None


def _write_clipboard_base64(text: str) -> bool:
    """Write via content provider."""
    cmd = ["adb", "shell", "content", "insert",
           "--uri", f"content://{PACKAGE_NAME}.provider/clipboard",
           "--bind", f"text:s:{text}"]
    _, code = _run_adb(cmd)
    return code == 0


def _launch_app() -> None:
    _run_adb(["adb", "shell", "am", "start", "-n", f"{PACKAGE_NAME}/.MainActivity"])
    _run_adb(["adb", "shell", "am", "startservice", "-n", f"{PACKAGE_NAME}/.ClipboardService"])


class AndroidClipperProvider(ClipboardProvider):

    @property
    def name(self) -> str:
        return "android_clipper"

    def is_available(self) -> bool:
        return _check_package_installed(PACKAGE_NAME)

    def get_clipboard(self) -> Optional[str]:
        if not self.is_available():
            return None
        # Ensure service is running first
        _run_adb(["adb", "shell", "am", "startservice", "-n",
                  f"{PACKAGE_NAME}/.ClipboardService"])
        # Try content provider first (most reliable)
        result = _read_clipboard_via_content_provider()
        if result is not None:
            return result
        # Fallback to file-based
        return _read_clipboard_base64()

    def set_clipboard(self, text: str) -> bool:
        if not self.is_available():
            return False
        return _write_clipboard_base64(text)


def is_android_clipper_available() -> bool:
    return _check_package_installed(PACKAGE_NAME)


def get_clipboard_via_clipper() -> Optional[str]:
    provider = AndroidClipperProvider()
    if provider.is_available():
        return provider.get_clipboard()
    return None


def install_clipper() -> bool:
    import os
    apk_paths = [
        "android/android_clipper/app/build/outputs/apk/debug/app-debug.apk",
        "../android/android_clipper/app/build/outputs/apk/debug/app-debug.apk",
    ]
    for apk_path in apk_paths:
        if os.path.exists(apk_path):
            result = subprocess.run(["adb", "install", "-r", apk_path], capture_output=True)
            return result.returncode == 0
    return False


def launch_clipper() -> bool:
    try:
        _launch_app()
        return True
    except Exception:
        return False