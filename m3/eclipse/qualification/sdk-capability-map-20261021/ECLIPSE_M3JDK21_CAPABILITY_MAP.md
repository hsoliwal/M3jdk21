<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse-derived capabilities: integrated M3JDK21 gap and admission map

Status: **GAP INVENTORY / DESIGN RECEIPT ONLY**. This file makes the Eclipse
capability-mining requirement reviewable; it is not an executable Eclipse port or
acceptance receipt. M3JDK21 is a single Java/HotSpot/JNI SDK product, not a
separate runtime overlay. Existing Java, Eclipse, provider, and plugin API names
and observable contracts remain authoritative.

## Extract higher-level primitives, not duplicate existing Java APIs

| Eclipse source | Existing Java/M3JDK21 authority | Useful Java-native improvement | Eclipse receiver and mandatory gates |
| --- | --- | --- | --- |
| EFS | NIO providers, `jdk.m3fs`, `com.m3.fs`, existing Eclipse EFS bundle | Rich provider-owned snapshots, indexed namespace and native bulk I/O | Eclipse EFS remains the compatibility surface; linked resources, cross-provider copy, symlinks, permissions, native lifetime, Equinox and platform suites |
| Jobs | `Runnable`, `Callable`, `ExecutorService`, existing M3 Eclipse jobs/progress adapters | Small generation-checked atomic multi-resource reservations over the **existing** `M3SlotAllocator28` and `M3BitLane28` | Preserve Eclipse `ISchedulingRule` conflict/containment, job families, priorities, cancellation, listeners and JobManager authority |
| Progress and SubMonitor | `java.util.concurrent.ProgressMonitor`, `ProgressMonitors`; `M3ProgressMonitor` and `M3ChildProgressMonitor` | Child budgets, borrowed versus owned lifecycle, cancellation propagation, monitored streams | Weighted/fractional progress, total unknown, generation, completion and concurrent cross-provider behavior |
| Resources | `com.m3.file.M3File` SHA-256/contentDelta; `M3EclipseResourceDeltaSnapshot`; `jdk.m3fs` | File/provider deltas, stable admitted file generations and dirty-index masks | Linked resources, refresh, markers, folder hierarchy, change ordering, watcher overflow, external replacement, failures |
| SWT | Existing SWT `Display` native owner, `m3/viewport-core`, M3 array/bit-lane primitives | Parent-owned indexed widget/view state; bounded JNI bulk operations | UI-thread confinement, disposal/GC/accessibility, selection, paint and input, Linux/Windows/macOS, p95/p99 UI latency |
| EMF | M3 indexed collections, `M3LazyEvents`, `M3LazyEventPolicy`, `M3ObjectStateSlots28` | Observable backed `List`/model-index views with compact state/delta lanes | `EList` and `EObject` proxy resolution, inverse refs, containment, `ENotification` order, undo/redo and serialization |
| JDT | Existing `M3AST`/javac compiler facts and source/ABI delta design | Single admitted source/binary facts, source coordinates, indexed dependency and binding views | JDT `ICompilationUnit`, `ASTParser`, AST ownership, resolver/bindings, refactoring, compiler and Eclipse builder differential suites |
| JFace/Workbench | M3 indexed viewports and existing Eclipse runtime snapshots | Paged/filtered/sorted backed projections | Selection identity and order, virtual/nonvirtual providers, extension points, event dispatch and lifecycle |
| Nebula/GEF | Existing M3 bit and viewport kernels | Sparse/large-grid/chart/diagram index, compact hit-test masks | Per-widget paint, focus/keyboard/mouse, accessibility, listener and resource tests |
| Equinox/PDE | Original OSGi services, existing additive M3 bundle/extension snapshots | Read-only primitive indexes over original registry state | Resolver, bundle identity and refresh, classloaders, activation, Tycho/PDE and extension tests |
| Team/Debug/Build | M3File, M3AST, Maven/Gradle source facts | Source/AST-aware delta and dependency DAG invalidation | Original Git, JDT, LTK, debug protocol and build transactions; rollback and stale output cleanup |

