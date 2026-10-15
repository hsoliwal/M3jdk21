<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Primitive set variants and int traversal

[Guide](README.md) · [Primitive heaps](specify-primitive-heaps.md) · [Primitive sequences](primitive-sequence-reference.md) · [Primitive maps](primitive-map-reference.md) · [Coverage](coverage.md)

The four primitive sets have different storage and traversal contracts.
`M3IntHashSet` stores zero separately and closes deleted probe gaps;
`M3LongHashSet` uses occupancy bytes and tombstones; `M3LongLinkedHashSet`
adds encounter-order links; `M3ConcurrentLongSet` publishes immutable sorted
arrays. None is an implementation of `java.util.Set`.

This page reviews the complete bodies of those four owners, `M3IntCollection`
and `M3IntIterator` at public source revision
`33cd1758a4a4613d3868ecde31d6d83400f66aed`. Their Git blobs are unchanged from
the collection audit at `1011050da4205a901ad079d7711b6b8efd8a9af7`.
The long collection/iterator defaults, factory and standalone build boundary
reuse the [sequence](primitive-sequence-reference.md) and
[map](primitive-map-reference.md) references. Tests and examples are
**NOT_RUN**. No Java/Maven execution, recipe verifier, jtreg, native test,
benchmark or installed-image qualification was performed for this page.

## API and ownership boundary

All six declarations are public in the exported `com.m3.collections` package
of the independently built [`com.m3.collections` module][module]. The
[POM][pom] declares `com.m3:m3-collections:0.1.0-SNAPSHOT`, Java release 21,
compiler 3.13.0 (`-Xlint:all,-module`, `-Werror`), Surefire 3.2.5 with test
module-path use disabled, and the exact test dependency
`org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`. Reading this configuration
does not establish dependency resolution or an installed runtime artifact.

The owners implement `M3IntCollection` or `M3LongCollection`, not `Collection`,
`Set`, `SequencedSet`, `SortedSet` or `Iterable`. They do not supply content-based
`equals`/`hashCode`, Java serialization, public cloning, bulk set algebra or
boxed collection adapters. Equality of owner objects remains `Object` identity.
Membership compares primitive values exactly. All signed values, including zero
and both extrema, are valid; null is not a primitive element. Passing a null
boxed argument through Java unboxing fails before the primitive call.

Construct each owner directly. The three hash owners offer no-arg and
`int expectedSize` constructors; the concurrent owner only has a no-arg
constructor. [`M3Collections.longHashSet()`][factory] returns a new
`M3LongHashSet` with default sizing. There is no corresponding int-set,
primitive linked-set or concurrent-long-set factory in the inspected factory.
The generic `hashSet()` and `linkedHashSet()` return `M3LinkedHashSet<E>`;
they do not select these primitive implementations.

| Owner | Storage and iteration/export order | Mutation boundary |
| --- | --- | --- |
| [`M3IntHashSet`][ints] | `int[]` plus zero-presence flag; zero first when present, then ascending table-slot index | Unsynchronized; modification-count checked iterator |
| [`M3LongHashSet`][longs] | `long[]` and `byte[]` states; ascending used-slot index | Unsynchronized; modification-count checked iterator |
| [`M3LongLinkedHashSet`][linked] | Long key/state arrays and two int link arrays; current first-to-last encounter order | Unsynchronized; membership changes and actual order moves invalidate iterators |
| [`M3ConcurrentLongSet`][concurrent] | One sorted `long[]` immutable after publication; ascending signed-long order | Acquire reads, whole-array CAS publication, captured-snapshot traversal |

Hash-slot order is neither sorted nor insertion order and can change after
rehashing. None accepts a comparator or an unsigned-order mode. Fail-fast is
not synchronization or a guarantee of detecting every cross-thread race.
Use the mutable owners only with single-threaded or externally coordinated
ownership. The concurrent owner does not make a sequence of calls transactional.

