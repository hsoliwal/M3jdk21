/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Persist sparse model ordinals without persisting dense per-row projections. */
public final class M3GridGroupsSerializationSmoke {
    private M3GridGroupsSerializationSmoke() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridGroupsSerializationSmoke::exercise);
        System.out.println("M3_GRID_GROUPS_SERIALIZATION_PASS");
    }

    private static void exercise() {
        try {
            DefaultTableModel model = new DefaultTableModel(100, 3);
            M3Grid original = new M3Grid(model);
            original.addRowGroup(10, 40, "alpha");
            original.addRowGroup(60, 95, "beta");
            original.setRowGroupCollapsed(10, true);
            original.setRowGroupCollapsed(60, true);
            check(original.getRowCount() == 37, "initial count");
            byte[] bytes;
            try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                 ObjectOutputStream stream = new ObjectOutputStream(output)) {
                stream.writeObject(original);
                stream.flush();
                bytes = output.toByteArray();
            }
            M3Grid restored;
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                restored = (M3Grid) input.readObject();
            }
            check(restored.getModel().getRowCount() == 100, "source rows retained");
            check(restored.getRowGroupCount() == 2 && restored.getRowCount() == 37,
                    "sparse view restored atomically with persistent collapse state");
            check(restored.getRowSorter() != null, "transient sparse sorter reconstructed");
            check(restored.isRowGroupCollapsed(10) && restored.isRowGroupCollapsed(60),
                    "collapse flags retained");
            check("alpha".equals(restored.getRowGroupTitle(10)) && "beta".equals(restored.getRowGroupTitle(60)),
                    "labels preserved");
            check(restored.convertRowIndexToView(11) == -1, "hidden child after deserialize");
            check(restored.convertRowIndexToModel(11) == 40, "visible model inverse after deserialize");
            restored.setRowGroupCollapsed(10, false);
            check(restored.getRowCount() == 66, "reexpanded group after restore");
            restored.clearRowGroups();
            check(restored.getRowCount() == 100 && restored.getRowGroupCount() == 0
                    && restored.getRowSorter() == null, "restored group owner detaches cleanly");
            // An empty new Grid should round-trip with null sorter and empty lanes.
            M3Grid empty = new M3Grid(new DefaultTableModel(4, 2));
            try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                 ObjectOutputStream stream = new ObjectOutputStream(output)) {
                stream.writeObject(empty);
                stream.flush();
                try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
                    M3Grid copy = (M3Grid) input.readObject();
                    copy.addRowGroup(0, 3, "new");
                    copy.setRowGroupCollapsed(0, true);
                    check(copy.getRowCount() == 2, "empty-read group arrays are initialized");
                }
            }
        } catch (Exception failure) {
            throw new AssertionError("group serialization", failure);
        }
    }

    private static void check(boolean condition, String explanation) {
        if (!condition) throw new AssertionError(explanation);
    }
}
