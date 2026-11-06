<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# utf16-atoms-20261106: bit-vector LCS length and whole-input glob over UTF-16 units

Two text atoms of the TEXT + HASH convergence plan (`C:/m3-superset/conv/plans/text-hash.tsv`, rows
`utf16-lcs-bitvector` and `utf16-wildcard-glob`) relocated from com.synexia into `m3/ports/precompute`, package
`jdk.m3.precompute`, beside `M3EditDistancePlan` (the same Myers / Allison-Dix / Hyyro bit-parallel family). Java 21,
no dependency outside `java.base`; the module's existing `-Xlint:all -Werror` flags apply.

## What landed

| Type | Scope | Contract |
|---|---|---|
| `M3LcsPlan.bitVectorLength(char[], char[])` | main | LCS **length** over equal UTF-16 code units (not code points), 64-bit recurrence `state = x & ~(x - ((state << 1) \| 1))` over a 65,536-lane mask table; both inputs at most 4,096 units, the shorter at most 63; `0` for an empty input; `NullPointerException` on null, `IllegalArgumentException` out of contract; inputs read-only |
| `M3Utf16Glob.match(char[] text, char[] pattern)` | main | whole-input match, `?` = one unit, `*` = zero or more, literals compared per code unit (surrogate halves included), no escapes, no `[..]`/`{..}` (not a `java.nio.file.PathMatcher` glob); greedy star backtracking, worst case O(n*m) time, O(1) auxiliary space; `MAX_TEXT` 1,048,576 and `MAX_PATTERN` 65,536 units, NPE/IAE refusals |
| `M3LcsPlanOracle` | test | the donor's independent rolling-row DP `referenceLength`, the seeded proof `main` (14 directed + 1,600 exhaustive + 46,000 seeded cases, 4 refusals, SHA-256 answer digest) and the optional JNI hook `--native <absolute library>` |
| `M3Utf16GlobOracle` | test | the donor's rolling-row boolean DP `reference`, the seeded proof `main` (49 directed + 41,261 exhaustive + 32,000 seeded cases, 4 refusals, input immutability, SHA-256 root) and the optional JNI hook |
| `M3LcsPlanTest`, `M3Utf16GlobTest` | test | the donor crates' JUnit 5 contract tests, retargeted at the M3 types |
| `M3LcsPlanProofTest`, `M3Utf16GlobProofTest`, `M3ProofCapture` | test | M3JDK-authored wrappers: run the proof mains in the test phase, echo the receipt line, assert it against the crate PROOF digests |
| `M3Utf16AtomsDump` | test | M3JDK-authored reflective dump driver of the differential gate (not a test) |

Neither atom has a JNI kernel here: the crates' C17 files (`utf16_bitvector_lcs_oracle.c` blob `35d95161b066`,
`utf16_wildcard_oracle.c` blob `9dbb0b3e294f`) stay in com.synexia until a Java consumer in M3JDK asks for one
(plan rows: "keep JNI out until a Java consumer exists"). The donor crates are DRAFT/HOLD in com.synexia (local
javac/JUnit/standalone proofs executed, no Maven/OpenRewrite replay); here they passed the module's Maven build.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `64652588f0d4c23b1f6e0411090b2a796325ae0e`; crates `synexia-openrewrite-recipes/recipe-crates/m3-utf16-lcs-bitvector-20261105` (created `811efb8e83d`, 2026-11-05) and `m3-utf16-wildcard-glob-20261105` (created `37055365e42`, 2026-11-05), single commit each |
| donor files | `Utf16BitVectorLcsOracle.java` (blob `079e66607954`), `Utf16BitVectorLcsOracleTest.java` (`2283262a7711`), `Utf16WildcardOracle.java` (`3727bd2168e5`), `Utf16WildcardOracleTest.java` (`e1fa77b626b5`); the hash-pinned-java snapshots under `synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-utf16-*-java-20261105/` carry the same blobs; no other copy exists in com.synexia (ls-tree over develop), so the crate copy is the newest and only production copy |
| recipe | `relocate.py` v1 (`RELOCATE_UTF16_ATOMS`): `python -I m3/ports/precompute/recipe/utf16-atoms-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--check]` |
| map | `RELOCATION_MAP.tsv`: 6 `FILE` rows (two donor files split in two targets each), `PACKAGE com.synexia.m3.proof -> jdk.m3.precompute`, 4 `TYPE` rows, 2 `QUALIFIED` rows, 32 `MEMBER` rows, 4 `IMPORT` rows, `EXTERNAL org.junit.jupiter.api` |
| provenance | `PROVENANCE.tsv`: donor commit, blob sha, donor sha256, target sha256, members, `body_match`, `type_renames` per target |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --only m3/ports/precompute/`: 0 moves, 0 rewrites, 0 refusals, fixed point |

The recipe rewrites only the license header (M3JDK style), the `package` line, the import block (mapped, filtered to
what a split half uses, extended by the `IMPORT` rows), type names (`QUALIFIED` then `TYPE` rows) and inserts one
`//FLLDCIM: RELOCATE` anchor before the top-level type naming the donor blob URL at the donor commit (and the
members of a split half). A donor type that carries the atom, its DP oracle and a proof `main` is **split** by the
`MEMBER` rows: every member's bytes are copied unchanged in donor order into its target, the private validator and
the private bounds the oracle half needs are copied into both halves, and the oracle half reaches the production
atom through `import static` lines, so no method body changes (`body_match=TRUE`, `type_renames=0` on all four
halves). The two donor JUnit tests are relocated whole; their `Type.member` qualifiers are retargeted
(`type_renames` 15 and 24, `body_match=FALSE` as the honest record). The recipe is idempotent (`--check`:
`changed=0`) and refuses an unmapped package, import or member instead of guessing.

