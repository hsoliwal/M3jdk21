// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/** Guaranteed deterministic pairwise categorical coverage with a flat int[] row store.
 * Produces at most the cross-product sum for each variable pair, not a minimal covering array.
 */
public final class M3PairwiseCases {
    private M3PairwiseCases() { }
    public record Cases(int parameters, int[] values) {
        public Cases {
            if (parameters < 2 || values.length % parameters != 0)
                throw new IllegalArgumentException("case geometry");
            values = values.clone();
        }
        @Override public int[] values() { return values.clone(); }
        public int rows() { return values.length / parameters; }
        public int at(int row, int column) {
            if (row < 0 || row >= rows() || column < 0 || column >= parameters)
                throw new IndexOutOfBoundsException("pairwise row/column");
            return values[Math.addExact(Math.multiplyExact(row, parameters), column)];
        }
    }

    /** 2..16 variables, 1..32 levels each, optional hard row cap (<= 65536). */
    public static Cases cover(int[] levels, int maxRows) {
        Objects.requireNonNull(levels);
        if (levels.length < 2 || levels.length > 16 || maxRows < 1 || maxRows > 65536)
            throw new IllegalArgumentException("pairwise geometry");
        long total = 0;
        for (int x : levels) if (x < 1 || x > 32)
            throw new IllegalArgumentException("levels");
        for (int i = 0; i < levels.length; i++)
            for (int j = i + 1; j < levels.length; j++)
                total = Math.addExact(total, (long)levels[i] * levels[j]);
        if (total > maxRows) throw new IllegalArgumentException("pairwise row budget");
        int parameters = levels.length;
        int[] values = new int[Math.multiplyExact((int)total, parameters)];
        int row = 0;
        for (int i = 0; i < parameters; i++)
            for (int j = i + 1; j < parameters; j++)
                for (int a = 0; a < levels[i]; a++)
                    for (int b = 0; b < levels[j]; b++) {
                        values[row * parameters + i] = a;
                        values[row * parameters + j] = b;
                        row++;
                    }
        return new Cases(parameters, values);
    }
}
