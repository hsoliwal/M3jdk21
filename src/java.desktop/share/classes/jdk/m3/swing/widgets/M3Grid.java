/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.table.TableModel;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.EventObject;
import javax.swing.event.TableModelEvent;
import javax.swing.CellRendererPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * A native Swing Grid with opt-in frozen leading view columns. This is a
 * JTable, not an SWT widget, model wrapper or second row/column store.
 * Models, sorters, selection, editing, column order, renderers and LookAndFeel
 * remain owned by JTable. The pinned plane reuses existing table renderers
 * and repaints only the currently visible rows, with no per-row components.
 *
 * <p>The initial pinned plane supports left-to-right layout. In RTL or
 * printing, stock JTable layout is retained; native parity remains pending.</p>
 */
public final class M3Grid extends JTable {
    private static final long serialVersionUID = 1L;
    private int frozenColumns;
    private transient CellRendererPane frozenPainter;
    private transient boolean stockPainting;
    private transient long frozenPaintedCells;
    // Opt-in pinned rows retain view-row coordinates, not extra model rows.
    // Their renderers and editors remain owned by this single JTable.
    private int pinnedTopRows;
    private int pinnedBottomRows;
    private transient long pinnedPaintedCells;
    private transient JViewport scrollModeViewport;
    private transient int previousScrollMode = -1;
    // Model-coordinate rectangular spans. Four primitive lanes per span:
    // modelRow, modelColumn, rows, columns. No cell entries or copied values.
    private int[] spanLanes = new int[32];
    private int spanCount;
    private transient long spanPaintedCells;
    // Sparse header/end/collapsed lanes in model-row coordinates; labels only
    // for the group headers. Child rows are never materialized as objects.
    private int[] groupLanes = new int[24];
    private String[] groupLabels = new String[8];
    private int groupCount;
    private transient GroupRowSorter groupSorter;
    private transient MouseAdapter groupMouse;
    private transient long groupHeaderPainted;
    private JTableHeader originalHeader;
    private PinnedHeader pinnedHeader;

    public M3Grid(TableModel model) { super(model); }

    /** The number of leading, currently displayed view columns to pin. */
    public int getFrozenColumnCount() { return Math.min(frozenColumns, getColumnCount()); }

    public void setFrozenColumnCount(int count) {
        requireEdt();
        if (count < 0 || count > getColumnCount()) throw new IllegalArgumentException("frozen columns out of range");
        if (frozenColumns == count) return;
        frozenColumns = count;
        if (count == 0) uninstallHeader();
        else installHeader();
        repaint();
        if (getTableHeader() != null) getTableHeader().repaint();
    }

    public int getFrozenWidth() {
        long width = 0;
        for (int i = 0; i < getFrozenColumnCount(); i++) {
            width += getColumnModel().getColumn(i).getWidth();
        }
        return (int) Math.min(Integer.MAX_VALUE, width);
    }

    /** Number of rendered frozen body cells, for performance and damage regression gates. */
    public long getFrozenPaintedCells() { return frozenPaintedCells; }

    /** Number of sparse, non-overlapping model-order groups. */
    public int getRowGroupCount() { return groupCount; }

    /** Number of group headers stamped in the visible viewport. */
    public long getGroupHeaderPainted() { return groupHeaderPainted; }

    /**
     * Adds a model-order group with its first row serving as the real Swing
     * header. Children occupy [firstModelRow+1, endModelRowExclusive).
     * Groups are sorted and non-overlapping; there are no synthetic rows.
     *
     * <p>Collapse uses a sparse RowSorter with O(log groups) index mapping,
     * not TableRowSorter's O(model rows) entry objects. It only activates when
     * no external RowSorter is installed. A caller-owned sorter cannot be
     * silently replaced. To sort/filter the table externally, clear groups
     * first, then install the caller sorter; replacing the sorter explicitly
     * also clears grouping. This first leaf does not support group sorting.</p>
     */
    public void addRowGroup(int firstModelRow, int endModelRowExclusive, String title) {
        requireEdt();
        if (firstModelRow < 0 || endModelRowExclusive <= firstModelRow + 1
                || endModelRowExclusive > getModel().getRowCount()) {
            throw new IllegalArgumentException("group bounds require header plus children");
        }
        if (title == null) throw new NullPointerException("title");
        if (groupSorter == null && getRowSorter() != null) {
            throw new IllegalStateException("external RowSorter is already installed; cannot replace it");
        }
        int index = findGroupInsertion(firstModelRow);
        if (index < groupCount && groupLanes[index * 3] == firstModelRow) {
            throw new IllegalArgumentException("duplicate group header");
        }
        if (index > 0 && groupLanes[(index - 1) * 3 + 1] > firstModelRow) {
            throw new IllegalArgumentException("overlapping row group");
        }
        if (index < groupCount && endModelRowExclusive > groupLanes[index * 3]) {
            throw new IllegalArgumentException("overlapping row group");
        }
        if (groupCount == 4_096) throw new IllegalArgumentException("group capacity exceeded");
        if (groupCount == groupLanes.length / 3) {
            groupLanes = Arrays.copyOf(groupLanes, groupLanes.length * 2);
            groupLabels = Arrays.copyOf(groupLabels, groupLabels.length * 2);
        }
        System.arraycopy(groupLanes, index * 3, groupLanes, (index + 1) * 3, (groupCount - index) * 3);
        System.arraycopy(groupLabels, index, groupLabels, index + 1, groupCount - index);
        groupLanes[index * 3] = firstModelRow;
        groupLanes[index * 3 + 1] = endModelRowExclusive;
        groupLanes[index * 3 + 2] = 0;
        groupLabels[index] = title;
        groupCount++;
        if (groupSorter == null) {
            groupSorter = new GroupRowSorter();
            super.setRowSorter(groupSorter);
            installGroupMouse();
        }
        groupSorter.rebuild();
        repaint();
    }

    public String getRowGroupTitle(int firstModelRow) {
        int index = groupAtStart(firstModelRow);
        return index < 0 ? null : groupLabels[index];
    }

    public boolean isRowGroupCollapsed(int firstModelRow) {
        int index = groupAtStart(firstModelRow);
        if (index < 0) throw new IllegalArgumentException("unknown group header");
        return groupLanes[index * 3 + 2] != 0;
    }

