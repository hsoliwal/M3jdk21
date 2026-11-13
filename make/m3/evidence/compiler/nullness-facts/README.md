<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/compiler/nullness-facts: opt-in javac nullness-facts plugin

Module `org.m3jdk.compiler.nullness` (package `jdk.m3.compiler.nullness`), Maven coordinates
`com.m3:m3-compiler-nullness-facts:0.1.0-SNAPSHOT`, Java 21, requires `jdk.compiler` (and through it
`java.compiler`), nothing else.

## What it is

| Type | Role |
|---|---|
| `M3JavaNullnessCompilerPlugin` | the javac plugin, opt-in with `-Xplugin:M3NullnessFacts` (no arguments). A `TaskListener` on `ANALYZE` scans every compilation unit once (identity-keyed array hash table, bounded at one million units), and for every method with 1 to 1024 parameters and every `if` whose condition refines a parameter's nullness on either edge prints one row to stderr: `M3_NULLNESS_FACT <source URI> <start offset> <method> <positive edge> <negative edge>` with `slot:NULLNESS` per parameter. It never rewrites trees, source or bytecode and never fails a compilation. |
| `M3JavaNullnessFlow` | the fact lattice: two bits per parameter slot (null, non-null, maybe, unreachable) packed in `long[]`, scoped by an `Epoch` (source SHA-256, compiler-context SHA-256, attributed-graph SHA-256, symbol generation); `start`, `assumeNull`, `assumeNonNull`, `assign`, `invalidate`, `invalidateAll`, `join`, `contentKey`. |
| `M3JavaNullnessJavacBridge` | the fail-closed bridge from javac's attributed Tree API to the lattice: pure null comparisons of parameters (literal on either side), `instanceof`, boolean literals, negation, parentheses, `&&` and `||`, in one traversal per guard (`Edges` record, the linear Boolean transfer); a field, array element, method call, assignment, local that is not a mapped parameter, or any other form, and guards beyond 96 nodes, 12 short-circuit operators or 2,000,000 fact words, leave the facts unchanged. |

Evidence only: a fact is a candidate, not a compiler certificate, and the plugin changes nothing. `jar --describe-module`:
`org.m3jdk.compiler.nullness` exports `jdk.m3.compiler.nullness`, requires `java.compiler` and `jdk.compiler` transitive,
provides `com.sun.source.util.Plugin` with `M3JavaNullnessCompilerPlugin`; `META-INF/services/com.sun.source.util.Plugin`
names the same provider for classpath use.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia`, lineage commit `982689e16763e54b01632407d64846fbea5719b6` (2026-11-01, "feat(compiler): materialize exact recipe-owned opt-in javac nullness plugin result"), an ancestor of develop `ca1aafc4d94f9dd1ee288226dcca070cc60991c9` |
| why the lineage commit | the develop live tree carries the plugin (`synexia-m3-recipe/.../M3JavaNullnessCompilerPlugin.java`, blob `e06512091f03`) but not the lattice and the bridge it calls; those survive on develop only as retained variants of PR 14877 (`m3/qualification/master-landing-20261028/retained-variants/pr14877/...`), and the retained bridge is the recursive-transfer predecessor `8553ed5abdf3`, while the donor crate `synexia-openrewrite-recipes/crates/m3-javac-nullness-plugin-20261101/qualification/verify.sh` seals the linear transfer `2045b84846cf` (`ed6eb1ca42e`). `982689e16763` is the last commit at which plugin `e06512091f03`, lattice `347ba86620ca`, bridge `2045b84846cf`, the three proofs and the fixture all sit at their live paths. `PROVENANCE.tsv` pins, per file, the lineage blob, the develop blob (or `ABSENT`) and the retained-variant path and blob. |
| donor files | the three main sources above; `synexia-m3-recipe/crates/m3-java-nullness-flow-20261031/qualification/{NullnessProof,JavacBridgeProof,JavacGuardMatrixProof}.java` (relocated as `M3NullnessProof`, `M3JavacBridgeProof`, `M3JavacGuardMatrixProof`); the plugin crate's `qualification/Sample.java` (fixture, blob `c8e4a4d8cb36`, byte for byte) and `qualification/META-INF/services/com.sun.source.util.Plugin` |
| recipe | `recipe/relocate.py` v1 (`RELOCATE_NULLNESS_FACTS_PLUGIN`): `python -I recipe/relocate.py --donor-git <clone> --donor-rev 982689e16763e54b01632407d64846fbea5719b6 --develop-rev <develop> [--check]` |
| map | `RELOCATION_MAP.tsv`: 6 `FILE`, 1 `FIXTURE`, 1 `SERVICE`, `PACKAGE com.synexia.m3.recipe -> jdk.m3.compiler.nullness`, 6 `TYPE` (3 identity, 3 `M3*` renames), 1 `PROSE` (the `PROSE_PRODUCT_VERSION` rule of `make/m3/tooling/jdk-naming/NAMING_RULES.tsv`), 5 `RETAINED` |
| provenance | `PROVENANCE.tsv`: donor commit, blob, sha256; develop blob; retained path and blob; target sha256; eol; rewrites; `body_match` |
| naming | `python -I make/m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/compiler/nullness-facts/`: 0 moves, 0 rewrites, 0 refusals |

The recipe rewrites only the license header (M3JDK style), the `package` line, mapped `import` lines, the declared
type names, comment lines matched by the declared `PROSE` rule, and inserts one `//FLLDCIM: RELOCATE` anchor before
the top-level type naming the donor blob URL at the donor commit; everything else is kept byte for byte. `body_match`
is strict: `TRUE` for the lattice, the bridge and the fixture; `FALSE` with the rewrite named in the `rewrites`
column for the plugin (one Javadoc line: `M3JDK21` to `M3JDK 21`), the three proofs (their own type name on two lines
each) and the service descriptor (one fully qualified name). The recipe is idempotent (second run `changed=0`, also
after `jdk_naming.py`) and refuses an unmapped package or import instead of guessing the closure. Not relocated:
`M3JavaNullnessFlowRecipeTest`, `M3JavaNullnessJavacBridgeRecipeTest`, `*LinearTransferProof`, `*RecipeProof`
(they test the donor's OpenRewrite recipe custody, its sealed pre/post-images and the old-vs-new bridge; recipe
authority stays in the donor); the lattice assertions of the first are a subset of `M3NullnessProof`.

