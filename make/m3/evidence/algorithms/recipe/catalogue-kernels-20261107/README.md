<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/algorithms: the computing kernels of the algorithm-catalogue family (leaf catalogue-kernels-20261107)

Census pass 2 routed 21 rows of `S:/m3-scratch/eb-conv/census2/LEAVES_PROPOSED.tsv` to the algorithm-catalogue family
(P2-019, -070, -076, -078, -081, -082, -083, -091, -107, -109, -110, -111, -112, -114, -123, -124, -138, -140, -141,
-142, -143; about 8k changed lines). The donor is `hsoliwal/com.synexia` develop
`78cf2ad4c7e56f7b0ade5aa206c491bc866178a6` (fetched 2026-11-07). Phase 1 decided an owner for every class of those
rows and of their closure. Phase 2 relocates the product algorithms into `org.m3jdk.algorithms` (package
`jdk.m3.algorithm`) by recipe. Everything else goes to its owner: [DECISION.tsv](DECISION.tsv) has 65 rows, and the
42 rows owned by the peer session com-synexia-4d are listed in [HANDOFF_4D.tsv](HANDOFF_4D.tsv).

## Owner decisions ([DECISION.tsv](DECISION.tsv))

| decision | rows | where |
|---|---|---|
| ABSORB, this leaf | 12 | 5 main classes and 7 donor tests, below |
| ABSORB, handoff | 41 | catalogue, corpus data and CLIs go to make/m3/tooling (ChallengeCategoryCatalog, the LeetCode atlases and the replay/export/parity gates, ProblemCatalogue/ProblemCategory*, AlgorithmShape, CompetitiveProblemReview, ChallengeDonorPassLedger, CompetitiveCapabilityPlanIndex, DonorMechanicalShapeCatalog -> make/m3/tooling/donor-catalogue); M3ProblemShapeCatalogue -> make/m3/recipes (jdk.internal.m3.recipe); M3CompilerPatternHints -> jdk.compiler (owner present, census2 UNAPPLIED); M3Wave9RollbackContract -> cpu-tools |
| HELD, handoff | 1 | PatternDetectionAlgorithm: modified DP-CORE (Apache-2.0), but its `parser.*` closure has no M3 owner |
| ALREADY | 2 | wave9 flow contract (census2-a5-20261106); the donor progress contract = `M3ProgressMonitor` |
| NEC | 4 | wave9 `M3MinCostFlow` (census2-a5); `ChallengePostingQuery` + its test vs `jdk.m3.arrays.M3IntPostingIndex`; the lombok `Strings` facade vs the landed string families |
| OMIT | 5 | JNI self-tests and loader (`ChallengeZSearchSelfTest`, `IntLcsBridgeSelfTest`, `ChallengeNativeSearch`), the opt-in wall-clock benchmark, the tool-only `PatternizerProof` main |

## Classes added (recipe [relocate.py](relocate.py), map [RELOCATION_MAP.tsv](RELOCATION_MAP.tsv))

| M3 class | donor | kept / dropped |
|---|---|---|
| `M3ZAlgorithm` (P2-078) | `StringsZAlgorithm` | all: `zSearch`, the first hit through a pattern-only extended Z table with O(m) auxiliary space, and `zArray`, its closure. `zArray` is NEC with `M3StringAlgorithms.zFunction`: the reference test asserts the two are equal |
| `M3OneMismatchWindowSearch` (P2-076) | `ChallengeOneMismatchWindowSearch` | all: windows at Hamming distance at most 1, over forward and reversed Z prefix geometry |
| `M3ZPrefixMatch` | `ChallengeZSearch` | kept: `prefixMatchLengths`. Dropped: `indexOf` and `indexOfJava`, the same named algorithm as `zSearch`, and the JNI lane |
| `M3IntLcsWitness` (P2-138) | `IntLcsWitnessOracle` (crate m3-int-lcs-witness-20261105) | kept: the Hirschberg witness, its bounds, `referenceLength` and `isSubsequence`. Dropped: the JNI method and the proof harness (`Proof`, `decode`, `main`) |
| `M3ProgressChecks` (package-private) | `ProgressMonitors` | kept: `nonNull` and `checkCanceled` (thread interruption first, then the monitor). Dropped: the child slices, owned by `jdk.m3.collections.M3ProgressMonitor.split` |

Donor tests ported as JUnit: `M3ZAlgorithmFirstHitContractTest`, `M3ZAlgorithmExtentInvariantTest`,
`M3OneMismatchWindowSearchTest`, `M3OneMismatchSingletonFastPathTest`, `M3OneMismatchSparseResultsTest`,
`M3ZPrefixMatchTest` (prefix-geometry test only) and `M3IntLcsWitnessTest`. M3JDK-authored files:
`M3CatalogueKernelsReferenceTest`, which holds the reference models, the replay of the crate's proof digest and the
catalog entry points, and `M3CatalogueKernelsDump`, the differential driver.

The recipe law is that of algorithm-extras-20261106: header, package, mapped imports, TYPE renames, declared drops
and one anchor; every other byte is kept. It adds three row kinds:

- **EDIT**: the census2-a5 law, hash-pinned. E1 makes the helper package-private. E2 to E5 rewrite four class
  comments that described dropped members.
- **MEMBER**: the donor `IProgressMonitor.noop()` becomes the owner's `M3ProgressMonitor.none()`.
- **DONOR_ONLY**: a donor closure source compiled on the donor side only.

`body_match` is TRUE for 9 of 12 files. `body_match_edited` is TRUE for 12 of 12: only the declared EDIT bytes
differ. The one owner edit is three `M3AlgorithmShapeCatalog` rows (z-search, one-mismatch-window, lcs-witness). It
is made by [apply.py](apply.py) from [EDITS.tsv](EDITS.tsv), which pins the sha256 of the owner before and after
the edit.

## Gates ([GATES.tsv](GATES.tsv); lessons in [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv))

- **Build**: real pom, `-Xlint:all -Werror`, 0 WARNING lines, 84 tests. The baseline was 48 tests, so the count only
  grows.
- **Differential**: the donor and relocated dumps agree byte for byte, and so does the `-Xint` run (60,013 lines,
  19 members).
- **javap**: `javap -p` matches for 7 of 7 classes.
- **Mutants**: 9 of 9 are killed, in 5 classes.
- **Untouched classes**: 64 of 64 main and 38 of 38 test class files are byte-identical to the master build. The
  only differing class file is the owner edit.
- **Donor proof**: the crate's proof figure (`cases=32651 sha256=97fe8bdf...`) holds on the donor side, under JIT and
  `-Xint`. The relocated owner reproduces it in JUnit.

## Run

```
python -I m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py --donor-git <donor clone> --donor-rev 78cf2ad4c7e56f7b0ade5aa206c491bc866178a6 [--check]
python -I m3/algorithms/recipe/catalogue-kernels-20261107/apply.py [--check]
MAVEN_REPO_LOCAL=<repo> JAVA_TOOL_TMP=<tmp> bash m3/algorithms/recipe/catalogue-kernels-20261107/verify.sh <donor clone> 78cf2ad4c7e56f7b0ade5aa206c491bc866178a6 <scratch>
```
