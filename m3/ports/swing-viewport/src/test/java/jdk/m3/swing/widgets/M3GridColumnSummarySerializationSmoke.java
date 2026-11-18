/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/** Native collapsed summary/detail columns survive serialization with object identity. */
public final class M3GridColumnSummarySerializationSmoke {
    private M3GridColumnSummarySerializationSmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridColumnSummarySerializationSmoke::exercise);
        System.out.println("M3_GRID_COLUMN_SUMMARY_SERIAL_PASS");
    }
    private static void exercise() {
        try {
            for (int frozen : new int[]{0, 3}) {
                M3Grid grid = new M3Grid(new DefaultTableModel(8, 7));
                grid.setColumnGroup(0,3,"Alpha");
                grid.setColumnGroup(3,3,"Beta");
                grid.setFrozenColumnCount(frozen);
                grid.setColumnGroupSummaryColumn(0,0);
                grid.setColumnGroupSummaryColumn(3,4);
                grid.setColumnGroupCollapsed(0,true);
                grid.setColumnGroupCollapsed(3,true);
                check(grid.getColumnCount()==3,"two collapsed groups leave two summaries and last column");
                M3Grid restored = copy(grid);
                check(restored.getColumnCount()==3 && restored.getColumnGroupCount()==2,"serialized group counts");
                check(restored.isColumnGroupCollapsed(0)&&restored.isColumnGroupCollapsed(3),"collapsed flags survive");
                check(restored.getFrozenColumnCount()==(frozen==0?0:1),"frozen view membership survives");
                check(restored.convertColumnIndexToModel(0)==0 && restored.convertColumnIndexToModel(1)==4
                        && restored.convertColumnIndexToModel(2)==6,"summary model identities survive");
                TableColumn summary=restored.getColumnModel().getColumn(0);
                restored.setColumnGroupCollapsed(0,false);
                check(restored.getColumnModel().getColumn(0).getModelIndex()==1,"native detail restored after serialized collapse");
                restored.setColumnGroupCollapsed(0,true);
                check(restored.getColumnModel().getColumn(0)==summary,"same summary reference reinserted");
                restored.clearColumnGroups();
                check(restored.getColumnCount()==7 && restored.getColumnGroupCount()==0,"all columns restored");
                for(int col=0;col<7;col++) check(restored.getColumnModel().getColumn(col).getModelIndex()==col,
                        "source-column identities returned in original model order");
                check(restored.getTableHeader()!=null,"native header restored");
            }
            M3Grid empty = copy(new M3Grid(new DefaultTableModel(3, 5)));
            empty.setColumnGroup(0,2,"new");
            empty.setColumnGroupSummaryColumn(0,0);
            empty.setColumnGroupCollapsed(0,true);
            check(empty.getColumnCount()==4,"empty group mode after read is usable");
        } catch(Exception error) { throw new AssertionError("summary/detail serialization", error); }
    }
    private static M3Grid copy(M3Grid input) throws Exception {
        byte[] bytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(output)) {
            out.writeObject(input);
            out.flush();
            bytes=output.toByteArray();
        }
        try(ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (M3Grid) in.readObject();
        }
    }
    private static void check(boolean ok,String why) { if(!ok)throw new AssertionError(why); }
}
