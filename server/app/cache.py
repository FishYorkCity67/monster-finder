import time
from typing import Any


class TTLCache:
    """Kleiner In-Memory-Cache, damit der Pi die APIs nicht bei jedem Tippen erneut fragt."""

    def __init__(self, ttl: int):
        self.ttl = ttl
        self._data: dict[str, tuple[float, Any]] = {}

    def get(self, key: str) -> Any | None:
        hit = self._data.get(key)
        if hit and time.monotonic() - hit[0] < self.ttl:
            return hit[1]
        self._data.pop(key, None)
        return None

    def set(self, key: str, value: Any) -> None:
        if len(self._data) > 500:
            self._data.clear()
        self._data[key] = (time.monotonic(), value)
