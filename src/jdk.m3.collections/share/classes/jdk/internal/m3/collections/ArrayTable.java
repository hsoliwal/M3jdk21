/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Table over a fixed list of row keys and a fixed list of column keys, stored as one dense value lane.
 *
 * <p>The cell of row {@code r} and column {@code c} is slot {@code r * columns + c} of the value lane, so a cell is
 * read and written by index in O(1) with no entry object. Two open-addressed int lanes map a key to its index; they
 * are at most half full and never change after construction. One state bit per cell tells whether the cell holds a
 * value. Every pair of a row key and a column key is a cell from the start: a cell without a value reads as null.
 * Keys are not null; values may be null.</p>
 *
 * <p>Provider comparison: Guava {@code ArrayTable} (Apache-2.0): the same results and the same refusals for the
 * operations declared here. Not thread safe. Not a {@code java.util} replacement.</p>
 */
public final class ArrayTable<R, C, V> {
    /** State-mask bit {@code PRESENT}: bit {@code i} of the state words is set while cell {@code i} holds a value. */
    static final long STATE_PRESENT = 1L << 0;
    private static final int NONE = -1;
    private static final int GOLDEN_RATIO_32 = 0x9E3779B9;
    private static final int FOLD_SHIFT = 16;

    /** Storage lane {@code rowKeyLane}: the row keys in their given order. */
    private Object[] rowKeyLane = new Object[0];
    /** Storage lane {@code columnKeyLane}: the column keys in their given order. */
    private Object[] columnKeyLane = new Object[0];
    /** Storage lane {@code valueLane}: the cell values, row after row. */
    private Object[] valueLane = new Object[0];
    /** State words (PRESENT): one bit per cell, 64 cells per word. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count: the cells that hold a value. */
    private int size;
    /** Open-addressed table of row index + 1; 0 is empty; at most half full. */
    private int[] rowIndexLane = new int[1];
    /** Open-addressed table of column index + 1; 0 is empty; at most half full. */
    private int[] columnIndexLane = new int[1];
    private List<R> rowKeys = List.of();
    private List<C> columnKeys = List.of();

    /** Creates a table without keys: it has no cell. */
    public ArrayTable() {
    }

    /**
     * Creates a table whose cells all read as null.
     *
     * @param rowKeys the row keys, in order, without null and without duplicates
     * @param columnKeys the column keys, in order, without null and without duplicates
     * @throws IllegalArgumentException if a key is repeated or exactly one of the two key lists is empty
     */
    public ArrayTable(Iterable<? extends R> rowKeys, Iterable<? extends C> columnKeys) {
        rowKeyLane = keys(rowKeys);
        columnKeyLane = keys(columnKeys);
        if ((rowKeyLane.length == 0) != (columnKeyLane.length == 0)) {
            throw new IllegalArgumentException("exactly one of the key lists is empty");
        }
        rowIndexLane = index(rowKeyLane);
        columnIndexLane = index(columnKeyLane);
        valueLane = new Object[Math.multiplyExact(rowKeyLane.length, columnKeyLane.length)];
        stateMaskWords = new long[(valueLane.length + Long.SIZE - 1) / Long.SIZE];
        this.rowKeys = view(rowKeyLane);
        this.columnKeys = view(columnKeyLane);
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "ArrayTable|ARRAY_TABLE|OBJECT|PRESENT|Object[] rowKeyLane;Object[] columnKeyLane;Object[] valueLane";
    }

    /** The value of the cell at a row index and a column index; {@code IndexOutOfBoundsException} outside the table. */
    @SuppressWarnings("unchecked")
    public V at(int rowIndex, int columnIndex) {
        return (V) valueLane[cell(rowIndex, columnIndex)];
    }

    /** Writes the cell at a row index and a column index and returns the value it held, or null. */
    @SuppressWarnings("unchecked")
    public V set(int rowIndex, int columnIndex, V value) {
        int cell = cell(rowIndex, columnIndex);
        V old = (V) valueLane[cell];
        valueLane[cell] = value;
        if ((old == null) != (value == null)) {
            stateMaskWords[cell / Long.SIZE] ^= STATE_PRESENT << cell;
            size += value == null ? -1 : 1;
        }
        return old;
    }

