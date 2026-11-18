<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native Swing Grid — frozen-column first leaf

Purpose: deliver an actual `JTable`-subtype widget with part of Nebula
Grid's frozen-column behavior. This does NOT embed SWT/Nebula, duplicate
JTable models or create a new DOOP/viewport rendering engine.

## Existing owner reuse

- New `jdk.m3.swing.widgets.M3Grid extends JTable` lives in canonical
  `src/java.desktop/share/classes/jdk/m3/swing/widgets`, alongside the
  already-received widget sources from #3313.
- `JTable`, `TableModel`, `TableColumnModel`, `RowSorter`, editors,
  renderers, selection models and Swing's LookAndFeel stay authoritative.
- Uses a single body table and one existing `JScrollPane`, not one widget
  per row or an additional copy of table data.
- Frozen columns are the first N **current view columns**, so a user move
  or model replacement retains the current view-to-model mapping.
- Opt-in `setFrozenColumnCount(int)` has default 0: no frozen painter,
  header replacement or listeners until enabled.
- While enabled, body cells and header are drawn at the viewport's left
  origin in a retained overlay and clipped to the frozen region. The stock
  BasicTableUI paint pass always uses stock hit geometry: it still visits
  actual viewport columns without building all preceding columns.
- Interactive `columnAtPoint` maps frozen pixels back to their view
  columns. The one JTable still manages selection, sorting and native
  editors; the editor is positioned over the pinned cell.
- Disabling pinning returns the exact original JTableHeader instance and
  the original header view in JScrollPane.
- Existing `CellRendererPane` is lazily reused; there are no per-cell
  persistent renderer peers. Damage outside the frozen rectangle does
  not run the pinned renderer. With a one-row dirty clip, only that row
  gets pinned repaint work.
- RTL or print output explicitly retains stock layout. Those are not
  represented as feature complete.

## Donor provenance

Requirements, not source copying:

| Source | Git blob | License and disposition |
|---|---|---|
| Nebula GridViewportProjection.java | 572f3647a4f08f18ddf147629597864d0c7328e7 | Apache-2.0 OR EPL-2.0; behavior/reference only |
| Nebula GridColumn.java | 9cac947a906e3ca41c32e5859208a223638bf1a1 | EPL-2.0; behavior only |
| M3JDK21 M3PackedTable.java | d6d15229713ae57142de427f6a8340bbac14b8ff | Existing Apache-2.0 table; unchanged |
| M3JDK21 M3DesktopViewportAdapters.java | 6d9a2491c7d1de69c5025f423f41584a10372d8a | Existing opt-in coordinate helpers; unchanged |

This `M3Grid` is an original Apache-2.0 Java implementation. It does not
copy Nebula (EPL-2.0) or SwingX (LGPL-2.1) source. It should later adopt
the existing M3 packed-cell path by an intrinsic owner recipe if the
source/binary/behavioral preservation gate succeeds, not by duplicating
`M3PackedTable` storage. `M3PackedTable` remains final and untouched.

## Executed qualification

- Stock OpenJDK 21.0.12.1 on Linux; `javac --release 21
  -Xlint:all,-serial,-this-escape -Werror` across product and standalone
  test sources: PASS.
- `M3GridContractSmoke`, headless `-Xmx128m`: PASS. Exact colored
  viewport and header pixels, frozen hit testing, editing bounds, native
  view-column reordering, sorter/filter retention, original-header restore,
  negative/overflow inputs, damage-only repaint counter.
- One-million-logical-row `AbstractTableModel` under the same 128 MiB
  heap: PASS. At a far vertical offset, the paint invokes the model
  only for visible cells (fewer than 140 reads); no one-million-row
  widget or entry objects are created.
- `M3GridHeadfulSmoke` under Linux `xvfb-run`: PASS. A real AWT Robot
  mouse press/release over a frozen column selects the correct native
  JTable view column and row; screenshot capture executed.
- Git blob hashes of the three standalone product/test Java files
  exactly match the local compiled/tested files:
  - `M3Grid.java`: `30cda1d9b0f97d43e7bd7134c13cd436ecc64bb1`.
  - `M3GridContractSmoke.java`: `588737165d1aad39f87d33e237833d202bd41094`.
  - `M3GridHeadfulSmoke.java`: `195425494a77609b7c1334aa757bc4154960611f`.

## Reproducers

From repository root:

```sh
OUT=/tmp/m3-grid-test
mkdir -p "$OUT"
javac --release 21 -Xlint:all,-serial,-this-escape -Werror -d "$OUT" \
  src/java.desktop/share/classes/jdk/m3/swing/widgets/M3Grid.java \
  m3/ports/swing-viewport/src/test/java/jdk/m3/swing/widgets/M3GridContractSmoke.java \
  m3/ports/swing-viewport/src/test/java/jdk/m3/swing/widgets/M3GridHeadfulSmoke.java
java -Djava.awt.headless=true -Xmx128m -cp "$OUT" jdk.m3.swing.widgets.M3GridContractSmoke
xvfb-run -a java -Djava.awt.headless=false -Xmx128m -cp "$OUT" jdk.m3.swing.widgets.M3GridHeadfulSmoke
```

## Remaining work — do not promote as full Nebula Grid parity

- Renderer/editor combinations including advanced Grid editors, the
  default Grid selection/row-group behavior and native checkbox controls.
- Noncontiguous frozen columns, row grouping, column groups,
  hierarchical Grid, long-index surfaces, RTL and print fidelity.
- Basic opt-in merged body cells (row/column spans) are now implemented
  in the same native Grid owner. See [GRID_SPANS.md](GRID_SPANS.md)
  for model/view identity, overlap and cross-plane limitations.
- Pinned top/bottom rows are now opt-in in the same M3Grid class;
  see [GRID_PINNED_ROWS.md](GRID_PINNED_ROWS.md) for test receipts,
  viewport-scrolling tradeoffs and still-open native qualification.
- Reordering and resizing during a mouse drag, focus traversal and
  keyboard/accessibility; headful GTK/Win32/Cocoa/HiDPI and alternate
  LookAndFeel pixel/event differentials.
- Full module Maven/JUnit/CI and source-built M3JDK jtreg, stock
  `javax.swing` ABI and binary/serialization proof, Synexia source-sealed
  OpenRewrite recipe fixed point and comparative performance benchmarks.

Native Swing promotion #3313 has merged. This Grid leaf uses the existing
canonical `java.desktop` widget package, with the exact tested source Git
blob rather than maintaining a second runtime copy in the legacy port.
It also carries the already merged #3308 `M3DateTime.java` Git blob
`2e4dc4695dac279c0d2279a19d0217c36f1aaab5` into the same canonical
package: it had remained only in the port's migration-residue directory.
The existing port copy is left untouched for a separately reviewed cleanup.
