"""Tasker clipboard provider.

Supports Tasker app with HTTP server plugin for clipboard access.

Tasker can be configured to:
1. Run a local HTTP server on a specific port
2. Respond to clipboard get/set requests via HTTP

This provider requires Tasker to be configured with:
- HTTP Server profile
- Profile: Clipboard Get - returns CLIP text
- Profile: Clipboard Set - sets CLIP to received text
"""

from __future__ import annotations

import subprocess
from typing import Optional

from .base import ClipboardProvider

TASKER_PACKAGES = [
    "net.dinglisch.android.taskerm",
    "net.dinglisch.android.tasker",
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


class TaskerProvider(ClipboardProvider):
    """Provider using Tasker app with HTTP server."""

    def __init__(self):
        self._package: Optional[str] = None

    @property
    def name(self) -> str:
        return "tasker"

    def is_available(self) -> bool:
        """Check if Tasker is installed."""
        for package in TASKER_PACKAGES:
            if _check_package_installed(package):
                self._package = package
                return True
        return False

    def _make_request(self, action: str, text: Optional[str] = None) -> Optional[str]:
        """Make HTTP request to Tasker HTTP server.

        Default port is 8888, but this can be configured in Tasker.
        """
        # Default Tasker HTTP server port
        port = 8888

        if action == "get":
            # Request clipboard content
            result = subprocess.run(
                ["curl", "-s", f"http://localhost:{port}/clipboard"],
                capture_output=True,
                text=True,
                timeout=5,
            )
            if result.returncode == 0:
                return result.stdout.strip()
        elif action == "set" and text:
            # Set clipboard content
            result = subprocess.run(
                ["curl", "-s", "-X", "POST", "-d", f"clipboard={text}", f"http://localhost:{port}/clipboard"],
                capture_output=True,
                text=True,
                timeout=5,
            )
            return str(result.returncode == 0)

        return None

    def get_clipboard(self) -> Optional[str]:
        """Get clipboard via Tasker HTTP server.

        Requires Tasker to be configured with an HTTP server profile.
        """
        # First, try port forwarding
        subprocess.run(
            ["adb", "forward", "tcp:8888", "tcp:8888"],
            capture_output=True,
            timeout=5,
        )

        return self._make_request("get")

    def set_clipboard(self, text: str) -> bool:
        """Set clipboard via Tasker HTTP server."""
        result = self._make_request("set", text)
        return result == "True"


# Configuration for Tasker HTTP server setup
TASKER_SETUP_INSTRUCTIONS = """
Tasker HTTP Server Setup for Clipboard Access
==============================================

1. Install Tasker from Play Store

2. Create a new Profile:
   - Name: "Clipboard Server"
   - Trigger: Event -> Misc -> HTTP Request (or use HTTP Server plugin)

3. For HTTP Server Plugin:
   - Install "HTTP Server Tasks" plugin for Tasker
   - Configure a server on port 8888

4. Create a Task for GET /clipboard:
   - Code -> Run Shell: echo %CLIP
   - Return the result as HTTP response

5. Create a Task for POST /clipboard:
   - Receive the text
   - Code -> Variable Set: %CLIP to received text

Note: This provider requires manual Tasker configuration.
"""


def get_tasker_instructions() -> str:
    """Get Tasker setup instructions."""
    return TASKER_SETUP_INSTRUCTIONS