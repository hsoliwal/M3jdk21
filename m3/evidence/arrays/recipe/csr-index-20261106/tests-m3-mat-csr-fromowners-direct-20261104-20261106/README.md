<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# tests-m3-mat-csr-fromowners-direct-20261104-20261106: crate test parity and mutant refusal for `M3CsrIndex`

Tests-only leaf L020 of the M3JDK convergence census (`S:/m3-scratch/eb-conv/census/LEAVES_PROPOSED.tsv`, pins
com.synexia develop `ca1aafc4d94f`, M3jdk21 master `80743e25b9e6`). The census proposed porting the contract of crate
`m3-mat-csr-fromowners-direct-20261104` (row `01-MatCsrDirectContract.java.txt`, status `NO_OWNER`, closest names
`M3DirectoryTable`, `M3DirectArrayView`) as a new test. Re-run on master `cf0429ac1d49` the census gives the same rows;
the `NO_OWNER` is a census false negative: the census looks the type up by naming rule (`MatCsrDirectContract` ->
`M3CsrDirectContract`), while the owner recipe `../relocate.py` v1 (PR #2499) already relocated the file under the
`TYPE` row `MatCsrDirectContract -> M3CsrIndexContract`.

## Verdict

| Side | Crate file at the census pin | Owner on master | Verdict |
|---|---|---|---|
| product | `MatCsrIndex.java` blob `5fed861ccfb8` (and its hash-pinned twin `00-MatCsrIndex.java.txt`, same blob) | `src/main/java/jdk/m3/arrays/M3CsrIndex.java` sha256 `339e220fc099...` | `APPLIED_SUPERSET` (census rerun), relocated by #2499 |
| test | `MatCsrDirectContract.java` blob `4c8e52d37040` (twin `01-MatCsrDirectContract.java.txt`, same blob) | `src/test/java/jdk/m3/arrays/M3CsrIndexContract.java` sha256 `2e0cdf57803c...`, run in the test phase by `M3CsrIndexContractTest` | `ALREADY_APPLIED` (#2499, `body_match_renamed=TRUE`) |
| recipe test | `M3MatCsrDirectRecipeTest.java` (OpenRewrite seal test of the com.synexia hash-pinned snapshot recipe) | none | `NOT_PORTED_BY_DESIGN`: tests com.synexia recipe infrastructure, not the CSR owner; its M3 counterpart is `../relocate.py --check` |

The crate files did not change between the owner's donor commit `64652588f0d4` and the census pin (same blobs), so
`../relocate.py --check` at `64652588f0d4` (`files=2 changed=0 provenance=UNCHANGED`) plus the blob and sha256 rows of
`CRATE_PARITY.tsv` prove the crate's tests are on master byte for byte with names mapped. No test file is added.

## What this leaf adds

What the owner leaf did not record is whether the relocated contract actually refuses the defects the crate README
names. `mutants.py` (MUTANT_REFUSAL v1) proves it: it copies the module (pom.xml, top-level files, src/) to a scratch
directory, replaces exactly one owner line per `MUTANTS.tsv` row, runs `mvn -o test -Dtest=M3CsrIndexContractTest`
on the copy and checks the expectation. The module is never written (owner sha256 before = after).

| Mutant | Owner line (M3CsrIndex.java) | Guard (crate README / contract) | Result |
|---|---|---|---|
| `csr-m1-null-order` | `Objects.requireNonNull(ownerByReference, ...)` dropped | exception categories/order unchanged | KILLED: `null outranks invalid rows: wrong exception java.lang.IllegalArgumentException: negative CSR capacity` |
| `csr-m2-stable-scatter` | `fromOwners` scatter loop reversed | original reference order inside a row | KILLED: `legacy builder reference order` |
| `csr-m3-detached-starts` | `starts()` returns the lane, not a copy | all retained CSR bytes unchanged | KILLED: `starts defensive copy` |
| `csr-s1-payload-bytes` | `payloadBytes()` drops the starts lane | not crate-guarded (documented gap) | SURVIVED as expected: the contract compares `payloadBytes()` only with the staged Builder, i.e. the same method |

Control (no mutation): `M3CsrIndexContractTest` 1 test, 0 failures (`MAT_CSR_DIRECT_CHECKS=7329532`, `RESULT=PASS`).

## Run

```sh
JAVA_HOME=<JDK 21> python -I m3/arrays/recipe/csr-index-20261106/tests-m3-mat-csr-fromowners-direct-20261104-20261106/mutants.py \
    --scratch <dir> --donor-git <com.synexia clone> --maven-repo <maven repo>
```

Prints `PARITY MATCH` x4, `CONTROL PASS`, `MUTANT PASS` x4 and `MUTANT_REFUSAL PASS`; needs a prior online build of
`m3/arrays` in that Maven repository (the copies build offline). Gates: `GATES.tsv`; lessons: `RECIPE_MEMORY.tsv`.

## Files

| File | Purpose |
|---|---|
| `CRATE_PARITY.tsv` | crate Java files at the census pin (blob) -> module files (sha256), verdict per row |
| `MUTANTS.tsv` | the mutants: owner line, replacement, expectation, failure text, guarded property |
| `mutants.py` | the gate (parity + control + mutants) |
| `GATES.tsv`, `RECIPE_MEMORY.tsv` | gate record and lessons |
