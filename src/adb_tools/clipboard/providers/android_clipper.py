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


def _read_clipboard_base64() -> Optional[str]:
    cmd = f"run-as {PACKAGE_NAME} cat files/clipboard_data.txt | base64"
    stdout, code = _run_adb(["adb", "shell", cmd])
    if code == 0 and stdout.strip():
        try:
            data = base64.b64decode(stdout.strip())
            return data.decode("utf-8", errors="replace")
        except Exception:
            return None
    return None


def _write_clipboard_base64(text: str) -> bool:
    encoded = base64.b64encode(text.encode("utf-8")).decode("ascii")
    cmd = f"run-as {PACKAGE_NAME} sh -c 'echo {encoded} | base64 -d > files/clipboard_data.txt'"
    _, code = _run_adb(["adb", "shell", cmd])
    return code == 0


def _launch_app() -> None:
    _run_adb(["adb", "shell", "am", "start", "-n", f"{PACKAGE_NAME}/.MainActivity"])
    _run_adb(["adb", "shell", "am", "startservice", "-n", f"{PACKAGE_NAME}/.FloatingService"])


class AndroidClipperProvider(ClipboardProvider):

    @property
    def name(self) -> str:
        return "android_clipper"

    def is_available(self) -> bool:
        return _check_package_installed(PACKAGE_NAME)

    def get_clipboard(self) -> Optional[str]:
        if not self.is_available():
            return None
        _launch_app()
        return _read_clipboard_base64()

    def set_clipboard(self, text: str) -> bool:
        if not self.is_available():
            return False
        _launch_app()
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