## Admission order and architecture

1. **Audit before rewriting.** Pin the exact Eclipse fork/source path, upstream
   license, M3JDK21 owner, canonical Synexia donor/recipe and before/after source
   seals. Track each contract separately as DISCOVERED, REVIEWED, RECIPE_READY,
   TESTED, ACCEPTED, BLOCKED or DEFERRED.
2. **One Java owner for each behavior.** Preserve `java.util`, NIO,
   `java.util.concurrent`, and `jdk.compiler` contracts; use M3 names for
   internal indexed dynamic-array/offset/mask/physical-slot implementation atoms.
   Don't replicate JobManager, filesystem providers, UI event loops or EMF
   notification rules behind an unrelated compatibility facade.
3. **Serial per-file implementation.** Identify independently replaceable
   behavioral atoms, select a qualified first-party owner or licensed donor,
   use the canonical Synexia Maven/OpenRewrite recipe, then apply and verify
   the exact product postimage. Correct the recipe instead of hand-editing
   generated targets. Challenge sites (LeetCode, HackerRank and GeeksforGeeks)
   are algorithm-category evidence, not a presumption of copyable source.
4. **Requalify in both domains.** Java21 warnings-as-errors; differential API
   behavior; resource/thread/exception/iterator contracts; native JNI
   -Xcheck:jni and sanitizers when applicable; real Eclipse OSGi, Tycho, UI/JDT/EMF
   suites; installed M3JDK21 JMOD/JDK image; comparative CPU/heap/native
   memory plus UI tail-latency measurements.
5. **Keep global phase ordering.** String -> arrays -> collections ->
   AST/compiler -> remaining frameworks. A draft prototype is not runtime
   admission and cannot advance the active String gate.

## Bounded resource-lock candidate

A Java21-only review candidate named `M3ResourceLockManager` was authored
for a separate canonical recipe dataset
`eclipse-lock-gap-20261021` in the established
`m3-collections-contract-refinement-20261017` crate. The proposed
implementation uses `M3SlotAllocator28` for manager-local generation-bearing
handles and `M3BitLane28` for occupancy, plus standard Java21
`ReentrantLock`/`Condition` to reserve a sorted, deduplicated set of
resource handles all-or-none. It does not introduce per-key lock objects,
a new executor, JNI callbacks, or Eclipse-specific public API signatures.

**Not a completed receiving change:** the canonical Maven/OpenRewrite SDK and
actual target-owner runs still need to execute. In particular, an independent
standalone Java21 experiment against local test doubles must never be reported
as full M3 owner or Eclipse rule qualification. Do not add the proposed class
to the delivered SDK or wire it into Eclipse Jobs until recipe and real-owner
gates pass.

This proposal is deliberately distinct from existing overlapping changes:
M3JDK21 PRs #1608, #1611, #1637, #1546 and #1720. Review their contracts,
merge ancestry and exact source epochs before creating a successor code PR.

## Existing source owners

- [M3JDK21 Eclipse substrate contract](../../M3_ECLIPSE_SUBSTRATE_CONTRACT.md)
- [Current Eclipse framework forwarding coverage](../../framework/ECLIPSE_FORWARDING_COVERAGE.tsv)
- [Core runtime receiving status](../../core-runtime/ECLIPSE_SUBSYSTEM_M3_RECEIVING.tsv)
- [M3FS backend](../../../fs/README.md)
- [M3File delta invariant](../../../docs/build-delta-invariant.md)
- [M3JDK21 porting invariant](../../../docs/m3jdk21-porting-invariant.md)
- [Canonical Synexia recipe ownership](https://github.com/hsoliwal/com.synexia/tree/develop/synexia-openrewrite-recipes)

Open issues: full Eclipse platform, EMF/JDT/SWT/Workbench compatibility,
cross-platform JNI/HotSpot acceptance, compiler and UI benchmarks, and the
canonical recipe receiving gates remain **unqualified**.
