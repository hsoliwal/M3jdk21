<!-- SPDX-License-Identifier: Apache-2.0 -->
# Viewport everywhere, phase 2: adapter designs (D2C)

Status: design, 2026-10-19. No adapter code exists yet. Each adapter becomes its own draft PR with a recorded
stock-vs-viewport differential (seeded public-API scripts on the stock widget and on the viewport widget; every step
records answer, scroll state, selection, focus and events; any difference fails with the op log). The adapter seams
follow the checklist in `DESIGN.md` section 8a. All adapters use `com.m3.viewport` (the core) and `com.m3.collections`
(the owners); none copies donor code.

## 1. AWT `java.awt.List` (GPLv2+CPE, `src/java.desktop`)

**Native constraint (measured from the source, not guessed).** The Windows peer (`WListPeer`, `awt_List.cpp`) is a
LISTBOX that holds every item natively; a virtual (no-data) listbox needs owner drawing in C++. Rule 13 (Java only) rules
out a native virtual mode, so the viewport cannot live inside the peer.

**Design: two leaves.**

| Leaf | What | Where | Proof |
|---|---|---|---|
| A1 model | `-Dm3.awt.viewport=true`: `java.awt.List` keeps its Java-side state (items, peerless selection) in DOOP lanes: an M3 list model instead of `Vector<String>` + `int[] selected`; peer calls unchanged, so native behaviour is stock | `java/awt/List.java` hook by recipe (`M3InsertStatements`, pass 2 UNCHANGED); the lane classes are copied into `java.desktop` as `TARGET_OWNED_PORT` under an explicit-relicence record like the item-residency engine | `--patch-module java.desktop=...` differential on public API answers, stock vs M3, headless where AWT allows (peerless lists) and on the hidden desktop with peers |
| A2 viewport | `M3AwtViewportList`: a `Panel` that owns a logical `Scrollbar` and a content-plane `java.awt.List` holding only the resident window (visible rows + overscan); `M3RowWindowPlan` plans head/tail deltas, `M3ScrollMetrics` maps rows to the int scroll range, `M3SelectionModel` holds the selection | new Apache module `m3/ports/awt-viewport` | differential vs stock `java.awt.List` on the public API (items, selection, `getVisibleIndex`, `makeVisible`, events); pixels differ by design (separate scroll bar), recorded as a divergence class |

Seams: `makeVisible` -> `M3ViewportRuntime.RowWindow.ensureVisible`; `select`/`deselect`/`getSelectedIndexes` ->
`M3SelectionModel` (translated through `M3RowWindowPlan.entryOf/rowOf` for the plane); `add`/`remove` ->
`M3RowWindowPlan.inserted/removed` (only resident rows touch the peer).

## 2. SwingX `JXList`, `JXTree`, `JXTable`, `JXTreeTable` (LGPL-2.1: pattern only)

- `JXTree` and the hierarchical column of `JXTreeTable` render through a `JTree` whose UI extends `BasicTreeUI`; on
  M3JDK with `-Dm3.swing.viewport=true` they get the DOOP layout caches of M3jdk21 #1001
  (`M3VariableHeightLayoutCache`, `M3FixedHeightLayoutCache`) without any SwingX code. Reuse only; the
  `m3/swing-viewport-20261017` branch is not edited. Proof: the #1001 differential run with a `JXTree`.
- `JXList` uses SwingX's own list UI. The adapter is an M3 Swing list UI that subclasses at run time (SwingX is an
  optional runtime dependency, as in `m3/ports/swing-viewport/optional-swingx/M3SwingXLinkageProof.java`) and replaces
  the O(n) `cellHeights` layout with `M3SparseExtents` (measured heights over an estimate, Fenwick offsets).
- `JXTable`: row heights through `M3SparseExtents`; sorting/filtering stay SwingX's (`RowSorter`).
- Highlighters and renderer stamps (catalogue rows SWX-HIGHLIGHTERS, SWX-TREETABLE-STAMP) map onto `M3RowPainter`
  themes; no SwingX code is copied.

## 3. IntelliJ platform UI (Apache-2.0), read with `gh api` at `06e208526c9d`

Measured from the sources:
- `com.intellij.ui.tree.ui.DefaultTreeUI extends BasicTreeUI` (platform/ui.tree) **overrides `createLayoutCache`** with
  its Kotlin `DefaultTreeLayoutCache`, and its bulk expand/collapse support checks
  `treeState instanceof DefaultTreeLayoutCache`. So the M3JDK `BasicTreeUI` hook does not reach IntelliJ trees.
- `com.intellij.ui.components.JBList<E> extends JList<E>` and swaps `BasicListUI` for `WideSelectionListUI`.

Design:
- Tree: `M3IntelliJTreeUI extends DefaultTreeUI`, overriding only `createLayoutCache` to return an
  `AbstractLayoutCache` over `M3TreeTopology` + `M3TreeProjection` (or the #1001 cache). Bulk operations then take the
  stock per-event path (`bulkOperationsSupported()` is false), which is behaviour-preserving. Opt-in through an
  IntelliJ registry key; installed with `Tree.setUI`.
- List: `M3WideSelectionListUI extends WideSelectionListUI` with `M3SparseExtents` row offsets.
- Minimal adapter jar: compiles against the platform jars of the local IntelliJ IDEA Community 2025.2.3 install
  (`lib/app-client.jar` holds `DefaultTreeUI` and `JBList`, checked with `unzip -l`); no clone. The source pin
  above (`06e208526c9d`) and the installed 2025.2.3 build are recorded separately. Not
  built yet (builds parked).

## 4. RWT (Eclipse RAP widget toolkit, EPL)

RAP's virtual `Tree`/`Table` keep items on the server and render a client-side row grid that asks for a preload window
(catalogue row RAP-TREE-FLATINDEX). The adapter keeps the server side lane-shaped: `M3TreeTopology` and
`M3TreeProjection` answer the flat index; the client's requested range is the resident window, planned with
`M3RowWindowPlan` / `M3TreeResidentPlan`; `SWT.SetData` materializes only that range; item facades are created lazily
and released outside the window (`M3SparseItemStore.releaseOutside`). The client keeps its own pixels; the M3 row
painter is not used on RAP.

## 5. M3 renderer mapping (`M3RowPainter`)

| Backend | Implementation | Source of truth |
|---|---|---|
| M3 drawn mode (SWT, Nebula) | `M3RowPainter` over the render-modes M3 plane backend (`M3GC`, `M3Surface`; SWT PR #202, read-only reference) | lanes + theme |
| Java2D (Swing, SwingX, AWT, IntelliJ) | `M3Java2DRowPainter`: clip -> row range (`M3SparseExtents.visibleRange`), one `Graphics2D` pass per row rectangle, no per-row allocation | lanes + theme |
| RWT | none (client pixels) | - |

Damage is a set of row rectangles from the window plan (head/tail deltas); per the donor-atomize run there is no
lane-shaped damage donor, so this part is designed here (DESIGN.md 8a).

## Order and gates

1. A1/A2 (AWT), 2. SwingX, 3. IntelliJ, 4. RWT, 5. row painters. Each: design row here, code, differential recorded,
JaCoCo >= 99% lines and branches, draft PR. UI runs only under `ui_lock.sh` + `hidden_java.sh`.