## Build

| Step | Command |
|---|---|
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/compiler/nullness-facts/pom.xml package` (`-Xlint:all,-missing-explicit-ctor -Werror`, `proc none`; the service descriptor is copied into `target/classes` only after compilation, as in `m3/compiler/jdk-standard`) |
| from the control reactor | `mvn -f make/m3/pom.xml -pl compiler/nullness-facts package` (`compiler/nullness-facts` is a default module of `make/m3/pom.xml`) |
| use the plugin | `javac -processorpath target/classes -Xplugin:M3NullnessFacts ... 2> facts.tsv` (or the jar on the processor path or module path) |
| self-tests directly | `java -cp target/classes;target/test-classes jdk.m3.compiler.nullness.M3NullnessProof` (also `M3JavacBridgeProof`, `M3JavacGuardMatrixProof`, `M3NullnessFactsPluginSelfTest`; `:` instead of `;` outside Windows) |
| differential | `bash recipe/differential.sh <donor clone> 982689e16763e54b01632407d64846fbea5719b6 <scratch dir>` after `package` |
| all gates | `bash verify.sh <donor clone> 982689e16763e54b01632407d64846fbea5719b6 <develop rev> <scratch dir>` |

The lint exclusion `-missing-explicit-ctor` is the only one: in a named module with an exported package javac reports
the donor plugin class (kept byte for byte, no constructor; the service loader needs the implicit public one). A
plain `-Xlint:all -Werror` run fails on exactly that warning and nothing else (GATES.tsv, gate 1).

## Gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 1 `mvn package`, JDK 21.0.9, `-Xlint:all,-missing-explicit-ctor -Werror` | BUILD SUCCESS |
| 2 donor tests in the test phase | 4 JUnit tests, 0 failures: `M3_JAVA_NULLNESS_PROOF_PASS checks=9003`, `M3_JAVA_NULLNESS_JAVAC_BRIDGE_PASS cases=22 checks=115`, `M3_JAVAC_GUARD_MATRIX_PASS guards=222 checks=6001`, `M3_NULLNESS_FACTS_PLUGIN_SELFTEST PASS immutable_bytecode=2 facts=3 modes=default,Xint checks=21` (this JDK's javac on `fixtures/Sample.java`: class files byte-identical with and without the plugin and under `-J-Xint`, exactly the three rows the donor plugin produced, `fixtures/Sample.facts.expected.tsv`) |
| 3 differential donor vs relocated | `fixtures/Sample.java` + 200 seeded sources (`recipe/generate_fixtures.py`, seed 20261106, 8,046 `if` statements, 286 class files): 2,803 `M3_NULLNESS_FACT` rows, stderr streams byte-identical for the donor plugin, the relocated plugin and the relocated plugin under `-J-Xint`, sha256 `411cd6e7221ed2fc180b2b6e194d9e283a8cc4656f26f7802bf20551dcd7988c`; all 286 class files equal to the plugin-free baseline in every instrumented compilation |
| 4 `jar --describe-module` | `org.m3jdk.compiler.nullness` exports `jdk.m3.compiler.nullness`, requires `java.compiler` and `jdk.compiler` transitive, provides `com.sun.source.util.Plugin` |
| 5 scope | `m3/compiler/nullness-facts/**` and one `make/m3/pom.xml` line after `compiler/lifetime-slots` |

Lessons of the run are in `RECIPE_MEMORY.tsv`.

## Files

| File | Purpose |
|---|---|
| `src/main/java/module-info.java`, `src/main/java/jdk/m3/compiler/nullness/M3JavaNullness{CompilerPlugin,Flow,JavacBridge}.java` | the module and the three relocated sources |
| `src/main/resources/META-INF/services/com.sun.source.util.Plugin` | the relocated service descriptor (installed after compilation) |
| `src/test/java/jdk/m3/compiler/nullness/M3{NullnessProof,JavacBridgeProof,JavacGuardMatrixProof}.java` | the three relocated donor proofs |
| `src/test/java/jdk/m3/compiler/nullness/M3NullnessFactsPluginSelfTest.java` | the donor plugin test (the plugin crate's `verify.sh`) as a Java self-test over the relocated plugin |
| `src/test/java/jdk/m3/compiler/nullness/M3NullnessFactsJUnitTest.java` | Maven/JUnit entry point for the four |
| `src/test/resources/fixtures/Sample.java`, `src/test/resources/fixtures/Sample.facts.expected.tsv` | the donor fixture (byte for byte) and the rows the donor plugin emits for it |
| `recipe/relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins (lineage, develop, retained) |
| `recipe/generate_fixtures.py`, `recipe/differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | corpus generator, gates and their record |
