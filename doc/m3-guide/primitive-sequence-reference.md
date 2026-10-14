<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Primitive long list and deque reference

[Guide](README.md) · [Package map](m3-packages.md#package-3) · [Linked-hash collections](collections-reference.md) · [Primitive maps](primitive-map-reference.md) · [Coverage](coverage.md)

`M3LongArrayList` is a mutable indexed sequence and tail-based stack.
`M3LongArrayDeque` is a mutable circular deque and head-based FIFO queue.
Both store primitive `long` payloads without one retained wrapper per element.
Their similarly named operations, traversal rules and capacity controls are
different; choose the contract before choosing the storage.

This reference inspects public source at
`c038343521587d96c9fa190715ce2f4b9f8c490a` on 14 October 2026. It adds complete
body review of the two owners, six public interfaces and the package-private
list spliterator. It reuses the already reviewed primitive iterator and factory
boundary. Existing source Javadocs already document many of these rules; this
page joins them to inherited methods, storage behavior and actual test assertions.
It does not replace the source contracts or document every collection family.

Examples and tests are **NOT_RUN**. No compiler, Maven build, recipe/policy
verifier, jtreg, JNI/native test or benchmark ran for this documentation pass.
Source paths and hashes establish inspected bytes, not executed behavior,
performance, installed-image delivery or JDK backend qualification.

## Types, factories and compatibility boundary

The [module descriptor][module] exports `com.m3.collections` and declares no
additional required module beyond implicit `java.base`. The [standalone
POM][pom] declares `com.m3:m3-collections:0.1.0-SNAPSHOT`, Java release 21,
compiler plugin 3.13.0 with `-Xlint:all,-module` and `-Werror`, and Surefire
3.2.5 with module-path tests disabled. Its test dependency is exactly
`org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`; dependency resolution was
not verified here. Recipe profiles are authoring/proof tooling, not runtime
dependencies of these classes.

| Surface | Role in this reference |
| --- | --- |
| [`M3LongCollection`][collection] | Size, membership, clear, primitive iterator, array export, primitive traversal and stream defaults |
| [`M3LongSequence`][sequence] | Mutable indexed operations, `insert`/`removeValue` names and capacity/trim contract |
| [`M3LongList`][list-interface] | Extends sequence; adds `add(int,long)`, `remove(long)`, sorting/search/dedup and explicit growth; supplies sequence aliases |
| [`M3LongStack`][stack] | `push`, `peek`, `pop`; implemented by the list at its tail |
| [`M3LongQueue`][queue] | `offer`, `peek`, `poll`, capacity, array batch/drain and polling defaults |
| [`M3LongDeque`][deque-interface] | Extends queue with both ends, occurrence removal, indexed reads, filter and descending traversal |
| [`M3LongArrayList`][list] | Public final dense `long[]` owner implementing list and stack |
| [`M3LongArrayDeque`][deque] | Public final ring `long[]` owner implementing deque |
| [`M3LongListSpliterator`][split] | Package-private owner/index descriptor used by the list, not another exported public type |

Use `new M3LongArrayList()`, `new M3LongArrayList(initialCapacity)` or
`M3LongArrayList.copyOf(long[])` for the list; use
`new M3LongArrayDeque()` or `new M3LongArrayDeque(expectedSize)` for the deque.
The four [primitive factory overloads][factory] are
`M3Collections.longArrayList()`, `longArrayList(int)`, `longArrayDeque()` and
`longArrayDeque(int)`. They delegate directly to the matching constructors.
The object factories `arrayList()` and `arrayDeque()` return JDK `ArrayList`
and `ArrayDeque`; they are not aliases for the primitive owners.

Neither primitive class implements `java.util.Collection`, `List`, `Deque` or
`Iterable`. `M3LongIterator` is its own primitive cursor, not
`java.util.Iterator<Long>`, and has no `remove` operation. These classes do
not supply content-based `equals`/`hashCode`, a public clone contract or Java
serialization merely because their methods are JDK-shaped; object equality
remains inherited from `Object`. The list also has no sublist/list-iterator
surface, and the deque does not implement `M3LongStack`.

Both owners are unsynchronized. Use them under a single-threaded or externally
coordinated ownership discipline; fail-fast checks are not memory ordering,
race safety or guaranteed concurrent-modification detection. See the existing
[collection reference](collections-reference.md) for the wider factory and
storage boundary, and the [map reference](primitive-map-reference.md) for the
separate mutable/snapshot map contracts.

The [native README][native-readme], [JDK-shaped backend guide][backend] and
[product scope][product] distinguish separately built source from installed
runtime delivery. The [porting invariant][policy] keeps reusable mechanics and
recipes under canonical ownership, preserves per-file licensing and requires
target evidence in the order String → arrays → collections → AST/compiler.
This guide changes no source, manifest, recipe, receiving phase or public JDK API.

## Values, order and empty results

All `long` bit patterns are valid payloads, including zero and both signed
extrema. Size, not a special value, distinguishes empty from nonempty. Indices
are zero-based logical positions; deque indices count forward from the head,
independent of ring wrap. Search and membership compare exact primitive values.

| Operation | List/stack | Deque/queue |
| --- | --- | --- |
| Append | `add(value)` returns true; `push(value)` returns void | `addLast(value)` returns void; `offer`/`offerLast` return true on success |
| Read `peek()` | Last element, leaving it in place | First element, leaving it in place |
| Consume | `pop()` removes the last element | `poll()` removes the first element |
| Empty `peek`/consume | `NoSuchElementException` | `NoSuchElementException` |
| Nonthrowing empty poll | No queue default on the list | `tryPoll(target,index)` returns false after validating the destination |
| Traversal | Logical index order | Head to tail, or tail to head for `descendingIterator` |

Primitive `poll` and `peek` do not return null or a zero sentinel. This differs
from the nullable empty results of corresponding JDK reference-queue methods.
The deque is growable: an offer grows storage or throws when necessary, and
returns true on success. It is not a bounded queue that returns false when full.

## List method contracts

The following covers the complete declared public method surface of
[`M3LongArrayList`][list] plus list/sequence aliases. Shared collection defaults
are listed separately below. Bounds and null checks precede the corresponding
owner mutation unless a callback has already performed its own effects.

| Method | Result, mutation and refusal |
| --- | --- |
| Constructors | Empty logical list. No-arg/zero capacity uses shared empty storage; positive capacity allocates that many slots. Negative capacity throws `IllegalArgumentException` |
| `copyOf(source)` | Copies all values into a new owner; does not retain the input array. Null throws `NullPointerException` |
| `add(value)` | Appends, grows if needed, increments size/modification count, returns true |
| `add(index,value)`; inherited `insert(index,value)` | Inserts before the current element or appends at `index == size`; shifts the suffix. Requires `0 <= index <= size`, otherwise `IndexOutOfBoundsException`; returns void |
| `addIf(value,predicate)` | Rejects null predicate; invokes it once before adding. False leaves the owner's add undone and returns false; true delegates to `add`. Callback exceptions propagate |
| `addAll(values)` | Rejects null; empty array returns false without mutation. Otherwise appends a copy of all values, increments modification count once and returns true. `Math.addExact(size,values.length)` can throw `ArithmeticException` before growth |
| `get(index)` | Returns the current value at `0 <= index < size`; invalid index throws `IndexOutOfBoundsException` |
| `set(index,value)` | Same bounds; returns the replaced value and changes that slot without changing size or modification count |
| `removeAt(index)` | Same bounds; returns removed value, shifts the suffix left, zeroes the vacated tail and increments modification count |
| `remove(value)`; inherited `removeValue(value)` | Removes the first equal value and returns true; absent value returns false without mutation. `remove(long)` is value-based, not index-based |
| `contains(value)` | True exactly when a matching value exists |
| `indexOf(value)`; `lastIndexOf(value)` | First/last matching logical index, or `-1` when absent |
| `sort()` | Sorts the logical range in signed natural order; always increments modification count, even for empty/already-sorted data |
| `binarySearch(value)` | Delegates to `Arrays.binarySearch` over the logical range. Caller must supply sorted order; found index or `-(insertionPoint)-1` when absent. No first/last duplicate-selection promise |
| `deduplicateSorted()` | Caller supplies sorted order. Removes adjacent repeats in place, preserves remaining order, zeroes removed tail and returns the count removed. Increments modification count only if removal occurs; does not sort or globally deduplicate unsorted data |
| `filterInPlace(predicate)` | Keeps matching values in encounter order; returns number removed on normal completion. Null is rejected. Exception/partial-effect rules are below |
| `push(value)` | Tail append through `add`, with void result |
| `peek()`; `pop()` | Tail read / tail removal and returned value; empty throws `NoSuchElementException`. `pop` uses `removeAt`, including zeroing and iterator invalidation |
| `size()`; `capacity()` | Logical element count / allocated backing-array length |
| `ensureCapacity(minimumCapacity)` | Grows when needed without changing logical contents or modification count. Negative request throws `OutOfMemoryError`; ordinary allocation failures can also propagate |
| `trimToSize()` | Shrinks backing length to size only when excess capacity exists; empty trim uses shared empty array. Actual shrink increments modification count; no-op trim does not |
| `clear()` | Nonempty: zeroes logical range, sets size to zero and increments modification count. Empty: no-op. Retains capacity |
| `toArray()` | Returns an independent array copy of the current logical range |
| `iterator()` | New owner-bound forward primitive cursor; both cursor methods check modification count |
| `longSpliterator()` | New owner/index descriptor with an initial size fence; different mutation rules from the iterator, detailed below |

### List capacity, cost and retained state

The owner has one `long[]`, size and modification count. No-arg and zero-capacity
construction delay payload allocation. Growing from zero chooses at least ten
slots, or the requested minimum when larger. Later ordinary growth chooses
the larger of the requested minimum and old capacity plus half the old capacity;
large-array handling uses `Integer.MAX_VALUE - 8` and `Integer.MAX_VALUE` limits.
These source branches are not a guarantee that a huge allocation can succeed.

`ensureCapacity` may copy all live values into relocated storage but preserves
logical size and does not invalidate an iterator. `trimToSize` can allocate a
smaller array and does invalidate an iterator when it actually shrinks. Removal,
filter, dedup and clear do not shrink storage. Repeated trim/grow cycles can
therefore repeatedly allocate and copy; no amortized benefit is promised for
that usage. A pre-sized list avoids growth only while sufficient capacity remains.

Indexed reads/writes and tail removal access a fixed number of slots; insertion
and middle removal shift a suffix. Search, filter, dedup and array export scan
the logical range. Growth copies storage and sorting delegates to `Arrays.sort`.
These are source-level work descriptions, not timing or whole-heap measurements.

## Deque method contracts

These rows cover every declared public method of [`M3LongArrayDeque`][deque]
and the deque aliases. Queue defaults are in the next section.

| Method | Result, mutation and refusal |
| --- | --- |
| Constructors | Empty deque with power-of-two capacity, minimum eight. Negative expected size or expected size greater than `1 << 30` throws `IllegalArgumentException`; allocation can fail |
| `addFirst(value)`; `addLast(value)` | Grow if full, add at the chosen end, increment size/modification count; return void |
| `offerFirst(value)`; `offerLast(value)` | Delegate to corresponding add and return true after success; do not return false on a full ring |
| `offer(value)` | Alias of `offerLast`, preserving FIFO insertion order |
| `addLastIf(value,predicate)` | Rejects null predicate. Tests once before adding; false returns false, true appends and returns true. Callback exceptions propagate |
| `getFirst()`; inherited `first()`; `peek()` | Read head without removal; empty throws `NoSuchElementException` |
| `getLast()`; inherited `last()` | Read tail without removal; empty throws `NoSuchElementException` |
| `get(index)` | Read logical head-relative index; requires `0 <= index < size`, otherwise `IndexOutOfBoundsException` |
| `removeFirstLong()`; inherited `removeFirst()`; `poll()` | Remove/return head, zero its slot, advance head, decrement size and increment modification count; empty throws `NoSuchElementException` |
| `removeLastLong()`; inherited `removeLast()` | Remove/return tail, zero its slot, decrement size and increment modification count; empty throws `NoSuchElementException` |
| `contains(value)` | Scans in logical order for exact equality |
| `removeFirstOccurrence(value)`; `removeLastOccurrence(value)` | Remove first/last equal value and return true; absent returns false. Shift one side of the ring, zero the vacated lane and increment modification count |
| `filterInPlace(predicate)` | Stable keep-matching compaction across ring wrap; returns removal count on normal completion. Null and callback-failure rules match the list's filter |
| `size()`; `capacity()` | Logical element count / allocated ring length |
| `clear()` | Nonempty: zeroes logical slots, resets head and size to zero, increments modification count; empty: no-op. Capacity is retained |
| `toArray()` | Copies values head-to-tail into a new contiguous array |
| `iterator()`; `descendingIterator()` | New fail-fast cursors in forward / reverse logical order; exhausted `nextLong` throws `NoSuchElementException` |

### Ring geometry, growth and lifetime

Construction rounds `max(8, expectedSize + 1)` to a power of two, capped at
`1 << 30`. Thus `new M3LongArrayDeque(8)` has sixteen slots; the constructor
argument is not an exact storage length. The implementation tracks size
separately and can use every allocated slot, including the last one.

Physical indexing uses `(head + logicalIndex) & (capacity - 1)`. End operations
can wrap without shifting all values. A full ring doubles on the next insertion,
copies values in encounter order into the new array and resets head to zero.
Growth at capacity `1 << 30` throws `OutOfMemoryError("deque too large")`.
There is no public `ensureCapacity` or trim method on this owner; removals and
clear retain the allocated ring. Occurrence removal scans then shifts one side;
it is not constant-time arbitrary deletion.

Both owners manage ordinary Java heap arrays, with no `close`, native handle,
external reservation object or manual-release protocol. Live iterators and
spliterators retain their owners, which retain current storage. Array exports
are independent materializations. Replaced backing arrays become reclaimable
when no references remain; this is not a prompt-GC guarantee. Zeroing discarded
primitive lanes is not a reference/GC-root-release mechanism. The absence of
per-element wrappers does not remove array, iterator, split or stream allocations.

`copyOf`, `addAll` and `toArray` copy values, but they are not atomic snapshots
against concurrent changes to caller arrays or owners. Synchronize the surrounding
ownership protocol when consistency across separate calls matters.

## Queue defaults: batches, destination writes and progress

The deque inherits these bodies unchanged from [`M3LongQueue`][queue]. The list
does not inherit them.

| Default | Validation and effects |
| --- | --- |
| `tryPoll(target,targetIndex)` | Requires nonnull array and valid destination index before polling, even when empty. Success removes head and writes it to exactly that slot, returning true. A caught `NoSuchElementException` returns false and leaves the destination unchanged |
| `offerBatch(source,offset,length)` | Requires nonnull source and `Objects.checkFromIndexSize`-valid range, including zero-length calls. Repeatedly offers in array order until `length` accepted or an offer returns false; returns accepted count |
| `drainTo(target,offset,length)` | Validates nonnull destination and full range first, then repeatedly `tryPoll`s until the requested length or an empty poll; returns count written/removed. Untouched destination suffix remains unchanged |
| `pollReserved()` | Repeats `poll` until one returns; catches `NoSuchElementException` and calls `Thread.onSpinWait` after each empty attempt |

The batch operations are loops, not transactions. Invalid array/range inputs
are rejected before iteration (`NullPointerException` / `IndexOutOfBoundsException`).
An exception after successful individual operations leaves that accepted or
removed prefix committed. On this growable deque, `offerBatch` normally accepts
the whole valid range; allocation failure is not converted to a partial-count
return. Empty ranges return zero after validation.

`pollReserved` does **not** create or verify a reservation. Its interface name
and documentation support other queue implementations that override it for
external reservation/publication protocols. This deque provides no such protocol.
When already nonempty it simply removes the head; when empty it can spin
indefinitely. It has no timeout, fairness bound, cancellation callback or interrupt
check. Cross-thread insertion into this unsynchronized deque is not a supported
way to make that loop safe or live. Use `tryPoll` for an empty-sensitive attempt.
The inherited empty path repeatedly throws and catches an exception; it does
not establish allocation-free waiting or lock-free/wait-free queue progress.

## Callback mutation and partial failure

Both `filterInPlace` implementations explicitly require that the predicate not
mutate the same collection. They visit the original logical sequence in order
and keep matching values without a new payload array. A `finally` block repairs
the sequence when the predicate throws:

1. Values accepted before the throwing call stay, in encounter order.
2. Values rejected before that call remain removed.
3. The throwing value and the unread suffix survive.
4. Vacated slots are zeroed; size/modification count change only if values were
   removed. The original callback exception propagates rather than returning a count.

For example, filtering `[1, 2, 3, 4, 5]` for even values and throwing at `4`
leaves `[2, 4, 5]`. This is committed-prefix behavior, not all-or-nothing rollback.
A null predicate is rejected before traversal. A filter that rejects nothing
does not invalidate iterators, even though accepted values may be written back.

Do not infer callback-reentrancy safety from the `finally` block: these filters
do not fence every predicate call with modification-count checks. Mutating the
same owner violates the documented precondition, so resulting corruption or
exceptions are not an alternative supported contract. `addIf` and `addLastIf`
also invoke their predicate before their own add; exceptions or false results
do not roll back side effects performed by the callback itself.

For traversal callbacks, values are fetched and cursors advance before the
consumer runs. A throwing consumer leaves that value consumed. Prior callback
effects are retained. A structural change during iterator-backed traversal is
normally detected by the next cursor check if the callback returns, rather than
being rolled back. Callbacks must not interfere with these nonconcurrent owners.

## Iterators, spliterators and streams are different boundaries

### Fail-fast primitive iterators

Both owner iterators capture `modCount`; the deque's descending cursor does too.
Both `hasNext` and `nextLong` check the count before examining position and
throw `ConcurrentModificationException` on a mismatch. Without mutation,
`nextLong` throws `NoSuchElementException` when exhausted. Cursor mutation/removal
is not offered. [`M3LongIterator.forEachRemaining`][iterator] validates a nonnull
consumer even on exhaustion, then repeatedly checks, advances and calls it.

| Change | List iterator | Deque iterator, either direction |
| --- | --- | --- |
| Successful insertion/removal, nonempty clear | Invalidated | Invalidated |
| Value replacement with `set` | Not invalidated; future reads see current owner values | No `set` method |
| Storage-only `ensureCapacity` | Not invalidated; follows new owner storage | No public method |
| Actual trim | Invalidated | No public method |
| `sort`, even without a change in order | Invalidated | No sort method |
| Filter/dedup removing at least one value | Invalidated | Filter invalidates; no dedup method |
| Empty clear, unsuccessful value/occurrence removal, false conditional add, all-kept filter | No owner structural change from that operation | No owner structural change from that operation |
| Empty `addAll`, no-op trim, dedup removing nothing | Not invalidated | No corresponding methods |

These are same-owner source rules, not a cross-thread detection or synchronization
guarantee. The finite integer modification counter is not an unlimited generation
identifier or lifetime token.

### List: fixed-fence, owner-indirected splitting

`longSpliterator()` captures the owner, starting index zero and the current size
as its fence. It does not capture the backing array or copy values.
[`M3LongListSpliterator`][split] reports exactly
`ORDERED | SIZED | SUBSIZED`, with `estimateSize() == fence - index`.
`trySplit` returns the remaining prefix up to the midpoint and moves the original
descriptor to the suffix; it returns null when fewer than two elements remain.
Reads call `owner.get`, so capacity relocation does not strand traversal on old
storage and nonstructural `set` replacements are visible when fetched.

This spliterator has **no modification-count check**. Do not transfer the
iterator's fail-fast rule to it. Structural changes after creation are
unsupported: a fixed fence does not become a snapshot, newly appended values do
not extend it, removals can shift values or cause bounds failures, and a later
split validates its range against the owner's then-current size. No
`ConcurrentModificationException` guarantee is established for this path.

`tryAdvance` and `forEachRemaining` reject null consumers before checking
exhaustion. They increment the index before invoking the consumer. A throwing
consumer leaves that item consumed; there is no callback rollback.

### Deque: unsplittable iterator wrapper

The deque does not override `longSpliterator`. The [`M3LongCollection`][collection]
default captures its iterator and supplies:

- `trySplit() == null`
- `estimateSize() == Long.MAX_VALUE`, including after exhaustion
- `characteristics() == NONNULL`, without `ORDERED`, `SIZED` or `SUBSIZED`
- `tryAdvance`: reject null, check the cursor, consume one value, invoke callback
  and return true; return false on normal exhaustion

Actual traversal follows deque encounter order, but the wrapper does not
advertise an `ORDERED` characteristic to stream consumers. Its fail-fast behavior
comes from the captured deque iterator. Do not describe it as a sized ring
snapshot or a splitting source merely because its owner knows the size.

### Shared collection defaults and stream lifetime

| Default from `M3LongCollection` | Contract |
| --- | --- |
| `isEmpty()` | Returns `size() == 0` |
| `forEachLong(consumer)` | Rejects null even when empty, then traverses a new primitive iterator with next-before-callback effects |
| `longSpliterator()` | Iterator-backed default above; replaced by the list's index implementation |
| `longStream()` | Creates a sequential primitive stream from the owner's spliterator |
| `parallelLongStream()` | Creates a parallel-mode primitive stream from that spliterator; does not add a splitting algorithm |

Traversal is lazy, but the spliterator/cursor is created when the stream method
is called. A list stream therefore retains that initial fence and owner; a deque
stream retains the captured iterator's modification count. Neither is an array
snapshot. Keep the owner stable for the stream's traversal, including parallel
traversal. A parallel-mode deque stream remains backed by an unsplittable source;
parallel mode is not a throughput promise. Primitive traversal avoids source
element boxing but still allocates cursor/pipeline/split metadata, and user
callbacks or stream operations may allocate further objects.

## Bounded usage examples: source-checked, NOT_RUN

These snippets illustrate the inspected API and expected results. They were not
compiled or executed and are not substitute test receipts.

```java
import com.m3.collections.M3Collections;
import com.m3.collections.M3LongArrayList;

var list = M3LongArrayList.copyOf(new long[] {3, 1, 1, Long.MIN_VALUE});
list.sort();
int removed = list.deduplicateSorted(); // 1
long[] sorted = list.toArray();         // [Long.MIN_VALUE, 1, 3]
list.push(9);
long top = list.peek();                 // 9, no removal
long popped = list.pop();               // 9, tail removed

var deque = M3Collections.longArrayDeque();
deque.offerBatch(new long[] {0, Long.MAX_VALUE}, 0, 2);
long first = deque.poll();              // 0 is a value, not an empty sentinel
long reserved = deque.pollReserved();   // Long.MAX_VALUE; already nonempty
long[] destination = {42};
boolean present = deque.tryPoll(destination, 0); // false; destination stays [42]
// Do not call deque.pollReserved() here: the empty deque can spin forever.
```

Capacity relocation and value replacement are distinct from structural changes:

```java
var live = M3LongArrayList.copyOf(new long[] {5, 6});
var cursor = live.iterator();
var split = live.longSpliterator();
live.ensureCapacity(100);               // relocation, no iterator invalidation
live.set(0, 15);                        // nonstructural replacement
long next = cursor.nextLong();          // 15
split.tryAdvance((long value) -> System.out.println(value)); // expected 15
live.trimToSize();                      // actual shrink invalidates cursor
// cursor.hasNext() now throws ConcurrentModificationException.
```

Predicate failure preserves the throwing value and unread suffix:

```java
var values = M3LongArrayList.copyOf(new long[] {1, 2, 3, 4, 5});
try {
    values.filterInPlace(value -> {
        if (value == 4) throw new IllegalStateException("stop");
        return (value & 1) == 0;
    });
} catch (IllegalStateException expected) {
    long[] retained = values.toArray(); // expected [2, 4, 5]
}
```

## Test evidence and unresolved checks

The table describes assertions read at the pinned revision, not passing results.
The two direct owner files, the three-method differential file and the generic
factory file were read in full. In the broader JDK-shaped suite, only the
sequence methods and shared value helper relevant below are evidence for this
page. Counts are finite workloads encoded in tests, not coverage percentages.

| Test source and method | Assertions actually inspected |
| --- | --- |
| [`M3LongArrayListTest`][list-tests] — `mixedOperationsMatchJdkValueOrder` | 32 seeds × 1,024 steps against `ArrayList<Long>`: add/insert/set/remove, copied `addAll`, clear, search, trim and logical order; includes extrema |
| Same — `callbackFailurePreservesThrowingValueAndUnreadSuffix` | Every throw position in a sixteen-value filter; visited prefix, exact retained suffix, exception identity, null predicate, successful filter and conditional add |
| Same — `directStreamsRetainOwnerAcrossStorageRelocation` | Split and iterator after `ensureCapacity(4096)` and `set`; trim invalidation; sequential array and parallel sum; exact list characteristics; null split consumers |
| Same — `sortDedupAndSearchPreserveSignedLongSemantics` | 64 finite seeds against `Arrays.sort`/`binarySearch`, signed extrema and adjacent dedup result/count |
| Same — `structuralMutationsFailFastAndStackIsJdkShapedRemoval` | Sort/dedup/removing-filter invalidation, all-kept filter preserving iterator, tail push/peek/pop |
| Same — `boundsOwnershipAndStackExceptions` | Negative capacity, null copy/addAll, selected bad indices, exhausted cursor/empty stack, input/export isolation and no-arg primitive list factory |
| Same — `primitiveStorageAndCapacityChecksStayBounded` | Reflection checks three instance-field types, negative ensure-capacity error, actual trim relocation and empty capacity zero; explicit-capacity primitive list factory |
| [`M3LongArrayDequeTest`][deque-tests] — `primitiveDequeMatchesJdkAcrossWrapGrowthReuseAndStreams` | 6,000-operation `ArrayDeque<Long>` comparison with deliberate wrap/growth/reuse, extrema/zero, filter/addLastIf, indexed reads, array/stream order and selected empty/bounds errors |
| Same — `callbackFailurePreservesThrowingValueAndUnreadSuffixAcrossRingWrap` | All sixteen throw positions from a nonzero-head wrapped ring, retained/visited values, exception identity, null callbacks, removing-filter invalidation and all-kept iterator validity |
| Same — `queueAndIndexedViewsAreJdkShapedAliases` | End/queue aliases, indexed bounds, descending-cursor invalidation, offerBatch/drainTo prefix, already-nonempty pollReserved, false tryPoll and default capacity eight |
| [`M3PrimitiveCollectionDifferentialTest`][differential] — `primitiveListMatchesJdkAcrossGrowthCompactionSortingAndStreams` | 4,000 operations covering mutation, search, sort, filter, trim, conditional add, stack and sequential/parallel values/sums against JDK oracles |
| Same — `primitiveListSnapshotFilterFailureAndOwnerBoundStream` | Input-copy isolation, stream created before growth/set, partial filter exception and sorted dedup |
| Same — `primitiveDequeMatchesJdkAcrossWrapGrowthReuseAndStreams` | 6,000-operation deque comparison, wrap/growth/reuse/filter and stream values; shares a deterministic seed and operation template with the direct deque test, whose extra indexed-read draws change its random sequence |
| [`M3JdkShapedPrimitiveCollectionsTest`][jdk-tests] — `arrayListMatchesJdkAcrossDeterministicMutationSequence` | 10,000-step list value-order/return comparison with JDK `ArrayList` |
| Same — `arrayDequeMatchesJdkOrderAndOccurrenceRemoval` | 20,000-step deque/end/occurrence-removal comparison with JDK `ArrayDeque` |
| Same — list/deque cases of `iteratorsAreFailFastOnStructuralMutation` | Structural add invalidates the respective forward iterator's `hasNext` |
| [`M3FactoryContractTest`][factory-tests] | Its list/deque assertions call generic object `arrayList()`/`arrayDeque()`, not the primitive long factories; no primitive factory coverage is inferred from the filename |

All three methods in `M3PrimitiveCollectionDifferentialTest` are list/deque
tests. Its broad name is not evidence for primitive maps. Conversely, set/map/
priority cases in the JDK-shaped suite are outside this page. Both primitive
list factory overloads are directly asserted by the direct list test; full
primitive deque-factory coverage was not established by these inspected tests.

The following remain source-only observations or unqualified gaps:

- Maximum-capacity allocation, integer-limit growth, allocation-failure recovery
  and bulk partial effects under resource failure were not exercised here.
- The queue-default assertions use valid ranges and an already-nonempty
  `pollReserved`; they do not establish empty-loop termination, interruption,
  fairness, concurrent publication or full null/range/refusal coverage.
- No inspected test establishes callback-reentrancy correctness, all iterator
  overflow/race cases, or concurrent access safety. Filter reentrancy is outside
  the documented precondition.
- List spliterator structural mutation is unsupported, and the implementation
  has no fail-fast check. This is a documented boundary, not a newly reproduced
  runtime bug or a promise that structural mutation produces one specific error.
- Null-consumer and callback-exception cursor rules, all deque spliterator flags,
  no-op mutation combinations and unchanged `tryPoll` destination behavior are
  source-backed; the listed tests do not exhaust those combinations.
- Reflection on the list's field types is not retained-heap accounting. No
  latency, allocation-rate, cache, throughput or parallel-speedup measurement was
  made; no full JDK concrete-class substitution, serialization, packaging or
  installed-runtime claim follows from these value comparisons.

## Source closure and continuation

Nine additional non-descriptor production bodies are reviewed here: the two
owners, `M3LongCollection`, `M3LongSequence`, `M3LongList`, `M3LongQueue`,
`M3LongDeque`, `M3LongStack` and package-private `M3LongListSpliterator`.
`M3LongIterator`, relevant `M3Collections` methods, descriptor/build facts and
existing guides are reused dependencies, not newly completed package counts.

This closes a selected sequence-documentation gap, not all 76 non-descriptor
files in the collection inventory. Sparse/segmented lanes, priority structures,
other concurrent/reservation owners, blocking/reference/enum/lazy/progress
families and `m3/ports/collections` are outside this packet. Follow the [coverage
ledger](coverage.md) and [native build/test routes][native-readme] for remaining
work. Historical receiver receipts retain their original scope and execution
status; linking them does not rerun them or advance current target acceptance.

[list]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongArrayList.java
[deque]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongArrayDeque.java
[collection]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongCollection.java
[sequence]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongSequence.java
[list-interface]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongList.java
[stack]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongStack.java
[queue]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongQueue.java
[deque-interface]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongDeque.java
[split]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongListSpliterator.java
[iterator]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3LongIterator.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/pom.xml
[list-tests]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3LongArrayListTest.java
[deque-tests]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3LongArrayDequeTest.java
[differential]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3PrimitiveCollectionDifferentialTest.java
[jdk-tests]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3JdkShapedPrimitiveCollectionsTest.java
[factory-tests]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/collections/src/test/java/com/m3/collections/M3FactoryContractTest.java
[native-readme]: ../../m3/collections/README.md
[backend]: ../../m3/collections/M3_JDK_SHAPED_BACKENDS.md
[product]: ../../m3/collections/M3_COLLECTION_PRODUCT_SCOPE.md
[policy]: ../../m3/docs/m3jdk21-porting-invariant.md
