<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 Eclipse monitored jobs: receiver qualification (2026-10-20)

**Authority:** M3JDK21 is a single integrated SDK. This receiving atom is source inside M3JDK21; it is not an independent Synexia runtime dependency and does not rename or replace Eclipse public interfaces.

## Source/provenance
- Target: `m3/eclipse/core-runtime/src/main/java/com/m3/eclipse/runtime/M3EclipseJobs.java`, plus its contract test.
- Canonical recipe: `hsoliwal/com.synexia#12575`, `com.synexia.rewrite.M3EclipseJobs20261020`, existing `M3HashPinnedJavaSnapshotRecipe`.
- Source custody: Java LST templates with `ABSENT` preimages and exact SHA-256 postimages in the canonical and receiver manifests; any drift is refused.
- Existing owner: `com.m3.collections.M3ProgressMonitor` / `M3ChildProgressMonitor`; borrowed monitor: `M3EclipseProgressMonitor`.
- Donor selections: OpenJDK 21 `ExecutorService` and `FutureTask` public APIs; Eclipse Platform `JobManager` remains authoritative for scheduling rules, priorities, job families and lifecycle. No donor implementation source copied.
- Java status and user callbacks stay Java; this slice introduces **no JNI symbols** or JNI callback dispatch.

## Behavioral contract
- Each submitted top-level task receives an exclusively owned M3 progress monitor. Concurrent children obtain distinct `split(parentWork)` monitors with shared parent progress.
- Normal completion produces the original `IStatus`. Declared `Exception` -> ERROR status with original throwable. `InterruptedException` / `CancellationException` -> CANCEL; interruption is preserved.
- `Future.cancel()` cancels a pending or running task following `FutureTask` semantics and marks the corresponding M3 owner canceled; `Future.get()` throws `CancellationException` after cancellation even if the callback later observes cancellation.
- `beginTask()` and `done()` are owned by the task body, not the borrowed Eclipse facade. Rejection occurs before beginning progress.
- `submitChild()` intentionally propagates cancellation to its parent; parents must not switch generations while their children run. A canceled child's final `done()` follows the existing child's credit semantics.
- This is **not a substitute for Eclipse JobManager**: callers requiring `ISchedulingRule`, UI thread affinity, family joins, Eclipse job priorities or job-change events must continue to schedule via the original Eclipse Job contract.

## Evidence and unexecuted gates
- **Observed local evidence:** Java 21 `javac -Xlint:all -Werror` compiled this isolated task implementation; seven behavior methods passed in a local harness with API-compatible Eclipse/JUnit stand-ins.
- **Not claimed:** real Eclipse dependencies, canonical Maven/OpenRewrite replay, fixed-point/drift tests with the actual OpenRewrite engine, full `m3/eclipse/core-runtime` Maven verification, OSGi/PDE/m2e/Tycho builds, M3JDK21 matched HotSpot/JNI image, platform job-scheduling parity, benchmarks or a universal speedup.
- The accompanying recipe test *defines* four recipe gates (generation, fixed-point, source drift refusal, hash integrity and named recipe discovery) but they have not been executed in the canonical Maven environment.
- Run `mvn -B -f m3/eclipse/core-runtime/recipe/pom.xml test` then `mvn -B -f m3/eclipse/core-runtime/pom.xml verify` with current dependency artifacts. Keep this PR **draft** until these and the relevant Eclipse integration gates pass.

## Next serial, file-scoped candidates
1. Receive original Eclipse Jobs scheduling-rule/conflict traces before changing the scheduler's internals. No unconditional executor substitution.
2. Reuse existing M3File source/class delta work and existing `M3EclipseResourceDeltaSnapshot` for workspace metadata; preserve IResourceDelta ordering and owner identities.
3. Integrate JDT's authoritative semantic AST/compiler with M3AST and Maven 4 reactor/Resolver, m2e and Tycho in independent, version-pinned recipes.
4. Reuse existing M3 filtered/sorted dynamic-array, offset, index and mask owners; avoid duplicate wrapper stacks.
5. Reuse Lucene+OpenNLP only for candidate retrieval/semantic enrichment; JDT retains binding/working-copy authority.
6. Preserve Eclipse SWT, Nebula, JFace and OS lifecycle contracts; JNI can accelerate primitive spans only after parity proof.
Each candidate requires source identity, donor comparison, recipe tests, differential tests, and its own acceptance evidence.

## Expanded original Eclipse JobManager path
- **New original Job path**: `M3EclipseJobScheduler.schedule()` uses `org.eclipse.core.runtime.jobs.Job` and `ISchedulingRule` without substituting an M3 executor for Eclipse scheduling. Clients get the actual `Job` for join/cancel/priorities/families/listeners.
- **Shared progress**: original Eclipse `IProgressMonitor` and existing borrowed `M3EclipseProgressMonitor` receive the same user callback progress. One M3 owner alone owns `beginTask/done`; original Eclipse JobManager controls the job lifecycle. Cancellation is read in both directions; task exceptions remain `IStatus.ERROR`.
- **Local evidence**: Java21 `javac -Xlint:all -Werror` on both Java sources and their 12 test methods passed with API-faithful Eclipse/JUnit stand-ins. No real Eclipse Job/OSGi runtime tests were run locally.
- **Canonical recipe**: same Synexia PR #12575 with four sorted, SHA-256-sealed `ABSENT` targets and an updated recipe fixed-point/drift suite. The actual Eclipse implementation remains M3JDK21 code.
- **Required new real integration gates**: PDE/Equinox JobManager ISchedulingRule conflict exclusion; native job cancellation before/after run; priority/family notifications; real `org.eclipse.core.jobs` 3.15.700 provided dependency and bundle imports.
