/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Dense primitive-long table with an insertion-ordered row and column universe.
 *
 * <p>Two {@link PackedLongRowIndex} instances index the canonical row/column
 * key lanes. Values and presence bits live in the table's row-major primitive
 * arrays, never in per-cell objects. Indexed access is O(1); key access is
 * expected O(1), subject to the primitive indexes' collision behavior.
 *
 * <p>The fixed-universe overload models the Guava ArrayTable key contract:
 * every in-universe pair exists, including cells that have never been set.
 * An unset long cell reads as zero. The original no-arg M3 constructor is an
 * additive growing-universe variant: the first put of an unseen row or column
 * registers that key. The logical size is always rows times columns.
 * This class is not thread-safe and does not retain an Entry/Cell per slot.
 */
public final class LongArrayTable {
    /** State-mask bit {@code PRESENT} (per-slot state word, bit 0). */
    static final long STATE_PRESENT = 1L << 0;

    private long[] rowKeyLane = new long[0];
    private long[] columnKeyLane = new long[0];
    private long[] valueLane = new long[0];
    private long[] stateMaskWords = new long[0];
    private int size;

    private final PackedLongRowIndex rowIndex = new PackedLongRowIndex(0);
    private final PackedLongRowIndex columnIndex = new PackedLongRowIndex(0);
    private final boolean fixedUniverse;
    private int rowCount;
    private int columnCount;
    private int rowCapacity;
    private int columnCapacity;

    /** Constructs the original M3 growing-universe table. */
    public LongArrayTable() {
        fixedUniverse = false;
    }

    /**
     * Constructs a fixed-universe table with independent snapshots of the
     * supplied row and column key arrays. Duplicate keys are rejected.
     * No boxed keys, cells, or entries are retained.
     */
    public LongArrayTable(long[] rowKeys, long[] columnKeys) {
        Objects.requireNonNull(rowKeys, "rowKeys");
        Objects.requireNonNull(columnKeys, "columnKeys");
        int cells = Math.multiplyExact(rowKeys.length, columnKeys.length);
        rowKeyLane = rowKeys.clone();
        columnKeyLane = columnKeys.clone();
        rowIndex.ensure(rowKeys.length, rowKeyLane);
        columnIndex.ensure(columnKeys.length, columnKeyLane);
        for (int i = 0; i < rowKeys.length; i++) {
            if (rowIndex.find(rowKeyLane[i], rowKeyLane) >= 0)
                throw new IllegalArgumentException("duplicate row key: " + rowKeyLane[i]);
            rowIndex.insert(i, rowKeyLane);
        }
        for (int i = 0; i < columnKeys.length; i++) {
            if (columnIndex.find(columnKeyLane[i], columnKeyLane) >= 0)
                throw new IllegalArgumentException("duplicate column key: " + columnKeyLane[i]);
            columnIndex.insert(i, columnKeyLane);
        }
        valueLane = new long[cells];
        stateMaskWords = new long[maskLength(cells)];
        rowCount = rowCapacity = rowKeys.length;
        columnCount = columnCapacity = columnKeys.length;
        size = cells;
        fixedUniverse = true;
    }

    /** Preserves the historical scaffold descriptor as an API contract. */
    public static String scaffoldDescriptor() {
        return "M3LongArrayTable|LONG_ARRAY_TABLE|LONG|PRESENT|long[] rowKeyLane;long[] columnKeyLane;long[] valueLane";
    }

    /** Returns a cell by registered zero-based row and column index. */
    public long at(int rowIndex, int columnIndex) {
        return valueLane[slot(Objects.checkIndex(rowIndex, rowCount),
                Objects.checkIndex(columnIndex, columnCount))];
    }

    /** Sets a registered cell, returning its previous long value. */
    public long set(int rowIndex, int columnIndex, long value) {
        int slot = slot(Objects.checkIndex(rowIndex, rowCount),
                Objects.checkIndex(columnIndex, columnCount));
        long old = valueLane[slot];
        valueLane[slot] = value;
        stateMaskWords[slot >>> 6] |= 1L << (slot & 63);
        return old;
    }

    /**
     * Returns the cell's value, or the fallback only for an unknown row or
     * column key. A valid but never-written cell is present and reads zero.
     */
    public long getOrDefault(long rowKey, long columnKey, long defaultValue) {
        int row = rowIndex.find(rowKey, rowKeyLane);
        int column = columnIndex.find(columnKey, columnKeyLane);
        return row < 0 || column < 0 ? defaultValue : valueLane[slot(row, column)];
    }

