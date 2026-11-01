<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# Per-atom tests: generator, runner and run evidence

The recipe for "a JUnit test and a perf test for every atom". Each atom (method or constructor) of an M3
package is one row of a spec file. The row names the atom's contract pattern and the inputs and oracle of
that pattern. The generator turns the row into one test method; each owner source file becomes one
jtreg-compatible JUnit class, `<Owner>AtomTest`, whose `@TestFactory` publishes one `DynamicTest` per atom.
When a generated test is wrong, fix the spec or the template and record the correction in
`RECIPE_MEMORY.tsv`. Never hand-edit a generated file.

| file | role |
|---|---|
| `gen_atom_tests.py` | Generator (ATOM-TEST GENERATOR v2). Spec rows plus templates produce test classes and `TEST_PATTERNS.tsv`. Every refusal of a run is listed and nothing is written: duplicate rows, unknown patterns, missing arguments, helper/atom name clashes, `#const` hygiene, the ASSERTION GATE v2 and the LITERAL GATE v1 (below). |
| `test_gen_atom_tests.py` | Unit tests of the generator gates and of the shape extraction (`python -m unittest test_gen_atom_tests`, in memory). |
| `templates/*.tmpl` | Contract patterns: `UTILITY_CTOR`, `PURE_DIFF`, `NATIVE_LANE`, `THROWS`, `LANGUAGE_DIFF`, `ORACLE_CALL`, `SCENARIO`, `PERF`, and the seven shapes extracted from SCENARIO rows: `MEMBER_VIEW_DIFF`, `GETTER_DIFF`, `CLASS_VIEW_DIFF`, `CI_ROW_DIFF`, `CI_MEMBER_DIFF`, `MEMBER_ROW_DIFF`, `MEMBER_ANNOTATION_NAMES`. Class templates: `CLASS` (JUnit) and `PERF_CLASS` (perf harness). Leading `## ` lines are metadata, not rendered: `## optional <arg>...` names arguments that default to empty. |
| `specs/<group>/*.atoms` | One spec file per owner source file. `M3MindexAtomPerf.atoms` is the perf harness of the hot atoms. |
| `scenario_shapes.py` | SCENARIO SHAPES v1. `census` lists the shapes three or more SCENARIO rows share; `extract` converts the rows of a shape into its template, keeping each rendered body byte-identical. |
| `render_equivalence.py` | Renders two spec sets through the generator and compares every generated test method (pattern name in the label masked): the proof of a spec refactoring. |
| `method_bytecode.py` | Per-method `javap -c -p` comparison of two compiled test trees (constant-pool indexes removed): which methods kept their instructions when literals became constants. |
| `run_atom_tests.sh` | Runs the tests without a JDK build. It compiles the package against a stock JDK 21 with `--patch-module java.base` (`-implicit:none`, with the M3 java.base sources on the patch path for M3-only API), then runs JUnit through the console launcher. |
| `perf_jit_tiers.py` | Joins the harness rows with the `-XX:+PrintCompilation` log: the highest tier reached, OSR and deoptimisation per atom. |
| `perf_variants.sh` | Times alternative forms of one owner file with the perf harness, interleaved, each in its own patch; ns/op, bytes/op and JIT tier per run. |
| `mutation_run.sh` | Applies every mutant of `runs/<group>-<date>/MUTANTS.tsv` once (with `mutate_once.py`), runs its test class, restores the owner and writes `MUTATION.tsv`. |
| `atom_census_join.py` | Joins PER-ATOM TEST CENSUS v1 before/after with `TEST_PATTERNS.tsv` and writes `ATOMIZE.tsv` and `CENSUS_BEFORE_AFTER.tsv`. |
| `junit_summary.py` | Projects the JUnit XML report to one row per class. |
| `mutate_once.py` | Applies one production mutant, which is then restored with `git checkout`. |
| `finalize_run.sh` | Runs all of the above and writes `GATES.tsv` and the evidence TSVs into `runs/<group>-<date>/`. The counts a report quotes (census tree files, RECIPE_MEMORY rows, C2 rows of the run) are written into `GATES.tsv` by the script. |

The test library lives next to the generated classes in `test/jdk/jdk/internal/<group>/atoms`:

- `M3AtomKit` reaches package-private atoms through `privateLookupIn`, so the tests declare `@modules ...:+open`. It uses seeded inputs and holds the utility-class and native-lane contracts.
- `M3AtomPerf` checks a checksum on every batch, warms up, runs interleaved rounds, samples CPU load and counts the bytes the timing thread allocates per op (`bytes_per_op`). Arguments select the cases whose label starts with one of them.
- The oracles are `RelationOracle`, `DotStarOracle`, `AutomatonOracle`, `TokenRegexOracle` and `ReflectOracle`. They are reference models over java.util collections, java.util.regex and java.lang.reflect.

## Generator gates (v2)

