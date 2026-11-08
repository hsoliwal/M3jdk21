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
package jdk.internal.m3.collections.mat;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable variable-width int rows encoded as {@code offsets[row..row+1]} into one arena.
 *
 * <p>This is the common physical shape for children, adjacency, tuples, posting lists and any
 * variable-length immutable integer relation.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatOffsetIntArena.java}
 * blob {@code 5aaa17e9f579d4f5368f619535d3804482aec175} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatOffsetIntArena {
    private final int[] offsets;
    private final int[] arena;

    private MatOffsetIntArena(int[] offsets, int[] arena) {
        this.offsets = offsets;
        this.arena = arena;
        validate(offsets, arena.length);
    }

    public static Builder builder() { return new Builder(16, 64); }

    public static Builder builder(int expectedRows, int expectedValues) {
        return new Builder(expectedRows, expectedValues);
    }

    public int rowCount() { return offsets.length - 1; }
    public int valueCount() { return arena.length; }
    public long payloadBytes() { return 4L * (offsets.length + arena.length); }

    public int start(int row) {
        return offsets[Objects.checkIndex(row, rowCount())];
    }

    public int end(int row) {
        int checked = Objects.checkIndex(row, rowCount());
        return offsets[checked + 1];
    }

    public int length(int row) {
        int checked = Objects.checkIndex(row, rowCount());
        return offsets[checked + 1] - offsets[checked];
    }

    public int get(int row, int ordinal) {
        int checked = Objects.checkIndex(row, rowCount());
        int start = offsets[checked];
        int length = offsets[checked + 1] - start;
        return arena[start + Objects.checkIndex(ordinal, length)];
    }

    public int valueAtAbsolute(int position) {
        return arena[Objects.checkIndex(position, arena.length)];
    }

    public int[] row(int row) {
        int checked = Objects.checkIndex(row, rowCount());
        return Arrays.copyOfRange(arena, offsets[checked], offsets[checked + 1]);
    }

    /** Exact, allocation-free comparison of one frozen row against a candidate. */
    boolean equalRow(int row, int[] candidate) {
        Objects.requireNonNull(candidate, "candidate");
        int checked = Objects.checkIndex(row, rowCount());
        int start = offsets[checked];
        int length = offsets[checked + 1] - start;
        if (length != candidate.length) return false;
        return Arrays.equals(arena, start, start + length, candidate, 0, length);
    }

    public int[] offsets() { return offsets.clone(); }
    public int[] arena() { return arena.clone(); }

    private static void validate(int[] offsets, int arenaLength) {
        if (offsets.length == 0 || offsets[0] != 0) {
            throw new IllegalArgumentException("offsets must start at zero");
        }
        int previous = 0;
        for (int offset : offsets) {
            if (offset < previous || offset > arenaLength) {
                throw new IllegalArgumentException("invalid offset arena");
            }
            previous = offset;
        }
        if (previous != arenaLength) {
            throw new IllegalArgumentException("offset arena has unaddressed tail");
        }
    }

    public static final class Builder {
        private int[] offsets;
        private int[] arena;
        private int rows;
        private int values;

        private Builder(int expectedRows, int expectedValues) {
            if (expectedRows < 0 || expectedValues < 0) {
                throw new IllegalArgumentException("negative expected capacity");
            }
            offsets = new int[Math.max(2, expectedRows + 1)];
            arena = new int[Math.max(1, expectedValues)];
        }

        public Builder addRow(int... rowValues) {
            Objects.requireNonNull(rowValues, "rowValues");
            return addRow(rowValues, 0, rowValues.length);
        }

        public Builder addRow(int[] rowValues, int offset, int length) {
            Objects.requireNonNull(rowValues, "rowValues");
            Objects.checkFromIndexSize(offset, length, rowValues.length);
            reserveRows(rows + 1);
            reserveValues(Math.addExact(values, length));
            System.arraycopy(rowValues, offset, arena, values, length);
            values += length;
            rows++;
            offsets[rows] = values;
            return this;
        }

        public int rowCount() { return rows; }
        public int valueCount() { return values; }

        public MatOffsetIntArena build() {
            return new MatOffsetIntArena(
                    Arrays.copyOf(offsets, rows + 1),
                    Arrays.copyOf(arena, values));
        }

        private void reserveRows(int requiredRows) {
            int requiredOffsets = Math.addExact(requiredRows, 1);
            if (requiredOffsets <= offsets.length) return;
            offsets = Arrays.copyOf(offsets, grow(offsets.length, requiredOffsets));
        }

        private void reserveValues(int required) {
            if (required <= arena.length) return;
            arena = Arrays.copyOf(arena, grow(arena.length, required));
        }

        private static int grow(int current, int required) {
            long grown = Math.max(required, Math.max(2L, current + (current >>> 1) + 1L));
            if (grown > Integer.MAX_VALUE - 8L) {
                throw new IllegalStateException("offset arena capacity");
            }
            return (int) grown;
        }
    }
}
