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

## Executed focused Java21/JNI proof (2026-10-19)

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

The exact [combined GitHub Actions run 39200001645](https://github.com/hsoliwal/M3jdk21/actions/runs/39200001645)
on the revised PR passed both `indexed-owners-and-jni` and
`actual-javabase` jobs. Strict Java21 compiler, strict C17 native
compiler, `-Xcheck:jni` and explicit `--native` selection were used.

| Oracle | Checks | Result |
| --- | ---: | --- |
| Read-only filtered M3TreeMap | 266,236 | PASS |
| Mutable filtered M3TreeMap | 478,161 | PASS |
| Indexed filter+stable sort, Java | 9,962 | PASS |
| Original deep predicate AVL (100,000 depth) | 3,119 | PASS |
| **New** read-only and mutable filtered/sorted M3TreeSet | 150,626 | PASS |
| Indexed filter+stable sort, JNI-required | 19,911 | PASS |
| Patched actual `java.base` EnumMap | 4,411 | PASS |
| Patched actual `java.base` IdentityHashMap | 78,847 | PASS |
| **Total** | **1,011,273** | **PASS** |

Source admission step printed
`M3_DOOP_SET_SOURCE_SEAL_PASS rows=5` and
`M3_DOOP_SET_SOURCE_SEAL_REFUSALS_PASS cases=2`.
The same five product Java blobs were independently checked against
their matching Synexia canonical source postimages and match byte for
byte. The sealed JUnit adapter is present, but this targeted CI compiles
the standalone oracle, not the entire Maven/JUnit reactor.

A historical candidate failed because its differential oracle
treated a comparator-accepted `null` as insertable into every bounded
`TreeSet` view. The canonical test was repaired to assert the JDK
and M3 range exception; the source postimage and SHA-256 manifest
were regenerated and received. The passing run also covers the
`SortedSet` overload separately from `NavigableSet`.

**Still open:** executed composite Maven/OpenRewrite fixed-point and
refusal gates, full M3JDK21 built-image/jtreg, whole-repository GC and
JNI resource lifetime, M3LFU/pressure, provider integration and
before/after performance/allocations. Focused Java/native checks
do not certify those global gates.
