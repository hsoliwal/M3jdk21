<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# hamming-topk-20261106: relocate the Hamming top-k selector into `jdk.m3.indexdb.M3HammingTopK` (recipe RELOCATE_HAMMING_TOPK v2)

Second leaf of the lexicon/index convergence plan (`C:/m3-superset/conv/plans/lexicon-index.tsv`, row 2
`hamming-topk`, decision ABSORB): the nearest-neighbour top-k over 64-bit SimHash fingerprints joins `m3/indexdb`
beside the SimHash owner `M3IndexDbChunkSimHash`, which already had `hammingDistance` and `fromFeatures`. The donor's
`SimHash64` is not ported a second time: its distance primitive is mapped onto a static overload of the existing owner,
and its fingerprint is only probed against the owner's (see NEC below).

## What it is

`M3HammingTopK` selects the `k` candidates nearest to a query fingerprint by Hamming distance, ordered
(distance ascending, candidate index ascending), with a bounded worst-first heap: `nearest(long query,
long[] candidates, int k)` computes each distance exactly once (`M3IndexDbChunkSimHash.hammingDistance(long, long)`
= `Long.bitCount(a ^ b)`), `fromDistances(int[] distances, long[] candidates, int k)` consumes prepared distances
(each must be in `0..64`), both return `Match[]` (`record Match(int index, int distance, long fingerprint)`), O(n log k
+ k log k) time and O(k) scratch; `k=0` returns an empty array before any distance is validated (the historical
contract the donor's oracle pins). Null arrays, negative `k`, a length mismatch and an out-of-range distance are
refused (`NullPointerException`, `IllegalArgumentException`). `java.util.Objects` only; the class joins the existing
module `m3/indexdb` (artifact `m3indexdb`, package `jdk.m3.indexdb`, an automatic module: no `module-info.java`), so
`pom.xml` is unchanged. The one change to an existing file is additive: `M3IndexDbChunkSimHash` gains
`public static int hammingDistance(long left, long right)` (9 lines, 0 deletions) beside its instance
`hammingDistance(M3IndexDbChunkSimHash)`.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `64652588f0d4c23b1f6e0411090b2a796325ae0e`; owner commit `d9c135d985f` (2026-11-04, O(k log k) heap extraction, crate `m3-hamming-topk-heapsort-20261104`), oracle commits `6a5b6d959d6` / `5d727831fc1` (2026-11-04), `SimHash64` last changed `4a2e6452a7a` (2026-10-31); original optimisation owner com.synexia #12072 |
| donor files | `synexia-algo/src/main/java/com/synexia/algorithms/agent/TopKByHamming.java` (blob `21938d5fe371`, sha256 `75124217b595eaff...` = the crate's sealed postimage), `.../src/test/.../TopKByHammingHeapSortProof.java` (blob `32aa8b666031`), `.../TopKByHammingHeapSortProofTest.java` (blob `3e9f4ae5fcb4`); donor-only dependency `SimHash64.java` (blob `5f961f3179c5`, DONOR_DEP row) |
| recipe | `relocate.py` v2: `python -I m3/indexdb/recipe/hamming-topk-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--module m3/indexdb] [--check]` |
| map | `RELOCATION_MAP.tsv`: 3 `FILE` rows, 1 `MUTANT` row, `PACKAGE com.synexia.algorithms.agent -> jdk.m3.indexdb`, `TYPE TopKByHamming -> M3HammingTopK` (scoped to the mutant file: `-> M3HammingTopKTieOrderMutant`), `TYPE TopKByHammingHeapSortProof -> M3HammingTopKContract`, `TYPE ...ProofTest -> M3HammingTopKContractTest`, `MEMBER SimHash64.distance -> M3IndexDbChunkSimHash.hammingDistance`, 1 `SUBST` row (the reversed tie comparator of the mutant), `EXTERNAL org.junit.jupiter.api`, `DONOR_DEP SimHash64.java` |
| provenance | `PROVENANCE.tsv`: donor commit, blob, donor sha256, target sha256, `body_match`, `body_match_renamed`, role, substitution count per file (4 relocated files + the DONOR_DEP pin) |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/indexdb/`: 0 moves, 0 rewrites, 0 refusals (before, after staging, FIXED_POINT) |

The recipe rewrites only the license header (M3JDK style), the `package` line, mapped `import` lines, the MEMBER token,
the mapped type names (whole-word tokens; the TYPE row scoped to the mutant file gives the mutant its own name), the
SUBST line of the mutant (exactly once), and inserts one `//FLLDCIM: RELOCATE` anchor before the top-level type naming
the donor blob URL at the donor commit. Everything else is kept byte for byte: `body_match_renamed=TRUE` for all four
files (the donor body with the MEMBER, TYPE and SUBST rows applied equals the relocated body minus the anchor; the
plain `body_match` is `FALSE` because names differ and is recorded as such). The recipe is idempotent (second run
`files=5 changed=0 provenance=UNCHANGED`) and refuses an unmapped package, import, member owner or substitution count
instead of guessing. Derived from `m3/arrays/recipe/csr-index-20261106/relocate.py` v1 (PR #2499) with MEMBER,
scoped TYPE, MUTANT, SUBST and DONOR_DEP rows added.

## Build and gates (2026-11-06, see GATES.tsv)

| Step | Command | Result |
|---|---|---|
| 1 compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/indexdb/pom.xml package` (module flags `-Xlint:all -Werror`, main and test) | BUILD SUCCESS; 73 tests, 0 failures, 0 errors, 0 skipped (baseline on the untouched module: 70 / 0 / 0 / 0) |
| 2 oracle in the test phase | `M3HammingTopKContractTest` (the donor's JUnit wrapper, relocated) runs `M3HammingTopKContract.main`; direct: `java -cp target/classes;target/test-classes jdk.m3.indexdb.M3HammingTopKContract` | `M3_TOPK_HEAPSORT_DIFFERENTIAL_PASS cases=35032 comparisons=1439283 seed=0x4d33544845415053` (0.59 s) |
| 2 mutant kept out of the green run | `M3HammingTopKTieOrderMutantTest` (negative-assertion wrapper): owner `[0,1]` vs mutant `[1,0]` on the oracle's first all-tie input (trial 0, n=2048, k=2); agreement without ties | 2 tests green; `differential.sh` runs the oracle renamed onto the mutant (scratch) and gets `topk oracle failure k=2 n=2048` |
| 3 differential | `bash m3/indexdb/recipe/hamming-topk-20261106/differential.sh <donor clone> <rev> <scratch>` after `package` | donor (`TopKByHamming` + `SimHash64` + oracle, `javac --release 21 -Xlint:all -Werror`) vs relocated: oracle outputs identical (sha256 `402854ba2e6a74ff...`); `M3HammingTopKDump` dumps identical over the oracle inputs and 100,000 seeded fingerprint sets: 235,039 calls, 11,543,068 result records, 22,142,971 bytes (sha256 `d71056b3f3a9dca9...` on both sides); mutant dump differs first at `T0 k=2 n=2048`; javap identical; fingerprint NEC lane 0 unexplained |
| 4 nothing outside the module | `git status` / `git diff --cached --stat` | every path under `m3/indexdb/` |
| recipe | `relocate.py --check` after apply and after `jdk_naming.py` | `files=5 changed=0 provenance=UNCHANGED` |

`verify.sh <donor clone> <rev> <scratch> [maven repo]` replays all of it. Lessons of the run are in `RECIPE_MEMORY.tsv`.

## Fingerprint reuse and the NEC finding

The selector consumes `long[]` fingerprints and only reuses the owner's distance; the byte-identical dump proves that
`M3IndexDbChunkSimHash.hammingDistance(long, long)` is the donor's `SimHash64.distance(long, long)`. The donor's
`SimHash64.fingerprint(long[])` overlaps `M3IndexDbChunkSimHash.fromFeatures(domain, hashes, 0, weights, 0, n)
.simHash64()` with unit weights, and the plan row asked for the bit-vote rules to be diffed first (NEC): over 20,000
seeded windows of 0..64 features the two fingerprints are equal on every window without a tied bit and on the empty
window (10,220) and differ exactly on the tie mask elsewhere (9,780; the donor votes 0 on a tie, the M3 owner votes
1), with 0 unexplained windows. This is recorded as a difference, not an equivalence: a future port of a
`SimHash64.fingerprint` consumer must choose a tie rule explicitly.

## Tie rule pinned (contract test, 2026-11-06)

The lane owner's decision after the NEC finding: the rule stays undecided (NEC), `M3IndexDbChunkSimHash` is the live
fingerprint owner, and its rule is pinned by a contract test before any fingerprint consumer is ported, so a future
change of it is a conscious one. `m3/indexdb/src/test/java/jdk/m3/indexdb/M3IndexDbChunkSimHashTieRuleContractTest.java`
(JUnit 5, 3 tests, `//FLLDCIM: CONTRACT` anchor, test tree only, no production change) pins:

- the tie: 2,000 seeded windows `W` (1..32 features, weights 1..64) mirrored through a tie mask `M` (`W ++ (W ^ M)`,
  same weights: every bit of `M` receives `+w - w = 0` per pair, every other bit twice the vote of `W`); the owner's
  `voteCopy()` proves the zero vote sums first, then `simHash64(W ++ (W ^ M)) == simHash64(W) | M` (1 on every tied
  position, the majority bit elsewhere, the same through `merge`): 45,112 constructed tied positions all 1, 82,631
  majority positions, 257 accidental ties of `W` (also 1); the pair `{f, ~f}` with equal weights (1 ..
  `Integer.MAX_VALUE`) yields `-1L`; `featureCount == 0` (`empty`, an empty window, `repeat(0)`) stays the explicit `0L`;
- the untied behaviour: the first 10,000 windows of the NEC lane (seed `0x4d3346494e475052`, 0..64 unit-weight
  features, the same draws as `M3HammingTopKDump --fingerprint`) against an independent per-bit vote count written in
  the test: `untied=4924 tied=4930 empty=146 tied_bits=52984 agree_untied=4924 agree_ties_to_one=10000
  agree_ties_to_zero_untied_or_empty=5070 differ_exactly_on_tiemask=4930 unexplained=0`, identical to the same prefix
  (`F0..F9999`) of the recorded `donor-fingerprints.txt` / `relocated-fingerprints.txt` dumps;
- the NEC statement in its Javadoc: com.synexia `SimHash64.fingerprint` resolves a tie to 0, the difference is recorded
  as NEC in the convergence ledger, and a consumer that expects ties -> 0 goes through a recipe-level adapter with its
  own differential, never a second fingerprint owner.

Gates: `mvn -f m3/indexdb/pom.xml package` on JDK 21 (`-Xlint:all -Werror`) BUILD SUCCESS, 76 tests (73 on master + 3),
0 failures, 0 errors; `jdk_naming.py plan --only m3/indexdb/` 0 moves, 0 rewrites, 0 refusals.

## Next leaf (not in this PR)

- crate `m3-repair-band-postings-20261031` (SimHash 8-band postings, radius 0..7) builds on this selector (plan row).
- `jdk.m3.indexdb` has no `module-info.java`; if the module ever gets one, `jdk.m3.indexdb` must be exported for
  `M3HammingTopK` as for the rest of the package.
- The module's CI job `full-indexdb-coverage` (`mvn clean verify` against Central) cannot resolve
  `junit-jupiter:5.10.2-m3-source-1` without the M3 source build of the vendor tree; pre-existing on master.

## Files

| File | Purpose |
|---|---|
| `m3/indexdb/src/main/java/jdk/m3/indexdb/M3HammingTopK.java` | the relocated owner (donor body, names and the MEMBER token mapped) |
| `m3/indexdb/src/main/java/jdk/m3/indexdb/M3IndexDbChunkSimHash.java` | existing SimHash owner: additive `static int hammingDistance(long, long)` |
| `m3/indexdb/src/test/java/jdk/m3/indexdb/M3HammingTopKContract.java` | the relocated oracle main (35,032 calls, 1,439,283 records) |
| `m3/indexdb/src/test/java/jdk/m3/indexdb/M3HammingTopKContractTest.java` | the relocated JUnit 5 wrapper, so surefire runs the oracle |
| `m3/indexdb/src/test/java/jdk/m3/indexdb/M3HammingTopKTieOrderMutant.java` | negative control generated by the MUTANT + SUBST rows (test tree only) |
| `m3/indexdb/src/test/java/jdk/m3/indexdb/M3HammingTopKTieOrderMutantTest.java` | negative-assertion wrapper that keeps the mutant out of the green run |
| `m3/indexdb/src/test/java/jdk/m3/indexdb/M3HammingTopKDump.java` | reflective dump driver of the differential gate and the fingerprint NEC lane (not a test) |
| `m3/indexdb/src/test/java/jdk/m3/indexdb/M3IndexDbChunkSimHashTieRuleContractTest.java` | contract pin of the owner's tie rule (vote >= 0 -> bit 1) and its NEC statement; 3 JUnit 5 tests |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins |
| `differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates, their replay and their record |
