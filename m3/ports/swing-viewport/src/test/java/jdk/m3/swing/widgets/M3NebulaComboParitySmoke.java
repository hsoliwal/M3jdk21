/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.IllegalComponentStateException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/** Headless/EDT standalone regression, also executable by a source-sealed recipe. */
public final class M3NebulaComboParitySmoke {
    private M3NebulaComboParitySmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3NebulaComboParitySmoke::exercise);
        System.out.println("M3_NEBULA_COMBO_PARITY_PASS");
    }

    private static void exercise() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{{"C", 30}, {"A", 10}, {"B", 20}},
                new Object[]{"name", "value"});
        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.toggleSortOrder(0);
        M3TableCombo combo = new M3TableCombo(table);
        AtomicInteger events = new AtomicInteger();
        combo.addActionListener(e -> events.incrementAndGet());
        check(!combo.isPopupCreated() && combo.getTable() == table, "lazy and original table");
        check(combo.selectModelRow(0), "select source row C");
        check(combo.getSelectedModelRow() == 0 && combo.getText().equals("C"), "sorted view-to-model mapping");
        combo.setDisplayModelColumn(1);
        check(combo.getText().equals("30"), "display model column");
        combo.commitSelection();
        check(events.get() == 1, "one commit event");
        sorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                return !"C".equals(e.getStringValue(0));
            }
        });
        check(!combo.selectModelRow(0), "filtered source row refused");
        check(combo.selectModelRow(1) && combo.getText().equals("10"), "new visible selection");
        check(combo.selectModelRow(-1) && combo.getSelectedModelRow() == -1, "clear selection");
        check(combo.getText().isEmpty() && events.get() == 1, "programmatic selection silent");
        check(combo.selectModelRow(1), "second selection");
        model.setValueAt(11, 1, 1);
        check(combo.getText().equals("11"), "model change updates the selected text");
        DefaultTableModel replacement = new DefaultTableModel(
                new Object[][]{{"new", 55}}, new Object[]{"name", "value"});
        table.setRowSorter(null);
        table.setModel(replacement);
        check(combo.selectModelRow(0) && combo.getText().equals("55"), "replacement model keeps current text");
        replacement.setValueAt(56, 0, 1);
        check(combo.getText().equals("56"), "new model observed without stale subscription");
        table.setSelectionModel(new javax.swing.DefaultListSelectionModel());
        check(combo.getSelectedModelRow() == -1 && combo.getText().isEmpty(), "selection-model replacement");
        combo.setVisibleRowCount(3);
        check(combo.getVisibleRowCount() == 3, "row preference");
        try {
            combo.setPopupVisible(true);
            throw new AssertionError("not-showing selector opened");
        } catch (IllegalComponentStateException expected) {
            check(!combo.isPopupCreated(), "no popup allocation while hidden");
        }
        combo.setCloseOnSelection(false);
        check(!combo.isCloseOnSelection(), "popup retention option");

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode alpha = new DefaultMutableTreeNode("alpha");
        root.add(alpha);
        M3TreeCombo trees = new M3TreeCombo(new JTree(new DefaultTreeModel(root)));
        AtomicInteger treeEvents = new AtomicInteger();
        trees.addActionListener(e -> treeEvents.incrementAndGet());
        TreePath path = new TreePath(new Object[]{root, alpha});
        trees.setSelectedPath(path);
        check(path.equals(trees.getSelectedPath()) && trees.getText().equals("alpha"), "tree path identity");
        trees.commitSelection();
        check(treeEvents.get() == 1 && !trees.isPopupCreated(), "tree event; popup lazy");
        alpha.setUserObject("beta");
        ((DefaultTreeModel) trees.getTree().getModel()).nodeChanged(alpha);
        check(trees.getText().equals("beta"), "changed selected tree node refreshes text");
        trees.setSelectedPath(null);
        trees.commitSelection();
        check(trees.getText().isEmpty() && treeEvents.get() == 1, "tree clear is silent");
        DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode("new-root");
        trees.getTree().setModel(new DefaultTreeModel(newRoot));
        trees.setSelectedPath(new TreePath(newRoot));
        check(trees.getText().equals("new-root"), "replacement tree model");

        // JTable, not the combo, owns the dataset. A million rows must not
        // instantiate a million values just to create or present this selector.
        AtomicInteger reads = new AtomicInteger();
        AbstractTableModel huge = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;
            @Override public int getRowCount() { return 1_000_000; }
            @Override public int getColumnCount() { return 1; }
            @Override public Object getValueAt(int row, int col) {
                reads.incrementAndGet();
                return "row-" + row;
            }
        };
        M3TableCombo virtual = new M3TableCombo(huge);
        check(reads.get() == 0 && !virtual.isPopupCreated(), "million-row unopened popup reads nothing");
        check(virtual.selectModelRow(999_999), "select last model row");
        check(virtual.getText().equals("row-999999") && reads.get() <= 3,
                "only the requested logical row is materialized");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
