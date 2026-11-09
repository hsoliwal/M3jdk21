<!-- SPDX-License-Identifier: Apache-2.0 -->
# Census group G4: long-long indexed sorted view (L084)

Census leaf L084, crate `m3-longlong-indexed-sorted-20261017` (com.synexia develop `ca1aafc4d94f`): three new files.

| leaf | verdict | classes | oracle |
|---|---|---|---|
| L084 | PORTED (new class) | `M3LongLongIndexedSortedView`: immutable long-to-long sorted-map snapshot over two primitive lanes and a sorted `int[]` physical-index directory; O(log n) lookup and navigation, O(1) key-range and logical slices, mask selection through the existing `M3IndexMaskCompactor` | donor = relocated dumps IDENTICAL (779,583 lines, 20,000 seeded snapshots); TreeMap model 747,982 modelled ops, 0 mismatches |
| L084 | PORTED (tests) | `M3LongLongIndexedSortedViewProof`, `M3LongLongIndexedSortedViewTest` | `M3_INDEXED_SORTED_MAP_PROOF_PASS evaluations=1368500`, donor = relocated |

NEC (recorded, not decided): overlapping sorted primitive long-key owners (`M3PrimitiveSortedArrayMap`, `M3LongSortedSnapshot`,
`M3LongLongTreeMap`); none is an immutable long-to-long map snapshot with index-directory slices.

Recipe `relocate.py` (RELOCATE_CENSUS v4), `RELOCATION_MAP.tsv`, `PROVENANCE.tsv`; proof `differential.sh` +
`differential/M3LongLongIndexedSortedViewDump.java.tmpl`, `GATES.tsv`, `CENSUS_RERUN.tsv`, `RECIPE_MEMORY.tsv`.
