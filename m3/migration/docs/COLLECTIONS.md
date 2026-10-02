# Whole-JDK M3 collections replacement specification

Status: documentation-only design contract. Target inspection pin: hsoliwal/M3jdk21@45f546ff5bcb06a1b2604f14baf998785d98d9a1. Synexia inspection pin: hsoliwal/com.synexia@3db24805d640c72ab1bd637d83561696d99561a0. No production collection implementation is changed or accepted by this document.

This document is the detailed collections work packet for the whole-JDK program. The program overview is ../../docs/whole-jdk-migration-scope.md. Operational migration state remains in ../../docs/name-mapping.json and the existing migration tooling; this file does not create another registry.

## 1. Design rule

The public Java collection contract is semantic. It does not require the current object shape, but an alternative physical representation is acceptable only when every observable contract that applies to that implementation is preserved.

M3 collection work therefore separates:

- semantic API shape: List, Set, Map, Queue, Deque, sorted/navigable/sequenced and concurrent contracts;
- Java object identity: each mutable public collection remains an independently observable object;
- element/key/value identity and equality: reference identity, equals/hashCode and comparator equivalence are different domains;
- physical storage: object-reference lanes, primitive lanes, bitmaps, open-address tables, indexed trees, CSR relations, segmented arrays and immutable snapshots;
- views and iteration: live views, snapshots, weakly consistent traversal, fail-fast best effort and encounter order;
- lifetime and publication: heap reachability, GC barriers, weak/soft references, native/mapped ownership and JMM ordering;
- adaptation: boxing, materialization, serialization, reflection, native and separately compiled boundaries.

Do not infer that a compact backend eliminates boxing at a generic API boundary. A List<Integer> still exposes Integer values. Route A can expose primitive APIs directly; Route B can remove boxing only where attribution proves the boxed object is not observably required; Route C can optimize internals but must preserve the ordinary Java signature.

## 2. Existing Synexia owners to evaluate before inventing new ones

The inspected Synexia tree already contains synexia-mat-collections and synexia-mindex collection families. The MAT collection architecture separates value, presence, order, group, membership and relation planes, and contains primitive vectors, adaptive sets/maps, range structures, posting indexes, CSR relations, bit vectors, record arenas and deterministic planners.

Relevant verified examples include:

- com.synexia.matcollections.MatAdaptiveIntVector
- MatAdaptiveIntSet and MatRoaringIntSet
- MatAdaptiveIntIntMap and MatAdaptiveLongLongMap
- MatIntList, MatLongList, MatIdList and MatIdIdMap
- MatIntPostingIndex, MatIntMultiMap and MatIdMultiMap
- MatIntRangeMap and MatIntRangeSet
- MatRankSelectBitVector and MatBitMatrix
- MatCsrGraph and MatAdaptiveIntGraph
- MatRecordArena and MatRecordSnapshot
- MatCollectionRequirements, MatCollectionFactory, MatCollectionPlan and MatCollectionPlanReceipt

These are candidate lower-level owners or references, not automatic replacements for java.util classes. Their published immutable/build-then-freeze model is directly compatible with some immutable or compiler-lowered workloads, but not automatically with arbitrary mutable Java collections.

Before adding another primitive collection owner, record why none of the existing Synexia owners has compatible semantics, lifetime, API and bootstrap dependencies.

## 3. Concrete JDK family matrix

The representation descriptions below are observations from the pinned JDK 21 source family and established implementation contracts. Candidate M3 forms are proposals to evaluate, not implementation claims.