## Build

| Step | Command |
|---|---|
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -q -B -ntp -f m3/ports/precompute/pom.xml package` (surefire runs the four relocated/wrapper test classes with the module's other tests) |
| proof mains directly | `java -cp target/classes;target/test-classes jdk.m3.precompute.M3LcsPlanOracle --java NO_LIB` and `...M3Utf16GlobOracle --java NO_LIB` (`:` instead of `;` outside Windows); they print `M3_UTF16_LCS_BITVECTOR_V1 cases=47614 answerSha256=b12c3039...67fc1 ...` and `M3_UTF16_GLOB_V1 cases=73310 sha256=5b3800a0...1763b ...` |
| differential | `bash recipe/utf16-atoms-20261106/differential.sh <donor clone> <donor rev> <scratch dir>` after `package`: compiles the two donor classes from the donor tree, replays 100,000 + 100,000 seeded inputs and the bound/refusal cases through both sides with `M3Utf16AtomsDump`, compares the dumps byte for byte |
| all gates | `MAVEN_REPO_LOCAL=<repo> bash recipe/utf16-atoms-20261106/verify.sh <donor clone> <donor rev> <scratch dir>` |

## Gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 1 `mvn package`, JDK 21.0.9, module flags `-Xlint:all -Werror` | BUILD SUCCESS, no lint tripped, pom unchanged |
| 2 oracles in the test phase | 84 tests, 0 failures, 2 pre-existing native skips; `M3_UTF16_LCS_BITVECTOR_V1 cases=47614 answerSha256=b12c30396266431206d782c55599803d5e05386d748536064302dbb746767fc1` and `M3_UTF16_GLOB_V1 cases=73310 sha256=5b3800a076a582f982dbe79f0146a5a4d1cae12f4e2f578e80b058120331763b`, both equal to the crates' PROOF_20261105.md digests |
| 3 differential donor vs relocated | identical dumps over 100,014 LCS + 100,018 glob cases (200,033 lines, 8,600,176 bytes), sha256 `56ced54347f6d388047f060cbd9aad48c0705cec2b45f1fefb232ac88805afe8` on both sides, inputs `4c930aff169370fb0a8fb3b20edc48c48066845c91fd0879557b38733b9705bc` |
| 4 `jdk_naming.py apply` | 0 moves, 0 rewrites, 0 refusals, fixed point |
| 5 scope | every change under `m3/ports/precompute/` |

Lessons of the run are in `RECIPE_MEMORY.tsv`.

## Files

| File | Purpose |
|---|---|
| `src/main/java/jdk/m3/precompute/M3LcsPlan.java`, `M3Utf16Glob.java` | the two relocated atoms (production halves) |
| `src/test/java/jdk/m3/precompute/M3LcsPlanOracle.java`, `M3Utf16GlobOracle.java` | the relocated oracle halves (DP oracle, proof main, JNI hook) |
| `src/test/java/jdk/m3/precompute/M3LcsPlanTest.java`, `M3Utf16GlobTest.java` | the relocated donor JUnit 5 tests |
| `src/test/java/jdk/m3/precompute/M3LcsPlanProofTest.java`, `M3Utf16GlobProofTest.java`, `M3ProofCapture.java` | proof wrappers (gate 2) |
| `src/test/java/jdk/m3/precompute/M3Utf16AtomsDump.java` | reflective dump driver of the differential gate (not a test) |
| `recipe/utf16-atoms-20261106/relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins |
| `recipe/utf16-atoms-20261106/differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
