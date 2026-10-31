<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# viewport-swing: one JUnit test and one perf measurement per atom of the Swing viewport

Scope: the 281 method and constructor atoms of `sun.swing.list`, `sun.swing.table` and `sun.swing.tree` in
`java.desktop` (`M3ListMeasure`, `M3ListRows`, `M3PrefixRowOffsets`, `M3RowOffsets`, `M3RowPieces`, `M3SorterLanes`,
`M3SparseSizes`, `M3ExpandedState`, `M3FixedHeightLayoutCache`, `M3VariableHeightLayoutCache`; design:
`m3/docs/swing-viewport/DESIGN.md`). Work list: the per-atom test census of com.synexia PR #14999 (259 untested,
20 class-only, 2 tested).

Every file under `test/jdk/sun/swing/{list,table,tree}` is generated; change the generators or their templates,
never the output.

| Stage | Tool | Output |
|---|---|---|
| 1 ATOMIZE | `tools/atomize_viewport.py <census> <src> ATOMS.tsv` | `ATOMS.tsv`: atom, signature, visibility, pattern (VIEWPORT_ATOM_PATTERNS v1) |
| 2 JUNIT | `tools/gen_atom_tests.py ATOMS.tsv tools/templates <test/jdk/sun/swing> ATOM_SPEC.tsv` | one `<File>AtomsTest` per source file, one dynamic test per atom; the pattern harnesses (`tools/templates/<pkg>`) copied beside them |
| run | `tools/run_atom_tests.sh <worktree> <out>` | stock JDK 21 + `--patch-module java.desktop=<M3 classes>`, JUnit console, one JVM per `@run` flag set |
| REACH | `tools/run_reach.sh <worktree> <out> REACH.tsv` | JaCoCo per dynamic test: each atom's own method executed in its own test |
| MUTATION | `tools/mutate.py <worktree> <out> MUTANTS.tsv MUTATION.tsv` | planted behaviour bugs, each must fail at least one dynamic test |
| 3 JIT/perf | `tools/gen_atom_perf.py`, `tools/run_atom_perf.sh`, `tools/perf_report.py` | `M3{List,Table,Tree}AtomsPerf` (MethodHandle.invokeExact per hot atom, warm-up, interleaved rounds, machine load, `-XX:+PrintCompilation` tier); `PERF.tsv` |
| 4 PATTERNIZE | `tools/patternize.py` | `TEST_PATTERNS.tsv` (atom, pattern, test, oracle, route, perf); census before/after |

Oracles are JDK types wherever an M3 type mirrors one: `javax.swing.SizeSequence`, `javax.swing.table.TableRowSorter`,
`javax.swing.tree.FixedHeightLayoutCache` / `VariableHeightLayoutCache`, `java.util.Hashtable` with the stock `JTree`
scans, the stock `BasicListUI` loops (through a `BasicListUI` subclass of the unnamed module), driven by
`DefaultListModel`, `DefaultTableModel` and `DefaultTreeModel` events. Private atoms are also checked white-box through
`MethodHandles.privateLookupIn` against their inline contracts. Seeds derive from the atom name; there are no magic
numbers in the generators or the tests (named constants).

The tests are jtreg-compatible (`@test`, `@modules ...:open`, `@run junit/othervm`); the perf harnesses run under jtreg
as one-round smoke tests (`-Dperf.rounds=1`). Gates and their results: `GATES.tsv`; corrections of the generators:
`RECIPE_MEMORY.tsv`; run logs, perf logs and the filtered `-XX:+PrintCompilation` lines: `evidence/`.
