/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Column-group header state round-trips with original JTable-owned column model. */
public final class M3GridColumnGroupsSerializationSmoke {
    private M3GridColumnGroupsSerializationSmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridColumnGroupsSerializationSmoke::exercise);
        System.out.println("M3_NATIVE_GRID_COLUMN_GROUPS_SERIAL_PASS");
    }
    private static void exercise() {
        try {
            for (int frozen: new int[]{0,1}) {
                M3Grid source=new M3Grid(new DefaultTableModel(12,4));
                source.setFrozenColumnCount(frozen);
                source.setColumnGroup(0,2,"AB");
                source.setColumnGroup(2,2,"CD");
                M3Grid restored=roundTrip(source);
                check(restored.getColumnGroupCount()==2 && "AB".equals(restored.getColumnGroupTitle(0))
                        && "CD".equals(restored.getColumnGroupTitle(2)),"both title bands restored");
                check(restored.getTableHeader()!=null
                        && restored.getTableHeader().getPreferredSize().height>=24,
                        "header retains an extra title band");
                check(restored.getFrozenColumnCount()==frozen,"frozen column count retained");
                restored.removeColumnGroup(0);
                restored.clearColumnGroups();
                check(restored.getColumnGroupCount()==0,"clear after deserialize");
                restored.setColumnGroup(1,2,"BC");
                check(restored.getColumnGroupCount()==1,"new groups work after deserialize");
            }
            M3Grid empty=roundTrip(new M3Grid(new DefaultTableModel(7,4)));
            empty.setColumnGroup(2,2,"last");
            check(empty.getColumnGroupCount()==1,"empty schema initializes its lanes");
        } catch(Exception failure) {throw new AssertionError("column header serialization",failure);}
    }
    private static M3Grid roundTrip(M3Grid source) throws Exception {
        byte[] encoded;
        try(ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                ObjectOutputStream output=new ObjectOutputStream(bytes)) {
            output.writeObject(source); output.flush();encoded=bytes.toByteArray();
        }
        try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(encoded))) {
            return (M3Grid)in.readObject();
        }
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
