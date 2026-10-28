<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 filtered/sorted set physical-slot receiver — 2026-10-19

Product runtime: **M3JDK21**, not an external M3 + JDK wrapper layer.
Source parent: `22e7a14ccd2998d994cb9a74abcdc7abd2086201` of
[M3JDK21 #1426](https://github.com/hsoliwal/M3jdk21/pull/1426),
which preserves the Java21/C17 JNI mask-selector and actual patched
`java.base` map source ancestry. New changes extend existing production
M3JDK21 owners, not a parallel collection implementation.

## Serial per-file inventory / atoms

| Owner | Original behavior | Refinement |
| --- | --- | --- |
| `M3TreeSet.java` | key-only `M3TreeMap` AVL owns all keys, bounds and descending topology | package-private `indexedOwner()` exposes the **existing** owner only to sibling collection algorithms |
| `M3FilteredViews.java` | generic JDK SortedSet/NavigableSet adapter repeatedly creates filtered iterators and subset ranges | fast path for **M3TreeSet** only: direct `firstSlot/lastSlot/nextSlot/previousIndexedSlot/nearIndexedSlot/findSlot` traversal, lazy readiness and owner revision; non-M3 sources unchanged |
| `M3MutableFilteredViews.java` | generic source iterator/range construction and per-step cursor seeks | M3 owner-only direct index iteration, lookup, bulk removal, navigation, polling and iterator removal, with existing AVL erase and physical revision; non-M3 fallback unchanged |
| `M3FilteredSortedSetSlotSelfTest.java` | absent | seeded JDK TreeSet differential with null-accepting comparators, bounded and descending views, live mutation, iterator/CME, retained key representative and read-only contracts |
| `M3FilteredSortedSetSlotTest.java` | absent | Maven/JUnit adapter to the same deterministic oracle |

No persistent per-element Entry, Node, boxed offset or redundant payload
exists. Java object references remain reachable through the original
GC-owned `M3TreeMap` key/value arrays. Filter/visibility predicates
execute in Java; the existing C17 JNI primitive-mask selector is
still tested separately for detached bitmap snapshots, not reused
for individual Java object predicate calls.

The external contracts determine whether a view is read-only or
mutable, backed, descending, bounded or comparator-identity-aware.
Logical equality of a query is **not** the identity of its retained
stored key; the predicate evaluates `keyAt(actual physical slot)`.
Null keys are allowed only when the supplied comparator admits them.
Read-only mutators still throw. Iterator remove is permitted only
for mutable views, using one index deletion with owner revision
tracking. Range and navigation bounds are not loosened.

## Canonical recipe and problem category

Synexia recipe:
`com.synexia.m3.Jdk21FilteredSortedSetSlots20261019`.
Its exact SHA-256 manifest seals three before/after Java owners,
two ABSENT-to-Java test postimages and original source postimages.
Catalogue:
`synexia-openrewrite-recipes/catalogs/m3jdk21-filtered-sorted-set-slots-20261019.tsv`.

Algorithm discovery uses LeetCode 94 tree inorder, HackerRank tree
inorder and GeeksforGeeks iterative traversal as **taxonomy**, not
copied donor code. The first-party original indexed AVL owner is
selected for its bounded/ranged successor/predecessor properties,
with no node object or recursion.

## Admission matrix (to be executed)

- Java 21 strict compiler for M3 collection owners with the new
  standalone selftest and the previous 860,647-check collection/JNI
  and patched java.base oracles.
- All 8 seeded bounded/descending and read-only/mutable JDK TreeSet
  comparisons, null, representative, iterator, bulk and AVL invariants.
- Existing JNI `NATIVE_REQUIRED` C17 source compile and runtime
  parity; no new exported JNI symbol.
- Synexia recipe fixed-point, refusal, source drift and rewrite
  type-attribution gates.
- JDK runtime image, complete `java.util` suite, JNI/GC,
  M3LFU/pressure and performance/allocations.

Only the first two and existing JNI checks are wired into the
focused combined workflow at initial PR submission. They are **not**
represented as passing until GitHub Actions confirms them.