## Int collection and iterator defaults

[`M3IntCollection`][int-collection] declares `size()`, `contains(int)`,
`clear()`, `iterator()` and `toArray()`. It deliberately does not declare
`add` or `remove`; use a concrete owner when those operations are needed.
Its five default methods are:

| Default | Result and effect |
| --- | --- |
| `isEmpty()` | `size() == 0` |
| `forEachInt(consumer)` | Reject null before acquiring an iterator, even when empty; repeat `hasNext`, `nextInt`, callback |
| `intSpliterator()` | Capture an iterator now; wrap it without an element-array copy |
| `intStream()` | Create a sequential primitive stream from that spliterator |
| `parallelIntStream()` | Create a parallel-mode stream from the same source mechanism; does not supply splitting itself |

The fallback spliterator's `tryAdvance` rejects null even when exhausted,
returns false on normal exhaustion, otherwise consumes one primitive value
before invoking the action. `trySplit()` always returns null,
`estimateSize()` always returns `Long.MAX_VALUE` even after exhaustion, and
`characteristics()` returns exactly `NONNULL`. It does not report `DISTINCT`,
`ORDERED`, `SIZED`, `SUBSIZED`, `SORTED`, `IMMUTABLE` or `CONCURRENT`.
Thus an int set's actual zero-first/slot traversal is not an advertised stream
encounter-order guarantee. Parallel mode does not turn this source into a
partitioned array or establish a speedup.

[`M3IntIterator`][int-iterator] declares only `hasNext()` and `nextInt()` and
adds `forEachRemaining(IntConsumer)`. That default rejects null before looking
for another element and repeatedly checks, consumes and invokes. It has no
`remove`, boxed `next` or `java.util.Iterator<Integer>` inheritance.
The set and heap implementations throw `NoSuchElementException` on ordinary
exhaustion; the interface itself supplies no implementation of `nextInt`.

For both callback loops and fallback `tryAdvance`, the current value is already
consumed if the callback throws. Its exception propagates; previous callback
effects are not undone. Mutation of a nonconcurrent owner is unsupported
interference. If a callback structurally mutates it and returns normally,
the next cursor check detects a changed modification count. A throwing callback
escapes before that next check. A single `tryAdvance` has no separate
post-callback modification check.

The long defaults in [`M3LongCollection`][long-collection] and
[`M3LongIterator`][long-iterator] behave analogously. The two mutable long sets
inherit that unsplittable fallback unchanged. The concurrent set overrides
`longSpliterator`, as described below. Stream consumption is lazy, but these
methods create their cursor/snapshot at stream construction time, not at the
terminal operation. Calling `.boxed()` or a boxing consumer is a caller-selected
allocation boundary; the primitive source does not itself create a wrapper
per element. Cursor, stream and array allocations still exist.

## Common mutable-set methods

These rows cover the declared shared methods of the three mutable hash owners.

| Method | Contract |
| --- | --- |
| Constructors | Empty logical set, minimum eight slots; reject negative expected size with `IllegalArgumentException`; expected size is a sizing hint, not a bound |
| `add(value)` | Insert a previously absent value and return true; duplicate returns false without a modification-count change or growth |
| `remove(value)` | Remove a present value and return true; absence returns false without mutation |
| `contains(value)` | Read membership with exact primitive equality |
| `size()` | Live-member count |
| `capacity()` | Backing key-array length, also the other slot-array lengths; not live count or byte count |
| `clear()` | Clear live contents and relevant presence state, retaining capacity; already empty/clean state is a no-op |
| `toArray()` | New caller-owned primitive array of exactly the live members in that owner's traversal order |
| `iterator()` | New primitive cursor bound to this owner and its modification count |

