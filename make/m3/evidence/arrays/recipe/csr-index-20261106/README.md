<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# csr-index-20261106: relocate the CSR index into `jdk.m3.arrays.M3CsrIndex` (recipe RELOCATE_CSR_INDEX v1)

First leaf of the collections/arrays convergence plan (`C:/m3-superset/conv/plans/collections-arrays.tsv`, row 3
`M3CsrIndex`, decision ABSORB, recommended first landing): it proves the relocation recipe and the `m3/arrays` module
wiring with no native risk before the JNI posting-index leaf.

## What it is

`M3CsrIndex` is an immutable compressed-sparse-row relation row -> contiguous references, held as exactly two `int[]`
lanes (`starts`, `references`). `fromOwners(rows, ownerByReference)` builds it directly from one owner row per
reference (count, prefix sum, stable scatter; no staging arrays), keeping each row's references in input order;
`builder(rows[, expectedEntries]).add(row, reference).build()` is the staged path, kept as the independent oracle.
Queries: `rowCount`, `size`, `payloadBytes`, `start`, `end`, `degree`, `referenceAt`, `referenceAtAbsolute`, `row`,
and defensive copies `starts()` / `references()`. Negative rows, out-of-range owners and overflowing counts are
refused (`IllegalArgumentException`, `IndexOutOfBoundsException`, `ArithmeticException`). `java.util.Arrays` and
`Objects` only; the class joins the existing module `jdk.m3.arrays` (package `jdk.m3.arrays`, already exported), so
`pom.xml` and `module-info.java` are unchanged.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `64652588f0d4c23b1f6e0411090b2a796325ae0e`; owner commit `8e2eb4d42bf` (2026-11-04, direct CSR from owner array), oracle commit `dde0ba6e4e5` (2026-11-04) |
| donor files | `synexia-mat-value/src/main/java/com/synexia/matvalue/MatCsrIndex.java` (blob `5fed861ccf`, 164 lines, the fromOwners-direct postimage of crate `m3-mat-csr-fromowners-direct-20261104`) and `synexia-mat-value/src/test/java/com/synexia/matvalue/MatCsrDirectContract.java` (blob `4c8e52d370`) |
| recipe | `relocate.py` v1: `python -I m3/arrays/recipe/csr-index-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--module m3/arrays] [--check]` |
| map | `RELOCATION_MAP.tsv`: 2 `FILE` rows, `PACKAGE com.synexia.matvalue -> jdk.m3.arrays`, `TYPE MatCsrIndex -> M3CsrIndex`, `TYPE MatCsrDirectContract -> M3CsrIndexContract`, no `EXTERNAL` row |
| provenance | `PROVENANCE.tsv`: donor commit, blob sha, donor sha256, target sha256, `body_match`, `body_match_renamed` per file |
| naming | `python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/arrays/`: 0 moves, 0 rewrites (before and after) |

The recipe rewrites only the license header (M3JDK style), the `package` line, mapped `import` lines and the two
mapped type names (whole-word tokens), and inserts one `//FLLDCIM: RELOCATE` anchor before the top-level type naming
the donor blob URL at the donor commit. Everything else is kept byte for byte: `body_match_renamed=TRUE` for both
files means the donor body with the two type names mapped equals the relocated body minus the anchor (the plain
`body_match` is `FALSE` because the names differ, and is recorded as such). `javap -public` of donor and relocated
class agree modulo names. The recipe is idempotent (second run `changed=0`) and refuses an unmapped package or import
instead of guessing the closure.

Derived from `m3/compiler/lifetime-slots/recipe/relocate.py` v1 (PR #2497): map and provenance moved into the
recipe directory because the class joins an existing module, and `body_match_renamed` added for non-identity TYPE rows.

## Build and gates (2026-11-06, see GATES.tsv)

| Step | Command | Result |
|---|---|---|
| 1 compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/arrays/pom.xml package` (module flags `-Xlint:all,-module -Werror`) | BUILD SUCCESS; 85 tests, 0 failures, 14 skipped (baseline 84 / 0 / 14; the skips are native-required tests without `libm3_arrays`) |
| 2 oracle in the test phase | `M3CsrIndexContractTest` wraps `M3CsrIndexContract.main`; direct: `java -cp target/classes;target/test-classes jdk.m3.arrays.M3CsrIndexContract` (`:` outside Windows) | `MAT_CSR_DIRECT_CHECKS=7329532`, `RESULT=PASS` (0.44 s) |
| 3 differential | `bash m3/arrays/recipe/csr-index-20261106/differential.sh <donor clone> <rev> <scratch>` after `package` | oracle outputs identical (sha256 `7a5615c7a2f1bf56...`); `M3CsrIndexDump` dumps identical over 1,722 inputs / 825,761 references, 11,796,262 bytes (sha256 `2581431270e4cab0...` on both sides) |
| 4 frozen CSR scaffolds | `git grep "scaffold: not yet received" -- M3FrozenLongMultimap.java M3FrozenLongTable.java` | not wired in this leaf (by design); see next leaf |
| recipe | `relocate.py --check` after apply and after `jdk_naming.py` | `files=2 changed=0 provenance=UNCHANGED` |

Lessons of the run are in `RECIPE_MEMORY.tsv`.

## Next leaf (not in this PR)

- `jdk.m3.collections.M3FrozenLongMultimap` (lines 74/79/84) and `M3FrozenLongTable` (lines 71/76/81/86) keep their
  `CSR ... (scaffold: not yet received)` lanes; activating them on `M3CsrIndex` is a behavioural promotion of
  `m3/collections` and needs its own recipe and oracle.
- `M3IntPostingIndex` + `m3_posting_merge.c` (plan row 1) consumes this owner for posting CSR validation; `M3BitMatrix`
  + CSR transpose (plan row 4) shares the CSR shape; `MatCsrGraph` (synexia-mat-collections) is the donor's own
  consumer.

## Files

| File | Purpose |
|---|---|
| `m3/arrays/src/main/java/jdk/m3/arrays/M3CsrIndex.java` | the relocated owner (donor body, names mapped) |
| `m3/arrays/src/test/java/jdk/m3/arrays/M3CsrIndexContract.java` | the relocated oracle main (7,329,532 assertions) |
| `m3/arrays/src/test/java/jdk/m3/arrays/M3CsrIndexContractTest.java` | JUnit 5 wrapper in the module's `M3*Test -> M3*SelfTest` template, so surefire runs the oracle |
| `m3/arrays/src/test/java/jdk/m3/arrays/M3CsrIndexDump.java` | reflective dump driver of the differential gate (not a test) |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins |
| `differential.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
