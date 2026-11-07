<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss audit phase 2: filtered views family (m3/collections)

Source: merge-loss audit phase 2 (`S:/m3-scratch/eb-conv/mergeloss2`, `CLEAN_0E.tsv` rows 1 4 9 10 11 13 16 17 19
27 35 42 43 51 52 56 57 61 63 64 65 66: 508 lost lines of `M3FilteredViews`, `M3MutableFilteredViews`,
`M3FilteredLists` and the files those rows name). Every lost line is a PR-side line of a RETAIN (or MERGE_RESOLVE)
superset merge of 2026-10-28 (`ATOM_VERDICTS.tsv` other=NEVER). Re-tested on master `33541216b2cd`, after #2641
(`b8fb8956aaac`, `qualification/filtered-views-owner-20261106`, whose `DECISION.tsv` rows ML2-B/ML2-O already
decided these leaves), and again on this change's base `0b97282f6d1d`: draft #2676 (TREEMAP_ABA_GUARD, not landed
when this was pushed) over master `2ec96ee32ff`. This change stacks on #2676 and leaves its `M3TreeMap.cursorStart`
change in `M3MutableFilteredViews` alone (no rule here targets that file); the gates ran on that base with the #2641
differential harness as #2676 updated it.

| decision | rows | citation |
|---|---|---|
| RESTORE | 42, 27 (Java), 43, 17 (`M3KeyedParallelismPlan`) | the origin commit's own test fails on master and passes on the restored owner |
| PRESENT_EVOLVED | 4, 13, 16, 51, 56, 63, 64, 65 | #2641 B1-B3 (behaviour, not bytes) |
| PRESENT_EVOLVED | 57, 61 | 04d018d7bb5 (#2103) `nullIsCandidate` |
| PRESENT_EVOLVED | 35 (Java), 66 | c9a1d41baaf `countTreeSlots` isEmpty; e39e6424a43 `CollectionView.isEmpty` |
| SUPERSEDED | 19, 52 | fce3b06bf18 / 79a8df95080 / c9cb52a9079 (direct navigation, wrappers); d11f07d78d1 + the pinned `M3FilteredNavigationReconciliationContract` (entry membership) |
| NEC | 1, 9, 10, 11 | private AVL fast paths, no origin test, no master test or caller; #2641 ML2-O KEEP_OWNER |
| HANDOFF | 82 lines of 17, 19, 27, 35 | `M3IndexedListSnapshot`, `M3IndexPathMasks` (+ `M3LongIndexPathMasks` callers), `m3_index_mask_compactor.c`: owners held by other agents, not edited |

Restored members (recipe `apply.py`, MERGELOSS2_FILTERED_VIEWS v1, `EDITS.tsv`):

- leaf 42 (origin 793988c0c6a, retain merge 082b54fa4ab7 #1368): `M3FilteredViews.NavigableSetView.descendingIterator`
  peels a private filter chain and iterates the backing's descending set through the depth-sized `CollectionView`
  plan (master recursed once per layer: `StackOverflowError` at depth 100000); the one-layer branch keeps #2641's
  owner-epoch check.
- leaf 27 (origin 01952e935de, retain merge 1633da209622 #1619): `M3FilteredViews.visibleMember` evaluates the
  stored representative of a comparator-ordered owner reached through private Collection/Set layers
  (`sortedMembershipOwner`, the origin's `sortedOwner(Collection)` renamed: it would overload #2641's
  `sortedOwner(Object)`); master answered `false` for a comparator alias of a visible member.
- leaf 43 (origin d1b9aa9f35c, retain merge 92805fd23aed #1616): `M3FilteredLists` cursor probe also probes the
  reverse direction (bounded providers refuse `next()` at their end before their generation check; "delivered element
  disappeared") and inserts into an empty provider through the provider iterator.
- leaf 17 (origin 3538a25a194, retain merge 6d0e9f5461e6 #1522): `M3KeyedParallelismPlan.put` re-checks the published
  lane failure after a fast admission.

Tests: the origin tests of those rows, relocated verbatim from the retained-variant blobs master carries
(`M3FilteredDescendingDepthTest`/`Contract`, `M3FilteredSortedRepresentativeTest`/`Contract`,
`M3StringTreeConsumerTest`/`Contract`, `M3FilteredListProviderInvalidationTest`, `M3KeyedParallelismConvergenceTest`),
and the reference model `M3MergelossFilteredViewsReferenceTest` (payload `oracle/`): descending iteration and `last()`
of filter chains over `java.util.TreeMap` key views (`navigableKeySet`, `subMap`, `headMap`, `tailMap`,
`descendingMap`, `descendingKeySet`) and the matching `M3TreeSet` views against the TreeMap view's own order and
predicate tape, and generic-filter `contains` against the backing's membership and stored representative.

Files: `DECISION.tsv` (row decisions + HANDOFF lines), `LOST_LINES.tsv` (the 508 lines re-tested now and before #2641),
`apply.py` + `EDITS.tsv` (recipe), `differential.sh` + `classify.py` + `oracle/jdk/m3/collections/
MergelossFilteredViewsDifferential.java` (seeded before/after differential, every differing line classified),
`mutants.py` + `MUTANTS.tsv`, `GATES.tsv`.

Re-run: `python -I apply.py --check`; `JAVA_HOME=<JDK 21> bash differential.sh <base classes> <head classes>
<m3-arrays jar> <out>`; `python -I mutants.py --java-home <JDK 21> --root <repo> --classes <head classes>
--test-classes <head test-classes> --arrays-jar <jar> --console <junit-platform-console-standalone jar> --work <dir>`.

Also in this change: `../mergeloss-01-15-20261106/apply.py` v1.1 names `interop/qualification/bindings-gating-20261107`
(#2648) as the successor owner of `interop/sdk-family-bindings.tsv` (SUPERSEDED_BY row in its `EDITS.tsv`, RULES
unchanged), so its `--check` passes on master again (its `GATES.tsv` row 17).
