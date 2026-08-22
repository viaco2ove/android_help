"""Clipper clipboard provider.

Supports:
- Clipper - Clipboard Manager (com.catchingnow.tinyclipboardmanager)
- Tiny Clipboard Manager (same app, older name)

Uses the app's content provider to read/write clipboard history.
"""

from __future__ import annotations

import subprocess
from typing import Optional

from .base import ClipboardProvider

# Package names for Clipper
CLIPPER_PACKAGES = [
    "com.catchingnow.tinyclipboardmanager",
    "com.catchingnow.clipboardmanager",
]


def _check_package_installed(package: str) -> bool:
    """Check if a package is installed."""
    result = subprocess.run(
        ["adb", "shell", "pm", "list", "packages", package],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return package in result.stdout


class ClipperProvider(ClipboardProvider):
    """Provider using Clipper - Clipboard Manager app."""

    @property
    def name(self) -> str:
        return "clipper"

    def is_available(self) -> bool:
        """Check if Clipper is installed."""
        for package in CLIPPER_PACKAGES:
            if _check_package_installed(package):
                self._package = package
                return True
        return False

    def __init__(self):
        self._package: Optional[str] = None

    def get_clipboard(self) -> Optional[str]:
        """Get current clipboard from Clipper's content provider.

        Clipper exposes clipboard history via content provider:
        content://com.catchingnow.tinyclipboardmanager.provider/clipboard
        """
        if not self._package:
            if not self.is_available():
                return None

        # Try reading via content provider
        result = subprocess.run(
            [
                "adb", "shell", "content", "read",
                "--uri", "content://com.catchingnow.tinyclipboardmanager.provider/clipboard"
            ],
            capture_output=True,
            text=True,
        )

        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip()

        return None

    def set_clipboard(self, text: str) -> bool:
        """Set clipboard via Clipper (not supported)."""
        # Clipper doesn't have a content provider for writing
        return False


# Standalone functions for direct usage
def is_clipper_available() -> bool:
    """Check if Clipper app is installed."""
    provider = ClipperProvider()
    return provider.is_available()


def get_clipper_clipboard() -> Optional[str]:
    """Get clipboard from Clipper app."""
    provider = ClipperProvider()
    if provider.is_available():
        return provider.get_clipboard()
    return None