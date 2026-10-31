<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse-derived Java capabilities: M3JDK21 SDK convergence (2026-10-21)

**Status: additive implementation candidate; not full Eclipse parity or a promoted M3JDK21 release gate.** M3JDK21 is one integrated Java 21 / HotSpot / JNI product, not an M3-on-JDK library overlay. This is a capability-led convergence plan, not a proposal to rename Eclipse public APIs as M3 or pretend Java lacks Runnable, NIO or Swing.

## Principle: Eclipse reveals higher-level contracts

Eclipse EFS/Resources teaches provider-aware filesystem identity, workspace deltas, markers, linked resources, refresh and transaction/lifecycle boundaries. Eclipse Jobs teaches cancellation-aware scheduling, nested progress and resource *scheduling rules* in addition to Java's existing Runnable/Callable/Future/ExecutorService. SWT teaches native UI lifecycle and thread ownership, not the absence of Swing/AWT. EMF teaches observable containment graphs, notifications and change recording, not the absence of Java Collections. JDT/LTK teaches sophisticated project-wide source indexing and refactoring, building on (not replacing) javac's compiler authority.

**Promote general capabilities into M3JDK21-owned modules/classes only where Java contracts and measured economics justify it.** Preserve public Java SE names and ABI; preserve original Eclipse `org.eclipse.*` names, extension points, identity, service lifecycles, notifications, UI thread rules and listeners. JNI does bounded native/primitive spans, never arbitrary Java predicates or EMF callbacks.

## Existing authority before any new code

| Capability | Existing product owner | Admission boundary |
| --- | --- | --- |
| Filesystem | `m3/fs`, `jdk.m3fs`, `com.m3.fs.M3FS`, `m3/eclipse/filesystem` | M3FS+EFS linked Resources tests are scoped; provider/OS/file-lock/ACL and all workspace semantics remain independent |
| File content change | `com.m3.file.M3File.fingerprint` and `contentDelta`, independent M3File source and AST delta work | Metadata is a hint; stable generation and full build context are required before cache/rebuild avoidance |
| Progress/child work | `M3ProgressMonitor`, `M3ChildProgressMonitor`, `M3SystemProgress`, `M3ShadowStreams` | Keep cancellation, generations, owned/borrowed lifetimes, single-pass I/O and stream failure semantics |
| Task scheduling | Java 21 ExecutorService/ScheduledExecutorService; existing `M3EclipseJobs` and scheduled task receiver | `ISchedulingRule`, families, priorities and Eclipse JobManager continue to own Eclipse scheduling |
| Indexed state/collections | `M3LongArrayList`, `M3IntArrayList`, `M3ObjectStateSlots28`, filtered view and JNI mask owners | No per-entry object store, no general provider substitutability without contract testing |
| UI, compiler, metadata | existing M3 viewport owners, `M3AST`, JDT source probes, indexed extension/bundle snapshots | No complete SWT/EMF/Equinox/JDT port or universal performance gain is claimed |

The detailed family-by-family state and next tests are in [ECLIPSE_SDK_CAPABILITY_LEDGER_20261021.tsv](ECLIPSE_SDK_CAPABILITY_LEDGER_20261021.tsv). See the earlier [Eclipse substrate contract](M3_ECLIPSE_SUBSTRATE_CONTRACT.md), [existing framework coverage](framework/ECLIPSE_FORWARDING_COVERAGE.tsv), and [build delta invariant](../docs/build-delta-invariant.md); those are unchanged, not overridden.

## Bounded implementation in this branch: indexed change journal

New `com.m3.collections.M3ChangeJournal` uses the **existing M3 primitive owners**: one `M3LongArrayList` for stable owner-issued handles, two `M3IntArrayList` lanes for full 32-bit change flags and packed 8-bit before/after state. It retains a caller-bounded ring of 16 payload bytes per event (excluding lane growth/headers), not another object graph. The class records **no payload references** and allocates no per-event notification object. One immutable, detached primitive page is materialized per consumer snapshot request.

`record(identity, changeMask, oldState, newState)` serializes concurrent writers and gives strictly monotone sequence numbers. `snapshotAfter(cursor, maxEvents)` pages in sequence order, reports exactly how many entries have been overwritten, and refuses future/negative cursors. After an overrun the consumer MUST rescan the source, never infer unchanged. Snapshot queries do not hold callbacks under locks. A generation-bearing identity comes from the source owner; the journal itself does not validate a recycled slot. Full 32-bit flags including bit 31 and all signed long identity values remain intact.

