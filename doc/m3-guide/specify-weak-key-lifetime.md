<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Weak-key map lifetime and API reference

[Guide](README.md) · [Linked-hash and shared map APIs](collections-reference.md) · [Identity specialization](specify-identity-specialization.md) · [Coverage](coverage.md)

`M3WeakHashMap<K,V>` is an equality-based, weak-key `Map`, implemented over
open-addressed reference/value/hash/state arrays. It is not an identity map.
Non-null keys are weakly referenced; values are strongly referenced. A value
that directly or indirectly retains its key prevents the intended reclamation.

This page reviews the complete [owner][owner] and the relevant shared
[slot-map][slot-map] and [capacity][arrays] bodies at
`1011050da4205a901ad079d7711b6b8efd8a9af7`, unchanged at
`7abdf87cb497d0d94abeb723d70c50f856ecc381`. Tests, examples, builds, recipes,
GC experiments and benchmarks are **NOT_RUN in this documentation
authoring/review pass**. Hosted CI for a subsequently published documentation
commit is separate evidence and must be reported for that exact commit.
Source deductions below are not Java SE replacement qualification or measured
memory/performance results.

## Construction, module and actual factory

The only public constructor is `M3WeakHashMap()`. It creates an empty owner
with shared zero-length lanes and its own `ReferenceQueue`; first insertion
allocates table lanes. [`M3Collections.weakMap()`][factory] returns this class.
The class extends package-private `M3SlotMap`, which extends `AbstractMap`.
Consequently the map interface is real inheritance, unlike the separate
`M3ObjectIdentityMap` specialization's deliberately narrower API.

The [module][module] exports `com.m3.collections` with implicit `java.base`
only. The [POM][pom] targets Java 21 and declares the separately built
`com.m3:m3-collections:0.1.0-SNAPSHOT` artifact. Use the [module guide][guide]
for build/receiver instructions; its historical results are not results of
this source review. There is no public capacity/load-factor/copy constructor,
public clone, `Serializable` implementation, raw-lane accessor, compact/trim
method, close operation or native adapter in this owner.

## Equality and the null key

Lookup evaluates the query key's `hashCode`, spreads it with `h ^ (h >>> 16)`,
then probes candidates with the cached hash and
`Objects.equals(queryKey, storedReferent)`. Keep hash/equality stable while a
key is stored. Equal-but-distinct keys address the same mapping; replacing a
value through an equal key does **not** replace the original weak reference.
Keeping only an equal replacement key alive does not keep that original key alive.

The null key is stored using a strongly held `NULL_KEY` sentinel **inside a
normal table slot**. It is not a weak reference or a separate null-key sidecar.
It contributes to `size` and occupancy and remains until explicit removal or
clear. Null values are legal. `get(k) == null` and `remove(k) == null` cannot
distinguish absence from a present null value; use `containsKey`, or conditional
removal when its boolean result is needed.

There is no insertion, sorted or access order. Traversal scans live slots in
ascending physical index; rehash can change that order. No sequenced endpoints,
reversed map or navigable/range-map view is supplied by this class.

## Reachability, queue draining and reuse

The retained lanes are `Object[] references`, `Object[] values`, `int[] hashes`
and `byte[] states`. State `0` means never occupied, `1` occupied and `2` a
tombstone. Every non-null-key insertion allocates a `KeyReference`, a
`WeakReference<Object>` with an integer slot. This is an unavoidable object
in this implementation, despite the absence of retained key/value entry nodes.
The null sentinel needs no per-key weak-reference object.

`drain()` polls references already enqueued by the JVM. It deletes a mapping
only if the reference's slot is nonnegative and the slot still contains that
exact reference. Explicit erase clears the reference and sets its recorded
slot to `-1`. Rehash updates surviving reference slot numbers. These checks
prevent an obsolete queue record from deleting a later occupant of a reused
slot. Erase clears both reference and value lanes and leaves a tombstone.

| Entry point | Cleanup performed |
| --- | --- |
| `expungeStaleKeys()` | Drains the queue only; does not request or await GC |
| `size()` and inherited `isEmpty()` | Drains before reading the count |
| Key lookup, including get/contains/remove/put | `findSlot` drains before evaluating the hash |
| Initial slot traversal | `firstSlot` drains, then scans for a live referent |
| Advancing an existing slot traversal | `nextSlot` skips non-live referents but does not drain the queue itself |
| Rehash during insertion | Drops entries whose referents are no longer live, even if not yet drained |
| Nontrivial `clear()` | Invalidates weak references, clears lanes/counts, then drains obsolete queued records |

A cleared but not yet enqueued reference can therefore remain counted by
`size()` while live traversal already skips it. Neither `size` nor an explicit
drain is a promise that every unreachable key has been discovered. Do not use
this map as a deterministic eviction clock, resource closer or memory quota.
Values can stay retained until a relevant cleanup operation removes their slots.

GC-driven erase does not increment the structural revision. Explicit insertion,
successful explicit erase and clear of nonzero occupancy do. Replacement of an
existing value does not. A table with only tombstones can have size zero but
nonzero occupancy, so clearing it still changes the revision. Already-never-
occupied clear returns immediately. Cleanup is not a background thread.

