<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3DualLinkedHashBidiMap (owned)

`com.m3.collections.apache.M3DualLinkedHashBidiMap` is an owned implementation of the public contract of
Apache Commons Collections 4.5.0 `org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap`. It covers
the `()` and `(Map)` constructors, the full `BidiMap` API, `mapIterator()` (a `ResettableIterator`), the
key, value and entry views, the inverse view and serialization. It implements the Commons `BidiMap`,
`MapIterator` and `ResettableIterator` interfaces directly. No Commons implementation class, helper or body
is called or copied.

## Storage

Each direction is one ordered slot table adapted from the first-party `M3LinkedHashMap.Table`:
- lanes: `Object[] keys`, `Object[] values`, `int[] hashes`, `int[] before`, `int[] after`, `int[] positions`
  and `int[] births`;
- an index: a power-of-two `int[]` (slot+1, 0 empty, -1 tombstone) probed with a `length - 1` mask;
- a free-slot chain.

Table 0 maps keys to values and table 1 maps values to keys. The two orders evolve independently, as the two
LinkedHashMaps behind Commons' class do. For example, re-putting a mapping moves its value to the end of
the inverse order. All operations are side-symmetric, and `inverseBidiMap()` is a cached flyweight over the
same two tables.

No Entry or Node object is stored per mapping:
- `entrySet()` iteration creates one boundary entry per `next()`, as Commons' decorator does.
- `mapIterator()`, `forEach`, `equals`, `hashCode` and `toString` create none.

The `births` lane stamps every slot allocation. A returned entry or map iterator stays bound to its slot
while the stamp is unchanged, so it reads live values the way a LinkedHashMap node does. Once its mapping
is removed it keeps its last value, even if the slot is reused.

## Commons-specific semantics kept

- `put` first drops the key's old value and the value's old key, then puts in both directions.
  An existing key keeps its position and its original key object.
- The `setValue` refusal compares owners by identity, so an equal but distinct key object is refused, as
  in Commons.
- `Entry.setValue` returns the installed value. `MapIterator.setValue` returns the previous value. A
  removed mapping is re-added by either.
- The exact `IllegalStateException` and `IllegalArgumentException` messages are kept, along with
  `MapIterator[k=v]` / `MapIterator[]` and reset.
- View bulk rules:
  - `removeAll` and `removeIf` return false on an empty map.
  - `removeIf(null)` returns false.
  - `retainAll(empty)` clears the map.
  - The values view has identity equality.
- Iterators are fail-fast on their direction's structural `modCount`, as LinkedHashMap's are. `forEach`
  reports a concurrent change only when the walk advances, matching `Map.forEach` over Commons' entry
  iterator.
- `toString` has no self-reference guard, because Commons prints its delegate map.
- A value whose hash changes after insertion leaves the inverse direction stale, exactly as in Commons.
- Two Commons quirks that leave the directions out of step are reproduced, because the owner of the
  DualHashBidiMap row (`M3BidiMap`) reproduces them too:
  - `entrySet().toArray()` and `toArray(T[])` return raw entries, like the LinkedHashMap nodes Commons
    exposes there. Their `setValue` skips the ownership check and the inverse, and changes no `modCount`.
    Iterator, stream and `forEach` entries stay guarded.
  - A values-iterator `remove()` drops the value it delivered from the inverse. After a concurrent
    non-structural replacement, that is the stale value, so the new value stays in the inverse.
  - The values view's `containsAll` scans forward values, as Commons' decorated collection does; `contains`
    uses the inverse index.

  `verifyStructure()` checks each table on its own; `verifyInvariants()` adds the mirror check.

## Deliberate differences

- The serialized form is the owned format: size, then key and value pairs, validated.
  - The inverse view serializes as its forward map and resolves back to that map's inverse.
  - Commons rebuilds the inverse order by re-putting. This class does the same, so the orders after a
    round trip agree.
- An entry or map iterator whose mapping was changed through another handle and then removed reports the
  last value it observed. Commons reports the node's final value. The Map.Entry contract leaves this
  undefined.
- The class is final.

## Validation

Run `mvn -f pom.xml test` (Java 21, `-Xlint:all -Werror`). There are 20 JUnit tests.

The seeded differential run applies 96,000 operations to the real Commons provider and to this class, and
makes 960,117 comparisons. Each operation targets the forward map or its inverse at random. Comparisons
cover exact forward and inverse order, the order of every view, and the iterator, entry and `forEach`
traces, including raw entry-array writes and stale values-iterator removals (per-table structure is checked
after every step, since those quirks leave the directions out of step exactly as in Commons).

JaCoCo line and branch coverage is 100%: 469/469 lines and 258/258 branches over 12 classes, gated at 99%
per class. The steady-state allocation probe measures 0 bytes. It covers `put`/`get`/`getKey`/`contains*`,
`removeValue`, inverse `put`/`remove`, `hashCode` and `forEach`.

Relation to `implementations/apache` `M3BidiMap` (the owner of the DualHashBidiMap row):
- That class composes two `M3LinkedHashMap` owners.
- It excludes serialization and post-mutation entries and iterators from its qualification boundary.
- This class embeds the same table pattern with birth stamps so that those cases are exact.
- Merging the two owners is a proposal only.
