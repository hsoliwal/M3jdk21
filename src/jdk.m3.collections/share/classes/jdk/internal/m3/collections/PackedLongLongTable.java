/* *****************************************************************************
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
package jdk.internal.m3.collections;

import jdk.m3.collections.LongLongConsumer;

import java.util.Arrays;
import java.util.Objects;

/** Mutable (row key, column key) -> long table with three long lanes and one int index.
 * No nested row maps, cell/triple objects or boxed coordinates. Expected O(1) point
 * operations. Row/column traversal deliberately scans O(n); it does not claim a second
 * sorted index. Deletion swaps the last cell, so scan order is unspecified.
 * Not thread-safe. Retention predicates are evaluated completely before mutation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongTable.java}
 * blob {@code b9f6e1d3bcc4370f0f92d51f451c6ba82e4d197a} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongTable {
    private long[] rows;
    private long[] columns;
    private long[] values;
    private final PackedLongRowIndex index;
    private int size;
    private int callbacks;
    private Transpose transposed;

    public PackedLongLongTable() { this(8); }
    public PackedLongLongTable(int expectedCells) {
        index = new PackedLongRowIndex(expectedCells);
        rows = new long[Math.max(1, expectedCells)];
        columns = new long[rows.length]; values = new long[rows.length];
    }
    private void writable() {
        if (callbacks != 0) throw new IllegalStateException("mutation during callback");
    }
    private void reserve(int required) {
        index.ensure(required, rows, columns);
        if (required <= rows.length) return;
        int n = PackedLongRowIndex.grownLength(rows.length, required);
        long[] r = Arrays.copyOf(rows, n), c = Arrays.copyOf(columns, n), v = Arrays.copyOf(values, n);
        rows = r; columns = c; values = v;
    }
    public boolean put(long row, long column, long value) {
        writable(); int at = index.find(row, column, rows, columns);
        if (at >= 0) { values[at] = value; return false; }
        reserve(Math.addExact(size, 1));
        rows[size] = row; columns[size] = column; values[size] = value;
        index.insert(size, rows, columns); size++; return true;
    }
    /** Admission runs before capacity growth or insertion and may not mutate this table. */
    public boolean putIfAdmitted(long row, long column, long value, LongLongLongPredicate admission) {
        writable(); Objects.requireNonNull(admission, "admission"); boolean accepted;
        callbacks++;
        try { accepted = admission.test(row, column, value); }
        finally { callbacks--; }
        if (!accepted) return false;
        put(row, column, value); return true;
    }
    public boolean contains(long row, long column) { return index.find(row, column, rows, columns) >= 0; }
    public long getOrDefault(long row, long column, long fallback) {
        int at = index.find(row, column, rows, columns); return at < 0 ? fallback : values[at];
    }
    public boolean remove(long row, long column) {
        writable(); int at = index.find(row, column, rows, columns);
        if (at < 0) return false;
        removeAt(at); return true;
    }
    private void removeAt(int at) {
        index.remove(rows[at], columns[at], rows, columns);
        int tail = --size;
        if (at != tail) {
            index.relocate(tail, at, rows, columns);
            rows[at] = rows[tail]; columns[at] = columns[tail]; values[at] = values[tail];
        }
    }
    public int filterInPlace(LongLongLongPredicate predicate) {
        writable(); Objects.requireNonNull(predicate, "predicate");
        long[] rejected = new long[(int) ((size + 63L) >>> 6)]; int count = 0;
        callbacks++;
        try {
            for (int i = 0; i < size; i++) if (!predicate.test(rows[i], columns[i], values[i])) {
                rejected[i >>> 6] |= 1L << (i & 63); count++;
            }
        } finally { callbacks--; }
        for (int i = size - 1; i >= 0; i--)
            if ((rejected[i >>> 6] & (1L << (i & 63))) != 0) removeAt(i);
        return count;
    }
    public void forEach(LongLongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try { for (int i = 0; i < size; i++) consumer.accept(rows[i], columns[i], values[i]); }
        finally { callbacks--; }
    }
    public void forEachInRow(long row, LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try { for (int i = 0; i < size; i++) if (rows[i] == row) consumer.accept(columns[i], values[i]); }
        finally { callbacks--; }
    }
    public void forEachInColumn(long column, LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try { for (int i = 0; i < size; i++) if (columns[i] == column) consumer.accept(rows[i], values[i]); }
        finally { callbacks--; }
    }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public void clear() { writable(); index.clear(); size = 0; }
    public long[] rowKeysToArray() { return Arrays.copyOf(rows, size); }
    public long[] columnKeysToArray() { return Arrays.copyOf(columns, size); }
    public long[] valuesToArray() { return Arrays.copyOf(values, size); }
    public long arrayPayloadBytes() { return 24L * rows.length + index.payloadBytes(); }

    /**
     * One lazy, live transpose facade; no second table, cell, Entry, or primitive lane.
     * The parent remains the sole storage, indexing, callback and LRU authority.
     */
    public Transpose transpose() {
        Transpose view = transposed;
        if (view == null) transposed = view = new Transpose();
        return view;
    }

    public final class Transpose {
        private Transpose() { }
        /** Double transpose returns the identical authoritative table. */
        public PackedLongLongTable transpose() { return PackedLongLongTable.this; }
        public boolean put(long row, long column, long value) {
            return PackedLongLongTable.this.put(column, row, value);
        }
        /** Admission receives transposed coordinates and runs before any mutation. */
        public boolean putIfAdmitted(long row, long column, long value,
                LongLongLongPredicate admission) {
            writable(); Objects.requireNonNull(admission, "admission"); boolean accepted;
            callbacks++;
            try { accepted = admission.test(row, column, value); }
            finally { callbacks--; }
            if (!accepted) return false;
            PackedLongLongTable.this.put(column, row, value); return true;
        }
        public boolean contains(long row, long column) {
            return PackedLongLongTable.this.contains(column, row);
        }
        public long getOrDefault(long row, long column, long fallback) {
            return PackedLongLongTable.this.getOrDefault(column, row, fallback);
        }
        public boolean remove(long row, long column) {
            return PackedLongLongTable.this.remove(column, row);
        }
        /** Retains parent mutation order and callback failure atomicity. */
        public int filterInPlace(LongLongLongPredicate predicate) {
            writable(); Objects.requireNonNull(predicate, "predicate");
            return PackedLongLongTable.this.filterInPlace(
                    (row, column, value) -> predicate.test(column, row, value));
        }
        public void forEach(LongLongLongConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            PackedLongLongTable.this.forEach(
                    (row, column, value) -> consumer.accept(column, row, value));
        }
        /** Transposed rows are the parent's columns; dense scan order is unchanged. */
        public void forEachInRow(long row, LongLongConsumer consumer) {
            PackedLongLongTable.this.forEachInColumn(row, consumer);
        }
        public void forEachInColumn(long column, LongLongConsumer consumer) {
            PackedLongLongTable.this.forEachInRow(column, consumer);
        }
        public int size() { return PackedLongLongTable.this.size(); }
        public boolean isEmpty() { return PackedLongLongTable.this.isEmpty(); }
        public void clear() { PackedLongLongTable.this.clear(); }
        public long[] rowKeysToArray() { return PackedLongLongTable.this.columnKeysToArray(); }
        public long[] columnKeysToArray() { return PackedLongLongTable.this.rowKeysToArray(); }
        public long[] valuesToArray() { return PackedLongLongTable.this.valuesToArray(); }
        public long arrayPayloadBytes() { return PackedLongLongTable.this.arrayPayloadBytes(); }
    }
}
