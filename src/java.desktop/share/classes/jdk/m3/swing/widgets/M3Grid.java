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
        if (!pinned()) return;
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
        if (!pinned() || !isEditing() || getEditingColumn() >= getFrozenColumnCount()) return;
        Component editor = getEditorComponent();
        if (editor == null) return;
        Rectangle cell = super.getCellRect(getEditingRow(), getEditingColumn(), false);
        int x = getVisibleRect().x;
        for (int i = 0; i < getEditingColumn(); i++) x += getColumnModel().getColumn(i).getWidth();
        editor.setBounds(x, cell.y, cell.width, cell.height);
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
