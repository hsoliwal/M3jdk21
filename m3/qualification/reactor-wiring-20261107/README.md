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
| `FULL_TESTS.tsv`, `FULL_FAILING.tsv` | the same for the wired reactor (final run) |

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
| ast | HELD | `src/test/java/jdk/m3/ast/M3SyntaxGrowthAgent.java:9` package `jdk.internal.org.objectweb.asm` is not visible (testCompile) |
| history | HELD | exec `history-contract-tests` (python3) exit 1: `OSError [Errno 22]` writing a fixture named `spaces tab<TAB>and<LF>newlines-λ.java` (Windows filename) |
| ports/compiletime | HELD | could not resolve `org.ow2.asm:asm{,-tree,-analysis}:9.6-m3-source-1` (not in the local repos nor Central) |
| ports/swing-viewport | HELD | `src/test/java/jdk/m3/swing/testkit/M3JUnitLauncher.java:25` package `org.junit.platform.launcher` does not exist |
| ports/jfx-viewport | HELD | could not resolve `com.m3:m3-swing-viewport:0.1.0-SNAPSHOT` (its dependency is HELD) |
| ports/parallel-bulk | HELD | run 1: Ant `<copy>` from `../../../src/java.base/.../jdk/internal/vm/parallel` (outside the `m3/` sparse checkout, environment); re-gate after adding that path: `maven-compiler-plugin:3.15.0:testCompile` failed: `Cannot invoke "java.util.Map.get(Object)" because the return value of "AbstractCompilerMojo.getPathElements()" is null` |
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
