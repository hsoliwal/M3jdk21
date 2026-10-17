<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors (Synexia) -->
# M3JDK Swing viewport: arithmetic layout caches for very large models

Status: leaf 1 implemented, opt-in. The JTree layout cache `sun.swing.tree.M3VariableHeightLayoutCache` answers every
public method exactly as the stock `javax.swing.tree.VariableHeightLayoutCache`, except for the two listed upstream
defects. Proof and numbers are below. `make` and jtreg have not run on this host (no OpenJDK build here); they are
PENDING_CI. Everything else in this page was run on stock JDK 21.0.9 with `--patch-module java.desktop=<classes>`.

The same viewport mediation that SWT uses (`hsoliwal/eclipse.platform.swt` PR #196, "viewport logic is king") is
applied here inside the JDK. The public contract of `JTree`, `TreeUI`, `AbstractLayoutCache`, `JList` and `JTable` does
not change. PR #828 (merged) already added the model side (packed and virtual models in the Apache-2.0 module
`m3/ports/swing-viewport`, and a `RepaintManager` leaf). This page covers the view side, which works for *any*
`TreeModel`: the layout cache that BasicTreeUI asks for rows, paths and bounds.

## 1. Switch, scope and licence

- `-Dm3.swing.viewport=true` makes `BasicTreeUI.createLayoutCache` return `M3VariableHeightLayoutCache` where it would
  return `VariableHeightLayoutCache` (the default `JTree` path: `largeModel` false, or a variable row height). Default is
  off in this leaf. A `BasicTreeUI` subclass that overrides `createLayoutCache` keeps what it creates.
- The class extends `VariableHeightLayoutCache`, so `treeState instanceof VariableHeightLayoutCache` and casts in
  `BasicTreeUI` subclasses keep working. It overrides every public method; the inherited private state stays empty.
- Licence: the new file is GPL-2.0-only WITH Classpath-exception-2.0, `TARGET_OWNED_PORT`, with Hitesh Soliwal
  (Synexia) recorded as rights holder in [`LICENSE_DECISIONS.tsv`](LICENSE_DECISIONS.tsv). No Synexia, EPL, Apache or
  LGPL expression is copied into `src/**`; the Synexia viewport core contributes ideas only. Donors and rejections:
  [`FOSS_REUSE_DECISION.tsv`](FOSS_REUSE_DECISION.tsv). Docs, bench and tests under `m3/**` are Apache-2.0
  (Copyright 2026 Hitesh Soliwal and contributors, [`NOTICE`](NOTICE)).

## 2. Inventory: where stock Swing is O(n) or object-heavy (read from the sources, then measured)

