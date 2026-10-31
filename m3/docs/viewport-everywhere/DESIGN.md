<!-- SPDX-License-Identifier: Apache-2.0 -->
# Viewport everywhere: one toolkit-neutral core in M3JDK, thin adapters per toolkit

Status: design (D2C), 2026-10-18. Code follows this page in the same branch (`m3/viewport-everywhere-20261018`).
What ran and what did not is recorded in section 9; anything not listed there has not run.

User directive (2026-10-18): "Everything enhanced with viewport logic: SWT, Nebula, RWT, IntelliJ, SwingX, AWT...
everything now has a viewport. Skija will help us map across the libs. Viewport UI is king. M3JDK is the superset
anyway." Follow-up: "if you have collections shaped for the viewport you can abstract them out and M3JDK can use
those too".

## 1. Shape

```
                 +-------------------------------------------------------------+
 toolkit-neutral |  m3/collections  (com.m3.collections, Apache-2.0)            |
 data structures |  M3OrderStatisticForest  M3SparseExtents  M3ColumnarLanes    |
                 |  M3BitLanes  M3MaxIntLane  M3LaneGrowth  M3HeapGrowth  ...    |
                 +------------------------------^------------------------------+
                                                | depends on
                 +------------------------------+------------------------------+
 toolkit-neutral |  m3/viewport-core  (com.m3.viewport, Apache-2.0)              |
 viewport logic  |  M3ViewportRuntime (row window, selection algebra, scroll     |
                 |  fixed point)  M3ScrollMetrics  M3RowWindowPlan              |
                 |  M3TreeTopology  M3TreeProjection  M3TreeResidentPlan        |
                 |  M3TreeViewport  M3ViewportPlanner  M3SelectionModel  ...    |
                 |  adapter contract: M3RowHost, M3RowEvents, M3RowPainter      |
                 +--------^-----------^-----------^-----------^-----------^-----+
                          |           |           |           |           |
                  SWT/Nebula      AWT List     Swing/SwingX  IntelliJ     RWT (RAP)
                  (in the SWT     (peer-safe   (M3JDK Swing  JBList/      (server-side
                  fork; source    model,       DOOP caches,  JBTree       window, client
                  of the core)    opt-in)      #1001)        adapter jar) rows)
                          \___________\___________|___________/___________/
                                          M3 renderer (row painter)
                     Java2D backend (Swing/AWT/IntelliJ) | M3 drawn-mode backend (SWT/Nebula)
```

- **Toolkit-neutral means no toolkit type.** Nothing in `m3/collections` or `m3/viewport-core` imports `java.awt`,
  `javax.swing`, `org.eclipse.swt`, `com.intellij` or a RAP type. Rows are int coordinates; handles are `long`.
- **DOOP (brief rule 18).** State lives in lanes: `int[]`/`long[]` lanes, `int`-word bitsets, Fenwick and
  order-statistic indexes. Toolkit objects (TreeItem, ListItem peers, Swing nodes, JBTree paths, RAP items) are thin
  lazy facades made only when a toolkit API asks, identified by an int id, released under pressure.
- **CPU/UI split (rule 5).** The core does O(window) work per frame and O(log n) per structural edit. Nothing in it
  walks all logical rows on a paint or scroll path.

## 2. Inventory of the source (SWT integration branch)