| JDK family | Current representation / pressure point | Candidate M3 direction | Required preservation / likely disposition |
| --- | --- | --- | --- |
| ArrayList | Object-array storage plus logical size; growth leaves capacity slack | Object-reference lane with checked capacity policy; optional segmented/chunked backend; Route B primitive specialization for proven boxed-local workloads | RandomAccess behavior, nulls, growth/ensureCapacity/trim semantics, subList backing, iterator modCount behavior, clone/serialization. Replace backend only after differential evidence |
| LinkedList | Per-element doubly linked node objects | Indexed arena: Object reference lane plus primitive next/prev indexes and free/generation metadata | Exact List + Deque behavior, iterator insertion/removal, nulls, link/unlink exception timing, independent list identity, GC visibility. Candidate for object-count reduction; high alias/lifetime test burden |
| Vector / Stack | Growable Object array with legacy synchronization and inherited APIs | Compact reference lane only if synchronization, subclass hooks and serialization remain exact | Per-method synchronization and legacy Enumeration are observable. Retain pending proof |
| HashMap / HashSet | Bucket table with per-entry nodes and tree bins for heavy collisions; set delegates to map | Flat metadata lanes plus Object key/value lanes, or another collision-resilient compact table; null key/value lane explicit | equals/hashCode, mutable-key hazards, collision correctness, resize/OOM, views, Map.Entry mutation, subclass hooks used by linked variants, serialization and adversarial keys. No acceptance based only on average-case microbenchmarks |
| LinkedHashMap / LinkedHashSet | Hash storage plus per-entry encounter/access-order links | Flat hash metadata plus primitive order predecessor/successor lanes | Insertion/access order, removeEldestEntry, access-order mutations, sequenced/reversed views. Separate gate from HashMap |
| TreeMap / TreeSet | Per-entry red-black tree nodes | Object key/value lanes plus primitive left/right/parent/color indexes in an owner arena | Comparator semantics, null rules, range views, navigation, mutable-key hazards, entry mutation, serialization, clone. Indexed tree must preserve exact comparator-driven behavior |
| EnumMap | Dense value array indexed by enum ordinal plus key universe | Usually retain/adapt; already close to compact indexed representation | Null-value sentinel, enum type identity, weakly consistent views where applicable, serialization. Replace only if measured benefit exists |
| EnumSet | Bit-vector implementations for small/large enum universes | Reuse/retain; map to shared bit-plane primitives only if no regression | Already primitive/compact. Treat as baseline to beat, not a mandatory rewrite |
| IdentityHashMap | Flat alternating key/value object array with reference-identity semantics | Usually retain/adapt; optional metadata refinements only | Reference equality and identity hash domain are non-negotiable. Never intern equal keys/values or substitute equals |
| WeakHashMap | Weak-reference entry nodes, reference queue and hash buckets | Specialized GC-aware compact arena only with explicit reference-processing integration | Weak reachability, stale-entry expunging and GC timing sensitivity. Retain pending evidence is the default |
| Hashtable / Properties | Legacy synchronized hash table; Properties adds defaults/string conventions | Possible compact backend only behind exact legacy compatibility | Synchronization, subclassing, serialization, Enumeration, null rejection and historical behavior. High compatibility risk |
| immutable collection factories | Compact internal immutable list/set/map implementations | Compare against MAT immutable/pool/primitive owners; reuse existing JDK form when already competitive | Null rejection, iteration order, duplicate rejection and serialization proxy behavior. Sharing is allowed only under immutable ownership rules |
| Arrays.asList / Collections wrappers | Views/wrappers around caller or delegate storage | Keep wrappers or lower only when aliasing and optional-operation behavior are proven | Backing aliases, set-through behavior, unsupported operations, runtime class/reflection and synchronized/checked wrapper semantics |
| ArrayDeque | Circular Object array | Usually retain/adapt; primitive lowering only for explicit/lowered specializations | No null elements, head/tail wrap, growth, iterator behavior, sequenced/reversed operations. Existing form is already flat |
| PriorityQueue | Object-array binary heap plus comparator | Retain/adapt; primitive heap specialization for Route A/B where type proof allows | Heap ordering, comparator exceptions, null rejection, iterator not sorted, serialization |
| BitSet | long-word bitmap | Retain/reuse as oracle or integrate common bit-plane helpers where dependencies allow | Exact bit operations, growth and serialization. Existing representation is already primitive |
| ConcurrentHashMap | Node/table bins, forwarding during resize, tree bins, counters and carefully placed synchronization/CAS | Do not transplant a non-concurrent MAT map. Investigate compact node/index lanes only with a full JMM/GC/progress proof | Per-operation atomicity, compute/merge callback rules, weakly consistent traversal, resize help, memory visibility, tree bins, counter behavior and no null keys/values. Defer backend replacement until concurrency proof exists |
| ConcurrentSkipListMap / Set | Lock-free/mostly nonblocking ordered node/index structure | Indexed arena only if CAS/generation/reclamation and ordering proof is complete | Sorted navigation, weakly consistent iterators, progress properties, comparator semantics, GC safety. High-risk runtime work |
| CopyOnWriteArrayList / Set | Volatile immutable Object-array snapshot replaced on mutation | Likely retain; compare immutable-sharing opportunities without changing snapshot identity | Snapshot iterators, volatile publication, mutation copy cost and unsupported iterator mutation. Already expresses an immutable-snapshot model |
| ConcurrentLinkedQueue / Deque | Linked nodes with VarHandle/CAS protocols | Indexed arena is only a research candidate until lock-free reclamation/GC proof exists | Linearization, progress, self-link/unlink mechanics, GC reachability and weakly consistent traversal |
| ArrayBlockingQueue | Fixed Object ring plus lock/conditions | Retain/adapt first; primitive specialization on explicit route only | Bounded capacity, fairness option, interruptible blocking, condition signalling, iterator semantics |
| LinkedBlockingQueue / Deque | Linked nodes plus lock/condition coordination | Indexed node arena is a candidate after lock/lifetime proof | Capacity, interruption, signalling, split locks where applicable, iterator behavior |
| PriorityBlockingQueue | Growable heap plus concurrency control | Compact heap storage may reuse PriorityQueue ideas, but concurrency is a separate proof | Blocking/condition semantics, comparator, growth and weak consistency |
| DelayQueue | Priority queue of Delayed values plus lock/condition leader protocol | Retain/adapt until timing and leader semantics are proven | Delay ordering, timed waits, interruption, weak consistency |
| SynchronousQueue | Transfer rendezvous algorithms, no retained elements | No generic storage replacement; treat as synchronization algorithm family | Fair/nonfair modes, cancellation, interruption and progress. Disposition: retain/specialize only with evidence |
| LinkedTransferQueue | Linked dual queue/transfer nodes | High-risk algorithmic owner; not a normal list replacement | Transfer/tryTransfer semantics, progress, cancellation and GC unlinking |
| concurrent set views | Often projections over concurrent maps | Preserve backing and weak-consistency semantics | Do not materialize snapshots unless API explicitly requests one |

