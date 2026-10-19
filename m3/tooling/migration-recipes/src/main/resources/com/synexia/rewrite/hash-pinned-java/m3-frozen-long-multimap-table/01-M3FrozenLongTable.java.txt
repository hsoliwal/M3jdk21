/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable {@code (row, column) -> value} table of {@code long}s in compressed-sparse-row (CSR) form: sorted distinct
 * row lane, row offset lane of length {@code rowCount + 1}, and parallel column/value lanes sorted by column within
 * each row.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET, leaf LS3):
 * <ul>
 *   <li>GUAVA {@code ImmutableTable} built with natural row and column ordering: {@code size}, {@code contains},
 *       {@code get}, {@code rowKeySet().size()}, cell iteration order, {@code hashCode} = {@code cellSet().hashCode()}
 *       (sum of {@code Arrays.hashCode(row, column, value)}), {@code toString} = {@code rowMap().toString()}, duplicate
 *       cells refused with {@link IllegalArgumentException}, mutators throw {@link UnsupportedOperationException}.</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; {@code rowLane}, {@code rowOffsetLane} (row ordinal {@code r} owns cell offsets
 * {@code [rowOffsetLane[r], rowOffsetLane[r + 1])}), {@code columnLane} and {@code valueLane}, plus the shared FROZEN
 * owner state word. Stored nodes: NONE (no Table.Cell, no nested maps). Per-element allocation: 0 for queries and
 * ordinal access. Build has no boxing: sort + dedupe rows, count, prefix-sum, stable scatter, then an in-place
 * insertion sort of each row by column (O(k^2) per row of k cells; rows are expected short). Donor: first-party Synexia {@code foss/FrozenLongTable} (blob e02549f8b734) and
 * {@code PackedLongLongTable}, adapted. Not a {@code java.util} replacement.
 */
public final class M3FrozenLongTable {
    /** Owner state bit {@code FROZEN}: every slot of all lanes is immutable. */
    static final long STATE_FROZEN = 1L << 0;
    private static final long[] EMPTY_LANE = new long[0];
    private static final int[] EMPTY_OFFSETS = {0};
    private static final long[] FROZEN_STATE = {STATE_FROZEN};
    private static final M3FrozenLongTable EMPTY = new M3FrozenLongTable();

    /** Storage lane {@code rowLane}: sorted distinct row keys. */
    private final long[] rowLane;
    /** Storage lane {@code rowOffsetLane}: CSR offsets, length rowCount + 1. */
    private final int[] rowOffsetLane;
    /** Storage lane {@code columnLane}: column of each cell, ascending within a row. */
    private final long[] columnLane;
    /** Storage lane {@code valueLane}: value of each cell. */
    private final long[] valueLane;
    /** Owner state word (FROZEN); shared, never mutated. */
    private final long[] stateMaskWords;
    /** Live row slot count. */
    private final int size;

    /** Creates an empty frozen table. */
    public M3FrozenLongTable() {
        this(EMPTY_LANE, EMPTY_OFFSETS, EMPTY_LANE, EMPTY_LANE, 0);
    }

    private M3FrozenLongTable(long[] rowLane, int[] rowOffsetLane, long[] columnLane, long[] valueLane, int size) {
        this.rowLane = rowLane;
        this.rowOffsetLane = rowOffsetLane;
        this.columnLane = columnLane;
        this.valueLane = valueLane;
        this.stateMaskWords = FROZEN_STATE;
        this.size = size;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongTable|FROZEN_LONG_TABLE|LONG|FROZEN|long[] rowLane;int[] rowOffsetLane;long[] columnLane;long[] valueLane|RECEIVED";
    }

    /** Guava ImmutableTable (natural row/column order) from parallel cells; duplicate cells are refused. */
    public static M3FrozenLongTable copyOf(long[] rows, long[] columns, long[] values) {
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(values, "values");
        if (rows.length != columns.length || rows.length != values.length) {
            throw new IllegalArgumentException("rows, columns and values differ in length");
        }
        int n = rows.length;
        if (n == 0) return EMPTY;
        long[] distinct = rows.clone();
        Arrays.sort(distinct);
        int rowCount = 1;
        for (int i = 1; i < n; i++) {
            if (distinct[i] != distinct[rowCount - 1]) distinct[rowCount++] = distinct[i];
        }
        int[] offsets = new int[rowCount + 1];
        for (int i = 0; i < n; i++) offsets[Arrays.binarySearch(distinct, 0, rowCount, rows[i]) + 1]++;
        for (int r = 0; r < rowCount; r++) offsets[r + 1] += offsets[r];
        int[] cursor = Arrays.copyOf(offsets, rowCount);
        long[] columnLane = new long[n];
        long[] valueLane = new long[n];
        for (int i = 0; i < n; i++) {
            int slot = cursor[Arrays.binarySearch(distinct, 0, rowCount, rows[i])]++;
            columnLane[slot] = columns[i];
            valueLane[slot] = values[i];
        }
        for (int r = 0; r < rowCount; r++) sortRow(columnLane, valueLane, offsets[r], offsets[r + 1], distinct[r]);
        return new M3FrozenLongTable(rowCount == n ? distinct : Arrays.copyOf(distinct, rowCount), offsets, columnLane, valueLane, rowCount);
    }

