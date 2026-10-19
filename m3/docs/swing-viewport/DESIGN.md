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

## 3. Leaf 1: `M3VariableHeightLayoutCache` (DOOP: state in lanes, objects only when an API asks)

**DOOP (data-oriented OOP, brief rule 18).** There are no node objects at all. A node with state (the root and every
node whose children were loaded, the stock `hasBeenExpanded` nodes) is an int id into struct-of-arrays node lanes:
`parent`, `index`, `depth`, packed flag bits (EXPANDED, LOADED, DIRTY, LANE_DIRTY), `count`, `below`, `belowH`,
`inval`, `laneMax`, `max`, plus its `TreePath` and user object. Children that were never loaded have no id. The only
objects handed out are the ones the public API returns (`TreePath`, `Rectangle`), created on request and reused
through small caches. Each node's loaded children are child lanes:

| Child lane | Per child | Meaning |
|---|---|---|
| `cv` | 1 reference | user object as loaded (the stock `TreeStateNode` user object; paths, sizes and leaf tests use it) |
| `cx`, `cw`, `ch` | 3 ints | cached bounds; `ch == 0` marks an invalid size, exactly as the stock `preferredHeight == 0` |
| `fh` | 1 int (Fenwick) | child height + height below the child when it is expanded |
| `fr` | 1 int (Fenwick, only when a child is expanded) | rows below the child when it is expanded |
| `kids` | 1 int (only when a child has state) | the child's node id |
| `lookup` | about 2 ints (only for more than 16 children, built after 32 scans) | open-addressing hash of user objects to child positions |

The path map (state `TreePath` -> node id) is a primitive open-addressing table with backward-shift deletion, not a
`Hashtable` of entries.

**Arithmetic instead of a row vector.**
- Row of a node: walk up its ancestors, adding `index + prefix(fr, index) + 1` per level: O(depth x log children).
- Node at a row: Fenwick descent per level (the implicit `+1` per child is folded into the descent): same cost.
- y of a row: Fenwick prefix of `fh` along the ancestors. Nothing stores a y origin, so nothing can go stale.
- Expand and collapse: flip one flag and add the subtree's rows and height to the ancestors' Fenwick trees until the
  first collapsed ancestor: O(depth x log children), plus the size computations the stock class also makes.
- Preferred width: per-node maxima with dirty flags; only changed subtrees are rescanned.
- Paths: a node's `TreePath` is kept; a child without state gets `parent.path.pathByAddingChild(v[i])`, reused through
  a 1024-slot table by position, and recognised by identity when it comes back (the paint loop hands the same paths
  to `getBounds` and `getExpandedState`). Other paths resolve through the cached user objects (a scan for small or rarely queried parents, the
  hash index otherwise, an identity memo for repeated components), as the stock class resolves them through its path
  map, then through `TreeModel.getIndexOfChild`, as the stock walk does.

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
- **D4**: stock `FixedHeightLayoutCache.getRowForPath` and `getVisiblePathsFrom` pass `getIndexOfChild`'s -1 on
  without a check when the parent is expanded and visible but no longer has the path's last component (a removed
  selected node, before `JTree` prunes the selection): the row is the parent's row and the enumeration starts at the
  parent, where `VariableHeightLayoutCache` answers -1 and null. The M3 cache reproduces it on purpose: answering -1
  would hand a row -1 to `resetRowSelection` during node removal (the UI's model listener runs before `JTree`'s) and
  expose D2 in CONTIGUOUS selections of large-model trees that do not throw today.

## 5. Proof

