# Whole-JDK M3 collections replacement specification

Status: **proposed documentation-only design**. No JDK collection implementation is changed by this document. Target source observations are pinned to `hsoliwal/M3jdk21@45f546ff5bcb06a1b2604f14baf998785d98d9a1`; Synexia owner observations are pinned to `hsoliwal/com.synexia@3db24805d640c72ab1bd637d83561696d99561a0`.

This document expands the collection workstream in [whole-jdk-migration-scope.md](whole-jdk-migration-scope.md). It is not a new mapping authority. Any implementation must be represented in the existing stable mapping system and pass the route-specific gates.

## Canonical role and execution overlay

This is the canonical detailed collection design for the whole-JDK documentation set. The implementation-oriented companion [../migration/docs/COLLECTIONS.md](../migration/docs/COLLECTIONS.md) deliberately does not repeat the family matrix; it turns this specification into bounded mapping, recipe and evidence packets. [whole-jdk-subsystem-matrix.md](whole-jdk-subsystem-matrix.md) owns module-level planning dispositions, while operational capability state remains in [name-mapping.json](name-mapping.json).

Do not create a second collection design by copying these tables into migration status documents. Extend this document for collection contract/design changes and update the execution overlay only when migration procedure or gates change.

## 1. Design rule

The Java collection contract is the semantic skeleton. Storage is replaceable only when all observable behavior remains valid for the exact class, operation and route.

A candidate M3 collection implementation therefore has four separately reviewed parts:

1. **Java compatibility object** — the ordinary JDK class/object with its required identity, inheritance, protected hooks, serialization shape and observable aliases.
2. **storage owner** — primitive/reference lanes, segmented arena, bitmap, packed table, tree, heap, ring or persistent node family.
3. **projection boundary** — object references, iterators, entries, arrays, serialization records and native/tooling views materialized when the public contract requires them.
4. **evidence owner** — differential tests, API/ABI snapshots, concurrency/JMM argument, benchmarks and exact candidate receipts.

Do not assume that a generic `Collection<E>` can store only primitive IDs. If the JDK object must keep arbitrary live object references, GC reachability and object identity remain part of storage semantics. ID indirection is valid only when a stable owner retains those references and the extra lookup is justified.

## 2. Existing Synexia owners to reuse or mine before creating another engine

The following are observed existing owners/reference implementations, not automatically bootstrap-safe JDK dependencies:

| Existing owner | Relevant capability | JDK migration use |
| --- | --- | --- |
| `synexia-mat-collections` | primitive lists, adaptive vector/set/map, CSR multimap, ring buffer, row/group/order indexes, record arenas and generation handles | reference designs for primitive/internal lanes and deterministic adaptive layout |
| `synexia-mindex/collections` | primitive-ID list/set/map/deque/priority/table/graph/tuple, canonical immutable collections, identity/value spaces, Java views | reference for explicit identity domains, frozen values and projection boundaries |
| `synexia-common:com.synexia.common.collections` | large packed/segmented family, including packed hash/tree/list/deque/weak/transfer/concurrent structures and selection/catalogue machinery | source of already-owned algorithms/storage shapes to audit and selectively port |
| `m3-java-contracts` | Java API/ABI and other contract snapshots; deterministic diff/receipt model | transformation/evidence gate, not runtime dependency |
| `synexia-openrewrite-recipes` | reusable recipe crates | tooling route only; never early `java.base` dependency |

A JDK implementation must not simply depend on these Maven modules. First isolate a minimal bootstrap-safe algorithm/storage contract; then source-pin, license-review, adapt and verify any selected implementation through a separately authorized work packet.

## 3. Shared compatibility checklist

Every concrete implementation below must specify and test:

- mutability, optional operations and independent Java object identity;
- element/key/value null policy and exact exception type/timing;
- equality/hash/comparator/reference-identity domain;
- duplicate and collision semantics;
- insertion, access, sorted, priority, sequenced or deliberately unspecified order;
- live backed views versus snapshots, including reversed/descending/range/subList views;
- iterator mutation and fail-fast best effort, snapshot, or weak-consistency semantics;
- spliterator flags, encounter order, estimated size, splitting and stream behavior;
- callbacks/reentrancy for `compute*`, `merge`, `replaceAll`, bulk removal and traversal;
- capacity, growth, integer overflow, OOME and partial-operation behavior;
- protected/subclass-visible state and hooks;
- reflection, generic signatures, bridges, binary linkage and runtime class identity;
- serialization form, UIDs/proxies, old-stream compatibility and deserialization validation;
- GC reachability, weak/soft/reference queues and owner lifetime;
- for concurrent classes: happens-before, publication, operation atomicity/linearization, progress/fairness, interruption/timeout and cancellation.

