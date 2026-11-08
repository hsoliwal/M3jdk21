<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/algorithms: byte-pattern search kernels of the challenge corpus (leaf corpus-search-20261108)

Census pass 4 of the com.synexia -> M3JDK convergence programme (S:/m3-scratch/eb-conv/census4, 2026-11-07; ledger
`docs/M3-SCALE/runs/20261106-m3jdk-convergence` of hsoliwal/com.synexia) covered the 200 module roots the earlier
passes never read. Its largest unowned family is `synexia-algo`, and inside it the package
`com.synexia.algorithms.corpus` (219 main files, 39.5k lines, 228 donor tests) closes on itself: its only sibling
dependencies are the progress contract, the `AlgorithmShape` vocabulary and, for two files, the javac tree API
(S:/m3-scratch/eb-conv/algo/CLOSURE_SUMMARY.tsv). `recipe/catalogue-kernels-20261107` had decided 18 of its classes
(Z search, one-mismatch window, the posting NEC, the JNI loader) and left two kernels as DONOR_ONLY "corpus search
lane, not this leaf". This leaf is that lane. The donor is `hsoliwal/com.synexia` develop
`a7b7bb639802c0042c11cf2a5dc8427edd6a9775` (the census4 pin).

Phase 1 decided an owner for every one of the 201 undecided classes and their 228 tests
([DECISION.tsv](DECISION.tsv), 318 rows): the class of each file is mechanical (S:/m3-scratch/eb-conv/algo/tools/classify.py:
a `main`, I/O and process imports, static primitive-signature kernels, catalogue-data names), the named kernels are
decided by hand and verified here. Phase 2 relocates the two computing kernels into `jdk.m3.algorithm` by recipe.

## Owner decisions ([DECISION.tsv](DECISION.tsv))

| decision / scope | rows | where |
|---|---|---|
| ABSORB, this leaf (THIS_PR) | 4 | `ChallengeMultiPatternSearch`, `ChallengePrefixIndex` and their two donor tests, below |
| NEC (NONE) | 4 | `ChallengeRabinKarpSearch` (+ test) = the landed `M3PrimitiveStringAlgorithms.rabinKarpIndexOf`, compared on every seeded case by the differential; `ChallengePostingQueries` (+ test) = facade of the posting kernels already NEC against `jdk.m3.arrays.M3IntPostingIndex` |
| HELD | 6 | `ChallengeSupersetKernels` (1,488 lines, 16 donor tests: the next leaf, NEC against the landed families first; its `ProgressAlgorithm<I,O>` contract has no M3 owner), `JavaSourceFingerprint` and `NativeFunctionAtoms` (owner decision open: m3/tooling vs jdk.m3.text / m3/native), their tests |
| ABSORB, handoff (HANDOFF_D7) | 304 | catalogue data (84 files), corpus consolidation and custody I/O (51), CLIs (33), recipe-pass and work-order tooling, their tests: owner m3/tooling, the landing lane com-synexia-d7 (ex-4d); rows in [HANDOFF_D7.tsv](HANDOFF_D7.tsv) |

## Classes added (recipe [relocate.py](relocate.py), map [RELOCATION_MAP.tsv](RELOCATION_MAP.tsv))

| M3 class | donor | kept / dropped |
|---|---|---|
| `M3MultiPatternSearch` (419 lines) | `ChallengeMultiPatternSearch` (734 lines) | kept: `prepare` (immutable byte automaton: sorted edge arrays, failure and output links, per-node subtree counts, flat pattern storage), `Prepared.patternCount/totalPatternBytes/pattern/prefixCount/startsWith/containsExact/exactCount`, the static `prefixCount/startsWith/containsExact/exactCount` (with and without monitor), `firstOffsets(Prepared, byte[])` (+ monitor) and `firstOffsetsJava(Prepared, byte[])` (the explicit Java lane; identical to `firstOffsets` once the JNI lane is gone, kept because every donor test calls it; MRL merge candidate). Dropped by declared rows: the native dispatch block (EDIT E2), the direct-buffer mirror of the flat patterns (EDIT E3, three fields, three accessors), the four native declarations (EDIT E4), `prefixCountNative`, `exactCountNative`, the `PreparedBytes` overloads of `firstOffsets`/`firstOffsetsJava` and their helpers, `verifyNative`, the two native result validators, the two dispatch thresholds, the `java.nio` buffer imports; the class comment names the pure-Java contract (EDIT E1) |
| `M3PrefixIndex` (66 lines) | `ChallengePrefixIndex` (64 lines) | all: the immutable facade over one prepared automaton (`prepare`, `size`, `prefixCount`, `exactCount`, `contains`, `startsWith`, package-private `preparedAutomaton`) |