| Test | What it compares | Result (2026-10-17, JDK 21.0.9, stock + patch) |
|---|---|---|
| `m3/ports/swing-viewport/jdk-reproducers/M3TreeLayoutCacheDifferential` | Seeded scripts against a stock cache and an M3 cache with their own `DefaultTreeSelectionModel`s: `setExpandedState` on live, removed and previously returned paths; inserts, removals (single and batched), `nodesChanged` (with replaced user objects), `nodeStructureChanged`, `setRoot`; `setRootVisible`, `setRowHeight` (0, 16, 20, -1), `invalidateSizes`, `invalidatePathBounds`, size changes without invalidation; selection in all three modes. After every step: every row's path, row, expanded state, visible child count and (often) bounds; random paths; `getVisiblePathsFrom`; `getRowsForPaths`; preferred sizes; `getPathClosestTo`; selection rows, lead, min, max and events; exception classes; and, for seeds without zero-height sizes, the exact trace of `NodeDimensions` calls (value, row, depth, expanded). Wide parents (17-46 children) exercise the hash index. | **500 of 500 seeds x 400 steps pass** (seeds 1000-1499); D1 accepted 126 times, rebuilds after D2 exceptions 55, other exceptions 0. Earlier run: 100 of 100 seeds x 200 steps. |
| `M3JTreeViewportDifferential` | Two `JTree`s on one `DefaultTreeModel` with `MetalTreeUI` (stock cache) and a `MetalTreeUI` whose `createLayoutCache` returns the M3 cache: `expandRow`, `collapseRow`, `expandPath`, `collapsePath`, `makeVisible`, `setRootVisible`, `setShowsRootHandles`, `setRowHeight`, `setLargeModel`, `setFont`, `setCellRenderer`, selection API, editing (`startEditingAtPath`, `stopEditing`, `cancelEditing`), model edits with HTML two-line labels. After every step: rows, row bounds, expanded and selected state, path queries, `getExpandedDescendants`, preferred and scrollable sizes, location queries, listener events (expansion, will-expand, selection, property); every 10 steps the painted pixels of both trees. | **120 of 120 seeds x 250 steps pass**, 3,120 pixel comparisons equal; D1 accepted 22 times. Earlier run: 20 of 20 seeds x 150 steps. |
| `M3VariableHeightLayoutCacheTest` (JUnit 5): both differentials plus edge scenarios, each compared with the stock cache (no selection model; null/empty events and paths; no model, no root, rebuilds; bad event indices; enumeration past the end; null children; a model changed without events; value-equal objects with a hash collision; 3000 rows and path-cache collisions) | **12/12 pass; JaCoCo 100.00% lines (996/996), 99.71% branches (678/680)** of `sun.swing.tree` (the 2 missed branches are `insertLane`'s `i < 0` guard, unreachable through the public API, and a defensive null-vs-stamp check) |
| Leaf 2: `M3TreeLayoutCacheDifferential` with `-Ddiff.fixed=true` | stock `FixedHeightLayoutCache` vs `M3FixedHeightLayoutCache`, the same scripts with positive row heights and the `setRowHeight(0)`/`(-1)` exception probes | **2026-10-18: 20 of 20 seeds x 200 steps pass**; the first run found an enumeration NPE past the root (fixed) and D4 (section 4, reproduced) |
| Leaf 2: `M3FixedHeightLayoutCacheTest` (JUnit 5): the fixed differential 60 seeds x 300 steps, the `ENABLED` property check and 8 edge scenarios (row-height contract, expansion below collapsed nodes, edits around state children, enumerations, 2,000 siblings, null and empty input, no model and no root, no selection model) | **10/10 pass; JaCoCo 100.00% lines (490/490), 99.70% branches (337/338)**; the missed branch is `walk`'s reuse of a state child whose path is not in the map, reachable only after model edits without events |
| Leaf 2: `M3JTreeViewportDifferential`, the M3 UI swapping both caches; `-Ddiff.fixed=true` starts both trees large-model at row height 16 | as above, through `JTree` | **fixed: 60 of 60 seeds x 250 steps pass, 9,889 steps on the fixed cache, 1,560 pixel comparisons equal; variable: 30 of 30 x 250 pass, 1,194 steps on the fixed cache, 780 pixel comparisons equal** |
| `javac -Xlint:all` of the patched classes against JDK 21.0.9 | | clean for both new classes; `BasicTreeUI` keeps its two existing `this-escape` warnings |
| Hook recipe `com.synexia.openrewrite.viewport.M3SwingViewportBasicTreeUI` (`m3/docs/swing-viewport/recipe/`) via `M3PerFileApply` | | pass 1 CHANGED, pass 2 UNCHANGED (fixed point) |
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

## 7. Leaf 2: `M3FixedHeightLayoutCache` (DOOP, the `largeModel` path)

Stock `FixedHeightLayoutCache` (selected by `BasicTreeUI` when `largeModel` is set and the row height is positive) is
already lazy: an `FHTreeStateNode` exists only for paths that were resolved, and only the state children of a node are
kept, sorted by model child index. Its costs are the absolute `row` field kept in every state node and the linear
walks over the sparse children:

| Site | Cost |
|---|---|
| `adjustRowBy`, `resetChildrenRowsFrom`, `didExpand` | every expand, collapse, insert or remove rewrites the rows of all following expanded state nodes, recursing to the root |
| `getChildAtModelIndex`, `getRowToModelIndex`, `addNode`, `getCountTo` | linear over the sparse children per level |
| `getTotalChildCount` | recursive over the expanded subtree (or one sibling subtraction) |

`sun.swing.tree.M3FixedHeightLayoutCache` keeps the same lazy state as int-id SoA lanes (parent, model index, depth,
EXPANDED flag, model child count at expansion) and, per node, its state children as a sorted `int[]` of model indices
plus a Fenwick tree of the rows below each expanded state child. No absolute row is stored anywhere: a row is the
prefix sum `index + rowsBefore + 1` along the ancestors, a row maps to a node by binary search over the sorted
indices with Fenwick descent, and expand, collapse, insert and remove update O(depth x log k) aggregates. The sorted
lanes follow the stock class's own sorted insertion (`addNode`); the pinned AVL donors (SWT #197
`VirtualTreeSiblingIndex`, Nebula `GridRowTree`) remain evidence for parents with very many *state* children, which
the stock class does not optimize either. `getNodeDimensions` is called only where the stock class calls it
(`getBounds`), with the same row and expanded arguments. The hook is the recipe of leaf 1 extended with one insertion
at `createLayoutCache`'s start, behind the same `m3.swing.viewport` property (default off).

