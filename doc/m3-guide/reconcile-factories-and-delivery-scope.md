<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Collection factories and delivery scope

[Guide](README.md) · [Coverage](coverage.md) · [Progress ownership](specify-progress-ownership.md)

This reference reviews the complete [`M3Collections` body][factory] and
[`package-info.java`][package] at
`7abdf87cb497d0d94abeb723d70c50f856ecc381`. It accounts for the private constructor
and **all 58 public static factory overloads**. Their source blobs are unchanged
from the collection inventory pin `1011050da4205a901ad079d7711b6b8efd8a9af7`.
Delegates, constructor boundaries, actual tests, module/build declarations and
receiving metadata were inspected where identified below. Compilation, tests,
recipe execution, JNI, benchmarks and installed-image qualification are **NOT_RUN**.

A factory-route review is not a complete method-level review of every returned
owner. Reuse the existing [linked-hash map/set reference](collections-reference.md),
[primitive/snapshot map reference](primitive-map-reference.md) and
[primitive sequence reference](primitive-sequence-reference.md) for their already
reviewed contracts. Presence of a factory, public declaration or catalog row does
not establish Java SE compatibility, runtime delivery, or performance superiority.

## Factory ownership and lifetime

`M3Collections` is a final utility class with a private empty constructor and no
fields or persistent state. Apart from the three immutable-copy factories, every
factory constructs an empty mutable owner or a fresh wrapper/backing pair. There
is no global collection singleton, registry, automatic monitor, I/O, native loading,
worker startup or close protocol in the factory body. Allocation/delegate failures
propagate; the factory does not translate errors or roll back user callback effects.

All unqualified `M3...` names below are in `com.m3.collections`. JDK names are
explicitly prefixed by `java.util` or `java.util.concurrent`. Generic payloads stay
Java references; constructing a packed owner does not clone payloads, change their
identity, or turn references into unmanaged addresses. Weak-key owners have their
own reachability rules. Primitive owners store their declared primitive values.

## Complete factory routes

Each row names every declared overload it groups. Parentheses distinguish overloads;
`C` means a supplied comparator, `T` an enum class, and `n` the int argument.
These are constructor/delegate calls, not claims that the factory returns a JDK
class merely because its name resembles one.

### Sparse storage and primitive values: 16 overloads

| Factory | Exact delegate/result | Construction boundary and contract route |
| --- | --- | --- |
| `longLane()` | `new M3LongLane28()` | Sparse long lane; no address argument validated by factory |
| `intLane()` | `new M3IntLane28()` | Sparse int lane |
| `bitLane()` | `new M3BitLane28()` | Sparse bit lane; does not load its optional native implementation |
| `stableSlots()` | `new M3SlotAllocator28()` | Stable-slot allocator with generation-tagged handles |
| `longRows(n)` | `new M3LongLaneStore28(n)` | Constructor rejects `n <= 0`; creates that many lane owners and an allocator |
| `longArena()` | `new M3LongArena28()` | Append/range owner; fresh empty arena |
| `longLinkedList()` | `new M3LongLinkedList28()` | Stable-slot linked long order; not `java.util.LinkedList` |
| `concurrentLongLane()` | `new M3ConcurrentLongLane28()` | Per-slot atomic long operations; first page allocation differs from prepared access |
| `concurrentIntLane()` | `new M3ConcurrentIntLane28()` | Per-slot atomic int operations; no whole-collection transaction implied |
| `longArrayList()` | `new M3LongArrayList()` | Fresh dense list, shared empty backing until growth; [sequence contracts](primitive-sequence-reference.md) |
| `longArrayList(n)` | `new M3LongArrayList(n)` | Negative initial capacity rejected; zero uses empty backing; positive allocates an array |
| `longArrayDeque()` | `new M3LongArrayDeque()` | Constructor delegates to size 0; minimum ring allocation; [sequence contracts](primitive-sequence-reference.md) |
| `longArrayDeque(n)` | `new M3LongArrayDeque(n)` | Rejects negative or `n > 1 << 30`; rounds representation capacity in constructor; n is expected size, not initial contents |
| `longHashSet()` | `new M3LongHashSet()` | Primitive long equality/membership, independent occupancy for all long values |
| `longLongHashMap()` | `new M3LongLongHashMap()` | Primitive long keys/values; [map contracts](primitive-map-reference.md), including insertion-result semantics |
| `longPriorityQueue()` | `new M3LongPriorityQueue()` | Primitive natural-order min-heap; heap order does not promise sorted traversal |

