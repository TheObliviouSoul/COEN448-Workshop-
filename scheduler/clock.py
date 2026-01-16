from __future__ import annotations

from dataclasses import dataclass
from threading import Lock
from typing import Protocol


class Clock(Protocol):
    def now_ms(self) -> int:
        """Return current time in epoch milliseconds."""


@dataclass
class ManualClock:
    _now_ms: int = 0

    def __post_init__(self) -> None:
        self._lock = Lock()

    def now_ms(self) -> int:
        with self._lock:
            return self._now_ms

    def advance_ms(self, delta_ms: int) -> None:
        if delta_ms < 0:
            raise ValueError("delta_ms must be non-negative")
        with self._lock:
            self._now_ms += delta_ms

    def set_ms(self, now_ms: int) -> None:
        if now_ms < 0:
            raise ValueError("now_ms must be non-negative")
        with self._lock:
            self._now_ms = now_ms
