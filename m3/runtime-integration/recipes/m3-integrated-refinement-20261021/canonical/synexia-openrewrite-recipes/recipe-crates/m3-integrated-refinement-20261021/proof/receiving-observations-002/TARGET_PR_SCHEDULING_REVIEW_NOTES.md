# Scheduling and Eclipse Jobs — target PR review notes

## Changes and compatibility

The Java 21 `ScheduledExecutorService` owner receives four requested additive defaults: `scheduleMonitored(Runnable, ...)`, `scheduleMonitored(Callable<V>, ...)`, `scheduleAtFixedRateMonitored(...)`, and `scheduleWithFixedDelayMonitored(...)`. All four original scheduler declarations and JVM descriptors remain unchanged. The defaults call the corresponding existing scheduler, bind the supplied monitor through the existing `ProgressMonitors.run` or `call` only when execution begins, and return the scheduler's actual `ScheduledFuture`.

The monitor is borrowed. The caller retains its lifecycle and responsibility for concurrent use; these defaults do not begin, complete, or cancel it. Each periodic invocation receives a fresh lexical binding and restores the preceding binding on exit. Timing, non-overlap, rejection, exception suppression of later periodic runs, and future interruption/cancellation remain the delegated scheduler's rules. A rejected task starts no monitor lifecycle. The existing `ProgressJob`, status, monitor, and child-monitor owners remain in place; there is no second executor implementation or new JNI path.

The Eclipse core-runtime module gains the existing donor `M3EclipseJobs` and `M3EclipseJobScheduler` types from PR #1576, pinned at `6b75e71559c09c9212ef051f2d3927202eaa5819`. This is an explicit additive public API relative to the receiving revision; recovered donor public/protected descriptors are retained. The unchanged core-runtime POM resolves the actual Eclipse runtime 3.35.0 and jobs 3.15.900 dependencies. The donor's older explicit jobs dependency was not imported.

`M3EclipseJobs.submit` uses a caller-supplied `ExecutorService` and an exclusive M3 progress owner. Here successful cancellation of an unfinished returned future propagates cancellation to that exclusive owner. `runNow` owns begin/done around the operation and supplies the existing borrowed Eclipse monitor facade. Child submission uses the existing M3 split/generation rules, including propagation of child cancellation to its parent. These exclusive-owner contracts differ deliberately from the Java scheduler defaults' borrowed-monitor contract.

`M3EclipseJobScheduler` schedules an actual Eclipse `Job` through the existing `JobManager` and returns that job. Scheduling-rule exclusion, cancellation, join, and lifecycle listeners remain native Eclipse behavior. The only recovered production correction catches standard Eclipse `OperationCanceledException` alongside Java `CancellationException`, sets M3 cancellation, and returns `CANCEL`; the donor previously converted that standard Eclipse cancellation to `ERROR`. Other checked exceptions become `ERROR`, interrupted execution restores interruption and reports cancellation, and JVM `Error` propagates after completion. User callbacks remain in Java.

## Verification of actual emitted owners

| Actual receipt | Scope and result |
| --- | --- |
| `jobs-work/emitted-target-proof-01/RESULT.json` | PASS against the source actually emitted by the recipe: 16 runtime launches with Java 21 mixed and interpreted execution. The unchanged `M3ProgressJob` oracle retains 81 checks; the scheduled contract oracle retains 153 checks for explicit baseline composition and the new defaults. A frozen stock-compiled legacy client retains six checks, including invocation through an old scheduler implementation. Strict compilation, documentation checks, exact descriptor comparison, and a retained baseline negative compile establish the four additions. |
| `jobs-work/emitted-eclipse-owning-01/RESULT.json` | PASS for the actual unchanged whole core-runtime owning Maven POM: three production sources, four test sources, four suites, 21 tests, no failures, errors, or skips. Uses the source-qualified receiving collections artifact and actual Eclipse dependencies. Includes native Eclipse cancellation comparison, conflicting-rule serialization, and cancellation of a waiting native job without starting the M3 owner. The output module JAR is SHA256 `a8d333178423abeda8b0454a8107f989852df3bafca56d3f1cc0201da5ff610c`. |

Receipt SHA256 values are `d3d012bf73991c9b1bd1dfcd5f2227cd02e090dc199b0bfc43b217e108cda1d8` for the scheduler and `f117fa9737bb59dcc6d138afbe5a543260ac2b04d507e7c5c6bd0b8b1b592e5a` for the full core-runtime owner. Both receipts bind exact commands, inputs, and output evidence. Earlier authoring failures remain history and are not substituted for these actual receiver results.

## CI correction under review

Independent review reproduced a concrete prerequisite failure in the emitted framework workflow: its SDK parser classpath omitted the actual `M3EclipseProgressMonitor` owner. The frozen 27-member CI classpath fails the sealed `#borrowed` attribution assertion. Adding only the actual qualified core-runtime classes produces a 28-member classpath that passes both unchanged SDK tests, all 48 recipe phases/paths, and the 129-check/32-case correction-memory conformance gate.

The single-file successor candidate runs the existing core-runtime owning POM after resource verification, adds its `target/classes` to the parser classpath, and retains its Surefire reports in the existing always-upload artifact. Existing triggers, permissions, checkout pinning, historical gates, and job budget are unchanged. Its canonical successor application is tracked separately; the isolated classpath trial alone is not a hosted Actions result.

Evidence is retained under `jobs-work/final-review/ci-classpath-01/` and `ci-classpath-plus-core-02/`; the latter `RESULT.json` has SHA256 `51d546bbdca192faa2472a2de8758b0d6bda925060b915183ff84bbba24a17fe`. The successor handoff is `jobs-work/final-review/workflow-successor/HANDOFF.json`. The existing object-progress workflow's new exact-head scheduled job uses source readback and the actual scheduler verifier and has no parser classpath requirement.

## Boundaries

These receipts qualify the stated Java 21 owner contracts and source replay. They do not assert a complete source-built JDK image, a full jtreg campaign, an OSGi/workbench/UI launch, source-built Eclipse dependencies, hosted GitHub Actions completion, or performance improvement. Existing global admission states are unchanged. The Eclipse whole-module test is distinct from the focused original donor evaluation, and the actual scheduler proof is distinct from the classpath SDK proof.
