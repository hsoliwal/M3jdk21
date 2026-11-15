/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary JTable rejects points above the first row and keeps lazy row geometry
 * @run main/othervm -Xmx64m -Djava.awt.headless=true -Dm3.swing.viewport=false RowAtPointBounds
 * @run main/othervm -Xmx64m -Djava.awt.headless=true -Dm3.swing.viewport=true RowAtPointBounds
 */

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

public class RowAtPointBounds {
    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable(new LazyRows(3));
            table.setRowHeight(16);
            verify(table, "uniform", new int[][] {
                {Integer.MIN_VALUE, -1}, {-17, -1}, {-16, -1}, {-15, -1}, {-1, -1},
                {0, 0}, {15, 0}, {16, 1}, {31, 1}, {32, 2}, {47, 2},
                {48, -1}, {Integer.MAX_VALUE, -1}
            });
            table.setRowHeight(1, 24);
            verify(table, "variable", new int[][] {
                {Integer.MIN_VALUE, -1}, {-17, -1}, {-16, -1}, {-15, -1}, {-1, -1},
                {0, 0}, {15, 0}, {16, 1}, {39, 1}, {40, 2}, {55, 2}, {56, -1}
            });
            verify(new JTable(new LazyRows(0)), "empty", new int[][] {
                {-1, -1}, {0, -1}, {Integer.MAX_VALUE, -1}
            });

            // Constant-height lookup must not traverse or materialize the model.
            JTable large = new JTable(new LazyRows(1_000_000_000));
            large.setRowHeight(16);
            verify(large, "large uniform", new int[][] {
                {-1, -1}, {1_600_000_000, 100_000_000}, {Integer.MAX_VALUE, 134_217_727}
            });
            if (Boolean.getBoolean("m3.swing.viewport")) {
                // One distant override must keep the existing sparse row owner.
                large.setRowHeight(500_000_000, 17);
                verify(large, "large sparse", new int[][] {
                    {-1, -1}, {1_600_000_000, 100_000_000}, {Integer.MAX_VALUE, 134_217_727}
                });
            }
        });
        if (!failures.isEmpty()) {
            throw new AssertionError(failures.size() + " of " + checks + " checks failed: " + failures);
        }
        System.out.println(checks + " row-hit checks passed");
    }

    private static void verify(JTable table, String label, int[][] cases) {
        for (int x : new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE}) {
            for (int[] entry : cases) {
                int actual = table.rowAtPoint(new Point(x, entry[0]));
                checks++;
                if (actual != entry[1]) {
                    failures.add(label + " at " + x + "," + entry[0]
                            + ": expected " + entry[1] + ", got " + actual);
                }
            }
        }
    }

    private static final class LazyRows extends AbstractTableModel {
        private final int rows;

        LazyRows(int rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int row, int column) {
            throw new AssertionError("hit testing must not materialize model row " + row);
        }
    }
}
