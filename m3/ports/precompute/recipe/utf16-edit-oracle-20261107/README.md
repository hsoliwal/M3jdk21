<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe utf16-edit-oracle-20261107: bit-parallel edit oracle bound to `M3EditDistancePlan`

Census pass-1 leaves L040 and L041 of the M3JDK convergence programme (tests-only rows, status NO_OWNER). Donor:
hsoliwal/com.synexia develop `b42ec59741ed` (2026-11-07).

| Leaf | Crate | Verdict |
|---|---|---|
| L040 | `m3-utf16-bitparallel-edit-20261105` (`Utf16BitParallelEditOracle` blob `380723653791`, its JUnit test blob `32c361471457`) | relocated into TEST scope of this module, bound to the owner (below) |
| L041 | `m3-utf16-wildcard-glob-20261105` (`Utf16WildcardOracle` blob `3727bd2168e5`, its JUnit test blob `e1fa77b626b5`) | CLOSED before this recipe: the same blobs were split by `recipe/utf16-atoms-20261106` into `M3Utf16Glob` + `M3Utf16GlobOracle` and relocated as `M3Utf16GlobTest`; `M3Utf16GlobProofTest` asserts the 73,310-case receipt in the test phase. The census row is stale; CLOSED rows of `RELOCATION_MAP.tsv` record it, nothing is copied |

## Owner and binding (L040)

`git grep` of master: the bit-parallel Levenshtein lane is owned by `jdk.m3.precompute.M3EditDistancePlan`
(`compile(pattern)` builds the single-word Myers verifier for 1..63 units, `distance(value)` runs it; longer patterns
run two-row DP). No production half is relocated. The crate type lands whole in test scope:

| Target | Scope | Donor |
|---|---|---|
| `M3EditDistancePlanOracle` | test | the crate type split by MEMBER rows: bounds, `admit`, independent DP `referenceDistance`, proof state, `verify`, `rejects`, `decode`, the proof `main`, the never-loaded JNI hook; `javaDistance` BOUND (`bindings/M3EditDistancePlanOracle.javaDistance.java`), `ALPHABET` dropped with the replaced body |
| `M3EditDistancePlanBitParallelTest` | test | the crate JUnit 5 contract test, type renames only (`M3EditDistancePlanTest` and `M3EditDistancePlanBoundaryTest` already exist) |
| `M3EditDistancePlanProofTest` | test | M3JDK-authored gate-2 wrapper (module style `M3Utf16GlobProofTest`): runs the proof main under `M3ProofCapture` and asserts the receipt line |

The declared edit: the crate's candidate Myers body (single 64-bit vector over a 65,536-lane table) is replaced by
`M3EditDistancePlan.compile(shorter).distance(longer)` (Myers lane) and `compile(longer).distance(shorter)` (DP lane)
under the donor `admit`; the two lanes must agree and `verify` compares the result with `referenceDistance` on every
case: 11 directed, 14,641 exhaustive tiny pairs over NUL / `a` / U+D800, 30,000 seeded pairs (lengths 0..95 x 0..63,
nine UTF-16 units incl. lone surrogates, NUL, U+FFFF, prefix copies) plus 2,308 swapped orientations = 46,960
reference differentials, 5 refusals, SHA-256 digest `170f1ab0ca28...` (the digest folds the DP reference distances,
so it equals the donor's own standalone Java run and is independent of the owner).

The owner's refusals are not the crate's (`Objects.requireNonNull`, 1,048,576-unit budgets): a 64x64 or a 4,097-unit
operand is legal for `M3EditDistancePlan`, so the crate's `IllegalArgumentException` refusals remain the proof
domain's (`admit`), recorded as a NEC row, not asserted against the owner. No production file changes.

## Run

```sh
python -I relocate.py --donor-git <com.synexia clone> --donor-rev b42ec59741ed18117c18c72f84cc165ea4900b6e [--check]
JAVA_HOME=<JDK 21> MAVEN_SETTINGS=<settings.xml> MVN=mvn bash verify.sh <com.synexia clone> b42ec59741ed18117c18c72f84cc165ea4900b6e <scratch> [<baseline module build>]
```

`verify.sh` runs every gate of `GATES.tsv`: recipe fixed point, `mvn package` (`-Xlint:all -Werror`, surefire runs
the contract test and the proof wrapper; the log is the authority, not the exit code), the naming plan and, with a
baseline build of the untouched module, `class_identity.sh` (every class file of master byte-identical).

## Held

The C17 JNI kernel `utf16_bitparallel_edit_oracle.c` stays in com.synexia (OMIT row); `nativeDistance` is declared
and never loaded in the test phase. Lessons: `RECIPE_MEMORY.tsv`.
