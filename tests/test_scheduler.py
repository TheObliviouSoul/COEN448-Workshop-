import threading
from concurrent.futures import ThreadPoolExecutor

import pytest

from scheduler.clock import ManualClock
from scheduler.task_scheduler import TaskScheduler, TaskState


def test_delayed_execution_runs_only_after_time_reaches():
    clock = ManualClock(1000)
    scheduler = TaskScheduler(clock, max_workers=2)
    ran = threading.Event()

    scheduler.schedule_in("t1", 10, lambda: ran.set())
    assert scheduler.dispatch_due() == 0
    assert not ran.is_set()

    clock.advance_ms(9)
    assert scheduler.dispatch_due() == 0
    assert not ran.is_set()

    clock.advance_ms(1)
    assert scheduler.dispatch_due() == 1
    ran.wait(timeout=1)
    assert ran.is_set()
    assert scheduler.get_task("t1").state == TaskState.COMPLETED
    scheduler.shutdown()


@pytest.mark.parametrize("run_at_ms", [-1, 999])
def test_invalid_schedule_times_raise(run_at_ms):
    clock = ManualClock(1000)
    scheduler = TaskScheduler(clock)
    with pytest.raises(ValueError):
        scheduler.schedule_at("t1", run_at_ms, lambda: None)
    scheduler.shutdown()


@pytest.mark.parametrize("delay_ms", [-1])
def test_negative_delay_raises(delay_ms):
    clock = ManualClock(1000)
    scheduler = TaskScheduler(clock)
    with pytest.raises(ValueError):
        scheduler.schedule_in("t1", delay_ms, lambda: None)
    scheduler.shutdown()


def test_duplicate_task_id_raises():
    clock = ManualClock(1000)
    scheduler = TaskScheduler(clock)
    scheduler.schedule_in("dup", 0, lambda: None)
    with pytest.raises(ValueError):
        scheduler.schedule_in("dup", 0, lambda: None)
    scheduler.shutdown()


def test_cancellation_prevents_run():
    clock = ManualClock(1000)
    scheduler = TaskScheduler(clock)
    ran = threading.Event()

    scheduler.schedule_in("t1", 0, lambda: ran.set())
    assert scheduler.cancel("t1") is True
    assert scheduler.dispatch_due() == 0
    assert not ran.is_set()
    assert scheduler.get_task("t1").state == TaskState.CANCELLED
    scheduler.shutdown()


def test_cancel_after_dispatch_does_not_crash():
    clock = ManualClock(1000)
    scheduler = TaskScheduler(clock, max_workers=1)
    started = threading.Event()
    unblock = threading.Event()

    def task():
        started.set()
        unblock.wait(timeout=1)

    scheduler.schedule_in("t1", 0, task)
    assert scheduler.dispatch_due() == 1
    started.wait(timeout=1)
    scheduler.cancel("t1")
    unblock.set()
    scheduler.shutdown()
    state = scheduler.get_task("t1").state
    assert state in (TaskState.CANCELLED, TaskState.COMPLETED, TaskState.RUNNING)


def test_concurrent_dispatch_executes_tasks_once():
    clock = ManualClock(0)
    scheduler = TaskScheduler(clock, max_workers=4)
    executed = []
    lock = threading.Lock()

    for i in range(50):
        scheduler.schedule_at(f"t{i}", 0, lambda i=i: (lock.acquire(), executed.append(i), lock.release()))

    with ThreadPoolExecutor(max_workers=3) as executor:
        futures = [executor.submit(scheduler.dispatch_due) for _ in range(3)]
        for f in futures:
            f.result(timeout=1)

    scheduler.shutdown()
    assert sorted(executed) == list(range(50))


def test_race_schedule_cancel_dispatch_no_duplicates():
    clock = ManualClock(0)
    scheduler = TaskScheduler(clock, max_workers=4)
    executed = set()
    executed_lock = threading.Lock()
    start = threading.Event()

    def record(task_id: str):
        def _run():
            with executed_lock:
                if task_id in executed:
                    pytest.fail(f"duplicate execution: {task_id}")
                executed.add(task_id)
        return _run

    def scheduler_thread():
        start.wait(timeout=1)
        for i in range(200):
            task_id = f"s{i}"
            scheduler.schedule_in(task_id, 0, record(task_id))

    def cancel_thread():
        start.wait(timeout=1)
        for i in range(200):
            scheduler.cancel(f"s{i}")

    def dispatch_thread():
        start.wait(timeout=1)
        for _ in range(50):
            scheduler.dispatch_due()

    threads = [
        threading.Thread(target=scheduler_thread),
        threading.Thread(target=cancel_thread),
        threading.Thread(target=dispatch_thread),
        threading.Thread(target=dispatch_thread),
    ]
    for t in threads:
        t.start()
    start.set()
    for t in threads:
        t.join(timeout=2)

    scheduler.shutdown()
