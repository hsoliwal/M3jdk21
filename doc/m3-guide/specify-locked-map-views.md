<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Locked maps, sorted sets and backed views

[Guide](README.md) · [Collection foundations](collections-reference.md) · [Queues](specify-blocking-and-transfer-queues.md) · [Source and test evidence](locked-queue-source-coverage.md)

This reference covers every declared member of `M3LockedMap`,
`M3LockedTreeMap` and `M3LockedTreeSet` in the independent
`com.m3.collections` library. Source pin:
`1011050da4205a901ad079d7711b6b8efd8a9af7`. The complete three source bodies,
relevant delegate bodies, factory calls and test assertions were inspected.
Tests, examples, Maven, recipes and runtime gates are **NOT_RUN** in this
pass. These exported classes implement the named Java interfaces; that fact
is not a claim of full JDK replacement, serialization compatibility, stable
product API or whole-package qualification.

## Construction and ownership

| Entry | Actual owner and storage |
| --- | --- |
| `new M3LockedMap<>()`; `M3Collections.lockedMap()` | One insertion-ordered `M3LinkedHashMap` and one new private monitor object |
| `M3Collections.lockedHashtableMap()` | The same `M3LockedMap`; no `Dictionary`, `Hashtable` subclass or serialized-form promise |
| `new M3LockedTreeMap<>()`; `lockedTreeMap()` | An eager, naturally ordered `M3TreeMap` and one new monitor |
| `new M3LockedTreeMap<>(comparator)`; `lockedTreeMap(comparator)` | Eager comparator-ordered tree; a null comparator selects natural order |
| `new M3LockedTreeSet<>()` / `(comparator)`; matching factories | A locked tree map storing `Boolean.TRUE` for each element and an insertable set wrapper |
| `M3Collections.lockedHashSet()` | JDK `Collections.newSetFromMap` over a new `M3LockedMap`; not an instance of `M3LockedTreeSet` |

Sources: [map construction][map-construction], [tree construction][tree-construction],
[set construction][set-construction], [factory routes][factory]. The standalone
locked tree set uses the ordinary map value lane; do not assign it the
key-only representation of the separate `M3TreeSet`. The locked tree map
exposes no lazy-provider constructor, deferred-value methods or delegate
invariant-check method.

Payloads remain strong Java references, not deep copies or immutable values.
The monitor protects collection operations, not arbitrary changes to key or
value objects. Keys must keep their hash/equality or comparison behavior
suitable for the delegate while stored. Snapshot arrays, views and returned
entries may keep payloads reachable after removal. No close, dispose,
shutdown or native-resource API is declared here.

## Lock scope and atomic operations

[The map][map-operations] synchronizes on its `mutex`, not on the public map
object. Tree range/descending wrappers retain both the same underlying tree
storage and the same monitor. Their key-set wrappers route back through
those maps. A separately constructed map/set has a different monitor.
` synchronized (map) ` in client code does not acquire this implementation's
private monitor. There is no public lock accessor or multi-operation
transaction facility.

The monitor covers the complete delegate call for `size`, `isEmpty`,
`get`, `getOrDefault`, `containsKey`, `containsValue`, `put`, `putIfAbsent`,
`remove(key)`, successful-path `remove(key,value)`, both `replace` forms,
`clear`, all compute/merge forms, `replaceAll`, `forEach`, `hashCode`,
`equals` and `toString`. The `equals(this)` fast path returns immediately.
Tree navigation and view construction also acquire the shared monitor;
`comparator()` reads the fixed comparator/view direction without taking it.

This provides mutual exclusion between the protected operations of these
wrappers, with the normal reentrancy of Java monitors. It does not imply
lock freedom, fairness, bounded waiting, interruptible lock acquisition,
or a transaction across several caller operations. In particular,
[`putAll(source)`][map-operations] calls `source.forEach(this::put)` without
one enclosing destination lock. Earlier entries remain inserted if a later
entry fails, and other operations can run between puts. Inherited bulk
operations on sets and collection views likewise have no added whole-call
monitor in these classes. Do not infer atomicity for `addAll`, `removeAll`,
`retainAll`, `removeIf`, or a separate read-then-write sequence.

User code can run while the monitor is held: explicit map callbacks,
comparators, key/value equality and hash functions, payload `toString`, and
operations on another map during `equals`. Keep these short and avoid
conflicting lock orders. In particular, comparing or copying between two
locked maps can acquire a second map's lock while holding the first.
The monitor is released on normal return and exception; release does not
undo earlier writes or callback effects.

## Nulls, return values and callbacks

[Argument checks][map-operations] precede delegation, so the locked wrappers
are stricter than their nullable underlying maps:

