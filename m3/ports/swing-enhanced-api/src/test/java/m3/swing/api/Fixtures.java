/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/** Components of the tests and of the behaviour probe; stock API only, so the probe also runs on master's classes. */
final class Fixtures {

    private Fixtures() {
    }

    static JList<String> list(int size) {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < size; i++) {
            model.addElement("item " + i);
        }
        return new JList<>(model);
    }

    static JTable table(int rows) {
        DefaultTableModel model = new DefaultTableModel(0, 2);
        for (int i = 0; i < rows; i++) {
            model.addRow(new Object[] {"row " + i, i});
        }
        return new JTable(model);
    }

    /* 20 roots with 4 children each under a hidden root; the even roots expanded */
    static JTree tree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        for (int r = 0; r < 20; r++) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode("r" + r);
            for (int c = 0; c < 4; c++) {
                node.add(new DefaultMutableTreeNode("r" + r + "." + c));
            }
            root.add(node);
        }
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        for (int r = 18; r >= 0; r -= 2) {
            tree.expandRow(r);
        }
        return tree;
    }
}
