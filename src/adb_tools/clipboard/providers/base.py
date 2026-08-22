"""Base class for clipboard providers."""

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Optional


@dataclass
class ProviderResult:
    """Result from a clipboard provider."""
    success: bool
    text: Optional[str] = None
    error: Optional[str] = None
    provider_name: Optional[str] = None


class ClipboardProvider(ABC):
    """Base class for clipboard providers."""

    @property
    @abstractmethod
    def name(self) -> str:
        """Provider name."""
        pass

    @abstractmethod
    def get_clipboard(self) -> Optional[str]:
        """Get clipboard text from this provider."""
        pass

    @abstractmethod
    def set_clipboard(self, text: str) -> bool:
        """Set clipboard text via this provider."""
        pass

    def clear_clipboard(self) -> bool:
        """Clear clipboard text via this provider.

        Default implementation sets empty text.
        Override if provider has a dedicated clear method.
        """
        return self.set_clipboard("")

    def is_available(self) -> bool:
        """Check if this provider is available (app installed)."""
        return True