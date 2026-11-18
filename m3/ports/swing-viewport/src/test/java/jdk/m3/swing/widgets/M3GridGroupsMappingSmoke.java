/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.util.Random;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/** Reproducible model/view mapping equivalence against an independent dense oracle. */
public final class M3GridGroupsMappingSmoke {
    private static final int ROWS = 24_000;
    private static final int GROUPS = 600;
    private M3GridGroupsMappingSmoke() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3GridGroupsMappingSmoke::exercise);
        System.out.println("M3_GRID_GROUPS_MAPPING_24K_600_PASS");
    }

    private static void exercise() {
        AbstractTableModel model = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;
            @Override public int getRowCount() { return ROWS; }
            @Override public int getColumnCount() { return 2; }
            @Override public Object getValueAt(int row, int column) { return row; }
        };
        M3Grid grid = new M3Grid(model);
        final int[] starts = new int[GROUPS];
        final int[] ends = new int[GROUPS];
        final boolean[] collapsed = new boolean[GROUPS];
        // A mixture of direct-adjacent ranges and small gaps.
        for (int i = 0; i < GROUPS; i++) {
            starts[i] = i * 40;
            ends[i] = starts[i] + 27 + i % 13;
            grid.addRowGroup(starts[i], ends[i], "Group " + i);
        }
        Random random = new Random(180026);
        for (int i = 0; i < GROUPS; i++) {
            collapsed[i] = random.nextBoolean();
            if (collapsed[i]) grid.setRowGroupCollapsed(starts[i], true);
        }
        verify(grid, starts, ends, collapsed);
        for (int iteration = 0; iteration < 45; iteration++) {
            int selected = random.nextInt(GROUPS);
            collapsed[selected] = !collapsed[selected];
            grid.setRowGroupCollapsed(starts[selected], collapsed[selected]);
            verify(grid, starts, ends, collapsed);
        }
        grid.clearRowGroups();
        if (grid.getRowSorter() != null || grid.getRowCount() != ROWS) {
            throw new AssertionError("opt-out must restore an unwrapped JTable");
        }
    }

    private static void verify(M3Grid grid, int[] starts, int[] ends, boolean[] collapsed) {
        int nextGroup = 0, view = 0;
        for (int row = 0; row < ROWS; row++) {
            while (nextGroup < GROUPS && row >= ends[nextGroup]) nextGroup++;
            boolean hidden = nextGroup < GROUPS && collapsed[nextGroup]
                    && row > starts[nextGroup] && row < ends[nextGroup];
            int actual = grid.convertRowIndexToView(row);
            if (hidden) {
                if (actual != -1) throw new AssertionError("hidden model row " + row + " mapped to " + actual);
            } else {
                if (actual != view) throw new AssertionError("model " + row + " maps " + actual + " vs " + view);
                if (grid.convertRowIndexToModel(view) != row) {
                    throw new AssertionError("inverse mapping failed for model row " + row);
                }
                view++;
            }
        }
        if (view != grid.getRowCount()) {
            throw new AssertionError("visible count " + view + " != JTable " + grid.getRowCount());
        }
    }
}
