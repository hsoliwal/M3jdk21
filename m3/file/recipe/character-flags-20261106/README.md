<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# character-flags-20261106: align `jdk.m3.file.M3CharacterFlags` with its sibling (recipe ALIGN_FILE_CHARACTER_FLAGS v1)

Follow-up leaf of the m3/file unification (#2580) and of `m3/ports/indexstring/recipe/character-flags-20261106` (#2523).
`jdk.m3.file.M3CharacterFlags` was the donor `IndexCharacterFlags` at its pre-combine blob: its body equalled the crate
before-copy `3219d69fef57` modulo header, package and type name, so it lacked `ALL_OF`, `ANY_OF` and the package-private
`combine(int,int)` that its sibling `jdk.internal.m3.indexstring.M3IndexCharacterFlags` carries since #2523. This leaf
relocates the same donor blob (`f6abb8a6c023`, with `combine`) onto it with the #2523 recipe and one `TYPE` row, so both
classes are one lineage again: bodies equal modulo package, type name and the anchor's recipe path.

## Owner decision: ALIGN, not RETIRE

| Question | Finding |
|---|---|
| consumers | `git grep -n M3CharacterFlags`: one production user, `jdk.m3.file.M3TextFacts` (same package, m3/file), which calls the package-private `initial(int)` and `add(int,char)` and reads `ASCII`/`LATIN1`/`BLANK`/`EMPTY`; every other hit is a ledger, custody copy or doc |
| sibling visibility | `M3IndexCharacterFlags` lives in `jdk.internal.m3.indexstring`; `initial`, `add` and `combine` are package-private there, so a `jdk.m3.file` caller cannot reach them without widening an internal type |
| module dependency | `m3/ports/indexstring` is `com.m3.compat:indexstring-retained-view`, a classpath module without `module-info`; `org.m3jdk.file` requires only `jdk.m3.collections`; a JDK module would have to depend on a compat artifact and an unexported `jdk.internal` package |
| public surface | `jdk.m3.file.M3CharacterFlags` is a public class of the exported package `jdk.m3.file`; retiring it removes API |

RETIRE is therefore not possible without a dependency from `org.m3jdk.file` onto a non-exported internal package and a
visibility change, so the capability keeps two classes with different consumers in different modules, aligned by one
recipe. `M3TextFacts.combine` (lines 58-61) inlines the same `ALL_OF`/`ANY_OF` composition without the out-of-domain
refusal; repointing it at `M3CharacterFlags.combine` would change its behaviour for arbitrary `characterFlags` of the
public record, so it is recorded here and not done (NEC).

## Recipe

| Item | Value |
|---|---|
| recipe | `relocate.py` v1 (ALIGN_FILE_CHARACTER_FLAGS), derived byte for byte from `m3/ports/indexstring/recipe/character-flags-20261106/relocate.py` v1: only the recipe path, the module default and the docstring differ |
| map | `RELOCATION_MAP.tsv`: 1 `FILE` row (the donor owner -> `src/main/java/jdk/m3/file/M3CharacterFlags.java`), `PACKAGE com.synexia.indexstring -> jdk.m3.file`, one `TYPE IndexCharacterFlags -> M3CharacterFlags` |
| donor | `hsoliwal/com.synexia` develop `ac5feec00a6d6206194b3ba954c6f738aae07126` (the #2523 donor commit), blob `f6abb8a6c023` |
| pins | `PROVENANCE.tsv`: donor sha256 `32d44ad5...`, target sha256 `ed407ae3...`, `body_match_renamed=TRUE` |
| change | `+ALL_OF`, `+ANY_OF`, `+combine(int,int)` (the crate hunk), the M3JDK header (the receiver line `Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.` is dropped, its mapping is named in the map) and one `//FLLDCIM: RELOCATE` anchor; `javap -public` unchanged |
| run | `python -I m3/file/recipe/character-flags-20261106/relocate.py --donor-git <com.synexia clone> --donor-rev ac5feec00a6d6206194b3ba954c6f738aae07126 [--check]` |

## Gates (GATES.tsv)

`JAVA_HOME=<JDK 21> MAVEN_REPO_LOCAL=<repo> bash m3/file/recipe/character-flags-20261106/verify.sh <com.synexia clone> ac5feec00a6d6206194b3ba954c6f738aae07126 <scratch>`
runs the recipe check, the sibling module's package (its smoke oracle and the dump driver), the m3/file package without
its tests, the naming plan and `differential.sh`. `differential.sh` replays #2523's inputs through
`M3IndexCharacterFlagsDump` for both classes: full dump (exhaustive 1,024 x 1,024 `combine` table, 256 pairs, 4,096
triples, labels, 1,000,000 random pairs, 100,000 random texts) and of-only dump byte-identical, with #2523's digests
`efa7e691...` and `fb1aced4...`; `javap -public` and `javap -p` identical modulo the type name. The m3/file tests and
class identity are compared against a master baseline build (GATES.tsv rows 4 and 5).

## Files

| File | Purpose |
|---|---|
| `m3/file/src/main/java/jdk/m3/file/M3CharacterFlags.java` | the aligned owner (donor body, names mapped) |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | recipe, map, pins |
| `differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
