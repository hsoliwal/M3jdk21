<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse capabilities as M3JDK21 product requirements

Status: category map and one implemented Java collection atom; **not** a full Eclipse fork migration or a new JDK standard API.

Eclipse projects reveal reusable Java platform capabilities. Their presence does not prove that the Java SE specification lacks those features: a workspace, extension registry, native event loop and EMF containment machinery are framework policies and cannot be silently introduced as `java.*` APIs.

| Eclipse evidence | Existing M3JDK21 owner to reuse | New work / admission gate |
| --- | --- | --- |
| EFS and Resources | `m3/fs`, `m3/eclipse/filesystem`, `m3/file/M3File` | Preserve NIO provider, mounted generation, linked resource, refresh and lock semantics; full platform Tycho/PDE still pending |
| IResourceDelta and builders | `M3File.contentDelta`, `M3FileDeltaLedger`, `M3EclipseResourceDeltaSnapshot` | Preserve moves, marker and description flags, pre/post snapshots and build dependency closure; no digest-only shortcut |
| Jobs, Runnable/Callable and rules | Java `ExecutorService`, `ScheduledExecutorService`, `M3EclipseScheduledTasks`, `M3EclipseJobScheduler` | Job families, nested `ISchedulingRule` containment, cancellation and thread ownership remain Eclipse JobManager policies |
| IProgressMonitor/SubMonitor | `M3ProgressMonitor`, `M3ChildProgressMonitor`, `M3EclipseProgressMonitor` | Fractional/cancellation-generation, lifecycle/ownership and actual Eclipse contract qualification |
| EMF EList/NotifyingList | `M3ArrayList`, `M3LazyEvents`, new `M3ObservableList` | Preserve EMF inverse/containment, proxies, uniqueness, notification-chain order, undo/redo and serialization in a future receiver; no EList equivalence claim |
| SWT/JFace/Workbench/Nebula/GEF | `M3WindowPlanner`, `M3ListModel`, indexed selection and viewport state | Preserve UI-thread and native widget ownership, focus/selection, accessibility, GC/disposal, DPI and plugin APIs; measure real UI latency |
| JDT/AST/LTK | Existing M3 AST/compiler/source facts and Synexia OpenRewrite recipes | Keep parser/task/binding ownership, compiler ABI and exact recipe preview/refactoring semantics; full JDT fork gate pending |
| OSGi/Equinox/PDE | `M3EclipseBundleSnapshot`, `M3EclipseExtensionSnapshot` | Never replace bundle resolver, extension execution authority or classloader identity with a snapshot |

## First additive atom: observable M3 list

`com.m3.collections.M3ObservableList<E>` is a concrete `java.util.List<E>` with one `M3ArrayList<E>` payload owner. Callback registration uses a copy-on-write array; mutation dispatch passes primitive index/count, one change kind and old/new single-value references without constructing per-row notification objects. It supports nulls, duplicates, backed sublists, ordered synchronous mutation notifications and reentrancy refusal. Listeners are external-synchronization clients; a callback exception propagates *after* the write. The standalone Java21 differential source is `m3/collections/src/test/java/com/m3/collections/M3ObservableListContract.java`.

This first atom is **not** EMF's `EList`: the future EMF receiver must project its actual notification event objects on demand at the public boundary and preserve its distinct model contracts. Do not confuse a minimal event primitive with completed model notification integration.

## Recipe-first receiving

Canonical owner: `hsoliwal/com.synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017`, dataset `eclipse-observable-20261021`. It reuses the existing hash-pinned Java and text snapshot recipes and records ABSENT-before paths plus exact SHA-256 postimages. It must be executed and its fixed-point/refusal gates must pass before qualification; do not manually alter any generated product source. The source candidates remain draft receiver output until that evidence exists.

Problem categories: dynamic array, copy-on-write subscription snapshot, observer notification, range edit, generation tracking. Existing M3 `M3ArrayList` selected; EMF Java API is a contract reference, **not** copied code. LeetCode/HackerRank/GeeksforGeeks are optional algorithm-category evidence only. No JNI calls or native symbols are needed for Java callbacks; primitive scan/compaction kernels can be considered for measured batch workloads, never for calling listeners.

Next receiver sequence: (1) true EMF `NotifyingList` adapter + differential notification oracles, (2) Resources delta and workspace locking, (3) job/submonitor ownership, (4) SWT/JFace/Workbench viewport migrations, (5) JDT AST/build/refactoring consumers. All follow original Eclipse APIs with package ownership and branch-specific compatibility acceptance.
