<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 naming standard

Status: normative. Decided by the user on 2026-10-31: M3JDK21 is standalone and self-sufficient, every M3 name follows
JDK naming, and the legacy Synexia-named code is renamed and kept (absorbed), not deleted. One rule table,
[NAMING_RULES.tsv](NAMING_RULES.tsv), is the executable form of this document; it is read by the rename recipe
[jdk_naming.py](jdk_naming.py) and by the JUnit conformance test in [conformance/](conformance/). A name that
either executor would still rewrite is a violation.

## 1. Scope

Every file M3JDK21 carries outside the OpenJDK sources it inherited: `m3/**`, `.m3/**`, `.github/**`, `make/**` and
the M3 classes under `src/**` and `test/**`. Text is in scope by suffix (`.java .yml .yaml .xml .sh .py .mjs .js
.properties .gradle .toml .cfg .ini .c .h .cpp .json .txt` and `pom.xml`). Lineage is out of scope (section 4).

## 2. Names

| Kind | Standard | Example |
|---|---|---|
| Module, exported | `jdk.m3.<area>` | `jdk.m3.collections` |
| Module, internal | `jdk.internal.m3.<area>` | `jdk.internal.m3.rewrite` |
| Package, exported | `jdk.m3.<area>[.<sub>]` (was `com.m3.<area>`) | `jdk.m3.collections.interop` |
| Package, internal | `jdk.internal.m3.<area>` (was `com.synexia.m3.<area>`, `com.synexia.<area>`, `org.synexia.<area>`) | `jdk.internal.m3.common.collections` |
| Type | `M3` + UpperCamel; the prefix appears once (`M3SynexiaX` and `SynexiaM3X` are `M3X`) | `M3LongArrayList` |
| Member | lowerCamel; a `Synexia` infix becomes `M3` | `canonicalM3Owner` |
| Constant, environment, property | UPPER_SNAKE; project prefix `M3_` (was `SYNEXIA_`) | `M3_RECIPE_HOME` |
| JNI symbol | follows the package: `Java_jdk_internal_m3_<pkg>_<Type>_<method>`, `Java_jdk_m3_<pkg>_<Type>_<method>` | `Java_jdk_m3_fs_M3FsNative_open` |
| JNI header | `jdk_internal_m3_<pkg>_<Type>.h`, `jdk_m3_<pkg>_<Type>.h` | `jdk_internal_m3_mindex_TextMetadata.h` |
| Native library | `libm3<name>` | `libm3textio` |
| Module, crate, artifact, script file | kebab `m3-<name>` (was `synexia-<name>`) | `m3-openrewrite-recipes` |
| Python and shell names | snake `m3_<name>` (was `synexia_<name>`) | `check_m3_full_family.py` |
| Path segment | `m3` (was `synexia`) | `m3/vendor/m3/` |
| Flattened package path (`__` for `/`) | follows the package | `src__main__java__jdk__internal__m3__search` |
| Invariant and ledger id | `M3-<NAME>-<N>` (was `SYNEXIA-<NAME>-<N>`) | `M3-LLM-FIRST-2` |
| Maven groupId | unchanged (`com.m3`, `com.synexia.*`): a coordinate, not a package; mapped by the artifact phase | `<groupId>com.m3</groupId>` |
| Maven artifactId | kebab `m3-<name>` | `m3-jdk-naming-conformance` |
| Product name, prose | `M3JDK`, with its version `M3JDK 21` (comment lines; the `PROSE` rules) | `// Adapted for M3JDK 21` |
| Product name, type prefix | `M3Jdk` | `M3Jdk21Handoff` |
| Product name, constant and id | `M3JDK` | `M3JDK21_REFUSE`, `M3JDK-PORT-1` |
| Product name, file, artifact and kebab | `m3jdk` | `m3jdk21.tar.gz`, `m3jdk-proof.yml` |
| Repository | `hsoliwal/M3jdk21`, unchanged in every link and PR reference | `hsoliwal/M3jdk21#2216` |

The word `Synexia` on its own stays where it names the donor project in prose: "Adapted from Synexia
MIndexRegexTrigramQuery", "No Synexia runtime dependency", a GPL modification notice. Renaming it would make the
statement false. Identifiers never keep it.

## 3. What is kept