This is a **new, optional observation sidecar** alongside existing immediate `M3LazyEvents`. It does not yet auto-record `M3ArrayList` mutation, translate `IResourceDelta`, install EMF `Adapter` / `NotificationChain`, replace native SWT event delivery or change a public JDK class. A later original-owner recipe must connect one source at a time with before/after tests. No JNI export has been introduced; for these short synchronized primitive writes JNI would add a crossing, and no speed claim is made.

## Ordered receiving and application to Eclipse forks

1. **Prove the SDK owner.** Canonical Synexia OpenRewrite crate `m3-eclipse-indexed-change-journal-20261021`, then exact target Java source/test. Run the real collection module, its JUnit/JDK21 source, strict warnings, installed-image module/ABI, source-seal and memory/resource benchmarks. Current global receiving phase remains **STRING**; this collection candidate does not promote that gate.
2. **Resources/EFS/Jobs.** Use `M3File` and provider-owned generations; qualify the latest M3 resource delta snapshot in `hsoliwal/eclipse.platform`. Keep Eclipse scheduling rule conflicts and beginRule/endRule identity/nesting under the original JobManager. A Java-native M3 resource scope/lock API is a separate future contract, not yet installed.
3. **EMF observability.** Catalogue `EList`, `InternalEList`, `Notification`, `NotificationChain`, `Adapter`, `EObject`, containment, proxies and resolving lists. For one actual EMF original owner, generate only an optional primitive notification index/journal; preserve notifier identity, eDeliver, event type/order, inverse/containment and exception semantics. Do not substitute a stream for an API-required synchronous notification.
4. **SWT/JFace/Workbench.** Apply existing M3 index/rank/mask/viewports to one virtual table/tree or viewport atom after native GTK/Win32/Cocoa resource lifetime, Display thread, paint/selection, disposal and accessibility tests; then reuse for Nebula/GEF. A generic new widget layer is not the goal.
5. **JDT/Editors/Compiler.** Retain JDT DOM/reconcile/search/LTK semantics and javac/HotSpot ownership. Feed `M3File` stable source/class generations and `M3AST` immutable indexed facts into original JDT and existing Maven/Gradle/Tycho/Ant owner planners; only skip work when ABI, lookup, processor and build context gates pass.
6. **Equinox/PDE/p2/Debug/Team.** Catalogue runtime services, plugin activation, provisioning, launch, marker and SCM behaviors and qualify small indexed atoms. Never replace OSGi identity/resolution or JGit/Team action semantics with a simple array scan.

For each original file: **Atomize -> Patternize -> Annotate -> Document**; identify a meaningful contract-bearing atom, compare already-qualified M3 owner and legally reusable donors by problem category (GitHub, LeetCode, HackerRank and GeeksforGeeks are category discovery, not blanket licensing), create/refine the Synexia Maven/OpenRewrite recipe, replay serial per file, prove source pre/post hash/fixed-point/drift refusal, then compile, test, measure and open a reviewable PR. Do not manually fix a generated target.

## Consolidation policy for existing PRs

Do not supersede overlapping work by name. Reconcile semantic source snapshots for [M3JDK21 #1608](https://github.com/hsoliwal/M3jdk21/pull/1608) (file/AST delta), [#1611](https://github.com/hsoliwal/M3jdk21/pull/1611) (progress/shadow/file receipt), [#1637](https://github.com/hsoliwal/M3jdk21/pull/1637) (collection/jobs/resource state), and [#1720](https://github.com/hsoliwal/M3jdk21/pull/1720) (viewport page). Preserve the union of validated behavior with forward recipes; no force updates, automatic merges, or silent cancellation of unresolved gates.

## Exact provenance and qualification

- Canonical source recipe: [Synexia PR #12823](https://github.com/hsoliwal/com.synexia/pull/12823), `feat/eclipse-sdk-capability-journal-20261021` at `f103e6f4336861f5f1684e5a4baa0e73b590154c`.
- Target postimage SHA-256: Java `7317a7c0291a79541c3d88b383eda4d86688fbf42c95ff4bba95229d04f7f83c`; test `96cb48effeb2860553523667310c6c865f3715145c0ebaca8075bf721062705e`; ABSENT-before paths.
- Isolated **Java 21 strict compile + runnable contract PASS** only with minimal API-equivalent stand-ins for the two M3 lane owners. The exact canonical and receiving Java files are source-sealed. This is a narrow smoke test, not actual original-owner Maven/JUnit.
- **Pending**: canonical OpenRewrite SDK execution, source-built original M3 collection Maven/JUnit/JMOD, full JDK/HotSpot/JTReg/JCK, Eclipse/EMF/OSGi/PDE, JNI/native platform matrix, and comparative CPU/heap/native memory. Retain draft until exact-head evidence exists.
