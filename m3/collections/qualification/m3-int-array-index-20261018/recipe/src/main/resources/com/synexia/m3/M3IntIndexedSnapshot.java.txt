/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntPredicate;
import java.util.function.IntConsumer;

/**
 * Immutable primitive-int view: detached int[] values + int[] logical row permutation.
 * Filtering and sorting manipulate only coordinates; slices share both backing arrays.
 * No Integer, Map.Entry, Node, or Java predicate ever crosses the JNI boundary.
 */
public final class M3IntIndexedSnapshot implements M3IntCollection {
    public enum Order { ENCOUNTER, ASCENDING, DESCENDING }
    private final int[] values;
    private final int[] rows;
    private final int from;
    private final int to;
    private final Order order;
    private final boolean distinct;

    private M3IntIndexedSnapshot(int[] values, int[] rows, int from, int to,
            Order order, boolean distinct) {
        this.values = values;
        this.rows = rows;
        this.from = from;
        this.to = to;
        this.order = order;
        this.distinct = distinct;
    }
    public static M3IntIndexedSnapshot copyOf(int[] input) {
        Objects.requireNonNull(input, "input");
        return copyOf(input, input.length);
    }
    static M3IntIndexedSnapshot copyOf(int[] input, int logicalLength) {
        Objects.requireNonNull(input, "input");
        Objects.checkFromIndexSize(0, logicalLength, input.length);
        int[] values = Arrays.copyOf(input, logicalLength);
        int[] rows = new int[values.length];
        for (int i = 0; i < rows.length; i++) rows[i] = i;
        return new M3IntIndexedSnapshot(values, rows, 0, rows.length, Order.ENCOUNTER, false);
    }
    /** JNI may select primitive logical positions only; payload arrays stay in Java heap. */
    public static M3IntIndexedSnapshot fromAlignedMask(int[] source, byte[] state,
            int required, M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(source, "source");
        return fromAlignedMask(source, source.length, state, required, backend);
    }
    static M3IntIndexedSnapshot fromAlignedMask(int[] source, int logicalLength,
            byte[] state, int required, M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(state, "state");
        Objects.checkFromIndexSize(0, logicalLength, source.length);
        Objects.requireNonNull(backend, "backend");
        if (logicalLength != state.length) throw new IllegalArgumentException("unaligned states");
        int[] selected = M3IndexMaskCompactor.selectPositions(state, required, backend);
        return new M3IntIndexedSnapshot(Arrays.copyOf(source, logicalLength), selected, 0, selected.length,
                Order.ENCOUNTER, false);
    }
    public Order order() { return order; }
    public boolean isDistinct() { return distinct; }
    @Override public int size() { return to - from; }
    public int get(int index) { return values[rows[from + Objects.checkIndex(index, size())]]; }
    public int physicalIndexAt(int index) { return rows[from + Objects.checkIndex(index, size())]; }
    public M3IntIndexedSnapshot slice(int begin, int end) {
        Objects.checkFromToIndex(begin, end, size());
        return new M3IntIndexedSnapshot(values, rows, from + begin, from + end, order, distinct);
    }
    public M3IntIndexedSnapshot filtered(IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int n = size(), count = 0;
        int[] selected = new int[n];
        for (int i = from; i < to; i++) {
            int row = rows[i];
            if (predicate.test(values[row])) selected[count++] = row;
        }
        if (count == n) return this;
        return new M3IntIndexedSnapshot(values, Arrays.copyOf(selected, count),
                0, count, order, distinct);
    }
    public M3IntIndexedSnapshot sortedAscending() { return sorted(Order.ASCENDING); }
    public M3IntIndexedSnapshot sortedDescending() { return sorted(Order.DESCENDING); }
    private M3IntIndexedSnapshot sorted(Order requested) {
        if (order == requested) return this;
        int n = size();
        return sortActual(requested, n);
    }
    private M3IntIndexedSnapshot sortActual(Order requested, int n) {
        int[] orderRows = Arrays.copyOfRange(rows, from, to);
        int[] scratch = new int[n];
        for (int width = 1; width < n; width = width >= n - width ? n : width << 1) {
            for (int start = 0; start < n;) {
                int mid = Math.min(n, start + width);
                int end = Math.min(n, mid + width);
                int left = start, right = mid, write = start;
                while (left < mid && right < end) {
                    int comparison = Integer.compare(values[orderRows[left]], values[orderRows[right]]);
                    if (requested == Order.DESCENDING) comparison = -comparison;
                    scratch[write++] = comparison <= 0 ? orderRows[left++] : orderRows[right++];
                }
                while (left < mid) scratch[write++] = orderRows[left++];
                while (right < end) scratch[write++] = orderRows[right++];
                start = end;
            }
            int[] temp = orderRows; orderRows = scratch; scratch = temp;
        }
        return new M3IntIndexedSnapshot(values, orderRows, 0, n, requested, distinct);
    }
    /** Sorted state retains first occurrence of each comparator-equal value. */
    public M3IntIndexedSnapshot distinctSorted() {
        if (order == Order.ENCOUNTER) throw new IllegalStateException("sort before distinct");
        if (distinct) return this;
        int n = size();
        if (n == 0) return new M3IntIndexedSnapshot(values, new int[0], 0, 0, order, true);
        int[] out = new int[n]; int keep = 0;
        for (int i = from; i < to; i++) {
            int row = rows[i];
            if (keep == 0 || values[out[keep - 1]] != values[row]) out[keep++] = row;
        }
        return new M3IntIndexedSnapshot(values, Arrays.copyOf(out, keep), 0, keep, order, true);
    }
    /** Binary rank with duplicates. Sorted ascending or descending is required. */
    public int lowerBound(int value) {
        if (order == Order.ENCOUNTER) throw new IllegalStateException("not sorted");
        int low = from, high = to;
        while (low < high) {
            int mid = (low + high) >>> 1;
            int cmp = Integer.compare(values[rows[mid]], value);
            if (order == Order.DESCENDING) cmp = -cmp;
            if (cmp < 0) low = mid + 1; else high = mid;
        }
        return low - from;
    }
    @Override public boolean contains(int value) {
        if (order == Order.ENCOUNTER) {
            for (int i = from; i < to; i++) if (values[rows[i]] == value) return true;
            return false;
        }
        int p = lowerBound(value);
        return p < size() && get(p) == value;
    }
    @Override public void clear() { throw new UnsupportedOperationException("immutable M3 snapshot"); }
    @Override public int[] toArray() {
        int[] copy = new int[size()];
        for (int i = 0; i < copy.length; i++) copy[i] = get(i);
        return copy;
    }
    @Override public M3IntIterator iterator() {
        return new M3IntIterator() {
            int cursor = from;
            @Override public boolean hasNext() { return cursor < to; }
            @Override public int nextInt() {
                if (!hasNext()) throw new NoSuchElementException();
                return values[rows[cursor++]];
            }
        };
    }
    @Override public Spliterator.OfInt intSpliterator() { return new SnapshotSpliterator(from, to); }
    private final class SnapshotSpliterator implements Spliterator.OfInt {
        private int cursor;
        private final int fence;
        SnapshotSpliterator(int first, int fence) { this.cursor = first; this.fence = fence; }
        @Override public boolean tryAdvance(IntConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (cursor == fence) return false;
            consumer.accept(values[rows[cursor++]]);
            return true;
        }
        @Override public OfInt trySplit() {
            int middle = (cursor + fence) >>> 1;
            if (middle == cursor) return null;
            OfInt prefix = new SnapshotSpliterator(cursor, middle);
            cursor = middle;
            return prefix;
        }
        @Override public long estimateSize() { return fence - cursor; }
        @Override public int characteristics() {
            return ORDERED | SIZED | SUBSIZED | NONNULL | IMMUTABLE
                    | (order == Order.ENCOUNTER ? 0 : SORTED)
                    | (distinct ? DISTINCT : 0);
        }
        @Override public java.util.Comparator<? super Integer> getComparator() {
            if (order == Order.ENCOUNTER) throw new IllegalStateException("not sorted");
            return order == Order.ASCENDING ? null : java.util.Comparator.reverseOrder();
        }
    }
}
