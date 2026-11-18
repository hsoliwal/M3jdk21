/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Native Swing pinned-row behavior using one existing JTable and its original models. */
public final class M3GridPinnedRowsSmoke {
    private M3GridPinnedRowsSmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridPinnedRowsSmoke::exercise);
        System.out.println("M3_NATIVE_GRID_PINNED_ROWS_PASS");
    }
    private static void exercise() {
        DefaultTableModel model = new DefaultTableModel(8, 4);
        for (int row = 0; row < 8; row++) for (int col = 0; col < 4; col++) {
            model.setValueAt(row + ":" + col, row, col);
        }
        M3Grid grid = new M3Grid(model);
        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        grid.setRowHeight(20);
        for (int c = 0; c < 4; c++) {
            grid.getColumnModel().getColumn(c).setPreferredWidth(80);
            grid.getColumnModel().getColumn(c).setWidth(80);
        }
        grid.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                                        boolean focus, int row, int col) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                setBackground(rowColor(table, row, col));
                setOpaque(true);
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setSize(160, 120);
        scroll.doLayout();
        grid.setSize(320, 160);
        scroll.getViewport().setViewSize(new Dimension(320, 160));
        scroll.getViewport().setExtentSize(new Dimension(160, 100));
        scroll.getViewport().setViewPosition(new Point(100, 60));
        check(grid.getPinnedTopRowCount() == 0 && grid.getPinnedBottomRowCount() == 0,
                "default-off retains stock rows");
        grid.setFrozenColumnCount(1);
        int originalScrollMode = scroll.getViewport().getScrollMode();
        grid.setPinnedRowCounts(1, 1);
        check(scroll.getViewport().getScrollMode() == javax.swing.JViewport.SIMPLE_SCROLL_MODE,
                "fixed rows disable viewport pixel blitting");
        check(grid.getPinnedTopRowCount() == 1 && grid.getPinnedBottomRowCount() == 1, "configured planes");
        check(grid.rowAtPoint(new Point(120, 65)) == 0, "top view-row hit");
        check(grid.rowAtPoint(new Point(120, 155)) == 7, "bottom view-row hit");
        check(grid.rowAtPoint(new Point(120, 100)) == 5, "body row retains stock coordinates");
        BufferedImage image = paint(scroll.getViewport(), 160, 100);
        pixel(image, 10, 10, Color.MAGENTA, "top-left frozen cell");
        pixel(image, 100, 10, Color.RED, "top scrolled-body column");
        pixel(image, 10, 90, Color.ORANGE, "bottom-left frozen cell");
        pixel(image, 100, 90, Color.BLUE, "bottom scrolled-body column");
        pixel(image, 10, 45, Color.YELLOW, "center frozen column");
        pixel(image, 100, 45, Color.GREEN, "center scrolling body");
        long before = grid.getPinnedPaintedCells();
        check(before > 0, "pinned row paint instrumentation");
        // Changing the viewport vertical offset must not move fixed headers/footers.
        scroll.getViewport().setViewPosition(new Point(100, 40));
        image = paint(scroll.getViewport(), 160, 100);
        pixel(image, 10, 10, Color.MAGENTA, "top remains on scrolling");
        pixel(image, 100, 90, Color.BLUE, "bottom remains on scrolling");
        grid.setRowHeight(0, 30);
        grid.setRowHeight(7, 15);
        scroll.getViewport().setViewPosition(new Point(100, 60));
        image = paint(scroll.getViewport(), 160, 100);
        pixel(image, 100, 25, Color.RED, "variable height top row");
        pixel(image, 100, 92, Color.BLUE, "variable height bottom row");
        check(grid.rowAtPoint(new Point(130, 144)) != 7, "bottom boundary is exact");
        check(grid.rowAtPoint(new Point(130, 145)) == 7, "variable bottom hit");
        grid.setPinnedRowCounts(2, 2);
        check(grid.rowAtPoint(new Point(130, 95)) == 1, "second pinned top row");
        check(grid.rowAtPoint(new Point(130, 135)) == 6, "second pinned bottom row");
        grid.setPinnedRowCounts(1, 1);
        // Sorting/filtering owns the row order; pinned indices refer to the current view.
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        grid.setRowSorter(sorter);
        sorter.toggleSortOrder(0);
        sorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                return !"1:0".equals(entry.getStringValue(0));
            }
        });
        check(grid.getRowCount() == 7, "filter retained");
        check(grid.rowAtPoint(new Point(110, 62)) == 0, "top filtered view row");
        check(grid.rowAtPoint(new Point(110, 155)) == 6, "bottom filtered view row");
        check(grid.convertRowIndexToModel(6) >= 0, "model-index mapping preserved");
        grid.getSelectionModel().setSelectionInterval(6, 6);
        check(grid.convertRowIndexToModel(grid.getSelectedRow()) == grid.convertRowIndexToModel(6),
                "pinned selection is still JTable selection");
        grid.setPinnedRowCounts(0, 0);
        check(scroll.getViewport().getScrollMode() == originalScrollMode,
                "unpin restores exact previous viewport scrolling mode");
        check(grid.getPinnedPaintedCells() == before || grid.getPinnedPaintedCells() >= before,
                "no counter reset with opt-out");
        int rawExpected = -1, startY = 0;
        for (int row = 0; row < grid.getRowCount(); row++) {
            int h = grid.getRowHeight(row);
            if (62 >= startY && 62 < startY + h) { rawExpected = row; break; }
            startY += h;
        }
        check(grid.rowAtPoint(new Point(130, 62)) == rawExpected,
                "opt-out restores original rowAtPoint");
        reject(() -> grid.setPinnedRowCounts(-1, 0));
        reject(() -> grid.setPinnedRowCounts(0, 100));
        reject(() -> grid.setPinnedRowCounts(Integer.MAX_VALUE, Integer.MAX_VALUE));
        // An empty changed model cannot leave stale pinned row references.
        grid.setPinnedRowCounts(1, 1);
        model.setRowCount(0);
        check(grid.getPinnedTopRowCount() == 0 && grid.getPinnedBottomRowCount() == 0,
                "zero-count effective pinned ranges");
        paint(scroll.getViewport(),160,100);

        AtomicInteger reads = new AtomicInteger();
        AbstractTableModel million = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;
            @Override public int getRowCount() { return 1_000_000; }
            @Override public int getColumnCount() { return 12; }
            @Override public Object getValueAt(int row, int col) {
                reads.incrementAndGet();
                return row + ":" + col;
            }
        };
        M3Grid huge = new M3Grid(million);
        huge.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        huge.setRowHeight(18);
        for (int c = 0; c < 12; c++) {
            huge.getColumnModel().getColumn(c).setPreferredWidth(80);
            huge.getColumnModel().getColumn(c).setWidth(80);
        }
        JScrollPane hugeScroll = new JScrollPane(huge);
        hugeScroll.setSize(260, 130);
        hugeScroll.doLayout();
        huge.setSize(960, 18_000_000);
        hugeScroll.getViewport().setViewSize(new Dimension(960,18_000_000));
        hugeScroll.getViewport().setExtentSize(new Dimension(260,90));
        hugeScroll.getViewport().setViewPosition(new Point(260,9_000_000));
        huge.setFrozenColumnCount(2);
        huge.setPinnedRowCounts(1, 1);
        paint(hugeScroll.getViewport(),260,90);
        check(huge.rowAtPoint(new Point(300,9_000_005)) == 0, "million first row projected");
        check(huge.rowAtPoint(new Point(300,9_000_085)) == 999_999, "million last row projected");
        check(huge.getPinnedPaintedCells() > 0 && huge.getFrozenPaintedCells() > 0, "both plane counters");
        check(reads.get() > 0 && reads.get() < 250, "paint bounded by viewport: " + reads.get());
    }
    private static Color rowColor(JTable table, int row, int column) {
        if (row == 0) return column == 0 ? Color.MAGENTA : Color.RED;
        if (row == table.getRowCount() - 1) return column == 0 ? Color.ORANGE : Color.BLUE;
        return column == 0 ? Color.YELLOW : Color.GREEN;
    }
    private static BufferedImage paint(javax.swing.JComponent target, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { target.paint(g); } finally { g.dispose(); }
        return image;
    }
    private static void pixel(BufferedImage image,int x,int y,Color c,String reason) {
        int actual=image.getRGB(x,y) & 0xffffff;
        if(actual != (c.getRGB()&0xffffff)) throw new AssertionError(reason+": actual #"+Integer.toHexString(actual));
    }
    private static void check(boolean truth,String reason) {if(!truth) throw new AssertionError(reason);}
    private static void reject(Runnable task) {
        try {task.run();} catch(IllegalArgumentException expected) {return;}
        throw new AssertionError("expected IllegalArgumentException");
    }
}
