/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary DefaultTreeSelectionModel.insureRowContinuity must not throw
 *          ArrayIndexOutOfBoundsException when a CONTIGUOUS selection holds a path that is not
 *          visible (row -1) and the visible rows have a gap (M3 upstream defect D2).
 * @run main/othervm -Djava.awt.headless=true M3TreeSelectionContinuityTest
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.RowMapper;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * D2 (m3/docs/swing-viewport/UPSTREAM_DEFECTS.tsv). {@code insureRowContinuity} keeps the first run
 * of contiguous selected rows: it sizes the new selection to that run and copies every selected
 * path whose row is below the first gap, {@code newSel[row - min]}. A selected path that is not
 * visible has row -1, which is below every gap, so the index is negative. A CONTIGUOUS selection
 * can hold such a path: a single path is always contiguous, and {@code JTree} does not make it
 * visible when {@code expandsSelectedPaths} is false. The documented result is the first run of
 * contiguous rows; a path without a row is in no run.
 */
public final class M3TreeSelectionContinuityTest {

    public static void main(String[] args) {
        modelWithRowMapper();
        jtreeInsertBetweenSelectedRows();
        System.out.println("passed");
    }

    /** The model alone, with a row mapper whose rows the test sets. */
    static void modelWithRowMapper() {
        TreePath p1 = new TreePath(new Object[] {"r", "1"});
        TreePath p2 = new TreePath(new Object[] {"r", "2"});
        TreePath p3 = new TreePath(new Object[] {"r", "3"});
        Map<TreePath, Integer> rows = new HashMap<>(Map.of(p1, 0, p2, 1, p3, 2));
        RowMapper mapper = paths -> {
            int[] out = new int[paths.length];
            for (int i = 0; i < paths.length; i++) {
                out[i] = rows.getOrDefault(paths[i], -1);
            }
            return out;
        };
        DefaultTreeSelectionModel model = new DefaultTreeSelectionModel();
        model.setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        model.setRowMapper(mapper);
        model.setSelectionPaths(new TreePath[] {p1, p2, p3});
        rows.put(p2, -1);
        rows.put(p3, 2);
        model.resetRowSelection();
        check(new TreePath[] {p1}, model.getSelectionPaths(), "model: first run of rows, hidden path dropped");
        if (!Arrays.equals(new int[] {0}, model.getSelectionRows())) {
            throw new RuntimeException("rows " + Arrays.toString(model.getSelectionRows()));
        }
    }

    /** Through JTree: a hidden selected path, two selected rows, a node inserted between them. */
    static void jtreeInsertBetweenSelectedRows() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
        DefaultMutableTreeNode hidden = new DefaultMutableTreeNode("hidden");
        DefaultMutableTreeNode b = new DefaultMutableTreeNode("b");
        DefaultMutableTreeNode c = new DefaultMutableTreeNode("c");
        a.add(hidden);
        root.add(a);
        root.add(b);
        root.add(c);
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        JTree tree = new JTree(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        tree.setExpandsSelectedPaths(false);
        TreePath hiddenPath = new TreePath(hidden.getPath());
        tree.setSelectionPath(hiddenPath);
        TreePath pb = new TreePath(b.getPath());
        TreePath pc = new TreePath(c.getPath());
        tree.addSelectionPath(pb);
        tree.addSelectionPath(pc);
        check(new TreePath[] {hiddenPath, pb, pc}, tree.getSelectionPaths(), "setup");
        treeModel.insertNodeInto(new DefaultMutableTreeNode("between"), root, 2);
        check(new TreePath[] {pb}, tree.getSelectionPaths(), "JTree: first run of rows, hidden path dropped");
    }

    static void check(TreePath[] want, TreePath[] got, String what) {
        if (!Arrays.equals(want, got)) {
            throw new RuntimeException(what + ": want " + Arrays.toString(want) + " got " + Arrays.toString(got));
        }
    }
}
