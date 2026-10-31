<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Primitive int and long heaps

[Guide](README.md) · [Primitive sets and int traversal](specify-primitive-set-variants.md) · [Primitive sequences](primitive-sequence-reference.md) · [Coverage](coverage.md)

`M3IntPriorityQueue` and `M3LongPriorityQueue` are growable binary min-heaps
over primitive arrays. They retain duplicates, use signed natural order, and
expose heap-array order through iteration. Repeated head removal or
`toSortedArray` produces ascending values; ordinary iteration does not sort.

This reference covers both complete owner bodies at public source revision
`33cd1758a4a4613d3868ecde31d6d83400f66aed`. Their Git blobs match the audit at
`1011050da4205a901ad079d7711b6b8efd8a9af7`. Shared int collection/iterator
contracts are in the [set reference](specify-primitive-set-variants.md);
the [sequence reference](primitive-sequence-reference.md) supplies the reused
long defaults and independent module/build boundary. Tests/examples are
**NOT_RUN**; no compiler, Maven execution, recipe verification, jtreg,
native execution, benchmark or runtime-image qualification ran for this page.

## Type, equality and ordering boundary

Both owners are public final classes in exported `com.m3.collections` of
the separate [`com.m3.collections` module][module]. The int owner implements
`M3IntCollection`; the long owner implements `M3LongCollection`. Neither
implements `java.util.Queue`, `Collection`, `Iterable`, or `M3LongQueue`.
In particular the long heap does not inherit that queue interface's `peek`,
`poll`, `tryPoll`, batch/drain or `pollReserved` methods.

There is no comparator constructor, comparator accessor, unsigned ordering,
max-heap mode, stable tie-order guarantee, boxed adapter or nullable empty
result. Primitive equality uses `==`; comparisons use `<`, `<=` and `>=`
directly, not subtraction that could overflow on signed extrema. All int/long
values, including zero and both extrema, are valid. Size identifies the live
range; zeroed unused slots are not absence sentinels for live values.

These owners do not override `equals`/`hashCode`, implement serialization or
expose public cloning. Object equality is identity, not multiset comparison.
Construct directly with no argument or an `int initialCapacity`.
[`M3Collections.longPriorityQueue()`][factory] delegates to the long no-arg
constructor. No int-priority factory exists in that source. The generic
`priorityQueue()` and `priorityQueue(Comparator)` return JDK `PriorityQueue<E>`,
not either primitive owner.

The owners are unsynchronized. Do not mutate them during traversal or access
them concurrently without an external ownership protocol. Fail-fast checks
are diagnostics, not memory ordering, locks or guaranteed race detection.

## Complete declared public method surface

The int and long method bodies are paired except for the head method names,
primitive width and the int-only payload query.

| Method | Result, mutation and refusal |
| --- | --- |
| No-arg constructor | Empty owner with shared zero-length primitive array; capacity zero |
| Constructor `(int initialCapacity)` | Negative throws `IllegalArgumentException`; zero shares empty storage; positive allocates exactly that many primitive slots |
| `offer(value)` | Grow if necessary; evaluate `siftUp(size++, value)`, incrementing size before the sift body executes; increment modification count after normal completion and return true; duplicates accepted |
| `add(value)` | Alias for `offer`, including its true result and exceptions |
| `firstInt()` / `firstLong()` | Return root/minimum without mutation; empty throws `NoSuchElementException` |
| `removeFirstInt()` / `removeFirstLong()` | Remove and return one minimum, zero vacated final slot, repair heap downward, increment modification count; empty throws `NoSuchElementException` |
| `remove(value)` | Linear scan of heap-array positions; remove one first matching position and return true; absence returns false unchanged |
| `contains(value)` | Linear scan of live positions using primitive equality |
| `size()` | Current live value count, including duplicates |
| `capacity()` | Backing primitive-array length, not a maximum accepted count or byte count |
| `clear()` | Nonempty: zero live range, reset size, increment modification count and retain capacity; empty: no-op |
| `toArray()` | Fresh exact-size copy of the live heap-array range, in heap order |
| `toSortedArray()` | Copy through `toArray`, sort the copy ascending with `Arrays.sort`, return it without changing this queue or its modification count |
| `iterator()` | New owner-bound primitive cursor scanning live heap-array positions with fail-fast checks |
| Int owner only: `payloadBytes()` | `(long) capacity() * Integer.BYTES`; long owner has no corresponding public method |

