/*
 * @test
 * @summary Native JTableHeader grouped bands expose dynamic accessible nodes and keyboard actions
 * @modules java.desktop/jdk.m3.swing.widgets
 * @run main/othervm M3GridAccessibleGroupedHeaderTest
 */
/* SPDX-License-Identifier: Apache-2.0 */
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import jdk.m3.swing.widgets.M3Grid;

public final class M3GridAccessibleGroupedHeaderTest {
    private static void check(boolean result, String label) {
        if (!result) throw new AssertionError(label);
    }
    private static void name(Accessible child, String expected) {
        String actual = child == null ? null : child.getAccessibleContext().getAccessibleName();
        if (!expected.equals(actual)) throw new AssertionError("expected '" + expected + "', got " + actual);
        check(child.getAccessibleContext().getAccessibleRole() == AccessibleRole.COLUMN_HEADER,
                "group has native column-header role");
    }
    private static void action(JTableHeader header, String key) {
        Action act = header.getActionMap().get(key);
        check(act != null, "registered header action " + key);
        act.actionPerformed(new ActionEvent(header, ActionEvent.ACTION_PERFORMED, key));
    }
    private static M3Grid copy(M3Grid source) {
        try {
            byte[] encoded;
            try (var b = new ByteArrayOutputStream(); var out = new ObjectOutputStream(b)) {
                out.writeObject(source);
                out.flush();
                encoded = b.toByteArray();
            }
            try (var in = new ObjectInputStream(new ByteArrayInputStream(encoded))) {
                return (M3Grid)in.readObject();
            }
        } catch (Exception failure) { throw new AssertionError("serialized grouped header", failure); }
    }
    private static void exercise() {
        DefaultTableModel model = new DefaultTableModel(4, 8);
        M3Grid grid = new M3Grid(model);
        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < 8; i++) {
            grid.getColumnModel().getColumn(i).setWidth(85);
            grid.getColumnModel().getColumn(i).setPreferredWidth(85);
        }
        JTableHeader original = grid.getTableHeader();
        int leafCount = original.getAccessibleContext().getAccessibleChildrenCount();
        check(leafCount == 8, "stock JTable accessible column children");
        JScrollPane pane = new JScrollPane(grid);
        pane.setSize(300, 160);
        pane.doLayout();
        grid.setSize(680, 100);
        pane.getViewport().setViewSize(new Dimension(680, 100));
        pane.getViewport().setExtentSize(new Dimension(300, 90));
        for (int i = 0; i < 8; i += 2) grid.setColumnGroup(i, 2, "Base" + i);
        grid.setColumnGroupAncestor(1, 0, 4, "Left");
        grid.setColumnGroupAncestor(1, 4, 4, "Right");
        grid.setColumnGroupAncestor(2, 0, 8, "All");
        grid.setColumnGroupSummaryColumn(0, 0);
        JTableHeader header = grid.getTableHeader();
        int fullHeight = header.getPreferredSize().height;
        header.setSize(680, fullHeight);
        pane.getColumnHeader().setViewSize(new Dimension(680, fullHeight));
        pane.getColumnHeader().setExtentSize(new Dimension(300, fullHeight));
        check(header.isFocusable(), "grouped header becomes a native keyboard focus stop");
        AccessibleContext root = header.getAccessibleContext();
        int visibleLeafCount = grid.getColumnCount();
        check(root.getAccessibleChildrenCount() == visibleLeafCount + 7,
                "preserve native leaf accessibility and append seven group labels");
        name(root.getAccessibleChild(visibleLeafCount), "All");
        name(root.getAccessibleChild(visibleLeafCount + 1), "Left");
        name(root.getAccessibleChild(visibleLeafCount + 2), "Right");
        name(root.getAccessibleChild(visibleLeafCount + 3), "Base0");
        Accessible base = root.getAccessibleChild(visibleLeafCount + 3);
        AccessibleContext baseContext = base.getAccessibleContext();
        AccessibleAction expand = baseContext.getAccessibleAction();
        check(expand != null && expand.getAccessibleActionCount() == 1, "summary header exposes one action");
        check("Collapse".equals(expand.getAccessibleActionDescription(0)), "initial action is collapse");
        check(baseContext.getAccessibleStateSet().contains(AccessibleState.EXPANDED), "accessible expanded state");
        check(root.getAccessibleChild(visibleLeafCount + 1).getAccessibleContext().getAccessibleAction() == null,
                "ancestor is a label, not a fabricated collapse control");
        check(root.getAccessibleChild(visibleLeafCount + 4).getAccessibleContext().getAccessibleAction() == null,
                "title-only group is not a disclosure action");
        AccessibleComponent baseGeometry = baseContext.getAccessibleComponent();
        Rectangle box = baseGeometry.getBounds();
        check(box != null && box.y == 48 && box.height == 24, "base group bounds are physical middle tier");
        check(root.getAccessibleComponent().getAccessibleAt(new Point(10, 8)).getAccessibleContext().getAccessibleName().equals("All"),
                "hit testing identifies highest nested ancestor");
        check(root.getAccessibleComponent().getAccessibleAt(new Point(10, 30)).getAccessibleContext().getAccessibleName().equals("Left"),
                "hit testing identifies middle ancestor");
        check(root.getAccessibleComponent().getAccessibleAt(new Point(10, 55)).getAccessibleContext().getAccessibleName().equals("Base0"),
                "hit testing identifies native summary group");
        check(root.getAccessibleComponent().getAccessibleAt(new Point(10, 80)) != null, "native JTable leaf child remains available");
        check(header.getInputMap(JComponent.WHEN_FOCUSED).get(
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK)).equals("m3.header.next"),
                "keyboard traversal installed only on grouped header");
        List<PropertyChangeEvent> events = new ArrayList<>();
        root.addPropertyChangeListener(events::add);
        action(header, "m3.header.first");
        action(header, "m3.header.next");
        action(header, "m3.header.next");
        action(header, "m3.header.next");
        check(events.stream().anyMatch(e -> AccessibleContext.ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY.equals(e.getPropertyName())),
                "active accessible group descendent events are emitted on focus traversal");
        action(header, "m3.header.toggle");
        check(grid.isColumnGroupCollapsed(0) && grid.getColumnCount() == 7,
                "native SPACE/ENTER action uses original visibility owner");
        check(baseContext.getAccessibleStateSet().contains(AccessibleState.COLLAPSED),
                "existing accessible group proxy reflects new collapse flag");
        check("Expand".equals(expand.getAccessibleActionDescription(0)), "action description updated");
        check(expand.doAccessibleAction(0), "assistive technology action accepted");
        check(!grid.isColumnGroupCollapsed(0) && grid.getColumnCount() == 7,
                "accessibility action restores detail columns and original model");
        check(events.stream().anyMatch(e -> AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(e.getPropertyName())),
                "native accessible state-change event emitted");
        action(header, "m3.header.last");
        action(header, "m3.header.previous");
        action(header, "m3.header.first");
        check(!expand.doAccessibleAction(1), "unadvertised action index refused");
        M3Grid persisted = copy(grid);
        JTableHeader restoredHeader = persisted.getTableHeader();
        check(restoredHeader.isFocusable() && persisted.getColumnGroupCount() == 4,
                "serialized header reconnects keyboard focus without adding model columns");
        AccessibleContext restored = restoredHeader.getAccessibleContext();
        int restoredLeafCount = persisted.getColumnCount();
        check(restored.getAccessibleChildrenCount() == restoredLeafCount + 7,
                "serialized header reconstructs virtual accessible children");
        AccessibleAction restoredAction = restored.getAccessibleChild(restoredLeafCount + 3)
                .getAccessibleContext().getAccessibleAction();
        check(restoredAction != null && restoredAction.doAccessibleAction(0),
                "serialized summary group still has a working native accessibility action");
        check(persisted.isColumnGroupCollapsed(0), "serialized native column visibility updates");
        action(restoredHeader, "m3.header.first");
        action(restoredHeader, "m3.header.next");
        check(restored.getAccessibleChildrenCount() == restoredLeafCount + 7,
                "keyboard traversal preserves serial grouping topology");
        grid.getColumnModel().moveColumn(2, 6);
        check(root.getAccessibleChildrenCount() < grid.getColumnCount() + 7,
                "non-contiguous source groups have no misleading accessible title");
        grid.clearColumnGroupAncestors();
        grid.clearColumnGroups();
        check(grid.getTableHeader() == original && pane.getColumnHeader().getView() == original,
                "full opt-out restores exactly the original accessible JTableHeader");
        check(!original.isFocusable() || original == grid.getTableHeader(),
                "native header was not replaced by a persistent accessibility wrapper");
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridAccessibleGroupedHeaderTest::exercise);
        System.out.println("M3GridAccessibleGroupedHeaderTest PASS");
    }
}
