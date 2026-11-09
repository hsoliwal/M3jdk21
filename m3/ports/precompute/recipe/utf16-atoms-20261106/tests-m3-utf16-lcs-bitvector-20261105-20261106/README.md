<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# tests-m3-utf16-lcs-bitvector-20261105-20261106: crate test parity and mutant refusal for `M3LcsPlan`

Tests-only leaf L039 of the M3JDK convergence census (`S:/m3-scratch/eb-conv/census/LEAVES_PROPOSED.tsv`, pins
com.synexia develop `ca1aafc4d94f`, M3jdk21 master `80743e25b9e6`). The census proposed porting the proof class of
crate `m3-utf16-lcs-bitvector-20261105` (`Utf16BitVectorLcsOracle`, status `NO_OWNER`, proposed owner
`jdk.internal.m3.proof`) as a new test. Re-run on master `cf0429ac1d49` the census gives the same row; the `NO_OWNER`
is a census false negative: the census looks the type up by naming rule (`Utf16BitVectorLcsOracle` ->
`M3Utf16BitVectorLcsOracle`), while the owner recipe `../relocate.py` v1 (PR #2504) already relocated the class,
split by its `MEMBER` rows into the production atom `M3LcsPlan` and the test-scope oracle half `M3LcsPlanOracle`,
and relocated the crate's JUnit test whole as `M3LcsPlanTest`.

## Verdict

| Side | Crate file at the census pin | Owner on master | Verdict |
|---|---|---|---|
| product | `Utf16BitVectorLcsOracle.java` blob `079e66607954` (members `MAX_TEXT`, `MAX_SHORT`, `ALPHABET`, `<init>`, `validate`, `bitVectorLength`) | `src/main/java/jdk/m3/precompute/M3LcsPlan.java` sha256 `d65a753f6245...` | `APPLIED` (#2504, `body_match=TRUE`, `type_renames=0`) |
| proof | the same blob (members `referenceLength`, `verify`, `reject`, `decode`, `main`, `nativeLength`, ...) | `src/test/java/jdk/m3/precompute/M3LcsPlanOracle.java` sha256 `4dfd6bf1e797...`, run by `M3LcsPlanProofTest` (crate digest `b12c3039...67fc1`, 47,614 cases) | `ALREADY_APPLIED` (#2504) |
| test | `Utf16BitVectorLcsOracleTest.java` blob `2283262a7711` | `src/test/java/jdk/m3/precompute/M3LcsPlanTest.java` sha256 `765a9102fa76...` (4 tests) | `ALREADY_APPLIED` (#2504, `type_renames=15`) |
| recipe test | `M3Utf16LcsRecipeTest.java.txt` (OpenRewrite seal test of the com.synexia hash-pinned snapshot recipes) | none | `NOT_PORTED_BY_DESIGN`: tests com.synexia recipe infrastructure; its M3 counterpart is `../relocate.py --check` |
| JNI | `utf16_bitvector_lcs_oracle.c` | none | `NOT_PORTED_BY_DESIGN` (#2504: no JNI until a Java consumer in M3JDK asks for one) |

The hash-pinned-java twins carry the same blobs, and the crate files did not change between the owner's donor commit
`64652588f0d4` and the census pin, so `../relocate.py --check` at `64652588f0d4` (`files=6 changed=0
provenance=UNCHANGED`) plus the rows of `CRATE_PARITY.tsv` prove the crate's proof and test are on master byte for
byte with names mapped. No test file is added.

## What this leaf adds

`mutants.py` (MUTANT_REFUSAL v1) proves the relocated tests refuse the defects the crate README names: it copies the
module (pom.xml, top-level files, src/) to a scratch directory, replaces exactly one owner line per `MUTANTS.tsv`
row, runs `mvn -o test -Dtest=M3LcsPlanTest,M3LcsPlanProofTest` on the copy and checks the expectation. The module is
never written (owner sha256 before = after).

| Mutant | Owner line (M3LcsPlan.java) | Guard (crate README) | M3LcsPlanProofTest | M3LcsPlanTest |
|---|---|---|---|---|
| `lcs-m1-recurrence` | `shifted = state << 1` (carry-in bit dropped) | the 64-bit recurrence yields the LCS length | `LCS_JAVA_DIFF 3 != 0` | 3 of 4 fail, e.g. `representativePlatformContracts: expected: <3> but was: <0>` |
| `lcs-m2-short-bound` | `MAX_SHORT = 64` | shorter length may not exceed 63 (IAE) | `MISSING_JAVA_REJECTION` | `refusesNullAndOverlength: Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.` |
| `lcs-m3-code-unit` | mask lane `shorter[i] & 0x7FFF` | equal UTF-16 code units only | `LCS_JAVA_DIFF 2 != 1` | `utf16LiteralAndZeroLengthContracts: expected: <1> but was: <0>` (+ exhaustive) |
| `lcs-m4-read-only` | `shorter[0] = longer[0]` before the return | input arrays are read-only | receipt digest differs from `b12c3039...` | `exhaustiveSmallAlphabetAndInputImmutability: array contents differ at index [0]` |

All four are KILLED by both relocated test classes. Control (no mutation): 5 tests, 0 failures.

## Run

```sh
JAVA_HOME=<JDK 21> python -I m3/ports/precompute/recipe/utf16-atoms-20261106/tests-m3-utf16-lcs-bitvector-20261105-20261106/mutants.py \
    --scratch <dir> --donor-git <com.synexia clone> --maven-repo <maven repo>
```

Prints `PARITY MATCH` x6, `CONTROL PASS`, `MUTANT PASS` x4 and `MUTANT_REFUSAL PASS`; needs a prior online build of
`m3/ports/precompute` in that Maven repository (the copies build offline). Gates: `GATES.tsv`; lessons:
`RECIPE_MEMORY.tsv`. `mutants.py` is the same v1 tool as `m3/arrays/recipe/csr-index-20261106/tests-*/mutants.py`
(PR #2548); only the tables next to it differ.

## Files

| File | Purpose |
|---|---|
| `CRATE_PARITY.tsv` | crate Java files at the census pin (blob) -> module files (sha256), verdict per row |
| `MUTANTS.tsv` | the mutants: owner line, replacement, expectation, failure text, guarded property |
| `mutants.py` | the gate (parity + control + mutants) |
| `GATES.tsv`, `RECIPE_MEMORY.tsv` | gate record and lessons |