The above factories expose only the listed signatures. The physical int-collection,
identity, enum, concurrent bitset and snapshot-map/set owners do not gain imaginary
factory overloads by being present in the same package. `M3LongCollection`-style
APIs are not automatically implementations of `java.util.Collection`.

### Ordinary reference collections: 23 overloads

| Factory | Exact delegate/result | Order/identity and boundary |
| --- | --- | --- |
| `arrayList()` | `new java.util.ArrayList<>()` | JDK positional list |
| `linkedList()` | `new M3LinkedList<>()` | M3 list/deque with primitive links and reusable slots |
| `hashMap()` | `new M3LinkedHashMap<>()` | Insertion-ordered M3 map, not JDK HashMap |
| `linkedHashMap()` | `new M3LinkedHashMap<>()` | Same owner/default as hashMap |
| `linkedHashMap(boolean)` | `new M3LinkedHashMap<>(accessOrder)` | False insertion order; true access order; live/reversed view rules in [linked-hash reference](collections-reference.md) |
| `treeMap()` | `new M3TreeMap<>()` | Natural-order eager tree; no lazy provider installed |
| `treeMap(C)` | `new M3TreeMap<>(C)` | Comparator passed through; null selects natural order; no lazy-provider overload here |
| `hashSet()` | `new M3LinkedHashSet<>()` | Insertion-ordered key-only M3 set |
| `linkedHashSet()` | `new M3LinkedHashSet<>()` | Same owner/default as hashSet |
| `treeSet()` | `new M3TreeSet<>()` | Natural-order key-only M3 tree |
| `treeSet(C)` | `new M3TreeSet<>(C)` | Comparator passed through, null natural order |
| `arrayDeque()` | `new java.util.ArrayDeque<>()` | JDK ring deque |
| `priorityQueue()` | `new java.util.PriorityQueue<>()` | JDK natural-order heap |
| `priorityQueue(C)` | `new java.util.PriorityQueue<>(C)` | Comparator passed directly; null natural order |
| `identityMap()` | `new java.util.IdentityHashMap<>()` | JDK reference-identity map, **not** `M3ObjectIdentityMap` |
| `weakMap()` | `new M3WeakHashMap<>()` | M3 weak-key map with reference queue cleanup |
| `enumMap(T)` | `new java.util.EnumMap<>(T)` | JDK enum-key map, **not** `M3EnumLongMap` or `M3AdaptiveEnumLongMap` |
| `enumSet(T)` | `java.util.EnumSet.noneOf(T)` | JDK-selected EnumSet implementation, **not** `M3EnumSet` |
| `bitSet()` | `new java.util.BitSet()` | JDK BitSet, **not** M3BitLane28/M3ConcurrentBitSet |
| `vector()` | `new java.util.Vector<>()` | JDK legacy synchronized positional list |
| `stack()` | `new java.util.Stack<>()` | JDK legacy stack |
| `copyOnWriteList()` | `new java.util.concurrent.CopyOnWriteArrayList<>()` | JDK copy-on-write owner; iterator snapshot behavior is tested below |
| `copyOnWriteSet()` | `new java.util.concurrent.CopyOnWriteArraySet<>()` | JDK copy-on-write set |

