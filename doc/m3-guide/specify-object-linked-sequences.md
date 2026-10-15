<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Object linked-list and deque reference

[Guide](README.md) · [Linked-hash collections](collections-reference.md) · [Primitive sequences](primitive-sequence-reference.md) · [Coverage](coverage.md)

`M3LinkedList<E>` is a mutable, nullable object sequence implementing `List`
through `AbstractSequentialList` and implementing `Deque` directly. Its links
are integer indexes into reusable array slots. It does not retain one linked
node object per element, but it strongly retains each element reference.

This reference covers the complete [owner][owner], including private `Storage`
and its anonymous list iterator, at `1011050da4205a901ad079d7711b6b8efd8a9af7`.
The body is unchanged at `7abdf87cb497d0d94abeb723d70c50f856ecc381`.
Examples and tests are **NOT_RUN in this documentation authoring/review pass**;
no compiler, build, recipe, runtime probe or benchmark ran in this pass.
Hosted CI for a subsequently published documentation commit is separate
exact-commit evidence. Cost and exception-order statements are source deductions.

## Type, module and inherited surface

The only public constructor is `M3LinkedList()`. The [factory][factory]
`M3Collections.linkedList()` returns that owner. `longLinkedList()` is the
separate stable-slot primitive owner; `arrayList()` and `arrayDeque()` return
JDK classes. The [primitive sequence reference](primitive-sequence-reference.md)
has different boxing, cursor and API contracts and is not an object-list adapter.

The exported [module][module] has implicit `java.base` only; the [POM][pom]
targets Java 21. See the [module guide][guide] for the separately built module's
build/receiver route. This final class is not a `java.util.LinkedList` subclass,
does not implement `RandomAccess`, `Cloneable` or `Serializable`, and supplies
no public clone, copy/capacity constructor or serialized-form compatibility.
Its list/deque interfaces are supported by actual inheritance, not by its name.

[`AbstractSequentialList`][sequential] implements indexed get/set/add/remove
using this owner's `listIterator(index)`. [`AbstractList`][abstract-list]
supplies list equality/hash, listIterator(), search and sublists; ordinary
[`AbstractCollection`][abstract-collection] methods supply contains/bulk removal,
array conversion and collection string behavior. [`List` defaults][list-api]
supply replaceAll and sort. These are inherited algorithms over M3 iterators,
not extra specialized M3 implementations or a completeness claim for every
possible aliasing and user-callback combination.

## List/deque methods and bounds

Duplicates and null elements are accepted. Occurrence removal uses
`Objects.equals(query, element)`. List equality compares corresponding elements
by equality and order, and list hash starts at 1 and folds by 31; neither uses
reference-identity collection semantics.

| Declared methods | Behavior |
| --- | --- |
| `size()` | Current shared storage size |
| `addFirst(e)`, `addLast(e)` | Insert at receiving view's head/tail |
| `offerFirst(e)`, `offerLast(e)` | Same insertions; return true after success, including for null |
| `peekFirst()`, `peekLast()` | Read endpoint or return null on empty |
| `pollFirst()`, `pollLast()` | Remove endpoint or return null on empty |
| `getFirst()`, `getLast()` | Read endpoint; empty throws NoSuchElementException |
| `removeFirst()`, `removeLast()` | Remove endpoint; empty throws NoSuchElementException |
| `add(e)`, `offer(e)` | Tail insertion; return true after success |
| `peek()`, `element()` | Aliases of peekFirst/getFirst |
| `poll()`, `remove()` | Aliases of pollFirst/removeFirst |
| `push(e)`, `pop()` | Head insertion/removal, with pop throwing on empty |
| `remove(Object)`, `removeFirstOccurrence(Object)` | Scan from head, delete first equal element, report success |
| `removeLastOccurrence(Object)` | Scan from tail, delete first match in that direction |
| `clear()` | Empty is a no-op; otherwise null all retained value slots and reset live/free/high-water bookkeeping |
| `listIterator(int)` | Mutable bidirectional cursor at an insertion position |
| `reversed()`, `descendingIterator()` | Shared reversed facade, and its forward iterator |
| `spliterator()` | Late-binding ordered adapter described below |
| `verifyInvariants()` | Allocating structural diagnostic, described below |

