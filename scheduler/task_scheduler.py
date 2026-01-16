from __future__ import annotations

import heapq
import itertools
from concurrent.futures import Future, ThreadPoolExecutor
from dataclasses import dataclass, field
from threading import Condition, Lock
from typing import Callable, Dict, List, Optional

from .clock import Clock


class TaskState:
    PENDING = "PENDING"
    CANCELLED = "CANCELLED"
    DISPATCHED = "DISPATCHED"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


@dataclass
class ScheduledTask:
    task_id: str
    run_at_ms: int
    func: Callable[[], None]
    state: str = TaskState.PENDING
    future: Optional[Future] = None
    cancel_requested: bool = False
    _lock: Lock = field(default_factory=Lock, repr=False)

    def mark_state(self, new_state: str) -> None:
        with self._lock:
            self.state = new_state


class TaskScheduler:
    def __init__(self, clock: Clock, max_workers: int = 4) -> None:
        self._clock = clock
        self._executor = ThreadPoolExecutor(max_workers=max_workers)
        self._lock = Lock()
        self._condition = Condition(self._lock)
        self._sequence = itertools.count()
        self._queue: List[tuple[int, int, str]] = []
        self._tasks: Dict[str, ScheduledTask] = {}
        self._shutdown = False

    def schedule_at(self, task_id: str, run_at_ms: int, func: Callable[[], None]) -> ScheduledTask:
        if func is None:
            raise ValueError("func must not be None")
        if run_at_ms < 0:
            raise ValueError("run_at_ms must be non-negative")
        now_ms = self._clock.now_ms()
        if run_at_ms < now_ms:
            raise ValueError("run_at_ms must not be in the past")
        with self._condition:
            if task_id in self._tasks:
                raise ValueError("task_id already exists")
            task = ScheduledTask(task_id=task_id, run_at_ms=run_at_ms, func=func)
            self._tasks[task_id] = task
            heapq.heappush(self._queue, (run_at_ms, next(self._sequence), task_id))
            self._condition.notify_all()
            return task

    def schedule_in(self, task_id: str, delay_ms: int, func: Callable[[], None]) -> ScheduledTask:
        if delay_ms < 0:
            raise ValueError("delay_ms must be non-negative")
        run_at_ms = self._clock.now_ms() + delay_ms
        return self.schedule_at(task_id, run_at_ms, func)

    def cancel(self, task_id: str) -> bool:
        with self._condition:
            task = self._tasks.get(task_id)
            if task is None:
                return False
            if task.state == TaskState.PENDING:
                task.mark_state(TaskState.CANCELLED)
                return True
            task.cancel_requested = True
            return False

    def dispatch_due(self) -> int:
        now_ms = self._clock.now_ms()
        due_task_ids: List[str] = []
        with self._condition:
            if self._shutdown:
                return 0
            while self._queue and self._queue[0][0] <= now_ms:
                _, _, task_id = heapq.heappop(self._queue)
                task = self._tasks.get(task_id)
                if task is None or task.state != TaskState.PENDING:
                    continue
                task.mark_state(TaskState.DISPATCHED)
                due_task_ids.append(task_id)

        for task_id in due_task_ids:
            task = self._tasks.get(task_id)
            if task is None:
                continue
            future = self._executor.submit(self._run_task, task)
            task.future = future
        return len(due_task_ids)

    def _run_task(self, task: ScheduledTask) -> None:
        task.mark_state(TaskState.RUNNING)
        if task.cancel_requested:
            task.mark_state(TaskState.CANCELLED)
            return
        try:
            task.func()
        except Exception:
            task.mark_state(TaskState.FAILED)
            raise
        else:
            task.mark_state(TaskState.COMPLETED)

    def get_task(self, task_id: str) -> Optional[ScheduledTask]:
        with self._lock:
            return self._tasks.get(task_id)

    def shutdown(self, wait: bool = True) -> None:
        with self._condition:
            self._shutdown = True
        self._executor.shutdown(wait=wait)
