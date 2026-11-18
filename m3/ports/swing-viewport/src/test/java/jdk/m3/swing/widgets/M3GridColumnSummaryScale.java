/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/** The summary/detail switch must not instantiate an entry per logical model row. */
public final class M3GridColumnSummaryScale {
    private M3GridColumnSummaryScale() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridColumnSummaryScale::exercise);
        System.out.println("M3_GRID_COLUMN_SUMMARY_1M_5K_128M_PASS");
    }
    private static void exercise() {
        AtomicInteger values = new AtomicInteger();
        AbstractTableModel model = new AbstractTableModel() {
            private static final long serialVersionUID=1L;
            @Override public int getRowCount() {return 1_000_000;}
            @Override public int getColumnCount() {return 5_000;}
            @Override public Object getValueAt(int row,int column) {
                values.incrementAndGet();
                return row+":"+column;
            }
        };
        M3Grid grid=new M3Grid(model);
        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        grid.setRowHeight(18);
        for(int c=0;c<5_000;c++) {
            grid.getColumnModel().getColumn(c).setWidth(75);
            grid.getColumnModel().getColumn(c).setPreferredWidth(75);
        }
        JScrollPane pane=new JScrollPane(grid);
        pane.setSize(320,132);
        pane.doLayout();
        grid.setColumnGroup(2_499,3,"Very far group");
        grid.setColumnGroupSummaryColumn(2_499,2_499);
        check(grid.getColumnCount()==4_999,"expanded hides only summary");
        grid.setColumnGroupCollapsed(2_499,true);
        check(grid.getColumnCount()==4_998,"collapsed projects one summary instead of two details");
        grid.setColumnGroupCollapsed(2_499,false);
        check(grid.getColumnCount()==4_999,"expanded projects two original details");
        grid.setSize(4_999*75,18_000_000);
        pane.getViewport().setViewSize(new Dimension(4_999*75,18_000_000));
        pane.getViewport().setExtentSize(new Dimension(310,90));
        int headerHeight=grid.getTableHeader().getPreferredSize().height;
        pane.getColumnHeader().setViewSize(new Dimension(4_999*75,headerHeight));
        pane.getColumnHeader().setExtentSize(new Dimension(310,headerHeight));
        pane.getViewport().setViewPosition(new Point(2_499*75,9_000_000));
        pane.getColumnHeader().setViewPosition(new Point(2_499*75,0));
        BufferedImage image = new BufferedImage(310,90,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics();
        try {pane.getViewport().paint(graphics);}finally{graphics.dispose();}
        image=new BufferedImage(310,headerHeight,BufferedImage.TYPE_INT_ARGB);
        graphics=image.createGraphics();
        try {pane.getColumnHeader().paint(graphics);}finally{graphics.dispose();}
        check(grid.getColumnGroupPainted()>0,"far right grouped header was painted");
        check(values.get()>0 && values.get()<160,"paint only visible rows and columns: "+values.get());
        check(grid.getModel().getRowCount()==1_000_000,"original million-row model retained");
        grid.clearColumnGroups();
        check(grid.getColumnCount()==5_000,"releases all group references without losing columns");
    }
    private static void check(boolean condition,String reason) {if(!condition) throw new AssertionError(reason);}
}
