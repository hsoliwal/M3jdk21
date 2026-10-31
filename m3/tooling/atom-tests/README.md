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
| `gen_atom_tests.py` | Generator (ATOM-TEST GENERATOR v1). Spec rows plus templates produce test classes and `TEST_PATTERNS.tsv`. It refuses duplicate rows, unknown patterns, missing arguments, assertion-free bodies and helper/atom name clashes. |
| `templates/*.tmpl` | Contract patterns: `UTILITY_CTOR`, `PURE_DIFF`, `NATIVE_LANE`, `THROWS`, `LANGUAGE_DIFF`, `ORACLE_CALL`, `SCENARIO`, `PERF`. Class templates: `CLASS` (JUnit) and `PERF_CLASS` (perf harness). |
| `specs/<group>/*.atoms` | One spec file per owner source file. `M3MindexAtomPerf.atoms` is the perf harness of the hot atoms. |
| `run_atom_tests.sh` | Runs the tests without a JDK build. It compiles the package against a stock JDK 21 with `--patch-module java.base` (`-implicit:none`, with the M3 java.base sources on the patch path for M3-only API), then runs JUnit through the console launcher. |
| `perf_jit_tiers.py` | Joins the harness rows with the `-XX:+PrintCompilation` log: the highest tier reached, OSR and deoptimisation per atom. |
| `atom_census_join.py` | Joins PER-ATOM TEST CENSUS v1 before/after with `TEST_PATTERNS.tsv` and writes `ATOMIZE.tsv` and `CENSUS_BEFORE_AFTER.tsv`. |
| `junit_summary.py` | Projects the JUnit XML report to one row per class. |
| `mutate_once.py` | Applies one production mutant, which is then restored with `git checkout`. Used to show that the tests discriminate. |
| `finalize_run.sh` | Runs all of the above and writes `GATES.tsv` and the evidence TSVs into `runs/<group>-<date>/`. |

The test library lives next to the generated classes in `test/jdk/jdk/internal/<group>/atoms`:

- `M3AtomKit` reaches package-private atoms through `privateLookupIn`, so the tests declare `@modules ...:+open`. It uses seeded inputs and holds the utility-class and native-lane contracts.
- `M3AtomPerf` checks a checksum on every batch, warms up, runs interleaved rounds and samples CPU load.
- The oracles are `RelationOracle`, `DotStarOracle`, `AutomatonOracle` and `TokenRegexOracle`. They are reference models over java.util collections and java.util.regex.

Run, from a worktree with the sparse checkout of the package and its tests:

```
bash m3/tooling/atom-tests/finalize_run.sh <worktree> <out_dir> <worktree>/m3/tooling/atom-tests/runs/mindex-<date> \
  <census_root holding per_atom_test_census.py and m3jdk/> <reference test root> mindex jdk.internal.mindex
```
