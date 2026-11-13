<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss leaf 88: ThreadPerTaskExecutor waits for cancelled running tasks

Origin a12623b2633 (2026-10-21) carried the upstream form of `ThreadPerTaskExecutor.ThreadBoundFuture`:
`executor.taskComplete(thread)` runs in the `finally` of `run()`, not in `done()`. A retain merge kept the 21.0.9 form.

With `done()`, a `cancel(true)` on a running task completes the future at once, and `done()` deregisters the
thread on the cancelling thread. `close()` and `awaitTermination` can then return while the task is still running.
`CloseProbe` shows this on every round with master and on no round with this change (GATES.tsv).
