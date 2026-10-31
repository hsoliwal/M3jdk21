/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Compare the two fixed-cache reset regressions with the real stock cache.
 * @modules java.desktop/sun.swing.tree
 * @run junit/othervm -Djava.awt.headless=true M3FixedHeightSelectionResetTest
 */

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.FixedHeightLayoutCache;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.Test;

import sun.swing.tree.M3FixedHeightLayoutCache;

/** Public selection-reset observations; the stock cache is the independent oracle. */
public class M3FixedHeightSelectionResetTest {

    @SuppressWarnings("serial")
    static final class ObservedSelection extends DefaultTreeSelectionModel {
        int resets;

        @Override
        public void resetRowSelection() {
            resets++;
            super.resetRowSelection();
        }
    }

    record Trace(int resets, int rows, List<String> paths, boolean expanded,
            String selectionRows, int minSelectionRow, int maxSelectionRow, int leadRow) { }

    static Trace exercise(FixedHeightLayoutCache cache, boolean structureBecomesLeaf) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root", true);
        DefaultMutableTreeNode group = new DefaultMutableTreeNode("group", true);
        root.add(group);
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child", false);
        if (structureBecomesLeaf) {
            group.add(child);
        }
        // asksAllowsChildren keeps the empty group non-leaf in the collapse scenario.
        DefaultTreeModel model = new DefaultTreeModel(root, true);
        TreePath rootPath = new TreePath(root.getPath());
        TreePath groupPath = new TreePath(group.getPath());
        ObservedSelection selection = new ObservedSelection();
        cache.setRowHeight(17);
        cache.setRootVisible(true);
        cache.setModel(model);
        cache.setSelectionModel(selection);
        cache.setExpandedState(rootPath, true);
        cache.setExpandedState(groupPath, true);
        selection.setSelectionPath(structureBecomesLeaf
                ? new TreePath(child.getPath()) : groupPath);
        selection.resets = 0;

        if (structureBecomesLeaf) {
            group.setAllowsChildren(false);
            cache.treeStructureChanged(new TreeModelEvent(model, groupPath));
        } else {
            cache.setExpandedState(groupPath, false);
        }

        List<String> paths = new ArrayList<>();
        for (int row = 0; row < cache.getRowCount(); row++) {
            paths.add(String.valueOf(cache.getPathForRow(row)));
        }
        return new Trace(selection.resets, cache.getRowCount(), List.copyOf(paths),
                cache.isExpanded(groupPath), Arrays.toString(selection.getSelectionRows()),
                selection.getMinSelectionRow(), selection.getMaxSelectionRow(),
                selection.getLeadSelectionRow());
    }

    static void compare(String scenario, boolean structureBecomesLeaf) throws Exception {
        Trace[] observed = new Trace[2];
        SwingUtilities.invokeAndWait(() -> {
            observed[0] = exercise(new FixedHeightLayoutCache(), structureBecomesLeaf);
            observed[1] = exercise(new M3FixedHeightLayoutCache(), structureBecomesLeaf);
        });
        System.out.println(scenario + " stock=" + observed[0]);
        System.out.println(scenario + " candidate=" + observed[1]);
        assertAll(scenario,
                () -> assertEquals(1, observed[0].resets(), "stock regression witness"),
                () -> assertEquals(observed[0], observed[1], "stock/candidate observable trace"));
    }

    @Test
    void collapseExpandedEmptyNonLeafResetsSelection() throws Exception {
        compare("collapse-expanded-empty-nonleaf", false);
    }

    @Test
    void structureChangedExpandedNodeBecomingLeafResetsSelection() throws Exception {
        compare("structure-expanded-to-leaf", true);
    }
}
