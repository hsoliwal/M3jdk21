<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 Eclipse scheduled Runnable/Callable tasks

## Exact scope

`M3EclipseScheduledTasks` adds one-shot `Runnable`, generic `Callable<V>` and
Eclipse `IStatus` execution using the caller-owned Java 21 `ExecutorService` and
`ScheduledExecutorService`. The sole progress state owner is the supplied
`M3ProgressMonitor`. No second queue, task registry, callback scheduler or JNI entry is introduced.

* Executor rejection never calls `beginTask` or `done`.
* Successful start begins the supplied exclusive monitor once; task exit completes it.
* A successfully cancelled Future sets M3 cancellation; a completed Future is not retroactively cancelled.
* `Future.get` preserves ordinary Callable exceptions. `scheduleStatus` delegates Eclipse
  status conversion to the pre-existing `M3EclipseJobs.runNow`.
* Zero/negative delays retain JDK 21 ScheduledExecutorService immediate semantics.
* Caller owns executor shutdown; one monitor cannot be shared between unrelated concurrent tasks.
* Periodic scheduling is deliberately excluded because a single-use progress owner cannot be
  reused across periodic invocations without changing generation/lifecycle contracts.
* Eclipse `JobManager` continues to own `ISchedulingRule`, job families, priority and UI dispatch.

JNI must not invoke arbitrary Java callbacks or own Eclipse lifecycles. Native M3FS remains
independently owned by `m3/fs`; this task adapter does not add a native ABI.

## Recipe custody

The reusable, first-party OpenRewrite engine is the existing
`M3HashPinnedJavaSnapshotRecipe` under `m3/tooling/migration-recipes`. The bounded recipe
`com.synexia.rewrite.M3EclipseScheduledTasks20261021` installs two SHA-256 pinned
ABSENT-before Java targets, including the nine-test regression suite.

The recipe host tests LST generation, same-source fixed point, drift rejection and recipe
discovery. Do not patch generated targets: change the recipe/template then rerun its tests.

## Verification (not a claim of execution)

```sh
mvn -B -ntp -f m3/eclipse/core-runtime/recipe/pom.xml test
mvn -B -ntp -f m3/eclipse/core-runtime/pom.xml verify
```

The existing `m3-eclipse-progress-forward-20261017` workflow builds the M3 collections
dependency, invokes both Maven gates under Java 21 with strict warnings and preserves
Surefire receipts. Compile, actual Eclipse API resolution, and JUnit execution must pass
on the precise PR head before qualification. Existing EFS JNI and UI/PDE/OSGi gates
remain separate; their unexecuted results cannot be inferred from this module.

## Donor review

See `algorithm-catalog/scheduled-tasks-20261021.tsv` for the category-by-category
selection, implementation complexity, license and rejection rationale.
Reference problems from LeetCode/HackerRank/GeeksforGeeks are category signals only;
no challenge or editorial source is copied into M3.
