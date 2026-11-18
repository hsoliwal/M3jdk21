<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3Grid — first native Swing two-tier column header groups

## Existing owner and public contract

This leaf enhances the **existing canonical** `src/java.desktop/share/classes/jdk/m3/swing/widgets/M3Grid.java` (the same final JTable subclass merged through PRs #3316, #3323, #3327 and #3333). It is not an SWT bridge, a new Grid implementation, a copied SwingX model, or an alternative column store.

New **opt-in**, additive APIs: `setColumnGroup(int firstModelColumn,int count,String title)`, `getColumnGroupCount()`, `getColumnGroupTitle(int)`, `removeColumnGroup(int)`, `clearColumnGroups()`, `getColumnGroupPainted()`. Groups occupy sorted, non-overlapping model-column intervals, in two primitive `int[]` lanes plus a parallel title `String[]`. The configured capacity is bounded to 512 groups. The original `TableColumnModel`, `TableColumn`, header renderer, `RowSorter`, selection, editor and data TableModel remain authoritative.

While enabled, the existing `PinnedHeader extends JTableHeader` reserves a 24-pixel group-title band **above** the ordinary column header row. It reuses existing native leaf renderers and the same `CellRendererPane`. Group-title clicks return no leaf column, so they do not sort; lower-tier leaf-header clicks continue through stock Swing `JTableHeader` and its own sorting. No second JTable, JViewport, peer per group, or duplicate model-cell values.

Each group follows the original model-column identities only when all members remain in ascending, contiguous *current view* columns. Column movement that makes a group noncontiguous suppresses its band rather than attaching it to unrelated data. A group crossing the boundary between pinned and scrollable columns is also suppressed. Frozen and scrolling portions have independent clips, and toggling pinning does not remove configured groups. Clearing the last group (with no frozen columns) restores the **identical original JTableHeader**, including its JScrollPane header view.

Replacing the TableModel or receiving a structural column-model change invalidates the stored model-column ordinals. Serialization validates group intervals and labels and restores the extra header tier with both frozen and unfrozen cases. A pre-column-group serialized Grid (including a collapsed row group) remains readable and gains usable empty column-group arrays.

## Memory and paint model

Only group labels and compact ranges are persistent. Rendering leaf headers is limited to a *visible* horizontal column window rather than all leaf columns; the 5,000-column regression asserts fewer than 12 header renderer invocations at a far right viewport. Computing visible starts and validating groups can still scan native TableColumnModel structures: **no end-to-end O(visibleColumns) layout, pixel-time or log-index claim is made**. One group header band is drawn per accepted intersecting group; no per-cell peers are retained.

## Source locks and donor licensing

- Previous M3Grid.java Git blob: `cfec30aba24110841a1a617fd45f67db34ec2c9e` (merged sparse row groups).
- New M3Grid.java Git blob: `0127f576d29bf7b563ee6f82906c962bf2b732fc` (matches exact strict-compiled/tested bytes).
- New M3GridColumnGroupsSmoke.java Git blob: `e3a87e7f1c9ab2b621cccdcb191bd1b45a5a2ca9`.
- New M3GridColumnGroupsSerializationSmoke.java Git blob: `b204d78db62ba7203db29722ff25928b0818beff`.
- New M3GridColumnGroupsHeadfulSmoke.java Git blob: `7068a19e643f057db11263804bf986999129692a`.
- Nebula `GridColumnGroup.java`: blob `17d0638bda658bb6c35b00c22f5c91dfcabb69b9`, EPL-2.0, **behavioral donor only**, no licensed code copied.
- Nebula groups additionally support collapse/expand, summary/detail columns and custom events. These are *not* implemented by this first header-band leaf.

## Executed Java 21 verification

Stock OpenJDK **21.0.12.1**, Linux headless and Xvfb, `-Xmx128m` test heap:

- `javac --release 21 -Xlint:all,-serial,-this-escape -Werror` over the tested production source plus standalone smokes: **PASS**.
- Canonical `--patch-module java.desktop` strict `-Werror` compile and named-module execution: **PASS**.
- **8 headless smokes PASS**: prior frozen Grid contract; pinned top/bottom rows; merged row/column spans; sparse row groups, dense mapping oracle, row-group serialization; new two-tier header pixel/window/reorder/schema test; new column-group serialization.
- **5 headful Linux/Xvfb Robot smokes PASS**: prior frozen column, pinned rows, merged-cell anchor, row-group disclosure; new grouped-header mouse test. Group-band click does not sort and regular header click still sorts through stock JTable.
- New tests cover exact red/blue leaf pixels below group titles, 24-pixel preferred size, two model-column groups, noncontiguous view permutations, cross-frozen boundary, full opt-out and original-header identity, replaced model, changed column schema, and **5,000 columns with fewer than 12 leaf render stamps**.
- New Java serialization round trips both with 0 and 1 frozen columns. An older, pre-column-group serialized Grid preserving collapsed row-group state is readable by the new class: `COLUMN_GROUP_PREIMAGE_TO_NEW_PASS`.
- `javap -public` comparison: previous **37** public signature lines and new **44**; **zero removed previous signatures**, with six new group methods and one `setModel(TableModel)` override.
- Exact five Git blob seals above match the local tested production and three new standalone test files. No full-source JDK / M3 reactor was built in this leaf.

Reproducer from repository root:

```sh
ROOT=src/java.desktop/share/classes
TEST=m3/ports/swing-viewport/src/test/java
OUT=/tmp/m3-column-groups
mkdir -p "$OUT"
javac -Xlint:all,-serial,-this-escape -Werror --patch-module java.desktop="$ROOT:$TEST" -d "$OUT" \
  "$ROOT/jdk/m3/swing/widgets/M3Grid.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridContractSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridPinnedRowsSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridSpansSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridGroupsSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridGroupsMappingSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridGroupsSerializationSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridColumnGroupsSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridColumnGroupsSerializationSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridHeadfulSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridPinnedRowsHeadfulSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridSpansHeadfulSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridGroupsHeadfulSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridColumnGroupsHeadfulSmoke.java"
java -Xmx128m -Djava.awt.headless=true --patch-module java.desktop="$OUT" \
  -m java.desktop/jdk.m3.swing.widgets.M3GridColumnGroupsSmoke
xvfb-run -a java -Xmx128m -Djava.awt.headless=false --patch-module java.desktop="$OUT" \
  -m java.desktop/jdk.m3.swing.widgets.M3GridColumnGroupsHeadfulSmoke
```

## Explicit HOLD gates

The entire M3JDK source-built runtime and jtreg, full Swing Maven/JUnit reactor at the receiving PR SHA, Win32/Cocoa/GTK4/HiDPI and alternative LookAndFeel screenshot/event differentials, native accessibility and keyboard/IME semantics, drag-resize under the two-tier header, RTL, print fidelity, nested column groups, real Nebula expand/collapse summary/detail behavior, and canonical Synexia source-sealed OpenRewrite recipe/fixed-point qualification are **NOT_RUN or OPEN**.

This is real native Swing header-band functionality, **not full Nebula GridColumnGroup parity**.
