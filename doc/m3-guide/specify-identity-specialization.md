<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Reference-identity map and set specializations

[Guide](README.md) · [Linked-hash collections](collections-reference.md) · [Weak-key lifetime](specify-weak-key-lifetime.md) · [Coverage](coverage.md)

`M3ObjectIdentityMap<K,V>` and `M3ObjectIdentitySet<E>` use reference identity
(`==`) for keys/members and mixed `System.identityHashCode` for probing. The
map also compares values by identity in containsValue and conditional remove.
Equal-but-distinct objects remain different keys/members. Both owners retain
their non-null object references strongly; they are not weak collections.

Both complete source bodies were inspected at
`1011050da4205a901ad079d7711b6b8efd8a9af7` and verified unchanged at
`7abdf87cb497d0d94abeb723d70c50f856ecc381`. All examples, tests, builds,
recipes and performance measurements are **NOT_RUN in this documentation
authoring/review pass**. Hosted CI for a subsequently published documentation
commit is separate exact-commit evidence. Descriptions of loops, allocation
and failure order are source deductions, not runtime qualification.

## Actual API and factory boundary

The [map][map] and [set][set] are public final classes extending Object only.
They do **not** implement `java.util.Map`, `Set`, `Collection` or `Iterable`.
They have no entrySet/keySet/values views, iterator, spliterator, streams,
public clone, serialization implementation, public bulk-copy constructor,
typed-array conversion, adapter-to-JDK collection or content-based equals/hashCode.
Object-level equals/hashCode remain inherited from Object and do not compare
collection content. Their forEach methods do not make the classes Iterable.

Both have a no-argument constructor and an `int expectedSize` constructor.
The no-argument form delegates to zero expected size; even an empty instance
eagerly allocates its minimum-capacity array lane(s). The exported
[module][module] and [Java 21 POM][pom] are the same separately built collection
module described in the [existing reference](collections-reference.md).

Crucially, [`M3Collections.identityMap()`][factory] returns
`java.util.IdentityHashMap`, **not** M3ObjectIdentityMap. There is no factory
for either specialization in that complete factory body. Construct these
specializations directly when their narrower API is wanted. The public
[family map][family-map] labels them a receiver candidate/proving backend;
the JDK owner remains authoritative. Naming and finite differential tests
do not establish substitutability for the JDK concrete class or full interfaces.

## Storage, null and reference lifetime

The map owns parallel Object[] keys/values. The set owns one Object[] keys.
Within a lane, null is the empty-slot marker. Null membership is instead
tracked by `hasNullKey` plus `nullValue` in the map, and `hasNull` in the set.
This is a true collection-level null sidecar, unlike the weak map's in-table
null sentinel. Null contributes to size and the growth threshold, but does
not occupy a key-array slot. Null values are legal in every map position.

Non-null key lookup uses only identity hash and `==`; it never calls the
object's overridden hashCode or equals. Identity hash collisions are resolved
by linear probing. The private mixer applies xor/unsigned-shift stages and
the integer multipliers `0x7feb352d` and `0x846ca68b` to identityHashCode;
no ordering or stable cross-process hash value is promised.

Successful non-null removal back-shifts entries in the probe run and clears
the final hole (both lanes for the map). It leaves no tombstones and does not
shrink capacity. Null removal clears its sidecar. Clear nulls the full array
lanes and resets sidecars/count only when nonempty; an already-empty clear is
a no-op. There are no finalizers, resource closers or queue-driven cleanup.
Externally held references and snapshots can keep removed objects reachable.

## Complete public method surface

Shared method names have these same meanings in both owners:

| Method | Contract |
| --- | --- |
| Constructor `()` / `(int expectedSize)` | Nonnegative expected logical count; allocate power-of-two storage, minimum capacity 8 |
| `size()`, `isEmpty()` | Logical count including null sidecar |
| `capacity()` | Key-lane length, not remaining capacity or maximum live count |
| `referenceSlots()` | Map: keys.length + values.length; set: keys.length. Excludes map's null-value field and object/array overhead |
| `clear()` | Remove all mappings/members and release their retained references, keeping arrays |