Proof: the leaf-1 differential run in fixed mode (`-Ddiff.fixed=true`: stock `FixedHeightLayoutCache` vs the M3
class, positive row heights, plus the `setRowHeight(0)` and `setRowHeight(-1)` IllegalArgumentException probes) and
the JUnit scenario suite with JaCoCo. Results in section 5 and `m3/ports/swing-viewport/TESTS_TO_RUN.md`.

## 7a. Seams for reuse (read-only, for `m3/viewport-core` and the AWT, SwingX, IntelliJ and RWT adapters)

Both caches live in `sun.swing.tree` (not exported) and stay Swing classes; another toolkit reuses them read-only,
through one of these seams, and never by reaching into their private lanes:

| Seam | What it is | Contract kept by this branch |
|---|---|---|
| `AbstractLayoutCache` surface | both classes are drop-in subclasses of the stock caches | every public method answers as the stock class (sections 5 and 7); a change here is a stock-behaviour change and needs a differential |
| `NodeDimensions` callback | the only toolkit coupling besides `TreeModel`, `TreePath` and `Rectangle` | called with the stock row, depth and expanded arguments, at the stock moments |
| Lane layout | int ids into struct-of-arrays lanes, id 0 = none; leaf 1: section 3 table; leaf 2: `parent`, `mindex`, `depth`, `flags` (EXPANDED), `count` (model child count at expansion), `tot` (visible rows below an expanded node), per node `smi`/`skid` (sorted resolved model indices and their ids) and `fb` (Fenwick of `tot` over expanded slots, `null` = rebuild on next read) | no absolute row or y origin is stored anywhere; a stale aggregate is impossible by construction |
| Row arithmetic | row of a child = row of parent + 1 + model index + Fenwick prefix of the slots before it; row to node = per-level binary search over that start offset (`descend`); expand, collapse, insert, remove = O(depth x log k) Fenwick updates along the ancestors (`propagate`, `fenwickAdd`) | pure int arithmetic over the lanes; no toolkit types; the part a toolkit-neutral core can lift as is |
| Mutation entry points | `TreeModelListener` methods, `setExpandedState`, `setModel`, `setRootVisible`, `setRowHeight`, `invalidate*` | the only writers of the lanes |

Lifting the lanes and the row arithmetic into `m3/viewport-core` (an int row model with a node-dimension callback,
with the Swing classes delegating to it) is the viewport-everywhere agent's leaf, not this one; until then these
method names and lane meanings change only together with a note in this section.

## 8. Next leaves

1. Leaf 2 above (done).
2. `JTree` expansion table (done: leaf 5, section 11): `getExpandedDescendants` and `removeDescendantToggledPaths` iterated the whole table on
   every expansion; an index by parent with the table's own order mirrored removes the O(expanded) factor.
3. `BasicTreeUI` paint by row index (inventory OH18 of `m3-swing-awt-viewport.md`): one `TreePath` per painted row only
   where `paintRow` needs it.
4. JList: prefix sums over `cellHeights` for `getCellBounds` and `convertLocationToRow` (done: leaf 3, section 9),
   guarded so that subclasses that write `cellHeights` keep the stock loops; `updateLayoutState` measures only the rows
   that list data events touched (done: leaf 4, section 10). An estimated-height lane is a contract change, recorded
   in `ENHANCED_API_MAP.tsv` for the user.
5. Fix D2 in `DefaultTreeSelectionModel` in the M3 mode, with its own differential.
6. Default-on switch, together with the SWT `swt.widget.mode` convention, once CI has run make and jtreg (proposal: section 12).

## 9. Leaf 3: JList row offsets by prefix lane (`sun.swing.list`)

Stock `BasicListUI` keeps one height per row in `cellHeights` (a new array per `updateLayoutState`) and sums it row
by row in two places: `getCellBounds(JList, int)` (the y of a vertical row) and `convertLocationToRow` (the row at a
y). `indexToLocation`, `locationToIndex`, `getPreferredSize` (the bounds of the last row), scroll increments and
every painted row pay O(row): 302-455 us per query at the end of 10^6 rows (section 2).

