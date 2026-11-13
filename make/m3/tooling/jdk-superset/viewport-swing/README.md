<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# viewport-swing: per-atom JUnit and scoped performance evidence

Scope: the 281 method and constructor atoms of `sun.swing.list`, `sun.swing.table` and `sun.swing.tree` in
`java.desktop` (`M3ListMeasure`, `M3ListRows`, `M3PrefixRowOffsets`, `M3RowOffsets`, `M3RowPieces`, `M3SorterLanes`,
`M3SparseSizes`, `M3ExpandedState`, `M3FixedHeightLayoutCache`, `M3VariableHeightLayoutCache`; design:
`doc/m3/swing-viewport/DESIGN.md`). Work list: the per-atom test census of com.synexia PR #14999 (259 untested,
20 class-only, 2 tested).

Every file under `test/jdk/sun/swing/{list,table,tree}` is generated; change the generators or their templates,
never the output.

| Stage | Tool | Output |
|---|---|---|
| 1 ATOMIZE | `tools/atomize_viewport.py <census> <src> ATOMS.tsv` | `ATOMS.tsv`: atom, signature, visibility, pattern (VIEWPORT_ATOM_PATTERNS v1) |
| 2 JUNIT | `tools/gen_atom_tests.py ATOMS.tsv tools/templates <test/jdk/sun/swing> ATOM_SPEC.tsv` | one `<File>AtomsTest` per source file, one dynamic test per atom; the pattern harnesses (`tools/templates/<pkg>`) copied beside them |
| run | `tools/run_atom_tests.sh <worktree> <out>` | stock JDK 21 + `--patch-module java.desktop=<M3 classes>`, JUnit console, one JVM per `@run` flag set; `SUMMARY atom_tests=<pass>/<run> generator_checks=<pass>/<run>` counts the per-atom tests and the `oneTestPerAtom` generator checks apart |
| REACH | `tools/run_reach.sh <worktree> <out> REACH.tsv` | JaCoCo per dynamic test: each atom's own method executed in its own test |
| MUTATION | `tools/mutate.py <worktree> <out> MUTANTS.tsv MUTATION.tsv` | planted behaviour bugs, each must fail at least one dynamic test |
| 3 JIT/perf | `tools/gen_atom_perf.py`, `tools/run_atom_perf.sh`, `tools/perf_report.py` | `M3{List,Table,Tree}AtomsPerf` (MethodHandle.invokeExact per hot atom, warm-up, interleaved rounds, machine load, `-XX:+PrintCompilation` tier); `PERF.tsv` |
| 4 PATTERNIZE | `tools/patternize.py` | `TEST_PATTERNS.tsv` (atom, pattern, test, oracle, route, perf); census before/after |

Oracles are JDK types wherever an M3 type mirrors one: `javax.swing.SizeSequence`, `javax.swing.table.TableRowSorter`,
`javax.swing.tree.FixedHeightLayoutCache` / `VariableHeightLayoutCache`, `java.util.Hashtable` with the stock `JTree`
scans, the stock `BasicListUI` loops (through a `BasicListUI` subclass of the unnamed module), driven by
`DefaultListModel`, `DefaultTableModel` and `DefaultTreeModel` events. Private atoms are also checked white-box through
`MethodHandles.privateLookupIn` against their inline contracts. Seeds derive from the atom name. Every number in the
tests is a named constant or an enum constant: `gen_atom_tests.py` and `gen_atom_perf.py` refuse to write a file with a
numeric literal outside one (`make/m3/tooling/atom-tests/literal_census.py --strict`; -1, 0, 1 and 2 are trivial).

Each generated class has one dynamic test per atom and one `oneTestPerAtom` check (`@Tag("generator-consistency")`).
The check tests the generator (one dynamic test per census atom, distinct atom ids), not an atom, so reports count the
281 atom tests and the 10 generator checks apart. The census of this PR together with PR #2249 is
`make/m3/tooling/atom-tests/CENSUS_UNION.tsv` (`census_union.py`).

The tests are jtreg-compatible (`@test`, `@modules ...:open`, `@run junit/othervm`); the perf harnesses run under jtreg
as one-round smoke tests (`-Dperf.rounds=1`). Gates and their results: `GATES.tsv`; corrections of the generators:
`RECIPE_MEMORY.tsv`; run logs, perf logs and the filtered `-XX:+PrintCompilation` lines: `evidence/`.

## Performance catalogue correction

The retained PERF.tsv has 80 rows: 59 atom rows and 21 references/composites. Eight atom rows explicitly say FOLDED and provide no measurement, leaving 51 nonfolded measured atoms out of 281. TEST_PATTERNS.tsv now marks those eight rows as unmeasured; its other273 rows and all route/oracle/test columns are unchanged. The atom catalogue has14 distinct patterns. Name-based perf PRESENT counts remain separate and unchanged.

GATES.tsv retains its historical rows and appends this CPU-only correction. The existing generator was exercised against frozen public input bytes through an in-memory file adapter; the failing regression was observed before the correction and nine regression checks pass afterward. Benchmarks, Java/JUnit, make and jtreg were not rerun. Canonical hash-pinned recipe and receipts: synexia-openrewrite-recipes/recipe-crates/m3-cpullm-history169-perf-metadata-20261104 in hsoliwal/com.synexia.