    public void setRowGroupCollapsed(int firstModelRow, boolean collapsed) {
        requireEdt();
        int index = groupAtStart(firstModelRow);
        if (index < 0) throw new IllegalArgumentException("unknown group header");
        if ((groupLanes[index * 3 + 2] != 0) == collapsed) return;
        groupLanes[index * 3 + 2] = collapsed ? 1 : 0;
        if (groupSorter != null) groupSorter.rebuild();
        revalidate();
        repaint();
    }

    public void removeRowGroup(int firstModelRow) {
        requireEdt();
        int index = groupAtStart(firstModelRow);
        if (index < 0) return;
        System.arraycopy(groupLanes, (index + 1) * 3, groupLanes, index * 3, (groupCount - index - 1) * 3);
        System.arraycopy(groupLabels, index + 1, groupLabels, index, groupCount - index - 1);
        groupCount--;
        Arrays.fill(groupLanes, groupCount * 3, groupCount * 3 + 3, 0);
        groupLabels[groupCount] = null;
        if (groupCount == 0) detachGroupSorter();
        else groupSorter.rebuild();
        repaint();
    }

    public void clearRowGroups() {
        requireEdt();
        if (groupCount == 0) return;
        Arrays.fill(groupLanes, 0);
        Arrays.fill(groupLabels, null);
        groupCount = 0;
        detachGroupSorter();
        repaint();
    }

    /** Returns the model header row for a displayed group-header view row, else -1. */
    public int groupHeaderAtViewRow(int viewRow) {
        if (groupSorter == null || viewRow < 0 || viewRow >= getRowCount()) return -1;
        int modelRow = convertRowIndexToModel(viewRow);
        return groupAtStart(modelRow) >= 0 ? modelRow : -1;
    }

    /** Binary search over sparse sorted model-header ordinals. */
    private int findGroupInsertion(int header) {
        int lo = 0, hi = groupCount;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (groupLanes[mid * 3] < header) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    private int groupAtStart(int header) {
        int index = findGroupInsertion(header);
        return index < groupCount && groupLanes[index * 3] == header ? index : -1;
    }

    /** An explicit external sorter overrides opt-in grouping, never vice versa. */
    @Override public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        if (groupSorter != null && sorter != groupSorter) {
            groupSorter = null;
            groupCount = 0;
            Arrays.fill(groupLanes, 0);
            Arrays.fill(groupLabels, null);
            removeGroupMouse();
        }
        super.setRowSorter(sorter);
    }

    /** Structural model changes invalidate bare model-row group ordinals. */
    private void clearGroupsOnStructure() {
        if (groupSorter != null) {
            groupSorter = null;
            groupCount = 0;
            Arrays.fill(groupLanes, 0);
            Arrays.fill(groupLabels, null);
            removeGroupMouse();
            super.setRowSorter(null);
        }
    }

    private void detachGroupSorter() {
        if (groupSorter != null) {
            groupSorter = null;
            removeGroupMouse();
            super.setRowSorter(null);
        }
    }

