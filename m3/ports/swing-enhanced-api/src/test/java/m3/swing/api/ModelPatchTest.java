/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.M3Patch;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.Test;

/**
 * Contract of the Swing model seam (leaf S8): DefaultTableModel.apply, DefaultListModel.apply, DefaultTreeModel.apply
 * and AbstractTableModel.firePatch. The twin receives the same delta through the stock per-row API (removeRow,
 * insertRow, setValueAt, remove/add/set, insertNodeInto/removeNodeFromParent). Data, selection and expansion must be
 * equal; the patched model sends one event per run, never a whole-model change.
 */
public class ModelPatchTest {

    /* a random patch of ids 0..size-1 style lists: removes, inserts with fresh ids, updates */
    static M3Patch randomPatch(Random random, int size, long[] nextId) {
        M3Patch.Builder builder = M3Patch.builder(M3Patch.ROOT);
        int limit = size;
        for (int k = random.nextInt(4); k > 0 && limit > 0; k--) {
            int count = 1 + random.nextInt(Math.min(5, limit));
            int at = random.nextInt(limit - count + 1);
            builder.remove(at, count);
            limit = at;
            size -= count;
        }
        int from = 0;
        for (int k = random.nextInt(4); k > 0; k--) {
            int at = from + random.nextInt(size - from + 1);
            int count = 1 + random.nextInt(4);
            long[] fresh = new long[count];
            for (int i = 0; i < count; i++) {
                fresh[i] = nextId[0]++;
            }
            builder.insert(at, fresh);
            size += count;
            from = at + count;
        }
        int up = 0;
        for (int k = random.nextInt(3); k > 0 && up < size; k--) {
            int at = up + random.nextInt(size - up);
            int count = 1 + random.nextInt(Math.min(3, size - at));
            builder.update(at, count);
            up = at + count;
        }
        return builder.build();
    }

    private static DefaultTableModel tableModel(long[] ids) {
        DefaultTableModel model = new DefaultTableModel(0, 2);
        for (long id : ids) {
            model.addRow(new Object[] {"row " + id, id});
        }
        return model;
    }

