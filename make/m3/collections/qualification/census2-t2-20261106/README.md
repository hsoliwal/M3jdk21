<!-- SPDX-License-Identifier: Apache-2.0 -->
# Census pass-2 group T2: common collection families (P2-122, P2-128, P2-135)

Census pass 2 (S:/m3-scratch/eb-conv/census2, `LEAVES_PROPOSED.tsv` rows P2-122, P2-128, P2-135, com.synexia develop
`d5ad5090d87e`), received into `jdk.m3.collections` by recipe, never by hand. Owner checks on master `5bd70ef520bb`
before any port; census rows re-checked: `CENSUS_RERUN.tsv`.

| leaf | crate | rows | verdict | evidence |
|---|---|---|---|---|
| P2-122 | m3-inline-indexof-20261031 | `M3PackedLongInlineList` (+ closure atom `M3PackedArrayCapacity`, package-private) | PORTED (new class) | no small-buffer inline long list on master (`M3LongArrayList`, `M3LongBigList`, `M3IndexedLongList` are one-array/big/indexed owners); crate model probe (`java.util.ArrayList<Long>`, 17 operations, FNV trace) relocated as `M3InlineOwnerBehaviorProbe` + `M3PackedLongInlineListTest`: donor = relocated stdout IDENTICAL (JIT 650: assertions=43447 trace=96f33fe7bb9056ee; `-Xint` 100: 6438, a4ce9d219f010865) |
| P2-128 | m3-long-sequence-policy-20261014 | `M3ForwardingLongSequence`, `M3BoundedLongSequence`, `M3ValidatingLongSequence`, `M3ObservedLongSequence`, `M3LongSequenceCompletion`, `M3LongSequenceListener`, `M3LongSequenceErrorHandler`, `M3LongSequenceMetrics` | PORTED (new classes) | no long-sequence decorator owner on master; the crate's 7 policy mains relocated as `M3*Proof` + `M3*Test` wrappers (+ `M3PolicyAssertions`, `M3PolicyProbe`): donor = relocated exit 0, output IDENTICAL, 7/7 |
| P2-128 + P2-122 | both | `void add`/`void addAll` of the donor `LongSequence` vs `boolean` in `M3LongSequence` | ADAPTED (declared REWRITE rows) | direct delegate calls return the delegate's answer, terminal appends return true (addAll: false for an empty array), the observed form reuses the donor's own `addIf` pattern; reference model (`java.util.ArrayList`) of the answers: M3_LONG_SEQUENCE_RETURN_MODEL_PASS checks=40000 |
| P2-135 | m3-common-flat-contains-20261019 | `FilteredCollectionView` / `FilteredArrayListPredicatePlan` flat nested contains | ALREADY (verify only) | owner `M3FilteredViews` (FilteredViews/FilteredCollectionView lineage, `filter(Collection, Predicate)`): the crate oracle `FilteredDeepContainsProof` passes on it with checks=5382, the same count as on the donor postimage |
| P2-135 | m3-common-flat-nonarray-20261019, m3-flat-filter-convergence-20261020 | non-ArrayList flat lazy spliterator | HOLD_OWNER_EDIT | absent from the owner: `CollectionView` inherits `Collection.spliterator` (SIZED, eager size); `FilteredNonArrayFlatProof` and `M3FlatFilterJoinProof` fail on it (4 + 3 checks, `ConcurrentModificationException` after a source edit before traversal; `OWNER_VERDICTS.tsv`); an edit of `M3FilteredViews.CollectionView.spliterator`, not a relocation and not a second view class |
| P2-135 | all three | the crate proofs and tests | NOT RELOCATED | their legacy control, subclass and anonymous-view checks need the donor `FilteredCollectionView` type (the owner's views are private); run as owner probes by `differential.sh` |

Owners mapped, not edited: `M3LongSequence`, `M3LongStack`, `M3LongIterator`, `M3LongCollection` (bodies identical to the
donor interfaces except the boolean add/addAll), `M3PackedSupport` (body identical after the TYPE row), `M3HeapGrowth`
(heap growth of the inlined `PackedFlatArrays.grow`; NEC row), `M3LongArrayList` (`PackedLongList` lineage). Two
explicit constructors are declared REWRITE rows (`-Xlint:all` missing-explicit-ctor in the exported package). NEC rows:
`M3HeapGrowth` vs donor `PackedHeapGrowth` growth geometry; `M3PackedArrayCapacity.required` vs the private
`M3ArrayList.required`; `M3FilteredViews` / `M3CollectionViews` vs the donor view (M3CollectionViews differs in contains
predicate call order on 864 oracle cases).

Recipe `relocate.py` RELOCATE_CENSUS v6 = v5 (`qualification/census-g7-20261106`, lineage
`recipe/entryless-lane-kernel-20261106/relocate.py` v3) + method-scoped REWRITE rows (`<DonorType>.<method>#regex`),
multi-line REWRITE replacements, inline-annotated declarations (and a bare self-call is not a declaration), and
default-package donors (`PACKAGE - <package>`). Files: `RELOCATION_MAP.tsv`, `PROVENANCE.tsv`, `differential.sh` +
`differential/M3LongSequenceReturnModel.java.tmpl`, `OWNER_VERDICTS.tsv`, `GATES.tsv`, `CENSUS_RERUN.tsv`,
`RECIPE_MEMORY.tsv`.

Re-run: `python -I make/m3/collections/qualification/census2-t2-20261106/relocate.py --donor-git <com.synexia clone> --donor-rev d5ad5090d87ef8477a56317e60ac0d29c65029f7 [--check]`.
