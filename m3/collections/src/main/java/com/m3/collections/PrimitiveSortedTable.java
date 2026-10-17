/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Primitive row/column/value table stored as three aligned sorted primitive lanes.
 *
 * <p>Cells are ordered lexicographically by row then column. There is no nested
 * Map-of-Map graph and no Cell/Entry object retained per mapping.
 */
public final class PrimitiveSortedTable {
    @FunctionalInterface
    public interface CellConsumer {
        void accept(long rowBits, long columnBits, long valueBits);
    }

    @FunctionalInterface
    public interface KeyValueConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final PrimitiveKind rowKind;
    private final PrimitiveKind columnKind;
    private final PrimitiveKind valueKind;
    private Object rows;
    private Object columns;
    private Object values;
    private int size;

    public PrimitiveSortedTable(
            PrimitiveKind rowKind, PrimitiveKind columnKind, PrimitiveKind valueKind) {
        this(rowKind, columnKind, valueKind, 8);
    }

    public PrimitiveSortedTable(
            PrimitiveKind rowKind, PrimitiveKind columnKind,
            PrimitiveKind valueKind, int initialCapacity) {
        this.rowKind = Objects.requireNonNull(rowKind, "rowKind");
        this.columnKind = Objects.requireNonNull(columnKind, "columnKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        int capacity = Math.max(1, initialCapacity);
        rows = rowKind.newArray(capacity);
        columns = columnKind.newArray(capacity);
        values = valueKind.newArray(capacity);
    }

    public PrimitiveKind rowKind() { return rowKind; }
    public PrimitiveKind columnKind() { return columnKind; }
    public PrimitiveKind valueKind() { return valueKind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(rows); }
    public long payloadBytes() {
        return rowKind.payloadBytes(capacity())
                + columnKind.payloadBytes(capacity())
                + valueKind.payloadBytes(capacity());
    }

    public boolean containsCellBits(long rowBits, long columnBits) {
        return search(rowBits, columnBits) >= 0;
    }

    public long getOrDefaultBits(
            long rowBits, long columnBits, long defaultValueBits) {
        int index = search(rowBits, columnBits);
        return index < 0 ? defaultValueBits : valueKind.readBits(values, index);
    }

    public long putBits(
            long rowBits, long columnBits, long valueBits, long absentValueBits) {
        int result = search(rowBits, columnBits);
        if (result >= 0) {
            long old = valueKind.readBits(values, result);
            valueKind.writeBits(values, result, valueBits);
            return old;
        }

        int insertion = -result - 1;
        ensure(size + 1);
        int moved = size - insertion;
        if (moved > 0) {
            System.arraycopy(rows, insertion, rows, insertion + 1, moved);
            System.arraycopy(columns, insertion, columns, insertion + 1, moved);
            System.arraycopy(values, insertion, values, insertion + 1, moved);
        }
        rowKind.writeBits(rows, insertion, rowBits);
        columnKind.writeBits(columns, insertion, columnBits);
        valueKind.writeBits(values, insertion, valueBits);
        size++;
        return absentValueBits;
    }

    public long removeOrDefaultBits(
            long rowBits, long columnBits, long defaultValueBits) {
        int index = search(rowBits, columnBits);
        if (index < 0) return defaultValueBits;
        long old = valueKind.readBits(values, index);
        removeAt(index);
        return old;
    }

    public int removeRowBits(long rowBits) {
        int from = lowerBoundRow(rowBits);
        int to = upperBoundRow(rowBits);
        int removed = to - from;
        if (removed == 0) return 0;
        removeRange(from, to);
        return removed;
    }

    public boolean containsRowBits(long rowBits) {
        return lowerBoundRow(rowBits) < upperBoundRow(rowBits);
    }

    public boolean containsColumnBits(long columnBits) {
        for (int i = 0; i < size; i++) {
            if (columnKind.equalBits(columnKind.readBits(columns, i), columnBits)) {
                return true;
            }
        }
        return false;
    }

    public int countRowBits(long rowBits) {
        return upperBoundRow(rowBits) - lowerBoundRow(rowBits);
    }

    public int countColumnBits(long columnBits) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (columnKind.equalBits(columnKind.readBits(columns, i), columnBits)) count++;
        }
        return count;
    }

    public int removeColumnBits(long columnBits) {
        int write = 0;
        int removed = 0;
        for (int read = 0; read < size; read++) {
            if (columnKind.equalBits(columnKind.readBits(columns, read), columnBits)) {
                removed++;
                continue;
            }
            if (write != read) {
                rowKind.writeBits(rows, write, rowKind.readBits(rows, read));
                columnKind.writeBits(columns, write, columnKind.readBits(columns, read));
                valueKind.writeBits(values, write, valueKind.readBits(values, read));
            }
            write++;
        }
        if (removed > 0) {
            rowKind.clear(rows, write, size);
            columnKind.clear(columns, write, size);
            valueKind.clear(values, write, size);
            size = write;
        }
        return removed;
    }

    public void forEachRowBits(long rowBits, KeyValueConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int from = lowerBoundRow(rowBits);
        int to = upperBoundRow(rowBits);
        for (int i = from; i < to; i++) {
            consumer.accept(
                    columnKind.readBits(columns, i),
                    valueKind.readBits(values, i));
        }
    }

    public void forEachColumnBits(long columnBits, KeyValueConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) {
            if (columnKind.equalBits(columnKind.readBits(columns, i), columnBits)) {
                consumer.accept(
                        rowKind.readBits(rows, i),
                        valueKind.readBits(values, i));
            }
        }
    }

    public void forEach(CellConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) {
            consumer.accept(
                    rowKind.readBits(rows, i),
                    columnKind.readBits(columns, i),
                    valueKind.readBits(values, i));
        }
    }

    public long rowBitsAt(int index) {
        return rowKind.readBits(rows, check(index));
    }

    public long columnBitsAt(int index) {
        return columnKind.readBits(columns, check(index));
    }

    public long valueBitsAt(int index) {
        return valueKind.readBits(values, check(index));
    }

    public void clear() {
        rowKind.clear(rows, 0, size);
        columnKind.clear(columns, 0, size);
        valueKind.clear(values, 0, size);
        size = 0;
    }

    private int search(long rowBits, long columnBits) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int comparison = compareCell(mid, rowBits, columnBits);
            if (comparison < 0) low = mid + 1;
            else if (comparison > 0) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    private int compareCell(int index, long rowBits, long columnBits) {
        int rowOrder = rowKind.compareBits(rowKind.readBits(rows, index), rowBits);
        if (rowOrder != 0) return rowOrder;
        return columnKind.compareBits(
                columnKind.readBits(columns, index), columnBits);
    }

    private int lowerBoundRow(long rowBits) {
        int low = 0;
        int high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (rowKind.compareBits(rowKind.readBits(rows, mid), rowBits) < 0) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private int upperBoundRow(long rowBits) {
        int low = 0;
        int high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (rowKind.compareBits(rowKind.readBits(rows, mid), rowBits) <= 0) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private void removeAt(int index) {
        removeRange(index, index + 1);
    }

    private void removeRange(int from, int to) {
        int removed = to - from;
        int moved = size - to;
        if (moved > 0) {
            System.arraycopy(rows, to, rows, from, moved);
            System.arraycopy(columns, to, columns, from, moved);
            System.arraycopy(values, to, values, from, moved);
        }
        int newSize = size - removed;
        rowKind.clear(rows, newSize, size);
        columnKind.clear(columns, newSize, size);
        valueKind.clear(values, newSize, size);
        size = newSize;
    }

    private int check(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return index;
    }

    private void ensure(int required) {
        if (required <= capacity()) return;
        int next = capacity() + (capacity() >>> 1) + 1;
        if (next < required) next = required;
        rows = rowKind.copyOf(rows, next);
        columns = columnKind.copyOf(columns, next);
        values = valueKind.copyOf(values, next);
    }
}
