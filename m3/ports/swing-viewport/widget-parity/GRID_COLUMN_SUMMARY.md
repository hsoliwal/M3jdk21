<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3Grid — native Swing column-group summary/detail collapse

## Scope

This is a feature addition to the *existing* canonical
`src/java.desktop/share/classes/jdk/m3/swing/widgets/M3Grid.java`, not
an SWT bridge, a second JTable, or a replacement TableModel. It follows
the already merged two-tier header PR #3337.

Nebula's `GridColumnGroup` distinguishes summary and detail columns.
This leaf supports one explicit summary column per existing first-tier
`setColumnGroup` interval. Calling
`setColumnGroupSummaryColumn(firstModelColumn, summaryModelColumn)`
enables the mode: the group's original summary column is hidden while
expanded, and the original detail columns are hidden while collapsed.
No new TableColumn is constructed. The original TableColumn references,
native renderers, widths, editors and model indices are retained, then
reinserted into the SAME caller-owned TableColumnModel.

The public additive APIs are:

- `setColumnGroupSummaryColumn(int,int)`
- `isColumnGroupCollapsed(int)` and `setColumnGroupCollapsed(int,boolean)`
- `clearColumnGroupSummary(int)`
- typed `ColumnGroupStateListener` add/remove subscriptions

Each group retains at most one `TableColumn[]` reference array of
original column objects and a pair of primitive summary/collapse lanes.
A configured group is limited to 512 original columns; 512 grouped
header intervals are supported in the existing owner. No model value
or row entry is copied, and toggling does not install a new RowSorter.

The group header's native disclosure triangle responds to a mouse
release in the top 24px title band. The ordinary lower-tier JTableHeader
still owns sorting. Listeners receive the fully committed post-mutation
column count and collapsed state. The existing `columnGroupViewStart`
projection now validates the exact native visible members and suppresses
noncontiguous or frozen/scrolling cross-plane groups.

## Contracts and failure handling

- All public mutations require the Swing EDT, consistent with the
  existing M3 Grid owner.
- Invalid/out-of-group summary indices, discontiguous model-column
  views, RTL mode and frozen-boundary splits fail before a native
  visibility mutation.
- Active JTable editors are requested to stop/commit first; if a cell
  editor vetoes, the transition fails without hiding a column.
  This holds for both initial mode setup and later collapse/expansion.
- A selected detail column that becomes hidden transfers to the
  visible summary; the original JTable column-selection model remains
  the owner.
- Frozen membership is adjusted when a whole group belongs to the
  frozen pane. Hiding a summary must never pull an unrelated body
  column into the frozen plane.
- An external column reorder that makes detail members noncontiguous
  prevents the next collapse. However, explicit opt-out still restores
  all original column objects, in a contiguous model-order group,
  rather than stranding hidden references.
- Removing the group or clearing the mode restores every original
  TableColumn identity, its header value, width, renderer, and editor.
  Replacing the TableModel or TableColumnModel invalidates group
  ownership. A TableModel structural column event drops stale
  group definitions before its schema is rebuilt.
- Java serialization records the original native TableColumn reference
  arrays and collapsed group metadata. A round-trip can re-expand and
  collapse the group; older pre-summary M3Grid serial streams are read
  with empty optional lanes. The existing serialVersionUID remains 1L.
- Title-only groups have exactly their earlier behavior unless
  summary/detail mode is explicitly configured.

### Trade-offs / unfinished parity

This implementation mutates the real TableColumnModel on user
expand/collapse, which produces normal column-added/removed/moved
events. Rebuilding a group costs O(visible columns + group size)
or more for stock column-model index operations, not O(1).
It does not support nested column groups, multiple summary columns,
group-specific arbitrary detail role masks, or RTL. Custom column-model
listeners that throw while a mutation is halfway through are not
transactionally rolled back. Full assistive-technology keyboard and
screen-reader parity, alternate LookAndFeel/HiDPI and foreign native
platform screenshot differences are not qualified.

## Donor and source provenance

- Nebula `GridColumnGroup.java`, Git blob
  `17d0638bda658bb6c35b00c22f5c91dfcabb69b9` (EPL-2.0):
  **behavioral requirements only**. No EPL source copied.
- Before: native M3Grid blob
  `0127f576d29bf7b563ee6f82906c962bf2b732fc`, from merged #3337.
- After: M3Grid blob
  `d921b33aa9f169f87bf9f0e155b35d354ddd5a8b`.
- New source-sealed tests:
  - M3GridColumnSummarySmoke.java
    `a5c081ae5058c29f6c043fea1e454e4211d3618e`
  - M3GridColumnSummarySerializationSmoke.java
    `c23adfb6a4c4b9f74d851196d29c5a28c5d77108`
  - M3GridColumnSummaryScale.java
    `c5b2427242faa973cae787d8c6152e99ac88793e`
  - M3GridColumnSummaryHeadfulSmoke.java
    `b71c8097b92769706bcb203c35ca092f83579bc0`

No EPL or SwingX LGPL implementation is copied into M3JDK21.
All five blobs are identical to the files compiled and executed
locally on OpenJDK 21.0.12.1.

## Executed qualification

On Linux, Java 21.0.12.1:

- `javac --release 21 -Xlint:all,-serial,-this-escape -Werror` over
  production and standalone test sources: **PASS**.
- `javac --patch-module java.desktop=...` with Werror and executable
  named-module entrypoints: **PASS**.
- **11 headless programs**: frozen Grid, pinned rows, row/column spans,
  sparse row groups + dense-oracle mapping + serial round-trip,
  two-tier grouped headers + serial round-trip, summary/detail behavior,
  summary/detail serialization, and 1M x 5K summary/detail scale:
  **11/11 PASS** under `-Xmx128m`.
- **6 native mouse programs** using Xvfb/Robot: frozen columns,
  pinned top/bottom, merged cells, row disclosure, two-tier sortable
  headers and new summary/detail disclosure: **6/6 PASS**.
- With a 1,000,000-row and 5,000-column source TableModel, a far
  horizontal and vertical viewport paints only the resident cells,
  under 160 source `getValueAt` reads in the tested frame, under
  `-Xmx128m`.
- Both frozen/unfrozen collapsed summary/detail groups serialize and
  reconstruct the exact original native TableColumn objects.
- The pre-group serialization fixture (from before the grouped header)
  was read successfully by the new class and received a new group.
- `javap -public` found **41 previous methods** and **48 new
  methods**, with **zero removed** old public signature lines.

## HOLD gates

The full source-built M3JDK runtime/jtreg, complete Maven/JUnit reactor
at this PR SHA, native Win32/Cocoa/GTK4 and alternative LAF/HiDPI,
accessibility/screen-reader keyboard testing, nested grouped header
hierarchy, multiple summary/detail roles, canonical OpenRewrite
recipe-first/source-sealed fixed point in Synexia, and comparative
UI-thread performance benchmarks remain **NOT_RUN / OPEN**.

This is functional native Swing summary/detail collapse, **not a claim
of total SWT/Nebula Grid parity**.