There are no public indexed get/set/remove operations, arbitrary priority
updates, trim, reserve, `ensureCapacity`, bulk collection constructors or
collection views. `remove(int)` on the int heap removes a value, not an index.
Because duplicates have the same primitive bits, the implementation does not
identify a particular insertion among tied values. An absent removal or empty
clear does not invalidate a cursor; every successful offer does, including an
offer equal to an existing value.

## Heap repair, costs and capacity

The root occupies index zero. A node's parent is `(index - 1) >>> 1`; children
start at `(index << 1) + 1`. Upward repair shifts parents while the inserted
value is smaller. Downward repair selects the smaller child, choosing the left
when child values tie, and stops when the replacement is no larger. These
mechanics maintain the min-heap relation, not globally sorted array order.

Arbitrary value removal takes the last live value, zeroes its old slot and
decrements size. If it is replacing an interior position, it sifts upward
when smaller than the parent; otherwise it sifts downward. Removing the last
position needs neither. Head removal uses downward repair only. No user
comparator/callback executes during heap maintenance.

Head lookup uses one root access; offer/head removal perform work along a heap
path apart from growth. `contains` and value removal search up to the live
count, and value removal can then repair a path. Array export copies the live
range; sorted export also invokes a primitive sort. These are source-level
work descriptions, not measured throughput, latency or allocation-rate claims.

When growth is necessary, the private `ensureCapacity` selects:

- From zero: at least eleven slots, or the requested minimum if larger
- From a positive capacity below 64: old capacity twice plus two
- From 64 or above: old capacity plus half, rounded down
- At the large-array threshold: `hugeCapacity` chooses between
  `Integer.MAX_VALUE - 8` and `Integer.MAX_VALUE`

Ordinary growth also respects a larger required minimum. The private
`hugeCapacity` checks a negative minimum and can throw `OutOfMemoryError`.
There is no public capacity limit, checked-add overflow contract, bounded-offer
false result or guarantee that any giant allocation succeeds. The offer path
computes `size + 1` using int arithmetic; do not treat the helper's guard as a
proof covering every integer-limit state. Construction or growth can fail
because the VM cannot allocate the array. No strong resource-failure recovery
contract was tested or established here.

Growth copies backing storage. Removal and clear zero discarded primitive
slots but never shrink allocation; no trim operation exists. Array length,
not live size, determines retained primitive payload. The int-only
`payloadBytes` excludes headers, alignment, scalar fields, snapshots, cursors
and old arrays; it is not whole-object or retained-heap accounting. Neither
owner retains a wrapper/node per live primitive, but arrays, iterators, streams
and caller-selected boxing can allocate. There is no native handle or
close/free protocol. Zeroing numeric slots is not a GC-root release mechanism
or secure-erasure guarantee.

## Traversal, materialization and callbacks

Each `iterator` captures the current modification count. Both `hasNext` and
`nextInt`/`nextLong` check it first; a mismatch throws
`ConcurrentModificationException`, including when the old cursor would
otherwise be exhausted. Without mutation, exhaustion throws
`NoSuchElementException`. The iterator has no `remove`, and is not a JDK boxed
iterator. Reading the head, `contains`, array export and sorted export do not
increment the count.

The inherited collection methods are `isEmpty`, `forEachInt`/`forEachLong`,
the primitive spliterator, sequential stream and parallel-mode stream. Neither
heap overrides the spliterator. Consequently both use a captured fail-fast
iterator, cannot split, always estimate `Long.MAX_VALUE`, and advertise only
`NONNULL`. They advertise neither `SORTED` nor `ORDERED`, `SIZED`, `SUBSIZED`
or `DISTINCT`. Heap-array order is already not priority order; parallel mode
adds no sorting or splitting. A sequential source traversal follows heap
indices, but clients should not infer a declared stream order characteristic.