A disposition can be replace-backend, adapt, reuse, retain-pending-evidence, platform/runtime-specific, blocked or deferred. The matrix intentionally contains several retain/adapt candidates: whole-JDK scope is a review obligation, not a rewrite quota.

## 4. Shared interfaces and views

The implementation matrix is insufficient unless shared contracts are recorded per operation.

### Mutability and identity

- Mutable collections are never globally value-interned.
- Two equal mutable collections remain independently mutable objects.
- Unmodifiable is not the same as immutable: an unmodifiable view can reflect changes in its mutable backing collection.
- Immutable/frozen collections may structurally share immutable backing when ownership and lifetime are explicit.
- Copy-on-write snapshots may share until a mutation publishes a new snapshot, preserving the documented snapshot semantics.

### Equality, hashing and comparator domains

- List equality is ordered element equality.
- Set equality is membership equality and hash is the sum of element hashes.
- Map equality is entry equality and hash is the sum of entry hashes.
- Sorted containers may use comparator equivalence for ordering while equals can still differ from comparator equality; preserve existing warnings and behavior.
- IdentityHashMap uses reference identity and identity hash behavior. It is a separate semantic domain.
- Exact equality is never replaced by hash, similarity or precomputed signal acceptance.

### Nulls and duplicates

Null policies differ by implementation and operation. Preserve key, value and element rules, including callback null handling and methods where null means removal or absence. Duplicate behavior differs among List, Set, Map and multisets outside the JDK core; never normalize duplicates globally.

