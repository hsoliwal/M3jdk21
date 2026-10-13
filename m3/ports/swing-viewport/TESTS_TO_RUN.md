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