    /** The value of the cell of a row key and a column key, or null when a key is not in the table. */
    public V get(Object rowKey, Object columnKey) {
        int row = find(rowKeyLane, rowIndexLane, rowKey);
        int column = find(columnKeyLane, columnIndexLane, columnKey);
        return row == NONE || column == NONE ? null : at(row, column);
    }

    /**
     * Writes the cell of a row key and a column key and returns the value it held, or null. A key that is not in the
     * table is refused with {@code IllegalArgumentException}; a null key with {@code NullPointerException}.
     */
    public V put(R rowKey, C columnKey, V value) {
        Objects.requireNonNull(rowKey);
        Objects.requireNonNull(columnKey);
        int row = find(rowKeyLane, rowIndexLane, rowKey);
        if (row == NONE) {
            throw new IllegalArgumentException("Row " + rowKey + " not in " + rowKeys);
        }
        int column = find(columnKeyLane, columnIndexLane, columnKey);
        if (column == NONE) {
            throw new IllegalArgumentException("Column " + columnKey + " not in " + columnKeys);
        }
        return set(row, column, value);
    }

    /** Writes null into the cell of two keys and returns the value it held; null when a key is not in the table. */
    public V erase(Object rowKey, Object columnKey) {
        int row = find(rowKeyLane, rowIndexLane, rowKey);
        int column = find(columnKeyLane, columnIndexLane, columnKey);
        return row == NONE || column == NONE ? null : set(row, column, null);
    }

    /** Tells whether both keys are in the table; the value of the cell does not matter. */
    public boolean contains(Object rowKey, Object columnKey) {
        return find(rowKeyLane, rowIndexLane, rowKey) != NONE && find(columnKeyLane, columnIndexLane, columnKey) != NONE;
    }

    /** The row keys in order, as a list that cannot be changed. */
    public List<R> rowKeyList() {
        return rowKeys;
    }

    /** The column keys in order, as a list that cannot be changed. */
    public List<C> columnKeyList() {
        return columnKeys;
    }

    /** The number of cells: rows times columns. */
    public int size() {
        return rowKeyLane.length * columnKeyLane.length;
    }

    private int cell(int rowIndex, int columnIndex) {
        Objects.checkIndex(rowIndex, rowKeyLane.length);
        Objects.checkIndex(columnIndex, columnKeyLane.length);
        return rowIndex * columnKeyLane.length + columnIndex;
    }

    private static Object[] keys(Iterable<?> given) {
        List<Object> keys = new ArrayList<>();
        given.forEach(key -> keys.add(Objects.requireNonNull(key)));
        return keys.toArray();
    }

    @SuppressWarnings("unchecked")
    private static <K> List<K> view(Object[] lane) {
        return Collections.unmodifiableList(Arrays.asList((K[]) lane));
    }

    private static int[] index(Object[] keys) {
        int[] table = new int[Integer.highestOneBit(Math.max(1, keys.length) << 1) << 1];
        for (int slot = 0; slot < keys.length; slot++) {
            int position = probe(keys, table, keys[slot]);
            if (table[position] != 0) {
                throw new IllegalArgumentException("Multiple entries with same key: " + keys[slot]);
            }
            table[position] = slot + 1;
        }
        return table;
    }

    private static int find(Object[] keys, int[] table, Object key) {
        return key == null ? NONE : table[probe(keys, table, key)] - 1;
    }

    /** The table position that holds the key, or the empty position where its probe run ends. */
    private static int probe(Object[] keys, int[] table, Object key) {
        int mask = table.length - 1;
        int hash = key.hashCode() * GOLDEN_RATIO_32;
        int position = (hash ^ (hash >>> FOLD_SHIFT)) & mask;
        while (table[position] != 0 && !key.equals(keys[table[position] - 1])) {
            position = (position + 1) & mask;
        }
        return position;
    }
}
