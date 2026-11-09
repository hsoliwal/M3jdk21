# reactor-wiring-20261107 — wire off-reactor Maven modules into m3/pom.xml, one gate per module

Lane 37 of the standalone-build programme (plus four `collections/*` modules gated by request of lane a4).
Base: `a33f82c8af9` (master 2026-11-07). Branch `m3/reactor-wiring-20261107a`. Pom edits only; no module source
was touched. Every module that has a `pom.xml` under `m3/` but was outside the `<module>` closure of `m3/pom.xml`
and is in this lane's set was gated once with its own `-Werror`/surefire settings:

```
mvn -B -ntp -T1 -f m3/pom.xml -pl <module> -am -DskipTests=false package
```

GREEN (`BUILD SUCCESS`) -> `<module>` line in the default `<modules>` of `m3/pom.xml` (WIRED).
RED -> `<module>` line in profile `m3-offreactor-held` of the same pom (HELD; `-Pm3-offreactor-held` builds it),
first error recorded in `DECISION.tsv` (file/class, line, message). Each added line carries an
`<!-- //FLLDCIM: MRL - reactor-wiring-20261107 ... -->` anchor (CNPCDA).

Not this lane (recorded, untouched): `collections/recipe`, `collections/snapshot-recipe-20261017` (lane a4),
`compiler/jdk-standard`, `compiler/source-signals`, `tooling/*` (lane d7), `vendor/third_party`, `vendor/tornadovm`.

## Files