| Class | Role |
|---|---|
| `sun.swing.list.M3RowOffsets` | the seam: `startOf(row)` (wrapped `int` sum of the heights before a row), `monotone()`, `firstEndingAfter(offset)`; the switch point for an `m3/collections` prefix-sum owner once one ships inside the JDK (none exists on `m3/viewport-everywhere-20261018` at `b9d9659c184`; `M3SparseExtents` is sparse and lives outside `java.desktop`) |
| `sun.swing.list.M3PrefixRowOffsets` | one `int[n + 1]` prefix lane built in one pass; `startOf` is a load, `firstEndingAfter` a binary search |
| `sun.swing.list.M3ListRows` | per-UI state: offsets keyed by the identity of the `cellHeights` array, the module guard, and the two stock loops' answers |

**Algorithm choice (rule 21, `ALGORITHM_CATALOGUE.tsv`, `bench/ROW_OFFSETS_20261018.tsv`).** At 10^6 rows the prefix
lane builds in 0.69 ms and answers the start of a row in 3.5 ns and the row at an offset in 149 ns; the in-repo
`javax.swing.SizeSequence` takes 4.4 ms, 133 ns and 176 ns, a Fenwick tree 1.8 ms, 28 ns and 236 ns, sqrt blocks
0.3 ms, 196 ns and 280 ns; the stock loop takes 288 us for the start of the last row. The heights never change in
place (the stock class replaces the array on every layout), so the point updates of SizeSequence and Fenwick buy
nothing here; SizeSequence stays the candidate owner for the measure-on-demand leaf, where single heights change.

**Same answers.** `getCellBounds`: the stock `y = insets.top + h[0] + ... + h[row - 1]` in `int` arithmetic equals
`insets.top + startOf(row)` for any heights, wrapping included (addition modulo 2^32); rows at or past
`cellHeights.length` keep the stock code (which answers y = 0). `convertLocationToRow`: the stock loop returns the
first row whose `[y, y + h)` holds `y0`, else `size - 1` (also when `y0` is above the list and `closest` is false),
and 0 for `closest` above the top; with non-negative heights and no `int` overflow of `insets.top + startOf(size)`
that is `min(firstEndingAfter(y0 - top), size - 1)`. With a negative height or an overflow, `M3ListRows.rowAt`
answers `STOCK` and the stock loop runs.

**Guard.** The stock class writes `cellHeights` only while it fills a newly allocated array in `updateLayoutState`,
and no other `java.desktop` class touches the field (`git grep cellHeights`), so the array's identity is its
version. A `BasicListUI` subclass from another module could write the array in place; such UIs keep the stock loops
(the check is `getClass().getModule()`). Third-party look and feels therefore keep stock behaviour and cost.

**Hook (recipe-first).** `recipe/META-INF/rewrite/m3-swing-viewport-list.yml`
(`com.synexia.openrewrite.viewport.M3SwingViewportBasicListUI`): one field (`M3AddMemberIfMissing`) and two blocks
(`M3InsertStatements`, `BEFORE` the stock code they bypass, falling through to it when the path is off). OpenJDK
writes `default:` without braces and `else if` chains, which the insert recipe did not search; the recipe was
extended (hsoliwal/com.synexia `8c122afccd25` on `m3/viewport-catalogue-recipes-20261016`: `else if` chains and
`case` statements with or without braces, two new recipe tests) and then applied.

| Proof (2026-10-19, JDK 21.0.9, stock + `--patch-module java.desktop`; re-run with build and test output on S: after G: showed failing writes, same results) | Result |
|---|---|
| `M3JListViewportDifferential 40 300` (`-Dm3.swing.viewport=true`): two JLists on one `DefaultListModel`, one with a `BasicListUI` subclass outside `java.desktop` (stock loops), one with `BasicListUI` (prefix lane); heights 0, negative and very large included; public answers after every step, pixels every 10 steps | **40 of 40 seeds x 300 steps pass, 1,240 pixel comparisons equal, offsets built at all 12,040 checks** |
| Mutation check: `firstEndingAfter` with `>=` in place of `>` | caught: 3 of 5 seeds fail |
| `M3ListRowsTest` (JUnit 5: lane and `rowAt` against the stock loops copied as oracles, wrapping, the monotone flag, guard and versioning, the JList differential) under JaCoCo 0.8.15, merged with a flag-off differential run | **7/7 pass; `sun.swing.list` 100.00% lines (47/47), 100.00% branches (30/30)** |
| `javac -Xlint:all` of the patched classes | clean for the three new classes; `BasicListUI` adds no warning |
| Hook recipe via `M3PerFileApply` | pass 1 CHANGED, pass 2 UNCHANGED (fixed point); the extended `M3InsertStatements` leaves the `BasicTreeUI` and SWT `Tree` hooks UNCHANGED |
| Recipe crate tests | 35 run, 0 failures, 2 skipped (`M3InsertStatementsTest` 9/9) |
| `make`, jtreg | PENDING_CI |