`peek`/`poll` returning null is ambiguous: it may mean empty or an actual null
element. A successful poll of null still removes an element. Check emptiness
under your ownership discipline, or use the throwing endpoint methods when
that distinction matters. Offers are not bounded-capacity refusal operations;
allocation/capacity failure can throw rather than return false.

`listIterator(index)` and inherited indexed insertion permit `0 <= index <= size`.
`get`, `set` and indexed `remove` require `0 <= index < size`. The M3 `slot`
helper rejects negative or greater-than-size indexes before constructing the
iterator; at exactly size it returns the end sentinel. The inherited read/set/
remove methods translate an exhausted next into `IndexOutOfBoundsException`.
For `addAll(index, source)`, the inherited implementation obtains the destination
iterator before traversing source, so a bad index can precede a null-source
failure. Earlier insertions can remain if source iteration fails. Self/aliased
bulk insertion is not a supported snapshot-copy protocol.

`remove(int)` and `remove(Object)` are different overloads. For an Integer list,
`remove(1)` removes position 1; use an explicitly boxed/object argument for an
occurrence removal when needed.

## Reversed facade and sublist ownership

The facade stores final `storage` and `backwards` fields and a lazily created
`reverse` companion. Both facades share one `Storage`. Their first/last and
next/previous helpers swap directions; insertion swaps physical neighbor roles.
Changing either facade changes the other immediately. `reversed()` caches its
companion and links it back, so repeated reversal returns the original owner
by reference. It copies no elements. `descendingIterator()` creates/uses the
reverse facade and returns that facade's ordinary iterator.

Every structural insertion/erase and nonempty clear increments the receiving
facade's inherited `modCount` and its reverse companion's count if present.
A newly created companion starts at the current count. A value replacement
through iterator set is nonstructural. Holding a reverse view or sublist keeps
the shared owner/storage reachable; it is not a detached or immutable snapshot.

Inherited `subList(from,to)` is a backed half-open view in the **receiving
facade's** order. Bounds checks run in this order: negative from, to greater
than size, then from greater than to (the last throws IllegalArgumentException;
the former checks throw IndexOutOfBoundsException). Sublist get/set/add/remove
delegate with an offset; sublist clear iterates and erases the range. Changes
through that sublist update its size/revision and ancestor sublist metadata.
Unrelated structural changes through the root, reverse or sibling view can
invalidate it and produce ConcurrentModificationException. The implementation
does not promise arbitrary mutations outside a sublist will preserve that view.

Nonstructural set is visible through all facades. Inherited replaceAll invokes
the operator then iterator set per element; null operator is rejected before
iteration. A throwing operator can leave earlier values replaced. Inherited
sort first copies elements to an array, sorts that array, then writes back via
iterator set. Sorting therefore allocates and is not direct link rearrangement.
Neither operation turns the collection into an immutable or thread-safe owner.

## Iterator state and failure order

The anonymous list iterator keeps `cursor` (next slot), `position`, `returned`
(last returned slot) and `expected` modCount. The private check compares the
revision before next, previous, remove, set and add.

| Iterator method | Exact source behavior |
| --- | --- |
| `hasNext`, `hasPrevious` | Compare position with current size/zero; **do not check modCount** |
| `nextIndex`, `previousIndex` | Return position / position minus one; no revision check |
| `next` | Check revision, then exhaustion; return next slot value and advance |
| `previous` | Check revision, then beginning; step backward, using tail when cursor is at end |
| `remove` | Check revision, then require a returned element; adjust cursor or position according to last direction, erase, clear returned and refresh expected |
| `set(e)` | Check revision, then require a returned element; replace value without changing size/revision |
| `add(e)` | Check revision, insert before cursor in receiving order, increment position, clear returned and refresh expected |

Exhausted next/previous throws NoSuchElementException. Remove/set before any
successful movement, or after add/remove without another movement, throws
IllegalStateException. If both revision and iterator state are invalid, the
revision failure comes first. Repeated set after a successful movement is
allowed. Mutation through a different iterator or facade invalidates this
iterator; its own successful add/remove remains usable. An unchanged size
after remove-plus-add does not avoid the revision change.