`M3IntHashSet.payloadBytes()` is `capacity * Integer.BYTES`.
`M3LongLinkedHashSet.payloadBytes()` is
`capacity * (Long.BYTES + Byte.BYTES + 2 * Integer.BYTES)`, or seventeen bytes
per slot of primitive array payload. `M3LongHashSet` has no `payloadBytes()`
method. These formulas exclude object/array headers, alignment, scalar fields,
old arrays, exports and cursors; they are not retained-heap measurements.

Both `hasNext` and `nextInt`/`nextLong` check the captured `modCount` before
examining position. New membership, successful removal and a non-no-op clear
increment it. Duplicate `add`, absent removal and empty clear do not.
Rehashing does not separately increment it. Without mutation, the next method
throws `NoSuchElementException` after exhaustion. These iterators have no removal
operation, and the finite int counter is not an unlimited generation token.
`toArray` is detached, but it is not an atomic snapshot against racing mutation
of an unsynchronized owner.

### Int zero sidecar and backward-shift deletion

The int array uses zero for an empty slot. A separate `hasZero` records zero
membership; `size` includes that member. Zero is therefore not forbidden or
an absence return sentinel. Iteration and `toArray` emit it first if present.
Nonzero lookup mixes the value, masks by `capacity - 1`, and probes until the
value or an empty slot. Removal shifts later keys backward when their probe
path crosses the gap; no tombstone array or persistent deleted marker exists.
Removing zero only clears the sidecar and adjusts counters.

Initial capacity is the next power of two at least
`max(8, ceil(expectedSize / (double) 0.65f))`. The literal is a float, not exact
decimal 0.65. A required capacity above `1 << 30` is rejected with
`IllegalArgumentException`. The threshold is
`min(capacity - 1, max(1, (int) (capacity * 0.65f)))`.
After an insertion changes membership and counters, `size > threshold`
triggers a doubling rehash; the size test includes a present zero.
At maximum capacity that rehash throws `IllegalStateException`.

This ordering matters: an insertion can have already taken effect before a
growth error. The page does not promise rollback on allocation failure or
maximum-capacity failure. Rehash creates a replacement key array and reinserts
nonzero keys; zero remains in its sidecar. Clear fills the key array with zeros
and resets size/presence. Deletion and clear never shrink storage; no public
reserve/trim/compact method is declared.

### Long occupancy, tombstones and compaction

The ordinary and linked long sets use byte states `EMPTY = 0`, `USED = 1` and
`DELETED = 2`. All long bit patterns fit in key slots. `size` counts used slots;
`occupied` counts used plus tombstoned slots. Search passes deleted slots and
stops at an empty slot. Insertion remembers the first tombstone and uses it
after finding the end of the probe run. Tombstone reuse leaves `occupied`
unchanged; use of a previously empty slot increases it.

Initial capacity rounds `ceil(expectedSize * 3 / 2)` to a power of two, minimum
eight. A required capacity above `1 << 30` is rejected. A new insertion first
checks whether `(occupied + 1) * 3 > capacity * 2`, using long arithmetic for
the comparison, and doubles/rebuilds when needed. This happens before actual
tombstone reuse, so a new value can cause growth even if a deleted slot exists.
At maximum capacity, required growth throws `IllegalStateException` before
inserting that new member. Duplicate `add` exits before the capacity check.

Removal marks the slot deleted, zeros its long value and decrements size.
Removing the last member clears all occupancy bytes and resets `occupied`.
Otherwise, if `occupied > size * 2` and capacity exceeds eight, removal rebuilds
at the same capacity. This removes tombstones, not capacity. Clear resets key
and state arrays, size and occupied; neither operation shrinks the arrays.
Internal rehash guards throw `IllegalStateException` if live count changes.
Rehash assigns newly allocated arrays in separate steps; no strong
allocation-failure recovery guarantee is established.

Hashing does not guarantee worst-case constant work. Probe chains can be linear;
rehash and ordinary `toArray` visit capacity-sized storage. Primitive storage
avoids retained per-entry nodes/wrappers in these owners, but growth/compaction
allocates new arrays. Zeroing primitive values is not a GC-root-release or
secure-erasure guarantee. Java reachability controls array reclamation, with
no explicit close/free operation or prompt-GC promise.

