/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.EventObject;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

/** Stock Swing contracts, real pinned-screen pixels, sorting and bounded viewport materialization. */
public final class M3GridContractSmoke {
    private M3GridContractSmoke() { }
    private static void check(boolean test, String note) { if (!test) throw new AssertionError(note); }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridContractSmoke::exercise);
        System.out.println("M3_NATIVE_GRID_CONTRACT_PASS");
    }
    private static void exercise() {
        DefaultTableModel model = new DefaultTableModel(new Object[][]{
                {"R0", "B0", "G0", "Y0"}, {"R1", "B1", "G1", "Y1"},
                {"R2", "B2", "G2", "Y2"}}, new Object[]{"R", "B", "G", "Y"});
        M3Grid grid = new M3Grid(model);
        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        grid.setRowHeight(24);
        for(int col=0;col<4;col++) { grid.getColumnModel().getColumn(col).setPreferredWidth(90); grid.getColumnModel().getColumn(col).setWidth(90); }
        grid.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID=1L;
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focused, int row, int viewColumn) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, viewColumn);
                setBackground(swatch(table.convertColumnIndexToModel(viewColumn)));
                return this;
            }
        });
        JTableHeader original=grid.getTableHeader();
        original.setDefaultRenderer((table, value, selected, focused, row, viewColumn) -> {
            JLabel label = new JLabel(String.valueOf(value));
            label.setOpaque(true);
            label.setBackground(swatch(grid.convertColumnIndexToModel(viewColumn)));
            return label;
        });
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setSize(180,120);
        scroll.doLayout();
        grid.setSize(360,100);
        scroll.getViewport().setViewSize(new Dimension(360,100));
        scroll.getViewport().setExtentSize(new Dimension(180,72));
        scroll.getViewport().setViewPosition(new Point(120,0));
        check(grid.getTableHeader()==original,"stock header identity before opt-in");
        int stockPixel=paint(scroll.getViewport(),180,72).getRGB(10,9) & 0xffffff;
        check(stockPixel != (Color.RED.getRGB() & 0xffffff),"off mode uses stock scrolled body");
        grid.setFrozenColumnCount(1);
        scroll.doLayout();
        grid.getTableHeader().setSize(360,24);
        scroll.getColumnHeader().setViewSize(new Dimension(360,24));
        scroll.getColumnHeader().setExtentSize(new Dimension(180,24));
        scroll.getColumnHeader().setViewPosition(new Point(120,0));
        check(grid.getFrozenColumnCount()==1 && grid.getFrozenWidth()==90,"one frozen view column");
        check(scroll.getColumnHeader()!=null && scroll.getColumnHeader().getView()==grid.getTableHeader(),
                "pinned header belongs to original JScrollPane");
        check(grid.columnAtPoint(new Point(125,8))==0,"body overlay hit maps to frozen column");
        check(grid.getTableHeader().columnAtPoint(new Point(125,8))==0,"header hit maps to frozen column");
        check((paint(scroll.getViewport(),180,72).getRGB(10,10) & 0xffffff)==(Color.RED.getRGB()&0xffffff),
                "fixed red body pixel");
        check((paint(scroll.getViewport(),180,72).getRGB(100,10) & 0xffffff)==(Color.GREEN.getRGB()&0xffffff),
                "body still scrolls independently");
        check((paint(scroll.getColumnHeader(),180,20).getRGB(10,9) & 0xffffff)==(Color.RED.getRGB()&0xffffff),
                "fixed header draws matching column");
        long beforeBodyDamage = grid.getFrozenPaintedCells();
        Rectangle visible = grid.getVisibleRect();
        renderClipped(grid, visible.x + grid.getFrozenWidth() + 2, visible.y, 20, 20);
        check(grid.getFrozenPaintedCells() == beforeBodyDamage,
                "body-only damage cannot repaint the separate frozen plane");
        renderClipped(grid, visible.x + 2, visible.y, 20, 20);
        check(grid.getFrozenPaintedCells() - beforeBodyDamage == 1,
                "one dirty frozen row repaints one frozen renderer");
        // JTable's editor must be owned by the one JTable and placed at the frozen on-screen x.
        check(grid.editCellAt(0,0,new EventObject(grid)),"native editor activates");
        check(grid.getEditorComponent()!=null && grid.getEditorComponent().getX()>=120,
                "native cell editor follows pinned viewport");
        grid.removeEditor();
        // BasicTableUI mouse events require a headful Toolkit; hit geometry is
        // checked above, and actual click/drag behavior remains a native gate.
        grid.getColumnModel().moveColumn(3,0);
        check(grid.convertColumnIndexToModel(0)==3,"view order remains authoritative");
        check((paint(scroll.getViewport(),180,72).getRGB(10,10) & 0xffffff)==(Color.YELLOW.getRGB()&0xffffff),
                "frozen view plane moves with model column reorder");
        check((paint(scroll.getColumnHeader(),180,20).getRGB(10,9) & 0xffffff)==(Color.YELLOW.getRGB()&0xffffff),
                "frozen header moves with reorder");
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        grid.setRowSorter(sorter);
        sorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> e) {
                return !"R1".equals(e.getStringValue(0));
            }
        });
        sorter.toggleSortOrder(0);
        check(grid.getRowCount()==2,"filtering remains JTable-owned");
        check(grid.convertRowIndexToModel(0)>=0,"sorter model mapping available");
        grid.setFrozenColumnCount(0);
        check(grid.getTableHeader()==original,"opt-out restores original JTableHeader instance");
        check(scroll.getColumnHeader().getView()==original,"opt-out restores JScrollPane header view");
        check(grid.getFrozenWidth()==0,"frozen plane off");
        try { grid.setFrozenColumnCount(5); throw new AssertionError("out of range accepted"); }
        catch(IllegalArgumentException expected) { /* expected */ }
        // A million-row datasource is read only for visible and overscan cells.
        AtomicInteger reads = new AtomicInteger();
        AbstractTableModel massive = new AbstractTableModel() {
            private static final long serialVersionUID=1L;
            @Override public int getRowCount(){ return 1_000_000; }
            @Override public int getColumnCount(){ return 12; }
            @Override public Object getValueAt(int row,int col){reads.incrementAndGet(); return row+":"+col;}
        };
        M3Grid huge = new M3Grid(massive);
        huge.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        huge.setRowHeight(18);
        for(int i=0;i<12;i++){huge.getColumnModel().getColumn(i).setPreferredWidth(80);huge.getColumnModel().getColumn(i).setWidth(80);}
        JScrollPane hugeScroll=new JScrollPane(huge);
        hugeScroll.setSize(260,100);
        hugeScroll.doLayout();
        huge.setSize(960,18_000_000);
        hugeScroll.getViewport().setViewSize(new Dimension(960,18_000_000));
        hugeScroll.getViewport().setExtentSize(new Dimension(260,90));
        hugeScroll.getViewport().setViewPosition(new Point(260,9_000_000));
        huge.setFrozenColumnCount(2);
        check(huge.getFrozenWidth()==160,"two frozen columns");
        BufferedImage largeImage=paint(hugeScroll.getViewport(),260,90);
        check(largeImage.getWidth()==260,"headless large paint");
        check(reads.get()>0 && reads.get()<140,"paint reads only viewport rows; got="+reads.get());
        check(huge.getRowCount()==1_000_000,"million logical rows retained");
    }
    private static void renderClipped(JComponent component,int x,int y,int width,int height) {
        BufferedImage img = new BufferedImage(Math.max(component.getWidth(),x+width),Math.max(component.getHeight(),y+height),BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = img.createGraphics();
        try { graphics.setClip(x,y,width,height);component.paint(graphics); } finally { graphics.dispose(); }
    }
    private static BufferedImage paint(JComponent c,int w,int h){
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();try{c.paint(g);}finally{g.dispose();}return image;
    }
    private static Color swatch(int c){
        return switch(c){case 0->Color.RED;case 1->Color.BLUE;case 2->Color.GREEN;default->Color.YELLOW;};
    }
}