### Backed views

subList, keySet, values, entrySet, navigable range views, descending/reversed views and Java 21 sequenced views can be live aliases. A backend view needs:

- owner reference and generation/lifetime protection;
- logical-to-owner range/order mapping;
- propagation of allowed mutations in both directions;
- exact exception rules when the backing structure is structurally modified outside the permitted path;
- iterator and spliterator behavior derived from the view contract, not from a copied snapshot.

A view implementation may be compact metadata, but a tiny live view can retain a large owner. Measure retained memory separately from immediate allocation.

## 5. Iteration, spliterators and streams

Do not treat all iterator behavior as one rule.

- ordinary mutable implementations often provide best-effort fail-fast behavior using modification counts;
- concurrent collections often provide weakly consistent traversal;
- copy-on-write collections provide snapshot traversal;
- immutable collections have no concurrent structural mutation;
- order can be insertion, access, sorted, deque/sequenced, priority-heap internal order, or deliberately unspecified.

Preserve iterator remove/set/add support, exception timing, late-binding where applicable, Spliterator characteristics, encounter order, sizing/subsizing claims and parallel splitting behavior.

Stream pipelines and collectors are consumers of these semantics. A compact backend can supply specialized internal loops, but short-circuiting, side effects, stateful operations, parallel combiner/collector contracts and encounter order must remain exact. A parallel stream is not permission to mutate a non-thread-safe backend concurrently.

## 6. Boxing and primitive specialization

Three examples clarify the boundary.

### Compact generic map

A Route C HashMap candidate may store hashes and bucket/control metadata in int/byte lanes while retaining keys and values in Object reference lanes. That removes per-entry node objects without changing Map<K,V> signatures. It does not make Integer keys primitive at the public boundary.

A Route B lowering of a private Map<Integer,Integer> can use a MatAdaptiveIntIntMap only when attribution proves that:

- callers do not observe boxed key/value object identity;
- null is not required;
- the map does not escape to unlowered generic/reflection/native code without an adapter;
- mutation model and iteration/update semantics are supported;
- exception/callback/ordering behavior remains equivalent.

Otherwise the transform refuses or materializes through a boundary adapter.

### Immutable shared collection versus mutable collection

An immutable list of canonical immutable atom IDs can share its primitive ID lane or persistent tree nodes across values and processes if publication/lifetime rules permit. A mutable ArrayList with equal contents cannot be replaced by that same shared object. Mutation must remain private to that logical list unless the API explicitly describes a shared view.

### Backed view

A subList over an ArrayList-like M3 owner stores owner + offset + length + expected structural generation. set may write through when allowed; structural changes follow the JDK subList contract. Copying the elements into a new list would break backed-view behavior even if reads initially match.

## 7. Collision and identity counterexample

Consider two distinct key objects a and b with a.equals(b) true.

- HashMap treats them as the same logical key according to equals/hashCode.
- IdentityHashMap treats them as two distinct keys because a != b.

Interning a and b into one canonical object before inserting into IdentityHashMap destroys the second behavior. Therefore canonicalization is admissible only inside an identity domain whose public contract permits it.

For hash collisions, candidate metadata may use a stored hash to reject nonmatches, but equal stored hashes never prove key equality. Exact key comparison remains mandatory. Adversarial collisions must be part of the differential/performance suite.

## 8. Concurrent update example

For a ConcurrentHashMap-like putIfAbsent(k,v), the replacement must identify a linearization point at which one value becomes the mapping visible to all threads. Reads that observe the published mapping must also observe the required initialization of the key/value according to JMM publication rules.

A compact control-byte or index lane is not sufficient evidence. The design must state:

- which field/VarHandle/atomic access publishes the mapping;
- how resize/migration coordinates with readers and writers;
- how removed/reused slots avoid ABA or stale-generation aliasing;
- how GC discovers every live key/value reference;
- what progress guarantee applies under contention;
- how callbacks used by compute/merge avoid forbidden recursive updates or preserve documented behavior.

If that argument cannot be made for an operation, the candidate remains blocked regardless of single-threaded benchmark results.

## 9. Serialization, reflection and subclass compatibility

For every concrete class inventory:

- serialVersionUID and custom writeObject/readObject/writeReplace/readResolve behavior;
- serialized element/order/form compatibility and malformed-input checks;
- runtime class names and reflection-visible fields/methods where compatibility requires them;
- protected methods/fields and subclass hooks;
- package-private implementation hooks used by sibling JDK classes;
- clone behavior and copy constructors;
- module exports/opens and service dependencies.

An internal backend can be hidden, but the compatibility surface cannot silently change old serialized data, subclass behavior or binary linkage.

## 10. Measurement plan

Measure stock and candidate under the same JDK build mode, GC, heap, flags, hardware and workload.

Memory categories:

- object headers/alignment;
- references and compressed-oop mode;
- wrapper/boxing objects;
- per-entry node/link objects;
- backing arrays, control lanes and unused capacity;
- resize/rehash transient peaks;
- views/iterators/spliterators/wrappers;
- synchronization objects/cells;
- precomputation/index/cache metadata;
- retained heap from views/snapshots;
- native/mapped memory and file-backed pages;
- code/class metadata and JIT footprint.

Workload axes:

- empty, singleton, tiny, medium and large;
- dense/sparse and low/high load;
- primitive-like and arbitrary reference payloads;
- random, sequential and adversarial hashes;
- hit/miss, insert/remove/update/iterate mixes;
- ordered/navigable range operations;
- mutation-heavy and read-mostly;
- single-thread and mixed reader/writer contention;
- cold admission/build and warm steady state;
- long-lived retention and cache eviction.

Report throughput, CPU, latency distribution/tails, allocation rate, retained/peak memory, GC work, native/mapped memory and conversion/precomputation/cleanup. Include regressions and break-even points. Lower object count or allocation is not by itself a throughput claim.

## 11. Acceptance gates

A concrete collection replacement cannot advance until its applicable gates pass:

1. public API and binary surface inventory is complete for the target class/family;
2. differential operation tests cover normal, boundary, malformed and allocation-failure paths;
3. equality/hash/comparator/null/duplicate/order contracts match;
4. all live/backed/snapshot views and Map.Entry mutation paths match;
5. iterator/spliterator/stream characteristics and exception behavior match;
6. serialization, clone, reflection and subclass hooks match where applicable;
7. Route B transformations prove attribution, evaluation, escape and refusal behavior;
8. concurrent families pass JMM, linearization, progress, contention, cancellation and timeout stress;
9. GC/lifetime/weak-reference/off-heap ownership is proven;
10. exact candidate builds in the required JDK configurations/platforms;
11. performance and memory results include admission/conversion/precompute cost and show any regressions;
12. source-to-target mapping, provenance and exact evidence are updated together.

A failed semantic gate blocks promotion even if memory or CPU improves.

## 12. Work order

Recommended dependency order:

1. inventory and contract tables for all collection/concurrency classes;
2. immutable/primitive Route A owners and explicit adapters using existing MAT structures;
3. nonconcurrent flat mutable vertical slices where compatibility is tractable;
4. backed/sequenced/sorted views and wrappers;
5. attributed Route B lowering with fail-closed boundaries;
6. custom-JDK Route C for selected nonconcurrent families;
7. concurrent families only after JMM/GC/runtime primitives are ready;
8. streams/collectors and wider java.base consumers;
9. full-module/platform acceptance and sustained upstream replay.

Keep the stock implementation as oracle and rollback path until each bounded replacement closes its gates.
