<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# character-flags-20261106: relocate `IndexCharacterFlags.combine` into `jdk.internal.m3.indexstring.M3IndexCharacterFlags` (recipe RELOCATE_INDEX_CHARACTER_FLAGS v1)

Leaf of the text/hash convergence plan (`C:/m3-superset/conv/plans/text-hash.tsv`, row 5 `index-character-flags-combine`,
decision ABSORB): the character-flags owner of `synexia-indexstring` with the recovered package-private `combine(int,int)`
joins the existing module `m3/ports/indexstring` (package `jdk.internal.m3.indexstring`, `com.m3.compat:indexstring-retained-view`),
which carried `FrozenChars`/`MIndexJoinedChars` but no flags class.

## What it is

`M3IndexCharacterFlags` names ten primitive UTF-16 properties as bits (`ASCII`, `LATIN1`, `HAS_ASCII_UPPER`, `HAS_ASCII_LOWER`,
`HAS_SURROGATE`, `BLANK`, `EMPTY`, `HAS_ASCII_DIGIT`, `HAS_ASCII_WORD`, `HAS_ASCII_SPACE`; `REGEX_PRESENCE`, `ALL`). `of(CharSequence)`
scans a text once; `labels(int)` is the debugger formatter. The recovered atom is `combine(left, right)`: exact flags of two
adjacent immutable UTF-16 ranges without rescanning a character. `ASCII`/`LATIN1`/`BLANK`/`EMPTY` hold only if both ranges
have them (`ALL_OF`), presence bits hold if either range has them (`ANY_OF`); bits outside `ALL` are refused with
`IllegalArgumentException` before composition. `combine` stays package-private as in the owner. `java.util.Objects` and
`StringJoiner` only; `pom.xml` gains one exec execution and nothing else.

`java.lang.M3StringFacts` `REGEX_*` bits are a different vocabulary (regex classes); the bit layouts are not merged.

## Owner check (before adding a class)

