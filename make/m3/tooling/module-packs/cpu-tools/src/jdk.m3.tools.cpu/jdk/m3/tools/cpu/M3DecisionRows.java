// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

import java.util.Arrays;

/** Deterministic, fail-closed numeric decision tables backed by primitive CSR lanes. */
public final class M3DecisionRows {
    private static final int MAX_ROWS = 65536;
    private static final int MAX_CONDITIONS = 1_048_576;

    private M3DecisionRows() { }

    /**
     * Select the matching row with minimum numeric priority.
     * If equal-priority matching rows disagree on action, reject ambiguity.
     * Otherwise tie-break by original row index. Returns a fresh
     * [rowIndex, action] pair, or [-1,-1] when no row matches.
     */
    public static int[] decide(int[] values, int[] rowOffsets, int[] field,
            int[] minimum, int[] maximum, int[] priority, int[] action) {
        validate(values, rowOffsets, field, minimum, maximum, priority, action);
        int best = -1;
        boolean ambiguous = false;
        for (int row = 0; row < priority.length; row++) {
            if (!matches(values, rowOffsets, field, minimum, maximum, row)) { continue; }
            if (best == -1 || priority[row] < priority[best]) {
                best = row;
                ambiguous = false;
            } else if (priority[row] == priority[best] && action[row] != action[best]) {
                ambiguous = true;
            }
        }
        if (ambiguous) { throw new IllegalStateException("equal priority has conflicting actions"); }
        return best == -1 ? new int[] {-1, -1} : new int[] {best, action[best]};
    }

    /** All matching row indexes in source order; never return a truncated result. */
    public static int[] matchingRows(int[] values, int[] rowOffsets, int[] field,
            int[] minimum, int[] maximum, int[] priority, int[] action, int maxMatches) {
        validate(values, rowOffsets, field, minimum, maximum, priority, action);
        if (maxMatches < 0 || maxMatches > MAX_ROWS) {
            throw new IllegalArgumentException("invalid result budget");
        }
        int[] result = new int[Math.min(priority.length, maxMatches)];
        int size = 0;
        for (int row = 0; row < priority.length; row++) {
            if (!matches(values, rowOffsets, field, minimum, maximum, row)) { continue; }
            if (size == result.length) { throw new IllegalStateException("match output budget exhausted"); }
            result[size++] = row;
        }
        return Arrays.copyOf(result, size);
    }

    private static boolean matches(int[] values, int[] rowOffsets, int[] field,
            int[] minimum, int[] maximum, int row) {
        for (int position = rowOffsets[row]; position < rowOffsets[row + 1]; position++) {
            int value = values[field[position]];
            if (value < minimum[position] || value > maximum[position]) { return false; }
        }
        return true;
    }

    private static void validate(int[] values, int[] rowOffsets, int[] field,
            int[] minimum, int[] maximum, int[] priority, int[] action) {
        if (values == null || rowOffsets == null || field == null || minimum == null
                || maximum == null || priority == null || action == null
                || priority.length > MAX_ROWS || field.length > MAX_CONDITIONS
                || rowOffsets.length != priority.length + 1 || action.length != priority.length
                || minimum.length != field.length || maximum.length != field.length) {
            throw new IllegalArgumentException("invalid decision lanes");
        }
        if (rowOffsets[0] != 0 || rowOffsets[priority.length] != field.length) {
            throw new IllegalArgumentException("invalid decision CSR endpoints");
        }
        for (int row = 0; row < priority.length; row++) {
            if (rowOffsets[row] < 0 || rowOffsets[row + 1] < rowOffsets[row]
                    || rowOffsets[row + 1] > field.length) {
                throw new IllegalArgumentException("invalid row offsets");
            }
        }
        for (int i = 0; i < field.length; i++) {
            if (field[i] < 0 || field[i] >= values.length || minimum[i] > maximum[i]) {
                throw new IllegalArgumentException("invalid decision condition");
            }
        }
    }
}