    @Test
    public void tableApplyEqualsStockRowEdits() {
        Random random = new Random(8);
        long[] ids = new long[80];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;
        }
        JTable patched = new JTable(tableModel(ids));
        JTable stock = new JTable(tableModel(ids));
        List<TableModelEvent> events = new ArrayList<>();
        patched.getModel().addTableModelListener(events::add);
        long[] nextId = {1000};
        int version = 0;
        for (int round = 0; round < 80; round++) {
            int[] selection = random.ints(4, 0, Math.max(1, ids.length)).toArray();
            patched.clearSelection();
            stock.clearSelection();
            for (int row : selection) {
                if (row < ids.length) {
                    patched.addRowSelectionInterval(row, row);
                    stock.addRowSelectionInterval(row, row);
                }
            }
            M3Patch patch = randomPatch(random, ids.length, nextId);
            long[] after = patch.apply(ids);
            String tag = "v" + (++version);
            events.clear();
            ((DefaultTableModel) patched.getModel()).apply(patch, row -> new Object[] {"row " + after[row] + tag, after[row]});
            DefaultTableModel model = (DefaultTableModel) stock.getModel();
            patch.applyTo(new M3Patch.Target() {
                @Override
                public void remove(int index, int count) {
                    for (int k = count - 1; k >= 0; k--) {
                        model.removeRow(index + k);
                    }
                }

                @Override
                public void insert(int index, int count) {
                    for (int k = 0; k < count; k++) {
                        model.insertRow(index + k, new Object[] {"row " + after[index + k] + tag, after[index + k]});
                    }
                }

                @Override
                public void update(int index, int count) {
                    for (int k = 0; k < count; k++) {
                        model.setValueAt("row " + after[index + k] + tag, index + k, 0);
                    }
                }
            });
            assertEquals(model.getDataVector(), ((DefaultTableModel) patched.getModel()).getDataVector(), "round " + round);
            assertArrayEquals(stock.getSelectedRows(), patched.getSelectedRows(), "round " + round);
            assertEquals(patch.removeRuns() + patch.insertRuns() + patch.updateRuns(), events.size(), "one event per run");
            for (TableModelEvent event : events) {
                assertNotEquals(TableModelEvent.HEADER_ROW, event.getFirstRow(), "never a structure change");
                assertNotEquals(Integer.MAX_VALUE, event.getLastRow(), "never a whole-data change");
            }
            ids = after;
        }
        DefaultTableModel model = (DefaultTableModel) patched.getModel();
        assertThrows(IndexOutOfBoundsException.class, () -> model.apply(M3Patch.builder(M3Patch.ROOT).remove(0, 10_000).build(), row -> null));
        assertThrows(NullPointerException.class, () -> model.apply(M3Patch.builder(M3Patch.ROOT).build(), null));
        assertThrows(NullPointerException.class, () -> model.apply(null, row -> null));
        assertThrows(NullPointerException.class, () -> model.firePatch(M3Patch.builder(M3Patch.ROOT).build(), null));
    }

    @Test
    public void listApplyEqualsStockElementEdits() {
        Random random = new Random(9);
        DefaultListModel<String> patched = new DefaultListModel<>();
        DefaultListModel<String> stock = new DefaultListModel<>();
        long[] ids = new long[60];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;
            patched.addElement("item " + i);
            stock.addElement("item " + i);
        }
        JList<String> list = new JList<>(patched);
        JList<String> stockList = new JList<>(stock);
        int[] events = new int[1];
        patched.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                events[0]++;
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                events[0]++;
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                events[0]++;
            }
        });
        long[] nextId = {1000};
        for (int round = 0; round < 80; round++) {
            int a = random.nextInt(Math.max(1, ids.length));
            list.setSelectionInterval(a, a);
            stockList.setSelectionInterval(a, a);
            M3Patch patch = randomPatch(random, ids.length, nextId);
            long[] after = patch.apply(ids);
            String tag = "r" + round;
            events[0] = 0;
            patched.apply(patch, index -> "item " + after[index] + tag);
            patch.applyTo(new M3Patch.Target() {
                @Override
                public void remove(int index, int count) {
                    for (int k = count - 1; k >= 0; k--) {
                        stock.remove(index + k);
                    }
                }

                @Override
                public void insert(int index, int count) {
                    for (int k = 0; k < count; k++) {
                        stock.add(index + k, "item " + after[index + k] + tag);
                    }
                }

                @Override
                public void update(int index, int count) {
                    for (int k = 0; k < count; k++) {
                        stock.set(index + k, "item " + after[index + k] + tag);
                    }
                }
            });
            assertArrayEquals(stock.toArray(), patched.toArray(), "round " + round);
            assertArrayEquals(stockList.getSelectedIndices(), list.getSelectedIndices(), "round " + round);
            assertEquals(patch.removeRuns() + patch.insertRuns() + patch.updateRuns(), events[0], "one event per run");
            ids = after;
        }
        assertThrows(IndexOutOfBoundsException.class, () -> patched.apply(M3Patch.builder(M3Patch.ROOT).update(0, 10_000).build(), i -> ""));
        assertThrows(NullPointerException.class, () -> patched.apply(M3Patch.builder(M3Patch.ROOT).build(), null));
    }

    @Test
    public void treeApplyKeepsUntouchedExpansionAndSelection() {
        Random random = new Random(10);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode stockRoot = new DefaultMutableTreeNode("root");
        long[] ids = new long[30];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;
            root.add(node(i, true));
            stockRoot.add(node(i, true));
        }
        DefaultTreeModel model = new DefaultTreeModel(root);
        DefaultTreeModel stockModel = new DefaultTreeModel(stockRoot);
        JTree tree = new JTree(model);
        JTree stockTree = new JTree(stockModel);
        for (int r = 28; r >= 0; r -= 4) {
            tree.expandRow(r);
            stockTree.expandRow(r);
        }
        int[] events = new int[2];
        model.addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                events[0]++;
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                events[0]++;
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                events[0]++;
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                events[1]++;
            }
        });
        long[] nextId = {1000};
        for (int round = 0; round < 50; round++) {
            int row = random.nextInt(tree.getRowCount());
            tree.setSelectionRow(row);
            stockTree.setSelectionRow(row);
            M3Patch patch = randomPatch(random, ids.length, nextId);
            long[] after = patch.apply(ids);
            events[0] = 0;
            model.apply(patch, root, index -> node(after[index], false));
            patch.applyTo(new M3Patch.Target() {
                @Override
                public void remove(int index, int count) {
                    for (int k = count - 1; k >= 0; k--) {
                        stockModel.removeNodeFromParent((DefaultMutableTreeNode) stockRoot.getChildAt(index + k));
                    }
                }

                @Override
                public void insert(int index, int count) {
                    for (int k = 0; k < count; k++) {
                        stockModel.insertNodeInto(node(after[index + k], false), stockRoot, index + k);
                    }
                }

                @Override
                public void update(int index, int count) {
                    for (int k = 0; k < count; k++) {
                        stockModel.nodeChanged(stockRoot.getChildAt(index + k));
                    }
                }
            });
            assertEquals(describe(stockTree), describe(tree), "round " + round);
            assertEquals(patch.removeRuns() + patch.insertRuns() + patch.updateRuns(), events[0], "one event per run");
            assertEquals(0, events[1], "never a structure change");
            ids = after;
        }
        assertThrows(IndexOutOfBoundsException.class, () -> model.apply(M3Patch.builder(M3Patch.ROOT).remove(0, 10_000).build(), root, i -> null));
        assertThrows(NullPointerException.class, () -> model.apply(M3Patch.builder(M3Patch.ROOT).build(), root, null));
    }

    private static DefaultMutableTreeNode node(long id, boolean withChildren) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("n" + id);
        if (withChildren) {
            for (int c = 0; c < 3; c++) {
                node.add(new DefaultMutableTreeNode("n" + id + "." + c));
            }
        }
        return node;
    }

    /* the visible rows, expansion and selection of a tree */
    private static String describe(JTree tree) {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            text.append(path.getLastPathComponent()).append(tree.isExpanded(row) ? "+" : "")
                    .append(tree.isRowSelected(row) ? "*" : "").append(' ');
        }
        return text.toString();
    }
}