Map-only surface:

| Method | Presence, identity and return rule |
| --- | --- |
| `containsKey(Object)` | Identity membership, including the null sidecar |
| `containsValue(Object)` | Identity value comparison; checks null-key sidecar first, then occupied array slots |
| `get(Object)` | Current value, or null if absent; a present null value is indistinguishable without containsKey |
| `getOrDefault(Object,V)` | Fallback only when absent; returns null for a present null value |
| `put(K,V)` | Replace existing identity's value and return old value; new mapping returns null |
| `putIfAbsent(K,V)` | get followed by put when result is null; a present null-valued mapping is replaced |
| `remove(Object)` | Remove identity mapping and return old value; null is ambiguous with absence |
| `remove(Object,Object)` | Require key presence and `get(key) == value`, remove and return true; absent or different value reference returns false |
| `forEach(BiConsumer)` | Null-key mapping first when present, then occupied key slots ascending; end-of-traversal structural check |
| `keysToArray()` | New shallow Object[] key snapshot in the same null-first/slot order |

Set-only surface:

| Method | Presence and return rule |
| --- | --- |
| `contains(Object)` | Identity membership, including null |
| `add(E)` | Add absent identity and return true; duplicate reference/null returns false |
| `remove(Object)` | Remove present identity and return true; absent returns false |
| `forEach(Consumer)` | Null member first when present, then occupied slots ascending; end-of-traversal structural check |
| `toArray()` | New shallow Object[] member snapshot, null first when present |

Map replacement is nonstructural even if the value reference changes.
PutIfAbsent therefore replaces an existing null without increasing size or
revision. Duplicate set add and absent remove are also nonstructural. New
membership, successful removal and nonempty clear increment modCount. The
source exposes no atomic compute/merge/replace or conditional-insert protocol;
these methods are not concurrent operations.

## Traversal, callback failure and snapshots

Each forEach validates consumer non-null before checking emptiness or calling
anything. It captures modCount, visits the null sidecar if present, then scans
occupied key slots. The remaining counter is initialized **after** a null-sidecar
callback. It compares modCount with the captured value only after normal
completion of the whole traversal.

Thus structural callback mutation is eventually reported with
ConcurrentModificationException if control reaches that final check, not
immediately after each callback. Earlier callback effects remain. Modified
table/size/sidecar state can affect subsequent visits before the exception;
do not rely on each original member being visited exactly once under mutation.
A consumer-thrown exception propagates before the final check and can mask it.
Value replacement on an existing map key does not change modCount, but the
traversal reads live values, not a captured value snapshot. There is no rollback.

`keysToArray` and set `toArray` allocate length size, then copy references.
Changing the returned array does not change the owner. Mutable referenced
objects remain shared and the snapshot strongly retains them. There is no
snapshot version check, deep copy or map-entry/value snapshot method. Order
is an implementation scan order, not insertion/sorted order; rehash and
backshift can change it. Concurrent mutation can make snapshots inconsistent
or fail; it is outside the supported ownership discipline.

Neither owner uses synchronization, volatile fields, CAS or locks. Use
single-threaded or externally coordinated access. End checks do not provide
safe publication, atomicity, concurrent correctness or guaranteed detection
of every race. Strong retention is distinct from the weak-map lifetime
contract documented separately.

## Sizing, allocation limits and mutation-before-growth

`capacityFor` calculates `max(8, ceil(expectedSize / (double) LOAD_FACTOR))`
with LOAD_FACTOR stored as the float literal `0.65f`, rejects a needed value
above `1L << 30`, and rounds up to a power of two. Negative expectedSize
throws IllegalArgumentException("negative expectedSize"); an oversized
request throws IllegalArgumentException("expectedSize too large") before
array allocation. Expected size is a sizing hint, not an exact array length.

