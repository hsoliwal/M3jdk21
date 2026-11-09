<!-- SPDX-License-Identifier: Apache-2.0 -->
# Census group G1: deep filter composition (L069 + L070)

M3JDK convergence census (S:/m3-scratch/eb-conv/census: LEAVES_PROPOSED.tsv rows L069 and L070, crate
`m3-deep-filter-list-20261017` of hsoliwal/com.synexia develop `ca1aafc4d94f`), received into `jdk.m3.collections`
by recipe, never by hand. Census rows re-run on this branch's base: `CENSUS_RERUN.tsv`.

| leaf | verdict | crate | classes / hunks | oracle |
|---|---|---|---|---|
| L069 | PORTED (new class) | m3-deep-filter-list-20261017 | `M3FilteredMaps` (live write-through filtered `Map`/`SortedMap`/`NavigableMap`, composition through the existing package-private `M3CollectionViews.orderedAnd(BiPredicate)`) | differential: donor = relocated dumps IDENTICAL over 600,000 seeded ops; reference model 447,269 modelled ops, 0 mismatches |
| L069 | HOLD_DEPENDENCY | m3-deep-filter-list-20261017 | `M3FilteredSortedMultisets`: every view method takes `M3IndexSortedMultiset<E>`, absent from the donor pin and from master | - |
| L070 | SUPERSEDED_BY_OWNER (no edit) | m3-deep-filter-list-20261017 | `M3FilteredLists` 1-line hunk (nested predicate via `orderedAnd`): the owner keeps the restored stack-safe `FilterChain` plan (`qualification/filtered-list-history-restore-20261017`); NEC row records the two composition owners | crate oracle `M3DeepFilteredListsProof` relocated (3 declared REWRITE lines: the shape check follows the owner) passes on the unedited owner, checks=55469 depth=65536 |
| L070 | PORTED (tests) | m3-deep-filter-list-20261017 | `M3DeepFilterCompositionProof`, `M3DeepFilteredListsProof` + 2 JUnit wrappers (WRAP template) | M3_DEEP_FILTER_PASS checks=1312160; donor = relocated outputs IDENTICAL |
| L070 | HOLD_DEPENDENCY | m3-deep-filter-list-20261017 | `M3DeepFilteredSortedMultisetsProof` (oracle of the held multiset views) | - |
| L070 | ALREADY | m3-deep-filter-list-20261017 | `M3CollectionViews` postimage: APPLIED_SUPERSET (56/56 added lines present) | - |

Files: `relocate.py` (RELOCATE_CENSUS v4, derived from `recipe/entryless-lane-kernel-20261106/relocate.py` v3: `.java.txt`
donors, SPDX block headers kept, WRAP rows), `RELOCATION_MAP.tsv` (rows, SKIP and NEC decisions), `PROVENANCE.tsv`
(donor commit, blob, sha256, URL, target sha256, body_match), `differential.sh` + `differential/M3FilteredMapsDump.java.tmpl`
(donor vs relocated + brute-force model), `GATES.tsv`, `RECIPE_MEMORY.tsv`.

Re-run: `python -I m3/collections/qualification/census-g1-20261106/relocate.py --donor-git <com.synexia clone> --donor-rev ca1aafc4d94f9dd1ee288226dcca070cc60991c9 [--check]`.