## Linked long-set sequencing

The linked owner adds `previous[]`, `next[]`, `first` and `last` table-slot
indices. `-1` is a link/end marker, not a reserved long value. Ordinary `add`
appends a new member, while adding an existing member does not move it.

| Additional declared method | Contract |
| --- | --- |
| `addFirst(value)` | Prepend an absent value, or move a present value to the current front; void result |
| `addLast(value)` | Append an absent value, or move a present value to the current back; void result |
| `first()`, `last()` | Current front/back value without removal; `NoSuchElementException` when empty |
| `removeFirst()`, `removeLast()` | Remove and return the corresponding endpoint; same empty exception |
| `toReversedArray()` | New detached array from current last to first; not a live reversed view |
| `payloadBytes()` | Primitive lane formula above |

An actual move of an already-present value increments `modCount`, even though
size and membership stay unchanged. Moving a value already at the requested
endpoint is a no-op. Unlink/relink changes adjacent indices; successful removal
then applies the same tombstone cleanup rules as the ordinary long set.
Growth and same-capacity compaction walk the old encounter-order chain and
append into the rebuilt table, preserving current order.

The class-level Javadoc says `addFirst`/`addLast` move a present key “to the end”.
The `addFirst` method body and its own comment explicitly move it to the front;
this page follows those bodies. Likewise, after explicit moves, `first` and
`last` mean current endpoints, not immutable oldest/newest insertion timestamps.
The reversed-array comment's `SequencedSet.reversed()` comparison does not
create that interface or a mutable reversed view. There is no descending iterator.
Its inherited stream still advertises only `NONNULL`, despite traversing the
linked encounter order in the fallback cursor.

## Concurrent long set: immutable arrays and CAS retries

The concurrent owner starts with a shared empty array in a volatile field.
Its `current()` helper uses `VarHandle.getAcquire`. Each published array is
strictly increasing under signed-long natural order and is never modified
after publication by this class. `contains` binary-searches one captured array;
`size` returns one captured length; inherited `isEmpty` calls `size`.
The static initializer resolves the `values` field's VarHandle; a caught
`ReflectiveOperationException` is wrapped in `ExceptionInInitializerError`.
No fallback publication mechanism is supplied if that initialization fails.

| Declared method | Effect against the snapshot examined in that attempt |
| --- | --- |
| Constructor | Empty set; no populated array allocation |
| `add(value)` | If present, false without copy/CAS; otherwise allocate one-long-larger array, copy prefix/suffix around sorted insertion, CAS-publish and return true |
| `remove(value)` | If absent, false without copy/CAS; for one member CAS to shared empty; otherwise allocate one-long-smaller array and copy around the removed value, then CAS-publish |
| `contains(value)`, `size()` | Acquire-read one current immutable array; exact membership / length |
| `clear()` | Empty returns immediately; otherwise retry CAS to shared empty |
| `toArray()` | Clone one captured array, including an empty array; caller mutation cannot affect set/traversal |
| `iterator()` | Capture one array, then visit its values without mod-count checks or recapture |
| `longSpliterator()` | Capture one array and delegate to the JDK primitive array spliterator; no element-array copy |

A failed compare-and-set restarts from a fresh array and repeats search/copy.
A retry can return false if another writer already established the requested
presence/absence. Successful publication atomically replaces the complete set
state. A reader can observe either complete state, not a partly shifted array.
Separate `contains`, `size`, `isEmpty` and `toArray` calls can see different
states; there is no compound check-then-add transaction, bulk mutation, public
CAS method, custom comparator or explicit capacity control.

