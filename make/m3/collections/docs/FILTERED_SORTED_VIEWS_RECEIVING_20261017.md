<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered and sorted collection views: recipe-received M3JDK21 proving ground (2026-10-17)

## Ownership and receiving status

These wrappers are **independent M3 collection components**, not replacements for OpenJDK
`java.util` and not evidence that the repository-wide collections phase has been promoted.
The required receiving sequence remains **STRING -> arrays -> collections -> AST/compiler**.

Canonical recipe/provenance ownership is in Synexia:

- filtered/sorted family receiver: [com.synexia #11995](https://github.com/hsoliwal/com.synexia/pull/11995),
  named recipe `com.synexia.m3.Jdk21FilteredSortedViews`;
- current consolidation/refinement: [com.synexia #12098](https://github.com/hsoliwal/com.synexia/pull/12098),
  Maven recipe `M3FilteredNavigableFacadeRecipe`, with exact M3JDK21 preimages and postimages;
- target receipts: `m3/collections/qualification/FILTERED_SORTED_VIEWS_20261017.md` and
  `qualification/filtered-navigable-consolidation-20261017/SOURCE_RECEIPT.tsv`.

The earlier statement that this wrapper had no qualified recipe is therefore superseded. The
correct response to future source drift is to update/test the canonical recipe and replay it, not
hand-edit generated product source.

## Current contracts and implementation owners

- `M3FilteredViews`: live read-only Collection, Set, SortedSet, NavigableSet, SortedMap and
  NavigableMap views. No element snapshot or predicate-result cache.
- `M3MutableFilteredViews`: separately admitted mutable NavigableSet/NavigableMap contract.
- `M3FilteredNavigableSet`: public compatibility facade over the read-only NavigableSet owner;
  it does not retain a second filtering/navigation algorithm.
- `M3FilteredLists`: separate List-specific owner because index/subList/list-iterator contracts
  are not equivalent to Set/Map filtering.
- `M3LongFilteredView`, `M3LongSortedViews`, `M3LongSortedSnapshot` and
  `M3LongCompositeViews`: primitive-long owners with no boxed Entry payload graph.

Sorted navigation delegates endpoint/comparator validity to the backing sorted collection. The
current read-only NavigableSet/NavigableMap implementation performs one bounded backing range seek
and scans rejected members with that range iterator. Worst-case rejected-item scanning remains
linear; no wall-clock speed claim is made.

## Dynamic arrays, indexes, masks and JNI boundary

M3 collection state remains centered on dynamic primitive/object lanes, physical offsets, logical
indexes and masks. Existing owners such as `M3IndexMaskCompactor`, `M3IndexMaskWindow`,
`M3IndexPathMasks`, state-mask arrays and object-slot projections remain the canonical path for
stable precomputed coordinates. Their optional JNI kernels operate on primitive arrays/masks.

Arbitrary Java Predicate/Comparator callbacks are intentionally **not** sent through JNI here:
doing so would add callback crossings and new exception/GC-lifetime semantics without an admitted
contract. No JNI ABI changes in this wrapper refinement.

## Category catalogue / donors

| Category | Reference | Decision |
| --- | --- | --- |
| Live filtered view | Guava Collections2/Sets.filter | Contract reference only; mutable Guava behavior differs |
| Ordered range view | OpenJDK 21 NavigableSet/TreeSet/NavigableMap/TreeMap | Comparator/range semantics authoritative |
| Materialized selection | Eclipse Collections select/reject | Separate eager/snapshot contract |
| Predicate admission | Apache Commons predicated collections | Different insertion-guard contract |
| Primitive compaction | Existing M3 index/mask lanes | Reuse existing Java/JNI owners |
| LeetCode / HackerRank / GeeksforGeeks | filtering, comparator, selection and bit-mask categories | Taxonomy only; no submission/editorial source copied |

Exact decisions for the current refinement are in
`qualification/filtered-navigable-consolidation-20261017/CATALOGUE.tsv`.

## Verification and hold

Selected Java21 proof:

```sh
bash m3/collections/qualification/filtered-navigable-consolidation-20261017/verify.sh
```

It verifies exact generated Git blobs, compiles with `--release 21 -proc:none -Xlint:all -Werror`,
and runs both the existing randomized `M3FilteredViewsContract` and the generated consolidation
oracle. Broader Maven/JUnit/module/JDK-image/jtreg/native-platform/performance gates remain separate.

A source receipt, finite oracle or exact fixed point does not promote the entire collection family.
No java.base, HotSpot or public Java SE API is changed by this bounded receiver.

## Follow-on categories

Continue file-by-file and contract-by-contract: List wrappers, mutable sorted variants where a real
non-Navigable owner requires them, map key/value/entry views, snapshot/index-backed filters,
dynamic-array growth, offsets/state masks, LRU/MRU policies and primitive/native batch operations.
Preserve comparator representatives, Map.Entry mutation rules, callback effects, concurrent
modification, serialization and identity explicitly before adapting each family.