- **ASSERTION GATE v2.** A rendered body must call a JUnit assertion (`assert*`/`fail` of
  `org.junit.jupiter.api.Assertions`) or an oracle comparison helper: an `assert*`/`expect*`/`check*`-named method
  of the test library or of the spec's `#helper` block whose body, with its catch blocks removed, calls a JUnit
  assertion, throws `AssertionError` or calls another such helper (fixed point). v1 accepted any `XxxOracle.method`
  or `check*(` call; lookup helpers (`findStatic` & co., whose only `AssertionError` is in a catch block) and
  unverified oracle calls no longer count. On the mindex specs every one of the 402 rows passes v2; v1 accepted
  no row that v2 refuses.
- **LITERAL GATE v1.** No numeric literal outside a named constant in a rendered body, a `#helper` block or the test
  library, with the dispositions of LITERAL_CONSTANTS v1.1 (hashing lane): EXEMPT_UNIT (0 and 1, unary minus
  included), CONSTANT_DEF and TABLE_DATA inside a `static final` field declaration, ANNOTATION_ARG. A spec names a
  constant with `#const <type> <NAME> = <initializer> // <role>`; the generator emits it as a documented
  `private static final`, refuses it when it is not UPPER_CASE, declared twice or never read. A scalar constant is
  folded back by javac, so the test method keeps its instructions (`METHOD_BYTECODE.tsv`).

## Run facts (runs/mindex-20261031)

| fact | value |
|---|---|
| JUnit rows (atoms) / perf rows | 385 / 17 (15 before; `M3TokenEngine.alternative#1` and `#2` added for finding e) |
| SCENARIO rows | 363 before, 289 after; 74 rows in 7 shape templates (`SCENARIO_SHAPES.tsv`: MEMBER_VIEW_DIFF 21, GETTER_DIFF 15, CLASS_VIEW_DIFF 14, CI_ROW_DIFF 11, CI_MEMBER_DIFF 5, MEMBER_ROW_DIFF 4, MEMBER_ANNOTATION_NAMES 4); the census after the extraction finds no shape of three or more rows; the 400 rows render byte-identical test methods before and after (`RENDER_EQUIVALENCE.tsv`) |
| numeric literals outside named constants (verifier scan, 29 test files) | 455 in 17 files before, 0 after; LITERAL GATE v1 (local finals not exempt) 472 before, 0 after |
| atom test methods with unchanged bytecode | 362 of 400 (`METHOD_BYTECODE.tsv`); the 38 that change are the rows whose vectors became named tables (cloned where the atom keeps or writes them) or derived expressions |
| `M3TokenEngine.alternative` | the clone of the 0e352f226 fix allocated 32 bytes per call (alternative#1: 141696 B/op against 138240 before the fix; alternative#2: 216728 against 212408); the save/restore lane keeps the pre-fix allocation, its time is within the host noise, and the pre-fix form fails alternative#2 (`PERF_VARIANTS_E1.tsv`, `PERF_VARIANTS_E2.tsv`) |
| `M3TokenRegexTest.java:171` | the test was wrong; its expectation follows the predicate contract (decision D1, `CONTRACT_DECISIONS.tsv`) |
| census tree of the PATTERNIZE gate | 586 test sources: the 557 of the reference test tree (master 52fb81c483c7) plus the 29 overlaid atom files (an earlier report said 576) |
| RECIPE_MEMORY.tsv | 21 correction rows at 1594d1e8d (an earlier report said 22); the rows of this round follow them |
| C2 tier of the perf rows | a property of one run, not of the atoms: 12 of 15 rows in the run of 2026-10-31, 11 of 15 in the verification run (M3Bits.bitMask stayed at C1); the run of each commit is in `GATES.tsv` (JIT_PERF evidence `c2=`) and `PERF.tsv` |

Run, from a worktree with the sparse checkout of the package and its tests:

```
bash m3/tooling/atom-tests/finalize_run.sh <worktree> <out_dir> <worktree>/m3/tooling/atom-tests/runs/mindex-<date> \
  <census_root holding per_atom_test_census.py and m3jdk/> <reference test root> mindex jdk.internal.mindex
bash m3/tooling/atom-tests/mutation_run.sh <worktree> <out_dir> <run_dir>/MUTANTS.tsv <run_dir>/MUTATION.tsv
python m3/tooling/atom-tests/scenario_shapes.py census m3/tooling/atom-tests/specs/mindex m3/tooling/atom-tests/templates <out.tsv>
```

The shape proof replays from the spec set before the extraction (the specs at 1594d1e8d): `scenario_shapes.py extract
<copy> templates MEMBER_ANNOTATION_NAMES MEMBER_VIEW_DIFF CLASS_VIEW_DIFF CI_ROW_DIFF MEMBER_ROW_DIFF CI_MEMBER_DIFF
GETTER_DIFF` (whole-body shape first, the receiver-generic GETTER_DIFF last), then
`render_equivalence.py <specs before> <copy> templates <out.tsv>`.
