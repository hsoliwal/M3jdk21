/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package m3.swing.api;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.Test;

/**
 * Contract of JTree.getSubtreeRowCount and getRowAtExtent (leaf S3), headless. The oracles walk the rows linearly:
 * a subtree is the path's row plus the following rows that are its descendants; the row at y is the row whose bounds
 * contain y.
 */
public class TreeRowMathTest {

    private static DefaultMutableTreeNode grow(Random random, String name, int depth) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
        int children = depth == 0 ? 0 : random.nextInt(7);
        for (int i = 0; i < children; i++) {
            node.add(grow(random, name + "." + i, depth - 1));
        }
        return node;
    }

    private static List<TreePath> allPaths(DefaultMutableTreeNode root) {
        List<TreePath> paths = new ArrayList<>();
        for (TreeNode node : Collections.list(root.breadthFirstEnumeration())) {
            paths.add(new TreePath(((DefaultMutableTreeNode) node).getPath()));
        }
        return paths;
    }

    private static int oracleSubtree(JTree tree, TreePath path) {
        int row = tree.getRowForPath(path);
        if (row < 0) {
            boolean hiddenRoot = path.getParentPath() == null && !tree.isRootVisible();
            return hiddenRoot && tree.isExpanded(path) ? tree.getRowCount() : 0;
        }
        int rows = 1;
        if (tree.isExpanded(row)) {
            for (int r = row + 1; r < tree.getRowCount() && path.isDescendant(tree.getPathForRow(r)); r++) {
                rows++;
            }
        }
        return rows;
    }

    private static int oracleRowAt(JTree tree, int y) {
        for (int row = 0; row < tree.getRowCount(); row++) {
            Rectangle r = tree.getRowBounds(row);
            if (y >= r.y && y < r.y + r.height) {
                return row;
            }
        }
        return -1;
    }

    @Test
    public void subtreeRowsAgreeWithTheWalk() {
        Random random = new Random(3);
        for (int round = 0; round < 12; round++) {
            DefaultMutableTreeNode root = grow(random, "r", 4);
            JTree tree = new JTree(new DefaultTreeModel(root));
            tree.setRootVisible(round % 2 == 0);
            List<TreePath> paths = allPaths(root);
            for (TreePath path : paths) {
                if (random.nextInt(3) > 0) {
                    tree.expandPath(path);
                }
            }
            for (int k = 0; k < 5; k++) {
                tree.collapsePath(paths.get(random.nextInt(paths.size())));
            }
            for (TreePath path : paths) {
                assertEquals(oracleSubtree(tree, path), tree.getSubtreeRowCount(path), "round " + round + " " + path);
            }
            assertEquals(tree.isRootVisible() || tree.isExpanded(paths.get(0)) ? tree.getRowCount() : 0,
                    tree.getSubtreeRowCount(paths.get(0)), "the root covers every row");
        }
    }

    @Test
    public void edgeCases() {
        DefaultMutableTreeNode root = grow(new Random(8), "r", 2);
        JTree tree = new JTree(new DefaultTreeModel(root));
        assertEquals(0, tree.getSubtreeRowCount(null));
        assertEquals(0, tree.getSubtreeRowCount(new TreePath("not in the tree")));
        assertEquals(0, tree.getSubtreeRowCount(new TreePath(new Object[] {root, "stranger"})));
        tree.setRootVisible(false);
        TreePath rootPath = new TreePath(root);
        assertEquals(tree.getRowCount(), tree.getSubtreeRowCount(rootPath));
        tree.collapsePath(rootPath);
        assertEquals(0, tree.getRowCount());
        assertEquals(0, tree.getSubtreeRowCount(rootPath));
        assertEquals(-1, tree.getRowAtExtent(0));

        DefaultMutableTreeNode leafy = new DefaultMutableTreeNode("leafy");
        leafy.add(new DefaultMutableTreeNode("empty", true));
        JTree flat = new JTree(new DefaultTreeModel(leafy, true));
        TreePath empty = new TreePath(new Object[] {leafy, leafy.getChildAt(0)});
        flat.expandPath(empty);
        assertEquals(1, flat.getSubtreeRowCount(empty), "an expanded node without children");
        assertEquals(2, flat.getSubtreeRowCount(new TreePath(leafy)));
        flat.setRootVisible(false);
        flat.setModel(null);
        assertEquals(0, flat.getSubtreeRowCount(new TreePath(leafy)));
    }

    @Test
    public void rowAtExtentAgreesWithTheRowBounds() {
        DefaultMutableTreeNode root = grow(new Random(21), "r", 3);
        JTree tree = new JTree(new DefaultTreeModel(root));
        allPaths(root).forEach(tree::expandPath);
        tree.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 0, 0));
        Rectangle last = tree.getRowBounds(tree.getRowCount() - 1);
        for (int y = -3; y < last.y + last.height + 4; y++) {
            assertEquals(oracleRowAt(tree, y), tree.getRowAtExtent(y), "y " + y);
            int closest = tree.getClosestRowForLocation(0, y);
            int exact = tree.getRowAtExtent(y);
            assertTrue(exact == -1 || exact == closest, "inside the rows the closest row is the row");
        }
        tree.setRowHeight(0);
        assertEquals(oracleRowAt(tree, 30), tree.getRowAtExtent(30), "variable row heights");
    }
}
