<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 primitive filtered/sorted composition receiving

**Receiver scope:** existing separately built `m3/collections` module.
`java.util`, `java.base` and the installed M3JDK image are **not** modified.

The canonical source-sealed Maven/OpenRewrite generation is in
[Synexia #11911](https://github.com/hsoliwal/com.synexia/pull/11911),
revision `bf62eed6b93b7feaee8426e2de736b04c95e45b5`. The original target parents are frozen at
`41ff6da0c7c8e6090a8adb6596602ecde96ea550` (after primitive masks).

## Runtime behavior
- `M3LongFilteredView` now extends the existing `M3LongCollection`
  contract; no second filtered collections hierarchy. Nested filters compose
  their short-circuit predicates over a **single source owner**; no retained
  intermediate wrapper arrays or element nodes. It remains a live read-only
  view (even an empty `clear()` throws).
- Existing `M3LongSortedViews.filter/subSet/headSet/tailSet/unmodifiable`
  remain live sorted-set decorators, with their original predicate/range
  semantics and backed mutations.
- `sortedSnapshot` and `filteredSortedSnapshot` return independent,
  duplicate-preserving, read-only primitive snapshots. A source traversal
  and primitive sort produce one resident `long[]`; a short-lived primitive
  selection workspace may be copied to its exact survivor length.
- `maskedSortedSnapshot` uses the established
  `M3IndexMaskCompactor.selectLong` array/mask kernel, one exact selected
  `long[]`, then `Arrays.sort` in place. It never asks JNI to call a
  Java predicate, retains no pointer, and creates no `Map.Entry`,
  `Long` or per-row intermediary. JNI remains an explicit batched option
  for the existing int-row mask backend, separately verified here.
- Snapshot spliterators truthfully advertise SORTED/ORDERED and SIZED; they
  do **not** advertise DISTINCT, because duplicate rows survive. Unmodifiable
  snapshot `clear` rejects unconditionally.

## Tests and remaining gates
The new standalone Java 21 contract test exercises 96 randomized input
cases with nested live filtering, backed mutation visibility, snapshot
isolation, sorted duplicates, signed extrema, mask combinations, Java
SortedSet navigation, fail-fast drift, rejected mutation and truthful
spliterator characteristics. JDK boxed TreeSet values occur only in tests.

Focused CI `.github/workflows/m3-filtered-sorted-composition-receiver.yml`
checks all existing array/mask source hashes, runs Java21 compilation
and actual primitive self-tests, then the **existing** C17 JNI mask
backend under `-Xcheck:jni`. No added JNI symbol is needed for Java
predicates or primitive sorting. Existing JNI owners are reused.

Pending: exact hosted result, Maven/OpenRewrite SDK, full JUnit/reactor,
binary ABI, installed-JDK `java.util` interposition, five-library
Guava/Eclipse/Commons/Spring semantic census, concurrency, serialization,
performance measurements and independent review. Preserve
**String → arrays → collections → AST/compiler** receiving order.
