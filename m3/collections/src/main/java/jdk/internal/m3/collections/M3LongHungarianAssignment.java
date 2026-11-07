/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Dense minimum-cost rectangular assignment using primitive potentials/predecessor lanes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongHungarianAssignment.java}
 * blob {@code 6a5f58273adde14f02665203d812394dca1ea997} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LongHungarianAssignment {
    public static final class Result {
        private final long minimumCost;
        private final int[] columnByRow;

        private Result(long minimumCost, int[] columnByRow) {
            this.minimumCost = minimumCost;
            this.columnByRow = columnByRow;
        }

        public long minimumCost() { return minimumCost; }
        public int columnForRow(int row) { return columnByRow[row]; }
        public int[] copyAssignment() { return columnByRow.clone(); }
    }

    private M3LongHungarianAssignment() {}

    /**
     * Solves assignment of every row to a distinct column.
     *
     * @param rowMajorCosts exactly rows*columns costs
     * @throws IllegalArgumentException when rows > columns
     */
    public static Result solve(
            int rows,
            int columns,
            long[] rowMajorCosts) {
        if (rows < 0 || columns < 0) throw new IllegalArgumentException("negative dimension");
        if (rows > columns) throw new IllegalArgumentException("rows must be <= columns");
        Objects.requireNonNull(rowMajorCosts, "rowMajorCosts");
        if (rowMajorCosts.length != Math.multiplyExact(rows, columns)) {
            throw new IllegalArgumentException("cost lane length mismatch");
        }
        if (rows == 0) return new Result(0L, new int[0]);

        long[] u = new long[rows + 1];
        long[] v = new long[columns + 1];
        int[] p = new int[columns + 1];
        int[] way = new int[columns + 1];
        long[] minValue = new long[columns + 1];
        boolean[] used = new boolean[columns + 1];

        for (int row = 1; row <= rows; row++) {
            p[0] = row;
            Arrays.fill(minValue, Long.MAX_VALUE);
            Arrays.fill(used, false);
            int column0 = 0;

            do {
                used[column0] = true;
                int row0 = p[column0];
                long delta = Long.MAX_VALUE;
                int column1 = 0;
                for (int column = 1; column <= columns; column++) {
                    if (used[column]) continue;
                    long cost = rowMajorCosts[(row0 - 1) * columns + (column - 1)];
                    long reduced = Math.subtractExact(
                            Math.subtractExact(cost, u[row0]),
                            v[column]);
                    if (reduced < minValue[column]) {
                        minValue[column] = reduced;
                        way[column] = column0;
                    }
                    if (minValue[column] < delta) {
                        delta = minValue[column];
                        column1 = column;
                    }
                }
                if (delta == Long.MAX_VALUE) {
                    throw new IllegalStateException("no augmenting assignment");
                }
                for (int column = 0; column <= columns; column++) {
                    if (used[column]) {
                        u[p[column]] = Math.addExact(u[p[column]], delta);
                        v[column] = Math.subtractExact(v[column], delta);
                    } else if (minValue[column] != Long.MAX_VALUE) {
                        minValue[column] = Math.subtractExact(minValue[column], delta);
                    }
                }
                column0 = column1;
            } while (p[column0] != 0);

            do {
                int column1 = way[column0];
                p[column0] = p[column1];
                column0 = column1;
            } while (column0 != 0);
        }

        int[] assignment = new int[rows];
        Arrays.fill(assignment, -1);
        for (int column = 1; column <= columns; column++) {
            if (p[column] != 0) assignment[p[column] - 1] = column - 1;
        }

        long total = 0L;
        for (int row = 0; row < rows; row++) {
            int column = assignment[row];
            if (column < 0) throw new IllegalStateException("incomplete assignment");
            total = Math.addExact(total, rowMajorCosts[row * columns + column]);
        }
        return new Result(total, assignment);
    }
}