The factory does not adapt payload/null/equality policies for JDK delegates.
For example, [IdentityHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IdentityHashMap.html)
uses reference identity and permits null, while
[EnumSet](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/EnumSet.html)
uses the supplied enum domain and declaration order, rejects null insertion, and
`noneOf` rejects a null class. The enum class argument is not replaced by a name or
copied domain. For M3 linked-hash containers, null support and key stability are
covered in the existing reference. A null tree comparator means natural ordering;
it does not itself promise that a null key/element is admissible. Comparator code
and the concrete owner's key validation decide that boundary.

### Locking, waiting and wrappers: 16 overloads

| Factory | Exact delegate/result | Concurrency/waiting boundary |
| --- | --- | --- |
| `lockedMap()` | `new M3LockedMap<>()` | M3 packed hash map behind one shared monitor; nulls rejected |
| `lockedHashtableMap()` | `new M3LockedMap<>()` | Same owner; Map alternative only, no Dictionary or Hashtable serialized-form emulation |
| `lockedTreeMap()` | `new M3LockedTreeMap<>()` | M3 tree under shared lock, natural order |
| `lockedTreeMap(C)` | `new M3LockedTreeMap<>(C)` | Comparator passed through; shared-lock range views |
| `lockedHashSet()` | `java.util.Collections.newSetFromMap(new M3LockedMap<>())` | JDK set facade over fresh empty M3 map; null-element rejection follows its backing map |
| `lockedTreeSet()` | `new M3LockedTreeSet<>()` | M3 shared-lock tree set, natural order |
| `lockedTreeSet(C)` | `new M3LockedTreeSet<>(C)` | Comparator passed through |
| `lockedQueue()` | `new M3BlockingDeque<>()` | Fresh non-fair deque with limit `Integer.MAX_VALUE - 8`; name does not select a lock-free queue |
| `lockedDeque()` | `new M3BlockingDeque<>()` | Exactly the same constructor/default as lockedQueue |
| `blockingQueue(n)` | `new M3BlockingDeque<>(n)` | Positive-capacity, non-fair M3 deque; constructor rejects `n <= 0` |
| `blockingDeque(n)` | `new M3BlockingDeque<>(n)` | Same constructor; growable backing up to fixed logical capacity |
| `arrayBlockingQueue(n,fair)` | `new java.util.concurrent.ArrayBlockingQueue<>(n,fair)` | JDK fixed-array owner; capacity/fairness passed directly |
| `priorityBlockingQueue(C)` | `new java.util.concurrent.PriorityBlockingQueue<>(11,C)` | JDK heap, initial capacity 11, comparator passed directly |
| `delayQueue()` | `new java.util.concurrent.DelayQueue<>()` | JDK delay-order heap for `E extends Delayed` |
| `transferQueue()` | `new M3TransferQueue<>()` | Constructor selects `(rendezvous=false,fair=false)` |
| `synchronousQueue(fair)` | `new M3TransferQueue<>(true,fair)` | M3 zero-capacity rendezvous; **not** `java.util.concurrent.SynchronousQueue` |

The M3 transfer queue constructor selects a `ReentrantLock(fair)` and condition.
Matching is FIFO in both modes; fairness changes lock admission, not the
implementation class. In rendezvous mode its visible size/remaining-capacity are
zero, `peek` returns null, and an offer without a waiting receiver fails. Ordinary
transfer mode can retain queued data. Factory creation itself never waits for a
peer. Blocking/interruption, null rejection, iterator snapshots and cleanup are
contracts of the returned owner, not a new protocol added by the factory.

The set returned by `lockedHashSet` is declared `Set<E>`, not a newly declared M3
set class or a promise of a sequenced API. Its backing map is created empty as
required by the JDK wrapper. Locking individual operations does not make an
arbitrary caller's compound read/modify sequence atomic. Shared-monitor callbacks
can have lock-order/reentrancy effects; this routing table is not a proof of
lock-free or wait-free progress.