Hash/signature/signal equality is never sufficient where the Java contract requires exact equality. Mutable values are never globally canonicalized merely because they are currently equal.

## 4. Concrete sequential families

The “candidate” column describes a design to evaluate, not an implementation claim.

| JDK implementation / observed shape | Candidate M3 representation | Contract/adaptation boundary | Complexity/tradeoff and first acceptance gates |
| --- | --- | --- | --- |
| `ArrayList`: contiguous `Object[]` + logical size | segmented or flat reference lane with checked growth; optional primitive-specialized internal lane only behind proven typed boundaries | preserve random access, nulls, capacity behavior, `subList` backing, modCount/fail-fast, `toArray`, clone/serialization | keep O(1) indexed access and amortized append; segmentation may reduce resize copies but adds address arithmetic. Differential ranges, growth/OOME, iterator/subList aliasing |
| `LinkedList`: per-element doubly linked `Node`, first/last, size | slot arena with parallel element/prev/next lanes and generation-stable slots; Java object refs remain GC-visible | preserve Deque/List order, ListIterator bidirectional mutation, null support, node-independent external identity, serialization | removes per-node object headers but adds slot management. O(1) end insertion/removal and iterator-local unlink; indexed access remains O(n). GC/barrier and iterator mutation gates |
| `Vector` / `Stack`: protected `Object[] elementData` and synchronized legacy API | retain Java object/field compatibility initially; evaluate packed backing only if protected-field/subclass compatibility can be preserved | protected fields, synchronization, Enumeration/Iterator, clone and serialization are hard constraints | likely **adapt/retain** before replacement. Subclass/reflection/serialization corpus required |
| `HashMap`: `Node[] table`, per-entry nodes, tree bins, size/modCount | compact slot table: parallel hash/key/value/next-or-probe lanes; candidate open addressing or slot-indexed chaining; tree fallback only if worst-case contract/security requires it | preserve null key/value, key equality/hash, entry mutability, views, callback semantics, subclass hooks used by linked variants, serialization | expected O(1) lookup remains target, but collision/adversarial behavior and resize peaks must be measured. Exact key equality after hashes. Differential collision/treeification/compute/merge/view tests |
| `HashSet`: `HashMap<E,Object>` owner | either preserve map delegation or use the same compact key slot engine with set facade | Set equality/hash, null, iterator/remove, clone/serialization | no duplicate map engine unless measured/contract reason. Cross-check HashMap gates |
| `LinkedHashMap` / `LinkedHashSet`: hash nodes plus head/tail encounter links; access-order option in map | compact hash slots plus primitive prev/next order lanes; stable slot IDs across resize | insertion/access order, sequenced first/last/reversed views, `removeEldestEntry`, iterator order, callbacks | expected O(1) lookup/order updates. Slot relocation must not break order. Access-order reentrancy and subclass-hook tests |
| `TreeMap` / `TreeSet`: comparator + red/black Entry tree | compact index tree using parallel key/value/left/right/parent/color lanes, or retain nodes if object-reference traffic dominates | comparator semantics, null behavior dictated by comparator/natural order, live range/descending views, Navigable entries, entry mutation, serialization | O(log n) operations and comparator invocation behavior must stay compatible. Rotation correctness, range fences, mutable-key/comparator cases |
| `IdentityHashMap`: flat alternating `Object[] table` | retain reference slots or compact parallel reference lanes; primitive metadata may change, **never value-ID canonicalization** | `==` key/value comparison and identity hash behavior are semantic | open-addressing layout is already compact. Treat as likely reuse/adapt; prove any change under identity-collision and iterator tests |
| `WeakHashMap`: weak-reference entries + ReferenceQueue + hash table | slot arena only if each key's weak reachability/reference-queue protocol remains exact; otherwise retain | GC reachability and stale-entry expunging are primary semantics; values may strongly refer to keys | memory reduction cannot weaken GC behavior. Dedicated GC/ReferenceQueue tests required; likely retain pending evidence |
| `EnumMap`: enum universe + value array | reuse/direct-address design; consider metadata tightening only | natural declaration-order iteration, null-value sentinel, enum type identity, clone/serialization | already near ideal direct addressing. Measure before changing |
| `EnumSet`: one `long` for small universes, `long[]` for jumbo | reuse | enum universe/type, bit semantics, clone/serialization | already primitive bitset; default disposition reuse |
| `ArrayDeque`: circular `Object[]`, head/tail | segmented or flat reference ring with checked growth; primitive ring for proven internal primitive route | forbids null, deque/sequenced order, reversed view, iterator behavior, array projections | O(1) ends; segmentation trades resize copy for branch/address cost. Wraparound, growth, removal and reversed-view tests |
| `PriorityQueue`: binary heap `Object[]` + comparator | slot/reference heap; primitive priority/ID lanes only where value projection semantics are explicit | comparator/natural order, null rejection, iterator's unspecified order, serialization, removal | O(log n) add/remove and O(1) peek target. Comparator count/timing may be observable through side effects; differential comparator-exception tests |
| `ImmutableCollections` and `List.of/Set.of/Map.of` families | compact immutable lanes, canonical empty/small shapes, optional structural sharing for derived immutable data | null rejection, iteration/order guarantees, duplicate rejection, object identity non-guarantees only where spec permits, serialization proxy | strongest candidate for shared immutable storage, but no unsupported global interning promise. Factory/serialization/null/duplicate gates |
| unmodifiable/synchronized/checked wrappers and `Collections` views | preserve wrapper semantics; optimize delegate storage, not wrapper identity, unless exact wrapper behavior is retained | live delegation, mutex ownership, runtime type checks, RandomAccess markers, serialization | wrapper removal can change identity/class/synchronization. Treat wrappers as compatibility objects |
| `Arrays.asList` and fixed-size array views | retain array-backed live view or equivalent exact alias | `set` writes through; structural add/remove unsupported; array lifetime | no copy unless explicitly allowed; exact alias tests |
| `BitSet` | reuse primitive word lanes; evaluate indexes only for workloads that justify them | logical length, trailing-zero semantics, serialization | already packed; additions should be derived indexes, not wholesale replacement |
| `Properties` / `Hashtable` | initially retain/adapt | synchronization, legacy Enumeration, defaults chain, text/XML persistence and serialization | legacy compatibility and subclass behavior dominate; migration only after dedicated surface inventory |