## Public map operations and callback order

The general [shared map reference](collections-reference.md#map-reads-writes-equality-and-nulls)
describes `M3SlotMap`; here its access hook is the inherited no-op, and traversal
is weak, unordered and capacity-scanning. The following rules apply to this owner:

| Methods | Result and effect |
| --- | --- |
| `size`, `isEmpty`, `containsKey`, `get`, `getOrDefault` | Queue-aware count/lookup; a present null is returned instead of the fallback |
| `containsValue` | Live-slot scan with `Objects.equals(queryValue, storedValue)`; value equality, not identity |
| `put` | Existing key: return old value and replace it without structural revision; missing key: insert and return null |
| `putAll` | Exact self argument is a no-op; otherwise uses `source.forEach(this::put)`; failure can leave earlier writes applied |
| `putIfAbsent` | Inserts if absent or replaces a present null; returns the previous value |
| `remove(key)` / `remove(key,value)` | Remove matching mapping; conditional form compares values by equality and returns success |
| `replace(key,value)` / `replace(key,old,new)` | Affect only present mappings; conditional form compares the old value by equality |
| `computeIfAbsent` | Callback only for absent/null-valued mapping; non-null result is put; null result does not remove an existing null mapping |
| `computeIfPresent` | Callback only for present non-null value; null result erases the saved slot, otherwise writes it; see the GC/slot boundary below |
| `compute` | Callback receives current value or null; null result removes if present, non-null result is put |
| `merge` | Requires non-null incoming value and function; absent/null old value takes incoming value without invoking the function; null combined result removes |
| `forEach` / `replaceAll` | Visit live slots; replaceAll writes each callback result, including null |
| `equals`, `hashCode`, `toString` | Direct live-slot traversal; ordinary map equality/value equality, summed key/value hash XOR, and map text with direct self-reference handling |
| `clear`, `expungeStaleKeys` | Explicit removal versus queue-only cleanup, as above |

Null callback rejection occurs before lookup. `merge` validates its function,
then incoming value, then looks up the key. Compute/merge capture the revision
before calling user code and check it after normal callback return, before the
outer result is applied. `forEach` checks after each visited callback;
`replaceAll` checks before writing that callback's result. A structural callback
change raises `ConcurrentModificationException`; the callback's own writes
remain, and earlier replaceAll writes remain. If user code throws, that failure
propagates before the later revision check. There is no rollback or atomic
multi-entry transaction. GC cleanup does not itself trip these checks.

`computeIfPresent` retains a slot number across its callback and, for a non-null
result, calls `writeAt` directly after the revision check. It does not revalidate
that the saved slot is live. An equal-but-distinct query key need not retain
the originally stored key. If that original referent is reclaimed and the
callback invokes a queue-draining map operation, GC cleanup can erase the
saved slot without changing the revision. The outer write can then put the
result into a tombstone value lane rather than restore a live mapping, while
returning that result. This is a source-derived, **unexecuted** boundary; no
GC timing reproduction or general callback/GC atomicity is claimed. Avoid
depending on reentrant cleanup during these callbacks, and keep the original
stored key strongly reachable when its mapping must survive the operation.

Empty key lookups still invoke non-null key hashing. A missing-key `put` hashes
once during lookup, may allocate/rehash, then hashes again before insertion.
An existing-key replacement only performs the lookup hash. A second-hash
exception can leave an allocated or rebuilt table without adding the requested
mapping. This observable order is covered by the focused footprint test below;
it is not a general guarantee for hostile or reentrant hash/equality code.

## Backed views, live entries and iterator lifetime

`keySet`, `values` and `entrySet` each create a backed view. View clear and
supported removals update the map. Key-set removal uses presence rather than
the removed value, so it correctly reports removal of null-valued mappings.
Entry membership/removal uses key lookup and value equality. View insertion is
unsupported; inherited bulk algorithms are not transactional. See the
[shared views discussion](collections-reference.md#views-iterators-and-entry-lifetimes)
for the common implementation boundary.

Every iterator captures a revision and slot cursor. `hasNext`, `next` through
`hasNext`, and `remove` check the revision. `next` throws
`NoSuchElementException` on exhaustion; `remove` requires one preceding
successful next not already removed, otherwise `IllegalStateException`.
The revision check comes before those state/exhaustion failures. Successful
iterator removal refreshes its own expected revision.

Weak lifetime changes these otherwise familiar operations:

- `hasNext` prepares and strongly holds the next live key in `heldKey`
- `next` transfers that key into `lastKey`; the iterator retains the most
  recently returned key until a subsequent next replaces it, iterator removal
  clears it, or the iterator itself becomes unreachable
- `hasNext` also executes a reachability fence for the preceding `lastKey`
- Even a **values iterator** has these key-holding fields; it is not a
  key-retention-free traversal
- Unheld keys may disappear between calls without a concurrent-modification
  exception. These iterators are not fixed snapshots

An entry iterator allocates a fresh `LiveEntry` holding the key strongly and
the enclosing map. `getValue` resolves the key again; `setValue` resolves and
updates it. A removed key gives null from getValue and
`IllegalStateException("entry no longer present")` from setValue. Removing
and reinserting an equal key can make an old entry resolve the new mapping.
Entries are not stable slot-generation handles. Holding an entry can keep its
key, the map, and therefore its strongly retained values alive.

Inherited view `toArray` operations copy references. Key arrays keep copied keys
strongly reachable; entry arrays contain connected live entries, not frozen
key/value pairs; value arrays keep values alive and can indirectly retain keys.
The weak owner exposes no detached-entry snapshot endpoint. The shared
package-private `snapshot` helper is not a new public weak-map method.
Typed arrays follow the [JDK collection conversion][abstract-collection]
rules, including null destination/type errors and a trailing null in an oversized
destination. Views supply inherited collection/set traversal adapters, not the
ordered M3 adapter used by linked lists. Do not infer a stable order or a stable
size under reclamation from a stream or spliterator.

All mutable fields are unsynchronized. Fail-fast checks, reference queues and
GC cooperation do not provide thread safety. Coordinate access externally,
including views and reads that drain the queue. No concurrent/lock-free map
contract is provided.

## Capacity, costs and failure boundaries

First insertion allocates four lanes of capacity 16. Before a later insertion,
`occupied + 1 >= C - C / 3` triggers rehash. The table doubles if
`size + 1` also reaches that threshold; otherwise it rebuilds at the same
capacity to remove tombstones. Rehash moves slot positions and reuses surviving
KeyReference objects. Doubling at `C >= 1 << 30` throws
`OutOfMemoryError("hash index capacity exhausted")`. Actual VM allocation can
fail earlier. Removal and clear retain allocated capacity.

For capacity C, lane payload is `2*C` reference slots plus `5*C` primitive
bytes, excluding headers, alignment, the queue, reference objects, owner and
payload objects. This is representation accounting, not retained-heap measurement.

Lookup/ordinary update has expected constant probing under suitable hashes,
plus queued cleanup work; a collision cluster can require O(C) probes. Direct
traversal, value search and full clear scan O(C) lanes. A rehash allocates/zeros
O(C) storage and reinserts live hashes; pathological collisions can make
reinsertion quadratic. Queue cleanup is proportional to polled references.
Views, iterators, live entries, arrays and stream machinery allocate. The
owner avoids retained structural map-entry nodes; it does not eliminate all
objects, temporary storage, caller boxing or allocation failures.

## Evidence, callers and remaining boundaries

The [factory][factory] is the inspected production caller. A bounded scan of
the collection module also found standalone allocation/cost probes and the
tests below; this is not a repository-wide caller audit. The factory's weak
smoke test is about this owner; its identity smoke test exercises a different,
JDK `IdentityHashMap` result.

| Existing source, read but NOT_RUN | Exact evidence relevant here |
| --- | --- |
| [M3MapContractTest][map-test] `differential` | 30,000 seeded operations for the weak case against WeakHashMap, with every key strongly held; checks map results/equality/hash, deliberately does not require weak iteration order |
| Same test, `backedViewsAndCallbacks` | Backed removal/setValue, iterator state/fail-fast, callback mutation with preserved side effect, replaceAll and clear |
| Same test, `weakCleanupIsDeterministicAndDoesNotDeleteReusedKeys` | Reflectively clears/enqueues the stored reference, drains it, preserves null mapping, reinserts the same key and exercises growth/clear; this is deterministic queue manipulation, not a GC timing test |
| [M3FootprintContractTest][footprint-test] | Empty lookup hashing, first/second-hash failure and recovery, independent empty owners |
| [M3NoRetainedStructuralEntryTest][entry-test] | Includes weak owner and nested types in field/type-name guard; its fresh-entry identity bursts are for hash/tree, not a separate weak-entry runtime proof |
| [M3FactoryContractTest][factory-test] | `weakMap()` supports a null key |
| [M3AllocationProbe][allocation-probe] | Prepared existing weak-key get only; no result is claimed here, and it does not cover new weak-reference allocation |

Real GC scheduling, retained entries/iterators under GC, equal-key replacement
lifetime, callback interference, arbitrary collision/reentrancy, exhaustive
default-method aliasing, OOME recovery and Java SE completeness remain broader
than these finite tests. No claim of deterministic eviction or leak freedom follows.

Exact owner blob: `5d7eb065aeca2cecc1d939532d154c87bbb01c2c`.
Shared bodies: `M3SlotMap` `a7d20300717661c26897f0b69e1e5a83400a62ad`,
`M3SlotArrays` `e337c69710c871f8caa77a1afe53149f1fd705cc`.

[owner]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3WeakHashMap.java
[slot-map]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotMap.java
[arrays]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotArrays.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/pom.xml
[guide]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/README.md
[map-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3MapContractTest.java
[footprint-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3FootprintContractTest.java
[entry-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3NoRetainedStructuralEntryTest.java
[factory-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3FactoryContractTest.java
[allocation-probe]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3AllocationProbe.java
[abstract-collection]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/src/java.base/share/classes/java/util/AbstractCollection.java