| Keep rule | Span or line | Why |
|---|---|---|
| `COPYRIGHT` | a line containing `Copyright` or `SPDX-` | legal text |
| `GROUP_ID` | `<groupId>com.synexia...</groupId>`, `<groupId>com.m3...</groupId>` | coordinates, not packages |
| `SECRET` | `secrets.SYNEXIA_*` | a repository secret is a dependency; the standalone check must still see it |
| `LEGAL_FILE` | `Synexia-NOTICE`, `Synexia-LICENSE`, `Synexia-RIGHTS`, `Synexia-AUTHORS`, `M3-SYNEXIA-*.md` | release-boundary attribution files |
| `REPOSITORY` | links to and the name `hsoliwal/com.synexia` | a true link to the donor; the standalone check removes dependencies, the rename never hides them |
| `LEGAL_MD` | `legal/synexia.md` | the JDK legal file of the Synexia components is named after the donor, like every `legal/<component>.md` |
| `LEGAL` (path) | files named `*synexia-notice*`, `-license`, `-rights`, `-authors`, `legal/synexia.md` | the files behind `LEGAL_FILE` and `LEGAL_MD`; they keep their names and follow their renamed directory (`.../m3-openrewrite-recipes/licenses/Synexia-LICENSE`) |

Three forms are not names and no rule touches them:

- A bare prefix literal `"com.synexia."` (nothing after the dot) is a check (`startsWith("com.synexia.")`), not a
  name; after the rename it matches nothing, which is the truthful outcome for a check against the old scheme.
- A lowerCamel record key such as `"synexiaState"` is data shared with the records that carry it (lineage JSON is
  not rewritten), so reader and record keep the same key.
- A domain name (`synexia.com`) is not a segment.
- A snake-case suffix (`dependency_on_synexia`) states something about the donor, like prose, and stays.

A kept span keeps only itself; the rest of its line is renamed, so a one-line
`<dependency><groupId>com.synexia</groupId><artifactId>synexia-x</artifactId></dependency>` names the renamed
module `m3-x`.

## 4. Lineage

Not rewritten: Markdown, TSV ledgers, `.rst .adoc .html .log .patch`, NOTICE/LICENSE/COPYRIGHT/AUTHORS files,
`m3/docs/**`, `m3/release/audit-*`, `evidence/`, `receipts/` and `surefire-reports/` folders, `TEST-*.xml`, and JSON
whose path says provenance, evidence, receipt, manifest, `SOURCE_SHA256`, `FINAL_` or readback. A lineage file whose
renamed path already exists stays where it is.

The pins inside lineage do follow the files they pin. [reseal.py](reseal.py) carries every sha256, git blob id and
repository path from its value before the rename to its value after, round after round until nothing changes, and
writes every carried value to `RENAME_RESEAL.tsv` (`kind before after`). A manifest therefore pins the live file,
and the old digest in history is traceable through the ledger. An old value with two different new values is
reported as `AMBIGUOUS` and left alone.

## 5. The rules

Order matters: the first matching rule rewrites a token. `NAMING_RULES.tsv` is the normative table; this list gives
one example per rule.

