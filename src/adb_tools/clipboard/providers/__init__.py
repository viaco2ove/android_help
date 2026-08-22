"""Clipboard providers package."""

from .base import ClipboardProvider, ProviderResult
from .builtin import BuiltinProvider
from .android_clipper import AndroidClipperProvider, install_clipper, launch_clipper, is_android_clipper_available
from .clipper import ClipperProvider
from .tasker import TaskerProvider

# All available providers (in order of preference)
ALL_PROVIDERS: list[ClipboardProvider] = [
    AndroidClipperProvider(),  # Custom app with Content Provider
    BuiltinProvider(),         # Native ADB commands
    ClipperProvider(),         # Clipper app
    TaskerProvider(),          # Tasker app
]


def get_clipboard_via_provider(provider: ClipboardProvider = None) -> ProviderResult:
    """Get clipboard using a specific provider or auto-detect."""
    if provider:
        try:
            text = provider.get_clipboard()
            return ProviderResult(
                success=text is not None,
                text=text,
                provider_name=provider.name,
                error=None if text else f"{provider.name} returned empty"
            )
        except Exception as e:
            return ProviderResult(
                success=False,
                error=str(e),
                provider_name=provider.name
            )

    # Auto-detect: try each available provider
    for p in ALL_PROVIDERS:
        if not p.is_available():
            continue
        try:
            text = p.get_clipboard()
            if text is not None:
                return ProviderResult(
                    success=True,
                    text=text,
                    provider_name=p.name
                )
        except Exception:
            continue

    return ProviderResult(
        success=False,
        error="No provider available or clipboard is empty"
    )


def list_providers() -> list[str]:
    """List all available providers."""
    return [p.name for p in ALL_PROVIDERS if p.is_available()]


__all__ = [
    "ClipboardProvider",
    "ProviderResult",
    "get_clipboard_via_provider",
    "list_providers",
    "ALL_PROVIDERS",
    "BuiltinProvider",
    "AndroidClipperProvider",
    "ClipperProvider",
    "TaskerProvider",
    "install_clipper",
    "launch_clipper",
    "is_android_clipper_available",
]