Progress: the donor `IProgressMonitor` / `ProgressMonitors` map onto the landed `M3ProgressMonitor` adapter and the
package-private `M3ProgressChecks` (catalogue-kernels-20261107) by TYPE rows; `IProgressMonitor.noop()` becomes
`M3ProgressMonitor.none()` by MEMBER rows (qualified and unqualified).

Donor tests ported as JUnit: `M3MultiPatternSearchTest` (5 of 6 methods: the optional JNI parity method dropped) and
`M3PrefixIndexTest` (3 of 4: the native shadow method dropped). Their `new com.synexia.job.DefaultProgressMonitor()`
+ `setCanceled(true)` becomes the test-scope `M3CancelableProgressMonitor` by one MEMBER row. M3JDK-authored files:
`M3CancelableProgressMonitor`, `M3CorpusSearchReferenceTest` (7 tests: naive first-offset scans and prefix/exact counts
on random forests, facade = automaton, the catalog entry points, cancellation, and the two NEC pairs) and
`M3CorpusSearchDump`, the differential driver.

Owner edit (recipe [apply.py](apply.py), rows [EDITS.tsv](EDITS.tsv)): two `Shape` rows of `M3AlgorithmShapeCatalog`
(`aho-corasick-first-offsets` -> `M3MultiPatternSearch.firstOffsets`, `byte-prefix-count` -> `M3PrefixIndex.prefixCount`),
hash-pinned on the master pre-image.

## NEC findings (recorded; nothing ported for them)

1. `jdk.m3.collections.text.IntAhoCorasick` is the same automaton family over int symbols, streaming every match
   through a consumer: no byte alphabet, no prefix or exact multiplicity, no first-offset-per-pattern result. Both are
   kept; `M3CorpusSearchReferenceTest` asserts equal first offsets on distinct non-empty patterns.
2. `ChallengeRabinKarpSearch.indexOfJava` (over `StringsRabinKarp.rabinKarp`) and the landed
   `M3PrimitiveStringAlgorithms.rabinKarpIndexOf` share algorithm and contract (first hit, exact verification, empty
   pattern = 0, null = NPE naming text/pattern); the differential replays them on every seeded case through the
   MEMBER row, and the reference test checks the owner against `String.indexOf`. The donor JNI lane is not ported.
3. `ChallengePostingQueries` is a facade over the posting kernels already NEC against `jdk.m3.arrays.M3IntPostingIndex`
   (catalogue-kernels-20261107); same verdict.

## Gates

[GATES.tsv](GATES.tsv) records every gate with its command, state and evidence; [verify.sh](verify.sh) runs them in
order. Recipe law and lineage: relocate.py and apply.py are those of catalogue-kernels-20261107 but for this leaf's
EDITS table and docstrings; differential.sh imports the DRIVER-tagged DONOR_ONLY row on the donor side;
javap_parity.py is v2 (selectors by javap simple names; a file's DROP rows apply to its nested classes); mutants.py
carries this leaf's six mutants. Lessons: [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv). Donor-first decision:
[FOSS_REUSE_DECISION.tsv](FOSS_REUSE_DECISION.tsv).

Hard limits (recorded, as in the lineage): the relocated `M3MultiPatternSearch` is 419 lines with 19 loop statements
and a 145-line `prepare`; donor bodies are relocated byte for byte (recipe law), not hand-split; kept for a later MRL
pass.

## Next

`ChallengeSupersetKernels` (HELD): a member-by-member NEC table against `M3ArrayAlgorithms`, `M3StackAlgorithms`,
`M3HeapAlgorithms`, `M3StringTrie` and the rest of the landed families before any port, and an owner for its
`ProgressAlgorithm<I,O>` contract. The 304 HANDOFF_D7 rows go to the landing lane's tooling custody.
