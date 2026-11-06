// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

/** Exact functional-dependency counterexamples for bounded primitive row tables. */
public final class M3FunctionalDependency {
    private static final int MAX_ROWS = 8192;
    private static final int MAX_COLUMNS = 64;
    private static final long MAX_WORK = 50_000_000L;

    private M3FunctionalDependency() { }

    /**
     * Find the lexicographically earliest (i,j), i<j, with equal determinant
     * fields and at least one unequal dependent field. Output [-1,-1] if valid.
     * All integer cells are compared exactly; no null or collation semantics.
     * The input table is never mutated or retained.
     */
    public static int[] firstViolation(int rows, int columns, int[] rowMajor,
            int[] determinantFields, int[] dependentFields, long maxFieldComparisons) {
        validate(rows, columns, rowMajor, determinantFields, dependentFields,
                maxFieldComparisons);
        if (rows < 2 || dependentFields.length == 0) { return new int[] {-1, -1}; }
        long[] left = {maxFieldComparisons};
        if (determinantFields.length == 0) {
            for (int j = 1; j < rows; j++) {
                if (!same(rowMajor, columns, 0, j, dependentFields, left)) {
                    return new int[] {0, j};
                }
            }
            return new int[] {-1, -1};
        }
        int[] order = new int[rows];
        int[] scratch = new int[rows];
        for (int row = 0; row < rows; row++) { order[row] = row; }
        for (int width = 1; width < rows; width <<= 1) {
            for (int start = 0; start < rows; start += width << 1) {
                int mid = Math.min(rows, start + width);
                int end = Math.min(rows, start + (width << 1));
                int l = start;
                int r = mid;
                for (int at = start; at < end; at++) {
                    if (r == end || (l < mid && lessOrEqual(rowMajor, columns,
                            order[l], order[r], determinantFields, left))) {
                        scratch[at] = order[l++];
                    } else {
                        scratch[at] = order[r++];
                    }
                }
            }
            int[] swap = order;
            order = scratch;
            scratch = swap;
        }
        int earliestI = -1;
        int earliestJ = -1;
        int begin = 0;
        while (begin < rows) {
            int end = begin + 1;
            while (end < rows && same(rowMajor, columns, order[begin],
                    order[end], determinantFields, left)) { end++; }
            if (end - begin > 1) {
                int first = order[begin];
                for (int k = begin + 1; k < end; k++) {
                    if (!same(rowMajor, columns, first, order[k], dependentFields, left)) {
                        int second = order[k];
                        if (earliestI == -1 || first < earliestI
                                || (first == earliestI && second < earliestJ)) {
                            earliestI = first;
                            earliestJ = second;
                        }
                        break;
                    }
                }
            }
            begin = end;
        }
        return new int[] {earliestI, earliestJ};
    }

    private static boolean lessOrEqual(int[] data, int cols, int a, int b,
            int[] fields, long[] remaining) {
        for (int field : fields) {
            charge(remaining);
            int x = data[a * cols + field];
            int y = data[b * cols + field];
            if (x != y) { return x < y; }
        }
        return a <= b;
    }

    private static boolean same(int[] data, int cols, int a, int b,
            int[] fields, long[] remaining) {
        for (int field : fields) {
            charge(remaining);
            if (data[a * cols + field] != data[b * cols + field]) { return false; }
        }
        return true;
    }

    private static void charge(long[] remaining) {
        if (remaining[0] == 0L) {
            throw new IllegalStateException("functional dependency comparison budget exhausted");
        }
        remaining[0]--;
    }

    private static void validate(int rows, int columns, int[] values,
            int[] determinants, int[] dependents, long maxWork) {
        if (rows < 0 || rows > MAX_ROWS || columns < 0 || columns > MAX_COLUMNS
                || values == null || determinants == null || dependents == null
                || values.length != rows * columns || maxWork < 0L || maxWork > MAX_WORK) {
            throw new IllegalArgumentException("invalid functional dependency table or budget");
        }
        checkFields(determinants, columns);
        checkFields(dependents, columns);
    }

    private static void checkFields(int[] fields, int columns) {
        int last = -1;
        for (int field : fields) {
            if (field <= last || field < 0 || field >= columns) {
                throw new IllegalArgumentException("field IDs must be strictly increasing");
            }
            last = field;
        }
    }
}