- Null keys are rejected by key lookups, puts, replacements, computes,
  removals and tree navigation/range endpoints, even with a comparator
  capable of ordering null
- Null stored values are rejected by `put`, `putIfAbsent`, `replace`, and
  the supplied value of `merge`; three-argument `replace` rejects a null
  expected old value as well
- `containsValue(null)` throws `NullPointerException`; a null
  `getOrDefault` fallback is allowed
- `remove(nonNullKey, null)` returns false; `remove(null, null)` still
  throws because the key is checked first
- Null functions/consumers throw. A null result from `replaceAll` throws
  before that result is written. A later failure may leave earlier
  replacements in place

For ordinary writes, `put` returns the previous value or null when absent;
`putIfAbsent` returns the current value or null after insertion;
`replace(key,value)` returns the prior value or null when absent;
conditional replace/remove return whether their value comparison matched;
`remove(key)` returns the removed value or null. Stored null values cannot
obscure absence through these public constructors and methods.

[Shared slot-map callbacks][slot-callbacks] have these concrete rules:

| Operation | Callback and result |
| --- | --- |
| `computeIfAbsent` | Existing value bypasses callback; otherwise invoke with key. Non-null result is inserted; null leaves absent |
| `computeIfPresent` | Absent key bypasses callback. Otherwise invoke with key/current value; null removes, non-null replaces |
| `compute` | Invoke with key and current value or null. Null removes an existing mapping; non-null writes |
| `merge` | Absent key inserts the supplied non-null value without invoking the remapper. Present key invokes with old/supplied values; null removes |
| `replaceAll` | Traverse the delegate's current encounter order, applying and writing one non-null replacement at a time |
| `forEach` | Traverse current delegate slots and invoke the consumer under the monitor |

The wrapper does not retry these callbacks. The delegate records its
structural revision and checks it after each normally returning callback;
a changed revision causes `ConcurrentModificationException` before the
operation's following planned write/removal. Reentrant structural changes
made by the callback itself remain. A callback exception propagates without
running the following revision check. Reentrant replacement of an existing
value need not change the structural revision, so this is not a universal
mutation guard. Avoid modifying the map from these callbacks. Tree
comparison callbacks have their own delegate validation; the wrapper does
not turn an arbitrary comparator into safe reentrant mutation code.

## Live views, snapshot traversal and entry lifetime

[`keySet`, `values` and `entrySet`][map-views] are newly allocated backed
view wrappers: their size, membership checks and clear operations consult
the current map. Adding through these map views is unsupported by their
abstract-collection bases. Key-view removal removes the current key;
entry-view removal conditionally matches key/value. Entry-view membership
returns false for a non-entry, null key or null value. Inherited values-view
removal uses snapshot iteration and its conditional removal described below.

Every [iterator construction][map-snapshot] copies parallel key/value pairs
under the monitor, then traverses outside it. This is a shallow snapshot of
membership and reference values at construction, not a live or fail-fast
traversal. Later insertions are absent from it; later deletions do not remove
its captured elements. Creation allocates `2 * size` reference slots using
`Math.multiplyExact`, so overflow can throw `ArithmeticException`, and
allocation may fail. `hasNext` reads cursor state; exhausted `next` throws
`NoSuchElementException`. Iterators themselves are not independently
synchronized for shared multi-threaded use.

Iterator `remove` requires one preceding `next` and allows one removal
attempt per returned element, otherwise `IllegalStateException`. It calls
`remove(capturedKey, capturedValue)` against the current map. A mapping
changed to an unequal value is retained; an absent mapping is a no-op. This
is value equality, not insertion identity: removal can affect a later
reinsertion whose key/value still compare equal. Key and value iterators
use the same original key/value pair for this check.

An entry iterator creates a fresh [`SnapshotEntry`][snapshot-entry]. Its
`getValue` returns its own captured value, not the map's latest value.
`setValue(nonNullReplacement)` unconditionally calls the owning wrapper's
`put(key,replacement)`, then changes the entry's local value, returning its
previous local value. Consequently it can overwrite a newer mapping or
reinsert a removed key. On a bounded map it still obeys that view's bounds.
Failure leaves the local entry value unchanged. Its equality, hash and
string use the local key/value; no lock surrounds that projection's local
fields. The iterator's saved removal pair is not updated by entry
`setValue`, so a following iterator removal still tests the original pair.

