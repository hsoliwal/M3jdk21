/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.EventObject;
import javax.swing.CellRendererPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

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

    /** Convert pinned pixels to JTable's existing view-row indices. */
    @Override public int rowAtPoint(Point point) {
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

    /** Pin the native cell editor without modifying JTable's stock geometry queries. */
    @Override public boolean editCellAt(int row, int column, EventObject event) {
        boolean editing = super.editCellAt(row, column, event);
        if (editing) positionPinnedEditor();
        return editing;
    }

    @Override public void doLayout() {
        super.doLayout();
        positionPinnedEditor();
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
