/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Stock JTable + sparse group rows without per-model-row grouping objects. */
public final class M3GridGroupsSmoke {
    private M3GridGroupsSmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridGroupsSmoke::exercise);
        System.out.println("M3_NATIVE_GRID_GROUPS_PASS");
    }
    private static void exercise() {
        DefaultTableModel model = new DefaultTableModel(12, 4);
        for (int row = 0; row < 12; row++) for (int column = 0; column < 4; column++) {
            model.setValueAt("row:" + row + "/" + column, row, column);
        }
        M3Grid grid = new M3Grid(model);
        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        grid.setRowHeight(24);
        for (int col = 0; col < 4; col++) {
            grid.getColumnModel().getColumn(col).setWidth(90);
            grid.getColumnModel().getColumn(col).setPreferredWidth(90);
        }
        grid.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                setOpaque(true);
                setBackground(column == 0 ? Color.YELLOW : Color.WHITE);
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setSize(210,160);
        scroll.doLayout();
        grid.setSize(360,288);
        scroll.getViewport().setViewSize(new Dimension(360,288));
        scroll.getViewport().setExtentSize(new Dimension(210,120));
        check(grid.getRowSorter()==null && grid.getRowGroupCount()==0 && grid.getRowCount()==12,
                "grouping is opt-in and preserves stock default");
        grid.addRowGroup(1,5,"North");
        grid.addRowGroup(6,10,"South");
        RowSorter<?> sparse = grid.getRowSorter();
        check(sparse != null && grid.getModel()==model,"single original model, sparse sorter only");
        check(grid.getRowGroupCount()==2,"two sparse group headers");
        check("North".equals(grid.getRowGroupTitle(1)) && "South".equals(grid.getRowGroupTitle(6)),
                "caller-owned group labels");
        check(grid.groupHeaderAtViewRow(1)==1 && grid.groupHeaderAtViewRow(6)==6,
                "group headings use original view rows");
        reject(()->grid.addRowGroup(2,4,"overlap"),IllegalArgumentException.class);
        reject(()->grid.addRowGroup(1,4,"duplicate"),IllegalArgumentException.class);
        reject(()->grid.addRowGroup(-1,2,"bad"),IllegalArgumentException.class);
        reject(()->grid.addRowGroup(10,13,"overflow"),IllegalArgumentException.class);
        check(grid.getRowGroupCount()==2 && grid.getRowCount()==12,"rejected definitions are atomic");
        check(grid.convertRowIndexToModel(11)==11 && grid.convertRowIndexToView(11)==11,
                "expanded mapping is identity");
        grid.setRowGroupCollapsed(1,true);
        check(grid.isRowGroupCollapsed(1) && grid.getRowCount()==9,"collapse hides three child rows");
        check(grid.convertRowIndexToView(2)==-1 && grid.convertRowIndexToView(4)==-1,
                "collapsed child not addressable");
        check(grid.convertRowIndexToView(5)==2 && grid.convertRowIndexToModel(2)==5,
                "visible ordinal skips collapsed children");
        check(grid.groupHeaderAtViewRow(3)==6,"second header shifts to first group's compact position");
        grid.setRowGroupCollapsed(6,true);
        check(grid.getRowCount()==6,"both groups collapse, keep two headers");
        check(Arrays.equals(new int[] {
                grid.convertRowIndexToModel(0),grid.convertRowIndexToModel(1),
                grid.convertRowIndexToModel(2),grid.convertRowIndexToModel(3),
                grid.convertRowIndexToModel(4),grid.convertRowIndexToModel(5)},
                new int[] {0,1,5,6,10,11}),"sparse two-range binary projection");
        check(grid.convertRowIndexToView(9)==-1 && grid.convertRowIndexToView(10)==4,
                "reverse mapping excludes second group's children");
        grid.setRowGroupCollapsed(1,false);
        check(grid.getRowCount()==9 && grid.convertRowIndexToView(6)==6,
                "independent expansion restores only one group");
        grid.setRowGroupCollapsed(6,false);
        check(grid.getRowCount()==12,"full expansion returns all original rows");
        // A heading is painted by the new overlay, not a per-row wrapper.
        paint(scroll.getViewport(),210,120);
        check(grid.getGroupHeaderPainted()>0,"visible header painted");
        grid.setRowGroupCollapsed(1,true);
        grid.setFrozenColumnCount(1);
        grid.setPinnedRowCounts(1,1);
        // A visible merged cell must still resolve through original model indices.
        grid.setCellSpan(5,1,1,2);
        int[] span = new int[4];
        check(grid.cellSpanAt(2,2,span,0) && span[0]==2 && span[1]==1,
                "active merges survive group collapse and view mapping");
        grid.setPinnedRowCounts(0,0);
        grid.setFrozenColumnCount(0);
        grid.clearCellSpans();
        // Opt-out removes the sparse sorter completely, preserving API identity.
        grid.removeRowGroup(1);
        check(grid.getRowGroupCount()==1 && grid.getRowCount()==12,"removing collapsed group restores children");
        grid.removeRowGroup(6);
        check(grid.getRowGroupCount()==0 && grid.getRowSorter()==null && grid.getRowCount()==12,
                "removing last group restores stock null sorter");
        TableRowSorter<DefaultTableModel> external = new TableRowSorter<>(model);
        grid.setRowSorter(external);
        reject(()->grid.addRowGroup(1,5,"must-not-steal-sorter"),IllegalStateException.class);
        check(grid.getRowSorter()==external && grid.getRowGroupCount()==0,
                "external sorter retains its exact identity and selection contract");
        grid.setRowSorter(null);
        grid.addRowGroup(1,5,"new-group");
        grid.setRowGroupCollapsed(1,true);
        grid.setRowSorter(external);
        check(grid.getRowSorter()==external && grid.getRowGroupCount()==0 && grid.getRowCount()==12,
                "explicit sorter replacement discards groups and restores model rows");
        grid.setRowSorter(null);
        grid.addRowGroup(1,5,"row-shift");
        grid.setRowGroupCollapsed(1,true);
        model.insertRow(0,new Object[] {"insert","x","y","z"});
        check(grid.getRowGroupCount()==0 && grid.getRowSorter()==null && grid.getRowCount()==13,
                "structural insert invalidates model-ordinal groups before forwarding");
        // Million-row groups: must remain sparse and avoid TableRowSorter per-row allocations.
        AtomicInteger reads = new AtomicInteger();
        AbstractTableModel million = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;
            @Override public int getRowCount() { return 1_000_000; }
            @Override public int getColumnCount() { return 12; }
            @Override public Object getValueAt(int row,int column) { reads.incrementAndGet();return row+":"+column; }
        };
        M3Grid huge = new M3Grid(million);
        huge.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        huge.setRowHeight(18);
        for (int col=0;col<12;col++) {huge.getColumnModel().getColumn(col).setWidth(80);huge.getColumnModel().getColumn(col).setPreferredWidth(80);}
        huge.addRowGroup(100,900_000,"Very large compact region");
        huge.addRowGroup(920_000,995_000,"Another compact region");
        huge.setRowGroupCollapsed(100,true);
        huge.setRowGroupCollapsed(920_000,true);
        check(huge.getRowCount()==25_102,"a million-row model compacts without dense mapping");
        check(huge.convertRowIndexToView(899_999)==-1,"last collapsed first group child hidden");
        check(huge.convertRowIndexToView(900_000)==101,"body resumes directly after first huge group");
        check(huge.convertRowIndexToModel(101)==900_000,"inverse mapping after first group");
        check(huge.convertRowIndexToView(920_000)==20_101,"second group header view ordinal");
        check(huge.convertRowIndexToModel(20_102)==995_000,"inverse mapping after second group");
        JScrollPane big = new JScrollPane(huge);
        big.setSize(300,120);
        big.doLayout();
        huge.setSize(960,451_836);
        big.getViewport().setViewSize(new Dimension(960,451_836));
        big.getViewport().setExtentSize(new Dimension(300,90));
        big.getViewport().setViewPosition(new Point(200,1800));
        huge.setFrozenColumnCount(1);
        huge.setPinnedRowCounts(1,1);
        paint(big.getViewport(),300,90);
        check(reads.get()>0 && reads.get()<300,"resident-only sample reads: "+reads.get());
        // Define thousands of tiny groups; projection must still use binary search,
        // no 1M Entry objects and no per-model-row group state.
        huge.clearRowGroups();
        for(int group=0;group<3_000;group++) {
            int start=group*10;
            huge.addRowGroup(start,start+6,"G"+group);
        }
        for(int group=0;group<3_000;group++)huge.setRowGroupCollapsed(group*10,true);
        check(huge.getRowCount()==985_000,"3K sparse collapsed ranges");
        check(huge.convertRowIndexToView(10)==5 && huge.convertRowIndexToModel(5)==10,
                "adjacent sparse ranges remain monotonic");
    }
    private static BufferedImage paint(javax.swing.JComponent view,int width,int height) {
        BufferedImage image = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics();
        try {view.paint(graphics);}finally{graphics.dispose();}
        return image;
    }
    private static void reject(Runnable runnable,Class<? extends Throwable> expected) {
        try {runnable.run();}catch(Throwable failure) {if(expected.isInstance(failure))return;throw failure;}
        throw new AssertionError("Expected "+expected.getSimpleName());
    }
    private static void check(boolean condition,String explanation) {
        if(!condition)throw new AssertionError(explanation);
    }
}
