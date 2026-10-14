# M3 object residency: parent-owned lazy projections under memory pressure

Status: design contract; implementation is opt-in and additive. Existing JDK/Swing/SWT APIs and behavior remain locked.

## Objective

Ordinary object-oriented APIs often allocate short-lived wrapper/view/widget objects for data already represented in a parent, packed lane, tree, or index. M3 should keep canonical state in the parent-owned flat representation and treat expensive child objects as disposable projections. The caller requests behavior; M3 chooses whether to materialize, retain, swap, or recreate the projection. Mutations intended to survive projection eviction must update the parent's canonical bit/word lanes (or a declared checkpoint), not only fields on the transient object.

This is not a promise that arbitrary Java object references can be transparently evicted. If a raw child reference escapes and is retained by caller code, the GC must keep that object alive. Transparent eviction therefore requires an M3-owned stable handle/access boundary or a compiler-proven call-site rewrite that does not expose the transient object beyond a bounded use scope.

## Invariants

1. Canonical state wins. Mutable logical state is stored in parent-owned primitive/reference lanes or a declared checkpoint. `M3ObjectResidency` exposes a canonical 64-bit `readParentBits`/`writeParentBits`/`updateParentBits` lane per logical slot; projection state is not canonical merely because it is stored in the arena. A transient projection is not the sole owner of state if it may be destroyed.
2. Stable identity is the handle, not the projection. The handle remains valid across materialize/swap/recreate transitions. Projection identity is not promised unless the opted-in contract explicitly requires it.
3. Behavior and public contracts stay locked. Existing Swing/SWT/JDK methods are not changed by default. New behavior is opt-in by construction flags and recipe-generated internal metadata.
4. No hidden escape. A projection cannot be evicted while a scoped operation is active. Direct raw-reference escape either fails under STRICT_NO_ESCAPE or permanently pins the slot under PIN_EXPOSED. This runtime check cannot detect references hidden in a wrapper, field, callback, collection, or native state; unknown indirect escape remains a compiler/recipe rejection.
5. Bounded memory. Each resident projection carries a conservative byte weight; the manager applies a resident budget and pressure reserve. LRU is a victim-selection policy, not the memory-pressure detector.
6. Recoverability is explicit. SWAP requires a codec/checkpoint store; RECREATE requires a factory and canonical parent state sufficient to reproduce behavior. Otherwise a slot is pinned/non-evictable.
7. No forced GC. Do not call System.gc() to make admission decisions. Sample heap limits/occupancy and use an explicit budget; samples are estimates.
8. Deterministic publication. Restore/materialization completes before the slot becomes RESIDENT. Failures leave the slot recoverable and do not publish partially restored objects.
9. Parent owns children. Child slots, packed flags, size estimates, last-touch order and persistence handles belong to the parent owner; avoid one bookkeeping object per tiny child where packed lanes suffice.
10. Compiler metadata is evidence, not runtime magic. Build-time metadata may record field escape, mutability, construction cost, references, annotations and candidate strategy. Runtime instrumentation must still honor dynamic references, reflection, JNI, serialization, callbacks and concurrency.

## Construction flags

M3ObjectResidencyFlags is an additive bitmask. Proposed stable meanings:

- LAZY_CREATE: do not materialize a child until first use.
- LRU: update access order and evict least-recently-used eligible child first.
- PRESSURE_AWARE: reduce the target resident budget as estimated heap headroom shrinks.
- SWAP_SECONDARY: checkpoint to a secondary store before releasing the projection.
- RECREATE: discard only when canonical parent state plus a deterministic factory can rebuild it.
- ALLOW_NULL: null is a valid materialized value.
- OBSERVE_ONLY: record decisions but do not evict, for safe measurement.
- STRICT_NO_ESCAPE: use only scoped handle access; reject operations that directly return the raw projection. This is a local guard, not a proof against wrapper/field/callback escape.
- PIN_EXPOSED: when an operation directly returns the projection, permanently pin that slot for the arena lifetime. This protects the direct raw reference; compiler escape analysis is still required for indirect escapes.
- DEFAULT: lazy + LRU + pressure-aware + recreate only when a valid recreation contract is supplied.

Unknown bits are rejected. PIN_EXPOSED is conservative and has no automatic unpin because Java does not expose a reliable general reference count. SWAP_SECONDARY and RECREATE are not interchangeable: swap preserves a codec-defined snapshot; recreation rebuilds from canonical parent state.

## State machine

ABSENT -> CREATING -> RESIDENT -> SWAPPING -> SWAPPED -> RESTORING -> RESIDENT