    private void installGroupMouse() {
        if (groupMouse != null) return;
        groupMouse = new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) return;
                Rectangle visible = getVisibleRect();
                if (event.getX() < visible.x || event.getX() >= visible.x + 20) return;
                int viewRow = rowAtPoint(event.getPoint());
                int modelHeader = groupHeaderAtViewRow(viewRow);
                if (modelHeader >= 0) setRowGroupCollapsed(modelHeader, !isRowGroupCollapsed(modelHeader));
            }
        };
        addMouseListener(groupMouse);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.ALT_DOWN_MASK), "m3.toggleRowGroup");
        getActionMap().put("m3.toggleRowGroup", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                int header = groupHeaderAtViewRow(getSelectedRow());
                if (header >= 0) setRowGroupCollapsed(header, !isRowGroupCollapsed(header));
            }
        });
    }

    private void removeGroupMouse() {
        if (groupMouse == null) return;
        removeMouseListener(groupMouse);
        groupMouse = null;
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.ALT_DOWN_MASK));
        getActionMap().remove("m3.toggleRowGroup");
    }

    /** Sparse identity RowSorter with O(log G) ordinal projection; no row entries. */
    private final class GroupRowSorter extends RowSorter<TableModel> {
        private int[] hiddenStart = new int[8];
        private int[] hiddenEnd = new int[8];
        private int[] viewThreshold = new int[8];
        private int[] prefixHidden = new int[8];
        private int hiddenGroups;
        private int totalHidden;

        @Override public TableModel getModel() { return M3Grid.this.getModel(); }
        @Override public int getModelRowCount() { return getModel().getRowCount(); }
        @Override public int getViewRowCount() { return getModelRowCount() - totalHidden; }
        @Override public List<? extends SortKey> getSortKeys() { return List.of(); }
        @Override public void toggleSortOrder(int column) {
            // JTableHeader calls this on an ordinary user click. Grouping
            // has no ordering comparator: preserve model order without
            // propagating an exception through Swing's event dispatch thread.
            if (column < 0 || column >= getModel().getColumnCount()) {
                throw new IndexOutOfBoundsException("sort column");
            }
        }
        @Override public void setSortKeys(List<? extends SortKey> keys) {
            if (keys != null && !keys.isEmpty()) {
                throw new UnsupportedOperationException("row-group sorter does not reorder model rows");
            }
        }

        private int countAtOrBefore(int[] sorted, int length, int value) {
            int low = 0, high = length;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (sorted[middle] <= value) low = middle + 1;
                else high = middle;
            }
            return low;
        }

        @Override public int convertRowIndexToModel(int viewRow) {
            if (viewRow < 0 || viewRow >= getViewRowCount()) throw new IndexOutOfBoundsException("view row");
            int completed = countAtOrBefore(viewThreshold, hiddenGroups, viewRow);
            int removed = completed == 0 ? 0 : prefixHidden[completed - 1];
            return viewRow + removed;
        }
        @Override public int convertRowIndexToView(int modelRow) {
            if (modelRow < 0 || modelRow >= getModelRowCount()) throw new IndexOutOfBoundsException("model row");
            int preceding = countAtOrBefore(hiddenStart, hiddenGroups, modelRow);
            if (preceding == 0) return modelRow;
            int slot = preceding - 1;
            if (modelRow < hiddenEnd[slot]) return -1;
            return modelRow - prefixHidden[slot];
        }
        void rebuild() {
            int needed = Math.max(8, groupCount);
            if (needed > hiddenStart.length) {
                hiddenStart = Arrays.copyOf(hiddenStart, needed);
                hiddenEnd = Arrays.copyOf(hiddenEnd, needed);
                viewThreshold = Arrays.copyOf(viewThreshold, needed);
                prefixHidden = Arrays.copyOf(prefixHidden, needed);
            }
            hiddenGroups = 0;
            totalHidden = 0;
            for (int group = 0; group < groupCount; group++) {
                int b = group * 3;
                if (groupLanes[b + 2] == 0) continue;
                int header = groupLanes[b], exclusive = groupLanes[b + 1];
                int slot = hiddenGroups++;
                hiddenStart[slot] = header + 1;
                hiddenEnd[slot] = exclusive;
                viewThreshold[slot] = header + 1 - totalHidden;
                totalHidden += exclusive - header - 1;
                prefixHidden[slot] = totalHidden;
            }
            fireRowSorterChanged((int[]) null);
        }
        @Override public void modelStructureChanged() { rebuild(); }
        @Override public void allRowsChanged() { rebuild(); }
        @Override public void rowsInserted(int first, int end) { rebuild(); }
        @Override public void rowsDeleted(int first, int end) { rebuild(); }
        @Override public void rowsUpdated(int first, int end) { }
        @Override public void rowsUpdated(int first, int end, int column) { }
    }

    /**
     * JTable serializes the TableModel but its RowSorter is not a durable
     * projection. Reconnect only the sparse group behavior after validating
     * the saved lane structure; never leave visible group flags inconsistent
     * with the restored logical row count.
     */
    @Serial
    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        // Serialized pre-group M3Grid instances do not contain these fields.
        // Restore an empty sparse lane rather than rejecting older streams.
        if (groupCount == 0) {
            if (groupLanes == null) groupLanes = new int[24];
            if (groupLabels == null) groupLabels = new String[8];
            return;
        }
        if (groupCount < 0 || groupCount > 4_096 || groupLanes == null || groupLabels == null
                || (long) groupCount * 3 > groupLanes.length || groupCount > groupLabels.length) {
            throw new InvalidObjectException("invalid group lane count");
        }
        int previousEnd = 0, modelRows = getModel().getRowCount();
        for (int i = 0; i < groupCount; i++) {
            int at = i * 3;
            int start = groupLanes[at], end = groupLanes[at + 1], collapsed = groupLanes[at + 2];
            if (start < previousEnd || end <= start + 1 || end > modelRows
                    || (collapsed != 0 && collapsed != 1) || groupLabels[i] == null) {
                throw new InvalidObjectException("invalid serialized group bounds/state");
            }
            previousEnd = end;
        }
        // The JTable's transient sorter was not serialized. The sparse
        // projection is deterministically rebuilt from the original model
        // ordinals and collapsed flags without materializing model rows.
        groupSorter = new GroupRowSorter();
        super.setRowSorter(groupSorter);
        installGroupMouse();
        groupSorter.rebuild();
    }

    /** Configured model-coordinate spans; stock JTable behavior when empty. */
    public int getCellSpanCount() { return spanCount; }

    /** Number of merged-cell renderer stamps actually painted. */
    public long getSpanPaintedCells() { return spanPaintedCells; }

    /**
     * Creates/replaces a rectangle in *model* coordinates. A configured span
     * follows its original model cells across sorting/filtering and column
     * reordering only when they still form an ascending contiguous view
     * rectangle. Otherwise it is inactive, never attached to the wrong data.
     * INSERT/DELETE/structure events clear spans because ordinary Swing
     * TableModel rows have no stable identity contract across such edits.
     */
    public void setCellSpan(int modelRow, int modelColumn, int rows, int columns) {
        requireEdt();
        TableModel model = getModel();
        if (modelRow < 0 || modelColumn < 0 || rows < 1 || columns < 1
                || rows > model.getRowCount() - modelRow
                || columns > model.getColumnCount() - modelColumn) {
            throw new IllegalArgumentException("span outside model bounds");
        }
        if (rows == 1 && columns == 1) {
            removeCellSpan(modelRow, modelColumn);
            return;
        }
        int existing = -1;
        for (int i = 0; i < spanCount; i++) {
            int b = i * 4;
            if (spanLanes[b] == modelRow && spanLanes[b + 1] == modelColumn) {
                existing = i;
                continue;
            }
            if ((long) modelRow < (long) spanLanes[b] + spanLanes[b + 2]
                    && (long) spanLanes[b] < (long) modelRow + rows
                    && (long) modelColumn < (long) spanLanes[b + 1] + spanLanes[b + 3]
                    && (long) spanLanes[b + 1] < (long) modelColumn + columns) {
                throw new IllegalArgumentException("overlapping model cell spans");
            }
        }
        if (existing < 0) {
            if (spanCount == 8_192) throw new IllegalArgumentException("too many spans");
            if (spanCount == spanLanes.length / 4) spanLanes = Arrays.copyOf(spanLanes, spanLanes.length * 2);
            existing = spanCount++;
        }
        int b = existing * 4;
        spanLanes[b] = modelRow;
        spanLanes[b + 1] = modelColumn;
        spanLanes[b + 2] = rows;
        spanLanes[b + 3] = columns;
        repaint();
    }

    public void removeCellSpan(int modelRow, int modelColumn) {
        requireEdt();
        for (int i = 0; i < spanCount; i++) {
            int b = i * 4;
            if (spanLanes[b] != modelRow || spanLanes[b + 1] != modelColumn) continue;
            System.arraycopy(spanLanes, b + 4, spanLanes, b, (spanCount - i - 1) * 4);
            spanCount--;
            Arrays.fill(spanLanes, spanCount * 4, spanCount * 4 + 4, 0);
            repaint();
            return;
        }
    }

    public void clearCellSpans() {
        requireEdt();
        if (spanCount == 0) return;
        spanCount = 0;
        Arrays.fill(spanLanes, 0);
        repaint();
    }

    /** Rebuild spans only on request; structural row events invalidate model-row ordinals. */
    @Override public void tableChanged(TableModelEvent event) {
        if (groupLanes != null && (event == null || event.getFirstRow() == TableModelEvent.HEADER_ROW
                || event.getType() == TableModelEvent.INSERT || event.getType() == TableModelEvent.DELETE)) {
            clearGroupsOnStructure();
        }
        super.tableChanged(event);
        if (spanLanes != null && spanCount > 0
                && (event == null || event.getFirstRow() == TableModelEvent.HEADER_ROW
                    || event.getType() == TableModelEvent.INSERT
                    || event.getType() == TableModelEvent.DELETE)) {
            spanCount = 0;
            Arrays.fill(spanLanes, 0);
            repaint();
        }
    }

    /**
     * A packed model-to-view projection; -1 when filtered, reordered into
     * a nonrectangle, intersecting frozen planes, or outside the current
     * view. Sorting always belongs to the original JTable/RowSorter.
     */
    private long spanViewOrigin(int span) {
        if (!getComponentOrientation().isLeftToRight()) return -1;
        int b = span * 4;
        int mr = spanLanes[b], mc = spanLanes[b + 1];
        int nr = spanLanes[b + 2], nc = spanLanes[b + 3];
        if (mr < 0 || mc < 0 || nr < 1 || nc < 1
                || nr > getModel().getRowCount() - mr || nc > getModel().getColumnCount() - mc) return -1;
        int vr = convertRowIndexToView(mr), vc = convertColumnIndexToView(mc);
        if (vr < 0 || vc < 0 || nr > getRowCount() - vr || nc > getColumnCount() - vc) return -1;
        // Span coordinates are never allowed to straddle frozen body planes.
        int top = getPinnedTopRowCount();
        int bodyEnd = getRowCount() - getPinnedBottomRowCount();
        if (vr < top || nr > bodyEnd - vr || vc < getFrozenColumnCount()) return -1;
        // No sorter means model order == view row order, so even a million-row
        // span is an O(1) identity projection rather than a million-row walk.
        // A sorter can arbitrarily permute rows: verify up to a bounded range
        // and fail closed for larger spans, rather than doing O(n) paint work.
        if (getRowSorter() != null) {
            if (nr > 4_096) return -1;
            for (int i = 1; i < nr; i++) if (convertRowIndexToView(mr + i) != vr + i) return -1;
        }
        // Column model movement/hiding is independently validated. A huge
        // nontrivial column span needs a future indexed proof, not a paint walk.
        if (nc > 4_096) return -1;
        for (int i = 1; i < nc; i++) if (convertColumnIndexToView(mc + i) != vc + i) return -1;
        return ((long) vr << 32) | (vc & 0xffffffffL);
    }

    /** Find an active span for the supplied JTable view cell, without objects. */
    private int spanForViewCell(int viewRow, int viewColumn) {
        if (spanCount == 0 || viewRow < 0 || viewColumn < 0) return -1;
        for (int i = 0; i < spanCount; i++) {
            long packed = spanViewOrigin(i);
            if (packed < 0) continue;
            int r = (int) (packed >>> 32), c = (int) packed;
            if (viewRow >= r && viewRow < r + spanLanes[i * 4 + 2]
                    && viewColumn >= c && viewColumn < c + spanLanes[i * 4 + 3]) return i;
        }
        return -1;
    }

    /**
     * Writes [anchorViewRow,anchorViewColumn,rowSpan,columnSpan] into caller
     * storage when the cell belongs to an active span. No allocation.
     */
    public boolean cellSpanAt(int viewRow, int viewColumn, int[] output, int offset) {
        if (output == null) throw new NullPointerException("output");
        if (offset < 0 || offset > output.length - 4) throw new IndexOutOfBoundsException("output lanes");
        int found = spanForViewCell(viewRow, viewColumn);
        if (found < 0) return false;
        long origin = spanViewOrigin(found);
        output[offset] = (int) (origin >>> 32);
        output[offset + 1] = (int) origin;
        output[offset + 2] = spanLanes[found * 4 + 2];
        output[offset + 3] = spanLanes[found * 4 + 3];
        return true;
    }

    /** Original stock cell bounds outside an active span; merged bounds only on caller query. */
    public Rectangle spannedCellRect(int viewRow, int viewColumn) {
        int found = spanForViewCell(viewRow, viewColumn);
        if (found < 0) return super.getCellRect(viewRow, viewColumn, false);
        long origin = spanViewOrigin(found);
        return spanRect((int) (origin >>> 32), (int) origin,
                spanLanes[found * 4 + 2], spanLanes[found * 4 + 3]);
    }

    private Rectangle spanRect(int row, int column, int rows, int columns) {
        Rectangle start = super.getCellRect(row, column, false);
        Rectangle end = super.getCellRect(row + rows - 1, column + columns - 1, false);
        return new Rectangle(start.x, start.y,
                (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) end.x + end.width - start.x)),
                (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) end.y + end.height - start.y)));
    }

    /** Currently pinned first view rows, automatically bounded after a filter/model change. */
    public int getPinnedTopRowCount() { return Math.min(pinnedTopRows, getRowCount()); }

    /** Currently pinned last view rows, excluding the first pinned rows. */
    public int getPinnedBottomRowCount() {
        return Math.min(pinnedBottomRows, Math.max(0, getRowCount() - getPinnedTopRowCount()));
    }

    /** Number of pinned-row cells actually rendered since construction. */
    public long getPinnedPaintedCells() { return pinnedPaintedCells; }

    /**
     * Pins the first and last rows of the current *view* (including its sorter
     * and filter). Uses only the original JTable renderers and TableModel;
     * no per-row objects or copied data. The default (0, 0) is stock Swing.
     *
     * <p>The enclosing viewport's original scrolling mode is restored when
     * pinning is disabled. While rows are pinned, SIMPLE_SCROLL_MODE prevents
     * a native blit from moving the top/bottom painted planes along with the body.</p>
     */
    public void setPinnedRowCounts(int top, int bottom) {
        requireEdt();
        if (top < 0 || bottom < 0 || (long) top + bottom > getRowCount()) {
            throw new IllegalArgumentException("pinned view rows out of range");
        }
        if (pinnedTopRows == top && pinnedBottomRows == bottom) return;
        pinnedTopRows = top;
        pinnedBottomRows = bottom;
        synchronizeScrollMode();
        positionPinnedEditor();
        repaint();
    }

    private boolean rowPinsActive() {
        return (getPinnedTopRowCount() > 0 || getPinnedBottomRowCount() > 0)
                && getParent() instanceof JViewport && getComponentOrientation().isLeftToRight()
                && !isPaintingForPrint();
    }

    /** Row heights are derived from JTable's own rowModel and capped by the viewport. */
    private int pinnedTopHeight(Rectangle visible) {
        int height = 0;
        for (int row = 0, end = getPinnedTopRowCount(); row < end && height < visible.height; row++) {
            height += Math.min(getRowHeight(row), visible.height - height);
        }
        return height;
    }

    private int pinnedBottomHeight(Rectangle visible, int topHeight) {
        int height = 0, available = visible.height - topHeight;
        int end = getRowCount() - getPinnedBottomRowCount();
        for (int row = getRowCount() - 1; row >= end && height < available; row--) {
            height += Math.min(getRowHeight(row), available - height);
        }
        return height;
    }

    /** Selection of a covered merged cell resolves to its original JTable anchor. */
    @Override public int rowAtPoint(Point point) {
        int row = rawRowAtPoint(point);
        if (stockPainting || spanCount == 0 || row < 0) return row;
        int column = rawColumnAtPoint(point);
        int found = spanForViewCell(row, column);
        if (found < 0) return row;
        return (int) (spanViewOrigin(found) >>> 32);
    }

    @Override public int columnAtPoint(Point point) {
        int column = rawColumnAtPoint(point);
        if (stockPainting || spanCount == 0 || column < 0) return column;
        int row = rawRowAtPoint(point);
        int found = spanForViewCell(row, column);
        if (found < 0) return column;
        return (int) spanViewOrigin(found);
    }

    /** Convert pinned pixels to JTable's existing view-row indices. */
    private int rawRowAtPoint(Point point) {
        if (!stockPainting && rowPinsActive()) {
            Rectangle visible = getVisibleRect();
            int relative = point.y - visible.y;
            if (relative >= 0 && relative < visible.height) {
                int topHeight = pinnedTopHeight(visible);
                if (relative < topHeight) {
                    int y = 0;
                    for (int row = 0; row < getPinnedTopRowCount(); row++) {
                        y += getRowHeight(row);
                        if (relative < y) return row;
                        if (y >= topHeight) break;
                    }
                }
                int bottomHeight = pinnedBottomHeight(visible, topHeight);
                int floor = visible.height - bottomHeight;
                if (bottomHeight > 0 && relative >= floor) {
                    int y = visible.height;
                    int end = getRowCount() - getPinnedBottomRowCount();
                    for (int row = getRowCount() - 1; row >= end; row--) {
                        int start = y - getRowHeight(row);
                        if (relative >= Math.max(floor, start) && relative < y) return row;
                        y = start;
                        if (y <= floor) break;
                    }
                }
            }
        }
        return super.rowAtPoint(point);
    }

    /**
     * JTable's keyboard/mouse selection calls scrollRectToVisible with the
     * *unprojected* row/cell bounds. A pinned selection is already on screen;
     * project that rectangle into the overlay to avoid jumping the viewport
     * to row zero or the model's final row. Keep stock behavior otherwise.
     */
    @Override public void scrollRectToVisible(Rectangle rect) {
        if (rect != null && rowPinsActive()) {
            int row = super.rowAtPoint(new Point(rect.x, rect.y));
            int top = getPinnedTopRowCount();
            int bottom = getPinnedBottomRowCount();
            if (row >= 0 && (row < top || row >= getRowCount() - bottom)) {
                Rectangle visible = getVisibleRect();
                Rectangle projected = new Rectangle(rect);
                if (row < top) {
                    int y = visible.y;
                    for (int i = 0; i < row; i++) y += getRowHeight(i);
                    projected.y = y;
                } else {
                    int y = visible.y + visible.height;
                    for (int i = getRowCount() - 1; i >= row; i--) y -= getRowHeight(i);
                    projected.y = y;
                }
                int column = super.columnAtPoint(new Point(rect.x, rect.y));
                if (pinned() && column >= 0 && column < getFrozenColumnCount()) {
                    int x = visible.x;
                    for (int i = 0; i < column; i++) x += getColumnModel().getColumn(i).getWidth();
                    projected.x = x;
                }
                super.scrollRectToVisible(projected);
                return;
            }
        }
        super.scrollRectToVisible(rect);
    }

    /** Hit testing in the actual viewport, with view columns as the result. */
    private int rawColumnAtPoint(Point point) {
        if (!stockPainting && pinned() && getFrozenColumnCount() > 0) {
            int x = point.x - getVisibleRect().x;
            if (x >= 0 && x < getFrozenWidth()) {
                for (int column = 0; column < getFrozenColumnCount(); column++) {
                    x -= getColumnModel().getColumn(column).getWidth();
                    if (x < 0) return column;
                }
            }
        }
        return super.columnAtPoint(point);
    }

    private boolean pinned() {
        return frozenColumns > 0 && getParent() instanceof JViewport && getComponentOrientation().isLeftToRight()
                && !isPaintingForPrint();
    }

    @Override protected void paintComponent(Graphics g) {
        // Stock BasicTableUI must compute its *real* visible view columns;
        // pinned hit testing is exclusively for interactive mouse events.
        boolean old = stockPainting;
        stockPainting = true;
        try { super.paintComponent(g); } finally { stockPainting = old; }
        if (pinned()) paintFrozenColumns(g);
        if (rowPinsActive()) paintPinnedRows(g);
        if (spanCount != 0) paintCellSpans(g);
        if (groupCount > 0 && groupSorter != null && !isPaintingForPrint()) paintGroupHeaders(g);
    }

    /**
     * Finds the first group header in the physical viewport in O(log groups)
     * and draws only resident headings. Pinned top/bottom headings are queried
     * separately; no per-model-row objects, temporary arrays or paint scans of
     * all groups are required.
     */
    private void paintGroupHeaders(Graphics graphics) {
        Rectangle visible = getVisibleRect();
        if (visible.isEmpty() || getColumnCount() == 0 || getRowCount() == 0) return;
        int topHeight = rowPinsActive() ? pinnedTopHeight(visible) : 0;
        int bottomHeight = rowPinsActive() ? pinnedBottomHeight(visible, topHeight) : 0;
        int firstY = visible.y + topHeight;
        int lastY = visible.y + visible.height - bottomHeight - 1;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setFont(getFont());
            if (lastY >= firstY) {
                int firstView = super.rowAtPoint(new Point(visible.x, firstY));
                int lastView = super.rowAtPoint(new Point(visible.x, lastY));
                if (firstView >= 0 && lastView >= firstView) {
                    int startModel = convertRowIndexToModel(firstView);
                    int endModel = convertRowIndexToModel(lastView);
                    int firstGroup = findGroupInsertion(startModel);
                    for (int group = firstGroup; group < groupCount && groupLanes[group * 3] <= endModel; group++) {
                        paintOneGroupHeader(g, visible, group);
                    }
                }
            }
            if (rowPinsActive() && topHeight > 0) {
                int y = 0;
                for (int row = 0; row < getPinnedTopRowCount() && y < topHeight; row++) {
                    int model = convertRowIndexToModel(row);
                    int group = groupAtStart(model);
                    if (group >= 0) paintOneGroupHeader(g, visible, group);
                    y += getRowHeight(row);
                }
            }
            if (rowPinsActive() && bottomHeight > 0) {
                int height = 0;
                int floor = getRowCount() - getPinnedBottomRowCount();
                for (int row = getRowCount() - 1; row >= floor && height < bottomHeight; row--) {
                    int model = convertRowIndexToModel(row);
                    int group = groupAtStart(model);
                    if (group >= 0) paintOneGroupHeader(g, visible, group);
                    height += getRowHeight(row);
                }
            }
        } finally { g.dispose(); }
    }

    private void paintOneGroupHeader(Graphics2D graphics, Rectangle visible, int group) {
        int modelHeader = groupLanes[group * 3];
        if (modelHeader >= getModel().getRowCount()) return;
        int row = convertRowIndexToView(modelHeader);
        if (row < 0 || row >= getRowCount()) return;
        Rectangle cell = super.getCellRect(row, 0, true);
        int y = cell.y;
        if (rowPinsActive()) {
            if (row < getPinnedTopRowCount()) {
                y = visible.y;
                for (int i = 0; i < row; i++) y += getRowHeight(i);
            } else if (row >= getRowCount() - getPinnedBottomRowCount()) {
                y = visible.y + visible.height;
                for (int i = getRowCount() - 1; i >= row; i--) y -= getRowHeight(i);
            }
        }
        int height = cell.height;
        int titleWidth = Math.min(200, visible.width);
        if (titleWidth <= 0 || y >= visible.y + visible.height || y + height <= visible.y) return;
        Rectangle dirty = graphics.getClipBounds();
        if (dirty != null && !dirty.intersects(visible.x, y, titleWidth, height)) return;
        int x = visible.x;
        graphics.setColor(getBackground());
        graphics.fillRect(x, y, titleWidth, height);
        graphics.setColor(getForeground());
        int middleY = y + height / 2;
        // Draw one disclosure triangle without per-group int[] allocations.
        if (groupLanes[group * 3 + 2] == 0) {
            graphics.drawLine(x + 5, middleY - 4, x + 13, middleY - 4);
            graphics.drawLine(x + 5, middleY - 3, x + 9, middleY + 3);
            graphics.drawLine(x + 13, middleY - 3, x + 9, middleY + 3);
        } else {
            graphics.drawLine(x + 6, middleY - 5, x + 13, middleY);
            graphics.drawLine(x + 13, middleY, x + 6, middleY + 5);
            graphics.drawLine(x + 6, middleY - 4, x + 6, middleY + 4);
        }
        String label = groupLabels[group];
        if (label != null && !label.isEmpty()) {
            java.awt.Shape clip = graphics.getClip();
            try {
                graphics.clipRect(x + 20, y, Math.max(0, titleWidth - 20), height);
                FontMetrics metrics = graphics.getFontMetrics();
                graphics.drawString(label, x + 21,
                        middleY + (metrics.getAscent() - metrics.getDescent()) / 2);
            } finally { graphics.setClip(clip); }
        }
        groupHeaderPainted++;
    }

    private void paintFrozenColumns(Graphics g) {
        Rectangle visible = getVisibleRect();
        if (visible.isEmpty()) return;
        int frozenWidth = Math.min(getFrozenWidth(), visible.width);
        if (frozenWidth <= 0) return;
        // A dirty rectangle wholly in the scrolling BODY must never repaint
        // the separate FROZEN plane (M3 viewport damage law D).
        Rectangle dirty = g.getClipBounds();
        if (dirty != null && !dirty.intersects(visible.x, visible.y, frozenWidth, visible.height)) return;
        int firstY = dirty == null ? visible.y : Math.max(visible.y, dirty.y);
        int endY = dirty == null ? visible.y + visible.height : Math.min(visible.y + visible.height, dirty.y + dirty.height);
        if (firstY >= endY) return;
        int first = rowAtPoint(new Point(visible.x, firstY));
        if (first < 0) return;
        int last = rowAtPoint(new Point(visible.x, endY - 1));
        if (last < first) last = getRowCount() - 1;
        if (frozenPainter == null) frozenPainter = new CellRendererPane();
        Graphics layer = g.create();
        try {
            layer.clipRect(visible.x, firstY, frozenWidth, endY - firstY);
            layer.setColor(getBackground());
            layer.fillRect(visible.x, firstY, frozenWidth, endY - firstY);
            int x = visible.x;
            for (int col = 0; col < getFrozenColumnCount(); col++) {
                TableColumn column = getColumnModel().getColumn(col);
                int cw = column.getWidth();
                if (cw <= 0) continue;
                for (int row = first; row <= last; row++) {
                    Rectangle cell = super.getCellRect(row, col, true);
                    if (cell.y >= endY) break;
                    if (cell.y + cell.height <= firstY) continue;
                    TableCellRenderer renderer = getCellRenderer(row, col);
                    Component component = prepareRenderer(renderer, row, col);
                    frozenPainter.paintComponent(layer, component, this, x, cell.y, cw, cell.height, true);
                    frozenPaintedCells++;
                }
                x += cw;
                if (x >= visible.x + frozenWidth) break;
            }
        } finally { layer.dispose(); }
    }

    /** Composes top and bottom planes over the stock scrolled body and frozen columns. */
    private void paintPinnedRows(Graphics graphics) {
        Rectangle visible = getVisibleRect();
        if (visible.isEmpty() || getColumnCount() == 0) return;
        int topHeight = pinnedTopHeight(visible);
        int bottomHeight = pinnedBottomHeight(visible, topHeight);
        if (topHeight + bottomHeight == 0) return;
        Rectangle clip = graphics.getClipBounds();
        boolean paintTop = topHeight > 0 && (clip == null ||
                clip.intersects(visible.x, visible.y, visible.width, topHeight));
        boolean paintBottom = bottomHeight > 0 && (clip == null ||
                clip.intersects(visible.x, visible.y + visible.height - bottomHeight,
                        visible.width, bottomHeight));
        if (!paintTop && !paintBottom) return;
        if (frozenPainter == null) frozenPainter = new CellRendererPane();
        // The raw JTable column lookup excludes the pinned-column hit mapping.
        int first = super.columnAtPoint(new Point(visible.x, visible.y));
        int last = super.columnAtPoint(new Point(visible.x + visible.width - 1, visible.y));
        if (first < 0) return;
        if (last < first) last = getColumnCount() - 1;
        Graphics layer = graphics.create();
        try {
            if (paintTop) {
                layer.clipRect(visible.x, visible.y, visible.width, topHeight);
                int y = visible.y;
                for (int row = 0; row < getPinnedTopRowCount() && y < visible.y + topHeight; row++) {
                    int h = getRowHeight(row);
                    paintPinnedRow(layer, visible, row, y, h, first, last);
                    y += h;
                }
            }
        } finally { layer.dispose(); }
        if (paintBottom) {
            layer = graphics.create();
            try {
                int floor = visible.y + visible.height - bottomHeight;
                layer.clipRect(visible.x, floor, visible.width, bottomHeight);
                int y = visible.y + visible.height;
                int limit = getRowCount() - getPinnedBottomRowCount();
                for (int row = getRowCount() - 1; row >= limit && y > floor; row--) {
                    int h = getRowHeight(row);
                    y -= h;
                    paintPinnedRow(layer, visible, row, y, h, first, last);
                }
            } finally { layer.dispose(); }
        }
    }

    /** Reuses CellRendererPane; no parallel row storage or model reads outside viewport bands. */
    private void paintPinnedRow(Graphics layer, Rectangle visible,
                                int row, int y, int height, int first, int last) {
        Rectangle clip = layer.getClipBounds();
        if (clip != null && !clip.intersects(visible.x, y, visible.width, height)) return;
        layer.setColor(getBackground());
        layer.fillRect(visible.x, y, visible.width, height);
        for (int column = first; column <= last; column++) {
            Rectangle cell = super.getCellRect(row, column, false);
            paintPinnedCell(layer, row, column, cell.x, y, cell.width, height);
        }
        if (pinned()) {
            int x = visible.x;
            for (int column = 0; column < getFrozenColumnCount() && x < visible.x + visible.width; column++) {
                int width = getColumnModel().getColumn(column).getWidth();
                paintPinnedCell(layer, row, column, x, y, width, height);
                x += width;
            }
        }
    }

    private void paintPinnedCell(Graphics layer, int row, int column,
                                 int x, int y, int width, int height) {
        Rectangle clip = layer.getClipBounds();
        if (width <= 0 || (clip != null && !clip.intersects(x, y, width, height))) return;
        TableCellRenderer renderer = getCellRenderer(row, column);
        Component component = prepareRenderer(renderer, row, column);
        frozenPainter.paintComponent(layer, component, this, x, y, width, height, true);
        pinnedPaintedCells++;
    }

    /** Merged-cell paint never materializes a row outside the visible rectangle. */
    private void paintCellSpans(Graphics graphics) {
        Rectangle visible = getVisibleRect();
        if (visible.isEmpty()) return;
        int top = rowPinsActive() ? pinnedTopHeight(visible) : 0;
        int bottom = rowPinsActive() ? pinnedBottomHeight(visible, top) : 0;
        int left = visible.x + (pinned() ? Math.min(getFrozenWidth(), visible.width) : 0);
        int width = visible.x + visible.width - left;
        int height = visible.height - top - bottom;
        if (width <= 0 || height <= 0) return;
        if (frozenPainter == null) frozenPainter = new CellRendererPane();
        Graphics layer = graphics.create();
        try {
            layer.clipRect(left, visible.y + top, width, height);
            for (int span = 0; span < spanCount; span++) {
                long origin = spanViewOrigin(span);
                if (origin < 0) continue;
                int row = (int) (origin >>> 32), col = (int) origin;
                int b = span * 4;
                Rectangle box = spanRect(row, col, spanLanes[b + 2], spanLanes[b + 3]);
                Rectangle clip = layer.getClipBounds();
                if (box.isEmpty() || (clip != null && !clip.intersects(box))) continue;
                layer.setColor(getBackground());
                layer.fillRect(box.x, box.y, box.width, box.height);
                TableCellRenderer renderer = getCellRenderer(row, col);
                Component component = prepareRenderer(renderer, row, col);
                frozenPainter.paintComponent(layer, component, this,
                        box.x, box.y, box.width, box.height, true);
                spanPaintedCells++;
            }
        } finally { layer.dispose(); }
    }

    /** Pin the native cell editor without modifying JTable's stock geometry queries. */
    @Override public boolean editCellAt(int row, int column, EventObject event) {
        boolean editing = super.editCellAt(row, column, event);
        if (editing) {
            positionPinnedEditor();
            positionSpanEditor();
        }
        return editing;
    }

    @Override public void doLayout() {
        super.doLayout();
        positionPinnedEditor();
        positionSpanEditor();
    }

    private void positionSpanEditor() {
        if (!isEditing() || spanCount == 0) return;
        int row = getEditingRow(), col = getEditingColumn();
        int found = spanForViewCell(row, col);
        if (found < 0) return;
        long origin = spanViewOrigin(found);
        int anchorRow = (int) (origin >>> 32), anchorCol = (int) origin;
        if (row != anchorRow || col != anchorCol) return;
        Component editor = getEditorComponent();
        if (editor != null) {
            int b = found * 4;
            editor.setBounds(spanRect(row, col, spanLanes[b + 2], spanLanes[b + 3]));
        }
    }

    private void positionPinnedEditor() {
        if (!isEditing()) return;
        boolean pinColumn = pinned() && getEditingColumn() < getFrozenColumnCount();
        boolean pinRow = rowPinsActive() &&
                (getEditingRow() < getPinnedTopRowCount()
                        || getEditingRow() >= getRowCount() - getPinnedBottomRowCount());
        if (!pinColumn && !pinRow) return;
        Component editor = getEditorComponent();
        if (editor == null) return;
        Rectangle cell = super.getCellRect(getEditingRow(), getEditingColumn(), false);
        if (pinColumn) {
            int x = getVisibleRect().x;
            for (int i = 0; i < getEditingColumn(); i++) x += getColumnModel().getColumn(i).getWidth();
            cell.x = x;
        }
        if (pinRow) {
            Rectangle visible = getVisibleRect();
            if (getEditingRow() < getPinnedTopRowCount()) {
                int y = visible.y;
                for (int row = 0; row < getEditingRow(); row++) y += getRowHeight(row);
                cell.y = y;
            } else {
                int y = visible.y + visible.height;
                for (int row = getRowCount() - 1; row >= getEditingRow(); row--) y -= getRowHeight(row);
                cell.y = y;
            }
        }
        editor.setBounds(cell);
    }

    /** Keep pinned top/bottom planes from being blitted along with a scrolling body. */
    private void synchronizeScrollMode() {
        JViewport viewport = getParent() instanceof JViewport current ? current : null;
        boolean needed = viewport != null && (pinnedTopRows > 0 || pinnedBottomRows > 0);
        if (scrollModeViewport != null && (scrollModeViewport != viewport || !needed)) {
            if (scrollModeViewport.getScrollMode() == JViewport.SIMPLE_SCROLL_MODE && previousScrollMode >= 0) {
                scrollModeViewport.setScrollMode(previousScrollMode);
            }
            scrollModeViewport = null;
            previousScrollMode = -1;
        }
        if (needed && scrollModeViewport == null) {
            scrollModeViewport = viewport;
            previousScrollMode = viewport.getScrollMode();
            viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        }
    }

    @Override public void addNotify() {
        super.addNotify();
        synchronizeScrollMode();
    }

    @Override public void removeNotify() {
        if (scrollModeViewport != null) {
            JViewport viewport = scrollModeViewport;
            scrollModeViewport = null;
            if (previousScrollMode >= 0 && viewport.getScrollMode() == JViewport.SIMPLE_SCROLL_MODE) {
                viewport.setScrollMode(previousScrollMode);
            }
            previousScrollMode = -1;
        }
        super.removeNotify();
    }

    private void installHeader() {
        if (pinnedHeader != null) return;
        originalHeader = getTableHeader();
        pinnedHeader = new PinnedHeader(getColumnModel());
        if (originalHeader != null) {
            pinnedHeader.setDefaultRenderer(originalHeader.getDefaultRenderer());
            pinnedHeader.setReorderingAllowed(originalHeader.getReorderingAllowed());
            pinnedHeader.setResizingAllowed(originalHeader.getResizingAllowed());
            pinnedHeader.setFont(originalHeader.getFont());
            pinnedHeader.setForeground(originalHeader.getForeground());
            pinnedHeader.setBackground(originalHeader.getBackground());
            pinnedHeader.setComponentOrientation(originalHeader.getComponentOrientation());
        }
        setTableHeader(pinnedHeader);
        syncScrollPaneHeader(pinnedHeader);
    }

    private void uninstallHeader() {
        if (pinnedHeader == null) return;
        JTableHeader old = originalHeader;
        pinnedHeader = null;
        originalHeader = null;
        if (old != null) old.setColumnModel(getColumnModel());
        setTableHeader(old);
        syncScrollPaneHeader(old);
    }

    private void syncScrollPaneHeader(JTableHeader header) {
        if (getParent() instanceof JViewport viewport && viewport.getParent() instanceof JScrollPane scroll
                && scroll.getViewport() == viewport) {
            scroll.setColumnHeaderView(header);
            if (scroll.getColumnHeader() != null) {
                scroll.getColumnHeader().setViewPosition(new Point(viewport.getViewPosition().x, 0));
            }
            scroll.revalidate();
        }
    }

    private final class PinnedHeader extends JTableHeader {
        private static final long serialVersionUID = 1L;
        private transient CellRendererPane painter;
        private transient boolean stockPainting;

        PinnedHeader(TableColumnModel columns) { super(columns); }

        @Override public int columnAtPoint(Point point) {
            if (!stockPainting && pinned() && getFrozenColumnCount() > 0) {
                int x = point.x - getVisibleRect().x;
                if (x >= 0 && x < getFrozenWidth()) {
                    for (int column = 0; column < getFrozenColumnCount(); column++) {
                        x -= getColumnModel().getColumn(column).getWidth();
                        if (x < 0) return column;
                    }
                }
            }
            return super.columnAtPoint(point);
        }

        @Override protected void paintComponent(Graphics g) {
            boolean old = stockPainting;
            stockPainting = true;
            try { super.paintComponent(g); } finally { stockPainting = old; }
            if (!pinned()) return;
            Rectangle visible = getVisibleRect();
            int pinnedWidth = Math.min(getFrozenWidth(), visible.width);
            if (visible.isEmpty() || pinnedWidth <= 0) return;
            Rectangle dirty = g.getClipBounds();
            if (dirty != null && !dirty.intersects(visible.x, visible.y, pinnedWidth, visible.height)) return;
            if (painter == null) painter = new CellRendererPane();
            Graphics layer = g.create();
            try {
                layer.clipRect(visible.x, visible.y, pinnedWidth, visible.height);
                layer.setColor(getBackground());
                layer.fillRect(visible.x, visible.y, pinnedWidth, visible.height);
                int x = visible.x;
                for (int i = 0; i < getFrozenColumnCount(); i++) {
                    TableColumn column = getColumnModel().getColumn(i);
                    int width = column.getWidth();
                    TableCellRenderer renderer = column.getHeaderRenderer() != null ? column.getHeaderRenderer() : getDefaultRenderer();
                    Component component = renderer.getTableCellRendererComponent(M3Grid.this,
                            column.getHeaderValue(), false, false, -1, i);
                    painter.paintComponent(layer, component, this, x, 0, width, getHeight(), true);
                    x += width;
                }
            } finally { layer.dispose(); }
        }
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Swing EDT required");
    }
}