A duplicate/absent result is about the observed attempt, not a promise that the
value stays present/absent after return. `clear` may race with subsequent adds;
it does not lock out other writers. Sharing the empty sentinel does not mutate
old snapshots. There is no explicit lock in these methods, but writer retries
have no fixed bound. No fairness, wait-free completion, allocation-free write,
bounded latency or general lock-free progress qualification is claimed.
Binary search has logarithmic comparison work; a copying attempt is linear
in snapshot length, and contention can repeat that cost and discard candidates.
There is no explicit maximum-cardinality guard comparable to the hash owners;
array allocation and Java integer limits remain resource boundaries.

### Snapshot traversal, stream flags and lifetime

The iterator captures once, does not throw `ConcurrentModificationException`
for later writes, and throws `NoSuchElementException` after its captured array
is exhausted. Inherited `forEachLong` therefore also traverses one snapshot.
Callback writes affect later set states, not that traversal; callback exceptions
still propagate without undoing effects or restoring the iterator position.

The `longSpliterator` call supplies `ORDERED | DISTINCT | IMMUTABLE` to
[`Spliterators.spliterator(long[], int)`][jdk-spliterators]. That array helper
adds `SIZED | SUBSIZED`; this path does not report `SORTED`, `CONCURRENT` or
`NONNULL`. Actual values are sorted, but the `SORTED` flag is not advertised.
The helper splits a remaining prefix at its midpoint, leaving the original
on the suffix, and shares the captured array without copying it.

At this pin, the helper's `tryAdvance` rejects null even when exhausted and
increments its index before the action. Its optimized `forEachRemaining`
sets the cursor to its fence before invoking the remaining actions. If an
action throws there, a caller reusing that same spliterator cannot recover
the unread suffix through it. This bulk behavior differs from the custom
primitive iterator's item-by-item `forEachRemaining`. Neither path rolls back
callback side effects. A reported `ORDERED` source does not require concurrent
parallel callbacks to execute in order; choose the terminal operation accordingly.

`longStream` and `parallelLongStream` capture through this override when called.
Clear or replacement before a terminal operation does not alter their values.
The spliterator/iterator retains an old snapshot while reachable; long-lived
traversals can retain old arrays beside the current array and failed write
candidates. `toArray` adds a separate payload copy. Primitive storage and
copy-free traversal do not imply zero total allocation or measured GC savings.

## Source-checked examples, NOT_RUN

```java
var set = new com.m3.collections.M3IntHashSet();
set.add(7);
set.add(0);
boolean duplicate = set.add(0);      // false
int first = set.iterator().nextInt(); // 0, zero sidecar comes first

var linked = new com.m3.collections.M3LongLinkedHashSet();
linked.add(10);
linked.add(20);
var oldCursor = linked.iterator();
linked.addFirst(20);                 // order [20, 10], size unchanged
// oldCursor.hasNext() now throws ConcurrentModificationException.
long[] reversed = linked.toReversedArray(); // [10, 20], detached

var snapshots = new com.m3.collections.M3ConcurrentLongSet();
snapshots.add(4);
snapshots.add(-2);
var stream = snapshots.parallelLongStream();
snapshots.clear();
long[] earlier = stream.toArray();   // [-2, 4]
```

## Exact evidence and remaining limits

All six primary bodies, the long collection/iterator defaults, factory, module,
POM and the four test files below were read. The JDK helper's array factory and
`LongArraySpliterator` body were inspected as a dependency; it is not counted
as another M3 collection owner. Hashes identify source bytes, not passing tests.

| Primary source | Git blob |
| --- | --- |
| [`M3IntCollection.java`][int-collection] | `bd6aff905cf47e64ccd76bc19acc899ef93742af` |
| [`M3IntIterator.java`][int-iterator] | `366f92e9da842e00e346872f386ecfede38c5cb6` |
| [`M3IntHashSet.java`][ints] | `e4272ab154175d115873d38f305a8fdf380d1f7d` |
| [`M3LongHashSet.java`][longs] | `1fe9ac1bfa11cf6a626a749c37d64ccec0ed09df` |
| [`M3LongLinkedHashSet.java`][linked] | `28c864a69a384dbfe6fbc499eff7da4015264a3f` |
| [`M3ConcurrentLongSet.java`][concurrent] | `1dd5331b42efa855a51ba31d40380b0064231983` |