| file | role |
|------|------|
| `apply.py` | hash-pinned replay of `EDITS.tsv` (`apply` idempotent, refuses drift; `verify`; `edit`/`record` used by the gates; ops `insert-before`, `delete-block`) |
| `EDITS.tsv` | the linear sha256 chain of every `m3/pom.xml` edit (anchor + payload as JSON strings) |
| `gate.sh` | one module gate: temporary wiring, one Maven run, revert, decision, recorded edit, ledger rows, one commit |
| `unhold.sh`, `held_payload.py` | take a HELD module out of the profile (recorded `delete-block`) so `gate.sh` can re-gate it |
| `DECISION.tsv` | module, lane, WIRED/HELD, GAV, reactor deps, gate, first error (file, line, message), own tests |
| `GATES.tsv` | every Maven run: command, log path (S: scratch), rc, build result, wall seconds, reactor modules built, own tests, log sha256 |
| `testsum.py` | per-module test counts and failing set from a reactor log (BLLSE-LAW) |
| `BASELINE_TESTS.tsv`, `BASELINE_FAILING.tsv` | master (untouched pom) `verify -fae -Dmaven.test.failure.ignore=true`: per-module counts and the 9-row failing set |
| `FULL_TESTS.tsv`, `FULL_FAILING.tsv` | the same for the wired reactor (final run of #2779) |
| `BASELINE_POST2781_*.tsv`, `FULL_UNHOLD_*.tsv` | the same on master 7b155a401e8 before and after the un-hold (section below) |
| `reconfirm.sh` | DECISION.tsv row re-confirming a HELD module from CPU evidence, no Maven run |

Replay on a checkout of the base: `python -I m3/qualification/reactor-wiring-20261107/apply.py apply --root .`
(`verify` reports whether `m3/pom.xml` is at the chain's final sha256).

## Decisions

Lane 37 (20 modules): WIRED 12 — `eclipse/core-runtime`, `eclipse/framework`, `migration`, `lexicon/hf-intake`,
`ports/capability-runtime`, `ports/precompute`, `ports/indexstring`, `ports/parallel`, `ports/vcpu`,
`ports/regex-cursor`, `runtime-integration/primitive-address-identity`, `runtime-integration/regex-precompute`;
HELD 8 — see the table. Lane a4 (by request, 4 modules): WIRED 3 — `collections/gc-interop`,
`collections/interop`, `collections/signal-codegen`; HELD 1 — `collections/pcollections`.

| module | decision | first error (DECISION.tsv has the full row) |
|--------|----------|----------------------------------------------|
| ast | HELD -> WIRED 2026-11-07 (un-hold section) | `src/test/java/jdk/m3/ast/M3SyntaxGrowthAgent.java:9` package `jdk.internal.org.objectweb.asm` is not visible (testCompile) |
| history | HELD -> WIRED 2026-11-07 (un-hold section) | exec `history-contract-tests` (python3) exit 1: `OSError [Errno 22]` writing a fixture named `spaces tab<TAB>and<LF>newlines-λ.java` (Windows filename) |
| ports/compiletime | HELD | could not resolve `org.ow2.asm:asm{,-tree,-analysis}:9.6-m3-source-1` (not in the local repos nor Central) |
| ports/swing-viewport | HELD -> WIRED 2026-11-07 (un-hold section) | `src/test/java/jdk/m3/swing/testkit/M3JUnitLauncher.java:25` package `org.junit.platform.launcher` does not exist |
| ports/jfx-viewport | HELD -> WIRED 2026-11-07 (un-hold section) | could not resolve `com.m3:m3-swing-viewport:0.1.0-SNAPSHOT` (its dependency is HELD) |
| ports/parallel-bulk | HELD -> WIRED 2026-11-07 (un-hold section) | run 1: Ant `<copy>` from `../../../src/java.base/.../jdk/internal/vm/parallel` (outside the `m3/` sparse checkout, environment); re-gate after adding that path: `maven-compiler-plugin:3.15.0:testCompile` failed: `Cannot invoke "java.util.Map.get(Object)" because the return value of "AbstractCompilerMojo.getPathElements()" is null` |
| runtime-integration/fix | HELD | run 1: `FileNotFoundError src/hotspot/share/prims/jni.cpp` (sparse checkout, environment); re-gate: `FileNotFoundError src/java.base/share/classes/jdk/internal/mindex/MIndexStringBacking.java` (file absent from master) + `test_patch_and_dependency_identity` sha256 mismatch |
| runtime-integration/fix-root-ascii-case | HELD | run 1: `FileNotFoundError src/java.base/share/classes/java/lang/String.java` (sparse checkout); re-gate: `ValueError: verification drift: test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java`, `'storage != null && locale.equals(Locale.ROOT)' not found` in String.java, identity sha256 mismatch |
| collections/pcollections | HELD | could not resolve `org.pcollections:pcollections:5.0.0-m3-source-1` (not in the local repos nor Central) |

Wiring order followed the dependency edges found in the poms (`ports/precompute` before `ports/compiletime` and
`ports/indexstring`; `ports/parallel` before `ports/parallel-bulk` and `ports/vcpu`; `ports/swing-viewport` before
`ports/jfx-viewport`). No module has a `<parent>`; none needed a coordinate change in its own pom.

## Environment and deviations (NTL)

* JDK `C:/Program Files/Java/jdk-21` (21.0.9), Maven 3.9.14, `-Dmaven.repo.local=S:/m3-scratch/eb-conv/reactor/m2`
  with `-Dmaven.repo.local.tail=S:/m3-scratch/eb-conv/m2`, online; `-Djava.io.tmpdir` (MAVEN_OPTS and surefire
  `argLine`) and `TMP`/`TEMP`/`TMPDIR` on `S:/m3-scratch/eb-conv/reactor/tmp`.
* `python3`: 43 poms under `m3/` call `python3`; on this Windows host the only `python3.exe` on PATH is the Store
  alias stub (exit 9009). A venv `S:/m3-scratch/eb-conv/reactor/venv` (Python 3.12.6, `Scripts/python3.exe`) was
  put first on PATH for every Maven run. The first baseline run (without it) failed at `m3-file` with exit 9009 and
  is kept only as evidence of that.
* Baseline: two extra Maven runs on the untouched pom (`verify -Dmaven.test.failure.ignore=true`, the second with
  `-fae`); the default build of master fails at `m3-file` (`verify.py`: `RuntimeError: module contract inventory
  changed`), so `-fae` was needed to see every module. The full runs below use `-fae` for the same reason.
* Sparse checkout: `m3` (cone). Three gates were RED only because their scripts read repository-root paths
  (`src/java.base/...`, `src/hotspot/...`, `test/jdk/...`); those five directories were added to the sparse
  checkout and the three modules were taken out of the profile (`unhold.sh`, recorded `delete-block` edits) and
  re-gated once. The run-1 rows stay in `DECISION.tsv`/`GATES.tsv` as `HELD_RUN1_SPARSE_ARTIFACT` / `*.run1`.
  All three stayed HELD for module-own reasons.
* `collections/signal-codegen`: its first Maven run (BUILD SUCCESS) was not recorded because a recipe edit
  (`apply.py --op`) landed mid-gate; the gate was rerun and that second run is the recorded one.
* The `history` gate's python wrote temp fixtures through the inherited `TMP` (`K:\codex-work\agent-temp`) before
  `TMP`/`TEMP`/`TMPDIR` were pinned to S:; later gates did not.
* `migration`, `lexicon/hf-intake`, `ports/capability-runtime` and the two `runtime-integration/*-receiver` poms
  run their contracts only in `verify` (exec-maven-plugin); `package` is GREEN for them by construction, their
  `verify` results are in `FULL_TESTS.tsv`.

## Full-reactor runs (final pom, 15 WIRED modules in the default reactor)

| run | command | result |
|-----|---------|--------|
| full-package | `mvn -B -ntp -T1 -f m3/pom.xml -DskipTests package` | BUILD FAILURE at `m3-file` (pre-existing: `verify.py` `RuntimeError: module contract inventory changed`, same as the untouched baseline); fail-fast skipped everything after it |
| full-package-fae | the same with `-fae` | 39 modules SUCCESS, all 15 WIRED modules among them; FAILURE only in previously-in-reactor modules with the same baseline reasons (`m3-file`, `tooling/migration-recipes`, `tooling/m3jdk-a3-recipes`, `tooling/module-pack-recipes`: `org.openrewrite:rewrite-*:8.17.1-m3-source-1` unresolvable; `backports`: exec exit 1); SKIPPED `fs`, `tooling/a3`, `tooling/backport-dag` (depend on those) |
| full-verify | `mvn -B -ntp -T1 -fae -f m3/pom.xml verify -Dmaven.test.failure.ignore=true` | 3238 tests run, 2 failures, 2 errors, 162 skipped (baseline on the untouched pom: 2911 / 2 / 2 / 160). `FULL_FAILING.tsv` = `BASELINE_FAILING.tsv` (9 rows, byte-identical) + 3 rows from newly wired modules (below). No previously-in-reactor module changed its result. |

Newly wired modules' own `verify`-phase results (their `package` gate was GREEN; `verify` runs their exec contracts):

| module | verify result |
|--------|---------------|
| lexicon/hf-intake | FAILURE — `M3HfLexiconIntakeProof.verifyLanguageResourceDiscovery`: `AssertionError: language-resource row has nine fields: 280` (module-own contract) |
| runtime-integration/primitive-address-identity | FAILURE — `verify.py` compiles `src/jdk.compiler/share/classes/com/sun/tools/javac/m3` (repository root, outside the `m3/` sparse checkout): `javac error: file not found ... M3PrimitiveImageCompiler.java` (environment) |
| runtime-integration/regex-precompute | FAILURE — `verify.py` path contains the unset property `${m3.checkout}` (the recipe expects `-Dm3.checkout=<com.synexia checkout>`; external checkout, not part of the standalone tree) |
| the other 12 | SUCCESS; test counts in `FULL_TESTS.tsv` (eclipse/core-runtime 37, eclipse/framework 25, ports/precompute 120 (2 skipped), ports/parallel 31, ports/vcpu 34, ports/regex-cursor 54, collections/interop 26; migration, ports/indexstring, collections/signal-codegen 0 tests; lexicon/hf-intake, ports/capability-runtime, collections/gc-interop, the two receivers: no surefire) |

`jdk_naming.py plan --only m3/pom.xml --only m3/qualification/reactor-wiring-20261107`: moves=0 rewrites=0 refusals=0
(fixed point). Maven logs: `S:/m3-scratch/eb-conv/reactor/logs/` (sha256 of each gate log in `GATES.tsv`).

## Un-hold 2026-11-07 (reactor-unhold-20261107; branch `m3/reactor-unhold-20261107a`)

Build-configuration fixes only (poms, one Python test fixture); no production source touched. Every fix is a
hash-pinned `EDITS.tsv` edit with a `//FLLDCIM: MRL` anchor; every module went through `unhold.sh` then one `gate.sh`
run (`-pl <module> -am -DskipTests=false package`, module's own `-Werror`, JDK 21). Base: master `7b155a401e8` (#2781).

| module | decision | fix (EDITS.tsv) | gate | own tests |
|---|---|---|---|---|
| ast | WIRED | `maven.compiler.source/target` 21 instead of `release` (javac refuses an add-exports of a java.base package together with `--release`); javac `--add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED` (the only `jdk.internal.org.objectweb.asm.*` package the test imports); surefire `argLine` with the same export. No m3 module had a JDK-internal-ASM precedent (`grep add-exports m3/**/pom.xml`: only vendored guava/eclipse-collections `-J` flags); the form is ports/parallel-bulk's. | gate-ast run 2, 28 s (run 1: non-parseable POM, `--` inside an XML comment of the fix; reworded) | 19 / 0 / 0 / 0 |
| ports/swing-viewport | WIRED | test dependency `org.junit.platform:junit-platform-launcher:1.10.2-m3-source-1` (the platform version `junit-jupiter:5.10.2-m3-source-1` already brings; jar with classes in the tail repo) | gate-ports-swing-viewport, 17 s | 375 / 0 / 0 / 0 |
| ports/jfx-viewport | WIRED | none (its only error was the HELD dependency) | gate-ports-jfx-viewport, 13 s | no surefire |
| ports/parallel-bulk | WIRED | maven-compiler-plugin 3.13.0 (every other m3 module; m3/pom.xml has no pluginManagement) did NOT remove the testCompile NPE (`AbstractCompilerMojo.execute:1152`, `getPathElements()` null for a test compile without a module descriptor when `--patch-module`/`--add-exports` are given as two tokens: the plugin's `META-INF/jpms.args` post-processing); fix: one-token `--patch-module=java.base=...` and `--add-exports=java.base/jdk.internal.vm.parallel=ALL-UNNAMED` javac args (javac accepts `option=value`). The Ant `<copy>` source path of #2779 is kept (`src/java.base/share/classes/jdk/internal/vm/parallel` is in the sparse set). | gate-ports-parallel-bulk run 2, 5 s (run 1: 3.13.0 pin alone, same NPE) | 6 / 0 / 0 / 0 |
| history | WIRED | test-harness fix in `test_audit.py` setUp: on `os.name == 'nt'` the odd fixture path is `spaces tab and newlines-λ.java` (NTFS refuses tab/newline: `OSError 22`); POSIX fixture unchanged; `test_odd_paths_binary_patches_and_hashes` still asserts the JSON path round trip. Not a contract change. | gate-history, 44 s (unittest: 78 tests OK; was 22 errors) | python unittest 78 OK |
| ports/compiletime | HELD (re-confirmed, no Maven run) | `org.ow2.asm:asm{,-tree,-analysis}:9.6-m3-source-1`: only `.lastUpdated` markers in both local repos, not in Central | reconfirm-ports-compiletime | - |
| collections/pcollections | HELD (re-confirmed) | `org.pcollections:pcollections:5.0.0-m3-source-1`: only `.lastUpdated` markers (tail repo has 3.2.0, 4.0.2) | reconfirm-collections-pcollections | - |
| runtime-integration/fix | HELD (re-confirmed) | `manifest.json`/`runtime.patch` pin `src/hotspot/share/prims/jni.cpp` (moved java.base/hotspot input outside `m3/`: lane d7) | reconfirm-runtime-integration-fix | - |
| runtime-integration/fix-root-ascii-case | HELD (re-confirmed) | `test_fix.py:108` pins `src/java.base/share/classes/java/lang/String.java` (lane d7 input) | reconfirm-runtime-integration-fix-root-ascii-case | - |

Full-reactor runs (`mvn -B -ntp -T1 -fae -f m3/pom.xml verify -Dmaven.test.failure.ignore=true`, both on master `7b155a401e8`):

| run | pom | result |
|---|---|---|
| baseline-post2781-verify | untouched (#2779 wiring as merged) | 3243 tests, 2 failures, 2 errors, 162 skipped; `BASELINE_POST2781_FAILING.tsv` = 12 rows, byte-identical to `FULL_FAILING.tsv` (#2781 changed no module result) |
| full-verify-unhold | this branch | 3643 tests, 2 failures, 2 errors, 162 skipped; `FULL_UNHOLD_FAILING.tsv` byte-identical to `BASELINE_POST2781_FAILING.tsv`; the only per-module differences are the 5 added SUCCESS rows (ast 19, swing-viewport 375, parallel-bulk 6, jfx-viewport and history-audit-recipe: no surefire; history's `verify`-phase `audit.py` exec SUCCESS) |

Tool fixes in this directory: `gate.sh` records the log path it actually wrote (`cygpath -m "$LOG"`) and strips any `wt-*`
worktree prefix from the first-error file; `reconfirm.sh` adds a DECISION.tsv row for a HELD module from CPU evidence
without a Maven run. After its gate the ast pom received one comment-only edit (`M3JDK21` -> `M3JDK 21`, jdk_naming
`PROSE_PRODUCT_VERSION`); `jdk_naming.py plan --only <touched files>` ends at `moves=0 rewrites=0 refusals=0`.

## Vendor stage 0 2026-11-07 (reactor-unhold-20261107b; branch `m3/reactor-unhold-20261107b`)

The two `-m3-source-1` HELD modules depended on artifacts that exist in this repository only as vendored sources
(`m3/vendor/third_party/`; standalone law: no Central artifact, no sibling-repo jar). `stage0.sh` installs them from
those sources into the head local repo with ONE Maven run each and records a `stage0-<slug>` GATES.tsv row; then the
usual `unhold.sh` + `gate.sh` pair gated the module. Base: master `a4c85e8b397` (#2783). No module source touched.

| module | stage 0 | gate | own tests |
|---|---|---|---|
| ports/compiletime | `stage0-asm-9.6`: `-f m3/vendor/third_party/pom.xml -pl asm-9.6,asm-tree,asm-analysis,asm-util install -DskipTests`, 13 s, `org.ow2.asm:asm{,-tree,-analysis,-util}:9.6-m3-source-1` installed | WIRED (gate-ports-compiletime, 27 s) | 30/0/0/0 |
| collections/pcollections (a4's module; its HELD row was a4's gate) | `stage0-pcollections`: `-f m3/vendor/third_party/pcollections/pom.xml install -DskipTests`, 9 s, `org.pcollections:pcollections:5.0.0-m3-source-1` installed. That pom is NOT a `<module>` of the vendor aggregator although the aggregator README says "Build all" installs every vendored artifact (debt, vendor lane) | WIRED (gate-collections-pcollections, 89 s) | 4/0/0/0 |

Full-reactor runs (`mvn -B -ntp -T1 -fae -f m3/pom.xml verify -Dmaven.test.failure.ignore=true`), compared with #2783's
`FULL_UNHOLD_FAILING.tsv` (its `full-verify-unhold` run on master `7b155a401e8` + that branch):

| run | result |
|---|---|
| full-verify-vendorhold (run 1) | 3677 tests, 2 failures, 2 errors, 162 skipped; failing set = #2783's + ONE row: `history-audit-recipe` exec `read-only-history-recipe` refused (`AUDIT_REFUSED: output must not already exist`): `m3/history/pom.xml` wrote the audit to the fixed name `${java.io.tmpdir}/m3-history-audit`, and the shared Maven tmpdir (`S:/m3-scratch/eb-conv/reactor/tmp`) still held #2783's output. Environment-order defect of the module, not of this branch; kept as evidence (`FULL_VENDORHOLD_*.tsv`) |
| regate-history-verify | after the recorded fix (`m3.output` = `${java.io.tmpdir}/m3-history-audit-${maven.build.timestamp}`, format `yyyyMMdd-HHmmss-SSS`, one directory per build session): BUILD SUCCESS, 290 s (`regate.sh history lane37 verify`) |
| full-verify-vendorhold2 (run 2) | 3677 tests, 2 failures, 2 errors, 162 skipped; `FULL_VENDORHOLD2_FAILING.tsv` byte-identical to `FULL_UNHOLD_FAILING.tsv`; per-module diff of `FULL_VENDORHOLD2_TESTS.tsv` against `FULL_UNHOLD_TESTS.tsv` = only the two added SUCCESS rows (m3-compiletime-precompute 30, m3-pcollections 4) |

Tools added here: `stage0.sh` (vendor install + GATES row; `f:<pom>` for a vendor pom the aggregator does not list),
`fullrun.sh` (one GATES row + `<TAG>_TESTS.tsv`/`<TAG>_FAILING.tsv` from an existing full-run log through `testsum.py`;
wall time from the log's START/END stamps), `regate.sh` (one Maven run for an already-wired module after a recorded
fix; `REGATED_GREEN`/`REGATED_RED` DECISION.tsv row). Standing prerequisite the default reactor already had and this
section makes explicit: the vendored artifacts a module pins (`junit-jupiter 5.10.2-m3-source-1`, `slf4j-api
2.0.12-m3-source-1`, now `asm 9.6-m3-source-1` and `pcollections 5.0.0-m3-source-1`) must be installed once per local
repo from `m3/vendor/third_party` (`mvn -B -ntp -f m3/vendor/third_party/pom.xml install`, plus the pcollections pom
until the aggregator lists it); aggregating the ~110 vendor modules into every reactor run was not done here.
Remaining in `m3-offreactor-held`: `runtime-integration/fix`, `runtime-integration/fix-root-ascii-case` (lane d7).