    /**
     * Sets the value at a key pair and returns the previous long value.
     * Fixed tables reject unknown keys. Growing M3 tables register them.
     */
    public long put(long rowKey, long columnKey, long value) {
        int row = rowIndex.find(rowKey, rowKeyLane);
        int column = columnIndex.find(columnKey, columnKeyLane);
        if (row < 0 || column < 0) {
            if (fixedUniverse)
                throw new IllegalArgumentException("key outside the fixed row/column universe");
            expandAndRegister(rowKey, columnKey, row < 0, column < 0);
            row = rowIndex.find(rowKey, rowKeyLane);
            column = columnIndex.find(columnKey, columnKeyLane);
        }
        return set(row, column, value);
    }

    /** True for every registered row/column pair, including zero-valued cells. */
    public boolean contains(long rowKey, long columnKey) {
        return rowIndex.find(rowKey, rowKeyLane) >= 0
                && columnIndex.find(columnKey, columnKeyLane) >= 0;
    }

    /** Independent snapshot of row keys in registration order. */
    public long[] rowKeysToArray() {
        return Arrays.copyOf(rowKeyLane, rowCount);
    }

    /** Independent snapshot of column keys in registration order. */
    public long[] columnKeysToArray() {
        return Arrays.copyOf(columnKeyLane, columnCount);
    }

    /** Number of logical cells, including cells whose value has never been set. */
    public int size() {
        return size;
    }

    private int slot(int row, int column) {
        return row * columnCapacity + column;
    }

    private static int maskLength(long cells) {
        return Math.toIntExact((cells + 63L) >>> 6);
    }

    private static boolean present(long[] words, int index) {
        return (words[index >>> 6] & (1L << (index & 63))) != 0;
    }

    /**
     * Expands the physical matrix geometrically so adding many adjacent columns
     * does not copy the entire matrix on every insertion. Existing keys and
     * PRESENT bits are copied to the new row-major stride before publication.
     */
    private void expandAndRegister(long rowKey, long columnKey,
            boolean addRow, boolean addColumn) {
        int neededRows = Math.addExact(rowCount, addRow ? 1 : 0);
        int neededColumns = Math.addExact(columnCount, addColumn ? 1 : 0);
        int logicalCells = Math.multiplyExact(neededRows, neededColumns);

        int nextRowCapacity = neededRows > rowCapacity
                ? PackedLongRowIndex.grownLength(rowCapacity, neededRows) : rowCapacity;
        int nextColumnCapacity = neededColumns > columnCapacity
                ? PackedLongRowIndex.grownLength(columnCapacity, neededColumns) : columnCapacity;
        long physicalCells = (long) nextRowCapacity * nextColumnCapacity;
        if (physicalCells > Integer.MAX_VALUE - 8L) {
            nextRowCapacity = neededRows;
            nextColumnCapacity = neededColumns;
            physicalCells = (long) neededRows * neededColumns;
        }
        if (physicalCells > Integer.MAX_VALUE - 8L)
            throw new IllegalArgumentException("table capacity overflow");

        long[] newRows = nextRowCapacity == rowCapacity ? rowKeyLane
                : Arrays.copyOf(rowKeyLane, nextRowCapacity);
        long[] newColumns = nextColumnCapacity == columnCapacity ? columnKeyLane
                : Arrays.copyOf(columnKeyLane, nextColumnCapacity);
        long[] newValues = valueLane;
        long[] newMaskWords = stateMaskWords;
        if (nextRowCapacity != rowCapacity || nextColumnCapacity != columnCapacity) {
            newValues = new long[(int) physicalCells];
            newMaskWords = new long[maskLength(physicalCells)];
            for (int row = 0; row < rowCount; row++) {
                int oldBase = row * columnCapacity;
                int newBase = row * nextColumnCapacity;
                System.arraycopy(valueLane, oldBase, newValues, newBase, columnCount);
                for (int column = 0; column < columnCount; column++) {
                    if (present(stateMaskWords, oldBase + column)) {
                        int cell = newBase + column;
                        newMaskWords[cell >>> 6] |= 1L << (cell & 63);
                    }
                }
            }
        }

        // Reserve both indexes before publishing new matrix state. These two
        // operations can rehash existing ordinals but do not change key sets.
        if (addRow) rowIndex.ensure(neededRows, rowKeyLane);
        if (addColumn) columnIndex.ensure(neededColumns, columnKeyLane);
        rowKeyLane = newRows;
        columnKeyLane = newColumns;
        valueLane = newValues;
        stateMaskWords = newMaskWords;
        rowCapacity = nextRowCapacity;
        columnCapacity = nextColumnCapacity;
        if (addRow) {
            rowKeyLane[rowCount] = rowKey;
            rowIndex.insert(rowCount, rowKeyLane);
            rowCount++;
        }
        if (addColumn) {
            columnKeyLane[columnCount] = columnKey;
            columnIndex.insert(columnCount, columnKeyLane);
            columnCount++;
        }
        size = logicalCells;
    }
}
