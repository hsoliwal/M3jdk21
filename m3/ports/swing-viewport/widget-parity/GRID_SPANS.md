<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3Grid: native merged-cell row/column spans, first leaf

## Existing owner, not a parallel table

The canonical `java.desktop/jdk.m3.swing.widgets.M3Grid` retains the original
`TableModel`, `RowSorter`, `TableColumnModel`, selection model, renderers,
editors and LookAndFeel. The new opt-in span mechanism stores only
`int[]` lanes for `(modelRow, modelColumn, rowSpan, columnSpan)`.
It allocates no persistent widget, cell entry, substitute TableModel or
rendering peer per span. At most 8,192 spans are admitted in this leaf;
additional capacity must be justified by a measured sorted/interval index.

## Model/view identity and fail-closed rules

- Public `setCellSpan(int,int,int,int)`, `removeCellSpan`, `clearCellSpans`,
  `getCellSpanCount`, `cellSpanAt(viewRow,viewColumn,out,offset)`,
  `spannedCellRect(viewRow,viewColumn)`, and `getSpanPaintedCells`.
- Spans are **model** coordinates and never silently attach to another
  model row/column after a Swing sort/filter or column reorder.
- Each paint/hit maps model anchors through the existing JTable
  row/column conversions; all model cells in the span must currently
  occupy an ascending contiguous view rectangle. Noncontiguous or
  filtered cells are inactive until the view becomes contiguous again.
- A span crossing a frozen-column or pinned-top/bottom-row boundary is
  inactive, rather than damaging the independent frozen planes. This
  first leaf supports body spans only; cross-plane spans are future work.
- A `TableModelEvent` INSERT/DELETE/HEADER_STRUCTURE event clears
  the packed span table because stock TableModel rows do not expose
  stable logical identity across structural mutation.
- Overlapping spans are rejected atomically; an existing anchor may
  update its own dimensions without generating duplicates. A 1x1
  assignment removes the existing span. Invalid/overflow bounds fail.
- `BasicTableUI` still paints using **stock physical coordinates**.
  After the stock/frozen/pinned painters, an opt-in merged-cell painter
  clips to the visible center body and reuses the original
  `CellRendererPane` / anchor `TableCellRenderer`.
- Covered-cell mouse hit testing resolves to its real anchor, while
  programmatic `JTable` view-coordinate selection and existing public
  JTable geometry methods retain their original behavior.
- The original JTable editor is stretched to the merged anchor's bounds.
- With no RowSorter, view rows have model order: verifying even a
  300,000-row span is O(1) identity projection, not 300,000 reads.
  With a RowSorter, proof is bounded to at most 4,096 rows per span;
  larger spans are inactive rather than using unbounded paint-time
  verification. The same limit applies to column permutations.
- The current hot path visits configured spans in O(S) for hit/paint
  (`S <= 8192`), then validates bounded member permutations.
  Nebula `GridRowSpanIndex` suggests the next optimization: sparse,
  mutation-driven sorted projection and interval-prefix endpoints.
  No claim of O(log S) span search is made by this leaf.

## Donor/source seals

| Source | Git blob SHA | Use |
|---|---|---|
| Nebula GridCellSpanManager.java | `8046007bcd43008f988b554443c22c6cad3f4d8a` | EPL-2.0 behavioral requirements only, no copied source |
| Nebula GridRowSpanIndex.java | `055c511d3bb54ecc72d734fbfdb9bc7be025bd33` | Dual Apache/EPL conceptual sparse index donor, not copied |
| M3JDK21 prior pinned Grid source | `0289f6d221f31edac17312a6a0f61120a7f082e5` | Original owner preimage |
| M3JDK21 new Grid source | `774225874c6c35f1d733b17ba087e4a1ae6af9a2` | Exact locally tested Java source |
| M3GridSpansSmoke.java | `ee86fbcec7d0f5e34489e9c94ce82b24e1fc602d` | Headless/1M tests |
| M3GridSpansHeadfulSmoke.java | `5bfac8d406bbf329fada9eaff8688775dfd32bf6` | Real native mouse test |

No EPL or SwingX LGPL implementation is copied into `java.desktop`.

## Executed Java 21 qualification

Stock OpenJDK 21.0.12.1, Linux headless and Xvfb, strict
`javac --release 21 -Xlint:all,-serial,-this-escape -Werror`:
PASS for the original product and all four predecessor/new tests.

- Existing `M3GridContractSmoke`: PASS (frozen columns, header,
  editor, sorter/filter, large model).
- Existing `M3GridPinnedRowsSmoke`: PASS (top and bottom overlays,
  variable row heights, 2+2 pins, million-row viewport).
- New `M3GridSpansSmoke`: PASS (merged body pixels, anchor-hit row and
  column, native editor dimensions, no-overlap, column move,
  contiguous sorted view, fail-closed descending/filter behavior,
  source-model structural invalidation and remove/clear).
- One-million logical rows, one visible span at a far model index
  and another *300,000-row* span: PASS under `-Xmx128m`, fewer than
  400 source `getValueAt` reads on the tested frame.
- Existing `M3GridHeadfulSmoke`: PASS (native frozen column click).
- Existing `M3GridPinnedRowsHeadfulSmoke`: PASS (native pinned row
  clicks without viewport jump).
- New `M3GridSpansHeadfulSmoke`: PASS on Linux/Xvfb: real AWT Robot
  click in the covered portion selects the anchor (view row 5, col 2),
  while the pinned/scrolling viewport stays at its previous y=48.
- Java 21 `--patch-module java.desktop` strict compilation, and
  named-module execution of the new and previous headless/headful
  programs: PASS.
- All Git blob identities above match locally compiled and executed files.

## Remaining acceptance gates

Full M3JDK native image build/jtreg, whole Maven/JUnit reactor, downstream
SwingX/NetBeans compatibility, Win32/Cocoa/GTK4 HiDPI, accessible merged-cell
navigation, keyboard editing, RTL, printing, mixed selection behavior,
cross-plane spans, sparse logarithmic span index, nested group headers
(flat model-order grouping is implemented separately), canonical Synexia source-sealed OpenRewrite fixed-point recipe and
performance benchmarks remain **NOT_RUN**.

This is a first native Swing merged-cell feature, **not** complete
Nebula Grid equivalence.
