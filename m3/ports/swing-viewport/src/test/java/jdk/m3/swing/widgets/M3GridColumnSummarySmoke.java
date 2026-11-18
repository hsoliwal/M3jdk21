/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;

/** Native TableColumn identity, disclosure policy, sorting, frozen planes and editor veto. */
public final class M3GridColumnSummarySmoke {
    private M3GridColumnSummarySmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridColumnSummarySmoke::exercise);
        System.out.println("M3_GRID_COLUMN_SUMMARY_PASS");
    }
    static void exercise() {
        DefaultTableModel model = new DefaultTableModel(12, 6);
        M3Grid grid = new M3Grid(model);
        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumn[] original = new TableColumn[6];
        for(int i=0;i<6;i++) {
            original[i]=grid.getColumnModel().getColumn(i);
            original[i].setWidth(51+i*11);
            original[i].setPreferredWidth(51+i*11);
            original[i].setHeaderValue("Original-"+i);
        }
        JTable table=grid;
        grid.setColumnGroup(0,3,"Alpha");
        grid.setColumnGroup(3,3,"Beta");
        check(grid.getColumnCount()==6 && grid.getColumnGroupCount()==2,"title-only keeps all columns");
        AtomicInteger events = new AtomicInteger();
        int[] ids = new int[3];
        grid.addColumnGroupStateListener((source,start,collapsed)->{
            ids[0]=start; ids[1]=collapsed?1:0; ids[2]=source.getColumnCount();
            events.incrementAndGet();
        });
        grid.setColumnGroupSummaryColumn(0,0);
        columns(grid,1,2,3,4,5);
        check(grid.getColumnGroupCount()==2 && !grid.isColumnGroupCollapsed(0),"expanded means details only");
        grid.setColumnGroupSummaryColumn(3,4);
        columns(grid,1,2,3,5);
        grid.setColumnSelectionAllowed(true);
        grid.getColumnModel().getSelectionModel().setSelectionInterval(1,1);
        // Collapsing an expanded group restores the original summary column
        // and hides original details, preserving TableColumn identity.
        grid.setColumnGroupCollapsed(0,true);
        columns(grid,0,3,5);
        check(grid.getColumnModel().getColumn(0)==original[0],"summary is original TableColumn");
        check(events.get()==1 && ids[0]==0 && ids[1]==1 && ids[2]==3,"event observes committed native view");
        check(grid.getColumnModel().getSelectionModel().getMinSelectionIndex()==0,"covered selection redirected to visible summary");
        grid.setColumnGroupCollapsed(3,true);
        columns(grid,0,4);
        check(grid.getColumnModel().getColumn(1)==original[4],"second group's summary is native object");
        grid.setColumnGroupCollapsed(3,false);
        columns(grid,0,3,5);
        check(grid.getColumnModel().getColumn(1)==original[3] && grid.getColumnModel().getColumn(2)==original[5],
                "second group's original details restored");
        grid.setColumnGroupCollapsed(0,false);
        columns(grid,1,2,3,5);
        check(grid.getColumnModel().getColumn(0)==original[1] && grid.getColumnModel().getColumn(1)==original[2],
                "first group's original detail column references retained");
        check(original[0].getWidth()==51 && original[1].getWidth()==62 && original[5].getWidth()==106,
                "native column widths preserved");
        check(events.get()==4,"only real collapse/expand events fired");
        // Existing external JTable sorter is still authoritative for row order.
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.toggleSortOrder(0);
        check(table.getRowSorter()==sorter && table.getRowCount()==12,"row sorter owner remains original");
        // A normal JTable cell editor can veto a group visibility change.
        TableCellEditor veto = new DefaultCellEditor(new JTextField()) {
            private static final long serialVersionUID=1L;
            @Override public boolean stopCellEditing() {return false;}
        };
        grid.getColumnModel().getColumn(0).setCellEditor(veto);
        check(grid.editCellAt(0,0),"editor activated on original detail cell");
        reject(()->grid.setColumnGroupCollapsed(0,true),IllegalStateException.class);
        check(grid.isEditing() && !grid.isColumnGroupCollapsed(0),"active editor veto preserved");
        grid.removeEditor();
        grid.setColumnGroupCollapsed(0,true);
        columns(grid,0,3,5);
        check(events.get()==5,"veto didn't send a collapse event");
        // Removing the configured group restores every original column exactly.
        grid.removeColumnGroup(0);
        columns(grid,0,1,2,3,5);
        check(grid.getColumnModel().getColumn(0)==original[0]
                && grid.getColumnModel().getColumn(1)==original[1]
                && grid.getColumnModel().getColumn(2)==original[2],"removing group returns all native TableColumns");
        grid.clearColumnGroups();
        columns(grid,0,1,2,3,4,5);
        for(int i=0;i<6;i++)check(grid.getColumnModel().getColumn(i)==original[i],"full opt-out restores native column " +i);
        check(grid.getColumnGroupCount()==0,"title and summary mode fully removed");
        // Frozen rows/columns and summary detail have correct native pane membership.
        grid.setColumnGroup(0,2,"Pinned group");
        grid.setFrozenColumnCount(2);
        grid.setColumnGroupSummaryColumn(0,0);
        columns(grid,1,2,3,4,5);
        check(grid.getFrozenColumnCount()==1,"hiding summary can't freeze an unrelated column");
        grid.setColumnGroupCollapsed(0,true);
        columns(grid,0,2,3,4,5);
        check(grid.getFrozenColumnCount()==1,"summary alone stays pinned");
        grid.clearColumnGroupSummary(0);
        columns(grid,0,1,2,3,4,5);
        check(grid.getFrozenColumnCount()==2,"full group native column restoration preserves frozen membership");
        grid.setFrozenColumnCount(0);
        grid.clearColumnGroups();
        // Model-column reorder that splits a group rejects configuration.
        grid.setColumnGroup(0,3,"Discontiguous");
        grid.getColumnModel().moveColumn(1,4);
        reject(()->grid.setColumnGroupSummaryColumn(0,0),IllegalStateException.class);
        check(grid.getColumnCount()==6 && grid.getColumnGroupCount()==1,"reordered group rejected without state change");
        grid.clearColumnGroups();
        // A caller can reorder visible group details. Collapse refuses to
        // misproject them, but disabling grouping must always give their
        // original TableColumn owners back, without abandoning hidden refs.
        grid.getColumnModel().moveColumn(4,1); // undo previous outside move
        grid.setColumnGroup(0,3,"Reordered detail");
        grid.setColumnGroupSummaryColumn(0,0);
        grid.getColumnModel().moveColumn(0,3);
        reject(()->grid.setColumnGroupCollapsed(0,true),IllegalStateException.class);
        grid.clearColumnGroupSummary(0);
        check(grid.getColumnCount()==6,"restore after external interleaving recovered all native columns");
        grid.clearColumnGroups();
        // A structural model change invalidates all summary/detail handles.
        grid.setColumnModel(new javax.swing.table.DefaultTableColumnModel());
        check(grid.getColumnGroupCount()==0,"external owner replacement discards stale group metadata");
        // Replacing the TableModel must not strand any hidden original objects.
        M3Grid replaced = new M3Grid(new DefaultTableModel(3, 4));
        replaced.setColumnGroup(0,3,"replace model");
        replaced.setColumnGroupSummaryColumn(0,0);
        replaced.setColumnGroupCollapsed(0,true);
        replaced.setModel(new DefaultTableModel(2, 5));
        check(replaced.getColumnGroupCount()==0 && replaced.getColumnCount()==5,
                "model replacement recreates the native five-column view");
        M3Grid structure = new M3Grid(new DefaultTableModel(4, 6));
        structure.setColumnGroup(0,3,"structure");
        structure.setColumnGroupSummaryColumn(0,0);
        structure.setColumnGroupCollapsed(0,true);
        ((DefaultTableModel)structure.getModel()).setColumnCount(5);
        check(structure.getColumnGroupCount()==0 && structure.getColumnCount()==5,
                "schema change restores the new column model without stale hidden owners");
        M3Grid vetoInitial = new M3Grid(new DefaultTableModel(3, 3));
        vetoInitial.setColumnGroup(0, 2, "veto before activation");
        vetoInitial.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JTextField()) {
            private static final long serialVersionUID=1L;
            @Override public boolean stopCellEditing() {return false;}
        });
        check(vetoInitial.editCellAt(0, 0),"initial editor activated");
        reject(()->vetoInitial.setColumnGroupSummaryColumn(0, 0), IllegalStateException.class);
        check(vetoInitial.getColumnCount()==3 && vetoInitial.getColumnGroupCount()==1,
                "veto before activation has no hidden column state");
        vetoInitial.removeEditor();
        vetoInitial.setColumnGroupSummaryColumn(0,0);
        vetoInitial.clearColumnGroups();
        check(vetoInitial.getColumnCount()==3,"veto recovery allows complete group restoration");
        M3Grid boundary = new M3Grid(new DefaultTableModel(3, 5));
        boundary.setColumnGroup(0,3,"cross-plane");
        boundary.setFrozenColumnCount(1);
        reject(()->boundary.setColumnGroupSummaryColumn(0,0), IllegalStateException.class);
        check(boundary.getColumnCount()==5,"frozen boundary rejection leaves native model untouched");
    }
    private static void columns(M3Grid grid,int... models) {
        int[] actual = new int[grid.getColumnCount()];
        for(int i=0;i<actual.length;i++) actual[i]=grid.convertColumnIndexToModel(i);
        if(!Arrays.equals(actual,models)) throw new AssertionError("columns="+Arrays.toString(actual)+" expected="+Arrays.toString(models));
    }
    private static void check(boolean state,String label) {if(!state)throw new AssertionError(label);}
    private static void reject(Runnable task,Class<? extends Throwable> type) {
        try {task.run();}catch(Throwable t){if(type.isInstance(t))return;throw t;}
        throw new AssertionError("expected "+type.getSimpleName());
    }
}