Fail-fast is diagnostic only. Fields are plain mutable state without locks,
atomics or volatile publication. These are not snapshot or weakly consistent
iterators. External coordination must cover both facades, views and traversal.
In particular, hasNext returning false is not itself a concurrent-modification
check, and parallel stream availability does not permit concurrent mutation.

## Spliterator, streams and array snapshots

`spliterator()` calls [`M3SlotSpliterators.ordered(this,false)`][spliterators],
which delegates to the collection-based JDK adapter. The inspected
[`IteratorSpliterator`][jdk-spliterators] creates the iterator and captures
size on first traversal, split or size query. Creation alone does not bind it;
calling estimateSize does. Structural mutation after binding can be detected
by the underlying iterator when it advances. It is not guaranteed to be
detected on an exhausted hasNext path or by every already-split child.

Characteristics are ORDERED, SIZED and SUBSIZED. DISTINCT, SORTED, NONNULL,
IMMUTABLE and CONCURRENT are not asserted. getComparator throws
IllegalStateException because it is not sorted. A null traversal consumer
is rejected before binding/traversal by the JDK adapter. Splitting batches
elements into new Object arrays, starting at up to 1024 references and growing
batch size; child array spliterators no longer use the original iterator's
modification check. This is ordered batching, not zero-copy splitting over slots.

Inherited `toArray()` returns a new shallow element-reference array; typed
toArray reuses a sufficiently large caller array, puts a trailing null when
there is spare room, and follows normal null-destination/runtime component
type checks. These are detached container snapshots, not copied element
objects. Mutating an array does not edit the list, but a mutable element
is still shared. A stream terminal toList result similarly does not make
the source or its elements immutable.

## Slot representation, capacity and costs

`Storage` owns Object[] values, int[] before/after, first/last endpoints, a
free-list head, live size and a used-slot high-water mark. `-1` marks no link.
`Storage.insert` reuses `free` before increasing used; a freed slot's after
field links the free list. Erase repairs its neighbors, nulls the payload,
sets before to -1, and pushes it onto that free list. Internal slot numbers
can be reused and are not exposed as stable IDs or generation-tagged handles.

Each empty owner has its own Storage but initially shares zero-length arrays.
First insertion allocates capacity 8. [`grow`][arrays] subsequently chooses
`max(8, old + (old >> 1) + 1)`, capped at Integer.MAX_VALUE - 8, and throws
OutOfMemoryError when asked to grow at that cap. The three copied lanes are
installed after their allocations succeed. Actual allocation can fail earlier;
there is no promise of reaching the arithmetic limit. Neither erasure nor
clear shrinks arrays. Clear nulls the full values array but need not zero all
integer links because endpoints/used/free reset and future slots overwrite links.

For capacity C, retained lane payload is C reference slots plus 8*C primitive
bytes, excluding array/owner/facade headers, alignment and payload objects.
There are no retained per-element link nodes. That does not imply zero
allocation: storage construction, growth, reverse facade, iterators, sublists,
toArray, sort and stream split batches allocate.

| Operation | Source-derived cost, excluding user equality/callback work |
| --- | --- |
| size and endpoint reads | O(1) |
| Endpoint insert/delete, iterator movement/set/delete at known slot | O(1), except capacity growth on insertion |
| Capacity growth | O(C) array allocation/copy; geometric growth amortizes insertion copying |
| Indexed position lookup | O(min(index, size-1-index)) link traversal for an existing element; end insertion position is O(1) |
| Occurrence removal, contains, sequential traversal | O(n) worst case |
| Clear of nonempty list | O(C) reference clearing; capacity retained |
| Sublist range clear | Position lookup plus O(number removed) iterator erases |
| verifyInvariants | O(used) allocation/scanning with boolean[used] |

Repeated indexed reads are not constant-time array access; prefer a single
iterator for a full pass. `verifyInvariants()` checks live links, cycles,
predecessors, count/tail consistency, disjoint free links, released free-slot
payloads and coverage of every used slot. It throws AssertionError with
`list links`, `list size`, `list free` or `list ownership` on the respective
detected failure. It is an explicit diagnostic, not automatically called
on mutation and not a proof of data-race safety.