The threshold is `min(C-1, max(1, (int)(C * LOAD_FACTOR)))`. New membership,
including null, is written and size/modCount are incremented **before** the
`size > threshold` growth check. Rehash doubles capacity and reinserts
non-null identities. At an existing capacity of `1 << 30`, growth instead
throws IllegalStateException("maximum hash-table capacity reached"). JVM
allocation can fail much earlier; the arithmetic ceiling is not a promise
of available memory or a fully successful insertion count.

Failure on growth is not transactionally rolled back. For a new non-null
mapping/member, initial admission already occurred. For a new null map key,
the flag/count change precedes rehash but the requested nullValue assignment
comes **after** it. A failure can therefore leave the new null key present
with its prior default-null field rather than the requested value. Rehash
also assigns replacement arrays during allocation, so arbitrary allocation
failure is not claimed to preserve a usable old table. Recoverability after
OOME is not established by the source tests.

`referenceSlots()` returns int arithmetic. At the map's theoretical
`1 << 30` lane capacity, adding the two lengths overflows a signed int;
do not treat it as a universally nonnegative byte-size or heap-budget API.
Ordinary-sized maps report twice capacity and sets report capacity. Lane
accounting excludes headers, alignment, the owner and retained objects.

Expected lookup/update work is constant with suitable identity-hash
distribution, but worst-case probing/backshift can scan O(C). ContainsValue,
forEach, array snapshot and clear scan O(C) slots (snapshot additionally
allocates O(size) references). Rehash allocates O(C) lanes; reinsertion can
be quadratic under extreme clustering. Growth temporarily retains old and
new arrays. No throughput, retained-heap or universal allocation advantage
is measured by this reference.

## Example and exact test evidence

Source-checked illustration, **NOT_RUN**:

```java
var map = new com.m3.collections.M3ObjectIdentityMap<String, Object>();
String a = new String("same");
String b = new String("same");
Object first = new Object();
map.put(a, first);
map.put(b, null);                 // distinct identity, map.size() is 2
boolean hasB = map.containsKey(b); // true despite its null value
Object fallback = new Object();
Object value = map.getOrDefault(b, fallback); // null, not fallback
boolean removed = map.remove(a, first);      // true by value identity
```

All four methods of [M3ObjectIdentityTest][test] were read, **not run**:

| Test method | Evidence designed into the test |
| --- | --- |
| `identityMapMatchesJdkAcrossChurnWithNullKeyAndValues` | 30,000 seeded operations against IdentityHashMap; distinct-but-equal key/value pool and null, identity return comparisons, membership, conditional remove, putIfAbsent, traversal membership and snapshot length |
| `identityMapEdgeContractsRehashAndFailFast` | Minimum capacity, negative sizing, equal/distinct strings, null mapping/fallback/replacement, conditional remove, null callback, structural callback failure, clear and 10,000-key growth/deletion |
| `identitySetMatchesJdkAcrossChurn` | 30,000 seeded operations against Collections.newSetFromMap(IdentityHashMap), null/duplicate identity cases, snapshot membership and structural callback failure |
| `identityOwnersRetainOnlyFlatLanes` | Exact nonstatic declared-field type lists for both owners; no full retained-heap or all-allocation proof |

The bounded module caller scan found these direct construction tests; the
factory returns the JDK identity map and does not route to these two owners.
No production adoption elsewhere in the repository is inferred from that
negative bounded result. Large-capacity overflow/OOME, callback exception
precedence, every reentrant mutation sequence, concurrent use and whole-JDK
interface compatibility are not established by these finite assertions.

Exact blobs: map `18f9277389f7fa5ab8ddbff9971bb0f941659750`,
set `6f20a64f8d7e9dc6bbc9880396c502349e2e3d7a`,
test `a2ea451fd5724e212df4c43e20776d3882d13254`.

[map]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3ObjectIdentityMap.java
[set]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3ObjectIdentitySet.java
[test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3ObjectIdentityTest.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/pom.xml
[family-map]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/JDK_FAMILY_MAP.tsv
