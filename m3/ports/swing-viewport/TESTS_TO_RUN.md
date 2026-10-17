<!-- SPDX-License-Identifier: Apache-2.0 -->
# Tests to run: m3/ports/swing-viewport

Status per row: RUN (with result), NOT_RUN, or PENDING_LINUX. Memory gate before every JVM: commit headroom at least
8 GB, one JVM at a time, `-Xmx2g` or less (the sized tree run uses `-Xmx256m`).

| # | What | Command | Status |
|---|---|---|---|
| 1 | Residency rename recipe (#826) applied, then re-run for the fixed point | `java ResidencyRename.java RENAME_MANIFEST.tsv <swt@136df9c6> <M3jdk21 root>` | RUN: `CHANGED 21 OF 21`, then `CHANGED 0 OF 21` |
| 2 | Engine compile, `javac --release 21 -Xlint:all` | `run-tests.sh compile` | RUN: clean |
| 3 | List and table differential tests (`M3PackedListModelDiffTest`: 40 seeds x 600 ops; `M3PackedTableModelDiffTest`: 30 seeds x 400 ops; batch coalescing; kind promotion) | `run-tests.sh test` | RUN 2026-10-13: 6/6 pass (JDK 21.0.9, JUnit console 1.11.4) |
| 4 | Engine files against #826 `APPLIED_SHA256.txt` (head 3fa05fc1e496) | `sha256sum -c APPLIED_SHA256.txt` | RUN 2026-10-13: 21/21 OK (identical to the residency agent's applied tree) |
| 5 | Tree differential (`M3PackedTreeModelDiffTest`: 30 seeds x 500 ops, plus asks-allows-children 30 x 300; stale proxies; removal event) | `run-tests.sh test` | RUN 2026-10-13: pass |
| 6 | Selection differential (`M3TreeSelectionModelDiffTest`: 25 seeds x 300 ops in each of DISCONTIGUOUS, CONTIGUOUS, SINGLE; failures compared by class; bulk range and pattern counts) | `run-tests.sh test` | RUN 2026-10-13: pass |
| 7 | Virtual tree (`M3VirtualTreeTest`): structure vs materialized DefaultTreeModel; rows vs VariableHeightLayoutCache over 200 expand/collapse steps; TreePath/proxy reuse; headless paint creates 0 proxies, 0 TreePaths, 0 fills, `materializedDuringPaint` 0 | `run-tests.sh test` | RUN 2026-10-13: pass (17/17 tests in the module) |
| 8 | Headless scale matrix (`scale-matrix.sh` -> `evidence/SCALE_RESULTS_20261013.tsv`): JTable and JList at 1M and 10M (virtual at `-Xmx256m`, packed and default at `-Xmx2g`), JList virtual at 100M, JTree virtual at 1,001,000 and 1,001,001,000 nodes at `-Xmx256m`, default baselines at 1M | `bash scale-matrix.sh` | RUN 2026-10-13: 14/14 rows; materialized per paint 0 for every M3 mode (default: 114, 34, 71) |
| 9 | Leaf 2 `M3RedrawSuspendTest` (headless) on stock JDK 21.0.9 with patched `RepaintManager` + `JComponent` (`--patch-module java.desktop`) | see `jdk-reproducers/` | RUN 2026-10-13: PASS patched; stock fails with `NoSuchFieldException: m3Deferred` (the test needs the change) |
| 10 | Leaf 2 `M3RepaintSkipHiddenTest` (headful, desktop lock), stock vs patched | `C:/m3-work-c/wt/swing-headful.sh` | NOT_RUN yet (desktop lock held by another agent) |
| 11 | jtreg runs of both reproducers on a JDK built from this branch | `jtreg -jdk:<built> m3/ports/swing-viewport/jdk-reproducers` | PENDING_LINUX (no JDK build or jtreg here) |
| 12 | Swing viewport leaf 1: `M3TreeLayoutCacheDifferential` (stock `VariableHeightLayoutCache` vs `sun.swing.tree.M3VariableHeightLayoutCache`, seeded scripts, every answer, selection events, exception classes, node-dimension call traces) on stock JDK 21.0.9 with `--patch-module java.desktop=<classes> --add-exports java.desktop/sun.swing.tree=ALL-UNNAMED` | `java ... M3TreeLayoutCacheDifferential 500 400 1000` | RUN 2026-10-17: 500/500 seeds pass; D1 accepted 126x, rebuilds after D2 exceptions 55x (see `m3/docs/swing-viewport/UPSTREAM_DEFECTS.tsv`) |
| 13 | Swing viewport leaf 1: `M3JTreeViewportDifferential` (two JTrees on one model, Metal UI with stock vs M3 cache; public API, events, pixels) | `java ... M3JTreeViewportDifferential 120 250 100` | RUN 2026-10-17: 120/120 seeds pass, 3,120 pixel comparisons equal; D1 accepted 22x |
| 14 | jtreg runs of tests 12 and 13 on a JDK built from this branch | `jtreg -jdk:<built> m3/ports/swing-viewport/jdk-reproducers` | PENDING_CI (no OpenJDK build or jtreg here) |
