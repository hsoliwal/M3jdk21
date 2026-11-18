<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3Grid — native frozen top and bottom view-row planes

## Scope and owner

Extends the existing `src/java.desktop/share/classes/jdk/m3/swing/widgets/M3Grid.java`
intrinsically; no new JTable, data model, row object, SWT adapter, JNI
backend, or painting service. `M3Grid` remains the single owner of native
Swing row selection, model/view mapping, editors, accessible context,
TableCellRenderer and TableColumnModel.

The caller opts in with `setPinnedRowCounts(top, bottom)`; default is
`(0,0)`, giving stock JTable row layout and behavior. Top and bottom
boundaries refer to the first and last rows in the *current view*, after
the existing RowSorter/RowFilter. Effective counts clamp automatically
if a filter or model deletion reduces the row count, never retaining
stale model-row identities. Invalid or overlapping requested counts
fail before mutating state.

- Row geometry uses JTable's current variable row heights and
  viewport-visible rectangle. Top rows render at the viewport's
  leading edge; bottom rows render backwards from its trailing edge.
- Rendering uses the existing CellRendererPane flyweight for visible
  view columns. Pinned row/column intersections replay the original
  renderer with matching source row and column, not a copied value.
- The painter clips the top and bottom planes and does not query rows
  outside the visible body or pinned bands. Effective iteration is
  bounded by viewport pixels and configured visible row/column spans.
- `rowAtPoint` uses pinned viewport coordinates for native selection,
  but BasicTableUI's stock rendering continues to use the stock
  physical row geometry via the already-existing stockPainting guard.
- A native mouse click on a pinned row must not trigger JTable's
  default `scrollRectToVisible` jump to row 0 or the last model row.
  `scrollRectToVisible` projects pinned row/column rectangles back
  into the visible pinned plane, preserving normal JTable scrolling
  for unpinned rows.
- Editing stays on the original JTable editor and positions the editor
  at the pinned top/bottom and/or frozen column coordinates.
- While pinned rows are configured, the JViewport is switched from
  its current scroll mode to SIMPLE_SCROLL_MODE. This prevents
  pixel-blit scrolling from moving painted top/bottom rows as if
  they were ordinary body content. The original scroll mode is
  restored on disable/removeNotify. This is a deliberate performance
  tradeoff: the body may repaint more often than with a blit.
- RTL and print mode are not upgraded; they retain stock behavior.
  Row grouping, spanning, hierarchical rows and long-index APIs
  remain separate features, not falsely implied by this leaf.

## Source seals

| Item | Git blob SHA-1 |
|---|---|
| preimage canonical M3Grid.java, merged PR #3316 | `30cda1d9b0f97d43e7bd7134c13cd436ecc64bb1` |
| new M3Grid.java (same class/API plus additive pinned methods) | `0289f6d221f31edac17312a6a0f61120a7f082e5` |
| new M3GridPinnedRowsSmoke.java | `894e72e5132eeff0599c350f010983a774513207` |
| new M3GridPinnedRowsHeadfulSmoke.java | `c836075a76bc4ba86d5235b4c318f7f76e6da783` |

No Nebula EPL source or SwingX LGPL source was copied.

## Executed Java 21 qualification

OpenJDK 21.0.12.1, Linux headless and Xvfb; `-Xmx128m` for
standalone tests, strict `javac --release 21
-Xlint:all,-serial,-this-escape -Werror`.

- Existing `M3GridContractSmoke`: PASS, stock-off, frozen column
  selection/pixels, header, editing and 1M virtual rows.
- New `M3GridPinnedRowsSmoke`: PASS, top and bottom paint exact
  pixel colors, first and second pinned rows, their frozen-cell
  intersections, variable row height/hit bounds, sorter/filter
  view identities, scroll-mode preservation/restoration, zero-row
  contraction, invalid arguments, 1M virtual rows at a far offset
  with no eager whole-model materialization.
- Existing `M3GridHeadfulSmoke`: PASS under Linux/Xvfb; real
  Robot mouse event selects the correct frozen column.
- New `M3GridPinnedRowsHeadfulSmoke`: PASS under Linux/Xvfb;
  real Robot selects first and last pinned rows without changing
  the body viewport's vertical offset.
- The new native mouse test **failed before** the
  `scrollRectToVisible` projection correction, showing a jump from
  scroll y=600 to 0, then passed after the intrinsic fix.
- A strict `--patch-module java.desktop` compile and
  named-module invocation of old/new headless tests and the
  new headful Robot test: PASS.
- Git blob identities match the exact locally tested Java sources.

### Reproduction

From the M3JDK21 repository root:

```sh
ROOT=src/java.desktop/share/classes
TEST=m3/ports/swing-viewport/src/test/java
OUT=/tmp/m3-grid-pinned
mkdir -p "$OUT"
javac -Xlint:all,-serial,-this-escape -Werror \
  --patch-module java.desktop="$ROOT:$TEST" -d "$OUT" \
  "$ROOT/jdk/m3/swing/widgets/M3Grid.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridContractSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridPinnedRowsSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridHeadfulSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridPinnedRowsHeadfulSmoke.java"
java -Xmx128m -Djava.awt.headless=true \
  --patch-module java.desktop="$OUT" \
  -m java.desktop/jdk.m3.swing.widgets.M3GridPinnedRowsSmoke
xvfb-run -a java -Xmx128m -Djava.awt.headless=false \
  --patch-module java.desktop="$OUT" \
  -m java.desktop/jdk.m3.swing.widgets.M3GridPinnedRowsHeadfulSmoke
```

## Qualification still open

The complete source-built M3JDK runtime and jtreg, the whole
Maven/JUnit reactor, platform Win32/Cocoa/GTK4/HiDPI,
alternate LookAndFeel, keyboard/IME, accessibility, native
drag-resize, serialization/API differential and the paired Synexia
OpenRewrite fixed-point recipe are not qualified by these tests.
Multi-row overlays may increase paint work on scroll while
SIMPLE_SCROLL_MODE is active; performance benchmarks against the
unmodified JViewport blit path remain outstanding.

This is an independently tested incremental feature, **not**
full Nebula Grid parity.