    /** Insertion sort of one row's cells by column (rows are short); refuses duplicate columns. */
    private static void sortRow(long[] columns, long[] values, int from, int to, long row) {
        for (int i = from + 1; i < to; i++) {
            long column = columns[i];
            long value = values[i];
            int j = i - 1;
            while (j >= from && columns[j] > column) {
                columns[j + 1] = columns[j];
                values[j + 1] = values[j];
                j--;
            }
            if (j >= from && columns[j] == column) throw new IllegalArgumentException("duplicate cell: " + row + "," + column);
            columns[j + 1] = column;
            values[j + 1] = value;
        }
    }

    /** Table.size: number of cells. */
    public int size() {
        return rowOffsetLane[size];
    }

    /** Table.contains. */
    public boolean contains(long row, long column) {
        return cell(row, column) >= 0;
    }

    /** Table.get with a default for absent cells. */
    public long getOrDefault(long row, long column, long defaultValue) {
        int cell = cell(row, column);
        return cell >= 0 ? valueLane[cell] : defaultValue;
    }

    /** Table.rowKeySet().size. */
    public int rowCount() {
        return size;
    }

    /** Row key at the ascending row ordinal. */
    public long rowAt(int rowOrdinal) {
        return rowLane[Objects.checkIndex(rowOrdinal, size)];
    }

    /** CSR offset of the row ordinal; {@code rowOffsetAt(rowCount())} is the cell count. */
    public int rowOffsetAt(int rowOrdinal) {
        return rowOffsetLane[Objects.checkIndex(rowOrdinal, size + 1)];
    }

    /** Column of the cell at the CSR cell ordinal. */
    public long columnAt(int cellOrdinal) {
        return columnLane[Objects.checkIndex(cellOrdinal, rowOffsetLane[size])];
    }

    /** Value of the cell at the CSR cell ordinal. */
    public long valueAt(int cellOrdinal) {
        return valueLane[Objects.checkIndex(cellOrdinal, rowOffsetLane[size])];
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public long put(long row, long column, long value) {
        throw new UnsupportedOperationException("M3FrozenLongTable is immutable");
    }

    /** Immutable: always throws {@link UnsupportedOperationException}. */
    public long remove(long row, long column) {
        throw new UnsupportedOperationException("M3FrozenLongTable is immutable");
    }

    private int cell(long row, long column) {
        int r = Arrays.binarySearch(rowLane, 0, size, row);
        if (r < 0) return -1;
        int c = Arrays.binarySearch(columnLane, rowOffsetLane[r], rowOffsetLane[r + 1], column);
        return c >= 0 ? c : -1;
    }

    /** Equal to another frozen table with the same cells. */
    @Override public boolean equals(Object other) {
        return other == this || other instanceof M3FrozenLongTable table
                && Arrays.equals(rowLane, 0, size, table.rowLane, 0, table.size)
                && Arrays.equals(rowOffsetLane, 0, size + 1, table.rowOffsetLane, 0, table.size + 1)
                && Arrays.equals(columnLane, 0, size(), table.columnLane, 0, table.size())
                && Arrays.equals(valueLane, 0, size(), table.valueLane, 0, table.size());
    }

    /** Table hash: sum over cells of {@code Arrays.hashCode(row, column, value)} (Guava Cell contract). */
    @Override public int hashCode() {
        int hash = 0;
        for (int r = 0; r < size; r++) {
            int rowHash = 31 + Long.hashCode(rowLane[r]);
            for (int i = rowOffsetLane[r]; i < rowOffsetLane[r + 1]; i++) {
                hash += 31 * (31 * rowHash + Long.hashCode(columnLane[i])) + Long.hashCode(valueLane[i]);
            }
        }
        return hash;
    }

    /** {@code {r={c=v, ...}, ...}} rendering (Guava {@code rowMap().toString()} with natural ordering). */
    @Override public String toString() {
        StringBuilder text = new StringBuilder(size * 12 + 2).append('{');
        for (int r = 0; r < size; r++) {
            if (r > 0) text.append(", ");
            text.append(rowLane[r]).append("={");
            for (int i = rowOffsetLane[r]; i < rowOffsetLane[r + 1]; i++) {
                if (i > rowOffsetLane[r]) text.append(", ");
                text.append(columnLane[i]).append('=').append(valueLane[i]);
            }
            text.append('}');
        }
        return text.append('}').toString();
    }

    int stateWord() {
        return (int) stateMaskWords[0];
    }
}