**Benchmark** (`bench/LIST_20261019.tsv`, `SwingViewportBench list`, 10^6 rows, three rounds on a heavily shared
machine, stock / M3): `locationToIndex` of the last row 405-626 us / 2.0-3.2 us; `indexToLocation` of the last row
366-501 us / 1.6-2.8 us; 800x600 frame p50 in the middle 0.82-0.99 ms / 0.42-0.57 ms, at the end 1.23-1.36 ms /
0.43-0.50 ms (the start frame is noise-bound in both); heap +4 MB (the lane). The single-shot relayout rows
(`getPreferredSize` 493-687 ms / 593-869 ms, `add(0)` + relayout 378-574 ms / 360-588 ms) are noise-bound: two further
rounds with a fixed 1.5 GB heap (no heap growth) gave `getPreferredSize` 559-699 ms stock and 622-656 ms M3. Every
row is still measured on each relayout; leaf 4 (section 10) removes that for model edits.

## 10. Leaf 4: JList measures only the rows that list data events touched (`sun.swing.list`)

Stock `BasicListUI.updateLayoutState` measures every row on every layout: one `getElementAt`, one renderer call and
one `getPreferredSize` per row. Each list data event (an insert, a removal, a `set`) therefore costs a full pass:
360-588 ms per edit at 10^6 rows (section 9). Leaf 4 keeps each row's measured height and width between layouts and
measures only the rows that the events touched.

| Class | Role |
|---|---|
| `sun.swing.list.M3RowPieces` | piece table: the rows after the edits as runs of rows measured at the last layout, or runs of new rows; an event edits it in O(pieces), the next layout copies the kept sizes in one O(n) pass |
| `sun.swing.list.M3ListMeasure` | per-UI state: the height and width lanes of the last layout (the height lane is the `cellHeights` array handed to `BasicListUI`, never written afterwards), the pieces, the inputs that set row sizes, and the `measure` decision |