Every `*Flags.java` on master was listed. `jdk.m3.file.M3CharacterFlags` (`m3/ports/file`, package `jdk.m3.file`) is the same
donor at its pre-combine blob: its body equals the crate's before-copy `3219d69fef57` modulo header, package and type name
(diff empty), it lacks `combine`/`ALL_OF`/`ANY_OF`, and its `of`/`labels` dump is byte-identical to the donor's and the
relocated class's (gate 3b). It lives in another module and this leaf changes `m3/ports/indexstring/**` only (gate 4), so the
plan row's new owner `M3IndexCharacterFlags` is added here and the sibling is recorded, not edited. Not ALREADY: the atom
`combine` exists nowhere on master.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `ac5feec00a6d6206194b3ba954c6f738aae07126`; owner restore commit `7a2429e2cc9` (2026-11-04, "restore(indexstring): historical exact segment flag composition", PR #7532 lineage); crate pin `92303e23339` (2026-11-04, `synexia-openrewrite-recipes/crates/m3-character-flags-combine-recovery-20261104`) |
| donor files | `synexia-indexstring/src/main/java/com/synexia/indexstring/IndexCharacterFlags.java` (blob `f6abb8a6c023`, 87 lines, sha256 `32d44ad5...` = the crate README's candidate digest) and `synexia-openrewrite-recipes/crates/m3-character-flags-combine-recovery-20261104/proof/FlagCombineSmoke.java` (blob `05e59df0ea7d`, sha256 `fbe67348...` = the crate EXECUTION.md oracle digest) |
| lineage | develop main-path blob == hash-pinned-java copy == held variant (`make/m3/qualification/source-only-category-utf16-kmp-16244-20261105`); the crate before-copy lacks `combine`; the `.superset/conflicts/20261102-...` copy is an older javadoc wording; no `retained-variants/pr<N>/` copy; the newest complete lineage (develop main path) is ported |
| not relocated | `synexia-indexstring/src/test/java/com/synexia/indexstring/IndexCharacterFlagsCombineRecoveryTest.java` (blob `5d5ebfbfa0fd`, JUnit 5): the module has no JUnit dependency and runs its proofs as exec mains; the smoke oracle carries the same figures, the package-private check is in the dump's `MODIFIERS` line |
| recipe | `relocate.py` v1: `python -I m3/ports/indexstring/recipe/character-flags-20261106/relocate.py --donor-git <clone> --donor-rev ac5feec00a6d6206194b3ba954c6f738aae07126 [--module m3/ports/indexstring] [--check]` |
| map | `RELOCATION_MAP.tsv`: 2 `FILE` rows, `PACKAGE com.synexia.indexstring -> jdk.internal.m3.indexstring`, `TYPE IndexCharacterFlags -> M3IndexCharacterFlags`, `TYPE FlagCombineSmoke -> M3IndexCharacterFlagsCombineSmoke`, no `EXTERNAL` row |
| provenance | `PROVENANCE.tsv`: donor commit, blob sha, donor sha256, target sha256, `body_match`, `body_match_renamed` per file |
| naming | `python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --only m3/ports/indexstring/`: 0 moves, 0 rewrites, 0 refusals |

The recipe rewrites only the license header (M3JDK style), the `package` line, mapped `import` lines and the two mapped type
names (whole-word tokens, string literals included: the smoke's PASS line names the relocated file), and inserts one
`//FLLDCIM: RELOCATE` anchor before the top-level type naming the donor blob URL at the donor commit. Everything else is kept
byte for byte: `body_match_renamed=TRUE` for both files (8 diff lines on the owner, 31 on the smoke, every one a header,
package, anchor or type-name line). `javap -public` (17 lines) and `javap -p` (24 lines) of donor and relocated class agree
modulo names. The recipe is idempotent (second run `changed=0`) and refuses an unmapped package or import. Derived byte for
byte from `m3/arrays/recipe/csr-index-20261106/relocate.py` v1 (PR #2499); only path, module default and docstring differ.

## Build and gates (2026-11-06, see GATES.tsv)

| Step | Command | Result |
|---|---|---|
| 1 compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/ports/indexstring/pom.xml package` (`-Xlint:all -Werror`) | BUILD SUCCESS, no warning; `EXISTING_SURFACE_PASS checks=502762` and `RETAINED_VIEW_PASS checks=1080071` unchanged from the untouched baseline |
| 2 oracle in the test phase | exec execution `character-flags-combine` -> `M3IndexCharacterFlagsCombineSmokeMain` (public launcher of the package-private relocated oracle); direct: `java -cp target/classes;target/test-classes jdk.internal.m3.indexstring.M3IndexCharacterFlagsCombineSmoke` | `PASS actual M3IndexCharacterFlags.java; pairs=256 triples=4096 rejectedInvalids=8` |
| 3 differential | `bash m3/ports/indexstring/recipe/character-flags-20261106/differential.sh <donor clone> ac5feec00a6d6206194b3ba954c6f738aae07126 <scratch>` after `package` | smoke outputs identical after the TYPE map (sha256 `caf90588...`; raw differ by the type name in the PASS literal only); `M3IndexCharacterFlagsDump` dumps identical, 45,877,105 bytes / 1,102,580 lines (sha256 `efa7e691...`): 16 cases, 256 pairs, 4,096 triples, **exhaustive** `combine` over all 1,024 x 1,024 = 1,048,576 valid ordered pairs (the flag space is 10 bits, so sampling is unnecessary), `labels` over all 1,024 values + 3 refusals, 1,000,000 seeded random pairs (937,524 IAE, 62,476 results), 100,000 seeded random UTF-16 texts; `combine=static` on both sides |
| 3b sibling | same run, of-only dumps | donor, relocated and `jdk.m3.file.M3CharacterFlags` byte-identical (sha256 `fb1aced4...`, 15,482,259 bytes) |
| 4 scope | `git status --short` | only `m3/ports/indexstring/**` |
| recipe | `relocate.py --check` after apply and after `jdk_naming.py` | `files=2 changed=0 provenance=UNCHANGED` |
| all | `JAVA_HOME=<JDK 21> MAVEN_REPO_LOCAL=<repo> bash m3/ports/indexstring/recipe/character-flags-20261106/verify.sh <donor clone> ac5feec00a6d6206194b3ba954c6f738aae07126 <scratch>` | runs the four in order |

Lessons of the run are in `RECIPE_MEMORY.tsv` (exec:java refuses a package-private main; the shared donor clone's
`FETCH_HEAD` moved mid-run, so the commit is pinned; MSYS `sed` drops the CR of a println line).

## Next leaf (not in this PR)

- `jdk.m3.file.M3CharacterFlags` (`m3/ports/file`) is the pre-combine lineage: give it `combine` by the same recipe (one
  TYPE row) or retire it onto `M3IndexCharacterFlags`; its consumers decide the direction.
- `MIndexJoinedChars` segments (this module) can carry per-segment flags and compose them with `combine` instead of a rescan
  (the plan row's purpose); that is a behavioural promotion of the joined-chars owner with its own recipe and oracle.

## Files

| File | Purpose |
|---|---|
| `m3/ports/indexstring/src/main/java/jdk/internal/m3/indexstring/M3IndexCharacterFlags.java` | the relocated owner (donor body, names mapped) |
| `m3/ports/indexstring/src/test/java/jdk/internal/m3/indexstring/M3IndexCharacterFlagsCombineSmoke.java` | the relocated executable oracle (256 pairs, 4,096 triples, 8 rejections) |
| `m3/ports/indexstring/src/test/java/jdk/internal/m3/indexstring/M3IndexCharacterFlagsCombineSmokeMain.java` | public launcher for exec-maven-plugin (the oracle class is package-private) |
| `m3/ports/indexstring/src/test/java/jdk/internal/m3/indexstring/M3IndexCharacterFlagsDump.java` | reflective dump driver of the differential gate (not a test) |
| `m3/ports/indexstring/pom.xml` | + exec execution `character-flags-combine` (phase test) |
| `m3/ports/indexstring/README.md` | + one paragraph naming the owner, the recipe and the sibling |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins |
| `differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
