/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable full-width long table with packed CSR cell lanes.
 *
 * <p>Rows retain first encounter order, as in the existing primitive multimap
 * owner. Each row's columns are sorted in signed order as required by this
 * owner's CSR contract. The existing stable primitive permutation, ordinal
 * index and bisection owners provide construction and lookup; no boxed key or
 * cell object is retained. Duplicate coordinates are rejected even when their
 * values match. All input arrays are copied before construction.
 */
public final class FrozenLongTable {
    static final long STATE_FROZEN = 1L;
    private final long[] rowLane;
    private final int[] rowOffsetLane;
    private final long[] columnLane;
    private final long[] valueLane;
    private final long[] stateMaskWords;
    private final PackedLongRowIndex rowOrdinals;
    private final int size;

    /** Creates a valid immutable empty table, including its terminal offset. */
    public FrozenLongTable() {
        this(new long[0], new int[] {0}, new long[0], new long[0],
                new PackedLongRowIndex(0));
    }

    private FrozenLongTable(long[] rows, int[] offsets, long[] columns,
            long[] values, PackedLongRowIndex ordinals) {
        rowLane = rows;
        rowOffsetLane = offsets;
        columnLane = columns;
        valueLane = values;
        rowOrdinals = ordinals;
        size = values.length;
        stateMaskWords = new long[(int) (((long) size + 63) >>> 6)];
        Arrays.fill(stateMaskWords, -1L);
        if ((size & 63) != 0) {
            stateMaskWords[stateMaskWords.length - 1] = (1L << (size & 63)) - 1;
        }
    }

    /** Retained compatibility descriptor; it does not describe admission status. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongTable|FROZEN_LONG_TABLE|LONG|FROZEN|long[] rowLane;int[] rowOffsetLane;long[] columnLane;long[] valueLane";
    }

    /** Copies parallel cell lanes; construction is unpublished until validated. */
    public static FrozenLongTable copyOf(long[] rows, long[] columns, long[] values) {
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(values, "values");
        if (rows.length != columns.length || rows.length != values.length) {
            throw new IllegalArgumentException("cell lane lengths differ");
        }
        if (rows.length == 0) { return new FrozenLongTable(); }
        long[] sourceRows = rows.clone();
        long[] sourceColumns = columns.clone();
        long[] sourceValues = values.clone();
        // Large, low-row-cardinality tables reserve compact primitive scratch.
        // Grow through 32/64/128/256 keys before the original full capacity.
        int scratchLength = rows.length >= 4_096 ? 32 : rows.length;
        long[] encountered = new long[scratchLength];
        long[] cellRows = new long[rows.length];
        PackedLongRowIndex ordinals = new PackedLongRowIndex(0);
        int distinct = 0;
        for (int cell = 0; cell < sourceRows.length; cell++) {
            long key = sourceRows[cell];
            int ordinal = ordinals.find(key, encountered);
            if (ordinal < 0) {
                if (distinct == encountered.length) {
                    int next = grownScratchLength(encountered.length, rows.length);
                    encountered = Arrays.copyOf(encountered, next);
                }
                ordinals.ensure(distinct + 1, encountered);
                ordinal = distinct++;
                encountered[ordinal] = key;
                ordinals.insert(ordinal, encountered);
            }
            cellRows[cell] = ordinal;
        }
        if (distinct == Integer.MAX_VALUE) {
            throw new OutOfMemoryError("FrozenLongTable offset capacity exhausted");
        }
        int[] order = LongKeyOrder.lexicographic(cellRows, sourceColumns);
        int[] offsets = new int[distinct + 1];
        long[] packedColumns = new long[order.length];
        long[] packedValues = new long[order.length];
        int previousRow = -1;
        for (int cell = 0; cell < order.length; cell++) {
            int source = order[cell];
            int row = (int) cellRows[source];
            long column = sourceColumns[source];
            if (row != previousRow) {
                offsets[row] = cell;
                previousRow = row;
            } else if (packedColumns[cell - 1] == column) {
                throw new IllegalArgumentException("duplicate table coordinate");
            }
            packedColumns[cell] = column;
            packedValues[cell] = sourceValues[source];
        }
        offsets[distinct] = order.length;
        // The indexed row lane is private and immutable after construction.
        return new FrozenLongTable(distinct == encountered.length
                ? encountered : Arrays.copyOf(encountered, distinct), offsets,
                packedColumns, packedValues, ordinals);
    }

    /** Grow a private encounter lane up to the already bounded input length. */
    private static int grownScratchLength(int length, int inputLength) {
        return length < 256 ? Math.min(inputLength, length << 1) : inputLength;
    }

    public int size() { return size; }
    public int rowCount() { return rowLane.length; }

    public boolean contains(long row, long column) { return cell(row, column) >= 0; }

    public long getOrDefault(long row, long column, long defaultValue) {
        int cell = cell(row, column);
        return cell < 0 ? defaultValue : valueLane[cell];
    }

    private int cell(long row, long column) {
        int ordinal = rowOrdinals.find(row, rowLane);
        if (ordinal < 0) { return -1; }
        int end = rowOffsetLane[ordinal + 1];
        int at = LaneSearch.lowerBound(columnLane, rowOffsetLane[ordinal], end, column);
        return at < end && columnLane[at] == column ? at : -1;
    }

    public long rowAt(int rowOrdinal) {
        return rowLane[Objects.checkIndex(rowOrdinal, rowLane.length)];
    }

    /** Includes the terminal boundary at {@code rowCount()}, equal to size. */
    public int rowOffsetAt(int rowOrdinal) {
        return rowOffsetLane[Objects.checkIndex(rowOrdinal, rowOffsetLane.length)];
    }

    public long columnAt(int cellOrdinal) {
        return columnLane[Objects.checkIndex(cellOrdinal, columnLane.length)];
    }

    public long valueAt(int cellOrdinal) {
        return valueLane[Objects.checkIndex(cellOrdinal, valueLane.length)];
    }

    public long put(long row, long column, long value) {
        throw new UnsupportedOperationException("immutable table");
    }

    public long remove(long row, long column) {
        throw new UnsupportedOperationException("immutable table");
    }
}