## 5. Concurrent collection families

Concurrency is not obtained by wrapping a sequential packed structure in a single lock unless the original contract permits that progress/ordering profile and measurements justify it.

| JDK implementation / observed shape | Candidate M3 representation | Required semantic proof |
| --- | --- | --- |
| `ConcurrentHashMap`: volatile node table, CAS bin access, forwarding nodes during resize, tree bins | compact stable-slot table with explicit state/control lanes only if lock-free/locking transitions, resize forwarding, GC barriers and per-key atomic operations are proven; otherwise retain current concurrency engine and optimize leaf metadata selectively | linearization for get/put/remove/replace/compute/merge, visibility, resize cooperation, weakly consistent iterators, null rejection, recursive-update behavior, bulk operations |
| `ConcurrentLinkedQueue`: volatile linked nodes/head/tail | indexed slot arena only with safe reclamation and no ABA; otherwise retain nodes | lock-free progress, happens-before producer→consumer, weak iterator, logical/physical deletion |
| `ConcurrentLinkedDeque`: volatile prev/next nodes/head/tail | same: generation-stamped stable slots are a research candidate, not a drop-in replacement | bidirectional unlink/helping, ABA/reclamation, iterator and end-operation linearization |
| `ConcurrentSkipListMap/Set`: ordered base nodes + index tower | compact indexed skip structure only if comparator/order/concurrent traversal semantics survive; otherwise selective metadata packing | expected logarithmic operations, weakly consistent traversal/range views, concurrent insertion/removal and comparator side effects |
| `CopyOnWriteArrayList/Set`: volatile immutable `Object[]` snapshot | immutable chunk/vector snapshot with structural sharing if snapshot iteration and publication remain exact | snapshot iterator semantics, volatile publication, array snapshot APIs, mutation copy cost, serialization. Structural sharing must not expose later writes |
| `ArrayBlockingQueue`: fixed `Object[]` ring + one `ReentrantLock` + notEmpty/notFull | packed/reference ring can reuse the existing synchronization policy | bounded capacity, FIFO, optional fairness, blocking/interrupt/timeout conditions, iterator weak consistency |
| `LinkedBlockingQueue`: linked nodes + atomic count + separate put/take locks | segmented slot queue candidate, retaining dual-lock/count protocol or a separately proven equivalent | capacity, put/take overlap, signaling, atomic count, interruption, iterator removal and GC unlinking |
| `LinkedBlockingDeque`: linked nodes + one lock/conditions | slot arena candidate under same lock, preserving bidirectional semantics | blocking ends, capacity, interrupt/timeout, iterator mutation |
| `PriorityBlockingQueue`: heap array + lock/condition | compact heap/reference lane under same synchronization first | unbounded growth, comparator behavior, blocking take, weak iterator |
| `DelayQueue`: priority queue under lock with leader/follower waiting | retain scheduling/wait protocol; storage optimization only beneath it | delay expiration ordering, leader handoff, timing/interrupt semantics |
| `SynchronousQueue`: transfer stack/queue nodes, fair/nonfair transferers | retain pending evidence | zero-capacity rendezvous, fairness mode, cancellation/timeout, lock-free matching are the data structure |
| `LinkedTransferQueue`: dual data/request volatile node chain | retain pending evidence or generation-stamped slot research only after formal proof | transfer/tryTransfer semantics, matching, cancellation, helping and lock-free progress |
| concurrent wrappers/sets derived from maps | use owning concurrent map decision | preserve view semantics; no separate incompatible engine |

