<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# wordbreak-20261107: vocabulary tree and word-break plan in M3JDK (recipe INDEXSTRING_RELOCATE_IX2 v1)

This is leaf 2 of the indexstring word-break family. It closes census2 rows P2-108 (`MIndexWordBreakPlan`) and
P2-056 (word-break, previously blocked on the indexstring family). It builds on leaf 1,
`recipe/trie-core-20261107` (#2639).

| Owner | What it does |
| --- | --- |
| `MIndexVocabularySearchTree` | Frozen vocabulary of distinct spellings with forward and reverse lexical ranks. Lookups: `exactRow`, `prefixRows`, `suffixRows`, `longestPrefixRow`, `longestSuffixRow`. `precomputeTries` prepares forward and reverse tries (trie core), after which there are rank ranges and `affixRows`. |
| `MIndexWordBreakPlan` | Trie-guided segmentation of a UTF-16 input into vocabulary rows. `canSegment`; `segment` (the historical first-predecessor result); `minimumSegments` (fewest segments, stable ties); per-query workspace and transition limits that refuse rather than return partial answers. A result gives boundaries, rows, segment views and row spellings. |

```java
MIndexWordBreakPlan plan = MIndexWordBreakPlan.compile(MIndexVocabularySearchTree.compile(List.of("pine", "apple", "pen", "applepen", "pineapple")));
plan.minimumSegments("pineapplepenapple").segmentViews();   // [pine, applepen, apple]
```

## Adaptation (RELOCATION_MAP.tsv)

The donor read its rows from `MIndexStringPrecomputation`. That type is a vocabulary of donor `MIndexString`
values, and its closure is 356 classes; M3JDK's `java.lang.M3String` is a different design.

Here the tree owns its rows. `compile(Collection<? extends CharSequence>)` keeps distinct spellings in
first-occurrence order and freezes each one once as `FrozenChars`. This matches the donor, whose row map is keyed
by `MIndexString`, and `MIndexString.equals` is spelling equality.

On the plan, inputs and segment views are `CharSequence`, so a String input gives String segments. The donor's
`MIndexStringBitOps.regionEquals` becomes a private unit loop: it is one equality call and needs no two-class span
helper.

## Proof (GATES.tsv)

- Maven package with the module's flags: all five exec mains pass, including `MIndexWordBreakSmokeMain`.
- javac live vs leaf: 0 warnings under -Werror.
- `Ix2Diff`: the real donor tree, plan, trie core and `MIndexStringBitOps` against the relocated owners. The donor
  precomputation, `MIndexString` and range view are stubs that keep the donor's row and equality rules. Two seeds
  give 6.08M checks, 0 mismatches and equal digests, over 228k segmentable inputs; tight-limit refusals are
  included.
- NEC row (`NecWordBreak`): `canSegment` agrees with `jdk.m3.algorithm.M3DynamicProgrammingAlgorithms.wordBreak`
  (LC139, boolean) on 300,000 cases. Nothing is merged; the plan additionally returns boundaries, rows and a
  minimum segmentation, and counts UTF-16 units.

## Not in this leaf

- The donor's 17-test JUnit oracle (`MIndexWordBreakPlanTest`) is written against `MIndexString` interning and
  composition. The differential covers the plan's answers instead.
- `MIndexVocabularyRelationIndex` / `MIndexVocabularyIndex` (the donor precomputation's other consumers): not needed
  for word break.
