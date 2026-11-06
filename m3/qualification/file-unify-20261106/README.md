<!-- SPDX-License-Identifier: Apache-2.0 -->
# file-unify-20261106: one module for the package `jdk.m3.file`

Recipe `FILE_UNIFY v1` (`unify.py`). Before it, two Maven modules exported the same package `jdk.m3.file` (a split
package; they could not share a module path): `m3/file` (`jdk.m3.file`, `com.m3:m3-file`, 18 main source types) and
`m3/ports/file` (`org.m3jdk.file`, `com.m3:m3-ports-file`, 82 main source types, four JNI sources, one qualification
lane). The lane owner agreed to unify under `m3/file`.

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
had `opens`, `uses` or `provides`. The union equals the `org.m3jdk.file` descriptor, and its class file is
byte-identical to that baseline's `module-info.class` (gate 2).

Pom union (`templates/pom.xml`): dependency `m3-collections` (ports) and `junit-jupiter` 5.10.2 test (file); compiler
3.13.0 (release 21, proc none, same lint on both sides), surefire 3.2.5 (`useModulePath=false`, `failIfNoTests=true`,
file), jar 3.4.2 (ports), exec 3.5.0 `file-source-class-content-contract` (file; `verify.py` now also gets
`--module-path` with the dependency jars), and one new execution, dependency 3.7.0 `properties`, which gives those
jar paths to the exec arguments. Name and description come from the ports pom, rewritten for the unified module.

## Ledgers

| File | Content |
|---|---|
| `MOVES.tsv` | 97 rows: 88 `MOVE`, 2 `MOVE+EDIT` (BUILD.md, verify.sh), 2 `ABSORBED` (the ports pom and descriptor, consumed by the unions), 4 `HELD`; blob before and after (equal for every pure move) |
| `EDITS.tsv` | 23 rows: file, pinned preimage blob, before and after sha256, rule; preimages pinned at master `dec547222b30` |
| `NEC.tsv` | the held pairs |
| `REMAINING_REFS.tsv` | gate 7: the files that still name `m3/ports/file`, and why |
| `GATES.tsv` | gate results |
| `RECIPE_MEMORY.tsv` | corrections made while building the recipe |

`python -I unify.py` applies the recipe. `python -I unify.py --check` writes nothing and exits 1 if a run would change
anything. A drifted preimage makes it refuse with exit 2. When master moves, re-pin the drifted rows and re-run it on
a fresh checkout. Do not merge by hand.

## Held pairs (NEC, not decided here)

`jdk.m3.file.M3File` and `jdk.m3.file.M3FileDelta` exist on both sides with different bytes. They are different types
under one name; the clash dates from the JDK naming rename `fe35ea98841`, which moved the ports package into
`jdk.m3.file`. `m3/file` has the NIO read boundary (from MIndexResourceAccess). `m3/ports/file` has the MIndexFile
token file. Both copies of each pair sit under `held/file/` and `held/ports-file/`. Until an owner names one side,
the unified module does not compile: 200 errors, 162 of them `cannot find symbol: class M3File`. `NEC.tsv` lists the
users of each side, in the module and outside it.

`probe_rename.py` is a measurement and never part of the recipe. It renames the `m3/file`-side pair in a scratch
worktree (`M3NioFile`, `M3NioFileDelta`) and restores the ports pair. This shows what compiles once a decision
exists: every ports-side class file stays byte-identical, and the only `m3/file`-side class files that differ are the
ones that name the renamed types (GATES.tsv gates 1-4).

## Open items

- Decide the held pairs (above). Then fold that decision into this recipe as a new version, or into a follow-up leaf.
- Some workflows and build scripts will not work until that decision exists and the module's new dependency is
  provided. They are not edited here because the fix is more than a path string:
  `m3-native-shadow-stream-progress-20261020.yml` compiles a hand-picked subset together with `module-info.java`
  (now `requires jdk.m3.collections`, `exports jdk.m3.file.json`) and runs `-m jdk.m3.file/...`;
  `m3-file-shadow-stream-20261020.yml`, `m3-actual-javac-capture-20261027.yml` and
  `m3-original-provider-parser-capture-20261026.yml` compile `m3/file/.../M3File.java` (held);
  `m3-file-content-delta.yml`, `m3fs-inline-primitives.yml` and the Maven step of `m3-eclipse-core-optional-jmod.yml`
  run `mvn -f m3/file/pom.xml`, so `m3-collections` must be installed first; `m3-file-build-impact-20261020.yml`,
  `m3-file-delta-20261020.yml` and `m3-file-move-candidates-20261021.yml` run `verify.py` without `--module-path`;
  `m3/tooling/module-packs/build-support/native-api` (`build.py`, `NativeApiContract.java`) names module
  `jdk.m3.file` and needs `jdk.m3.collections` on its module path.
- The nlp-search input lock (`INPUTS.json`, recipe custody, `m3/docs/name-mapping.d/m3-file-nlp-search.json`) pins
  sha256 values per `m3/ports/file` path. It had already drifted on master (`m3/qualification/ci-repair-20261031`,
  C5). Re-seal it with the module-pack owner.
- `jdk.m3.file.M3CharacterFlags` still lacks `combine`. That is a follow-up leaf, as
  `m3/ports/indexstring/recipe/character-flags-20261106` records.
