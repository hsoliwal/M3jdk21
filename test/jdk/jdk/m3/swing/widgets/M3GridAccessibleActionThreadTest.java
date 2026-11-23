/*
 * @test
 * @summary Accessible grouped-header action returns actual EDT commit outcome, including editor veto
 * @modules java.desktop/jdk.m3.swing.widgets
 * @run main/othervm M3GridAccessibleActionThreadTest
 */
/* SPDX-License-Identifier: Apache-2.0 */
import java.util.concurrent.atomic.AtomicReference;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import jdk.m3.swing.widgets.M3Grid;

public final class M3GridAccessibleActionThreadTest {
    private static final AtomicReference<M3Grid> TABLE = new AtomicReference<>();
    private static final AtomicReference<AccessibleAction> ACTION = new AtomicReference<>();
    private static void check(boolean value, String detail) {
        if (!value) throw new AssertionError(detail);
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(() -> {
            M3Grid grid = new M3Grid(new DefaultTableModel(4, 4));
            grid.setColumnGroup(0, 2, "Summary or details");
            grid.setColumnGroupSummaryColumn(0, 0);
            AccessibleContext root = grid.getTableHeader().getAccessibleContext();
            AccessibleAction action = root.getAccessibleChild(grid.getColumnCount())
                    .getAccessibleContext().getAccessibleAction();
            check(action != null, "summary group exposes native accessibility action");
            TABLE.set(grid);
            ACTION.set(action);
        });
        // A non-EDT caller must observe the *completed* mutation, not a queued task.
        check(ACTION.get().doAccessibleAction(0), "successful off-EDT collapse reports true");
        check(TABLE.get().isColumnGroupCollapsed(0), "collapse completed before returning true");
        check(ACTION.get().doAccessibleAction(0), "successful off-EDT expand reports true");
        check(!TABLE.get().isColumnGroupCollapsed(0), "expansion completed before returning true");
        SwingUtilities.invokeAndWait(() -> {
            M3Grid grid = TABLE.get();
            DefaultCellEditor veto = new DefaultCellEditor(new JTextField()) {
                private static final long serialVersionUID = 1L;
                @Override public boolean stopCellEditing() { return false; }
            };
            grid.getColumnModel().getColumn(0).setCellEditor(veto);
            check(grid.editCellAt(0, 0), "original JTable editor is active");
        });
        // The previous implementation wrongly returned true here, then the EDT
        // later rejected collapse. A screen reader would announce an unmade change.
        check(!ACTION.get().doAccessibleAction(0), "editor veto is reported synchronously as false");
        check(!TABLE.get().isColumnGroupCollapsed(0), "veto leaves original column state unchanged");
        SwingUtilities.invokeAndWait(() -> {
            M3Grid grid = TABLE.get();
            check(grid.isEditing(), "editor is not silently discarded by a veto");
            grid.removeEditor();
            grid.getTableHeader().setEnabled(false);
        });
        check(!ACTION.get().doAccessibleAction(0), "disabled header refuses an off-EDT action");
        SwingUtilities.invokeAndWait(() -> TABLE.get().getTableHeader().setEnabled(true));
        check(!ACTION.get().doAccessibleAction(1), "invalid action index fails without scheduling");
        SwingUtilities.invokeAndWait(() -> TABLE.get().clearColumnGroups());
        check(!ACTION.get().doAccessibleAction(0), "stale accessible group refuses action after removal");
        System.out.println("M3GridAccessibleActionThreadTest PASS");
    }
}
