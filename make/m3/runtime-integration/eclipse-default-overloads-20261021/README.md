# Eclipse-derived progress conveniences

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

## Contract and ownership

This leaf adds four default overloads to existing Java API owners. It does not introduce a new monitor, future, scheduler, collection representation, native entry point, or public M3 type. The existing naming authority already identifies `ExecutorService`, `ProgressMonitor`, and `ProgressMonitors`.

| Public owner | Additive signature | Existing implementation owner |
| --- | --- | --- |
| `java.util.concurrent.ExecutorService` | `<T> Future<T> submitMonitored(Runnable task, T result, ProgressMonitor monitor)` | Executor's virtual `submit(Runnable,T)` and `ProgressMonitors.run` |
| `java.util.concurrent.ProgressMonitor` | `child(long parentTicks, long totalWork)` | Virtual `child(String,long,long)`, with a null name |
| `java.util.concurrent.ProgressMonitor` | `child(String name, long parentTicks)` | Virtual `child(String,long,long)`, with `UNKNOWN_WORK` |
| `java.util.concurrent.ProgressMonitor` | `child(long parentTicks)` | Virtual `child(String,long,long)`, with a null name and `UNKNOWN_WORK` |

The monitored submission returns the delegate executor's actual Future. It accepts a null result, rejects a null task or monitor before submission, borrows the monitor, and restores the previous binding on every exit. Cancellation checks run inside the submitted operation so the existing Future records exceptional completion. Monitor cancellation and `Future.cancel` remain different operations. The method does not call `done`, shut down an executor, or select a worker policy.

The existing two-argument `submitMonitored(Runnable,ProgressMonitor)` retains its original `submit(Runnable)` dispatch. Redirecting it through the new result-bearing overload could change subclass behavior. The existing `submit(Runnable,T)` API, including `submit(task,null)`, remains unchanged.

All child conveniences delegate directly to the existing full virtual method. Overrides retain control of validation, exception identity, result identity, cancellation and allocation. Existing independent-child lifetime semantics remain authoritative. Eclipse `SubMonitor` has a different single-active-child contract and is not substituted.

## Source and recipe custody

- Target source pin: `hsoliwal/M3jdk21@5eeb841168ed420085223806fefcfca8d1b6b3ca`.
- Canonical recipe pin: `hsoliwal/com.synexia@97d870b2128985c4050d8b958a6d03325bc7fb2f`.
- Existing owner: `synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017`.
- Additive dataset: `eclipse-defaults-20261021`.
- Existing class-backed composition: `com.synexia.m3jdk.AstBuildReverse20261017`.

The dataset preserves exact source preimages and postimages. The existing Maven/OpenRewrite proof emits actual SDK Results. The existing receiver admits all target paths and source hashes before writing, checks the qualified recipe input/output receipts, and supports a zero-change second application. Product source must be received from those Results; the canonical recipe engine remains unchanged.

## Donor evidence and M3 constraints

The [e4 Progress Service discussion](https://wiki.eclipse.org/E4/EAS/Progress_Service) identified progress-aware execution and the Equinox concurrent package. The inspected [Equinox source](https://github.com/eclipse-equinox/equinox/tree/53e3dbba7e8e960f814efecb5d7af213242eda48/bundles/org.eclipse.equinox.concurrent) uses its own future/executor contracts and an abstract-class bridge. It is contract evidence; its thread-per-task implementation is not adopted.

M3's existing canonical payloads, array/index/mask layouts, parent-owned lazy residency and qualified bulk JNI policy remain the implementation rules for subsequent families. These convenience defaults add no alternate storage or scheduler. No speed, allocation, whole-platform equivalence or family-promotion claim follows from this change.

## Qualification contract

Apply the documented order: recipe/template patch, lint, compile, tests, runtime. Preserve failures and pre/post hashes; restart at lint after a code correction.

`test/jdk/java/util/concurrent/ProgressJob/M3ProgressOverloads.java` exercises default and SAM shapes, delegate dispatch and Future identity, null handling, legacy overload resolution, scope restoration, task failures, monitor cancellation, interruption, executor rejection, real-worker isolation, queued Future cancellation, weighted progress, saturation and child overrides.

For a finite old-binary witness, compile `M3LegacyExecutor.java` using stock JDK 21 without a patched module. Hash and retain its class files. Compile the suite against the patched API and that frozen class, then run with the same frozen bytes. A normal jtreg `@build` recompiles the helper and is not by itself old-binary evidence.

Required recipe gates are the existing five SDK tests, eight input-sealing tests, nine receiver-integrity cases, actual apply, and zero-change second apply. Target gates are Java 21 lint/compile, the behavioral suite, the frozen-binary run and an exact public-API comparison. Record actual results in the canonical enhancement receipt and pull request. Full source-built JDK, jtreg execution through the harness, Eclipse/native UI acceptance and performance qualification require separate runs and must not be inferred from this focused patch-module proof.

## Remaining convergence work

Reconcile existing scheduled-executor/resource-delta and Platform UI drafts before adding corresponding work. Logical scheduling rules, filesystem/provider behavior, coherent workspace deltas, observable-model semantics, native UI lifetimes and incremental Java tooling each retain their own semantic and runtime acceptance gates. This leaf does not assert their migration is complete.
