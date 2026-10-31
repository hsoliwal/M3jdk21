<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 read-only filtered/sorted map DOOP source refinement

**Product runtime owner: `hsoliwal/M3JDK21`.** This is an internal
behavioral-atom refinement of existing `M3FilteredViews`, not a new
M3-to-JDK integration layer. No per-record `Map.Entry` or `Node`
storage is added. The original `M3TreeMap` owns all arrays, physical
slots, comparator, parent/left/right AVL indexes, backing range bounds,
and Java object references.

Synexia recipe/provenance owner: `com.synexia.m3.Jdk21ReadonlyDoopSlots20261019`
in `hsoliwal/com.synexia`; see its
`synexia-openrewrite-recipes/catalogs/m3jdk21-readonly-doop-slots-20261019.tsv`
for per-operation category, donor selection, complexity, license and
proof disposition.

## Serial per-file atoms

1. In `M3FilteredViews.SortedMapView`, an existing `M3TreeMap`
   triggers a direct-slot path for get/containsKey and retained key,
   size/isEmpty, entrySet's selection, values(), containsValue,
   forEach, equals, hashCode, toString, firstKey and lastKey.
   The fallback for a non-M3 SortedMap is unchanged.
2. In `M3FilteredViews.NavigableMapView`, bounded lower/floor/
   ceiling/higher navigation uses the incumbent AVL
   `nearIndexedSlot`, `nextSlot` and `previousIndexedSlot`,
   skipping hidden slots without constructing intermediate entries.
   This applies equally to descending and bounded M3TreeMap views.
3. Only explicit entry-returning API operations, including
   `entrySet().iterator().next()` and `lowerEntry`, construct the
   returned immutable entry. Key-only navigation does not.
   `Collections.unmodifiableSortedMap/NavigableMap` remains the
   exact public read-only contract boundary; no mutation admission
   is weakened.
4. An arbitrary supplied JDK SortedMap/NavigableMap still follows
   the existing entry-iterator fallback because M3 cannot control
   a third-party backing store's representation.

## Correctness and costs

- Exact cardinality of a live view with arbitrary predicates cannot
  safely be precomputed without a source mutation generation and
  predicate-validity contract; size scans O(n) and has O(1)
  auxiliary slot storage.
- AVL neighbor search is O(log n) for its initial candidate, plus
  the work needed to skip hidden candidates; worst-case O(n).
  Whole-view traversal is O(n) amortized over the existing
  parent-indexed tree topology.
- Array payloads remain GC-traced. No Java object addresses are
  retained by C/JNI. Existing `M3IndexMaskCompactor` Java/C17 JNI
  kernels remain the only optional route for already-precomputed
  primitive masks; no native invocation is used to evaluate Java
  predicates on a live view.
- The change adds no `M3LFU` state. Reclamation cannot evict live
  authoritative mapping keys/values; any future cold-cache strategy
  may discard only validated derived indexes or an explicitly
  recoverable, pinned payload.
- Comparator-equivalent query aliases evaluate the stored key
  representative and its stored value, including null-valued
  mappings. Changes made directly to the owner remain visible.

## Admission and evidence

New `M3ReadonlyFilteredSlotSelfTest` and JUnit adapter exercise
four ascending/descending and bounded/unbounded configurations,
140 seeded mutations per configuration, null/hidden values,
comparator-equal aliases, key and entry navigation, values/entry
iteration, equals/hash/toString, immutable mutation refusal and
AVL internal consistency.

A focused GitHub Actions workflow compiles actual M3JDK21 owner
sources using Java 21 and runs this oracle plus the previously
submitted `M3MutableFilteredSlotSelfTest` and
`M3IndexedFilterSortFusionSelfTest`.

Full JUnit gate (requires the repository's admitted Maven artifacts):

```sh
mvn -f m3/collections/pom.xml \
    -Dtest=M3ReadonlyFilteredSlotTest,M3MutableFilteredSlotTest,M3IndexedFilterSortFusionTest test
```

Source-receiving, OpenRewrite fixed point/refusal, JNI/native
parity, performance, M3LFU/GC, installed JDK image and `java.base`
jtreg gates must be verified separately. This change alone does
**not** certify full M3JDK21 collection-family integration.

## Executed focused Java21 gate

On 2026-10-19, GitHub Actions run 39185305177 completed success: actual M3JDK21 owner source compiled with Java21 warnings-as-errors and the three standalone differential oracles passed (266236 + 478161 + 9962 = 754359 checks). JNI=false in the filter/sort oracle. The installed JDK, full Maven/JUnit and native JNI parity gates remain pending.
