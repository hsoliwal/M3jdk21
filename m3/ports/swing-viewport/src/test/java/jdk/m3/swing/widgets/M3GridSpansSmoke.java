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

/** Native merged cells: original TableModel ownership and view-index hit mapping. */
public final class M3GridSpansSmoke {
    private M3GridSpansSmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridSpansSmoke::exercise);
        System.out.println("M3_NATIVE_GRID_SPANS_PASS");
    }
    private static void exercise() {
        DefaultTableModel model = new DefaultTableModel(8, 4);
        for (int r=0;r<8;r++) for(int c=0;c<4;c++) model.setValueAt("CELL"+r+":"+c,r,c);
        model.setValueAt("ANCHOR",1,1);
        M3Grid grid=new M3Grid(model);
        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        grid.setRowHeight(24);
        for(int i=0;i<4;i++) {
            grid.getColumnModel().getColumn(i).setPreferredWidth(80);
            grid.getColumnModel().getColumn(i).setWidth(80);
        }
        grid.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            private static final long serialVersionUID=1L;
            @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,
                    boolean focus,int row,int column){
                super.getTableCellRendererComponent(table,value,selected,focus,row,column);
                setBackground("ANCHOR".equals(value)?Color.MAGENTA:Color.GREEN);
                setOpaque(true);
                return this;
            }
        });
        grid.setSize(320,192);
        JScrollPane pane = new JScrollPane(grid);
        pane.setSize(320,220);
        pane.doLayout();
        pane.getViewport().setViewSize(new Dimension(320,192));
        pane.getViewport().setExtentSize(new Dimension(320,192));
        check(grid.getCellSpanCount()==0,"stock default unmerged");
        grid.setCellSpan(1,1,2,2);
        check(grid.getCellSpanCount()==1,"one span stored in four lanes");
        int[] window={-1,-1,-1,-1,-1,-1};
        check(grid.cellSpanAt(2,2,window,1),"covered cell in active span");
        check(window[1]==1 && window[2]==1 && window[3]==2 && window[4]==2,"view anchor lanes");
        check(!grid.cellSpanAt(0,0,window,0),"unmerged cell has no span");
        Rectangle r=grid.spannedCellRect(2,2);
        Rectangle baseline = grid.getCellRect(1,1,false);
        Rectangle tail = grid.getCellRect(2,2,false);
        check(r.equals(baseline.union(tail)) && r.width > baseline.width && r.height > baseline.height,
                "merged geometry spans actual native cell bounds");
        check(grid.rowAtPoint(new Point(190,60))==1 && grid.columnAtPoint(new Point(190,60))==1,
                "covered cell hit redirects to anchor");
        BufferedImage img=paint(pane.getViewport(),320,192);
        pixel(img,190,60,Color.MAGENTA,"merged cell hides subordinate original renderer");
        pixel(img,270,60,Color.GREEN,"uncovered cells remain original");
        check(grid.getSpanPaintedCells()>0,"span painter counters");
        check(grid.editCellAt(1,1),"native editor opens on anchor");
        Rectangle e=grid.getEditorComponent().getBounds();
        check(e.width==r.width && e.height==r.height,"native editor occupies merged rectangle");
        grid.removeEditor();
        reject(()->grid.setCellSpan(2,2,2,2));
        reject(()->grid.setCellSpan(6,3,3,2));
        reject(()->grid.setCellSpan(-1,1,2,2));
        grid.setCellSpan(1,1,2,2); // replacing an existing span does not duplicate it
        check(grid.getCellSpanCount()==1,"same model-anchor replacement");

        grid.getColumnModel().moveColumn(3,0);
        check(grid.convertColumnIndexToView(1)==2 && grid.convertColumnIndexToView(2)==3,
                "column reorder still contiguous by model identity");
        check(grid.cellSpanAt(2,3,window,0) && window[1]==2,"span follows model columns across reorder");
        grid.getColumnModel().moveColumn(3,1);
        check(!grid.cellSpanAt(2,1,window,0),"noncontiguous model columns fail closed");
        grid.getColumnModel().moveColumn(1,3); // [3,0,1,2]
        grid.getColumnModel().moveColumn(0,3); // [0,1,2,3]
        check(grid.convertColumnIndexToView(1)==1 && grid.convertColumnIndexToView(2)==2,
                "original contiguous columns restored");

        grid.clearCellSpans();
        check(grid.getCellSpanCount()==0,"clear leaves no secondary model state");
        grid.setCellSpan(1,1,2,2);
        TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model);
        grid.setRowSorter(sorter);
        sorter.toggleSortOrder(0); // ascending
        check(grid.cellSpanAt(2,2,window,0) && window[0]==1 && window[1]==1,
                "ascending sorter preserves model-identical merged rectangle");
        sorter.toggleSortOrder(0); // descending flips model-row adjacency
        check(!grid.cellSpanAt(6,1,window,0),"reverse order fails closed rather than painting wrong rows");
        sorter.toggleSortOrder(0); // ascending again
        sorter.setRowFilter(new RowFilter<>(){
            @Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> e){
                return e.getIdentifier()!=2;
            }
        });
        check(!grid.cellSpanAt(1,1,window,0),"filtered source row cannot produce stale span");
        grid.removeCellSpan(1,1);
        check(grid.getCellSpanCount()==0,"explicit remove");
        grid.setCellSpan(1,1,2,2);
        model.insertRow(0,new Object[]{"INSERTED","X","Y","Z"});
        check(grid.getCellSpanCount()==0,"structural insert clears unkeyed model row ordinals");

        AtomicInteger reads=new AtomicInteger();
        AbstractTableModel million=new AbstractTableModel(){
            private static final long serialVersionUID=1L;
            @Override public int getRowCount(){return 1_000_000;}
            @Override public int getColumnCount(){return 12;}
            @Override public Object getValueAt(int row,int col){reads.incrementAndGet();return row+":"+col;}
        };
        M3Grid huge=new M3Grid(million);
        huge.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        huge.setRowHeight(18);
        for(int c=0;c<12;c++){huge.getColumnModel().getColumn(c).setPreferredWidth(80);huge.getColumnModel().getColumn(c).setWidth(80);}
        JScrollPane wide=new JScrollPane(huge);
        wide.setSize(260,120);
        wide.doLayout();
        huge.setSize(960,18_000_000);
        wide.getViewport().setViewSize(new Dimension(960,18_000_000));
        wide.getViewport().setExtentSize(new Dimension(260,90));
        wide.getViewport().setViewPosition(new Point(80,9_000_000));
        huge.setFrozenColumnCount(1);
        huge.setPinnedRowCounts(1,1);
        huge.setCellSpan(500_001,1,2,2);
        huge.setCellSpan(500_000,3,300_000,1); // O(1) source-to-view identity without a RowSorter
        paint(wide.getViewport(),260,90);
        check(huge.getSpanPaintedCells()>=2,"far and wide spans rendered with original model values");
        check(reads.get()>0 && reads.get()<400,"million-row paint bounded by viewport, reads="+reads.get());
    }
    private static BufferedImage paint(javax.swing.JComponent target,int width,int height){
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();try{target.paint(g);}finally{g.dispose();}return image;
    }
    private static void pixel(BufferedImage image,int x,int y,Color expected,String reason){
        int found=image.getRGB(x,y)&0xffffff;
        if(found!=(expected.getRGB()&0xffffff))throw new AssertionError(reason+" actual #"+Integer.toHexString(found));
    }
    private static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
    private static void reject(Runnable action){try{action.run();}catch(IllegalArgumentException good){return;}throw new AssertionError("expected invalid span");}
}