**When it applies.** With `-Dm3.swing.viewport=true`, a UI class in `java.desktop`, vertical layout, variable row
heights (`fixedCellHeight == -1`), an enabled list, and a renderer whose class is exactly `DefaultListCellRenderer`
or `DefaultListCellRenderer.UIResource` (the look and feel's default). That renderer sizes a row from the value
(text, HTML or icon), the list's font and orientation, its border from the look and feel, and its own label
properties; never from the row index (the index only selects drop-location colours). Anything else keeps the stock
loop, and the next accepted layout measures every row.

**What a layout measures.**
- Every row, in the stock order: the first layout, and any layout after the model, the renderer, the list's font
  (identity) or component orientation, or the renderer's `html.disable` property, icon-text gap, text positions,
  alignments, label UI or set preferred size changed.
- Otherwise the inserted and changed rows, in ascending order, plus one probe: the last row kept from the last
  layout. When the probe is the last row it is measured last; otherwise it is measured first and the rows after it
  are all new. Either way the renderer ends configured for the last row, as after the stock loop.
- Every row again when the probe measures differently from its recorded size. This catches changes that resize every
  row without an event or a tracked input, such as a `UIManager` border or a display scale change.
- Every row when the events cannot be followed: `contentsChanged` with an index below 0, indices out of range, a
  model size that the events do not explain, or more than 1,024 pieces.

**Same answers.** The hook sets `cellWidth` (the fixed width, else the widest row, -1 for an empty list),
`cellHeight = -1`, `cellHeights` (a new array per layout) and `columnCount = 1`, exactly the fields the stock
method sets for a vertical list. They equal the stock values whenever every kept row would still measure as
recorded. The tracked inputs, the probe and the events make sure of that, except in the one case below.

**The one known divergence: a value changed without a list data event.** The `ListModel` contract says listeners are
told of every change, and `DefaultListModel` does so. A model that breaks it, for example a mutable value whose
`toString` changes, an `Icon` that changes size, or HTML whose images load later, gives different answers:
- Stock keeps the row's old size until the next `updateLayoutState`, which any later event or property change
  causes (an insert elsewhere, a font change). That layout measures every row and picks the change up.
- The M3 path keeps the recorded size until the row itself gets an event, or until every row is measured again
  (a tracked input changes, the probe differs, the events cannot be followed, or the changed row is the probe).
- `M3ListMeasureTest.eventlessMutationIsTheOneKnownDivergence` records both behaviours. An application that hits
  it fires the event the contract asks for (`DefaultListModel.set(i, sameValue)`).

The renderer and the model also see fewer calls (the touched rows plus one probe instead of every row). Only the
JDK's own renderer classes take the path, and neither has side effects; a model whose `getElementAt` has side
effects sees fewer of them.

**Not built: an estimated-height lane** (measure on demand, estimate unmeasured rows). `getPreferredSize` and every y
below an unmeasured row would be estimates, so the answers would differ from stock until every row is measured. It
is a contract change, recorded in `ENHANCED_API_MAP.tsv` (`LIST-ESTIMATED-ROW-HEIGHTS`, an opt-in client property)
for the user to decide.

**Algorithm choice (rule 21, `ALGORITHM_CATALOGUE.tsv`, `bench/ROW_EDITS_20261019.tsv`).** Bookkeeping between two
layouts at 10^6 rows, measuring excluded: the piece table takes 1.0-3.0 ms whatever the edits (one front insert,
40 random inserts, 1,000 appends, 1,000 front inserts, 100 changes, 100 removals); shifting both lanes on every
event takes 0.5-1.0 ms for a single edit but 15.5 ms for 40 random inserts and 346 ms for 1,000 front inserts.
The piece table is chosen. **Donor check:** JetBrainsRuntime `jbr21` `BasicListUI` is identical to stock and `main`
changes only comments (`FOSS_REUSE_DECISION.tsv`); there is nothing to port.

**Hook (recipe-first).** The same recipe, `M3SwingViewportBasicListUI`, adds a second field, a block at the start
of `updateLayoutState` (it falls through to the stock loop when `measure` declines), and one statement at the start
of `Handler.intervalAdded`, `intervalRemoved` and `contentsChanged`. The statement runs before the selection model
is updated, because the selection listener can lay the list out in the middle of the event. Pass 1 CHANGED, pass 2
UNCHANGED.

| Proof (2026-10-19, JDK 21.0.9, stock + `--patch-module java.desktop`, output on S:) | Result |
|---|---|
| `M3JListViewportDifferential 40 300` (`-Dm3.swing.viewport=true`). Every third seed now keeps `DefaultListCellRenderer` with plain, HTML (one to four lines), empty and null strings and icons; those seeds also toggle `html.disable`, the icon-text gap, the text position, the enabled state and the orientation, and swap the renderer (UIResource, plain, a subclass). Public answers are compared after every step and pixels every 10 steps | **40/40 seeds x 300 steps pass, 1,240 pixel comparisons equal, 990 layouts measured only part of the rows** |
| Planted bugs: `contentsChanged` not recorded; the piece split losing its row offset | caught: 2 of 9 and 1 of 9 seeds fail (the 3 renderer seeds among them) |
| `M3ListMeasureTest` (JUnit 5, 9 tests: piece table against a reference list, which rows a layout measures, the probe at the front, at the end and for a height-only change, 12 tracked inputs, declined cases, untrackable events and the 1,024-piece cap, the event-less mutation divergence, empty/clear/fixed width, 30 x 200 differential) with `M3ListRowsTest`, under JaCoCo 0.8.15, merged with a flag-off run | **16/16 pass; `sun.swing.list` 100% lines (213/213), 100% branches (162/162)** |
| `javac -Xlint:all` of the patched classes | clean for both new classes; warnings unchanged (`BasicTreeUI`'s two `this-escape`) |
| Hook recipe via `M3PerFileApply` | pass 1 CHANGED, pass 2 UNCHANGED |
| `make`, jtreg | PENDING_CI |

**Benchmark** (`bench/LIST_MEASURE_20261019.tsv`, `SwingViewportBench list`, 10^6 rows, two interleaved rounds,
stock / M3; `relayout_*` = one model edit + `getPreferredSize`, median of 11):

| Edit | stock | M3 |
|---|---:|---:|
| insert at the front | 564-584 ms | 6.3 ms |
| `set` in the middle (two-line HTML) | 681-971 ms | 12.1-14.0 ms |
| append | 792-836 ms | 4.1-4.2 ms |
| remove at the front | 693-1,007 ms | 5.3-6.1 ms |
| first layout (`getPreferredSize`, single shot) | 1,302-1,350 ms | 939-1,165 ms (every row is measured either way; noise-bound) |
| heap after the first layout | 62.3-63.3 MB | 70.9 MB (+4 MB width lane, +4 MB leaf 3 prefix lane) |

The machine was more loaded than in section 9's run; the stock edit rows there were 360-588 ms single shot.

## 11. Leaf 5: the JTree expansion table indexed by parent (`sun.swing.tree.M3ExpandedState`)

`JTree` keeps every path ever expanded in a `Hashtable<TreePath, Boolean>`. Two of its operations scan the whole
table with `TreePath.isDescendant`:
- `getExpandedDescendants(parent)`. `BasicTreeUI` calls it on every expansion, so expanding n nodes costs O(n * table).
- `removeDescendantToggledPaths`, run for every removed toggled child and on structure changes.

On the deep shape (1,111,111 nodes), stock got through 23,152-30,752 of the 111,111 expansions in the 120 s budget,
and so did leaves 1-2.

**The table.** With `-Dm3.swing.viewport=true`, `JTree` creates `M3ExpandedState` instead of the plain table, in
its constructor and in `readObject`. It is the stock `Hashtable`, with the same constructor and the same `put`
sequence, so `get`, `keys()` and their order are those of stock. Beside it, it keeps:
- A trie of the keys and their ancestors (`HashMap<TreePath, Node>`: parent, children, toggled), updated by
  `put`, `remove` and `clear`.
- The `Hashtable`'s bucket count, mirrored: 11 buckets, then 2n + 1 when a new key arrives and the count has
  reached 0.75 of the capacity.
- A stamp per key, whose descending order is the key's position in its bucket's chain. A new entry goes to the head
  of its chain. `rehash` moves the entries in `keys()` order, each to the head of its new bucket, and the stamps are
  renumbered in that order.

**The two operations.**
- `getExpandedDescendants` takes the TRUE keys at or below the parent from the trie, sorted as `keys()` walks them
  (bucket descending, then stamp descending). It then tests `isVisible` on them in that order, which yields the stock
  enumeration with the stock elements in the stock order. The parent is skipped by identity, as the stock loop's
  `path != parent` does, so a fresh path equal to a key returns that key, as in stock.
- `removeDescendantToggledPaths` removes each path's subtree through the trie. The order of removals does not change
  the table: a removal unlinks one entry and never rehashes.

**When the stock code still runs:**
- The `JTree` class overrides `getDescendantToggledPaths`: it keeps a plain `Hashtable`, checked once per class
  under `doPrivileged`. A class whose methods cannot be resolved also keeps it.
- A mutation the index did not see. None exists in `JTree`, which only calls `get`, `put`, `remove`, `clear` and
  `keys`; one would come from reflection or an entry-set iterator. It makes the key count differ, and every call
  goes back to the stock scan until `clear`.

**Precondition (the one known difference).** As for the `Hashtable` itself, a path's last component must keep its
`hashCode` and `equals` while the tree shows it. The trie also holds the ancestors of keys, so a component whose
hash changes while only its descendants are keys can make the index miss keys that the stock loop, which uses only
`equals`, finds. `M3ExpandedStateTest.changedHashIsTheRecordedPrecondition` records it, and records that the
cases where the index loses track fall back to the stock loop.

**Algorithm choice (rule 21, `ALGORITHM_CATALOGUE.tsv`, `bench/EXPANSION_TABLE_20261019.tsv`).**
- Chosen: the trie with mirrored bucket and chain order.
- Rejected: the first version, which handed bucket ties to the stock scan. 17% of queries tied in the differential,
  and every re-expansion with 100+ answers tied.
- Rejected: replacing the table with a sorted or linked map, because it changes the order callers see.
- Donors: JetBrainsRuntime has nothing to port. Its `jbr21` `JTree` differs only in accessibility, and
  `DefaultTreeSelectionModel` is identical (`FOSS_REUSE_DECISION.tsv`).

**Hook (recipe-first).** The recipe `M3SwingViewportJTree` makes four insertions into `JTree`: the constructor,
`readObject`, `getExpandedDescendants` and `removeDescendantToggledPaths`. Pass 1 CHANGED, pass 2 UNCHANGED.
`JTree`'s two `this-escape` warnings are stock.

| Proof (2026-10-19, JDK 21.0.9, stock + `--patch-module java.desktop`, output on S:) | Result |
|---|---|
| `M3JTreeExpansionDifferential 40 300`. Two JTrees on one `DefaultTreeModel`; the stock side overrides `getDescendantToggledPaths` and keeps the plain table. Steps: expand and collapse by path and by row, makeVisible, selection, inserts, removals, subtree moves, structure changes, reloads, new roots, `setModel`, vetoes; every fourth seed uses nodes equal by name with reused names. Compared after every step: table keys, values and order; public queries with fresh paths and with the table's own keys; rows; selection; preferred size; pixels every 10 steps | **40/40 pass, 1,240 equal pixel captures; 130,207 expanded-parent queries answered by the index, 0 by the stock loop** |
| Planted bugs: parent identity, growth rule, subtree removal without the path, stamp order, no renumbering on rehash | caught: 10, 10, 8, 10 and 7 of 10 seeds fail |
| `M3ExpandedStateTest` (JUnit 5, 10 tests: 30 x 500 random operations against a plain `Hashtable`, including the bucket count read from `Hashtable.table`; the growth rule to the array limit; parent identity; chains in one bucket across three rehashes; null key or value at the threshold; an unseen removal; the recorded precondition; the class gate; serialization; 20 x 200 differential), under JaCoCo, merged with a flag-off run | **10/10 pass; `M3ExpandedState` 100% lines (136/136), 100% branches (90/90)** |
| `javac -Xlint:all` | no new warnings |
| `make`, jtreg | PENDING_CI |

**Benchmark** (`bench/JTREE_EXPANSION_20261019.tsv`, `bench/EXPANSION_TABLE_20261019.tsv`):

| Measure | stock | M3 |
|---|---:|---:|
| expand all inner nodes in preorder, deep shape (111,111) | 30,752 done in 120 s (budget) | all in 776 ms (1,226 ms with the stamps, loaded machine) |
| expand all, wide shape (1,001) | 1,261 ms | 267-301 ms |
| `getExpandedDescendants` table loop, fresh expansion, 11,111 keys | 755 us | 1.6 us |
| re-expansion with 1,111 remembered keys | 411 us | 170 us |
| subtree removal of 1,111 keys (with re-insert) | 779 us | 310 us |
| memory | table only | about 190 bytes per key more |

## 12. Proposal for the user: from opt-in to default-on (not applied)

Today every M3 path is opt-in (`-Dm3.swing.viewport=true`). This section proposes how it becomes the default, with the
property kept only as a kill switch. Nothing here is applied yet; the default stays off until the user decides.

**Already in place.** M3jdk21 is the JDK, so the M3 paths are part of `java.desktop`, not a layer beside it:
- The `sun.swing.tree` and `sun.swing.list` classes are reached from stock entry points (`BasicTreeUI.createLayoutCache`,
  `BasicListUI.getCellBounds`, `convertLocationToRow` and `updateLayoutState`, the `JTree` constructor and
  `readObject`) through hooks that a recipe applies (section 7a).
- Each hook falls through to the unchanged stock code when the property is off, so the off state is the stock JDK by
  construction.
- No leaf so far needs JNI or HotSpot changes.

**Gates that stay after the flip.** These are correctness conditions, not part of the opt-in, so they remain:

| Leaf | Path | Stock code still runs when |
|---|---|---|
| 1, 2 | `M3VariableHeightLayoutCache`, `M3FixedHeightLayoutCache` | a `BasicTreeUI` subclass overrides `createLayoutCache` |
| 3 | `M3ListRows` | the UI class is outside `java.desktop`, or `cellHeights` is null |
| 4 | `M3ListMeasure` | renderer other than `DefaultListCellRenderer`(`.UIResource`), fixed cell height, disabled list, wrapped layout, UI outside `java.desktop` |
| 5 | `M3ExpandedState` | the `JTree` class overrides `getDescendantToggledPaths`; a table mutation the index did not see (none in `JTree`) |

**Known differences from stock.** Each needs a decision before the flip: accept it as a documented change, make
M3 match stock, or keep that leaf opt-in.

| Id | Leaf | Difference | Proposal |
|---|---|---|---|
| D1 | 1 | stock keeps stale y origins after `treeNodesChanged` under a hidden root; M3 rows stay exact | accept (M3 is right; upstream bug) |
| D4 | 2 | reproduced on purpose; M3 equals stock | none |
| leaf 4 | 4 | a value changed without a list data event: stock re-measures it at the next layout, M3 when its row gets an event or all rows are re-measured | accept, documented as a `ListModel` contract requirement; or keep leaf 4 opt-in |
| leaf 5 | 5 | a path component whose `hashCode` changes while the tree shows it can make the index miss keys the stock loop finds | accept (the stock `Hashtable` breaks under the same misuse) |

**Exit criteria. All must hold before the flip:**
1. CI builds the JDK (`make images`) from this branch. Today: PENDING_CI.
2. The jtreg groups `test/jdk/javax/swing` (JTree, JList, JTable, `plaf/basic`, `plaf/metal`, `plaf/nimbus`) and the
   new reproducers pass with the property true and with it false. The failure lists must equal those of stock.
3. Every M3 differential is identical to stock under each shipped look and feel (Metal and Nimbus everywhere,
   Windows, GTK and Aqua on their platforms). Today they run headless under Basic and Metal.
4. Heap deltas are documented per leaf (leaf 3 +4 MB and leaf 4 +4 MB per 10^6 JList rows; leaf 5 about 190 bytes per
   expanded path).
5. The user approves the differences table above.

**Steps (one PR each, recipe first):**
1. **No behaviour change.** Move the four copies of `ENABLED` (in `M3ListRows`, `M3VariableHeightLayoutCache`,
   `M3FixedHeightLayoutCache` and `M3ExpandedState`) into one `sun.swing.M3Viewport.ENABLED`, read once.
2. **CI.** Run step 2 of the exit criteria with the property true. Fix or gate every new failure.
3. **The look-and-feel matrix** for the differentials (exit criterion 3).
4. **The flip.** `ENABLED = !"false".equals(property)`. `-Dm3.swing.viewport=false` is the kill switch: it restores
   every stock path. Release note: what changed, the heap deltas, the differences table, the kill switch.
5. **Only if CI finds a leaf-specific regression:** one switch per leaf, `m3.swing.viewport.<leaf>=false`, so the
   other leaves stay on while it is fixed. Not proposed otherwise; one switch is simpler to support.
