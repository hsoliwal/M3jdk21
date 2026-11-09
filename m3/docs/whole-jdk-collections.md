# Collections as a first-class M3 replacement programme

Status: **proposed replacement specifications, not implemented replacements or executed tests**.
Target inspection pin: `45f546ff5bcb06a1b2604f14baf998785d98d9a1`.
Read the [architecture](whole-jdk-architecture.md), [inventory](whole-jdk-inventory.md), [text operation bridge](whole-jdk-text-operations.md), and [execution/measurement gates](whole-jdk-execution.md).

## 1. Evidence and design boundary

Selected target bodies were read at the inspection pin: [ArrayList storage](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/src/java.base/share/classes/java/util/ArrayList.java#L110-L180), [HashMap nodes and hashing](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/src/java.base/share/classes/java/util/HashMap.java#L260-L345), and [Properties backing and construction](https://github.com/hsoliwal/M3jdk21/blob/45f546ff5bcb06a1b2604f14baf998785d98d9a1/src/java.base/share/classes/java/util/Properties.java#L155-L245). ArrayList retains an `Object[]`; HashMap uses node objects with key/value/next fields and tree-bin machinery; Properties uses its own `ConcurrentHashMap`, not inherited Hashtable storage.

Other current-representation entries below are **Java 21 reference models to verify against each exact fork owner before implementation**, not a claim that every body or nested helper was read in this pass. Relevant public specifications include [List](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html), [Collections](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html), and the [concurrent package](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html). The recursive class/method inventory remains an explicit WP-00 obligation. Every backend in the candidate columns is a proposal.

Extend compatible existing lean-collection owners and primitives, documented in the private source-side companion; do not start a parallel collection library. Reuse geometry, searching, sorting, capacity checks, bit lanes or probe mechanics by semantic fit. Keep payload ownership, null policy, callback behavior, publication and concurrency in the appropriate concrete owner. Algorithm donor selection and source admission require their own pinned recipe and license review.

## 2. Common representation and lifetime vocabulary

A **reference lane** is GC-traced Java object storage. A **primitive lane** stores actual primitive values or structural coordinates, not hidden raw Java pointers. A **stable slot** survives movement of a separate index until explicitly removed; a dense position need not. A **segment directory** maps logical offsets to owned chunks. A **snapshot root** owns an immutable structural version, not necessarily immutable element objects.

For generic collections, first target structural overhead: node headers, next/previous references, spare capacity and temporary entries. Retain the actual key/value/element objects and their supported identity observations. Replacing `Map<K,V>` with IDs can add an object registry, indirection and retention, so account for that registry rather than calling it free. Primitive APIs can avoid boxing; generic calls, returned wrappers, casts, arrays, reflection and external callbacks may still need objects. Route B may remove those boundaries only with sufficient proof.

Mutable storage is owned by its collection. Clear dead reference slots after supported removal/clear so deleted elements are not retained accidentally. A structural-sharing design copies affected structure before mutation and keeps old snapshot owners alive. Reclamation must respect live iterators, views and native leases. A weak cache must never be the sole owner of storage required by a live collection.

Use checked capacity arithmetic. Public int-based sizes, arrays and offsets cannot silently inherit a donor's different bounds or sentinel policy. Each method records allocation-failure and partial-update semantics; do not invent blanket transactional guarantees where the original API permits partial effects, or publish a corrupt half-grown table.

## 3. Concrete sequential collections

`n` is live size, `c` capacity, and `b` a configured chunk size. Complexity entries are review obligations, not measured results. All rows additionally require the common contract matrix in section 6.

| Concrete owner/family and reference model | Candidate M3 representation and lifetime | Complexity/tradeoff and decisive tests |
|---|---|---|
| **ArrayList**: resizable reference array, observed at pin | Reuse checked growth and bulk/range primitives first. Consider an owner-local segmented reference vector only after a separate compatibility/performance case. Preserve logical size and live subviews. | Keep indexed access efficient and append amortization explicit. Middle shifts remain work. Tiny lists can lose from directories. Test ensureCapacity/trimToSize, clone, serialization, modCount-sensitive paths and reversed/subList composition. |
| **LinkedList**: doubly linked element nodes | Stable-slot reference payload plus primitive previous/next lanes; a free-slot owner controls reuse. Do not substitute a simple contiguous array merely because it is compact. | End operations and iterator-local insertion/removal must retain their profile; indexed traversal remains distinct. Test null elements, list/deque views, bidirectional iterators and slot reuse while valid iterators exist. |
| **Vector**: synchronized resizable array with exposed protected storage/capacity fields | Prefer internal algorithm reuse while retaining required fields and monitor behavior. Segmentation is blocked where it would change the protected array contract. | Preserve capacity increment, enumeration, subclass behavior, synchronization and serialization. A faster unsynchronized list is not a replacement. See [Vector fields/API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Vector.html). |
| **Stack**: Vector subclass | Reuse compatible Vector internals; do not silently replace the inheritance/API surface with ArrayDeque. | Preserve LIFO methods, inherited indexed operations, empty behavior, monitor and subclass contracts. |
| **Arrays.asList / nested Arrays list**: fixed-size view of the supplied object array | Retain the same backing array and write-through `set`; only reuse operation primitives. | O(1) access; no structural growth. Test input-array mutation, `set`, toArray and unsupported add/remove. `Arrays.asList(int[])` is not a list of boxed ints. |
| **ArrayDeque**: growable reference ring | Reuse ring/capacity atoms; consider segmented ring only with measured large-queue benefit. Collection owns writable slots. | End operations amortized O(1), interior/search operations distinct. Preserve null rejection, wraparound, descending/reversed encounter and iterator removal. |
| **PriorityQueue**: reference-array heap | Reuse heap geometry and specialized sift loops without changing comparator/natural-order policy. | Offer/poll logarithmic, peek constant; arbitrary removal/search linear in reference model. Heap iteration is not sorted order. Test comparator throws/reentrancy, ties, resize and iterator removal. |
| **HashMap**: observed nodes, bucket table and tree-bin support | Compact bucket/control/link lanes plus actual key/value reference lanes; or an independently reviewed open-addressed design. Entry adapters bind supported entry operations to stable ownership. | Expected constant-time lookup is conditional on distribution. Collision defense, resize cost, sparse iteration and callback effects need adversarial evidence. Preserve null key/value and distinguish absent from mapped-null. |
| **HashSet**: map-backed membership | Share the admitted hash-set/map substrate without a second dictionary. Retain the actual member references. | Match equality, null, duplicate and iterator behavior; count dummy-value/entry savings against new index metadata. |
| **LinkedHashMap**: hash entries plus encounter-order links | Stable entry slots with primitive order links; reuse hash geometry only where compatible. Access-order policy remains owner-specific. | Retain insertion/access order, explicit positioning, reversed views and subclass eviction hooks. Test access through reversed views; a `get` can affect access order. See [LinkedHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashMap.html). |
| **LinkedHashSet**: ordered map-backed membership | Reuse admitted ordered membership and stable slots. | Preserve encounter/reversed order and Java 21 first/last operations, not just membership. |
| **TreeMap**: comparator-based balanced tree | Primitive child/parent/color lanes with reference keys/values, or another reviewed ordered backend preserving complexity and views. | Keep logarithmic basic update/search requirements. A sorted array's linear insertion is not a general substitute. Test comparator-equal unequal objects, range endpoints, descending views and comparator exceptions. See [TreeMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html). |
| **TreeSet**: navigable-map-backed set | Reuse the admitted ordered-map owner rather than duplicate ordering logic. | Preserve comparator-based uniqueness, null behavior of the selected comparator, range views and navigation. |
| **IdentityHashMap**: alternating key/value reference array with probing | Reuse probe/index mechanics while keeping reference-identity comparisons for keys, values and views. Already compact; require evidence of benefit. | Never content-intern keys. Test distinct equal objects, identity-hash collisions, null, removal/rehash and entry/view identity semantics. See [IdentityHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IdentityHashMap.html). |
| **WeakHashMap**: weak-key entries and reference-queue cleanup | Retain collector-integrated weak references; compact unrelated structural metadata only if it does not add strong key retention. | GC can change contents independently of explicit mutation. Test queued cleanup, value-to-key cycles, null-key behavior and expunging. A global ID table retaining keys defeats the design. See [WeakHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/WeakHashMap.html). |
| **EnumMap**: enum-universe-indexed value array | Reuse ordinal indexing and compact presence metadata within the correct enum class/loader. | Already specialized; preserve declaration order, null values, type checks, class-loader boundaries and serialized enum identities. Ordinal alone is not a globally valid key. |
| **EnumSet / regular and jumbo variants**: bit-vector membership | Reuse bit geometry and bulk bit operations; retain enum universe/owner and shallow snapshot rules. | Space depends on enum universe, not generic hash nodes. Test empty/type inference, declaration order, incompatible enum classes and weakly consistent iteration. See [EnumSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumSet.html). |
| **Hashtable**: synchronized hash structure | Reuse structural atoms under the existing synchronization contract; retain null rejection and compatibility APIs. | Test enumerations versus iterators, monitor interactions, subclasses, serialization and callback methods. Do not impose HashMap null policy. |
| **Properties**: observed separate ConcurrentHashMap plus defaults chain; simple reads omit synchronization while writes/bulk operations remain synchronized | Preserve this distinct owner and defaults semantics. Reuse text/metadata operations only through exact operation bridges. | Do not treat it as ordinary Hashtable storage or assume all inherited values are Strings. Test getProperty/defaults, inherited object operations, load/store/XML, escapes, synchronization and serialization. |
| **BitSet**: related primitive bit-vector API, not a generic Set | Reuse bit/range geometry without changing bit indexing or array-export semantics. | Test logical length versus capacity, trailing zeros, negative/overflow indices, range mutation and independent returned arrays. Primitive specialization is already present. |

## 4. Immutable structures, wrappers and views

| Owner/family | Candidate and ownership | Required compatibility boundary |
|---|---|---|
| **List.of/copyOf, Set.of/copyOf, Map.of/ofEntries/copyOf and internal immutable implementations** | Reuse compact immutable structural atoms or chunks only where beneficial; small instances may already be more compact than a generic descriptor. Elements are not made deeply immutable. | Null/duplicate acceptance depends on the particular factory and operation. Preserve shallow contents, equals/hashCode, serialization and value-based restrictions where documented; never generalize them to mutable collection identity. |
| **Collections empty/singleton/nCopies forms** | Retain existing tiny specializations or reuse equivalent immutable metadata. | `nCopies` repeats the same element reference; do not create equal substitutes. Preserve optional-operation and null behavior for each factory. |
| **Unmodifiable wrappers, including sequenced forms** | View descriptor retains the original owner, not a copied snapshot. | Underlying mutations remain visible. Mutation is rejected through all supported entry, iterator, bulk and nested-view paths; allowed no-op/exception behavior is method-specific. |
| **Synchronized wrappers and nested views** | Retain wrapper/mutex ownership and delegation; only replace underlying backend after its own admission. | Preserve which monitor clients must hold for traversal and which mutex subviews share. Do not introduce a different hidden lock and claim equivalent external synchronization. |
| **Checked wrappers** | Retain runtime type-check policy at every mutating boundary. | Test add/put/bulk/default-method/entry mutation and type/exception ordering. Primitive lowering must not erase observable checks. |
| **subList, subMap, headMap, tailMap, keySet, values, entrySet, descending and reversed views** | Owner-relative ranges or order projections with required version/iterator state. | Remain backed where specified; preserve bounds, removal/write-through, ordering, mutability and stale-view behavior. A cheap immutable slice is not a substitute for a live mutable subList. |
| **newSetFromMap / asLifoQueue and other adapters** | Preserve the supplied backing owner and translated operations. | Caller-visible mutation through either surface must agree; do not detach or duplicate the backing store. |
| **Map.Entry forms** | Distinguish mutable backed entries, standalone mutable entries and immutable snapshots. Compact entries may allocate adapters on demand. | `setValue`, equality/hash, supported lifetime and view mutation must match the particular producer. Slot reuse must not redirect a still-valid entry to another mapping. |

Java 21 adds sequenced interfaces and reversed views. Review [Sequenced Collections](https://openjdk.org/jeps/431) and the concrete owner APIs, not just older List/Map tests. Preserve `SequencedCollection`, `SequencedSet`, `SequencedMap`, first/last operations and inverse-view write-through where supported. Sorted owners can reject repositioning operations whose semantics conflict with their ordering. Do not turn every reversed operation into a materialized copy.

## 5. Concurrent and blocking families

Concurrency is not supplied by packing a sequential backend. The current reference families have different synchronization and progress properties. The following are proposed reuse boundaries; exact method/algorithm proofs remain required.

| Family and reference model | Candidate reuse and lifetime | Acceptance and tradeoff |
|---|---|---|
| **ConcurrentHashMap**: concurrent node/table algorithms with specialized update/resize paths | Retain publication, resize and per-key update protocols while evaluating compact structural lanes. GC-traced payload and safe reclamation are mandatory. | Null rejection, atomic conditional/compute operations, weakly consistent traversal and per-entry visibility. Aggregates are not automatically snapshots. Test reentrant/throwing callbacks, resize races and slot reuse. [API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html). |
| **ConcurrentSkipListMap / ConcurrentSkipListSet**: concurrent ordered structures | Reuse search/index geometry only with an admitted ordered-concurrency protocol. | Ordering/comparators, navigable views, weak consistency and atomic methods. A locked sorted array is not an automatic replacement for the existing progress/complexity profile. [Map API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentSkipListMap.html). |
| **CopyOnWriteArrayList / CopyOnWriteArraySet**: published immutable array snapshots | Consider immutable chunk/root sharing, retaining each live iterator's structural version and the same element references. | Read paths, snapshot iteration, unsupported iterator mutation and bulk/write behavior must hold. Writes and retained old snapshots cost memory; measure tiny reads and long-lived iterators. [List API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CopyOnWriteArrayList.html). |
| **ConcurrentLinkedQueue / ConcurrentLinkedDeque**: nonblocking linked algorithms | Compact slots/segments only after a complete CAS, ABA, helping and reclamation design; geometry reuse alone is insufficient. | Preserve FIFO/deque semantics, visibility, weak traversal and documented nonblocking behavior. Stress stalled threads, removals and reclaimed slots; do not add a global lock as a hidden optimization. |
| **ArrayBlockingQueue**: bounded array with lock/conditions | Reuse ring arithmetic inside the established bounded synchronization owner. | Capacity, interruptible/timed/blocking methods, fairness option and wakeup behavior; no silent resizing beyond the bound. |
| **LinkedBlockingQueue**: optionally bounded linked queue | Segment/node packing may reduce structural allocation; preserve ownership and coordination of producer/consumer paths. | Capacity accounting, blocking/interrupts, drainTo and cross-operation visibility. Do not assume ArrayBlockingQueue's locking protocol is interchangeable. |
| **LinkedBlockingDeque**: blocking deque with linked state | Reuse deque geometry only within its own lock/condition and capacity protocol. | Both-end blocking/timed operations, interrupts, traversal and drain behavior. |
| **PriorityBlockingQueue**: concurrent unbounded priority queue | Reuse heap atoms while preserving synchronization and allocation behavior. | Comparator/tie rules, blocking retrieval, unbounded-capacity contract and unsorted iterator semantics; failed allocations remain possible. |
| **DelayQueue**: delayed-priority ownership and waiting | Reuse heap mechanics, not a generic FIFO/ring substitution. | Eligibility depends on delay/time; head inspection, expiration, timed waits and interruption need separate tests. [API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/DelayQueue.html). |
| **SynchronousQueue**: rendezvous, not element storage | Retain the handoff protocol; there is no ordinary retained-element array to compact. | Zero-capacity handoff, fairness option, timeout, interruption and unmatched participants. [API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/SynchronousQueue.html). |
| **LinkedTransferQueue**: linked transfer/matching protocol | Reuse only compatible allocation/geometry under a verified transfer protocol. | Distinguish enqueue from transfer waiting for receipt; preserve cancellation, waiting-consumer queries and weak traversal. [API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/LinkedTransferQueue.html). |

Atomics, locks, synchronizers, executors, fork/join and virtual-thread runtime consumers remain WP-05/WP-11 obligations beyond these collection rows. A storage optimization must not silently change their blocking, interruption, parking, cancellation or memory semantics. Use the [Java 21 memory model](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html) and concrete concurrent API contracts as oracles. Test linearizability only where promised; do not demand a whole-map atomic snapshot from an API that does not provide it.

## 6. Mandatory per-method contract matrix

For each concrete owner and every inherited/default/overridden operation, capture the following dimensions in the existing capability mapping or its derived contract projection. A class-name row is not enough.

| Dimension | Required discriminators |
|---|---|
| Mutability | Mutable, immutable structure, unmodifiable view, snapshot, copy-on-write; shallow versus deep contents. |
| Identity/equality | Same reference versus equal value, stable key/element objects, owner namespace, enum/class-loader identity. |
| Null/type/exceptions | Null key/value/element, incompatible types, absent versus mapped-null, required exception class and evaluation/validation order. |
| Ordering | Insertion, access, comparator, natural, FIFO/LIFO, priority and explicitly unspecified order; duplicates and ties. |
| Hash/comparator behavior | Equal hashes, adversarial collisions, comparator-equal unequal keys, mutable keys, callback throw/reentry and invalid cached facts. |
| Views | Nested/range/reversed/descending/live views, entry write-through, removal, owner mutation and supported lifetime. |
| Iteration | Iterator/ListIterator remove/set/add, enumeration, best-effort fail-fast, weak consistency, snapshot and external synchronization. |
| Bulk/default methods | addAll/putAll/removeIf/replaceAll/compute/merge/sort and callback effects; preserve method-specific partial-failure behavior. |
| Arrays/clone/streams | Independent versus backed arrays, runtime component type, covariance, null termination, shallow clone, generator callbacks. |
| Spliterators | Exact characteristics, encounter order, comparator, sizing, splitting and concurrent/immutable claims. |
| Capacity/failure | Empty/default capacity, explicit capacity, grow/shrink, integer overflow, allocation failure and dead-slot clearing. |
| Compatibility | Public binary signatures, protected fields/hooks, serial forms, subclasses, supported reflection/agents and module access. |
| Concurrency | Linearization point where required, publication/visibility, progress/fairness, interruption/timeout/cancellation and reclamation. |

Fail-fast is not a correctness mechanism for data races. Never turn best-effort detection into a claim of guaranteed concurrent failure. Conversely, do not emit fail-fast exceptions from a traversal that promises weak consistency or a snapshot.

Private field introspection needs an explicit compatibility decision: changing storage can change declared private fields even when public methods remain. Do not promise universal reflective compatibility while changing those fields. Retain required public/protected fields and supported introspection, refuse compiler lowering for unresolved field-dependent use, and test declared support for custom-JDK consumers. Unsupported assumptions are documented, not silently used to justify broad compatibility claims.

Serialization must retain applicable serialVersionUID, custom write/read behavior, ordering/identity semantics and external interoperability. A stable UID alone is insufficient. Exact byte-for-byte identity is a separate requirement when the format or application depends on it; do not confuse it with semantic round-trip success.

## 7. Worked semantic examples

All examples below are illustrations and proposed test scenarios; they were not executed in this documentation pass.

### Compact generic map and boxing

A proposed generic hash backend keeps `Object[] keys`, `Object[] values` and primitive occupancy/hash/link lanes. For `Map<Long,Object>`, replacing bucket nodes may save structural allocations, but keys and returned objects remain Java objects. A separate primitive-key API can avoid key boxing; automatically changing the generic API cannot. On `put(k, v)`, preserve the actual `v` object for a supported later `get(k)` and preserve the concrete owner's key-retention behavior. A side table that reconstructs equal objects on demand is not transparently equivalent.

An entry-view adapter may be created only when requested, but it must refer to the correct owner/slot generation for its specified lifetime. A transient entry object is an allocation to measure, not a reason to omit Map.Entry behavior. Null presence uses explicit metadata rather than treating a valid zero primitive as absent.

### Immutable structure versus mutable collection

`List.of(box)` can have immutable structure while `box` is mutable. Updating a field of `box` must remain visible through the list; a cached value-derived list hash is unsafe without stronger element assumptions. Two independent `new ArrayList<>(source)` results may reuse immutable metadata only if later structural mutation remains isolated. `Collections.unmodifiableList(source)` instead remains a live wrapper over `source`, not a detached copy. These three ownership policies cannot share one mutation rule.

### Backed subList and reversed view

With `base = new ArrayList<>(List.of("a", "b", "c"))`, `base.subList(1, 3).set(0, "x")` must write through to the corresponding base element. Clearing the supported subview removes that range from the base. An unrelated structural base modification can invalidate the subview according to the concrete contract; a cached range descriptor cannot silently become a snapshot. Compose this with a reversed view and verify index translation, mutation, iterator state and exception behavior against the selected Java 21 owner.

### Identity and collisions

Two independently allocated `new String("same")` objects are text-equal but reference-distinct. An IdentityHashMap can retain both keys; canonicalizing their text into one key would destroy a mapping. A normal HashMap instead uses its equality policy. In either design, identical hash values alone cannot confirm equality. For collision tests, use controlled keys with constant hash and distinct equality results, then exercise resize, remove, views and compute/merge without assuming a particular unspecified iteration order.

### Concurrent update and visibility

For a proposed replacement of `ConcurrentHashMap.compute(k, remappingFunction)`, identify the atomic commit of the relevant mapping and the publication edge that makes its resulting state visible as the API requires. A packed entry must not publish occupancy before its key/value state is safely readable. A naïve CAS retry loop that calls user code repeatedly can violate the concrete method's callback contract. Preserve the current method's requirements or retain its synchronization protocol; do not borrow weaker guarantees from a different ConcurrentMap implementation.

Record histories with racing compute/remove/resize and a reader observing the mapping. Check only specified outcomes, callback behavior and visibility; bulk traversal does not become an atomic snapshot. A generation-tagged slot is not sufficient by itself to prove safe reclamation or eliminate ABA.

### Failure prevents promotion

A candidate packed linked map preserves membership but loses insertion order after a resize through `reversed().entrySet()`. Even with lower allocation, it fails the ordered-view gate and stays unaccepted. A candidate weak map that keeps keys alive through its global ID table fails the GC/lifetime gate. Neither failure may be hidden by disabling the test or relabelling the implementation as equivalent.

## 8. Acceptance and implementation sequence

First inventory actual concrete and nested owners, including AbstractCollection/AbstractList/AbstractSequentialList/AbstractSet/AbstractMap/AbstractQueue, interfaces, default methods, iterator/spliterator helpers and wrappers. Then select one compatible primitive operation family, reuse its recipe and tests, and integrate into one owner without changing contracts. Advance through file, package, module and full-JDK gates; do not ratify a whole-file superset merely because isolated donor tests pass.

WP-03 covers sequential mutable families; WP-04 covers immutable forms and all views; WP-05 covers concurrency. Views are a prerequisite for promotion, not a later optional feature. Establish exact API differential, randomized state-machine, adversarial key/comparator, serialization/subclass, lifetime and concurrency tests before any overhead claim. Benchmark tiny and large, sparse and dense, cold and warm, mutation-heavy and read-heavy cases, including cache admission and retained snapshots. Record losing candidates and leave the original backend where it is better or safer.

The scope table is a concrete starting specification, not an exhaustive declaration census. Any additional nested implementation, wrapper, factory, platform path or runtime consumer discovered by WP-00 must acquire an owner and explicit disposition in the existing mapping system. No collection is considered replaced solely because a primitive donor or facade works.
