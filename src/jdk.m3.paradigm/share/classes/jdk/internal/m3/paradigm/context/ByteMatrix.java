/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Objects;

/**
 * Row-major U8 context matrix with one presence word per row.
 *
 * <p>Cells are addressed by row*columns+column. No row, cell, key, number,
 * iterator or entry object is retained on the hot path.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextByteMatrix.java}
 * blob {@code 192de28b9031fc925c29569514f166fb93310ba4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ByteMatrix {
    @FunctionalInterface
    public interface CellConsumer {
        void accept(int column, int unsignedValue);
    }

    private final SlotSchema schema;
    private final int rows;
    private final long[] present;
    private final byte[] values;

    public ByteMatrix(SlotSchema schema, int rows) {
        this.schema = Objects.requireNonNull(schema, "schema");
        if (rows < 0) throw new IllegalArgumentException("rows must be >= 0");
        this.rows = rows;
        this.present = new long[rows];
        this.values = new byte[Math.multiplyExact(rows, schema.size())];
    }

    public SlotSchema schema() { return schema; }
    public int rows() { return rows; }
    public int columns() { return schema.size(); }

    public boolean contains(int row, int column) {
        check(row, column);
        return (present[row] & bit(column)) != 0L;
    }

    public long presentMask(int row) {
        return present[Objects.checkIndex(row, rows)];
    }

    public int getUnsignedOrDefault(int row, int column, int fallback) {
        check(row, column);
        return (present[row] & bit(column)) == 0L ? fallback : Byte.toUnsignedInt(values[offset(row, column)]);
    }

    public int requireUnsigned(int row, int column) {
        check(row, column);
        if ((present[row] & bit(column)) == 0L) {
            throw new IllegalArgumentException("context matrix cell is absent");
        }
        return Byte.toUnsignedInt(values[offset(row, column)]);
    }

    public void setUnsigned(int row, int column, int unsigned) {
        check(row, column);
        if ((unsigned & ~0xff) != 0) throw new IllegalArgumentException("unsigned byte must be in [0,255]");
        values[offset(row, column)] = (byte) unsigned;
        present[row] |= bit(column);
    }

    public void clear(int row, int column) {
        check(row, column);
        values[offset(row, column)] = 0;
        present[row] &= ~bit(column);
    }

    public void clearRow(int row) {
        int checked = Objects.checkIndex(row, rows);
        int start = checked * columns();
        java.util.Arrays.fill(values, start, start + columns(), (byte) 0);
        present[checked] = 0L;
    }

    public int sumUnsigned(int row) {
        int checked = Objects.checkIndex(row, rows);
        long mask = present[checked];
        int sum = 0;
        while (mask != 0L) {
            int column = Long.numberOfTrailingZeros(mask);
            sum += Byte.toUnsignedInt(values[offset(checked, column)]);
            mask &= mask - 1L;
        }
        return sum;
    }

    public long dot(int leftRow, int rightRow) {
        int left = Objects.checkIndex(leftRow, rows);
        int right = Objects.checkIndex(rightRow, rows);
        long mask = present[left] & present[right];
        long sum = 0L;
        while (mask != 0L) {
            int column = Long.numberOfTrailingZeros(mask);
            sum += (long) Byte.toUnsignedInt(values[offset(left, column)])
                    * Byte.toUnsignedInt(values[offset(right, column)]);
            mask &= mask - 1L;
        }
        return sum;
    }

    public int l1(int leftRow, int rightRow) {
        int left = Objects.checkIndex(leftRow, rows);
        int right = Objects.checkIndex(rightRow, rows);
        long mask = present[left] | present[right];
        int sum = 0;
        while (mask != 0L) {
            int column = Long.numberOfTrailingZeros(mask);
            int a = (present[left] & bit(column)) == 0L ? 0 : Byte.toUnsignedInt(values[offset(left, column)]);
            int b = (present[right] & bit(column)) == 0L ? 0 : Byte.toUnsignedInt(values[offset(right, column)]);
            sum = Math.addExact(sum, Math.abs(a - b));
            mask &= mask - 1L;
        }
        return sum;
    }

    public long selectAtLeast(int row, int threshold) {
        if ((threshold & ~0xff) != 0) throw new IllegalArgumentException("threshold must be in [0,255]");
        int checked = Objects.checkIndex(row, rows);
        long mask = present[checked];
        long selected = 0L;
        while (mask != 0L) {
            int column = Long.numberOfTrailingZeros(mask);
            if (Byte.toUnsignedInt(values[offset(checked, column)]) >= threshold) selected |= bit(column);
            mask &= mask - 1L;
        }
        return selected;
    }

    public void forEachPresent(int row, CellConsumer consumer) {
        int checked = Objects.checkIndex(row, rows);
        Objects.requireNonNull(consumer, "consumer");
        long mask = present[checked];
        while (mask != 0L) {
            int column = Long.numberOfTrailingZeros(mask);
            consumer.accept(column, Byte.toUnsignedInt(values[offset(checked, column)]));
            mask &= mask - 1L;
        }
    }

    public byte[] rowCopy(int row) {
        int checked = Objects.checkIndex(row, rows);
        int start = checked * columns();
        return java.util.Arrays.copyOfRange(values, start, start + columns());
    }

    public long logicalPayloadBytes() {
        return (long) present.length * Long.BYTES + values.length;
    }

    private int offset(int row, int column) {
        return Math.addExact(Math.multiplyExact(row, columns()), column);
    }

    private void check(int row, int column) {
        Objects.checkIndex(row, rows);
        Objects.checkIndex(column, columns());
    }

    private static long bit(int column) {
        return 1L << column;
    }
}
