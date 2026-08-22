"""Clipboard providers - re-exports from providers package for convenience."""

from .providers import (
    ClipboardProvider,
    ProviderResult,
    get_clipboard_via_provider,
    list_providers,
    ALL_PROVIDERS,
)
from .providers.base import ProviderResult
from .providers.builtin import BuiltinProvider
from .providers.clipper import ClipperProvider
from .providers.tasker import TaskerProvider

__all__ = [
    "ClipboardProvider",
    "ProviderResult",
    "get_clipboard_via_provider",
    "list_providers",
    "ALL_PROVIDERS",
    "BuiltinProvider",
    "ClipperProvider",
    "TaskerProvider",
]