## Evidence and practical example

Source-checked illustration, **NOT_RUN**:

```java
var list = new com.m3.collections.M3LinkedList<String>();
list.addLast("a");
list.addLast(null);
var reverse = list.reversed();
reverse.addFirst("b");                 // original: [a, null, b]
var it = list.listIterator(1);
it.next();                             // returns the stored null
it.set("middle");                      // shared, nonstructural replacement
it.remove();                           // original: [a, b]
boolean sameOwner = reverse.reversed() == list; // true
```

All following test/probe bodies were read, **not run**:

| Existing source | Relevant assertions / limits |
| --- | --- |
| [M3SequenceContractTest][sequence-test] `linkedListAndDequeDifferential` | 30,000 seeded list/deque operations against LinkedList, including null, reverse insertion and sublist clear; invariant checks after each step |
| Same test, `listIteratorAlternatingMutations` | 10,000 seeded forward/backward/add/remove/set operations, invalid-state comparisons, cursor indexes and content |
| Same test, `sublistAndReverseShareStructuralVersion` | Reverse insertion invalidates an existing iterator and sublist; double reversal identity; null and empty removal |
| Same test, `streamsRetainEncounterOrderAndSortedComparator` | The list portion compares ordered parallel-stream content; sorted comparator checks belong to the tree set, not this list |
| [M3JdkCollectionDifferentialTest][jdk-test] | Pre-binding mutation and post-estimateSize invalidation; 1,800 seeded reverse/index/addAll/sort/replaceAll/reversed-sublist operations and typed arrays |
| [M3FootprintContractTest][footprint-test] | Empty reverse creation, first insertion, clear/reuse, iterator invalidation and independent empty storage |
| [M3NoRetainedStructuralEntryTest][entry-test] | Includes list/Storage in declared-field/nested-type guard; fresh Map.Entry tests target maps, not lists |
| [M3FactoryContractTest][factory-test] | linkedList factory add/pop smoke case |
| [M3AllocationProbe][allocation-probe], [M3CollectionCostProbe][cost-probe] | Prepared add/poll and bounded list traversal diagnostic workloads; no measurement result is claimed |

The factory and these module tests/probes are observed callers, not a complete
repository-wide consumer inventory. Bounds combinations, sublist exception
precedence, mutable/throwing equality, aliasing, consumer-side structural
mutation, already-split child behavior and allocation-failure recovery are
not exhaustively qualified by these tests. This page does not add methods to
the owner, promote a JDK backend or establish serialization compatibility.

Exact owner blob: `da39d792baf7311691d2d6e8398adf62f3aa88a8`.
Shared helpers: `M3SlotArrays` `e337c69710c871f8caa77a1afe53149f1fd705cc`,
`M3SlotSpliterators` `30cf137538353ac8e91514fff7c5db3903da8819`.

[owner]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LinkedList.java
[arrays]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotArrays.java
[spliterators]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java
[factory]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3Collections.java
[module]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/pom.xml
[guide]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/README.md
[sequential]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/src/java.base/share/classes/java/util/AbstractSequentialList.java
[abstract-list]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/src/java.base/share/classes/java/util/AbstractList.java
[abstract-collection]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/src/java.base/share/classes/java/util/AbstractCollection.java
[list-api]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/src/java.base/share/classes/java/util/List.java
[jdk-spliterators]: https://github.com/hsoliwal/M3jdk21/blob/7abdf87cb497d0d94abeb723d70c50f856ecc381/src/java.base/share/classes/java/util/Spliterators.java
[sequence-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3SequenceContractTest.java
[jdk-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3JdkCollectionDifferentialTest.java
[footprint-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3FootprintContractTest.java
[entry-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3NoRetainedStructuralEntryTest.java
[factory-test]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3FactoryContractTest.java
[allocation-probe]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3AllocationProbe.java
[cost-probe]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/test/java/com/m3/collections/M3CollectionCostProbe.java