For any indexed-slot lock-free design, the mapping must define slot generation, safe reclamation and the exact point at which a stale handle becomes impossible to observe. Epoch/hazard/GC-managed strategies cannot be mixed implicitly.

## 6. Primitive specialization and boxing boundary

Primitive internal storage can eliminate boxing only before a generic boundary. It cannot make `List<Integer>.get()` return an `int`; the public descriptor returns `Integer`.

Classify each call site:

- **primitive-native**: internal JDK or explicit M3 API consumes primitive lanes end-to-end;
- **generic projection**: materialize/cache a wrapper as required by the Java API;
- **identity-sensitive generic use**: wrapper object/reference identity is observable; do not substitute value identity;
- **nullable generic use**: primitive lane needs an explicit presence/null representation;
- **numeric-semantic use**: preserve wrapper equality/hash, including floating NaN and signed-zero behavior.

Compiler Route B may remove boxing only when attribution/escape/identity analysis proves the wrappers are unobservable and exception/evaluation order remains unchanged. Otherwise it refuses the transformation.

## 7. Backed views and iterators

A compact owner does not license snapshotting a live view.

A view descriptor must retain:
- exact owner identity and generation/lifetime;
- range/key-set/value-set/descending/reversed projection;
- mutation permissions and route back into the owner;
- modCount/version semantics if fail-fast applies;
- comparator/order context;
- expected iterator consistency model.

Examples:
- `ArrayList.subList` mutations affect the root and vice versa within contract constraints.
- `Map.keySet/values/entrySet` are backed views; iterator removal changes the map.
- Navigable submaps/descending maps carry live range/order constraints.
- Copy-on-write iterators are snapshots and do not become live merely because a chunked backend shares nodes.
- Concurrent weakly consistent views do not become atomic snapshots.

## 8. Serialization, reflection and subclassing

For each changed class record:

1. current `serialVersionUID` and custom `writeObject/readObject/readResolve/writeReplace` behavior;
2. serialized element order and validation;
3. protected fields/methods and subclass hooks;
4. implementation class names reachable through factories/views;
5. reflective field/layout consumers inside and outside the JDK;
6. VM/intrinsic assumptions, if any.

A smaller in-memory layout that cannot read old streams or changes required subclass behavior is not API-preserving Route C. Such a change needs an explicitly versioned compatibility/migration decision, not a benchmark exception.

## 9. Allocation and performance acceptance

Measure stock and candidate separately for:

- empty, singleton, tiny, medium and very large sizes;
- occupancy/load factors and resize boundaries;
- primitive-like versus arbitrary reference payloads;
- duplicate-heavy, sparse/dense and ordered workloads;
- benign and adversarial hash collisions;
- mutation-heavy, iteration-heavy and mixed workloads;
- view/iterator churn;
- cold creation/admission and warm steady state;
- retained backing after tiny views/slices;
- single-thread and contended concurrent cases.

Report object count, allocated bytes, retained heap, native/mapped memory, peak during resize/copy, GC work, CPU, throughput and latency distribution. Include metadata/index/precompute memory and cleanup. A candidate promotes only if required semantic gates pass; performance determines whether to enable it, never whether failed semantics may be waived.

## 10. Per-family acceptance packet template

Every concrete collection work packet must contain:

```text
capability_id
target_pin
source_owner_pin
jdk_class_symbols
current_representation_observation
candidate_owner_and_layout
public/protected/binary/serialization_contracts
identity_null_order_duplicate_rules
views_iterators_spliterators
concurrency_and_lifetime_rules
route_A_boundaries
route_B_preconditions_and_refusals
route_C_bootstrap_vm_native_consumers
recipe_or_native_transform_identity
differential_tests
stress_gc_serialization_tests
measurement_matrix
rollback_and_default_off_plan
open_blockers
evidence_state
```

Promotion states remain separate: discovered → contract-reviewed → proposed → implemented → verified → accepted. “Implemented” is not “verified”; “verified” for Route A is not Route C acceptance.
