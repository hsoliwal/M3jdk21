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
2. `JTree` expansion table: `getExpandedDescendants` and `removeDescendantToggledPaths` iterate the whole table on
   every expansion; an expanded-path index keyed by parent removes the O(expanded) factor.
3. `BasicTreeUI` paint by row index (inventory OH18 of `m3-swing-awt-viewport.md`): one `TreePath` per painted row only
   where `paintRow` needs it.
4. JList: prefix sums over `cellHeights` for `getCellBounds` and `convertLocationToRow` (identical answers, O(log n)),
   guarded so that subclasses that write `cellHeights` keep the stock loops; measure-on-demand for `updateLayoutState`.
5. Fix D2 in `DefaultTreeSelectionModel` in the M3 mode, with its own differential.
6. Default-on switch, together with the SWT `swt.widget.mode` convention, once CI has run make and jtreg.
