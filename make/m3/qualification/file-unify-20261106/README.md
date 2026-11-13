<!-- SPDX-License-Identifier: Apache-2.0 -->
# file-unify-20261106: one module for the package `jdk.m3.file`

Recipe `FILE_UNIFY v2` (`unify.py`). Before it, two Maven modules exported the same package `jdk.m3.file` (a split
package; they could not share a module path): `m3/file` (`jdk.m3.file`, `com.m3:m3-file`, 18 main source types) and
`m3/ports/file` (`org.m3jdk.file`, `com.m3:m3-ports-file`, 82 main source types, four JNI sources, one qualification
lane). The lane owner agreed to unify under `m3/file`. Revision a (#2545, v1) held the two name clashes. The naming
owner then decided them, and revision b (v2) applies that decision.

## Target shape

| Item | Value |
|---|---|
| Module directory | `m3/file` (`m3/ports/file` is removed, no stub left) |
| Maven coordinates | `com.m3:m3-file:0.1.0-SNAPSHOT` |
| Module descriptor | `org.m3jdk.file`: `requires transitive jdk.m3.collections`; `exports jdk.m3.file`, `exports jdk.m3.file.json` |
| Natives | `m3/file/src/main/native` (was `m3/ports/file/native`) |
| Qualification | `m3/file/qualification/m3fmt-shared-lexicon-20261021` (was under `m3/ports/file`) |
| Lint | `-Xlint:all,-module -Werror` (the same on both sides before) |

Descriptor union (`templates/module-info.java`): `jdk.m3.file` contributed `exports jdk.m3.file`; `org.m3jdk.file`
contributed `requires transitive jdk.m3.collections`, `exports jdk.m3.file`, `exports jdk.m3.file.json`. Neither side
had `opens`, `uses` or `provides`. The union's class file is byte-identical to the former `org.m3jdk.file`
`module-info.class`.

Pom union (`templates/pom.xml`): dependency `m3-collections` (ports) and `junit-jupiter` 5.10.2 test (file); compiler
3.13.0 (release 21, proc none, same lint on both sides), surefire 3.2.5 (`useModulePath=false`, `failIfNoTests=true`),
jar 3.4.2, exec 3.5.0 `file-source-class-content-contract` (`verify.py`, now also given `--module-path` with the
dependency jars), and a new dependency 3.7.0 `properties` execution that supplies those jar paths.

## Name collision (decided)

| Name before | Former module | Becomes | Why |
|---|---|---|---|
| `jdk.m3.file.M3File` | `m3/ports/file` (MIndexFile token file) | `jdk.m3.file.M3File` (pure move) | donor-mapped name MIndexFile -> M3File |
| `jdk.m3.file.M3FileDelta` | `m3/ports/file` (its record) | `jdk.m3.file.M3FileDelta` (pure move) | same family |
| `jdk.m3.file.M3File` | `m3/file` (NIO boundary from MIndexResourceAccess) | `jdk.m3.file.M3NioFile` | not donor-mapped: takes the role prefix |
| `jdk.m3.file.M3FileDelta` | `m3/file` (content-delta evidence) | `jdk.m3.file.M3NioFileDelta` | companion of the NIO boundary |

The decision came from the naming owner (lane session 45). `NAME_COLLISION.tsv` records it, and
`make/m3/tooling/jdk-naming/RENAME_MAP.tsv` (two rows) and `RECIPE_MEMORY.tsv` (pattern `SPLIT_PACKAGE_NAME_COLLISION`)
carry it.

The rename happens inside the recipe; nothing is edited by hand:
- Two `RENAME` rows (`git mv` plus the token rename) run before the moves, so the ports pair moves onto the paths
  they free.
- `words` rows rename every whole-word `M3FileDelta` and `M3File` in a file where every mention means the NIO pair:
  6 main and 10 test/verification users in `m3/file`, `m3/fs` (3), eclipse optional-jmod (3), and native-api
  `NativeApiContract`.
- `qualified` rows rename only `jdk.m3.file.M3File` in the two parser integration contracts, which also use
  `jdk.m3.parser.M3File`.
- Every row pins the exact count per token.
- Module-name and path rows cover native-api `build.py` and the workflows `m3-file-shadow-stream-20261020`,
  `m3-native-shadow-stream-progress-20261020`, `m3-actual-javac-capture-20261027` and
  `m3-original-provider-parser-capture-20261026`.

`KEEP` rows: `M3FileMoveCandidates.java`, `M3ResourceLocks.java` and `M3FileMoveCandidatesContract.java` mention the NIO
pair only in prose. `make/m3/history/verify_m3_handoff.py` pins their bytes. They are not edited, and the recipe refuses if
one of them names the pair outside a comment line.

## Ledgers

| File | Content |
|---|---|
| `MOVES.tsv` | 96 rows: 90 `MOVE`, 2 `MOVE+EDIT` (BUILD.md, verify.sh), 2 `ABSORBED` (the ports pom and descriptor), 2 `RENAME`; blob before and after |
| `EDITS.tsv` | 57 rows: file, pinned preimage blob, before and after sha256, rule; preimages pinned at master `c4e268d495a0` |
| `NAME_COLLISION.tsv` | the four types of the decided collision |
| `REMAINING_REFS.tsv` | gate 7: files that still name `m3/ports/file`, and why |
| `GATES.tsv` | gate results on the PR tree |
| `RECIPE_MEMORY.tsv` | corrections made while building the recipe |

`python -I unify.py` applies the recipe. `python -I unify.py --check` writes nothing and exits 1 if a run would change
anything. A drifted preimage makes it refuse with exit 2. When master moves, re-pin the drifted rows and re-run it on
a fresh checkout. Do not merge by hand.

## Open items

- Several builds now need `jdk.m3.collections` on the module path, which the path and name edits here cannot supply:
  - `verify.py` is called without `--module-path` by `m3-file-shadow-stream-20261020` (its first step),
    `m3-file-build-impact-20261020`, `m3-file-delta-20261020` and `m3-file-move-candidates-20261021`.
  - `m3-file-content-delta`, `m3fs-inline-primitives` and the eclipse-core Maven step run `mvn -f m3/file/pom.xml`
    without `m3-collections` installed.
  - `m3-native-shadow-stream-progress-20261020` compiles `module-info.java` with a hand-picked subset of sources.
  - native-api `build.py` takes a hash-pinned m3-file jar (foundation sha256) and puts only that jar on the module
    path; it needs a re-seal by the pack owner.
- The nlp-search input lock (`INPUTS.json`, recipe custody, `doc/m3/name-mapping.d/m3-file-nlp-search.json`) pins
  sha256 values per `m3/ports/file` path, including the token `M3File`, which now lives at
  `m3/file/src/main/java/jdk/m3/file/M3File.java`. No tool in the tree re-seals that lock, and it had already drifted on
  master (`make/m3/qualification/ci-repair-20261031`, C5). Re-seal it with the module-pack owner.
- Pre-existing problems that this branch does not touch:
  - `jdk_naming.py plan --only m3/search/` has 2 rewrites on master (#2550 files).
  - `make/m3/history/verify_m3_handoff.py` refuses on master ("invalid immutable first-party recipe custody"). The KEEP files
    are byte-identical to master.
  - `m3-resource-locks-20261021.yml` pins an older sha256 of `M3ResourceLocks.java`.
- `jdk.m3.file.M3CharacterFlags` still lacks `combine`. That is a follow-up leaf.
