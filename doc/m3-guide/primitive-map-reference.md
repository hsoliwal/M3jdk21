<!-- SPDX-License-Identifier: Apache-2.0 -->

# Primitive and snapshot long-map reference

[Guide](README.md) · [Package map](m3-packages.md#package-3) · [Linked-hash maps](collections-reference.md) · [Coverage](coverage.md)

`M3LongLongHashMap` and `M3ConcurrentLongMap` both store primitive long keys and
values, but their return values, mutation rules and traversal lifetimes differ.
This reference covers those two complete owner bodies, `M3LongIterator`,
`M3LongLongConsumer` and the relevant `M3Collections` factory. It deepens one
existing package, not the other collection families or a JDK replacement.

Source inspection pin: `c038343521587d96c9fa190715ce2f4b9f8c490a`,
14 October 2026. All implementation and test files named in the evidence section
were read in full. Examples and tests are **NOT_RUN**: no compiler, Maven build,
recipe/policy verifier, jtreg, native execution or benchmark ran for this pass.
Complexity and allocation descriptions follow the source, not measurements.

## Choose the contract before the representation

Both classes are public final classes in the independently built
[`com.m3.collections` module][module]. Neither implements `Map<Long, Long>` or
`ConcurrentMap<Long, Long>`. They do not inherit those interfaces' default
operations, entry views, equality or serialization contracts. The ordinary
public JDK collection owners remain unchanged.

| Question | `M3LongLongHashMap` | `M3ConcurrentLongMap` |
| --- | --- | --- |
| Storage | Mutable parallel `long[]` key/value arrays and a `byte[]` occupancy array | One interleaved `long[]` of key/value pairs, immutable after publication |
| Key lookup | Mixed hash, power-of-two mask and linear probing | Binary search in signed-long key order |
| `put(k, v)` returns true | Only if `k` was absent and is inserted | If `k` is inserted or its value changes |
| Existing key, different value | Updates value; returns false | Publishes replacement snapshot; returns true |
| Existing key, same value | Writes the same value; returns false | Returns false without copying or publishing |
| Traversal order | Physical table-slot order; not sorted or insertion order | Ascending key order using `Long.compare` |
| Traversal under mutation | Owner-bound, structurally fail-fast | Captured immutable snapshot; later writes do not change it |
| Thread boundary | Unsynchronized mutable owner; fail-fast is not synchronization | Acquire reads and compare-and-set publication of complete snapshots |
| Construction | Empty constructor, expected-size constructor, factory | Empty constructor only; no corresponding factory in the inspected `M3Collections` |

Create the mutable owner with `new M3LongLongHashMap()`,
`new M3LongLongHashMap(expectedSize)`, or
[`M3Collections.longLongHashMap()`][factory]. The factory delegates to the empty
constructor; it does not select the concurrent map. Construct the snapshot owner
with `new M3ConcurrentLongMap()`.

These are separate proving-ground APIs. The [porting invariant][policy] keeps
reusable collection mechanics under their canonical ownership and requires
per-contract target evidence before JDK backend promotion. Documentation does
not advance the ordered receiving phase or change source, recipes or licenses.

## Shared values and absence rules

Keys and values are exact primitive `long` values. Zero, negative numbers,
`Long.MIN_VALUE` and `Long.MAX_VALUE` are representable. The mutable map's
occupancy bytes, and the snapshot map's array length and key positions, encode
presence independently of the key/value bits. There is no reserved missing key,
missing value or null value.

`getOrDefault(key, fallback)` returns the stored value when present, including
when that value equals `fallback`. Otherwise it returns the caller's fallback.
It does not insert. Use `containsKey` when presence matters; comparison with a
fallback cannot establish absence.

For the concurrent owner, separate `containsKey` and `getOrDefault` calls can
observe different snapshots. That pair is not an atomic presence/value query.
Use one captured traversal or `toInterleavedArray()` when several observations
must refer to one snapshot. This API does not expose a conditional replace,
conditional remove, `compute`, `merge` or transaction operation.

## Mutable hash map: operation contracts

The complete implementation is [`M3LongLongHashMap`][mutable].

| Operation | Result and effect |
| --- | --- |
| `put(key, value)` | True for a newly inserted key; false for any replacement. A false result can still change the stored value |
| `getOrDefault`, `containsKey` | Probe until an empty slot or matching used key; tombstones do not stop the search |
| `containsValue(value)` | Scan used slots, comparing primitive values with `==`; ignore empty/deleted slots |
| `replace(key, expectedValue, newValue)` | True exactly when the key exists with the expected value; write the new value. True also when expected and new values are equal |
| `remove(key)` | True exactly when an existing mapping is deleted; false has no effect |
| `remove(key, expectedValue)` | Delete only a matching key/value pair; false for absence or value mismatch |
| `size()`, `isEmpty()` | Report the current live-entry count |
| `capacity()` | Number of key slots, equal to value/state-array lengths; not bytes or live-entry count |
| `clear()` | Zero payload and state arrays and reset counters, retaining allocated capacity; an already empty, clean table is a no-op |
| `keysToArray()` | Fresh caller-owned array of live keys in current physical-slot order |
| `keyIterator()` | New primitive key cursor tied to this owner and its structural modification count |
| `forEach(consumer)` | Key/value callbacks over used slots in ascending physical index order |

These value comparisons are not atomic compare-and-set operations. In
particular, the name `replace` and its expected-value argument do not make this
mutable class safe for racing updates.

### Capacity, tombstones and reuse

The minimum capacity is eight. The expected-size constructor rejects negative
arguments with `IllegalArgumentException`. For nonzero expected size, it rounds
`ceil(expectedSize * 3 / 2)` up to a power of two, with a minimum of eight; it
rejects a required capacity above `1 << 30` with `IllegalArgumentException`.
This is initial sizing, not a maximum number of accepted entries. Accepted
sizing can still fail because the VM cannot allocate the arrays.

Each slot has one of three states: empty, used or deleted. `size` counts live
mappings; `occupied` counts slots that are used or tombstoned. A lookup walks
past deleted slots. A new insertion remembers the first deleted slot in the
probe sequence and reuses it after reaching an empty slot. Reusing a tombstone
does not increase `occupied`; inserting into an empty slot does.

Before probing for a new slot, `put` checks whether `(occupied + 1) * 3` exceeds
`capacity * 2`. If so, it doubles the table and rehashes live mappings. This check
happens before tombstone reuse, so a new insertion can grow even when a deleted
slot could otherwise be reused. Replacement of an existing key bypasses it.
At capacity `1 << 30`, an insertion requiring growth throws
`IllegalStateException` rather than doubling again.

Deletion clears that slot's key and value to zero and marks it deleted. If it
removed the last entry, all states become empty and `occupied` becomes zero.
Otherwise, when `occupied > size * 2` and capacity exceeds eight, deletion
rehashes at the **same capacity** to remove tombstones. Neither deletion nor
`clear` shrinks the table, and this class has no trim operation. A rehash
allocates three replacement arrays; old arrays can be reclaimed once unreachable.
Its internal size-consistency guard throws `IllegalStateException` if rehashing
fails to preserve the live-entry count.

These details explain capacity retention and probe cleanup. They do not promise
constant-time worst-case operations: a collision/probe chain can be linear in
table capacity, and `containsValue`, traversal and key export scan table slots.
No strong failure-atomicity or recovery guarantee for allocation failure is
established here; in particular, rehash assigns its new arrays in separate steps.

### Replacement versus structural mutation

New-key insertion, successful deletion and a non-no-op `clear` increment
`modCount`. Value replacement by `put` or successful `replace` does not. Failed
removal/replacement does not increment it either. Rehashing does not separately
increment it; the triggering structural operation does.

`keyIterator()` captures the modification count. Both `hasNext()` and
`nextLong()` check it before doing their work and throw
`ConcurrentModificationException` if it differs. With no structural change,
`nextLong()` throws `NoSuchElementException` once exhausted. Replacing a value
does not invalidate a key iterator. This cursor has no `remove` operation and
is not a `java.util.Iterator<Long>`.

[`M3LongIterator.forEachRemaining`][iterator] rejects a null `LongConsumer`
before checking whether any keys remain, then repeats `hasNext`, `nextLong`
and the callback. The cursor advances before the callback runs. If the callback
throws, that key remains consumed; the exception propagates without rollback.
If it structurally mutates the map and returns normally, the next iterator
check detects the changed count. Detection is not a correctness mechanism for
unsynchronized cross-thread access or a promise to detect every possible race.

### Callback order, exceptions and partial effects

[`M3LongLongConsumer`][consumer] accepts two primitive longs. Both maps pass key
first and value second; they do not construct a `Map.Entry` for each callback.
The mutable map's `forEach` rejects a null consumer, even for an empty map.
It checks for structural modification **after each successful callback**.
Therefore a callback that inserts or deletes leaves that mutation in place and
then triggers `ConcurrentModificationException`; traversal is not transactional.

If the callback itself throws, its exception escapes before the post-callback
modification check. Its earlier effects remain. Nonstructural value replacement
is allowed by this check, so later callbacks can see changed values; the
traversal is not a frozen copy. Do not rely on a stable key order across
insertion, deletion or rehashing.

## Concurrent map: immutable snapshots and publication

The complete implementation is [`M3ConcurrentLongMap`][concurrent]. Its private
volatile reference starts at a shared empty array. A populated snapshot has
`[key0, value0, key1, value1, ...]` layout, with strictly increasing keys under
signed `Long.compare`: negative keys precede zero and positive keys. There is
no custom comparator, unsigned order or insertion-order mode.

`size`, `isEmpty`, `containsKey` and `getOrDefault` each acquire the current
array. Lookup performs binary search on pair indices. Once captured, the array
is never modified by this class, so key/value observations from that snapshot
cannot mix different published states.

| Write | Work before successful publication |
| --- | --- |
| `put`, absent key | Allocate an array two longs longer and copy the prefix/suffix around the sorted insertion position |
| `put`, existing key with different value | Clone the whole array and change the clone's value slot |
| `put`, identical mapping | Return false without an array copy or compare-and-set |
| `remove`, absent key | Return false without copying or publication |
| `remove`, one remaining pair | Compare-and-set the reference to the shared empty array |
| `remove`, multiple pairs | Allocate an array two longs shorter and copy around the removed pair |
| `clear`, nonempty | Compare-and-set to the shared empty array; no replacement payload array is allocated |
| `clear`, empty | Return immediately |

A changed write returns only after its compare-and-set succeeds. If another
writer won first, it recaptures the current array, searches again and retries.
A retry may allocate another candidate array; the failed candidate is discarded.
The result is decided against the snapshot examined in that attempt, so a
racing writer can make a retry return false if the requested value is already
present or the key is already absent. Each successful publication replaces the
whole map state atomically; consecutive API calls are still separate operations.

Reads do not take a lock or retry loop. Writes contain no explicit lock but can
retry without a fixed bound; this source does not provide fairness, wait-free
writer completion or a latency bound. Copying and allocation costs still apply.
Lookup is logarithmic in snapshot entry count; a copying write attempt is
linear, and contention can multiply that work. No universal performance gain,
resource bound or full concurrency qualification is claimed.

### Traversal captures once

`forEach` rejects a null consumer, captures one snapshot and invokes callbacks
in that snapshot's signed-key order. Later writes, including writes from its
own callback, do not alter the captured pairs. There is no mutable-map
modification-count check. A callback exception stops traversal and propagates;
completed callbacks and any map changes they made are not undone. This method
does not copy an array or create per-entry objects, but callback code can allocate.

`toInterleavedArray()` clones one captured snapshot. The result is a detached,
caller-mutable copy with length twice the entry count, including a fresh empty
array for an empty map. Modifying it cannot change the owner or an existing
traversal. There is no public method to adopt that mutable array as backing
storage, no live entry view and no manual snapshot-release API.

### Key and value streams

`keyStream()` and `valueStream()` capture their snapshot **when the method is
called**, before a terminal operation. They return sequential primitive
`LongStream`s; calling `parallel()` enables splitting without recapturing the
owner. Clearing or replacing the map before consumption does not change their
contents. A key stream and a value stream created by separate calls may capture
different states under concurrent mutation; do not zip them as an atomic pair
snapshot. Use `forEach` or `toInterleavedArray` for paired observations.

Both streams encounter entries in ascending key order. Values are not sorted
by value and may repeat. The internal `PairLane` spliterator reports
`ORDERED | SIZED | SUBSIZED | NONNULL | IMMUTABLE`, plus `DISTINCT` for keys only.
It does **not** report `SORTED`, even for the ordered keys, or `CONCURRENT`.
These flags describe the captured traversal, not the mutability of the map.

Splitting creates a new prefix cursor over half the remaining pair-index range
and leaves the original on the suffix. Both share the same snapshot; no key,
value or entry payload array is copied for the split. Stream construction and
splitting do create cursor/stream machinery, so “no payload copy” is not a
zero-allocation promise.

`PairLane.tryAdvance` rejects a null action even when exhausted, advances its
cursor before invoking the action, and propagates callback exceptions. If a
caller directly operates on the spliterator and catches such an exception, the
failed callback's element has already been consumed. Encounter order does not
make parallel callback execution sequential; callers needing ordered effects
must select the appropriate stream terminal operation.

A stream or traversal retains its captured array while that array remains
reachable through it, independently of newer map states. A slow reader can
therefore prolong an old snapshot's lifetime. Multiple live snapshots and failed
CAS candidates can increase memory pressure; source layout alone is not a
retained-heap measurement. These owners contain Java heap arrays, not native
handles requiring explicit close/free.

## Small source-checked examples

The following method bodies illustrate the inspected calls and expected
sequential outcomes. They were not compiled or executed.

```java
import com.m3.collections.M3Collections;
import com.m3.collections.M3ConcurrentLongMap;
import com.m3.collections.M3LongLongHashMap;

static void mutableReturnAndPresence() {
    M3LongLongHashMap map = M3Collections.longLongHashMap();
    boolean inserted = map.put(Long.MIN_VALUE, 0L); // true
    boolean replaced = map.put(Long.MIN_VALUE, 7L); // false, but value changed
    boolean matched = map.replace(Long.MIN_VALUE, 7L, 7L); // true
    long absentOrZero = map.getOrDefault(4L, 0L); // 0, key is absent
    boolean present = map.containsKey(4L); // false
}

static void snapshotReturnAndLifetime() {
    M3ConcurrentLongMap map = new M3ConcurrentLongMap();
    map.put(5L, 50L);
    map.put(-2L, 20L);
    boolean changed = map.put(5L, 51L); // true
    boolean unchanged = map.put(5L, 51L); // false
    var keys = map.keyStream();
    long[] pairs = map.toInterleavedArray(); // [-2, 20, 5, 51]
    map.clear();
    long[] earlierKeys = keys.toArray(); // [-2, 5]
    pairs[1] = 99L; // modifies only the detached copy
}
```

## Evidence and remaining coverage

The source/test Git identities below belong to the inspection pin above. A read
or a hash match is not a passing execution result. The three initially relevant
broad tests were checked rather than assumed to cover these owners by name.

| Complete file read | Git blob | Relevant coverage in its source |
| --- | --- | --- |
| [`M3LongLongHashMap.java`][mutable] | `b1d85622c2e19db069d0b2a64fb8011b5c01be34` | Full mutable owner, probes, resizing, tombstones, traversal and failures |
| [`M3ConcurrentLongMap.java`][concurrent] | `bbe25dfd008fe143806c8c3aefac6d33e413aec2` | Full snapshot owner, publication, copy boundaries and pair-index streams |
| [`M3LongIterator.java`][iterator] | `44778a63d99c5b1f99adee536b954a4ebb8c64f3` | Primitive cursor and default callback loop |
| [`M3LongLongConsumer.java`][consumer] | `b1c01d1afacc76bd338179398ab42835e16992fa` | Two-long functional callback |
| [`M3Collections.java`][factory] | `3d2f5aa53bda55293b3254bed3d526b896cbd044` | Mutable factory delegation; no concurrent-long-map factory |
| [`M3JdkShapedPrimitiveCollectionsTest.java`][shaped-test] | `9a6710e2985676613778895dd42c8bb9d08e8534` | Direct mutable-map differential and iterator tests described below |
| [`M3ConcurrentLaneContractTest.java`][snapshot-test] | `86f5ae48e363b14a00e62ed6fd6d74fe572cc2c3` | Direct map-stream capture/order assertions described below |
| [`M3PrimitiveCollectionDifferentialTest.java`][primitive-test] | `7c641d69f04cebbdca2e7fdb6226396a2e98a736` | List/deque assertions; no direct use of either map |
| [`M3ConcurrentContractTest.java`][concurrent-test] | `a9e57f112d5ca4e224d3776fc49e4afdf3a62df1` | Locked maps and queue/deque assertions; no direct use of either map |
| [`M3FactoryContractTest.java`][factory-test] | `e7e156e6d73d178b1bf6f2ee32deb1db5bc046c6` | Other factory/wrapper assertions; does not call `longLongHashMap` or construct the concurrent long map |
| [`M3ReceivingRecoveryTest.java`][recovery-test] | `1c5f24f32807b408de25e73080640cdb5fef5206` | List/deque receiving assertions; no direct use of either map |

The relevant direct assertions are narrower than full acceptance:

- `hashMapMatchesJdkAcrossReplacementDeletionAndRehash` describes a deterministic
  30,000-step comparison with boxed `HashMap` for insertion reporting, removal,
  lookup, presence, matching replacement, size and periodic `forEach` contents.
  Its value generator includes zero, both signed extremes and negative values.
- `duplicateSetAddAndMapValueReplacementAreNonStructural` checks that a map value
  replacement returns false, keeps the key cursor usable and changes the value.
  `iteratorsAreFailFastOnStructuralMutation` checks map insertion invalidating
  `hasNext`.
- `concurrentCollectionStreamsPreserveCapturedSnapshotsAndIndexOrder` populates
  map keys 2000 down to zero, captures key/value streams, clears the owner and
  asserts the earlier ordered contents through parallel consumption. Its map
  portion is sequential setup and snapshot use, not a racing-writer proof.
  The other methods' atomic lane races do not qualify this map's CAS loop.

Remaining explicit gaps include dedicated tests for mutable conditional remove,
capacity limits/cleanup, callback exception precedence, iterator exhaustion and
copy detachment; concurrent put return cases, signed extremes, callback
mutation/failure, paired-snapshot isolation, competing writers, retry behavior
and resource pressure; and exact-head execution of all applicable tests.
These are gaps in this bounded evidence set, not a claim that no other test
exists elsewhere. Allocation-failure recovery, CPU/retained-heap/native-memory
measurements and JDK runtime promotion remain unestablished. Lists, deques,
sparse lanes, trees, residency and native integration are outside this reference.

[module]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/module-info.java
[policy]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/docs/m3jdk21-porting-invariant.md
[mutable]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongLongHashMap.java
[concurrent]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3ConcurrentLongMap.java
[iterator]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongIterator.java
[consumer]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongLongConsumer.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[shaped-test]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3JdkShapedPrimitiveCollectionsTest.java
[snapshot-test]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3ConcurrentLaneContractTest.java
[primitive-test]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3PrimitiveCollectionDifferentialTest.java
[concurrent-test]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3ConcurrentContractTest.java
[factory-test]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3FactoryContractTest.java
[recovery-test]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3ReceivingRecoveryTest.java