### Immutable shallow copies: 3 overloads

| Factory | Actual construction | Copy, null and order contract |
| --- | --- | --- |
| `immutableList(Collection)` | `java.util.List.copyOf(source)` | JDK unmodifiable result in source iteration order; duplicates retained; null source/elements rejected; instance reuse is allowed |
| `immutableSet(Collection)` | New `M3LinkedHashSet`; `source.forEach(e -> set.add(requireNonNull(e)))`; `Collections.unmodifiableSet(set)` | Fresh private M3 container and JDK wrapper; null source/elements fail; equals-duplicate elements collapse; first retained encounter position/order |
| `immutableMap(Map)` | New `M3LinkedHashMap`; `source.forEach((k,v) -> map.put(requireNonNull(k),requireNonNull(v)))`; `Collections.unmodifiableMap(map)` | Fresh private M3 container and JDK wrapper; null source/key/value fail; order follows the source callback encounter order |

These factories freeze container mutation, not the reachable object graph. Keys,
values and elements are shared references. Later source-container mutations do not
update the copied result; mutations to shared payload objects can still be visible,
and mutating an equality/hash key can violate that key's container obligations.
`List.copyOf` need not allocate a different list when an eligible unmodifiable
input is supplied. See the Java 21
[List copy contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#copyOf(java.util.Collection)).

The set/map wrappers expose only the declared Set/Map API; their backing owner's
sequenced methods are not promised on the wrapper. Entry `setValue` cannot bypass
map immutability. The existing [linked-hash reference](collections-reference.md)
explains the reused M3 order, key retention and view implementation; the
[JDK unmodifiable-map contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html#unmodifiableMap(java.util.Map))
defines the wrapper boundary.

The factory does not synchronize the source or make its iteration an atomic
snapshot under concurrent mutation. Source `forEach`, hash/equality code and
iteration can throw or have side effects. A failed set/map copy may have filled a
private partial container, but does not return it or intentionally mutate the
source; external callback effects are not undone. Null map key validation occurs
before value validation for each visited pair. Set duplicates do not reorder the
first stored representative. For unusual source implementations that repeatedly
visit equal map keys, later values replace earlier ones while the original
insertion position is retained; ordinary Map sources expose one mapping per key.

## Read package wording at owner granularity

The package descriptor describes indexed array payload/control storage, primitive
ordering/reclamation and boundary allocations. It contains no types or callable
members. Its statement that concurrent alternatives specify blocking locks fits
the `M3Locked*`, blocking-deque and transfer-queue families, but it is **not** a
package-wide progress classification:

- CAS int/long lanes use atomic per-slot operations and separately published pages
- `M3ConcurrentBitSet` uses atomic words; a traversal is not a coherent whole-set transaction
- `M3ConcurrentLongMap` / `M3ConcurrentLongSet` publish immutable snapshots; changed writes copy arrays and can retry CAS
- Single-threaded primitive/reference owners do not become safe for concurrent mutation by sharing this package
- Collection progress has distinct synchronized-local and atomic-field observation rules; see [progress ownership](specify-progress-ownership.md)

Likewise the broad phrase “implementations of Java collection interfaces” does not
turn every primitive lane, monitor, helper or specialized primitive API into a
`java.util` implementation. The [module README][module-readme] already distinguishes
these representations. This external documentation qualifies the shorthand without
changing the sealed package source or the canonical recipe.

## Delivery evidence is not a factory property

The [module descriptor][module] exports `com.m3.collections` and declares no
requires beyond implicit `java.base`. The [POM][pom] builds
`com.m3:m3-collections:0.1.0-SNAPSHOT`, targets release 21, declares the JUnit
source-qualified test dependency, disables module-path test compilation/execution,
and packages license/provenance resources. Its optional OpenRewrite profiles are
authoring/verification configuration, not a Synexia runtime dependency. This is a
separately built module alongside OpenJDK, not proof that an installed JDK image
contains it or that `java.util` implementations were replaced.

### Three distinct dated inventory scopes

At the source pin above, complete bounded directory/tree reads established:

| Evidence unit | Observed count / identity | Interpretation |
| --- | --- | --- |
| Current physical `src` Java | 77 package Java files (including package-info), plus module-info = **78 main Java**; **30 test Java**; **108 total** | Source-tree presence; no runtime pass inferred |
| Current [installed manifest][installed] | **98 path rows** = 68 main Java + 25 test Java + 2 standalone Java probes + 3 non-Java inputs | Rows counted after excluding blank/comment lines, not “98 production owners” |
| Current manifest SHA-256 | `90e6e4adcc6a9224b6ca7f85b5d01b950ed8c368fcc660b04ce56fda59eae689` | Matches `installed_source_manifest_sha256` in [donation metadata][donation] |
| Linked dated [qualification receipt][receipt] | Records **95 installed rows**, 66 main Java, 24 tests, 90 module Java, 2 probes, 3 other inputs; product inventory digest `6627c851e6a879e28c0434b41f988669c118649e04a4d89bcf30102aa9e767ed` | Historical declared qualification scope, not the current manifest or current directory count |
| Module README historical receiver paragraph | Describes an earlier **88-Java** manifest and 113 tests | Earlier bounded receipt narrative, not a current-source census |

The current installed manifest and its donation checksum agree. The count/digest
differences from the dated qualification receipt are **not a current checksum
failure**. They do mean the old 117-test/pass declarations cannot be silently
relabelled as a run against all current sources. The receipt explicitly leaves
full reactor, OpenJDK/jtreg/HotSpot and phase advancement unqualified.

The manifest's declared 93 `src` Java paths omit ten currently present main files:
`M3AdaptiveEnumLongMap`, `M3IntCollection`, `M3IntHashSet`, `M3IntIntConsumer`,
`M3IntIntHashMap`, `M3IntIterator`, `M3IntLongConsumer`, `M3IntLongHashMap`,
`M3IntPriorityQueue`, and `M3LongLinkedHashSet`; and five tests:
`M3AdaptiveEnumLongMapTest`, `M3IntLanesTest`, `M3IntLongHashMapTest`,
`M3LongLinkedHashSetTest`, and `M3ObjectResidencyRuntimePressureTest`.
The current `src` subtree is `c9dce538dc75eb2ca5fe0b048b35e5698f5c0394`, read
recursively with `truncated=false`.

[`verify_donation.py` lines 63–84][source-set-guard] first checks manifest identity
and row hashes, then compares declared Java paths with every Java path under src.
The observed **93 versus 108** sets differ: if reached with these pinned files,
that equality guard would refuse before compilation and request a successor
packet. This is a **static predicate inference**, not a claim that the verifier
was executed or that earlier prerequisites passed. The missing current-source
successor qualification remains open; no manifest, policy, source or recipe was
repaired by this documentation.

### Catalog and provenance are separate from implementation

[`JDK_FAMILY_MAP.tsv`][family-map] contains candidate/proving/union rows alongside
PENDING rows. At this pin the PENDING names `M3LongLongLinkedHashMap`,
`M3LongLongTreeMap`, `M3LongTreeSet`, `M3LongCopyOnWriteArrayList`,
`M3LongLongConcurrentSkipListMap`, `M3LongConcurrentSkipListSet`, and
`M3WeakObjectMap` are not physical production files in this package. The existence
of `M3LinkedHashMap`, `M3TreeMap`, `M3WeakHashMap` or corresponding factories does
not complete those differently named specialization rows. Similarly a JDK enum
or identity factory does not select the physical M3 proving specialization.

[`provenance.json`][provenance] preserves the original lane intake and separately
records later receiving repair; historical source/postimage seals retain their
original meaning. [`synexia-donation.json`][donation] maps the received factory and
progress owners to donor names and concrete target paths/hashes, including the
legacy `Progress` versus shared progress SPI. The current factory/progress source
hashes match their installed-manifest entries. A body/hash match is a source
identity result, not replay, contract execution or installed-image proof.

The [product-scope document][product-scope] makes full-family delivery pending and
requires concrete target implementation, module/API entry points, dependencies,
provenance, applicable native libraries and installed-image execution. The existing
[porting invariant][invariant] keeps Synexia the canonical portable recipe authority
and M3JDK the target/runtime authority. Source changes require the qualified recipe
process; this reference changes neither source seals nor receiving phase. Preserve
**String → arrays → collections → AST/compiler → remaining families**. Broader
product intent does not promote the current independent module into a qualified
JDK distribution.

## Test and member evidence

The factory has one private constructor, 58 public static methods, and no fields,
instance methods, nested types or alternate initialization path. Every overload is
listed in the three routing sections plus immutable-copy section (16 + 23 + 16 + 3).
The package descriptor contributes descriptive text only.

| Test body read | Actual assertion coverage | Limit |
| --- | --- | --- |
| [`M3FactoryContractTest.allFactoryFamiliesAreUsable`][factory-test] | Basic construction/use of generic JDK/M3 factory families; reverse-order heaps; reference-identity map behavior; null weak-map key; copy-on-write iterator snapshot; ordinary transfer offer succeeds and unmatched synchronous offer fails | Does not establish every inherited API, concrete JDK substitution equivalence, fairness, serialization, race behavior or all factory failure paths |
| `immutableAndJdkUtilityWrappers` in the same file | List/set/map mutation refusal; duplicate-set collapse; immutable entry mutation refusal; null set element/map value refusal; JDK checked/synchronized/unmodifiable tree wrappers | Does not test null source/map key, shallow payload aliasing, exact copy order or source mutation during copy |
| [`M3ReceivingRecoveryTest`][recovery-test] | Factories select the existing long list/deque owners; list/sequence identity; current small-sort invalidation; deque callback partial effects and no-op iteration behavior | Reuses [sequence reference](primitive-sequence-reference.md), not another implementation of that contract |
| [`SegmentedPrimitiveStorageTest.leanFactoryExposesStorageWithoutChoosingSemantics`][storage-test] | Seven storage factories return owners; four-lane count; empty arena/list | Other tests in the file exercise addresses/lane allocation/stale handles/storage order directly; those do not test every factory route |

The complete bodies of these three test files were read; no test was run. Direct
constructor snippets were checked for lane count, primitive capacities, tree
comparator delegation, shared-map locking, deque capacities and transfer mode.
Those bounded checks do not close the complete delegate classes. Linked-hash,
primitive map and sequence references retain their own already-reviewed scope.

Factory blob: `3d2f5aa53bda55293b3254bed3d526b896cbd044`.
Package-descriptor blob: `3998c9f78a8617aa3b593c353f7d7ac48e93a52b`.
The public package, all delegates, the current successor source set, Java SE
compatibility, full reactor and installed-runtime gates remain independently open.

[factory]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[package]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/com/m3/collections/package-info.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/pom.xml
[module-readme]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/README.md
[installed]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/qualification/installed-source-enum-set-20261014.tsv
[donation]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/synexia-donation.json
[receipt]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/qualification/receiving-enum-set-20261008.json
[source-set-guard]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/verify_donation.py#L63-L84
[family-map]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/JDK_FAMILY_MAP.tsv
[provenance]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/provenance.json
[product-scope]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/M3_COLLECTION_PRODUCT_SCOPE.md
[invariant]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/docs/m3jdk21-porting-invariant.md
[factory-test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/M3FactoryContractTest.java
[recovery-test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/M3ReceivingRecoveryTest.java
[storage-test]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/m3/collections/src/test/java/com/m3/collections/SegmentedPrimitiveStorageTest.java
