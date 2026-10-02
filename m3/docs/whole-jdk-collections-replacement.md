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
- element/key/value null policy and exact exception type/timing where specified;
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

Hash/signature/signal equality is never sufficient where the Java contract requires exact equality. Mutable values are never globally canonicalized merely because they are currently equal. Keep normative API promises, stricter pinned-baseline compatibility requirements and explicitly unspecified behavior as separate evidence categories. In particular, best-effort fail-fast behavior is not a synchronization guarantee, and unspecified callback counts are not universal API promises.

## 4. Concrete sequential families

The “candidate” column describes a design to evaluate, not an implementation claim. Selected pinned source landmarks are [ArrayList's reference array and size](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/src/java.base/share/classes/java/util/ArrayList.java#L109-L146), [HashMap's node and entry mutation](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/src/java.base/share/classes/java/util/HashMap.java#L277-L315), and [Properties' independent map and defaults](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/src/java.base/share/classes/java/util/Properties.java#L143-L170).

| JDK implementation / observed shape | Candidate M3 representation | Contract/adaptation boundary | Complexity/tradeoff and first acceptance gates |
| --- | --- | --- | --- |
| `ArrayList`: contiguous `Object[]` + logical size | segmented or flat reference lane with checked growth; optional primitive-specialized internal lane only behind proven typed boundaries | preserve random access, nulls, capacity behavior, `subList` backing, modCount/fail-fast, `toArray`, clone/serialization | preserve constant-time indexed access and amortized append for this class; an arbitrary tree-backed vector does not automatically meet that target. Segmentation may reduce resize copies but adds addressing/directory cost. Differential ranges, growth/OOME, iterator/subList aliasing |
| `LinkedList`: per-element doubly linked `Node`, first/last, size | slot arena with parallel element/prev/next lanes and generation-stable slots; Java object refs remain GC-visible | preserve Deque/List order, ListIterator bidirectional mutation, null support, node-independent external identity, serialization | can reduce per-node object headers but adds slot management. Constant-time end insertion/removal and iterator-local unlink; positional lookup remains linear without an additional maintained index. GC/barrier and iterator mutation gates |
| `Vector` / `Stack`: protected `Object[] elementData` and synchronized legacy API | retain Java object/field compatibility initially; evaluate packed backing only if protected-field/subclass compatibility can be preserved | protected fields, synchronization, Enumeration/Iterator, clone and serialization are hard constraints | likely **adapt/retain** before replacement. Subclass/reflection/serialization corpus required |
| `HashMap`: `Node[] table`, per-entry nodes, tree bins, size/modCount | compact slot table: parallel hash/key/value/next-or-probe lanes; candidate open addressing or slot-indexed chaining; collision fallback reviewed against the baseline and security requirements | preserve null key/value, key equality/hash, entry mutability, views, callback semantics, subclass hooks used by linked variants, serialization | expected constant-time lookup requires stated hash/load assumptions; analyze collision/adversarial behavior, traversal and resize peaks separately. Tree bins do not by themselves prove a universal bound for arbitrary keys. Exact key equality after hashes; differential collision/treeification/compute/merge/view tests |
| `HashSet`: `HashMap<E,Object>` owner | either preserve map delegation or use the same compact key slot engine with set facade | Set equality/hash, null, iterator/remove, clone/serialization | no duplicate map engine unless measured/contract reason. Cross-check HashMap gates |
| `LinkedHashMap` / `LinkedHashSet`: hash nodes plus head/tail encounter links; access-order option in map | compact hash slots plus primitive prev/next order lanes; stable slot IDs across resize | insertion/access order, sequenced first/last/reversed views, `removeEldestEntry`, iterator order, callbacks | expected constant-time lookup/order updates under stated assumptions. Slot relocation must not break order. Access-order reentrancy and subclass-hook tests |
| `TreeMap` / `TreeSet`: comparator + red/black Entry tree | compact index tree using parallel key/value/left/right/parent/color lanes, or retain nodes if object-reference traffic dominates | comparator semantics, null behavior dictated by comparator/natural order, live range/descending views, snapshot navigation entries versus connected entry-set entries, serialization | logarithmic structural search/update for the balanced tree, plus comparator cost. Preserve specified comparator/exception behavior; do not assume all methods are logarithmic or all entries mutable. Rotation, range fence and mutable-key/comparator gates |
| `IdentityHashMap`: flat alternating `Object[] table` | retain reference slots or compact parallel reference lanes; primitive metadata may change, **never value-ID canonicalization** | `==` key/value comparison and identity hash behavior are semantic | open-addressing layout is already compact. Treat as likely reuse/adapt; prove any change under identity-collision and iterator tests |
| `WeakHashMap`: weak-reference entries + ReferenceQueue + hash table | slot arena only if each key's weak reachability/reference-queue protocol remains exact; otherwise retain | GC reachability and stale-entry expunging are primary semantics; values may strongly refer to keys | memory reduction cannot defeat weak reachability through an auxiliary strong key index. Dedicated GC/ReferenceQueue tests required; likely retain pending evidence |
| `EnumMap`: enum universe + value array | reuse/direct-address design; consider metadata tightening only | natural declaration-order iteration, null-value sentinel, enum type identity, clone/serialization | already near ideal direct addressing. Measure before changing |
| `EnumSet`: one `long` for small universes, `long[]` for jumbo | reuse | enum universe/type, bit semantics, clone/serialization | already primitive bitset; default disposition reuse |
| `ArrayDeque`: circular `Object[]`, head/tail | segmented or flat reference ring with checked growth; primitive ring for proven internal primitive route | forbids null, deque/sequenced order, reversed view, iterator behavior, array projections | amortized constant-time end operations, not worst-case constant-time growth. Searches/removal by value and other scanning operations are separate. Segmentation trades resize copy for addressing cost; wraparound/growth/removal/reversed-view gates |
| `PriorityQueue`: binary heap `Object[]` + comparator | slot/reference heap; primitive priority/ID lanes only where value projection semantics are explicit | comparator/natural order, null rejection, iterator's unspecified order, serialization, removal | distinguish logarithmic offer/add/poll/no-arg remove from linear remove(Object)/contains, and constant-time peek. Include comparator cost, growth and exception paths; do not label every removal logarithmic |
| `ImmutableCollections` and `List.of/Set.of/Map.of` families | compact unmodifiable lanes, canonical empty/small shapes, optional structural sharing for derived immutable data | factory-specific null/order/duplicate rules: List.of retains duplicates, Set.of rejects them, Set.copyOf collapses equal duplicates, Map.of rejects duplicate keys; identity non-guarantees only where specified; serialization proxy | immutable container shape does not imply immutable elements or a permanently valid cached hash. Compare already-compact JDK baselines; no unsupported global interning or cross-process-sharing promise |
| unmodifiable/synchronized/checked wrappers and `Collections` views | preserve wrapper semantics; optimize delegate storage, not wrapper identity, unless exact wrapper behavior is retained | live delegation, mutex ownership, runtime type checks, RandomAccess markers, serialization | wrapper removal can change identity/class/synchronization. Treat wrappers as compatibility objects |
| `Arrays.asList` and fixed-size array views | retain array-backed live view or equivalent exact alias | `set` writes through; unsupported structural operations remain unsupported; array lifetime | no copy unless explicitly allowed; exact alias and optional-operation tests |
| `BitSet` | reuse primitive word lanes; evaluate indexes only for workloads that justify them | logical length, trailing-zero semantics, serialization | already packed; additions should be derived indexes, not wholesale replacement |
| `Hashtable`: synchronized legacy hash table | initially retain/adapt actual Hashtable storage | synchronization, legacy Enumeration, null rejection, serialization and subclass hooks | legacy compatibility dominates; migration only after dedicated surface inventory |
| `Properties`: Hashtable subclass with its own transient volatile ConcurrentHashMap and separate protected defaults chain | retain/adapt its actual overrides, entry owner, defaults and persistence together | simple reads omit inherited-table synchronization; writes/bulk operations remain synchronized in the pinned source. Preserve Object APIs versus String property APIs, defaults, load/store/XML and serialization | inheritance does not identify storage ownership. A Hashtable node recipe cannot claim Properties coverage; require its own operation/publication/defaults and persistence tests |

## 5. Concurrent collection families

Concurrency is not obtained by wrapping a sequential packed structure in a single lock unless the original contract permits that progress/ordering profile and measurements justify it.

| JDK implementation / observed shape | Candidate M3 representation | Required semantic proof |
| --- | --- | --- |
| `ConcurrentHashMap`: volatile node table, CAS bin access, forwarding nodes during resize, tree bins | compact stable-slot table with explicit state/control lanes only if locking/nonblocking transitions, resize forwarding, GC barriers and per-key atomic operations are proven; otherwise retain current concurrency engine and optimize leaf metadata selectively | operation-specific linearization and publication, resize cooperation, weakly consistent iterators, null rejection, recursive-update behavior and bulk operations; no whole-map snapshot inferred from per-key guarantees |
| `ConcurrentLinkedQueue`: volatile linked nodes/head/tail | indexed slot arena only with safe reclamation and no ABA; otherwise retain nodes | applicable nonblocking progress, happens-before producer→consumer, weak iterator, logical/physical deletion |
| `ConcurrentLinkedDeque`: volatile prev/next nodes/head/tail | same: generation-stamped stable slots are a research candidate, not a drop-in replacement | bidirectional unlink/helping, ABA/reclamation, iterator and end-operation linearization |
| `ConcurrentSkipListMap/Set`: ordered base nodes + index tower | compact indexed skip structure only if comparator/order/concurrent traversal semantics survive; otherwise selective metadata packing | expected logarithmic operations, weakly consistent traversal/range views, concurrent insertion/removal and comparator side effects; progress must be stated per operation, not inferred for arbitrary callbacks |
| `CopyOnWriteArrayList/Set`: volatile immutable `Object[]` snapshot | immutable chunk/vector snapshot with structural sharing if snapshot iteration and publication remain exact | snapshot iterator semantics, volatile publication, array snapshot APIs, mutation copy cost, serialization. Structural sharing must not expose later writes |
| `ArrayBlockingQueue`: fixed `Object[]` ring + one `ReentrantLock` + notEmpty/notFull | packed/reference ring can reuse the existing synchronization policy | bounded capacity, FIFO, optional fairness, blocking/interrupt/timeout conditions, iterator weak consistency |
| `LinkedBlockingQueue`: linked nodes + atomic count + separate put/take locks | segmented slot queue candidate, retaining dual-lock/count protocol or a separately proven equivalent | capacity, put/take overlap, signaling, atomic count, interruption, iterator removal and GC unlinking |
| `LinkedBlockingDeque`: linked nodes + one lock/conditions | slot arena candidate under same lock, preserving bidirectional semantics | blocking ends, capacity, interrupt/timeout, iterator mutation |
| `PriorityBlockingQueue`: heap array + lock/condition | compact heap/reference lane under same synchronization first | unbounded growth, comparator behavior, blocking take, weak iterator |
| `DelayQueue`: priority queue under lock with leader/follower waiting | retain scheduling/wait protocol; storage optimization only beneath it | delay expiration ordering, leader handoff, timing/interrupt semantics |
| `SynchronousQueue`: transfer stack/queue nodes, fair/nonfair transferers | retain pending evidence | zero-capacity rendezvous, fairness mode, cancellation/timeout and matching/wait protocols; a nonblocking matching step does not make blocking transfer methods nonblocking |
| `LinkedTransferQueue`: dual data/request volatile node chain | retain pending evidence or generation-stamped slot research only after formal proof | transfer/tryTransfer semantics, matching, cancellation, helping and applicable operation-specific progress |
| concurrent wrappers/sets derived from maps | use owning concurrent map decision | preserve view semantics; no separate incompatible engine |

For any indexed-slot lock-free design, the mapping must define slot generation, safe reclamation and the exact point at which a stale handle becomes impossible to observe. Epoch/hazard/GC-managed strategies cannot be mixed implicitly. A successful per-key operation takes effect at its appropriate point between invocation and response; later removal or replacement remains possible. Retrievals reporting a published mapping must satisfy the applicable happens-before requirements. See [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) and [JLS 17](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html).

## 6. Primitive specialization and boxing boundary

Primitive-only execution is possible where an explicit API or proven lowering keeps wrappers unobservable. It does not change generic Java binary descriptors. At source level, `List<Integer>.get(int)` has an Integer result; the erased interface method is `java.util.List.get:(I)Ljava/lang/Object;`, not an Integer- or int-returning JVM descriptor. Callers may apply casts and unboxing according to context. Preserve erasure, bridge methods, casts, null/unboxing exceptions and separately compiled callers. See [JLS 4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.6).

Classify each call site:

- **primitive-native**: internal JDK or explicit M3 API consumes primitive lanes end-to-end;
- **generic projection**: return the required object reference or create a value wrapper only where its identity and lifetime permit it;
- **identity-sensitive generic use**: wrapper object/reference identity is observable; do not substitute value identity;
- **nullable generic use**: primitive lane needs an explicit presence/null representation;
- **numeric-semantic use**: preserve wrapper equality/hash, including floating NaN and signed-zero behavior.

Compiler Route B may remove boxing only when attribution/escape/identity analysis proves the wrappers are unobservable and exception/evaluation order remains unchanged. Otherwise it refuses the transformation. Reboxing at a later adapter cannot restore an observable reference identity that was discarded earlier. Shared canonical String payloads likewise do not permit replacing a caller-supplied String key/value object when its reference identity must remain observable.

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
- `Map.keySet/values/entrySet` are backed views; iterator removal changes the map where supported.
- Navigable submaps/descending maps carry live range/order constraints.
- Copy-on-write iterators are snapshots and do not become live merely because a chunked backend shares nodes.
- Concurrent weakly consistent views do not become atomic snapshots.

[ArrayList.subList](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html#subList(int,int)) has undefined semantics following a structural backing-list modification made other than through the returned view. Do not invent a universal guaranteed ConcurrentModificationException or stable-view result for that case. Preserve specified behavior and separately record any stricter retained-baseline requirement.

Entry lifetimes also differ: [TreeMap's navigation entry methods](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html) return snapshots that do not support setValue, whereas its entry-set traversal has a different connection/mutation contract. Review the exact method instead of sharing a universal mutable or detached entry implementation. [Map.Entry](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.Entry.html) further limits connected-entry behavior after unsupported backing changes.

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

Every concrete collection work packet must contain the following information. This is a documentation template, not permission to add unvalidated fields or statuses to the operational registry schema.

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

Track discovery, contract review, proposal, source porting, target retention, test execution and acceptance independently. A source can be inspected but not ported, or a port retained but untested. Evidence for Route A does not automatically establish Route B or Route C acceptance; a changed candidate does not inherit a historical pass.

## 11. Worked contract discriminators

These are **proposed test cases and design examples, not executed tests or newly implemented APIs**. Contributors should implement them through the existing recipe/test owners and bind results to exact baseline and candidate revisions. Keep normative contract assertions separate from unspecified-behavior probes and stricter source-preservation requirements.

### 11.1 Supplied object identity and a compact generic map

Supply an Integer object as a HashMap<String,Integer> value, retrieve it and compare the result with the supplied reference using `==`. A primitive-only backend that returns a newly constructed equal wrapper is not a sufficient identity-preserving adaptation. Do not base the proof on a guessed Integer cache range. The pinned HashMap Node stores key/value references; preserving the final key representative on equal-key replacement is also a source-level compatibility question for recipes.

A compact generic map may retain Object key/value lanes and use primitive hash/bucket/link/control lanes. This targets metadata/node overhead without asserting that generic object boundaries disappear. Do not conflate ordinary equality-based lookup with reference identity: [IdentityHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IdentityHashMap.html) compares both keys and values by reference identity. Two distinct equal keys must remain distinct there.

### 11.2 Overloads, null and absence survive lowering

For a List<Integer> holding 10, 20, 1, `remove(1)` removes the element at index 1, while `remove(Integer.valueOf(1))` removes the equal value 1. An int-vector lowering must preserve the already-resolved overload, not re-resolve a changed primitive expression against a new API.

For maps, distinguish absent, present-with-null where allowed, and present-with-zero. A primitive zero default is not a complete Map.get/containsKey representation. Preserve the context in which casts or unboxing can throw, rather than eagerly unboxing every inserted value. See [List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html) and [Map](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html).

### 11.3 Unmodifiable shape does not freeze contained objects

Place a mutable ArrayList inside a List.of container, then mutate the inner list. The outer container still exposes that object's changed state. An unchanged outer structural generation does not alone validate a permanently cached outer hash, equality result or comparison fact.

Precomputation requires immutable relevant element facts, a valid observed version protocol, or recomputation. Structural sharing must not secretly freeze arbitrary elements. An immutable vector of VM heap references is not automatically an OS-shareable vector across processes. The [List factory contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#unmodifiable) distinguishes an unmodifiable container from mutable contained objects.

### 11.4 Different immutable factories need different admission rules

Use the same equal element twice. List.of retains two positions; Set.of rejects duplicate elements; Set.copyOf over a duplicate-containing collection keeps one representative per equal element without promising which equal representative. Map.of rejects duplicate keys. Do not implement all factories with one deduplication-or-rejection rule.

Null restrictions, order, value-based container identity and serialization must also be tested per factory. Sources: [List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), [Set](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Set.html), [Map](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html).

### 11.5 A supported live-view update

Start with a mutable list containing A, B, C. Obtain subList(1, 3), then set the view's first element to X. The parent becomes A, X, C. Clear that still-valid view; the parent becomes A. A snapshot copy fails this backed-view example.

Do not structurally modify the parent outside the view and then assert universal deterministic behavior for the old subList. For Arrays.asList, separately test writes through to the original array and unsupported size changes. A small live view retaining a large owner must appear in retained-memory accounting.

### 11.6 A failing acceptance gate blocks promotion

Put one mapping into a HashMap. Obtain its entry from entrySet().iterator().next(), invoke entry.setValue(newValue) before any other structural modification, then read map.get(entry.getKey()). The supported entry update must be reflected in the map.

A compact candidate returning a detached mutable entry whose setValue modifies only the temporary entry fails this gate, even if its lookup benchmark improves. Keep the candidate blocked, record the exact failure, preserve the baseline and repair the entry/view recipe. This example stays within a supported entry lifetime; it does not demand permanent attachment after unsupported map modification.

By contrast, test that a TreeMap navigation snapshot does not become a write-through mutable entry. A shared entry primitive must encode these different permissions rather than unify them by name.

### 11.7 A concurrent update has a bounded claim

Initialize a payload before a successful ConcurrentHashMap putIfAbsent, then have a reader retrieve that mapping. The planned stress test checks the required visibility of prior initialization. Separately race removal, replacement and resizing; traversal, size and unrelated-key reads do not become one atomic transaction.

For compute/merge, record callback multiplicity, null-result/removal behavior, exceptions and recursive-update restrictions for each concrete method. Do not memoize arbitrary stateful callbacks because keys are interned, or claim a function runs once per key over the map's lifetime. The [ConcurrentHashMap contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) is the starting point; proof and stress evidence must match the candidate algorithm.

### 11.8 Properties cannot inherit an uninspected port

Create a Properties object with a defaults Properties. Compare getProperty against Object-map lookup for a key present only in defaults, local replacement/removal and later defaults mutation. Inventory non-String Object entries separately from String property APIs and persistence.

The selected recipe must inspect Properties' own ConcurrentHashMap and overrides. It cannot rewrite Hashtable nodes and count Properties as delivered. The pinned [Properties source](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/src/java.base/share/classes/java/util/Properties.java#L143-L214) explicitly distinguishes these owners and synchronization policies.

### 11.9 Exact confirmation and array export

Use unequal keys with equal hashes: metadata must not return a false membership or the wrong value. Use matching numeric IDs from different owner namespaces/generations: they must not be accepted as the same entity without an explicit compatible identity mapping.

Collection.toArray overloads must produce the required Java array and retain applicable component-type, writable-result, caller-array and ArrayStoreException behavior. A descriptor for discontiguous lanes is not an ordinary Java array. Required array exports are explicit materialization, not proof that internal payload sharing failed.

## 12. Complexity, cached facts and lifetime accounting

These are analysis obligations, not benchmark results. Let n be live elements, c capacity, s segments, d index-directory depth and w the relevant bitmap word count. Include user hash/comparator cost rather than assuming each callback is a bounded-time primitive.

| Candidate | Required cost analysis | Lifetime and space tradeoff |
| --- | --- | --- |
| Flat or segmented vector | Separate direct lookup, O(d) directory lookup where used, amortized growth, suffix shifting and segment splitting; an arbitrary tree does not establish ArrayList's constant-time access | Charge c versus n, directory nodes, temporary old/new arrays and retained view owners. Clear removed references and preserve valid state on allocation failure |
| Indexed linked arena | Constant-time local link edits require a known valid position; nth-element discovery remains traversal unless a maintained rank index exists | Element references stay GC-visible. Next/prev/free/generation lanes and rank-index maintenance cost memory and work; reused slots must not retarget valid cursors |
| Compact hash storage | State load/hash assumptions, probe/chain work, tombstone behavior, iteration, collision fallback and resize peak | Charge Object lanes, control/index/order lanes, slack, entry wrappers and rehash coexistence. Reference handles still need a strong owner where the contract requires reachability |
| Indexed balanced tree | Derive logarithmic structural search/update for the chosen balanced algorithm; include comparator cost and output size for ranges | Rotations/relocation must preserve valid range and entry semantics. Extra rank/interval indexes add update and retention costs |
| Heap/ring | Distinguish heap maintenance from value scans and amortized ring growth from individual resize cost | Storage complexity does not prove blocking/fairness/timeout semantics. Include capacity, comparator failures and wraparound invariants |
| Enum/bitmap | Cost depends on universe and w, not only n; include initialization and sparse large universes | Compare existing EnumMap/EnumSet/BitSet first. Sharing metadata cannot merge enum/class-loader identity domains |
| Immutable/copy-on-write structure | Separate construction, reads, full/path-copy updates and reclamation | Old snapshots can retain old backing; structural sharing can reduce copied bytes while increasing retained memory. Contained object references retain their own mutability/identity |
| Concurrent indexed storage | Separate successful steps, retries and operation-specific progress under stated contention | Publication, roots/barriers, reclamation, ABA protection and cleanup are part of the cost and proof, not free services |

Use a symbolic compact-map memory ledger: container + reference lanes + index/control/order lanes + capacity slack + entry/view wrappers + synchronization + shared sidecars + transient resize peak. Compare it with the actual pinned baseline under identical object layout and GC settings. Do not assume a fixed byte saving from a presumed header or reference width.

Every proposed precomputed fact needs an exact validity domain, owner/namespace/generation, relevant range or occurrence, operation/policy version, admission budget, concurrency/publication rule and eviction rule. Keep reusable derived metadata in the established shared indexed sidecars; do not add an unbounded per-value cache or a second canonical payload store. Evicting derived facts must not invalidate live payload owners or change observable results. An outer collection generation alone cannot validate facts depending on independently mutable elements.

For each owner, specify construction, publication, mutation, resizing, view/cursor acquisition, removal/clear, failure rollback and reclamation. Weak keys, normal strong values, copy-on-write snapshots and native consumers have different reachability requirements. A global canonical key pool or strong auxiliary key index can defeat WeakHashMap; GC timing must not be guessed.

Measure admission/conversion/export, precomputation, cold/warm use, allocation, peak/retained heap, native/mapped memory and cleanup. Separate per-process virtual mappings from physically shared pages to avoid claiming the same saving twice. Include regressions and break-even workloads. The complexity distinctions for baseline [PriorityQueue](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html) and [ArrayDeque](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayDeque.html) illustrate why operation-level measurements matter.

No executable tests, benchmarks, source reactor build or matched-JDK acceptance were run for the documentation refinements in sections 11–12.