| Site | Cost at 10^6 rows | Leaf |
|---|---|---|
| `VariableHeightLayoutCache.TreeStateNode.getRow` = `visibleNodes.indexOf(this)` | O(n) for every `getRowForPath`, every expand and collapse | 1 (this) |
| `visibleNodes.insertElementAt` / `removeElementAt` per row on expand and collapse | O(n) shift per row: collapse + expand of a 10-child subtree near the top of a 10^6-row tree took 36 s | 1 |
| `updateYLocationsFrom`, `getMaxNodeWidth` | O(n) per call; `getPreferredWidth(null)` runs on every revalidate | 1 |
| `TreeStateNode` (a `DefaultMutableTreeNode`) + `TreePath` + `Hashtable` entry + two `Vector` slots per loaded node | 125-249 MB for 10^6 rows | 1 |
| `FixedHeightLayoutCache` (`largeModel` + fixed height): `getChildAtModelIndex`, `getRowToModelIndex`, `addNode` linear in loaded children; `adjustRowBy` and `getTotalChildCount` recursive over expanded nodes | O(expanded) per expand | 2 (next) |
| `JTree.getExpandedDescendants` iterates the whole `expandedState` table (`JTree.java:2034`), called by `BasicTreeUI.updateExpandedDescendants` on every expansion | O(expanded) per expansion | 3 |
| `DefaultMutableTreeNode.getIndex` = `Vector.indexOf` (`DefaultTreeModel.getIndexOfChild`) | O(children) per lookup | model side (#828) |
| `BasicListUI.convertLocationToRow`, `getCellBounds` sum `cellHeights` linearly | 470 us `locationToIndex`, 393 us `indexToLocation` at the end of 10^6 rows | 4 (JList) |
| `BasicListUI.updateLayoutState` measures every row unless the cell size is fixed | 551 ms per relayout of 10^6 rows | 4 |
| `JTable` row heights: `SizeSequence` is already a prefix structure | 2.3 us `setRowHeight`, 16 ms insert at 10^6 rows | none needed (5: object-heavy `DefaultTableModel` is #828's) |

## 3. Leaf 1: `M3VariableHeightLayoutCache`

**State only where the stock class has state that matters.** A `Node` exists for the root and for every node whose
children were loaded (the stock `hasBeenExpanded` nodes). Children that were never loaded have no object. Each node
holds its loaded children as lanes:

| Lane | Per child | Meaning |
|---|---|---|
| `v` | 1 reference | user object as loaded (the stock `TreeStateNode` user object; paths, sizes and leaf tests use it) |
| `x`, `w`, `h` | 3 ints | cached bounds; `h == 0` marks an invalid size, exactly as the stock `preferredHeight == 0` |
| `fh` | 1 int (Fenwick) | child height + height below the child when it is expanded |
| `fr` | 1 int (Fenwick, only when a child is expanded) | rows below the child when it is expanded |
| `kids` | 1 reference (only when a child has state) | the child's `Node` |
| `lookup` | about 2 ints (only for more than 16 children, built on first lookup) | open-addressing hash of user objects to child positions |

**Arithmetic instead of a row vector.**
- Row of a node: walk up its ancestors, adding `index + prefix(fr, index) + 1` per level: O(depth x log children).
- Node at a row: Fenwick descent per level (the implicit `+1` per child is folded into the descent): same cost.
- y of a row: Fenwick prefix of `fh` along the ancestors. Nothing stores a y origin, so nothing can go stale.
- Expand and collapse: flip one flag and add the subtree's rows and height to the ancestors' Fenwick trees until the
  first collapsed ancestor: O(depth x log children), plus the size computations the stock class also makes.
- Preferred width: per-node maxima with dirty flags; only changed subtrees are rescanned.
- Paths: a node's `TreePath` is kept; a child without state gets `parent.path.pathByAddingChild(v[i])`, reused through
  a 1024-slot table by position, and recognised by identity when it comes back (the paint loop hands the same paths
  to `getBounds` and `getExpandedState`). Other paths resolve through the hash index, as the stock class resolves them
  through its path map, then through `TreeModel.getIndexOfChild`, as the stock walk does.

**Same answers, by construction and by test.** Every operation performs the stock steps in the stock order:
sizes are computed at the same moments with the same row arguments (including the stock quirks: all children of a
first expansion get the parent's row + 1; children loaded by `setExpandedState(path, false)` get increasing rows; the
`startHeight` read before an expansion recomputes an invalid size first); `resetRowSelection`,
`removeSelectionPath` and `clearSelection` are called at the same points; the stock enumeration order of
`getVisiblePathsFrom` (including a start at a hidden root) and the stock binary search of `getPathClosestTo`
(including its behaviour around rows of height 0) are reproduced; exceptions (`ArrayIndexOutOfBoundsException` for
event indices beyond the loaded children) are the same classes.

**Known non-claims.** (1) Sibling user objects that are `equals` to each other: the stock path map keeps one of them
in an order that depends on its `Hashtable` history; the M3 index picks the last loaded. (2) For nodes whose
`NodeDimensions` answer is null or of height 0, the stock class asks again at more lazy reads than the M3 cache does;
the answers are the same (such sizes stay 0), the call counts are not. (3) After an exception escapes a cache call
(for example D2 below), the stock cache keeps stale y origins and the M3 cache does not.

## 4. Upstream defects (default-on policy: fixed in the M3 mode, accepted exactly in the differential)

[`UPSTREAM_DEFECTS.tsv`](UPSTREAM_DEFECTS.tsv):
- **D1**: `treeNodesChanged` for children of a hidden root does not move the following rows when a child's height
  changes (`VariableHeightLayoutCache.java:436-440`: the hidden root's row is -1). Stock rows overlap or leave gaps,
  for example after a node's text changes from a two-line HTML label to one line with `setRootVisible(false)`. The
  M3 cache has no stored y origins, so its rows stay exact.
- **D2**: `DefaultTreeSelectionModel.insureRowContinuity` throws `ArrayIndexOutOfBoundsException`
  (`DefaultTreeSelectionModel.java:918`) when a CONTIGUOUS selection holds an invisible path; both caches throw it at
  the same point. Not fixed in this leaf.

## 5. Proof

| Test | What it compares | Result (2026-10-17, JDK 21.0.9, stock + patch) |
|---|---|---|
| `m3/ports/swing-viewport/jdk-reproducers/M3TreeLayoutCacheDifferential` | Seeded scripts against a stock cache and an M3 cache with their own `DefaultTreeSelectionModel`s: `setExpandedState` on live, removed and previously returned paths; inserts, removals (single and batched), `nodesChanged` (with replaced user objects), `nodeStructureChanged`, `setRoot`; `setRootVisible`, `setRowHeight` (0, 16, 20, -1), `invalidateSizes`, `invalidatePathBounds`, size changes without invalidation; selection in all three modes. After every step: every row's path, row, expanded state, visible child count and (often) bounds; random paths; `getVisiblePathsFrom`; `getRowsForPaths`; preferred sizes; `getPathClosestTo`; selection rows, lead, min, max and events; exception classes; and, for seeds without zero-height sizes, the exact trace of `NodeDimensions` calls (value, row, depth, expanded). Wide parents (17-46 children) exercise the hash index. | **500 of 500 seeds x 400 steps pass** (seeds 1000-1499); D1 accepted 126 times, rebuilds after D2 exceptions 55, other exceptions 0. Earlier run: 100 of 100 seeds x 200 steps. |
| `M3JTreeViewportDifferential` | Two `JTree`s on one `DefaultTreeModel` with `MetalTreeUI` (stock cache) and a `MetalTreeUI` whose `createLayoutCache` returns the M3 cache: `expandRow`, `collapseRow`, `expandPath`, `collapsePath`, `makeVisible`, `setRootVisible`, `setShowsRootHandles`, `setRowHeight`, `setLargeModel`, `setFont`, `setCellRenderer`, selection API, editing (`startEditingAtPath`, `stopEditing`, `cancelEditing`), model edits with HTML two-line labels. After every step: rows, row bounds, expanded and selected state, path queries, `getExpandedDescendants`, preferred and scrollable sizes, location queries, listener events (expansion, will-expand, selection, property); every 10 steps the painted pixels of both trees. | **120 of 120 seeds x 250 steps pass**, 3,120 pixel comparisons equal; D1 accepted 22 times. Earlier run: 20 of 20 seeds x 150 steps. |
| `javac -Xlint:all` of the patched classes against JDK 21.0.9 | | clean for the new class; `BasicTreeUI` keeps its two existing `this-escape` warnings |
| `make`, jtreg (`@run` lines are in the tests) | | PENDING_CI (no OpenJDK build on this host) |

## 6. Benchmarks (headless, CPU only, `-XX:+UseSerialGC -Xmx1500m`, one JVM at a time)

Tool: [`bench/SwingViewportBench.java`](bench/SwingViewportBench.java). Raw rows: [`bench/BENCH_20261017.tsv`](bench/BENCH_20261017.tsv)
(stock and M3 interleaved per shape). The cache is driven directly with the renderer path of `BasicTreeUI`
(`DefaultTreeCellRenderer`, `x = 20 * depth`, variable row heights as in Metal). Shapes: **wide** = 1,000 x 1,000
(1,001,001 nodes), **flat** = 1,000,000 children of the root, **deep** = 10-ary, depth 6 (1,111,111 nodes, 111,111
expanded). All rows visible after expand-all.

| Metric | wide stock | wide M3 | flat stock | flat M3 | deep stock | deep M3 |
|---|---:|---:|---:|---:|---:|---:|
| expand-all (`setExpandedState` per internal node) | 1554 ms | 309 ms | 22 ms | 22 ms | 26403 ms | 749 ms |
| heap retained by the cache | 125.1 MB | 20.8 MB | 249.4 MB | 39.9 MB | 141.3 MB | 53.3 MB |
| `getRowForPath`, fresh path, first query | 175.42 us | 34.86 us | 203.72 us | 109.12 us | 205.87 us | 3.98 us |
| `getRowForPath`, same paths again | 214.32 us | 0.95 us | 173.83 us | 0.96 us | 185.31 us | 2.88 us |
| `getPathForRow` | 0.76 us | 0.83 us | 1.06 us | 0.74 us | 1.62 us | 2.17 us |
| `getBounds` | 1.51 us | 1.41 us | 1.63 us | 1.71 us | 1.68 us | 2.95 us |
| `getPathClosestTo` | 3.38 us | 3.97 us | 4.25 us | 3.72 us | 2.73 us | 16.01 us |
| collapse + expand of the first subtree | 432788.74 us | 3.49 us | 150489.71 us | 7.43 us | 44379319.47 us | 22.93 us |
| `getPreferredWidth(null)` | 26.88 ms | 7.10 ms | 25.20 ms | 8.95 ms | 65.30 ms | 24.93 ms |
| paint row walk at the end (p50) | 0.314 ms | 0.008 ms | 0.651 ms | 0.006 ms | 0.337 ms | 0.018 ms |

| JTree through its public API (`-Dm3.swing.viewport=true` vs stock) | wide stock | wide M3 | flat stock | flat M3 |
|---|---:|---:|---:|---:|
| `JTree.expandPath` for every internal node | 1734 ms | 615 ms | 24 ms | 12 ms |
| `getPreferredSize` after expand-all | 31 ms | 32 ms | 94 ms | 8 ms |
| heap of JTree + UI + cache + expansion table | 124.7 MB | 21.9 MB | 128.5 MB | 21.5 MB |
| `getRowForPath` of the last node | 468.65 us | 2.58 us | 554.68 us | 1557.70 us |
| frame at the start (setViewPosition + paint 800x600, p50) | 1.786 ms | 17.048 ms | 1.904 ms | 1.638 ms |
| frame at the end (p50) | 1.476 ms | 0.839 ms | 1.607 ms | 1.045 ms |

| Stock JList and JTable at 10^6 rows (next leaves) | value |
|---|---:|
| JList `getPreferredSize` (measures every row) | 1246 ms |
| JList `locationToIndex` at the end | 502.42 us |
| JList `indexToLocation` at the end | 455.23 us |
| JList frame at the end (p50) | 1.280 ms |
| JTable `rowAtPoint` at the end, variable heights | 4.10 us |
| JTable `setRowHeight(row, h)` | 9.54 us |
| JTable insert at row 0, variable heights | 27 ms |
| JTable frame at the end (p50) | 1.365 ms |

Notes on this run (rechecked in a later commit): the first `getRowForPath` of a fresh path below a parent with more
than 16 children builds that parent's hash index once (for the 10^6-child root of `flat`, about 0.1 s); later queries
are about 1 us. The JTree-level M3 `frame_start` of `wide` (17 ms p50) and the p95 values of that run are being
re-measured: the machine was shared with other agents during the run, and the same walk through the cache alone
(`paint_walk`) takes 0.01-0.02 ms.

`paint_walk` is the row walk of `BasicTreeUI.paint` for an 800x600 window (`getPathClosestTo`, `getVisiblePathsFrom`,
`getBounds` and `getExpandedState` per row). Stock JList and JTable at 10^6 rows (the next leaves) are in the same TSV.

## 7. Next leaves

1. Arithmetic `FixedHeightLayoutCache` (the `largeModel` path), same method and differential.
2. `JTree` expansion table: `getExpandedDescendants` and `removeDescendantToggledPaths` iterate the whole table on
   every expansion; an expanded-path index keyed by parent removes the O(expanded) factor.
3. `BasicTreeUI` paint by row index (inventory OH18 of `m3-swing-awt-viewport.md`): one `TreePath` per painted row only
   where `paintRow` needs it.
4. JList: prefix sums over `cellHeights` for `getCellBounds` and `convertLocationToRow` (identical answers, O(log n)),
   guarded so that subclasses that write `cellHeights` keep the stock loops; measure-on-demand for `updateLayoutState`.
5. Fix D2 in `DefaultTreeSelectionModel` in the M3 mode, with its own differential.
6. Default-on switch, together with the SWT `swt.widget.mode` convention, once CI has run make and jtreg.
