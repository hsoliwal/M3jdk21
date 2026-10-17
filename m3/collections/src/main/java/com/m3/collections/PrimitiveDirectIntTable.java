/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Dense fixed-universe int-row/int-column primitive table.
 *
 * <p>Cells are flattened into one exact-width primitive lane and a presence
 * bitset. Row/column keys are implicit indexes, so no key objects or key lanes
 * are retained.
 */
public final class PrimitiveDirectIntTable {
    @FunctionalInterface
    public interface CellConsumer {
        void accept(int row, int column, long valueBits);
    }

    private final PrimitiveKind valueKind;
    private final int rowCount;
    private final int columnCount;
    private final Object values;
    private final LongBitSet present;
    private int size;

    public PrimitiveDirectIntTable(
            PrimitiveKind valueKind, int rowCount, int columnCount) {
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        if (rowCount < 0 || columnCount < 0) {
            throw new IllegalArgumentException("negative dimensions");
        }
        long cells = (long) rowCount * columnCount;
        if (cells > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("table exceeds int-indexed dense lane");
        }
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        int capacity = (int) cells;
        values = valueKind.newArray(capacity);
        present = new LongBitSet(capacity);
    }

    public PrimitiveKind valueKind() { return valueKind; }
    public int rowCount() { return rowCount; }
    public int columnCount() { return columnCount; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() {
        return valueKind.payloadBytes((int) ((long) rowCount * columnCount))
                + present.payloadBytes();
    }

    public boolean containsCell(int row, int column) {
        return present.get(index(row, column));
    }

    public long getOrDefaultBits(
            int row, int column, long defaultValueBits) {
        int index = index(row, column);
        return present.get(index)
                ? valueKind.readBits(values, index)
                : defaultValueBits;
    }

    public long putBits(
            int row, int column, long valueBits, long absentValueBits) {
        int index = index(row, column);
        if (present.get(index)) {
            long old = valueKind.readBits(values, index);
            valueKind.writeBits(values, index, valueBits);
            return old;
        }
        valueKind.writeBits(values, index, valueBits);
        present.set(index);
        size++;
        return absentValueBits;
    }

    public long removeOrDefaultBits(
            int row, int column, long defaultValueBits) {
        int index = index(row, column);
        if (!present.get(index)) return defaultValueBits;
        long old = valueKind.readBits(values, index);
        valueKind.clear(values, index, index + 1);
        present.clear(index);
        size--;
        return old;
    }

    public int clearRow(int row) {
        checkRow(row);
        int removed = 0;
        int start = row * columnCount;
        int end = start + columnCount;
        for (int index = present.nextSetBit(start);
                index >= 0 && index < end;
                index = present.nextSetBit(index + 1)) {
            valueKind.clear(values, index, index + 1);
            present.clear(index);
            removed++;
        }
        size -= removed;
        return removed;
    }

    public int clearColumn(int column) {
        checkColumn(column);
        int removed = 0;
        for (int row = 0; row < rowCount; row++) {
            int index = row * columnCount + column;
            if (present.get(index)) {
                valueKind.clear(values, index, index + 1);
                present.clear(index);
                removed++;
            }
        }
        size -= removed;
        return removed;
    }

    public void clear() {
        valueKind.clear(values, 0, java.lang.reflect.Array.getLength(values));
        present.clear();
        size = 0;
    }

    public void forEach(CellConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int index = present.nextSetBit(0);
                index >= 0;
                index = present.nextSetBit(index + 1)) {
            int row = columnCount == 0 ? 0 : index / columnCount;
            int column = columnCount == 0 ? 0 : index - row * columnCount;
            consumer.accept(row, column, valueKind.readBits(values, index));
        }
    }

    Object valueArray() { return values; }

    private int index(int row, int column) {
        checkRow(row);
        checkColumn(column);
        return row * columnCount + column;
    }

    private void checkRow(int row) {
        if (row < 0 || row >= rowCount) throw new IndexOutOfBoundsException(row);
    }

    private void checkColumn(int column) {
        if (column < 0 || column >= columnCount) throw new IndexOutOfBoundsException(column);
    }
}