| Test file / Git blob | Relevant assertions read, all NOT_RUN |
| --- | --- |
| [`M3IntLanesTest`][int-test] / `b617f579eecc6f48078076c642be33438eea9481` | `hashSetMatchesJdkAcrossChurnIncludingZeroAndRehash`: finite 30,000-step `HashSet<Integer>` comparison, zero/extrema, snapshots, sorted stream and payload formula; `hashSetEdgeContractsAndFailFast`: sizing, negative input, duplicate zero, exhaustion, invalidation and 10,000-key growth/removal; `intLanesRetainOnlyPrimitiveFields`: int-set/map/heap field-type predicate |
| [`M3JdkShapedPrimitiveCollectionsTest`][shaped-test] / `9a6710e2985676613778895dd42c8bb9d08e8534` | `hashSetMatchesJdkAcrossCollisionsDeletionAndRehash`: finite 30,000-step long-set comparison and periodic content exports; set case of `duplicateSetAddAndMapValueReplacementAreNonStructural`; set case of `iteratorsAreFailFastOnStructuralMutation` |
| [`M3LongLinkedHashSetTest`][linked-test] / `a4202b54eeb03aa662101a8c3d3c57e0c9a96e2e` | `matchesJdkSequencedSetAcrossChurnIncludingMovesAndRehash`: 40,000-step JDK `LinkedHashSet` comparison, moves/endpoints, periodic forward/reverse exports, final stream and payload; `edgeContractsFailFastAndSizing`: empty endpoints, negative sizing, extrema/moves, exhaustion, removal invalidation, 10,000 insertions followed by two-thirds deletion, surviving order and retained 16,384 capacity; `retainsOnlyPrimitiveLanes`: exact instance-field types |
| [`M3ConcurrentLaneContractTest`][concurrent-test] / `86f5ae48e363b14a00e62ed6fd6d74fe572cc2c3` | `concurrentCollectionStreamsPreserveCapturedSnapshotsAndIndexOrder`: add 2,001 descending nonnegative members, capture a parallel set stream, clear the owner and compare stream output with ascending `0..2000` |

The concurrent test's multithreaded `race` helper is used for atomic lane
tests, not the long set. It is not evidence of competing set writers,
linearizability, retry fairness or allocation-failure behavior. The set stream
test does not cover every sentinel/extreme, no-op or cursor exception path.
Other methods in those broad files concern maps, heaps or lanes and are not
counted as set tests. A reflection assertion is not a heap/GC benchmark.

Maximum allocation, resource-failure recovery, all no-op/order-move iterator
combinations, callback exception combinations, counter overflow and broad
concurrency qualification remain unexecuted. No complete JDK replacement,
serialization parity, package-wide acceptance or runtime delivery follows.
See the [heap reference](specify-primitive-heaps.md) for the other int-collection
owner and the [coverage ledger](coverage.md) for the wider collection work.

[int-collection]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntCollection.java
[int-iterator]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntIterator.java
[ints]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntHashSet.java
[longs]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongHashSet.java
[linked]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongLinkedHashSet.java
[concurrent]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ConcurrentLongSet.java
[long-collection]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongCollection.java
[long-iterator]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongIterator.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/pom.xml
[jdk-spliterators]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/src/java.base/share/classes/java/util/Spliterators.java#L1161-L1256
[int-test]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3IntLanesTest.java
[shaped-test]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3JdkShapedPrimitiveCollectionsTest.java
[linked-test]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3LongLinkedHashSetTest.java
[concurrent-test]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3ConcurrentLaneContractTest.java
