# DECISIONS.md

## 1. Atomic job claiming across processes

The exact line is the single `UPDATE` statement in `claimNextJob()`
(`WorkerRepository.java`):

```java
String sql = "UPDATE jobs SET state='processing', workerId=?, updatedAt=?, lastHeartbeat=? " +
        "WHERE id = (SELECT id FROM jobs WHERE state='pending' " +
        "OR (state='failed' AND nextRetry<=?) " +
        "OR (state='processing' AND lastHeartbeat<=?) " +
        "ORDER BY createdAt LIMIT 1) " +
        "AND (state='pending' OR (state='failed' AND nextRetry<=?) OR (state='processing' AND lastHeartbeat<=?))";
```

This is atomic across separate OS processes for two reasons:

- **It's one statement, not read-then-write.** The candidate row is picked
  and flipped to `processing` inside a single `UPDATE`. There's no gap
  between "check state" and "set state" where another process could
  interleave — SQLite executes the whole statement as one implicit
  transaction.
- **SQLite serializes writers at the file level.** Even with multiple
  `queuectl worker start` processes hitting the same `queuectl.db`, SQLite
  only lets one writer hold the database lock at a time. A second process's
  `UPDATE` blocks until the first commits, and by the time it runs, the row
  it was targeting either no longer matches `state='pending'` (0 rows
  affected -> `null` returned) or a different row is chosen. `executeUpdate()`
  returning `1` is the only signal a job was actually claimed by *this*
  worker — that check (`code == 1`) is what prevents two workers from
  both thinking they got the job.

**Trade-off:** this relies on default SQLite locking, not WAL mode. Fine for
this assignment's concurrency levels; under heavier write contention I'd add
`PRAGMA busy_timeout` so competing workers retry instead of throwing
`SQLITE_BUSY`, rather than switch storage engines.

## 2. Worker SIGKILLed mid-job — recovery walk-through

Step by step:

1. Worker A claims job X: `claimNextJob()` sets `state='processing'`,
   `workerId=A`, `lastHeartbeat=now`.
2. `SIGKILL` hits worker A mid-execution. No shutdown hook runs (SIGKILL
   can't be caught), so job X is left sitting in `processing` with a
   `lastHeartbeat` that stops advancing.
3. Every other worker's poll loop keeps calling `claimNextJob()` every
   500ms (existing loop, unchanged). The claim query's `WHERE` clause
   treats `state='processing' AND lastHeartbeat<=staleCutoff` (cutoff =
   `now - 30s`) as claimable, exactly like a pending job.
4. Once job X's `lastHeartbeat` is more than 30 seconds old, the next
   worker that polls picks it up in the same atomic `UPDATE`, overwrites
   `workerId` to itself, and refreshes `lastHeartbeat`. `attempts` is left
   untouched — the crash didn't count as a failed *execution*, so it
   doesn't consume a retry.
5. Job X now runs to completion (or fails/backs off) normally.

**Worst-case delay before recovery:** ~30–30.5s (30s staleness threshold +
up to one 500ms poll interval), comfortably under the 60s requirement with
margin for timing jitter.

**Trade-off (the honest one):** the heartbeat is set once, at claim time,
not refreshed while the job runs. This is deliberately simple — no
per-job heartbeat thread — but it means a job that legitimately runs
*longer* than 30 seconds would get falsely reclaimed and executed twice by
another worker, breaking the "exactly once" guarantee for long-running
commands. I accepted this because the assignment's jobs are short shell
commands; a production version would refresh `lastHeartbeat` periodically
from inside `executeJob()` while the process is running, or size the
threshold to the expected job duration.

**Known gap I'm not solving here:** the crashed worker's row in the
`workers` table still shows `stateWorker='running'` forever, since nothing
ever flips it (no shutdown hook fires on SIGKILL). That's cosmetic for
`status` output — it doesn't affect job claiming or correctness, since
claiming only reads the `jobs` table — but it's a known inaccuracy I'd fix
with the same heartbeat idea applied to the `workers` table if I had more
time.

## 3. Does `dlq retry` reset `attempts`?

Yes — `retryDeadJob()` in `JobRepository.java` sets `attempts=0` explicitly:

```java
ps.setInt(2, 0);
```

**Why that's the right call:** a job in the DLQ has already burned its
entire `max_retries` budget under whatever `max_retries` was configured at
the time. `dlq retry` is an explicit, human-initiated decision to give the
job a fresh shot — it should get the *same* number of attempts a brand-new
job would, using the *current* `max_retries` config (also re-read on retry,
not frozen from the original enqueue). Not resetting `attempts` would mean
a retried job could immediately re-die on its very next failure with zero
chances, which defeats the purpose of a manual retry.

## 4. `worker stop` cross-process signaling — designs considered

**Chosen: a state column on the `workers` table**, flipped by
`markWorkerStopAll()` (`UPDATE workers SET stateWorker='stopped' WHERE
stateWorker='running'`) and checked on every iteration of each worker's
poll loop (`isWorkerRunning()`). Since SQLite is already the shared source
of truth for job state, this needed no new infrastructure — `worker stop`
from another terminal is just another SQL statement, and workers notice it
within one poll cycle (<=500ms), the same latency bound as job claiming.

**Rejected: PID files + OS signals (`SIGTERM` to a stored PID).**
Rejected because it's platform-inconsistent (signal semantics differ on
Windows, which this project also targets via `queuectl.bat`), and it opens
a stale-PID problem: if the OS reuses a PID after a crash, signaling it
could hit an unrelated process. A DB flag has no such ambiguity.

**Rejected: a control socket / local IPC server per worker.** Rejected as
disproportionate complexity — it means standing up and tearing down a
listener per worker process, handling partial reads/writes, and a new
failure mode (socket bind conflicts) for a feature that a single UPDATE
statement already solves given SQLite is already in the critical path.

## 5. If priorities were added tomorrow

**Survives unchanged:**
- The atomic claim mechanism (single `UPDATE ... WHERE id = (SELECT ...)`)
  — priority is just another predicate/ordering key inside the same
  subquery, the atomicity argument doesn't change.
- The heartbeat/reclaim logic — stale-processing detection doesn't care
  why a job was picked, so it's orthogonal to priority.
- `dlq retry`, backoff/exponential retry, and `worker stop` signaling — all
  operate on job state independent of ordering.

**Breaks / needs changes:**
- The claim query's `ORDER BY createdAt` becomes `ORDER BY priority DESC,
  createdAt ASC` — a one-line change, but every place that assumes FIFO
  ordering (mainly `list`/`status` output) would need to decide whether to
  show priority order too.
- Schema needs a `priority` column (default 0) and `enqueue` needs a
  `--priority` flag.
- Starvation becomes a real risk once high-priority jobs can jump the
  queue indefinitely — I'd need an aging rule (e.g. effective priority
  increases with wait time) to guarantee low-priority jobs eventually run,
  which the current design has no concept of.
