<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 indexed filter + stable-sort fusion (2026-10-18)

Product owner: **M3JDK21 `m3/collections`**. Synexia owns only the reusable
OpenRewrite transformation recipe and algorithm comparison:
[hsoliwal/com.synexia PR #12366](https://github.com/hsoliwal/com.synexia/pull/12366).
No Synexia runtime jar, entry-object storage, secondary payload cache or extra
collection hierarchy is introduced.

## Representation and semantics

The canonical, GC-traced `Object[]` payload remains owned by
`M3IndexedListSnapshot`. It is never sorted or compacted destructively.
`int[] positions` is the unique selection/order directory; `from` and
`to` designate a logical slice, and `BitSet physicalMembership` is
computed only when a caller requests membership. The existing Java predicate
produces one transient `byte[]` mask; `M3IndexMaskCompactor.selectPositions`
creates the exact-sized output index directory, optionally via the existing
JNI backend, and maps logical offsets back to physical positions.

`filteredSorted(predicate, comparator)` applies the predicate once per
source logical occurrence in encounter order, then performs stable
bottom-up merge passes on the selected integer offsets, with one `int[]`
scratch lane and at most one final copy. No `Integer[]` boxing,
`Map.Entry`, or per-row Node is introduced. Comparator ties keep their
source-order physical occurrence identity, even when equal values or the
same object occur several times. Nulls use comparator-defined or natural
JDK behavior, including the same failures as the existing `sorted` method.

`M3GenericCollections.filteredSortedSnapshot(source, predicate, comparator)`
exposes a one-copy detached, immutable indexed snapshot to consumers.
The additional overload selects `Backend.NATIVE_REQUIRED` explicitly;
unavailable JNI throws before evaluating the predicate. Java owns every
object reference and runs every callback. Native code sees only primitive
state/coordinate arrays. Source modifications following the factory call do
not modify the resulting detached snapshot.

This is *not* a live `NavigableMap` or `java.util.Collections` wrapper:
existing `M3MutableFilteredViews`, `M3FilteredViews`, `M3LongSortedViews`,
`M3TreeMap`, and `M3SlotMap` remain authoritative for their distinct
backed mutation/navigation contracts. No runtime-wide or `java.base`
replacement is claimed.

## Acceptance and statuses

- Source changes: two existing Java owners plus a deterministic Java
  differential oracle and JUnit adapter on this branch.
- Recipe: Synexia OpenRewrite `com.synexia.m3.Jdk21IndexedFilterSortFusion20261018`,
  manifest with exact before/after SHA-256 for all four source paths.
- Differential reference: `java.util.List.sort` stable order over the
  same logical encounter window; physical slot identity compared separately.
- Coverage: empty, all/none masks, nulls/duplicates, 400 seeded random cases,
  comparator equivalence ties, immutable source, offset slices, missing native
  backend refusal and exception containment.
- Pending: execute Maven/JUnit, recipe fixed-point/refusal, native-enabled
  parity and installed JDK image smoke tests. Do not claim a speedup until
  before/after allocation and CPU measurements are recorded.

Focused Java 21 command (when the full receiver toolchain is present):

```sh
mvn -f m3/collections/pom.xml -Dtest=M3IndexedFilterSortFusionTest test
```

Separate reproducible native parity uses the same self-test's
`--native` argument after loading the compatible JNI library. No new JNI
symbols or JNI object-handle lifetimes are introduced in this refinement.

## Category decisions

See `synexia-openrewrite-recipes/catalogs/m3jdk21-indexed-filter-sort-fusion-20261018.tsv`
in the donor PR. LeetCode array two-pointer, HackerRank arrays and
GeeksforGeeks compaction categories are comparison inputs, not copied code.
Rejected: per-entry object sort and destructively compacting a shared
canonical `Object[]`. Selected: existing M3 owned mask selector and stable
integer-offset merge sort. All third-party site pointers are references only.

## Follow-on admission

Before changing `src/java.base/share/classes/java/util`, inventory that
specific public class, its serialization/reflection/iterator/concurrency
contract, the equivalent M3 backend, and jtreg/installed-image proofs. PR
#1325 remains source custody only; this change is not an adapter between
M3JDK and an external JDK.
