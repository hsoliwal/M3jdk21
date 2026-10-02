# Whole-JDK collection migration execution overlay

Status: documentation-only execution handoff. Target inspection pin: hsoliwal/M3jdk21@45f546ff5bcb06a1b2604f14baf998785d98d9a1. Synexia inspection pin: hsoliwal/com.synexia@3db24805d640c72ab1bd637d83561696d99561a0. No JDK collection implementation or operational mapping state is changed by this file.

## Authority and role

The canonical detailed collection design is [../../docs/whole-jdk-collections-replacement.md](../../docs/whole-jdk-collections-replacement.md). It owns the concrete family matrix, representation candidates, Java contract checklist, concurrency requirements, boxing boundaries, backed-view rules, serialization/reflection/subclass constraints and measurement plan.

This file is deliberately narrower. It translates that design into migration execution rules and evidence packets alongside [WORK_PACKETS.md](WORK_PACKETS.md), [COVERAGE.md](COVERAGE.md) and [ACCEPTANCE.md](ACCEPTANCE.md). Operational capability IDs, synchronized pins and evidence bindings remain in [../../docs/name-mapping.json](../../docs/name-mapping.json) and its existing tooling.

If prose here and the canonical collection design appear to disagree, stop and reconcile them before implementation. Do not create another collection specification or registry.

## Existing owners to evaluate first

Before creating new collection storage, inspect the exact pinned bodies and dependencies of existing Synexia owners, especially:

- synexia-mat-collections primitive/adaptive vectors, sets, maps, postings, ranges, bit planes, record arenas and deterministic planners;
- synexia-mindex/collections identity/value spaces, primitive-ID collections, canonical frozen collections and Java views;
- other retained Synexia compact/segmented collection families identified by the source inventory;
- existing JDK implementations that are already compact or whose compatibility burden outweighs a proposed replacement.

Reuse is chosen by compatible semantics, lifetime and dependency closure, not by similar names. Maven/application modules are reference sources until a minimal bootstrap-safe component has been source-pinned, license-reviewed, adapted and accepted.

## Per-family migration packet

Each concrete JDK class/family receives one bounded packet containing:

1. stable capability/mapping ID or an explicit pending-ID decision;
2. exact target source pin and fully qualified classes/methods/fields/views;
3. exact Synexia/donor owner pin when reuse/porting is proposed;
4. current JDK physical representation and its measured pressure points;
5. candidate representation or explicit retain/reuse disposition;
6. mutability, object identity, equals/hash/comparator/reference-identity domain;
7. null, duplicate, collision, encounter/sorted/access/priority order rules;
8. backed views, iterators, spliterators, streams and Map.Entry mutation;
9. capacity/growth/overflow/OOME and partial-operation behavior;
10. serialization, clone, reflection, protected/subclass and binary compatibility;
11. GC/lifetime/reference-processing behavior and native/off-heap boundaries;
12. JMM/atomicity/linearization/progress/fairness rules when concurrent;
13. Route A, B and C eligibility plus explicit refusal/materialization boundaries;
14. deterministic transformation/recipe identity where applicable;
15. differential, concurrency, runtime and platform tests;
16. memory/CPU/latency/throughput methodology including admission/precompute/cleanup;
17. rollback/default-off plan;
18. provenance, license, unresolved blockers and exact evidence state.

Discovery, implementation, verification and acceptance remain separate states.

## Route-specific execution

### Route A — explicit M3 APIs

Use existing primitive/immutable M3 or MAT owners directly where their semantics fit. Provide standard-Java adapters only when their aliasing, boxing, mutation and identity costs are explicit. Route A can demonstrate a storage technique without changing ordinary java.util objects.

### Route B — attributed compiler lowering

Lower only operations whose original JDK attribution proves type, dispatch, evaluation order, exceptions, null behavior, aliasing/escape, identity and external ABI are preserved. Primitive lowering of generic collections is rejected when boxed object identity, nullability, reflection, serialization, JNI/native descriptors or unlowered external callers make the wrappers observable.

Transformation work is recipe-first: source-pinned, hash-bound, idempotent, drift-refusing and rollback-aware. A textual class-name replacement is never sufficient.

### Route C — matched custom JDK

A standard java.util class may use an M3 internal backend only after its complete Java compatibility surface is preserved. VM changes are required only where the selected representation crosses normal library boundaries, for example GC-visible handle storage, special object layouts, compiler intrinsics or serviceability assumptions.

Never transplant individual class files into an installed JDK. Build and test one coherent matched image.

## Mandatory stop gates

A collection candidate cannot promote while any applicable item below is unresolved:

- scoped API/binary/serialization/subclass inventory is incomplete;
- exact equality, identity, comparator, null, duplicate or collision behavior differs;
- a live backed/reversed/descending/range view has become an accidental snapshot;
- iterator/spliterator/stream behavior or exception timing differs;
- boxing/materialization boundaries are hidden or wrapper identity becomes observable;
- GC cannot discover a live reference or a weak-reference/reference-queue contract changes;
- a concurrent operation lacks a defensible happens-before/publication and linearization argument;
- progress/fairness/interruption/timeout behavior regresses where promised;
- resize/growth/overflow/OOME/error paths are untested;
- serialization, clone, reflection or subclass hooks are incompatible;
- source/target drift makes the transformation recipe inapplicable;
- exact-candidate build/runtime/platform gates are unexecuted or failing;
- a benchmark excludes admission, conversion, precomputation, cleanup or retained backing;
- provenance/licensing or mapping lineage is unresolved.

A memory or throughput improvement cannot waive a semantic failure.

## First implementation order

Use the stock JDK implementation as oracle and rollback path.

1. Finish the complete java.util/java.util.concurrent symbol, view, serialization and reverse-consumer inventory.
2. Reconcile that inventory into the existing mapping authority without inventing new status semantics.
3. Evaluate already-flat/reuse baselines first, such as EnumSet, EnumMap, IdentityHashMap, ArrayDeque and BitSet, so the programme learns when not to rewrite.
4. Select bounded nonconcurrent mutable slices such as ArrayList or HashMap only after their full view/subclass/serialization contracts are enumerated.
5. Evaluate immutable factory families and Route A primitive/immutable owners, where structural sharing has the clearest ownership model.
6. Add Route B only after the selected semantic owner is stable and the compiler refusal corpus is present.
7. Attempt concurrent family replacements only after WP1/WP4/WP10 prerequisites in WORK_PACKETS.md close the JMM, GC and reclamation gaps.
8. Promote Route C only on an exact matched JDK image with the applicable acceptance matrix.
9. Advance synchronized mapping pins and evidence together; never borrow receipts from a prior candidate.
10. Repeat source-drift reconciliation before the next family.

## Worked failure rule

If a compact HashMap candidate reduces node count but its entrySet entry fails to write through with setValue, or adversarial equal-hash keys are lost, the candidate remains rejected. Record the memory result as evidence, fix the semantic defect, then retest the exact new candidate. The same rule applies to every collection family.

The canonical collection specification contains the detailed representation and contract examples used by these packets.