The stream method creates its iterator/spliterator immediately, before lazy
terminal consumption. A later structural mutation invalidates that cursor;
this differs from the concurrent set's immutable captured array. `toArray`
and `toSortedArray` return detached materializations, but neither is an atomic
snapshot against racing writes to an unsynchronized owner.

`forEachInt`/`forEachLong`, iterator `forEachRemaining` and fallback
spliterator `tryAdvance` reject null consumers even when empty/exhausted.
They fetch and advance before invoking the callback. A callback exception
propagates with that element consumed and prior effects retained. Mutating the
owner in a callback is unsupported interference; if the callback returns,
the next cursor check detects an actual count change. There is no rollback or
post-callback check inside a single fallback `tryAdvance`. See the
[int defaults](specify-primitive-set-variants.md#int-collection-and-iterator-defaults)
and [long defaults](primitive-sequence-reference.md#iterators-spliterators-and-streams-are-different-boundaries)
for the shared implementation boundary.

## Source-checked examples, NOT_RUN

```java
var queue = new com.m3.collections.M3IntPriorityQueue(2);
queue.offer(3);
queue.offer(1);
queue.offer(1);                      // duplicates retained; capacity can grow
int minimum = queue.firstInt();      // 1, does not remove
boolean removed = queue.remove(1);   // true, removes one occurrence
int[] sorted = queue.toSortedArray(); // [1, 3], queue unchanged
int first = queue.removeFirstInt();  // 1
int second = queue.removeFirstInt(); // 3
// queue.firstInt() and removeFirstInt() now throw NoSuchElementException.

var longs = com.m3.collections.M3Collections.longPriorityQueue();
longs.add(Long.MAX_VALUE);
longs.add(Long.MIN_VALUE);
long smallest = longs.removeFirstLong(); // Long.MIN_VALUE
```

## Exact source and test evidence

Both owner files and both broad test files were read completely. The relevant
assertions are identified rather than treating all methods in a broad suite
as heap coverage. Hashes belong to the inspection revision above.

| Source | Git blob | Inspected scope |
| --- | --- | --- |
| [`M3IntPriorityQueue.java`][int-heap] | `3ee7233f27105d326832e212b01b8da17142569e` | All methods, fields, iterator body, capacity and sift helpers |
| [`M3LongPriorityQueue.java`][long-heap] | `2fb403549f2baf0afe5b04743647c26ebd4cce80` | All methods, fields, iterator body, capacity and sift helpers |
| [`M3IntLanesTest.java`][int-test] | `b617f579eecc6f48078076c642be33438eea9481` | `priorityQueueMatchesJdkHeadContentsAndRemoval`: finite 20,000-step `PriorityQueue<Integer>` comparison, generated zero/extrema, periodic sorted contents, drain/empty exceptions, negative capacity, initial capacity zero, alias add and invalidation, stream sum; `intLanesRetainOnlyPrimitiveFields`: includes int heap |
| [`M3JdkShapedPrimitiveCollectionsTest.java`][long-test] | `9a6710e2985676613778895dd42c8bb9d08e8534` | `priorityQueueMatchesJdkHeadContentsAndRemoval`: finite 20,000-step `PriorityQueue<Long>` comparison of offer/head removal/value removal/contains/head, size and sorted content every step; heap case of `iteratorsAreFailFastOnStructuralMutation` |

All are **NOT_RUN**, not passing receipts. Source review establishes the API
differences, capacity formulas, iterator checks and absence of comparator/
boxed queue surfaces. The listed tests do not exhaust exact capacity thresholds,
no-op invalidation, callbacks, source flags, all exhaustion paths, maximum-array
arithmetic, allocation failure or racing mutation. Finite JDK-oracle comparisons
and field-type reflection do not establish concrete JDK substitutability,
serialization, performance, memory-use qualification or installed delivery.

These two complete owner bodies are additional heap documentation. Int
collection/iterator and existing long traversal/factory dependencies are not
counted again here. Other collection families remain in the
[coverage ledger](coverage.md).

[int-heap]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntPriorityQueue.java
[long-heap]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongPriorityQueue.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/module-info.java
[int-test]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3IntLanesTest.java
[long-test]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/test/java/com/m3/collections/M3JdkShapedPrimitiveCollectionsTest.java