| Rule | Before | After |
|---|---|---|
| `PKG_M3` | `com.synexia.m3`, `org.synexia.m3` | `jdk.internal.m3`, `jdk.internal.m3` |
| `PKG` | `com.synexia.util.X`, `com.synexia.M3Planner`, `lab_com.synexia.rewrite.X` | `jdk.internal.m3.util.X`, `jdk.internal.m3.M3Planner`, `lab_jdk.internal.m3.rewrite.X` |
| `PKG_PATH_M3` | `com/synexia/m3/x/` | `jdk/internal/m3/x/` |
| `PKG_PATH` | `com/synexia/util/` | `jdk/internal/m3/util/` |
| `JNI_M3` | `Java_com_synexia_m3_X_f` | `Java_jdk_internal_m3_X_f` |
| `JNI` | `com_synexia_mindex_TextMetadata.h` | `jdk_internal_m3_mindex_TextMetadata.h` |
| `PKG_DUNDER_M3` | `java__com__synexia__m3__search` | `java__jdk__internal__m3__search` |
| `PKG_DUNDER` | `java__com__synexia__rewrite` | `java__jdk__internal__m3__rewrite` |
| `TYPE_M3` | `M3SynexiaSourceCanon`, `SynexiaM3Owner` | `M3SourceCanon`, `M3Owner` |
| `TYPE` | `SynexiaFoo`, `canonicalSynexiaOwner` | `M3Foo`, `canonicalM3Owner` |
| `CONST` | `SYNEXIA_TOKEN`, `M3JDK21_SYNEXIA_RECIPE_HOME` | `M3_TOKEN`, `M3JDK21_M3_RECIPE_HOME` |
| `KEBAB` | `synexia-import`, `reviewed_synexia-commit` | `m3-import`, `reviewed_m3-commit` |
| `SNAKE` | `check_synexia_full_family.py` | `check_m3_full_family.py` |
| `KEBAB_SUFFIX` | `check-synexia.py` | `check-m3.py` |
| `CONST_KEBAB` | `SYNEXIA-LLM-FIRST-2` | `M3-LLM-FIRST-2` |
| `SEGMENT` | `m3/vendor/synexia/lib` | `m3/vendor/m3/lib` |
| `SEGMENT_DOT` | `jdk.m3.synexia.importer`, `synexia.recipe-source-custody/1` | `jdk.m3.importer`, `m3.recipe-source-custody/1` |
| `PKG_COM_M3` | `com.m3.collections.A` | `jdk.m3.collections.A` |
| `PKG_PATH_COM_M3` | `com/m3/collections/` | `jdk/m3/collections/` |
| `JNI_COM_M3` | `Java_com_m3_fs_M3FsNative_open` | `Java_jdk_m3_fs_M3FsNative_open` |
| `PKG_DUNDER_COM_M3` | `java__com__m3__collections` | `java__jdk__m3__collections` |
| `DOUBLE` | `m3-m3-x`, `M3_M3_MODE`, `jdk.m3.m3.importer`, `jdk/m3/m3/importer` | `m3-x`, `M3_MODE`, `jdk.m3.importer`, `jdk/m3/importer` |
| `PRODUCT_CONST` | `M3_JDK21_REFUSE` | `M3JDK21_REFUSE` |
| `PRODUCT_KEBAB` | `m3-jdk21.tar.gz` | `m3jdk21.tar.gz` |
| `PRODUCT_ID` | `M3-JDK-PORT-1` | `M3JDK-PORT-1` |
| `PROSE_PRODUCT_VERSION` | `// built for M3jdk21` | `// built for M3JDK 21` |
| `PROSE_PRODUCT` | `# the M3 JDK image` | `# the M3JDK image` |

Rows of kind `PROSE` apply only to whole comment lines (`//`, `/*`, `*`, `#`, `<!--`), so a string literal or a data
value with the product name keeps its exact text. The product-name rules follow the user's decision of 2026-10-31
(per-kind JDK style).

`DOUBLE` repairs the `m3-m3-x` that `synexia-m3-x` becomes and the `jdk.m3.m3.x` that `com.m3.synexia.x` becomes
(no doubled prefix or segment existed before the rename). Paths are
renamed by the same rules as text, so a reference and the file it names always agree. The Python test replays every
row of this table, so the table is a fixture, not a description.

## 6. Enforcement

| Gate | Command | Passes when |
|---|---|---|
| Recipe fixed point | `python -I m3/tooling/jdk-naming/jdk_naming.py plan --tree HEAD` | `moves=0 rewrites=0 refusals=0` |
| Recipe tests | `python -I m3/tooling/jdk-naming/test_jdk_naming.py`, `test_reseal.py` | all pass |
| JUnit conformance | `mvn -B -ntp -f m3/tooling/jdk-naming/conformance/pom.xml test` | `JdkNamingConformanceTest` finds no violation and agrees with the recipe on the shared fixtures |
| Workflow | `.github/workflows/m3jdk-naming.yml` | the three gates above, on every pull request |
| Compile-time scheme | javac plugin `M3JdkStandard scheme=m3` (#2188) | package and name scheme of every compiled M3 source |

## 7. Changing a rule

1. Edit the row in `NAMING_RULES.tsv` (never the executors).
2. Add the discriminating fixture to `test_jdk_naming.py` and to `JdkNamingConformanceTest` (both must agree).
3. Run `plan --tree HEAD`, then `apply --tree HEAD --ref <branch>`, then `reseal.py`, until each reports its fixed point.
4. Add the example row to section 5 (the Python test checks every rule name appears here).
5. Record the correction as a `RECIPE_MEMORY.tsv` row in the com.synexia recipe memory in the same change.
