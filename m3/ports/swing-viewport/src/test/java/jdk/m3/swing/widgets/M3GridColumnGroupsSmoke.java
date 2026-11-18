/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.AbstractTableModel;
import java.util.concurrent.atomic.AtomicInteger;

/** First native two-tier column-header feature without a second JTable. */
public final class M3GridColumnGroupsSmoke {
    private M3GridColumnGroupsSmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridColumnGroupsSmoke::exercise);
        System.out.println("M3_NATIVE_GRID_COLUMN_GROUPS_PASS");
    }

    private static void exercise() {
        DefaultTableModel model = new DefaultTableModel(new Object[][] {
            {"A0","B0","C0","D0"}, {"A1","B1","C1","D1"}
        }, new Object[]{"A","B","C","D"});
        M3Grid grid = new M3Grid(model);
        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        grid.setRowHeight(24);
        for (int i = 0; i < 4; i++) {
            grid.getColumnModel().getColumn(i).setWidth(80);
            grid.getColumnModel().getColumn(i).setPreferredWidth(80);
        }
        JTableHeader original = grid.getTableHeader();
        original.setDefaultRenderer((table, value, selected, focused, row, column) -> {
            JLabel label=new JLabel(String.valueOf(value));
            label.setOpaque(true);
            label.setBackground(swatch(grid.convertColumnIndexToModel(column)));
            return label;
        });
        int originalHeight=original.getPreferredSize().height;
        JScrollPane scroll=new JScrollPane(grid);
        scroll.setSize(180,125);scroll.doLayout();
        grid.setSize(320,70);
        scroll.getViewport().setViewSize(new Dimension(320,70));
        scroll.getViewport().setExtentSize(new Dimension(180,70));
        check(grid.getColumnGroupCount()==0 && grid.getTableHeader()==original,"stock default header untouched");
        grid.setColumnGroup(0,2,"A + B");
        grid.setColumnGroup(2,2,"C + D");
        check(grid.getColumnGroupCount()==2,"two grouped header bands");
        check("A + B".equals(grid.getColumnGroupTitle(0)),"group title available");
        check(grid.getTableHeader()!=original,"opt-in nested JTableHeader owner");
        check(scroll.getColumnHeader()!=null && scroll.getColumnHeader().getView()==grid.getTableHeader(),
                "JScrollPane retains same table header as control");
        check(grid.getTableHeader().getPreferredSize().height==originalHeight+24,
                "one group band reserved above original preferred height");
        grid.getTableHeader().setSize(320,originalHeight+24);
        scroll.getColumnHeader().setViewSize(new Dimension(320,originalHeight+24));
        scroll.getColumnHeader().setExtentSize(new Dimension(180,originalHeight+24));
        BufferedImage initial=paint(scroll.getColumnHeader(),180,originalHeight+24);
        pixel(initial,10,originalHeight+7,Color.RED,"original leaf A painted in lower tier");
        pixel(initial,90,originalHeight+7,Color.BLUE,"original leaf B painted in lower tier");
        check(grid.getColumnGroupPainted()==2,"two grouping labels painted without new controls");
        check(grid.getTableHeader().columnAtPoint(new Point(30,6))==-1,
                "group title band does not select/sort a leaf column");
        check(grid.getTableHeader().columnAtPoint(new Point(30,29))==0,
                "ordinary leaf band retains native header hit model");
        reject(()->grid.setColumnGroup(1,2,"overlap"));
        reject(()->grid.setColumnGroup(3,2,"outside model"));
        reject(()->grid.setColumnGroup(-1,1,"negative"));
        check(grid.getColumnGroupCount()==2,"rejected groups preserve state");
        grid.getColumnModel().moveColumn(1,3); // [0,2,3,1]
        grid.getColumnModel().moveColumn(2,1); // [0,3,2,1], both groups discontiguous
        long before=grid.getColumnGroupPainted();
        paint(scroll.getColumnHeader(),180,originalHeight+24);
        check(grid.getColumnGroupPainted()==before,"reordered noncontiguous groups fail closed");
        grid.getColumnModel().moveColumn(1,2); // [0,2,3,1]
        grid.getColumnModel().moveColumn(3,1); // [0,1,2,3]
        paint(scroll.getColumnHeader(),180,originalHeight+24);
        check(grid.getColumnGroupPainted()==before+2,"contiguity restored displays original groups");
        grid.setFrozenColumnCount(1);
        scroll.getViewport().setViewPosition(new Point(60,0));
        scroll.getColumnHeader().setViewPosition(new Point(60,0));
        before=grid.getColumnGroupPainted();
        paint(scroll.getColumnHeader(),180,originalHeight+24);
        check(grid.getColumnGroupPainted()==before+1,"cross-frozen group disabled, body group remains");
        grid.setColumnGroup(0,1,"Pinned A");
        grid.setColumnGroup(1,1,"Body B");
        paint(scroll.getColumnHeader(),180,originalHeight+24);
        check(grid.getTableHeader().columnAtPoint(new Point(70,29))==0,
                "leaf hit inside viewport frozen band maps to first view column");
        check(grid.getColumnGroupCount()==3,"replacement preserves other groups");
        grid.setFrozenColumnCount(0);
        check(grid.getTableHeader()!=original,"groups keep enhanced header after disabling column pin");
        grid.clearColumnGroups();
        check(grid.getTableHeader()==original && grid.getColumnGroupCount()==0,
                "full opt-out restores exact original JTableHeader");
        check(scroll.getColumnHeader().getView()==original,"scroll header identity restored");
        grid.setColumnGroup(0,2,"before schema change");
        grid.setModel(new DefaultTableModel(3,4));
        check(grid.getColumnGroupCount()==0 && grid.getTableHeader()==original,
                "model replacement invalidates model-column identity");
        grid.setColumnGroup(0,2,"before column schema update");
        ((DefaultTableModel)grid.getModel()).setColumnCount(3);
        check(grid.getColumnGroupCount()==0 && grid.getTableHeader()==original,
                "model structure change invalidates stale column groups");
        // The grouped header must paint only the horizontal viewport, not all
        // columns of a wide real JTable; the source model stays unchanged.
        AtomicInteger leafRenders = new AtomicInteger();
        AbstractTableModel wideModel = new AbstractTableModel() {
            private static final long serialVersionUID=1L;
            @Override public int getRowCount(){return 2;}
            @Override public int getColumnCount(){return 5_000;}
            @Override public Object getValueAt(int row,int col){return row+":"+col;}
        };
        M3Grid wideGrid=new M3Grid(wideModel);
        wideGrid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        wideGrid.getTableHeader().setDefaultRenderer((table,value,selected,focused,row,col)->{
            leafRenders.incrementAndGet();
            JLabel label=new JLabel(String.valueOf(value));
            label.setOpaque(true);
            return label;
        });
        JScrollPane wideScroll=new JScrollPane(wideGrid);
        wideGrid.setColumnGroup(2_499,3,"Three visible columns");
        wideScroll.setSize(170,110);wideScroll.doLayout();
        int wholeWidth=5_000*75;
        int groupHeaderHeight=wideGrid.getTableHeader().getPreferredSize().height;
        wideGrid.setSize(wholeWidth,50);
        wideScroll.getViewport().setViewSize(new Dimension(wholeWidth,50));
        wideScroll.getViewport().setExtentSize(new Dimension(160,50));
        wideScroll.getColumnHeader().setViewSize(new Dimension(wholeWidth,groupHeaderHeight));
        wideScroll.getColumnHeader().setExtentSize(new Dimension(160,groupHeaderHeight));
        wideScroll.getViewport().setViewPosition(new Point(2_499*75,0));
        wideScroll.getColumnHeader().setViewPosition(new Point(2_499*75,0));
        paint(wideScroll.getColumnHeader(),160,groupHeaderHeight);
        check(leafRenders.get()>0 && leafRenders.get()<12,
                "only visible wide-table header cells render: "+leafRenders.get());
        check(wideGrid.getColumnGroupPainted()>0,"far column group visible without all-column rendering");
    }
    private static BufferedImage paint(javax.swing.JComponent target,int width,int height) {
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();try{target.paint(g);}finally{g.dispose();}return image;
    }
    private static Color swatch(int modelColumn) {
        return switch(modelColumn){case 0->Color.RED;case 1->Color.BLUE;case 2->Color.GREEN;default->Color.YELLOW;};
    }
    private static void pixel(BufferedImage image,int x,int y,Color c,String msg) {
        int rgb=image.getRGB(x,y)&0xffffff;
        if(rgb!=(c.getRGB()&0xffffff))throw new AssertionError(msg+" actual #"+Integer.toHexString(rgb));
    }
    private static void reject(Runnable action){try{action.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("invalid group accepted");}
    private static void check(boolean condition,String msg){if(!condition)throw new AssertionError(msg);}
}
