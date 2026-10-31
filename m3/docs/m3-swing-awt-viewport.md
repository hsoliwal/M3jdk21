<!-- SPDX-License-Identifier: Apache-2.0 -->
# Swing, SwingX and AWT under the M3 viewport laws

Status: design (D2C), leaf 1 code with tests and scale runs, and leaf 2 `java.desktop` changes with reproducers.
What ran and what did not is in `m3/ports/swing-viewport/TESTS_TO_RUN.md`.

The laws are the ones recorded for SWT and Nebula in Synexia PR #10921
(`docs/M3-SCALE/runs/20261013-all-widget-viewport/CONSOLIDATION_20261013.md`). This page maps each one onto Swing
(`javax.swing`), AWT (`java.awt`) and SwingX, and names the code that realizes it.

## 1. Licence boundaries

| Area | Licence | Rule |
|---|---|---|
| `src/java.desktop/**` (Swing, AWT) | GPLv2 with Classpath Exception | Changes are TARGET_OWNED_PORT. No Apache code is pasted in. Engine copies into `java.desktop` are EXPLICIT_RELICENSE_PORT under `m3/docs/m3-item-residency-explicit-relicense.md` (PR #826). |
| `m3/ports/swing-viewport/**` | Apache-2.0 | New first-party code. The engine under `engine/` is `EPL-2.0 OR Apache-2.0`; this module takes Apache-2.0. |
| SwingX (`3dcitydb/swingx-all@94f86aab`) | LGPL-2.1 | Design notes only (section 9). No SwingX code is copied into Apache or GPL files. SwingX is not vendored under `m3/vendor` (checked). |
| OpenJDK `Default*Model` classes | GPLv2 with Classpath Exception | Runtime differential oracles in tests only. No code copied. |

## 2. Inventory (CPU, `rg`)

`m3/ports/swing-viewport/inventory/PATTERNS.tsv` holds 44 patterns. `inventory.sh` turns them into
`INVENTORY.tsv`: 73 rows, each with file, line, the file's SHA-256 prefix and the matched text. The scan covers
`src/java.desktop/share/classes` at M3jdk21 `736ae65b1b9f`. A pattern that stops matching emits a MISSING row, so
drift shows up on the next run.

### 2.1 Object-heavy sites (law 2, state and data; law 4, residency)

| Id | Site (file:line) | What is object-heavy |
|---|---|---|
| OH01 | `javax/swing/table/DefaultTableModel.java:74` | `Vector<Vector> dataVector`: one Vector per row, one boxed Object per cell |
| OH02 | `DefaultTableModel.java:99`, `:696`, `:713` | a new Vector per added, inserted or converted row |
| OH03 | `javax/swing/tree/DefaultMutableTreeNode.java:117` | one node object plus a children Vector per node |
| OH04 | `DefaultMutableTreeNode.java:202` | children Vector created on first insert |
| OH05 | `DefaultMutableTreeNode.java:640` | `getPath` allocates a `TreeNode[]` per call |
| OH06 | `javax/swing/tree/DefaultTreeModel.java:441`, `:446` | `getPathToRoot` allocates a `TreeNode[]` per event |
| OH07 | `javax/swing/DefaultListModel.java:58` | synchronized `Vector<E>` |
| OH08 | `javax/swing/tree/VariableHeightLayoutCache.java:1012` | `TreeStateNode extends DefaultMutableTreeNode` per visible node |
| OH09 | `VariableHeightLayoutCache.java:97` | every visible row held in a `Vector<Object>` |
| OH10 | `VariableHeightLayoutCache.java:1049`, `:1309` | a `TreePath` kept per state node |
| OH11 | `javax/swing/tree/FixedHeightLayoutCache.java:747` | `FHTreeStateNode` per expanded node |
| OH13 | `javax/swing/JTree.java:249` | expansion as `Hashtable<TreePath, Boolean>` |
| OH14 | `JTree.java:720`, `:3205` | `Stack<Stack<TreePath>>` |
| OH15 | `javax/swing/tree/DefaultTreeSelectionModel.java:84` | selection as `TreePath[]` |
| OH16 | `DefaultTreeSelectionModel.java:124`, `:125` | two `Hashtable<TreePath, Boolean>` |
| OH17 | `DefaultTreeSelectionModel.java:126`, `:197`, `:244`, `:254`, `:319`, `:354`, `:442`, `:949`, `:1004`, `:1058` | per-call `new TreePath[1]` |
| OH25 | `javax/swing/plaf/basic/BasicListUI.java:1425`, `:1460` | `int[]` of all row heights, rebuilt per layout |
| OH26 | `java/awt/List.java:130` | `Vector<String>` mirrored into the native peer |
| OH27 | `java/awt/TextComponent.java:83` | the whole text as one `String`, plus the peer's copy |
| OH28 | `java/awt/TextArea.java:315` | insert goes through the peer |
| OH29 | `java/awt/Canvas.java:147` | heavyweight clear-and-paint through the peer |

### 2.2 Paint materializes (law 6, redraw)

| Id | Site | What paint creates |
|---|---|---|
| OH12 | `FixedHeightLayoutCache.java:1468`, `:1526` | a `TreePath` per non-expanded visible row while enumerating |
| OH18 | `javax/swing/plaf/basic/BasicTreeUI.java:1354` | `getVisiblePathsFrom`: one `TreePath` per painted row |
| OH19 | `BasicTreeUI.java:1379` | a `Rectangle` per paint |
| OH20 | `BasicTreeUI.java:1370`, `:1407` | `Hashtable` insert per ancestor path per paint |
| OH21 | `javax/swing/JTable.java:5801`, `:5872` | `getValueAt` per painted cell (a boxed Object for primitive data) |
| OH22 | `javax/swing/plaf/basic/BasicTableUI.java:2187` | `prepareRenderer` per cell |
| OH23 | `BasicListUI.java:270` | `getElementAt` per painted row |
| OH24 | `BasicListUI.java:1442`, `:1462` | `updateLayoutState` measures every row, which materializes every element, unless both fixed cell width and height are set |

### 2.3 Work while not visible (law 3)

| Id | Site | Work done while hidden |
|---|---|---|
| WH01 | `javax/swing/plaf/basic/BasicProgressBarUI.java:1347` | the indeterminate timer stops on DISPLAYABILITY_CHANGED only; it ticks while hidden but displayable (for example, a background tab) |
| WH02 | `JTable.java:4435`, `:6924` | `tableChanged` resizes, revalidates and repaints whether or not the table is showing |
| WH03 | `BasicListUI.java:957`, `:2763`, `:2786`, `:2806` | list data events invalidate layout and revalidate whether or not the list is showing |
| WH04 | `javax/swing/RepaintManager.java:878` | each dirty root builds an `AccessControlContext` and a `PrivilegedAction` before `paintImmediately` tests `isShowing` |
| WH05 | `VariableHeightLayoutCache.java:371` | `invalidateSizes` re-measures every visible node through the renderer, showing or not |
| WH06 | `java/awt/Component.java:3491` | a heavyweight repaint tests the component's own `isVisible`, not `isShowing`, before posting a `PaintEvent` |

### 2.4 What Swing already has (reuse, do not rebuild)

| Id | Site | Asset |
|---|---|---|
| EX01 | `RepaintManager.java:944` | `collectDirtyComponents` clips each dirty rectangle by every ancestor: visible ∩ damaged is already the rule |
| EX02 | `RepaintManager.java:461` | add-time visibility gate over all ancestors, iconified frames included |
| EX03 | `RepaintManager.java:639` | per-component dirty-region coalescing |
| EX04 | `FixedHeightLayoutCache.java:52` | a lazy layout cache: objects only for expanded nodes, rows by arithmetic |
| EX05 | `javax/swing/JViewport.java:191` | blit scrolling: only the exposed strip is damaged |
| EX06 | `javax/swing/CellRendererPane.java:73` | the renderer flyweight: one component stamps every cell |
| EX07 | `javax/swing/SizeSequence.java:125` | packed `int[]` prefix structure for JTable row heights |
| EX08 | `javax/swing/JList.java:682`, `:759` | prototype or fixed cell size removes the all-rows measure (OH24) |
| EX09 | `JTree.java:1079` | `setLargeModel` selects `FixedHeightLayoutCache` |

## 3. The laws on Swing and AWT

| Law | Swing/AWT today | M3 realization |
|---|---|---|
| 1. One control | `JScrollPane` + `JViewport` wrap the view; scrollbars are not focusable | The component stays the one control. `JViewport` and the scrollbars are helper surfaces without focus. The models take viewport runs from the view's visible rectangle (`M3ViewportBinding`), so no M3 type wraps a component. |
| 2. State and data | Vectors, node objects, `TreePath` hashes (OH01-OH17) | State in engine lanes (`ItemLanes`: ARRAY, BITMAP and RUN containers plus patterns), keyed by row or node key. Data in columnar primitive arrays grown through `PackedHeapGrowth`. Objects only when a caller's API asks. |
| 3. Visibility and damage | EX01 and EX02 already give visible ∩ damaged; WH01-WH06 are gaps | Leaf 2 closes WH04. WH01, WH02, WH03, WH05 and WH06 are listed as next leaves (section 8). |
| 4. Residency | Layout caches keep node objects (OH08-OH12) | Proxies (section 4). Source-backed text lives in engine payload slots: HOT for the viewport plus overscan, WARM under CLOCK, COLD in the ported swap store, or DESTROYED and re-requested. |
| 5. SetData | No equivalent; `TableModel.getValueAt` is pull-only | `ItemResidency.DataSource` negotiation: visible, then overscan, then prefetch; batched ranges; cancelled on scroll-out; at most one fill per row per residency. |
| 6. Redraw | Paint fetches an Object per cell or row (OH18-OH23) | Packed renderers read lanes and slots; `M3PackedTable.prepareRenderer` skips `getValueAt`. Model batches coalesce events (`beginBatch` and `endBatch`). Leaf 2 adds the redraw-suspend construction flag to `RepaintManager`. |
| 7. Layout | `BasicListUI` measures every row (OH24) | Packed lists set a fixed cell height (EX08) from one prototype measure. JTable already uses `SizeSequence` (EX07). |
| 8. Scale | DefaultTreeModel at 1B nodes needs about 100 GB | Targets are the engine's measured numbers (section 7). |
| 9. Platform | Java2D renders through the D3D, OpenGL or Metal pipelines | Only rendering is GPU or Java2D. Sort, filter, swap and data structures are CPU. |

## 4. Proxies (the residency contract applied to Swing)

Every `TreeNode`, row or `TreePath` component handed to a caller is a proxy `(model, key, generation)`. It holds no
state: each call forwards to the model's packed lanes and arrays.

- **The key is a logical model index, never a payload slot.** Engine slots are recycled under eviction. A pinned proxy
  (generation 0) survives DESTROY, recreate and swap by re-resolving through the engine directory on every call.
- **Explicit trees use stable node ids** that do not shift on insert or remove. A removed node's proxy is rebound to
  key -1: `isCurrent()` is false, structural queries answer as a detached leaf, and it keeps the text it had at
  removal so `toString` stays meaningful in the removal event.
- **Virtual trees and flat models use engine items.** The proxy is `ItemResidency.handle(key)` through a
  `HandleFactory`. Insert and remove rebind pinned proxies (`rebind(proxy, newIndex, 0)`); removed ones get -1.
- **Flyweights** (generation > 0) exist only where the M3IRS01 sidecar proves the call site behaviour-only
  (`IDENTITY_BY_METADATA`). They go stale when rebound or after any insert or remove, and fail with the disposed
  contract; they are never silently re-resolved. The default (`IDENTITY_PIN_ALL`) pins.
- **Equality is identity.** JTree keys `Hashtable`s by `TreePath`, whose `equals` uses node `equals`. Identity equality
  keeps those tables valid across inserts, as with `DefaultMutableTreeNode`.
- **When proxies are made.** `getRoot`, `getChild`, events and `getSelectionPaths` create or return proxies, because
  the caller asked. The M3 paint paths (section 6) create none.

## 5. Leaf 1: `m3/ports/swing-viewport` (Apache-2.0, stock JDK 21)

Package `com.m3.swing.viewport`. The engine is under `com.m3.swing.viewport.engine.{residency,swap}` and
`engine.PackedHeapGrowth`. It was produced by the hash-pinned rename recipe of PR #826
(`m3/tooling/residency-rename`, manifest SHA-256 `1a5abf31dc07…`, source SWT commit `136df9c62ba5`): 21 files, a second
run reported `CHANGED 0 OF 21`, and `javac --release 21 -Xlint:all` is clean.

| Class | Interface | Storage |
|---|---|---|
| `M3PackedListModel<E>` | `ListModel<E>`, the `DefaultListModel` mutation API | `Object[]` grown through `PackedHeapGrowth`; no Vector, no synchronization |
| `M3VirtualListModel` | `ListModel<String>` | `ItemResidency`: text in payload slots, filled by `M3TextSource` through SetData negotiation |
| `M3PackedTableModel` | `TableModel`, the `DefaultTableModel` row API | columnar: `int[]`, `long[]`, `double[]`, `ItemBitSet` for booleans, dictionary-coded `int[]` for strings, `Object[]` for anything else |
| `M3VirtualTableModel` | `TableModel` | `ItemResidency` with one text column per table column |
| `M3PackedTreeModel` | `TreeModel`, the `DefaultTreeModel` mutation API | stable node ids; `parent`, `firstChild`-free child blocks in one `int[]` pool; values in `Object[]` |
| `M3VirtualTreeModel` | `TreeModel` | `ItemTreeShape` (uniform fan-out per depth, keys by arithmetic) plus `ItemResidency` for node text |
| `M3TreeNodeProxy` | `TreeNode` | `(model, key, generation)` only |
| `M3TreeSelectionModel` | `TreeSelectionModel` | `ItemBitSet` over node keys (ranges as RUN containers); lead and anchor as keys; `TreePath`s only when asked |
| `M3TreeRows` | `AbstractLayoutCache` (`RowMapper`) for `M3VirtualTreeModel` | `ItemTreeShape` rows; no `TreePath` per row |
| `M3PackedTable` | `JTable` | `prepareRenderer` binds `M3PackedCellRenderer` to `(row, column)` without `getValueAt` |
| `M3PackedCellRenderer` | `TableCellRenderer`, `ListCellRenderer` | draws from packed columns or resident slot text through a reusable `char[]` |
| `M3ViewportBinding` | `ChangeListener` on `JViewport` | turns the visible rectangle into `setViewport` or `setViewportRuns` |

**Differential proof (written, not run).** Each test drives the M3 model and the OpenJDK default model with the same
seeded operation sequence and compares every public read and every event:

- `M3PackedListModelDiffTest` against `DefaultListModel`: `ListDataEvent` type, index0, index1;
- `M3PackedTableModelDiffTest` against `DefaultTableModel`: `TableModelEvent` firstRow, lastRow, column, type;
- `M3PackedTreeModelDiffTest` against `DefaultTreeModel` over `DefaultMutableTreeNode`: `TreeModelEvent` path values,
  child indices and child values;
- `M3TreeSelectionModelDiffTest` against `DefaultTreeSelectionModel`: selected set, count, lead, rows, and the
  `TreeSelectionEvent` added and removed sets with old and new lead.

Selection order is the one declared divergence. `getSelectionPaths` returns key order; `DefaultTreeSelectionModel`
returns insertion order. `TreeSelectionModel` does not specify an order, so the tests compare sets.

**Scale harness (written, not run).** `M3SwingScale` paints headless into a `BufferedImage`, so no window opens. It
covers JTable and JList at 1M and 10M rows, and JTree at 1M and 1,001,001,000 nodes at `-Xmx256m`. Each run reports:

- heap after GC;
- p50 and p95 frame time;
- bytes allocated per paint frame (`com.sun.management.ThreadMXBean`);
- items materialized per paint (model counters plus `ItemResidency.materializedDuringPaint`, target 0);
- swap-in p95.

## 6. Paint never materializes

The renderer reads packed state:

- **Primitive columns** format into a reusable `char[]` and draw with `Graphics.drawChars`.
- **Dictionary string columns** draw the dictionary's `String` (no allocation).
- **Virtual models** read `ItemResidency.slotText` inside `beginPaint` and `endPaint`. A row without a resident slot
  draws the empty placeholder, and its fill is requested by the viewport binding, never by paint.
- **The tree** paints through `M3TreeRows`, which maps rows to keys by `ItemTreeShape` arithmetic.

Stock `BasicTreeUI` still asks its layout cache for `TreePath`s (OH18); `M3TreeRows` returns them on request, as
proxies. Removing that last path object per row needs the `java.desktop` change in section 8.

## 7. Targets and measured results

These are the engine numbers measured on SWT #122 (`-Xmx256m -XX:+UseSerialGC`, JDK 21.0.9):

| Shape | Heap after GC | Swap-in p95 |
|---|---|---|
| flat, 1M items | 14.7 MB | 34 µs |
| flat, 1B items | 18.9 MB | 16 µs |
| tree, 1,010,100 nodes | 3.6 MB | 1,212 µs (10 samples) |
| tree, 1,001,001,000 nodes | 7.2 MB | 1,107 µs (10 samples) |

**Swing results** (`evidence/SCALE_RESULTS_20261013.tsv`, `M3SwingScale`). Every run is headless: 30 frames of 800x600 at
each of the start, middle and end, after 5 warm-up frames, on win32 x86_64 with JDK 21.0.9 and SerialGC.

| Mode | Items | Heap | Heap after GC | Frame p50 / p95 | Bytes per frame | Materialized per paint |
|---|---:|---|---:|---|---:|---:|
| JTable, virtual (`M3VirtualTableModel`) | 1M | 256m | 5.0 MB | 0.59 / 2.16 ms | 86,448 | 0 |
| JTable, virtual | 10M | 256m | 5.0 MB | 0.77 / 1.35 ms | 86,448 | 0 |
| JList, virtual (`M3VirtualListModel`) | 1M / 10M / 100M | 256m | 4.8 / 4.8 / 5.0 MB | 0.48-0.90 / 1.17-1.68 ms | 37,440 | 0 |
| JTree, virtual (`M3VirtualTreeModel`) | 1,001,000 nodes | 256m | 5.1 MB | 0.34 / 1.05 ms | 29,120 | 0 |
| JTree, virtual | 1,001,001,000 nodes | 256m | 7.0 MB | 0.37 / 1.35 ms | 32,288 | 0 |
| JTable, packed (`M3PackedTableModel`, int + string + boolean) | 1M / 10M | 2g | 43.6 / 318.1 MB | 0.97 / 1.82, 0.79 / 1.58 ms | 86,448 | 0 |
| JList, packed (`M3PackedListModel`) | 1M / 10M | 2g | 62.9 / 592.5 MB | 0.49 / 1.73, 0.68 / 2.33 ms | 37,440 | 0 |
| JTable, `DefaultTableModel` (baseline) | 1M | 2g | 131.9 MB | 0.85 / 1.66 ms | 141,152 | 114 (`getValueAt`) |
| JList, `DefaultListModel` (baseline) | 1M | 2g | 62.9 MB | 0.57 / 2.20 ms | 50,224 | 34 (`getElementAt`) |
| JTree, `DefaultTreeModel` (baseline) | 1,001,000 nodes | 2g | 94.9 MB | 0.51 / 2.48 ms | 53,664 | 71 (`getChild`, one `TreePath` per row) |

What the numbers show:

- **Flat memory.** The virtual models stay at 4.8-7.0 MB of heap after GC from 1M to 100M rows, and from 1M to 1B
  tree nodes. This is at or below the engine targets.
- **Swap.** The virtual JTable repeats the engine's flat swap run: 200,000 rows scrolled, every 10th caller-edited.
  That gave 39,187 swap-outs and 19,593 swap-ins, and all 20,000 edits were reproduced. Swap-in p50 / p95 / p99 was
  0.1 / 0.5 / 15.6 µs at 1M rows and 0.1 / 0.4 / 0.9 µs at 10M, served from the in-memory tier of `TieredSwapCache`.
- **Paint.** The M3 models create nothing during paint. The bytes still allocated per frame come from stock Swing
  and Java2D: `CellRendererPane.paintComponent` calls `Graphics.create` per cell, `getCellRect` allocates a
  `Rectangle`, and the harness itself calls `createGraphics` once per frame. Removing them is a `BasicTableUI` and
  `CellRendererPane` leaf (section 8).
- **Packed table.** The explicit table holds 1M rows in 43.6 MB, against 131.9 MB for `DefaultTableModel`. The packed
  list holds the same heap as `DefaultListModel`, because both keep the caller's `String` objects.
- **The 1B-node JTree.** It shows 1,002,000 rows: depth 1 is expanded, plus the last node. Swing pixel coordinates
  are `int`, so at 18 px a component can reach about 119 million rows. The model itself has 1,001,001,000 keys:
  - select-all counts 1,001,001,000;
  - every third node counts 333,667,000, exactly;
  - collapsing the first node leaves 1,001,000 rows.

## 8. Leaf 2: `java.desktop` (GPLv2+CPE, TARGET_OWNED_PORT)

**Change A, WH04: skip non-showing dirty roots early.** `RepaintManager.paintDirtyRegions` skips a root that is not
showing before it builds the `AccessControlContext` and the `PrivilegedAction`, when no repaint root is pending.

This preserves behaviour exactly:

- a `JComponent` root's `paintImmediately` returns at once when the root is not showing;
- the non-`JComponent` branch already tests `isShowing`;
- a pending `repaintRoot` is still serviced on the usual path.

**Change B, the redraw-suspend construction flag.** This is the Swing analog of SWT's
`setRedraw(false) … setRedraw(true)`. While a `JComponent` has the client property `"m3.redrawSuspended"` set to
`Boolean.TRUE`:

- every `RepaintManager.addDirtyRegion(JComponent …)` for it or a descendant is unioned into one rectangle in that
  component's coordinates, instead of being queued;
- clearing the property posts that union once.

A `static volatile int` counts the suspended components, so with nothing suspended the cost is one volatile read.
Native expose paths (`nativeAddDirtyRegion`) are not suspended, so damage from the window system is still painted.

**Proof.** The reproducers are under `m3/ports/swing-viewport/jdk-reproducers/` and are jtreg-shaped (`@test`,
`@run main`). They need the built M3 JDK, so they are PENDING_LINUX here.

**Next `java.desktop` leaves (design only):**

- WH01: also stop the progress-bar animator on SHOWING_CHANGED.
- WH02, WH03: defer revalidation while the component is not showing, and replay it on SHOWING_CHANGED.
- WH05: defer `updateNodeSizes` while the tree is not showing.
- WH06: test `isShowing` for heavyweight repaints.
- OH18, OH12: a row-index paint loop in `BasicTreeUI` over `AbstractLayoutCache`, with `TreePath` only for
  `paintRow`'s public signature, and a reusable `TreePath` flyweight for the stock caches.
- OH24: `BasicListUI` derives a uniform height from the prototype when the model declares uniform rows.

## 9. SwingX (LGPL-2.1, design notes only)

| SwingX type | Adopts | How |
|---|---|---|
| `JXTable` | `M3PackedTableModel` or `M3VirtualTableModel` | `JXTable` extends `JTable`, so the packed models drop in. Its sorter and filter pipeline (`DefaultSortController`, a `RowSorter`) still builds an `int[]` view-to-model map, which is acceptable because it is CPU and packed. Highlighters run per painted cell through `ComponentAdapter.getValue`, which calls `getValueAt`. A packed `ComponentAdapter` would read the packed column. |
| `JXTreeTable` | `M3PackedTreeModel` behind `TreeTableModel` | `TreeTableModelAdapter` turns tree rows into table rows through `JTree.getPathForRow`. That allocates a `TreePath` per painted row (the OH18 pattern again). With `M3TreeRows`, the row maps to a key by arithmetic and `getValueAt(node, column)` reads the packed column of that key. |
| `JXList` | `M3PackedListModel`, `M3VirtualListModel` | `JXList` extends `JList`; its filter and sort map is an `int[]`. |
| `JXTree` | `M3VirtualTreeModel` with `M3TreeRows` | as for JTree |

The licence row is in `m3/ports/swing-viewport/FOSS_REUSE_DECISION.tsv`. Any SwingX change stays LGPL-2.1 in separate
files, in a SwingX fork, never in this module.

## 10. AWT

AWT `List`, `TextArea` and `TextField` are heavyweight. The native peer (win32 `awt_List.cpp`, X11 `XListPeer`) holds
its own copy of the data, so a lazy model needs peer changes in `src/java.desktop/{windows,unix}`. Those paths are
outside this sparse checkout and are left as design only.

The M3 route for large data is the lightweight `JList` with `M3VirtualListModel`. `Canvas` users get law 3 from
WH06.