Alternative: RESIDENT -> DESTROYING -> ABSENT is legal only for a declared reconstructible projection whose state is canonical elsewhere. Any failure returns to the last recoverable state. Reentrant creation and eviction while a scoped use is active are rejected. Concurrent use must be serialized or use an explicit owner lock; the initial implementation is thread-confined unless a later proof adds concurrency.

## Viewport, overscan and widget projection

For tree/table viewers, the viewport is a presentation window over the logical parent model, not a limit on logical data. Compute a bounded visible interval plus configured overscan, include required ancestors, and materialize only those widget/projection objects. Scrolling changes window membership; it must not change canonical parent state or logical row identity. Recycle/release only non-escaping presentation projections whose toolkit lifecycle permits it. Focus, selection, accessibility, editing, drag/drop, event order, and disposal tombstones are separate contract surfaces and must be covered by receiver tests.

Existing evidence to reuse rather than duplicate: Synexia's `.m3/donor-convergence/treeviewer-tv50-20261013/` and `.m3/task-crates/swt-tree-residency-live-20261005/`; the latter records compilation and source replay, but explicitly says GUI/runtime tests were not run. In this M3JDK21 checkout, Swing's `TreeDemo.java` is available as a donor, while SWT/JFace receiver source is not present. Keep receiver-specific work open until exact source paths and tests are inventoried.

## Pressure and LRU policy

At construction, the parent owns maxResidentBytes, reserveBytes, strategy flags, and the secondary store/factory. On access:

1. Sample Runtime.maxMemory(), totalMemory(), and freeMemory() without forcing GC.
2. Estimate heap headroom and compare it with reserve and target budget.
3. Before creating a child, reclaim eligible LRU victims until the target budget can admit it or no victim remains.
4. Prefer swap when enabled and a valid codec/store exists; otherwise recreate-only destruction when explicitly valid; otherwise leave resident and report BUDGET_EXHAUSTED.
5. Record estimates, decisions, bytes and failures for verification. Do not claim an allocation is guaranteed merely because the estimate fits.

Pressure checks occur at object materialization/access boundaries and optionally after a coarse operation count, not in every field read. A VM-integrated implementation may add GC notifications later; it must not make collector-specific behavior part of the public contract.

## Compile-time facts and recipe

The compiler/recipe should inventory candidate transit-object types and record: class/member annotations, construction sites, estimated shallow/retained size where available, field mutability, parent ownership, escape edges, callback/reflection/JNI/serialization boundaries, constructor side effects, and reconstruction/checkpoint capability.

Recipe stages are serial and per file: inventory -> classify atoms -> generate private metadata/flags -> apply a narrowly scoped transformation -> compile -> differential behavior tests -> escape/identity tests -> memory-pressure tests -> fixed-point replay. Metadata is generated separately from runtime objects and is pinned to class-file/source hashes. A stale hash or unknown escape edge blocks transformation.

The first recipe only generates metadata and an opt-in handle adapter for explicitly annotated, proven candidates. It must not automatically rewrite all Swing/SWT/JDK objects. The existing SWT TreeViewer port is a regression fixture/donor to inspect; its exact paths and tests must be inventoried before claiming it is covered.

## JNI boundary

JNI can help with compact primitive state lanes, spill-file I/O, or measured serialization kernels. It cannot safely make arbitrary Java objects disappear while Java references remain live. Keep the Java reference implementation authoritative. Native handles must not be used as Java object addresses. Require -Xcheck:jni, sanitizer runs, native/Java differential tests and measured improvement before enabling a native path.

## Verification matrix

- Lazy construction: never-used child is never created.
- Parent canonical state: mutation survives projection destruction and recreation.
- LRU: deterministic tie-breaking, touch refresh, pinned/active children never evicted.
- Swap: round-trip state, checksum/source identity, corrupt snapshot, I/O failure and quota exhaustion.
- Recreate: stable behavior from canonical state; constructor side effects not repeated unless declared safe.
- Memory pressure: small -Xmx, headroom/reserve boundary, allocation failure, no forced GC, budget exhausted.
- Escape/identity: leaked raw references block eviction; handle identity remains stable.
- API: existing Swing/SWT/JDK public signatures and event order unchanged when flags are OFF.
- Compiler: stale metadata refusal, unknown annotation/bit refusal, reflection/JNI/serialization escapes block rewriting.
- Fixed point: recipe second pass has zero diff.

## Deliberately not claimed

No arbitrary-object transparent swapping, no GC-guaranteed admission, no reliable object reference count from ordinary Java, no automatic rewrite of unannotated APIs, no source-compatible behavior change for existing Swing/SWT/JDK callers, and no JNI object-address scheme. Those require VM/compiler instrumentation and separate proof.
