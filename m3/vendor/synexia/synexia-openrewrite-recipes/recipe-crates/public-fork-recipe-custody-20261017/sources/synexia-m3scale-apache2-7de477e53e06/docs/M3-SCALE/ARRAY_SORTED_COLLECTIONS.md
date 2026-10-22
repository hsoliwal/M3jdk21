<!-- SPDX-License-Identifier: Apache-2.0 -->
# Array-only sorted, filtered and control-byte collections

This extension keeps the same M3 rule: **no helper object per entry**.

Two additional layouts are provided because one layout is not optimal for every CPU
access pattern.

## Hashing: bitset control and byte control

The original primitive hash tables use compact occupancy/deletion bitsets. The new
control-byte variants use one byte per slot:

```text
LongControlHashSet
  long[] keys
  byte[] control       // EMPTY / OCCUPIED / DELETED

LongLongControlHashMap
  long[] entries       // key,value,key,value...
  byte[] control

LongByteControlHashMap
  long[] keys
  byte[] values        // unsigned 0..255
  byte[] control
```

The bitset representation is denser. The byte-control representation spends a small
amount of metadata to make state testing direct and sequential. Both eliminate
`HashMap.Node` / `Map.Entry` per mapping.

## Sorted structures: binary search + a movable gap

A tree is not required merely to keep values sorted.

`LongGapSortedSet`, `LongLongGapSortedMap` and `LongByteGapSortedMap` maintain
sorted order in primitive arrays. They use binary search for lookup and a single
movable empty region for insertion:

```text
sorted prefix     GAP                         sorted suffix
[01 03 08 11] [ _ _ _ _ _ _ _ _ ] [17 22 40 91]
                ^ gapStart          ^ gapEnd
```

Insertion binary-searches the logical array, moves the gap to that position with
`System.arraycopy`, writes into the gap and shrinks the gap by one. A nearby series
of insertions therefore does not have to shift the entire suffix every time.

When a sorted batch is already available, `LongGapSortedSet.addAllSorted` and
`LongLongGapSortedMap.putAllSorted` use a linear merge. This is the intended
batch path: sort/filter once, merge once.

This follows the same idea as maintaining markers or other read-heavy indexes:
the workload decides whether a sorted flat array, a gap-sorted array or a hash table
is appropriate. No tree node is required just for clean API design.

## Filter before insertion

`BoundedLongSortedSet` implements fixed-capacity top-K / bottom-K selection. Once
full it compares a candidate with the current minimum/maximum boundary **before**
performing an array copy. Candidates that cannot enter the retained set are rejected
immediately.

It also supports an accepted numeric range, again checked before mutation.

`FilteredLongGapSortedSet` accepts a single primitive `LongPredicate` for arbitrary
pre-insertion filtering. The predicate is collection-level state; it is not repeated
per entry.

## Selection rule

Use the smallest structure that matches the operation profile:

| Need | Layout |
| --- | --- |
| exact membership, maximum density | primitive hash + packed bitsets |
| exact membership, direct state bytes | long[] + byte[] control |
| sorted, mostly reads | compact sorted arrays |
| sorted, localized inserts | movable-gap sorted arrays |
| sorted bulk ingestion | sort/filter + linear batch merge |
| top-K / bottom-K | bounded sorted array + boundary prefilter |
| arbitrary admission rule | filter-before-insert facade |
| priority dequeue | primitive array binary heap |
| insertion order | flat hash + int[] slot links |

All of these keep collection metadata proportional to the collection/table, not to
Java wrapper objects per element.