Source: hsoliwal/eclipse.platform.swt `m3/viewport-integration-20261016` at `553eefd41d9961d303c41e0e4e181712e64a1e45`
(PR #203), directory `bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/{widgets,internal}`. Every commit
touching the candidate files is by Hitesh Soliwal (30 commits, `git log --format=%an`, 2026-10-18). Blob ids are in
`provenance-viewport-20261018.json` beside each target module and in `FOSS_REUSE_DECISION.tsv` here.

Code dependencies inside the set (comments excluded, `grep -w`):

| SWT class | lines | depends on (inside the set) | toolkit references |
|---|---:|---|---|
| VirtualTreeSiblingIndex | 281 | none | none |
| VirtualRowExtents | 348 | PackedHeapGrowth | none |
| VirtualCellLanes | 268 | VirtualGrowth | none |
| VirtualGrowth | 34 | PackedHeapGrowth | none |
| PackedHeapGrowth (internal) | 102 | none | none |
| VirtualBitLanes | 301 | VirtualGrowth | `SWT.error` (8 sites) |
| VirtualListWidth | 367 | VirtualListModel.growth (2 sites) | none |
| VirtualItemStorage | 320 | none | none |
| VirtualScrollMetrics | 162 | none | none |
| VirtualRowWindowPlan | 176 | none | none |
| ViewportRuntime (internal) | 736 | none | none |
| VirtualSelectionModel | 318 | VirtualBitLanes, ViewportRuntime | none |
| VirtualTreeTopology | 644 | VirtualTreeSiblingIndex, VirtualItemState | none in code |
| VirtualItemState | 49 | none | none |
| VirtualTreeVisibleProjection | 471 | VirtualTreeTopology, VirtualItemState | none |
| VirtualTreeResidentPlan | 254 | VirtualTreeVisibleProjection | none (uses `HashMap<Long,Integer>`) |
| VirtualTreeViewport | 247 | VirtualScrollMetrics, VirtualTreeVisibleProjection, ViewportRuntime | none |
| VirtualViewportPlanner | 356 | VirtualScrollMetrics, VirtualSelectionModel, ViewportRuntime | none |
| VirtualListModel | 333 | VirtualListWidth, VirtualViewportPlanner | `SWT.` in docs/errors |
| ViewportScrollLayout | 76 | ViewportRuntime | none |
| ViewportScrollBarGeometry / ViewportPlaneGeometry | 101 / 93 | none | none |

Nebula (hsoliwal/nebula `m3/grid-viewport-20261016`, `eda708d14c`), already `Apache-2.0 OR EPL-2.0` Synexia files,
typed to toolkit classes: `GridRowTree` (601, `AbstractList<GridItem>`), `GridSelectionList` (266,
`AbstractList<GridItem>`), `GridCellSelectionList` (186, `ArrayList<Point>`).

M3JDK Swing (hsoliwal/M3jdk21 `m3/swing-viewport-20261017`, PR #1001): `sun.swing.tree.M3VariableHeightLayoutCache`
and `M3FixedHeightLayoutCache` (GPLv2+CPE, `src/java.desktop`); seams documented in that branch's
`m3/docs/swing-viewport/DESIGN.md` section 7a. Already on master: `m3/ports/swing-viewport` (Apache-2.0 Swing/AWT
packed models, `M3DesktopViewportAdapters`, the item-residency engine under the `EXPLICIT_RELICENSE_PORT` record) and
`m3/ports/jfx-viewport`.

## 3. The split (user, 2026-10-18)

### 3a. Collection-shaped owners go to `m3/collections` (package `com.m3.collections`)

General-purpose data structures, arrays + offsets + masks, no entry objects. The collections-superset agent lists them
in its five-provider INVENTORY as CLAIMED by viewport-everywhere; they do not duplicate its types.

| Wave | Source | M3 owner | What it is | Route |
|---|---|---|---|---|
| 1 (done) | VirtualTreeSiblingIndex | **M3OrderStatisticForest** | one AVL order-statistic tree per parent over borrowed `int[]` key and `long[]` weight lanes; nodes are the caller's int ids (no node objects); find/lower/insert/remove/adjust/prefix/total/seek | rename recipe |
| 1 (done) | VirtualRowExtents | **M3SparseExtents** | variable extents over an implicit estimate: sorted measured ids + Fenwick tree of signed deltas; offsetOf/rowAt/visibleRange/anchored resize | rename recipe |
| 1 (done) | VirtualCellLanes | **M3ColumnarLanes** | per (field, column) lazy lanes indexed by row; lane allocated on its first non-default value; row/column insert and remove move lanes | rename recipe |
| 1 (done) | VirtualGrowth | **M3LaneGrowth** | the one growth rule for packed lanes | rename recipe |
| 1 (done) | PackedHeapGrowth | **M3HeapGrowth** | heap-aware capacity choice, sampled once per miss | rename recipe |
| 2 (done) | VirtualItemStorage | **M3SparseItemStore** | sparse identity-preserving store of materialized entries with shifting logical indices | rename recipe |
| 1 (done) | VirtualBitLanes | **M3BitLanes** | per-row bit lanes in `int` words, lazy, word-wise range ops and shifting | rename + `SWT.error` -> `IllegalArgumentException` recipe |
| 2 (done, in core) | VirtualListWidth | **com.m3.viewport.M3ListWidth** | incremental widest-item index over an `int` lane with lazy slots | rename recipe; kept in viewport-core because it shares the list model's replaceable growth hook (a move into collections would split that hook) |
| 2 | VirtualTreeResidentPlan map | (reuse) **M3LongLongHashMap** | the `(parentId, childIndex) -> slot` and `handle -> slot` maps, keyed by `key(parentId, childIndex)`; replaces two boxed `HashMap<Long,Integer>` | recipe on the core class; no new type |
| 3 | Nebula GridRowTree | **M3RowTree** | subtree size, visible count, pixel extent per node | rename + generify (`GridItem` -> type parameter) |
| 3 | Nebula GridSelectionList | **M3PositionMarkedSelection** | O(1) position-marked selection list | rename + generify |
| 3 | Nebula GridCellSelectionList | **M3CellSelectionCounts** | per-cell selection with per-column counts | rename + replace `Point` with packed `long` |
| 4 | Swing layout caches (#1001) | **M3FenwickForest** | per-node Fenwick trees for rows and heights | GPLv2+CPE source: needs the rights holder's relicence record first (as for item residency), else re-derived from M3SparseExtents |

Existing owners reused instead of new types: `M3LongLongHashMap` (wave 2), `M3IntIntHashMap`, `M3IntLongHashMap`,
`M3BitLane28` (block-lazy flags; M3BitLanes differs: dense row-indexed lanes with insert/remove shifting),
`M3SpanIndex` (Grid row spans, already extracted). Each new owner gets a `JDK_FAMILY_MAP.tsv` row (`VIEWPORT_EXTRACTED_V1`)
and a `name-mapping.json` fragment (`m3/collections/recipe/viewport-everywhere-name-mapping.json`).

### 3b. UI-specific logic goes to `m3/viewport-core` (package `com.m3.viewport`)

| Wave | Source | M3 class |
|---|---|---|
| 1 | ViewportRuntime | M3ViewportRuntime (row window with overscan, selection range algebra, scroll-bar fixed point) |
| 1 | VirtualScrollMetrics | M3ScrollMetrics (long logical rows projected onto int scroll ranges) |
| 1 | VirtualRowWindowPlan | M3RowWindowPlan (flat resident window diff: O(1), allocation-free) |
| 1 | VirtualItemState | M3ItemState (flag masks) |
| 1 | VirtualTreeTopology | M3TreeTopology (sparse columnar topology, sibling index = M3OrderStatisticForest) |
| 1 | VirtualTreeVisibleProjection | M3TreeProjection (visible-row projection over the materialized frontier) |
| 1 | VirtualTreeResidentPlan | M3TreeResidentPlan (diff planner: keep/retire/insert by logical coordinate) |
| 1 | VirtualTreeViewport | M3TreeViewport |
| 1 | ViewportScrollLayout, ViewportScrollBarGeometry, ViewportPlaneGeometry | M3ScrollLayout, M3ScrollBarGeometry, M3PlaneGeometry |
| 1 (done) | VirtualSelectionModel | M3SelectionModel |
| 1 (done) | VirtualViewportPlanner | M3ViewportPlanner |
| 2 (done) | VirtualListModel, VirtualListWidth | M3ListModel, M3ListWidth |

The SWT fork keeps its classes. Later, an SWT recipe can switch SWT to the M3 owners; the core is a copy, not a move.

### 3c. Recipe route (rule 3, rule 14, rule 20)

`m3/docs/viewport-everywhere/recipe/` holds:
- `META-INF/rewrite/m3-viewport-everywhere.yml`: `org.openrewrite.java.ChangePackage` and `ChangeType` per row of 3a/3b,
  plus two local recipes;
- `M3ApacheHeader` (replaces the leading licence comment with the m3/** Apache-2.0 header and a provenance line naming
  the source path and blob) and `M3PublicSurface` (top-level class and its package-private constructors, methods and
  nested types become `public`; fields and private members are untouched), because the core is consumed across
  modules;
- `M3BatchApply`: parses one set of files together (so cross references inside the set are typed), runs the
  composite recipe, writes the targets and prints `CHANGED`/`UNCHANGED` per file. Pass 2 on the output must print
  `UNCHANGED` for every file.

Tests: the SWT JUnit tests for these classes (`Test_VirtualTreeOrderStatistic`, `Test_VirtualRowExtents`,
`Test_VirtualCellLanes`, `Test_VirtualRowWindowPlan`, `Test_VirtualTreeResidentPlan`, `Test_VirtualTreeTopology`,
`Test_ViewportRowWindow`, `Test_ViewportSelectionShifts`, ...) go through the same recipe. A test that needs a
`Display` or SWT reflection stays in SWT and is listed as not ported, with the reason.

## 4. Core API (m3/viewport-core)

| Concern | Owner | Contract |
|---|---|---|
| Resident window | M3ViewportRuntime.RowWindow, M3RowWindowPlan, M3TreeResidentPlan | `[first, first+size)` plus overscan; a move answers rows to delete (head/tail) and rows to insert, never per-row objects; a tree window is keyed by `(parentId, childIndex)`, rows present before and after keep their native handle |
| Topology lanes | M3TreeTopology | int ids; parent, child index, child count, flags, visible-extra-rows lanes; cold children are arithmetic, no slot |
| Row math | M3OrderStatisticForest, M3SparseExtents | row of a node = prefix over the sibling forest along ancestors; row at offset = Fenwick descent; O(log n) |
| Residency / LRU | item-residency engine (`m3/ports/swing-viewport` engine, `M3ObjectResidency`) | facades are parent-owned, pinned while exposed, recycled under pressure |
| Diff planner | M3TreeResidentPlan.plan/commit, M3RowWindowPlan.move | minimum edit between old and new window; allocation-free on the hot path |
| Generation checks | `long` handle + generation in the adapter's handle lane | a facade's native handle is validated before use (handles are reused natively); stale = re-resolve by logical coordinate |
| Selection | M3SelectionModel (range pairs + bitset hybrid) | O(1)/O(ranges) select-all, shift-range, invert; shift on insert/remove |
| Scroll | M3ScrollMetrics, M3ScrollLayout | logical `long` rows; projection onto `int` native scroll ranges; scroll-bar fixed point |

## 5. Adapter contract (what each toolkit supplies)

```java
/** Native or peer row operations; O(window) per frame. */
interface M3RowHost {
	long insertRow (long parentHandle, long afterHandle, int row);   // returns native handle (0 = none)
	void deleteRow (long handle);                                    // subtree goes with it
	void bindRow (long handle, int row);                             // push text/image/check state from lanes
	void setScroll (int selection, int maximum, int thumb);          // logical scroll bar
	boolean isHandleLive (long handle, int generation);              // generation check
}
/** Toolkit events mapped to logical rows. */
interface M3RowEvents {
	void expanded (int row, boolean expanded);
	void selected (int row, int modifiers);
	void scrolled (long topRow);
	void resized (int viewportWidth, int viewportHeight);
}
/** Paint hook: drawn modes paint rows themselves (section 6). */
interface M3RowPainter {
	void paintRow (int row, int x, int y, int width, int height, int stateMask);
}
```

These three interfaces are the only toolkit seam; they are added in wave 1 as new files (they have no SWT source).

## 6. M3 renderer mapping (the cross-library row painter)

"Skija will help us map across the libs": one row painter is implemented once per drawing backend and shared by
every toolkit. Our code is M3-named (rule 14); the third-party Skia binding stays behind the one internal M3 adapter
on the render-modes branch (`m3/render-modes-20261017`, SWT PR #202, read-only reference).

| Backend | Implements `M3RowPainter` over | Used by |
|---|---|---|
| M3 drawn-mode backend | the render-modes M3 plane backend (`M3GC`, `M3Surface`) | SWT, Nebula (`-Dswt.widget.mode=m3`) |
| Java2D | `Graphics2D` clip -> row range (as `BasicTableUI` paints), damage = row rectangles | Swing, SwingX, AWT, IntelliJ |
| RWT | no pixels: rows serialized to the client; the painter is the client widget | RAP |

The painter takes a theme (rule 16): colours, fonts, metrics, glyphs. A row is painted from lanes (text, image id,
state mask); no toolkit item object is needed to paint.

## 7. Capability matrix (toolkit x widget)

Legend: **V** viewport exists (proven by recorded differential), **P** planned adapter (this programme), **D** design
only / donor pattern, **-** no such widget.

| Toolkit (licence) | list | tree | table | grid | text |
|---|---|---|---|---|---|
| SWT (EPL-2.0, our fork) | V (List, #199) | V (Tree, #196) | V (Table, #200) | - | V (StyledText proof) |
| Nebula (EPL-2.0) | - | V (Grid tree rows) | V (Grid) | V (Grid, nebula#234/#235) | - |
| AWT (GPLv2+CPE, java.desktop) | P wave 1: `java.awt.List` model, `-Dm3.awt.viewport=true` | - | - | - | D (`TextArea` peer) |
| Swing (GPLv2+CPE) | V (`m3/ports/swing-viewport` M3PackedList) | V (M3VariableHeight/FixedHeightLayoutCache, #1001) | V (M3PackedTable) | - | D (GapContent pattern) |
| SwingX (LGPL-2.1: pattern only) | P (JXList as M3 Swing adapter) | P (JXTree/JXTreeTable) | P (JXTable) | - | - |
| IntelliJ platform (Apache-2.0) | P (JBList adapter jar) | P (Tree/TreeTable adapter) | P (JBTable) | - | D (editor fold/soft-wrap prefix arrays) |
| RWT / RAP (EPL) | P (List) | P (virtual Tree: server window, client rows) | P (virtual Table) | - | - |
| JavaFX (GPLv2+CPE) | D (`m3/ports/jfx-viewport`) | D | D | - | - |

## 8. Gates and phases

- **Phase 1 (this branch):** this page and `FOSS_REUSE_DECISION.tsv` first; then wave 1 of 3a and 3b by recipe; gate:
  `-Xlint:all,-module -Werror` compile of both modules, ported tests pass, JaCoCo line and branch >= 99% (report in
  `m3/viewport-core/coverage/` and `m3/collections/qualification/viewport-everywhere-20261018/`).
- **Phase 2:** adapters, smallest first, each a draft PR with a recorded stock-vs-viewport differential:
  1. AWT `java.awt.List` (peer-safe model, `--patch-module` proof);
  2. SwingX JXList/JXTree/JXTable as M3 Swing adapters over the core, reusing the #1001 caches (that branch is not
     edited);
  3. IntelliJ JBList/Tree adapter design + minimal adapter jar (APIs read with `gh api`, no clone);
  4. RWT virtual Tree/Table adapter design;
  5. M3 renderer: `M3RowPainter` over the M3 drawn-mode backend and over Java2D.

## 8a. Donor pattern evidence (UI donor-atomize run)

Source: hsoliwal/com.synexia draft PR #12398, branch `codex/m3-ui-donor-atomize-20261017`,
`docs/M3-SCALE/runs/20261017-ui-donor-atomize/PATTERN_MAP.md`: 69 donors, including IntelliJ UI (459 files,
`06e20852`), RAP RWT (192 files, `b0830aae`, EPL-2.0 file headers) and Skiko (hash-only). Every row there is a CPU
projection (identifier theme x L-Hash structural key x DOOP lexical shape); keys are candidate evidence only.

| Theme | DOOP-shaped keys / all | What it means for the core |
|---|---|---|
| prefix_sums | 24 / 51 | the most lane-shaped theme (own code 22, SWT 9, IntelliJ 6, JDK desktop 5): lift the Fenwick/prefix-sum owners first (M3SparseExtents, M3OrderStatisticForest in wave 1; M3FenwickForest in wave 4) |
| expansion_maps | 2 / 62 | expansion and tree-model events are object graphs in every toolkit; `M3TreeTopology` (lane-shaped expansion, cold children as arithmetic) and the `(parentId, childIndex)` resident lanes are the gap this core fills |
| damage_blit | 0 / 46 | no lane-shaped donor anywhere: the resident-window blit and damage math (`M3RowWindowPlan` head/tail deltas, `M3ScrollMetrics`, row-rectangle damage in `M3RowPainter`) is a first-of-its-kind core component, designed here, not adapted |
| visible_range | 7 / 74 | 6 of 7 DOOP keys include our own code; `M3ViewportRuntime.RowWindow` is the reference |
| selection_ranges | 5 / 154 | `setSelection`/`isCellSelected`/`deselectCell` share one key across SWT, Nebula, RAP and SWTSwing: `M3SelectionModel` is the common target |
| lazy_facades | 10 / 85 | `getItem`/`getCell`/`release*` keys (SWT, RAP, NatTable, SwingX): the residency engine plus int-id facades |

**Adapter checklist** (the strongest cross-family keys per theme; each adapter maps every listed seam onto the core
or records why not):

| Seam (method names from the pattern map) | AWT | SwingX | IntelliJ | RWT |
|---|---|---|---|---|
| visible range: `scrollPathToVisible`, `showSelection`, `setFillsViewportHeight`, `updateVisibleRows`, `getViewLocation` | `makeVisible` | JXTree/JXList | Tree, JBList | Tree/Table `showSelection` |
| prefix sums: `convertRowToY`, `getVisibleChildCount`, `ceilingRow`/`higherRow` | row -> y on the peer | JXTreeTable row y | `getPositionInWorld`-style row math | flat index + preload window |
| expansion: `treeNodesInserted/Removed/Changed`, `treeStructureChanged`, `nodeForRow`, `rowCount` | - | TreeModel events -> topology edits | Tree model events | server-side expansion -> client rows |
| selection: `setSelection`, `isCellSelected`, `deselectCell`, `fireSelectionChanged` | `select`/`deselect` | ListSelectionModel bridge | JBList selection | Table/Tree selection |
| lazy facades: `getCellRenderer`, `getItemAsString`, `releaseChildren`, `getCellByPosition` | item strings from lanes | renderer stamp from lanes | renderer stamp | virtual items |
| damage: `repaintPath`, `extendDirtyRegion`, `invalidate` | row-rect repaint | row-rect repaint | row-rect repaint | client redraw of changed rows |
| theme: `installDefaults`, `getFont`, `decorateColumnHeader`, `applySpeedSearchHighlighting` | - | highlighters as theme | LaF + speed search | theming via CSS |

## 9. Run record

| Step | Command | Result |
|---|---|---|
| inventory | `git ls-tree`, `git show`, `grep -w` over the SWT integration branch | 29 candidate files, 2 internal; dependency table above |
| authorship | `git log --format=%an` on the candidate paths | 30 commits, all Hitesh Soliwal |
| wave 1 recipe | `M3BatchApply` with `Wave1Main` / `Wave1Tests`, then pass 2 | 21 main classes + 16 test files; pass 2 UNCHANGED 58/58 |
| wave 1 build | local reactor `mvn -o verify`, `-Xlint:all,-module -Werror` | BUILD SUCCESS; 151 tests after the direct tests, 0 failures |
| wave 1 coverage | JaCoCo 0.8.15 agent | 99.96% lines, 99.03% branches (reports were on G:, flagged for regeneration; see `m3/viewport-core/coverage/COVERAGE.md`) |
| wave 2 recipe | same recipe, 3 more classes + 2 tests | pass 2 UNCHANGED 66/66; build 161 tests, 0 failures; coverage not yet measured on a healthy location |

## 11. Fold into m3/collections (user, 2026-10-19)

User: "if you want to fold all of your viewport logic into collection, go ahead". This supersedes the 3a/3b split.
The viewport logic becomes general collection views in `com.m3.collections`, usable without any UI. `m3/viewport-core`
keeps only the toolkit seam (`M3RowHost`, `M3RowEvents`, `M3RowPainter`) and thin glue. An adapter is now collection
views plus a host.

**Step 1: move by recipe** (`ChangeType` rows `com.m3.viewport.X -> com.m3.collections.Y`; provenance kept; pass 2
UNCHANGED).

| Now (`com.m3.viewport`) | After the fold (`com.m3.collections`) |
|---|---|
| M3ViewportRuntime (RowWindow, Selection, scroll fixed point) | M3WindowRuntime |
| M3RowWindowPlan | M3WindowPlan |
| M3SelectionModel | M3RangeSet |
| M3TreeResidentPlan | M3TreeWindowPlan, with its two boxed `HashMap<Long,Integer>` replaced by M3LongLongHashMap keyed `parentId << 32 \| childIndex` |
| M3TreeViewport | M3TreeWindow |
| M3ViewportPlanner (+ HorizontalWindow) | M3WindowPlanner |
| M3TreeTopology, M3TreeProjection, M3ItemState, M3ListModel, M3ListWidth, M3ScrollMetrics, M3ScrollLayout, M3ScrollBarGeometry, M3PlaneGeometry | same names |

**Step 2: general views** (new code, only the gap; each with JUnit and oracle tests; arrays + offsets + masks, no
entry objects). They compose, so a view can wrap another view:

| View | What | Built on |
|---|---|---|
| M3WindowView | sliding window `[first, first + size)` over any indexed sequence, with a residency mask lane; a move answers the head/tail delta | M3WindowPlan, M3BitLanes |
| M3FlatteningView | tree-to-list flattening with expand masks over the order-statistic forest; also a filtered view | M3TreeTopology, M3TreeProjection, M3OrderStatisticForest |
| M3ListDiff / M3Patch | diff between two keyed snapshots, as remove/insert/move runs (diffable-snapshot pattern; rule 21 catalogue) | sorted int lanes, longest increasing subsequence |
| M3BoundedCache | bounded cache with clock (second-chance) reference bits | int/long lanes, M3BitLanes |
| M3RangeSet | selection as ranges with a bitset fallback | the folded M3SelectionModel |
| M3SparseExtents | scroll geometry (done) | Fenwick |

The collections-superset INVENTORY lists all of these as first-party viewport types CLAIMED by viewport-everywhere
(`m3/collections/recipe/viewport-everywhere-20261018/CLAIMED.tsv`).

**Consumers re-pointed:**
- SWT Tree/Table/List: an M3WindowView or M3TreeWindow plus an SWT `M3RowHost`.
- Nebula Grid: M3RowTree, M3PositionMarkedSelection and M3CellSelectionCounts.
- Swing/SwingX/IntelliJ: M3FlatteningView plus M3SparseExtents for layout.
- AWT: M3WindowView over the item lane.
- RWT: M3WindowView over the server-side flattening.