The base map's key and entry spliterators use `CONCURRENT | NONNULL |
DISTINCT`; its values spliterator omits `DISTINCT`. These adapters are
unknown-size and do not advertise `SIZED` or `ORDERED`, even though the
captured insertion-order delegate has an encounter order. The tree's values
and entries inherit these same adapter choices. A concurrent characteristic
does not turn a captured snapshot into ongoing live observation.

## Sorted maps and sets

[Tree navigation][tree-navigation] uses the eager delegate's comparator
ordering, including comparator-equivalent keys, and current view direction.
`firstKey`/`lastKey` throw `NoSuchElementException` when empty. Neighbor keys
(`lower`, `floor`, `ceiling`, `higher`) and navigational entries return null
when no visible match exists. `firstEntry`, `lastEntry`, all neighbor-entry
methods and the polling-entry methods return detached
`SimpleImmutableEntry` snapshots (or null); their `setValue` is unsupported.
Polling removes the corresponding visible mapping under the monitor.
These entries differ from the writable entry-set snapshot projections.

[Descending and bounded maps][tree-views] are backed, not copies. Descending
changes encounter/comparison direction. Four-argument `subMap` preserves
explicit endpoint inclusivity; two-argument `subMap` is from-inclusive,
to-exclusive. One-argument `headMap` is exclusive and `tailMap` inclusive.
Endpoints are validated against the parent view, and inverted/outside
ranges throw `IllegalArgumentException`. A write outside the view throws;
a validated out-of-range lookup/removal finds no mapping. Compute-style
calls can invoke the user function before a later non-null result reaches
that rejected write. `clear` removes only the view's visible mappings.
Bounded `size` traverses visible keys under the monitor rather than reading
a constant-time whole-tree count. Comparator exceptions and incompatible
natural-order keys propagate from the delegate.

`keySet()` equals `navigableKeySet()` in kind, and `descendingKeySet()`
wraps the descending map. Each is an `M3LockedTreeSet` with insertion
disabled. Its `add` immediately throws `UnsupportedOperationException`
(`"map key view"`); bounded/descending descendants preserve that restriction.
Standalone sets have insertion enabled: `add` delegates to `putIfAbsent`
with `Boolean.TRUE`, returns false for an existing comparator-equivalent
key, and range-view insertion remains restricted to the view's bounds.
This representation distinction matters when following similarly named
factory or key-view APIs.

[Set methods][set-methods] delegate size, emptiness, contains, remove, clear,
comparator, first/last and neighbor queries to the locked map. `pollFirst`
and `pollLast` atomically remove the corresponding key, returning null if
empty. `descendingSet`, both `subSet` forms and both `headSet`/`tailSet`
forms preserve backing storage, monitor and insertability. The short-form
inclusivity rules match their map counterparts. The iterator is the map's
key/value snapshot iterator; descending iteration uses a descending view.

The set/key-view spliterator is different from the map's generic adapters:
[`sorted(iterator(), size(), comparator())`][spliterators] advertises
`ORDERED | SORTED | DISTINCT`, retains the comparator across split
children, and does not advertise `CONCURRENT`, `NONNULL` or `SIZED` on its
initial adapter. It captures the iterator before separately asking for the
current size, so concurrent changes can make that initial estimate differ
from the captured element count. It is not a late-binding view.

## Limits and regression evidence

The complete member inventory, source hashes and exact test routes are in
[the evidence companion](locked-queue-source-coverage.md). In particular,
source tests exercise merge contention, unequal-value conditional iterator
removal, range backing, null rejection, traversal snapshots, custom
comparator splitting and self-value string formatting. They were read,
not run. They do not establish arbitrary callback safety, fair scheduling,
all inherited-interface behavior, performance, or every possible race.

The [linked-hash reference](collections-reference.md) owns detailed slot and
hash storage contracts; the [tree reference](specify-trees-and-lazy-values.md)
covers eager/lazy tree ownership and the [tree source][tree-source] supplies
the bounded-ordering implementation. This page adds the three wrapper contracts without
counting those dependencies again as new primary owner coverage.

[map-construction]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedMap.java#L27-L33
[tree-construction]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedTreeMap.java#L10-L18
[set-construction]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedTreeSet.java#L13-L20
[factory]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3Collections.java#L128-L159
[map-operations]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedMap.java#L34-L79
[slot-callbacks]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotMap.java#L60-L121
[map-views]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedMap.java#L117-L150
[map-snapshot]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedMap.java#L80-L103
[snapshot-entry]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedMap.java#L104-L116
[tree-navigation]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedTreeMap.java#L18-L32
[tree-views]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedTreeMap.java#L33-L49
[set-methods]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3LockedTreeSet.java#L21-L48
[spliterators]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java#L26-L61
[tree-source]: https://github.com/hsoliwal/M3jdk21/blob/1011050da4205a901ad079d7711b6b8efd8a9af7/m3/collections/src/main/java/com/m3/collections/M3TreeMap.java#L225-L291
