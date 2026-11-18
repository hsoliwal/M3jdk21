/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Component;
import java.awt.Dimension;
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
    // Original model-column header ranges. No extra ColumnModel or table.
    private int[] columnGroupLanes = new int[16];
    private String[] columnGroupLabels = new String[8];
    // Optional Nebula summary/detail semantics. -1 means a title-only
    // group; each configured group retains its original TableColumn objects
    // in one small array, not duplicate models, columns, or per-cell peers.
    private int[] columnGroupSummary = new int[8];
    private boolean[] columnGroupCollapsed = new boolean[8];
    private TableColumn[][] columnGroupOwners = new TableColumn[8][];
    private int columnGroupCount;
    // Additive ancestor-title bands: [modelFirst, modelCount, level] in compact
    // primitive lanes. Level 1 contains first-tier groups, level 2 contains
    // level-1 groups, etc. No second TableColumnModel or cell objects.
    private int[] ancestorGroupLanes = new int[24];
    private String[] ancestorGroupTitles = new String[8];
    private int ancestorGroupCount;
    private transient long ancestorGroupPainted;
    private static final int MAX_ANCESTOR_LEVEL = 3;
    private static final int MAX_ANCESTOR_GROUPS = 256;
    private transient long columnGroupPainted;
    private static final int COLUMN_GROUP_BAND_HEIGHT = 24;
    private JTableHeader originalHeader;
    private PinnedHeader pinnedHeader;

    public M3Grid(TableModel model) { super(model); }

    /** The number of leading, currently displayed view columns to pin. */
    public int getFrozenColumnCount() { return Math.min(frozenColumns, getColumnCount()); }

    public void setFrozenColumnCount(int count) {
        requireEdt();
        if (count < 0 || count > getColumnCount()) throw new IllegalArgumentException("frozen columns out of range");
        // A group must remain entirely in the frozen or scrolling plane.
        if (columnGroupCount > 0 && columnGroupSummary != null) {
            for (int i = 0; i < columnGroupCount; i++) {
                if (columnGroupSummary[i] < 0) continue;
                int start = columnGroupViewStart(i);
                int width = columnGroupVisibleCount(i);
                if (start >= 0 && start < count && start + width > count) {
                    throw new IllegalArgumentException("frozen boundary splits a summary/detail group");
                }
            }
        }
        if (frozenColumns == count) return;
        frozenColumns = count;
        if (count == 0 && columnGroupCount == 0) uninstallHeader();
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

    /** Number of retained ancestor header bands above the existing first tier. */
    public int getColumnAncestorGroupCount() { return ancestorGroupCount; }

    /** Maximum configured ancestor level, 0 when there are none. */
    public int getColumnAncestorDepth() {
        int depth = 0;
        for (int i = 0; i < ancestorGroupCount; i++) {
            depth = Math.max(depth, ancestorGroupLanes[i * 3 + 2]);
        }
        return depth;
    }

    /** Number of visible ancestor header labels drawn through the real Swing header. */
    public long getColumnAncestorPainted() { return ancestorGroupPainted; }

    /** Height of all group-header bands, excluding the native leaf-column tier. */
    public int getColumnGroupBandsHeight() {
        return columnGroupCount == 0 ? 0 : (getColumnAncestorDepth() + 1) * COLUMN_GROUP_BAND_HEIGHT;
    }

    /**
     * Defines one immutable model-order ancestor title band. Level 1 wraps
     * existing first-tier groups, and higher levels wrap complete groups at
     * the next lower level. Every interval must be an exact tiling of at
     * least two direct child groups; gaps and partial overlaps fail before
     * any state is changed. Configure bottom-up; only title replacement is
     * allowed under a level that already has higher ancestors.
     *
     * <p>Its painted viewport projection is fail-closed when a child group is
     * reordered, hidden by an external column model, or crosses a frozen
     * plane. Existing summary/detail groups continue using their original
     * TableColumn owners and visibility logic.</p>
     */
    public void setColumnGroupAncestor(int level, int firstModelColumn, int modelColumnCount, String title) {
        requireEdt();
        if (level < 1 || level > MAX_ANCESTOR_LEVEL) throw new IllegalArgumentException("ancestor level");
        java.util.Objects.requireNonNull(title, "title");
        if (firstModelColumn < 0 || modelColumnCount < 2
                || modelColumnCount > getModel().getColumnCount() - firstModelColumn) {
            throw new IllegalArgumentException("ancestor interval outside model");
        }
        int at = ancestorSlot(level, firstModelColumn);
        if (at >= 0) {
            int b = at * 3;
            if (ancestorGroupLanes[b + 1] != modelColumnCount) {
                throw new IllegalStateException("remove higher levels before changing an ancestor interval");
            }
            ancestorGroupTitles[at] = title;
            refreshGroupedHeader();
            return;
        }
        if (getColumnAncestorDepth() > level) {
            throw new IllegalStateException("configure ancestors from lower to higher levels");
        }
        if (ancestorGroupCount >= MAX_ANCESTOR_GROUPS) {
            throw new IllegalArgumentException("ancestor group capacity");
        }
        int exclusive = firstModelColumn + modelColumnCount;
        if (!ancestorTiledByChildren(level, firstModelColumn, exclusive)) {
            throw new IllegalArgumentException("ancestor must contain complete adjacent child groups");
        }
        for (int i = 0; i < ancestorGroupCount; i++) {
            int b = i * 3;
            if (ancestorGroupLanes[b + 2] != level) continue;
            int otherStart = ancestorGroupLanes[b], otherEnd = otherStart + ancestorGroupLanes[b + 1];
            if (firstModelColumn < otherEnd && otherStart < exclusive) {
                throw new IllegalArgumentException("overlapping ancestors at one level");
            }
        }
        if (ancestorGroupCount == ancestorGroupLanes.length / 3) {
            ancestorGroupLanes = Arrays.copyOf(ancestorGroupLanes, ancestorGroupLanes.length * 2);
            ancestorGroupTitles = Arrays.copyOf(ancestorGroupTitles, ancestorGroupTitles.length * 2);
        }
        int b = ancestorGroupCount * 3;
        ancestorGroupLanes[b] = firstModelColumn;
        ancestorGroupLanes[b + 1] = modelColumnCount;
        ancestorGroupLanes[b + 2] = level;
        ancestorGroupTitles[ancestorGroupCount++] = title;
        refreshGroupedHeader();
    }

    /** Title lookup by exact level and starting model column. */
    public String getColumnGroupAncestorTitle(int level, int firstModelColumn) {
        int index = ancestorSlot(level, firstModelColumn);
        return index < 0 ? null : ancestorGroupTitles[index];
    }

    /** Removal also clears higher-level ancestors whose child topology changed. */
    public void removeColumnGroupAncestor(int level, int firstModelColumn) {
        requireEdt();
        int index = ancestorSlot(level, firstModelColumn);
        if (index < 0) return;
        int oldCount = ancestorGroupCount;
        for (int i = oldCount - 1; i >= 0; i--) {
            if (i == index || ancestorGroupLanes[i * 3 + 2] > level) removeAncestorSlot(i);
        }
        refreshGroupedHeader();
    }

    /** Restore the original single-tier header without changing any leaf columns. */
    public void clearColumnGroupAncestors() {
        requireEdt();
        if (ancestorGroupCount == 0) return;
        clearAncestorsInternal();
        refreshGroupedHeader();
    }

    private void clearAncestorsInternal() {
        ancestorGroupCount = 0;
        if (ancestorGroupLanes != null) Arrays.fill(ancestorGroupLanes, 0);
        if (ancestorGroupTitles != null) Arrays.fill(ancestorGroupTitles, null);
    }

    private void removeAncestorSlot(int index) {
        int b = index * 3;
        System.arraycopy(ancestorGroupLanes, b + 3, ancestorGroupLanes, b,
                (ancestorGroupCount - index - 1) * 3);
        System.arraycopy(ancestorGroupTitles, index + 1, ancestorGroupTitles, index,
                ancestorGroupCount - index - 1);
        ancestorGroupCount--;
        Arrays.fill(ancestorGroupLanes, ancestorGroupCount * 3, ancestorGroupCount * 3 + 3, 0);
        ancestorGroupTitles[ancestorGroupCount] = null;
    }

    private int ancestorSlot(int level, int modelFirst) {
        for (int i = 0; i < ancestorGroupCount; i++) {
            int b = i * 3;
            if (ancestorGroupLanes[b + 2] == level && ancestorGroupLanes[b] == modelFirst) return i;
        }
        return -1;
    }

    /** Require exact model-order coverage by at least two direct children. */
    private boolean ancestorTiledByChildren(int level, int first, int exclusive) {
        int at = first, count = 0;
        while (at < exclusive) {
            int childLength = -1;
            if (level == 1) {
                int group = columnGroupIndex(at);
                if (group >= 0) childLength = columnGroupLanes[group * 2 + 1];
            } else {
                int child = ancestorSlot(level - 1, at);
                if (child >= 0) childLength = ancestorGroupLanes[child * 3 + 1];
            }
            if (childLength < 1 || childLength > exclusive - at) return false;
            at += childLength;
            count++;
        }
        return at == exclusive && count >= 2;
    }

    /** Packed [viewFirst,visibleViewColumns]; negative means invalid projection. */
    private long ancestorProjection(int index) {
        int b = index * 3, first = ancestorGroupLanes[b], end = first + ancestorGroupLanes[b + 1];
        int level = ancestorGroupLanes[b + 2], at = first, count = 0;
        int viewFirst = -1, viewEnd = -1;
        while (at < end) {
            int childLength, childView, childVisible;
            if (level == 1) {
                int child = columnGroupIndex(at);
                if (child < 0) return -1;
                childLength = columnGroupLanes[child * 2 + 1];
                childView = columnGroupViewStart(child);
                childVisible = columnGroupVisibleCount(child);
            } else {
                int child = ancestorSlot(level - 1, at);
                if (child < 0) return -1;
                childLength = ancestorGroupLanes[child * 3 + 1];
                long projected = ancestorProjection(child);
                if (projected < 0) return -1;
                childView = (int) (projected >>> 32);
                childVisible = (int) projected;
            }
            if (childLength < 1 || childLength > end - at || childView < 0 || childVisible < 1
                    || (viewEnd >= 0 && viewEnd != childView)) return -1;
            if (viewFirst < 0) viewFirst = childView;
            viewEnd = childView + childVisible;
            at += childLength;
            count++;
        }
        if (at != end || count < 2 || viewFirst < 0 || viewEnd > getColumnCount()) return -1;
        int frozen = getFrozenColumnCount();
        if (viewFirst < frozen && viewEnd > frozen) return -1;
        return ((long) viewFirst << 32) | (viewEnd - viewFirst);
    }

    /** Number of disjoint top-level column-header groups (not table columns). */
    public int getColumnGroupCount() { return columnGroupCount; }

    /** Number of column-group bands painted since construction. */
    public long getColumnGroupPainted() { return columnGroupPainted; }

    /**
     * Adds or replaces one first-level grouped header over a contiguous model
     * column interval. The original TableColumnModel, column reorder controls,
     * sorters and renderers remain authoritative. After a view-column reorder
     * the band is shown only while its original columns form a contiguous
     * ascending visible interval. Group bands crossing the frozen-column
     * border are suppressed until moved wholly into one plane.
     */
    public void setColumnGroup(int firstModelColumn, int columns, String title) {
        requireEdt();
        int modelColumns = getModel().getColumnCount();
        if (firstModelColumn < 0 || columns < 1 || columns > modelColumns - firstModelColumn) {
            throw new IllegalArgumentException("group outside model columns");
        }
        if (title == null) throw new NullPointerException("title");
        int position = 0;
        while (position < columnGroupCount && columnGroupLanes[position * 2] < firstModelColumn) position++;
        int existing = position < columnGroupCount && columnGroupLanes[position * 2] == firstModelColumn ? position : -1;
        for (int i = 0; i < columnGroupCount; i++) {
            if (i == existing) continue;
            int other = columnGroupLanes[i * 2], length = columnGroupLanes[i * 2 + 1];
            if ((long) firstModelColumn < (long) other + length
                    && (long) other < (long) firstModelColumn + columns) {
                throw new IllegalArgumentException("overlapping column groups");
            }
        }
        if (existing >= 0 && columnGroupSummary[existing] >= 0
                && columnGroupLanes[existing * 2 + 1] != columns) {
            throw new IllegalStateException("restore the configured group's columns before changing its extent");
        }
        if (existing < 0) {
            if (columnGroupCount >= 512) throw new IllegalArgumentException("too many column groups");
            if (columnGroupCount == columnGroupLanes.length / 2) {
                columnGroupLanes = Arrays.copyOf(columnGroupLanes, columnGroupLanes.length * 2);
                columnGroupLabels = Arrays.copyOf(columnGroupLabels, columnGroupLabels.length * 2);
                columnGroupSummary = Arrays.copyOf(columnGroupSummary, columnGroupSummary.length * 2);
                columnGroupCollapsed = Arrays.copyOf(columnGroupCollapsed, columnGroupCollapsed.length * 2);
                columnGroupOwners = Arrays.copyOf(columnGroupOwners, columnGroupOwners.length * 2);
            }
            System.arraycopy(columnGroupSummary, position, columnGroupSummary, position + 1,
                    columnGroupCount - position);
            System.arraycopy(columnGroupCollapsed, position, columnGroupCollapsed, position + 1,
                    columnGroupCount - position);
            System.arraycopy(columnGroupOwners, position, columnGroupOwners, position + 1,
                    columnGroupCount - position);
            columnGroupSummary[position] = -1;
            columnGroupCollapsed[position] = false;
            columnGroupOwners[position] = null;
            System.arraycopy(columnGroupLanes, position * 2, columnGroupLanes, (position + 1) * 2,
                    (columnGroupCount - position) * 2);
            System.arraycopy(columnGroupLabels, position, columnGroupLabels, position + 1,
                    columnGroupCount - position);
            columnGroupCount++;
            existing = position;
        }
        columnGroupLanes[existing * 2] = firstModelColumn;
        columnGroupLanes[existing * 2 + 1] = columns;
        columnGroupLabels[existing] = title;
        installHeader();
        refreshGroupedHeader();
    }

    public String getColumnGroupTitle(int firstModelColumn) {
        for (int i = 0; i < columnGroupCount; i++) {
            if (columnGroupLanes[i * 2] == firstModelColumn) return columnGroupLabels[i];
        }
        return null;
    }

    /**
     * Enables Nebula-style summary/detail for one existing header group.
     * The specified original TableColumn is visible when collapsed; all other
     * members are visible when expanded. No replacement table model or column
     * objects are created: the original column instances are retained and
     * reinserted in their model order on each user operation.
     *
     * <p>This first leaf supports one summary column, a contiguous group in
     * either the frozen or scrolling plane, and LTR mode. It fails closed on
     * externally hidden/reordered columns or an active cross-pane group.</p>
     */
    public void setColumnGroupSummaryColumn(int firstModelColumn, int summaryModelColumn) {
        requireEdt();
        int index = columnGroupIndex(firstModelColumn);
        if (index < 0) throw new IllegalArgumentException("unknown column group");
        if (columnGroupSummary[index] >= 0) {
            if (columnGroupSummary[index] == summaryModelColumn) return;
            throw new IllegalStateException("remove the current summary/detail mode before reconfiguring");
        }
        int base = index * 2, length = columnGroupLanes[base + 1];
        if (length < 2 || length > 512 || summaryModelColumn < firstModelColumn
                || summaryModelColumn >= firstModelColumn + length) {
            throw new IllegalArgumentException("summary column must belong to a nontrivial group");
        }
        if (!getComponentOrientation().isLeftToRight()) {
            throw new IllegalStateException("summary/detail projection for RTL is not qualified");
        }
        int start = columnGroupViewStart(index);
        if (start < 0) throw new IllegalStateException("group is not a contiguous single-plane view");
        // Editor veto must occur before we retain any hidden-column owners.
        if (isEditing() && getCellEditor() != null && !getCellEditor().stopCellEditing()) {
            throw new IllegalStateException("active editor vetoed column visibility change");
        }
        TableColumn[] existing = new TableColumn[length];
        for (int i = 0; i < length; i++) {
            int view = convertColumnIndexToView(firstModelColumn + i);
            if (view < 0 || view != start + i) {
                throw new IllegalStateException("all original group columns must currently be visible");
            }
            existing[i] = getColumnModel().getColumn(view);
            if (existing[i].getModelIndex() != firstModelColumn + i) {
                throw new IllegalStateException("caller modified the model-column identities");
            }
        }
        columnGroupOwners[index] = existing;
        // Original group is fully visible. Hide its summary column, showing
        // original detail columns in the expanded state.
        applyGroupColumns(index, start, length, false, summaryModelColumn);
        columnGroupSummary[index] = summaryModelColumn;
        columnGroupCollapsed[index] = false;
        refreshGroupedHeader();
    }

    /** Returns true when the configured group shows only its summary column. */
    public boolean isColumnGroupCollapsed(int firstModelColumn) {
        int index = columnGroupIndex(firstModelColumn);
        if (index < 0 || columnGroupSummary[index] < 0) {
            throw new IllegalArgumentException("column group has no summary/detail mode");
        }
        return columnGroupCollapsed[index];
    }

    /** Changes the native TableColumnModel view, keeping the same column objects. */
    public void setColumnGroupCollapsed(int firstModelColumn, boolean collapsed) {
        requireEdt();
        int index = columnGroupIndex(firstModelColumn);
        if (index < 0 || columnGroupSummary[index] < 0) {
            throw new IllegalArgumentException("column group has no summary/detail mode");
        }
        if (columnGroupCollapsed[index] == collapsed) return;
        int start = columnGroupViewStart(index);
        if (start < 0) throw new IllegalStateException("group columns are not contiguous or cross a frozen plane");
        int before = columnGroupVisibleCount(index);
        applyGroupColumns(index, start, before, collapsed, columnGroupSummary[index]);
        columnGroupCollapsed[index] = collapsed;
        refreshGroupedHeader();
        fireColumnGroupState(firstModelColumn, collapsed);
    }

    /** Disables detail/summary mode without discarding the grouped title. */
    public void clearColumnGroupSummary(int firstModelColumn) {
        requireEdt();
        int index = columnGroupIndex(firstModelColumn);
        if (index < 0) throw new IllegalArgumentException("unknown column group");
        if (columnGroupSummary[index] < 0) return;
        restoreColumnGroupOwners(index);
        columnGroupOwners[index] = null;
        columnGroupSummary[index] = -1;
        columnGroupCollapsed[index] = false;
        refreshGroupedHeader();
    }

    /** Native Swing typed event, emitted after the full column mutation. */
    @FunctionalInterface public interface ColumnGroupStateListener extends java.util.EventListener {
        void groupStateChanged(M3Grid grid, int firstModelColumn, boolean collapsed);
    }

    public void addColumnGroupStateListener(ColumnGroupStateListener listener) {
        listenerList.add(ColumnGroupStateListener.class,
                java.util.Objects.requireNonNull(listener, "listener"));
    }

    public void removeColumnGroupStateListener(ColumnGroupStateListener listener) {
        listenerList.remove(ColumnGroupStateListener.class, listener);
    }

    private void fireColumnGroupState(int firstModelColumn, boolean collapsed) {
        for (ColumnGroupStateListener listener : listenerList.getListeners(ColumnGroupStateListener.class)) {
            listener.groupStateChanged(this, firstModelColumn, collapsed);
        }
    }

    private int columnGroupIndex(int firstModelColumn) {
        for (int i = 0; i < columnGroupCount; i++) {
            if (columnGroupLanes[i * 2] == firstModelColumn) return i;
        }
        return -1;
    }

    /**
     * Restores all original TableColumn instances in their model order.
     * Called before removing group metadata or changing group configuration.
     */
    private void restoreColumnGroupOwners(int index) {
        TableColumn[] owners = columnGroupOwners[index];
        if (owners == null) throw new IllegalStateException("missing group native columns");
        // Restore remains possible even if the caller has interleaved/reordered
        // visible group members. Collapse/expand remains fail-closed until the
        // group is contiguous, but disabling it must not strand columns.
        int start = getColumnCount(), before = 0;
        for (TableColumn owner : owners) {
            int view = convertColumnIndexToView(owner.getModelIndex());
            if (view >= 0) { start = Math.min(start, view); before++; }
        }
        if (before == 0) start = getColumnCount();
        rewriteColumnGroup(index, start, before, owners, -1, true);
        columnGroupSummary[index] = -1;
        columnGroupCollapsed[index] = false;
    }

    private void applyGroupColumns(int index, int firstView, int before,
                                   boolean collapsed, int summary) {
        TableColumn[] owners = columnGroupOwners[index];
        if (owners == null) throw new IllegalStateException("missing captured columns");
        rewriteColumnGroup(index, firstView, before, owners, collapsed ? summary : -summary - 2, false);
    }

    /**
     * target = -1: show all, nonnegative: summary only,
     * target <= -2: all details except encoded summary model column.
     * Validates complete native ownership before the first model mutation.
     */
    private void rewriteColumnGroup(int index, int firstView, int before,
                                    TableColumn[] owners, int target, boolean restoring) {
        TableColumnModel columns = getColumnModel();
        int firstModel = columnGroupLanes[index * 2], size = owners.length;
        int maxView = -1;
        for (TableColumn column : owners) maxView = Math.max(maxView, convertColumnIndexToView(column.getModelIndex()));
        boolean withinFrozen = firstView < getFrozenColumnCount() && maxView < getFrozenColumnCount();
        int frozenBefore = frozenColumns;
        int selectedModel = getSelectedColumn() < 0 ? -1
                : convertColumnIndexToModel(getSelectedColumn());
        // Any active cell editor may be repositioned when native columns
        // shift. Commit or respect its veto before mutating any column owner;
        // never silently discard caller-edited data outside this group.
        if (isEditing() && getCellEditor() != null && !getCellEditor().stopCellEditing()) {
            throw new IllegalStateException("active editor vetoed column visibility change");
        }
        int observed = 0;
        for (int i = 0; i < size; i++) {
            TableColumn owner = owners[i];
            if (owner == null || owner.getModelIndex() != firstModel + i) {
                throw new IllegalStateException("native group column changed identity");
            }
            int view = convertColumnIndexToView(firstModel + i);
            if (view >= 0) {
                if ((!restoring && view != firstView + observed) || columns.getColumn(view) != owner) {
                    throw new IllegalStateException("group has externally replaced native columns");
                }
                observed++;
            }
        }
        if (observed != before) throw new IllegalStateException("group model changed externally");
        // Remove right-to-left, so all other native columns retain their order.
        for (int i = size - 1; i >= 0; i--) {
            int view = convertColumnIndexToView(firstModel + i);
            if (view >= 0) columns.removeColumn(owners[i]);
        }
        int inserted = 0;
        for (int i = 0; i < size; i++) {
            int model = firstModel + i;
            boolean desired = target == -1 || (target >= 0 ? model == target : model != -target - 2);
            if (!desired) continue;
            columns.addColumn(owners[i]);
            int from = columns.getColumnCount() - 1;
            columns.moveColumn(from, firstView + inserted);
            inserted++;
        }
        if (withinFrozen) frozenColumns = frozenBefore + inserted - before;
        // JTable selection is view-indexed; when the selected model column is
        // temporarily hidden, move to the visible summary/detail representative.
        if (selectedModel >= 0) {
            int nextColumn = selectedModel < 0 ? -1 : convertColumnIndexToView(selectedModel);
            if (nextColumn < 0) nextColumn = inserted > 0 ? firstView : -1;
            if (nextColumn >= 0 && nextColumn < getColumnCount()) {
                getColumnModel().getSelectionModel().setSelectionInterval(nextColumn, nextColumn);
            }
        }
        revalidate();
        repaint();
    }

    public void removeColumnGroup(int firstModelColumn) {
        requireEdt();
        for (int i = 0; i < columnGroupCount; i++) {
            if (columnGroupLanes[i * 2] != firstModelColumn) continue;
            // Removing group metadata must never abandon its caller's
            // original TableColumn objects inside an invisible array.
            if (columnGroupSummary[i] >= 0) restoreColumnGroupOwners(i);
            System.arraycopy(columnGroupSummary, i + 1, columnGroupSummary, i, columnGroupCount - i - 1);
            System.arraycopy(columnGroupCollapsed, i + 1, columnGroupCollapsed, i, columnGroupCount - i - 1);
            System.arraycopy(columnGroupOwners, i + 1, columnGroupOwners, i, columnGroupCount - i - 1);
            System.arraycopy(columnGroupLanes, (i + 1) * 2, columnGroupLanes, i * 2,
                    (columnGroupCount - i - 1) * 2);
            System.arraycopy(columnGroupLabels, i + 1, columnGroupLabels, i,
                    columnGroupCount - i - 1);
            columnGroupCount--;
            Arrays.fill(columnGroupLanes, columnGroupCount * 2, columnGroupCount * 2 + 2, 0);
            columnGroupLabels[columnGroupCount] = null;
            columnGroupSummary[columnGroupCount] = -1;
            columnGroupCollapsed[columnGroupCount] = false;
            columnGroupOwners[columnGroupCount] = null;
            if (columnGroupCount == 0 && getFrozenColumnCount() == 0) uninstallHeader();
            refreshGroupedHeader();
            return;
        }
    }

    public void clearColumnGroups() {
        requireEdt();
        if (columnGroupCount == 0) return;
        // Reconstitute every original column before discarding metadata.
        for (int i = columnGroupCount - 1; i >= 0; i--) {
            if (columnGroupSummary[i] >= 0) restoreColumnGroupOwners(i);
        }
        columnGroupCount = 0;
        Arrays.fill(columnGroupLanes, 0);
        Arrays.fill(columnGroupLabels, null);
        Arrays.fill(columnGroupSummary, -1);
        Arrays.fill(columnGroupCollapsed, false);
        Arrays.fill(columnGroupOwners, null);
        if (getFrozenColumnCount() == 0) uninstallHeader();
        refreshGroupedHeader();
    }

    private void refreshGroupedHeader() {
        JTableHeader header = getTableHeader();
        if (header != null) {
            header.revalidate();
            header.repaint();
            if (header.getParent() != null) header.getParent().revalidate();
        }
    }

    /** Number of original group columns currently projected as real Swing view columns. */
    private int columnGroupVisibleCount(int index) {
        int length = columnGroupLanes[index * 2 + 1];
        return columnGroupSummary[index] < 0 ? length : columnGroupCollapsed[index] ? 1 : length - 1;
    }

    /** A valid model-column group projected into current ascending view columns. */
    private int columnGroupViewStart(int index) {
        int b = index * 2, first = columnGroupLanes[b], length = columnGroupLanes[b + 1];
        if (first < 0 || length < 1 || length > getModel().getColumnCount() - first) return -1;
        int viewStart = -1, count = 0, summary = columnGroupSummary[index];
        for (int i = 0; i < length; i++) {
            int model = first + i;
            boolean wanted = summary < 0 || (columnGroupCollapsed[index] ? model == summary : model != summary);
            int view = convertColumnIndexToView(model);
            if (wanted) {
                if (view < 0 || (viewStart >= 0 && view != viewStart + count)) return -1;
                if (viewStart < 0) viewStart = view;
                count++;
            } else if (view >= 0) {
                return -1;
            }
        }
        if (count != columnGroupVisibleCount(index) || viewStart < 0) return -1;
        int frozen = getFrozenColumnCount();
        if (viewStart < frozen && viewStart + count > frozen) return -1;
        return viewStart;
    }

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

    /** Column-group model ordinals cannot silently move to a replaced TableModel. */
    @Override public void setModel(TableModel model) {
        boolean hadGroups = columnGroupLanes != null && columnGroupCount > 0;
        if (hadGroups) {
            // Before switching the source model, return every caller column
            // object (including hidden summary/detail columns) to the native
            // model. A fresh model then takes over its normal schema contract.
            clearColumnGroups();
        }
        super.setModel(model);
        if (hadGroups) {
            if (getFrozenColumnCount() == 0) uninstallHeader();
            refreshGroupedHeader();
        }
    }

    /** Explicit replacement of column owners invalidates configured groups. */
    @Override public void setColumnModel(TableColumnModel model) {
        if (columnGroupLanes != null && columnGroupCount > 0) {
            columnGroupCount = 0;
            Arrays.fill(columnGroupLanes, 0);
            Arrays.fill(columnGroupLabels, null);
            Arrays.fill(columnGroupSummary, -1);
            Arrays.fill(columnGroupCollapsed, false);
            Arrays.fill(columnGroupOwners, null);
        }
        super.setColumnModel(model);
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
        // Serialized pre-column-group versions have no header-group lanes.
        // Reinitialize those optional arrays without disturbing old Grid state.
        if (columnGroupCount < 0 || columnGroupCount > 512) {
            throw new InvalidObjectException("invalid column group count");
        }
        if (columnGroupSummary == null) {
            columnGroupSummary = new int[Math.max(8, columnGroupCount)];
            Arrays.fill(columnGroupSummary, -1);
        }
        if (columnGroupCollapsed == null) columnGroupCollapsed = new boolean[Math.max(8, columnGroupCount)];
        if (columnGroupOwners == null) columnGroupOwners = new TableColumn[Math.max(8, columnGroupCount)][];
        if (columnGroupSummary.length < columnGroupCount
                || columnGroupCollapsed.length < columnGroupCount
                || columnGroupOwners.length < columnGroupCount) {
            throw new InvalidObjectException("invalid group visibility lanes");
        }
        if (columnGroupCount == 0) {
            if (columnGroupLanes == null) columnGroupLanes = new int[16];
            if (columnGroupLabels == null) columnGroupLabels = new String[8];
        } else {
            if (columnGroupLanes == null || columnGroupLabels == null
                    || (long) columnGroupCount * 2 > columnGroupLanes.length
                    || columnGroupCount > columnGroupLabels.length) {
                throw new InvalidObjectException("invalid column group lanes");
            }
            int previousEnd = 0, columns = getModel().getColumnCount();
            for (int i = 0; i < columnGroupCount; i++) {
                int first = columnGroupLanes[i * 2], width = columnGroupLanes[i * 2 + 1];
                if (first < previousEnd || width < 1 || width > columns - first
                        || columnGroupLabels[i] == null) {
                    throw new InvalidObjectException("invalid column group interval/title");
                }
                previousEnd = first + width;
                int summary = columnGroupSummary[i];
                if (summary == -1) continue;
                if (width < 2 || summary < first || summary >= first + width
                        || columnGroupOwners[i] == null || columnGroupOwners[i].length != width) {
                    throw new InvalidObjectException("invalid serialized summary/detail owner");
                }
                TableColumn[] owners = columnGroupOwners[i];
                for (int member = 0; member < width; member++) {
                    int modelColumn = first + member;
                    TableColumn owner = owners[member];
                    if (owner == null || owner.getModelIndex() != modelColumn) {
                        throw new InvalidObjectException("invalid serialized native TableColumn");
                    }
                    int view = convertColumnIndexToView(modelColumn);
                    boolean visible = columnGroupCollapsed[i] ? modelColumn == summary : modelColumn != summary;
                    if (visible != (view >= 0)) {
                        throw new InvalidObjectException("serialized column group visibility mismatch");
                    }
                    if (visible && getColumnModel().getColumn(view) != owner) {
                        throw new InvalidObjectException("serialized model/column identity mismatch");
                    }
                }
            }
            if (pinnedHeader == null) installHeader();
        }
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
        boolean changedColumns = columnGroupLanes != null && columnGroupCount > 0
                && (event == null || event.getFirstRow() == TableModelEvent.HEADER_ROW);
        if (changedColumns) {
            columnGroupCount = 0;
            Arrays.fill(columnGroupLanes, 0);
            Arrays.fill(columnGroupLabels, null);
            if (columnGroupSummary != null) Arrays.fill(columnGroupSummary, -1);
            if (columnGroupCollapsed != null) Arrays.fill(columnGroupCollapsed, false);
            if (columnGroupOwners != null) Arrays.fill(columnGroupOwners, null);
        }
        if (groupLanes != null && (event == null || event.getFirstRow() == TableModelEvent.HEADER_ROW
                || event.getType() == TableModelEvent.INSERT || event.getType() == TableModelEvent.DELETE)) {
            clearGroupsOnStructure();
        }
        super.tableChanged(event);
        if (changedColumns) {
            if (getFrozenColumnCount() == 0) uninstallHeader();
            refreshGroupedHeader();
        }
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

        PinnedHeader(TableColumnModel columns) {
            super(columns);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseReleased(MouseEvent event) {
                    if (event.getButton() != MouseEvent.BUTTON1 || event.getY() >= COLUMN_GROUP_BAND_HEIGHT) return;
                    int group = disclosureGroupAt(event.getPoint());
                    if (group >= 0) setColumnGroupCollapsed(columnGroupLanes[group * 2], !columnGroupCollapsed[group]);
                }
            });
        }

        private int disclosureGroupAt(Point point) {
            Rectangle visible = getVisibleRect();
            if (point.x < visible.x || point.x >= visible.x + visible.width || point.y < 0) return -1;
            for (int i = 0; i < columnGroupCount; i++) {
                if (columnGroupSummary[i] < 0) continue;
                int start = columnGroupViewStart(i);
                if (start < 0) continue;
                int left = getHeaderRect(start).x;
                if (pinned() && start < getFrozenColumnCount()) {
                    left = visible.x;
                    for (int c = 0; c < start; c++) left += getColumnModel().getColumn(c).getWidth();
                }
                int right = left;
                int count = columnGroupVisibleCount(i);
                for (int c = 0; c < count; c++) right += getColumnModel().getColumn(start + c).getWidth();
                if (point.x >= left && point.x < Math.min(right, left + 20)) return i;
            }
            return -1;
        }

        @Override public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            if (columnGroupCount > 0) {
                size = new Dimension(size);
                size.height = (int) Math.min(Integer.MAX_VALUE, (long) size.height + COLUMN_GROUP_BAND_HEIGHT);
            }
            return size;
        }

        @Override public int columnAtPoint(Point point) {
            // The title band is a group label, not a leaf sortable column.
            if (columnGroupCount > 0 && point.y < COLUMN_GROUP_BAND_HEIGHT) return -1;
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
            if (columnGroupCount > 0) {
                paintGroupedHeader(g);
                return;
            }
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
        /**
         * Two painted header tiers, one native JTableHeader and original
         * renderer instances: group titles above regular view column labels.
         * Frozen and scrolling bands are clipped as independent planes.
         */
        private void paintGroupedHeader(Graphics graphics) {
            Rectangle visible = getVisibleRect();
            if (visible.isEmpty() || getColumnCount() == 0) return;
            if (painter == null) painter = new CellRendererPane();
            int band = Math.min(COLUMN_GROUP_BAND_HEIGHT, getHeight());
            int leafHeight = Math.max(0, getHeight() - band);
            Graphics layer = graphics.create();
            try {
                layer.clipRect(visible.x, visible.y, visible.width, visible.height);
                layer.setColor(getBackground());
                layer.fillRect(visible.x, visible.y, visible.width, visible.height);
                if (leafHeight > 0) {
                    // Header leaf renderers: only paint the horizontal viewport.
                    // Frozen columns are then restamped over the scrolling band.
                    int from = getColumnModel().getColumnIndexAtX(visible.x);
                    int to = getColumnModel().getColumnIndexAtX(visible.x + visible.width - 1);
                    if (from >= 0) {
                        if (to < from) to = getColumnCount() - 1;
                        for (int column = from; column <= to; column++) {
                            Rectangle rect = getHeaderRect(column);
                            if (rect.width <= 0 || rect.x + rect.width <= visible.x
                                    || rect.x >= visible.x + visible.width) continue;
                            drawHeaderCell(layer, column, rect.x, band, rect.width, leafHeight);
                        }
                    }
                    if (pinned()) {
                        Graphics frozen = layer.create();
                        try {
                            frozen.clipRect(visible.x, band, Math.min(visible.width, getFrozenWidth()), leafHeight);
                            int x = visible.x;
                            for (int column = 0; column < getFrozenColumnCount(); column++) {
                                int width = getColumnModel().getColumn(column).getWidth();
                                drawHeaderCell(frozen, column, x, band, width, leafHeight);
                                x += width;
                            }
                        } finally { frozen.dispose(); }
                    }
                }
                Graphics body = layer.create();
                try {
                    int frozenWidth = pinned() ? Math.min(visible.width, getFrozenWidth()) : 0;
                    body.clipRect(visible.x + frozenWidth, 0, visible.width - frozenWidth, band);
                    for (int i = 0; i < columnGroupCount; i++) {
                        int view = columnGroupViewStart(i);
                        if (view < 0 || view < getFrozenColumnCount()) continue;
                        paintGroupBand(body, i, view, false, visible);
                    }
                } finally { body.dispose(); }
                if (pinned()) {
                    Graphics frozen = layer.create();
                    try {
                        frozen.clipRect(visible.x, 0, Math.min(visible.width, getFrozenWidth()), band);
                        for (int i = 0; i < columnGroupCount; i++) {
                            int view = columnGroupViewStart(i);
                            if (view < 0 || view >= getFrozenColumnCount()) continue;
                            paintGroupBand(frozen, i, view, true, visible);
                        }
                    } finally { frozen.dispose(); }
                }
            } finally { layer.dispose(); }
        }

        private void drawHeaderCell(Graphics g, int column, int x, int y, int width, int height) {
            if (width <= 0 || height <= 0) return;
            TableColumn actual = getColumnModel().getColumn(column);
            TableCellRenderer renderer = actual.getHeaderRenderer() == null ? getDefaultRenderer() : actual.getHeaderRenderer();
            Component component = renderer.getTableCellRendererComponent(M3Grid.this,
                    actual.getHeaderValue(), false, false, -1, column);
            painter.paintComponent(g, component, this, x, y, width, height, true);
        }

        private void paintGroupBand(Graphics g, int index, int viewStart, boolean frozen, Rectangle visible) {
            int modelColumns = columnGroupVisibleCount(index);
            int x = frozen ? visible.x : getHeaderRect(viewStart).x;
            if (frozen) {
                for (int c = 0; c < viewStart; c++) x += getColumnModel().getColumn(c).getWidth();
            }
            long sum = 0;
            for (int i = 0; i < modelColumns; i++) sum += getColumnModel().getColumn(viewStart + i).getWidth();
            int width = (int) Math.min(Integer.MAX_VALUE, sum);
            if (width <= 0 || x >= visible.x + visible.width || (long) x + width <= visible.x) return;
            int band = Math.min(COLUMN_GROUP_BAND_HEIGHT, getHeight());
            g.setColor(getBackground());
            g.fillRect(x, 0, width, band);
            g.setColor(getForeground());
            g.drawRect(x, 0, Math.max(0, width - 1), Math.max(0, band - 1));
            String title = columnGroupLabels[index];
            if (title != null && !title.isEmpty()) {
                java.awt.Shape clip = g.getClip();
                try {
                    int inset = columnGroupSummary[index] >= 0 ? 21 : 5;
                    if (columnGroupSummary[index] >= 0) {
                        int mid = band / 2;
                        if (columnGroupCollapsed[index]) {
                            g.drawLine(x + 6, mid - 5, x + 13, mid);
                            g.drawLine(x + 13, mid, x + 6, mid + 5);
                        } else {
                            g.drawLine(x + 5, mid - 4, x + 13, mid - 4);
                            g.drawLine(x + 5, mid - 3, x + 9, mid + 3);
                            g.drawLine(x + 13, mid - 3, x + 9, mid + 3);
                        }
                    }
                    g.clipRect(x + inset, 0, Math.max(0, width - inset - 4), band);
                    java.awt.FontMetrics fm = g.getFontMetrics();
                    g.drawString(title, x + inset,
                            (band - fm.getHeight()) / 2 + fm.getAscent());
                } finally { g.setClip(clip); }
            }
            columnGroupPainted++;
        }
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Swing EDT required");
    }
}
