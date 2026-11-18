/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

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
public final class IntIndexedSnapshot implements IntCollection {
    public enum Order { ENCOUNTER, ASCENDING, DESCENDING }
    private static final int INSERTION_SORT_LIMIT = 32;
    private final int[] values;
    private final int[] rows;
    private final int from;
    private final int to;
    private final Order order;
    private final boolean distinct;

    private IntIndexedSnapshot(int[] values, int[] rows, int from, int to,
            Order order, boolean distinct) {
        this.values = values;
        this.rows = rows;
        this.from = from;
        this.to = to;
        this.order = order;
        this.distinct = distinct;
    }
    public static IntIndexedSnapshot copyOf(int[] input) {
        Objects.requireNonNull(input, "input");
        return copyOf(input, input.length);
    }
    static IntIndexedSnapshot copyOf(int[] input, int logicalLength) {
        Objects.requireNonNull(input, "input");
        Objects.checkFromIndexSize(0, logicalLength, input.length);
        int[] values = Arrays.copyOf(input, logicalLength);
        int[] rows = new int[values.length];
        for (int i = 0; i < rows.length; i++) rows[i] = i;
        return new IntIndexedSnapshot(values, rows, 0, rows.length, Order.ENCOUNTER, false);
    }
    /** JNI may select primitive logical positions only; payload arrays stay in Java heap. */
    public static IntIndexedSnapshot fromAlignedMask(int[] source, byte[] state,
            int required, IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(source, "source");
        return fromAlignedMask(source, source.length, state, required, backend);
    }
    static IntIndexedSnapshot fromAlignedMask(int[] source, int logicalLength,
            byte[] state, int required, IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(state, "state");
        Objects.checkFromIndexSize(0, logicalLength, source.length);
        Objects.requireNonNull(backend, "backend");
        if (logicalLength != state.length) throw new IllegalArgumentException("unaligned states");
        int[] selected = IndexMaskCompactor.selectPositions(state, required, backend);
        return new IntIndexedSnapshot(Arrays.copyOf(source, logicalLength), selected, 0, selected.length,
                Order.ENCOUNTER, false);
    }
    public Order order() { return order; }
    public boolean isDistinct() { return distinct; }
    @Override public int size() { return to - from; }
    public int get(int index) { return values[rows[from + Objects.checkIndex(index, size())]]; }
    public int physicalIndexAt(int index) { return rows[from + Objects.checkIndex(index, size())]; }
    public IntIndexedSnapshot slice(int begin, int end) {
        Objects.checkFromToIndex(begin, end, size());
        return new IntIndexedSnapshot(values, rows, from + begin, from + end, order, distinct);
    }
    public IntIndexedSnapshot filtered(IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int n = size(), count = 0;
        // One transient bit per logical occurrence; callbacks retain encounter order.
        long[] selected = new long[(int) (((long) n + 63) >>> 6)];
        for (int i = from; i < to; i++) {
            int row = rows[i];
            if (predicate.test(values[row])) {
                int logical = i - from;
                selected[logical >>> 6] |= 1L << (logical & 63);
                count++;
            }
        }
        if (count == n) return this;
        int[] admitted = new int[count];
        int written = 0;
        for (int word = 0; word < selected.length; word++) {
            long bits = selected[word];
            while (bits != 0) {
                int logical = (word << 6) + Long.numberOfTrailingZeros(bits);
                admitted[written++] = rows[from + logical];
                bits &= bits - 1;
            }
        }
        return new IntIndexedSnapshot(values, admitted, 0, count, order, distinct);
    }
    public IntIndexedSnapshot sortedAscending() { return sorted(Order.ASCENDING); }
    public IntIndexedSnapshot sortedDescending() { return sorted(Order.DESCENDING); }
    private IntIndexedSnapshot sorted(Order requested) {
        if (order == requested) return this;
        int n = size();
        // Primitive value reads are side-effect-free. An already-ordered view
        // needs new metadata only; its immutable row directory can be shared.
        if (isMonotone(requested)) {
            return new IntIndexedSnapshot(values, rows, from, to, requested, distinct);
        }
        return sortActual(requested, n);
    }

    private boolean isMonotone(Order requested) {
        for (int i = from + 1; i < to; i++) {
            int previous = values[rows[i - 1]];
            int current = values[rows[i]];
            if (requested == Order.ASCENDING ? previous > current : previous < current) {
                return false;
            }
        }
        return true;
    }

    private IntIndexedSnapshot sortActual(Order requested, int n) {
        int[] orderRows = Arrays.copyOfRange(rows, from, to);
        // Stable insertion avoids allocating a second int[n] scratch lane for
        // small unsorted windows. Only integer slot indexes are moved.
        if (n <= INSERTION_SORT_LIMIT) {
            for (int i = 1; i < n; i++) {
                int selected = orderRows[i];
                int cursor = i;
                while (cursor > 0) {
                    int comparison = Integer.compare(
                            values[orderRows[cursor - 1]], values[selected]);
                    if (requested == Order.DESCENDING) comparison = -comparison;
                    if (comparison <= 0) break; // preserve encounter order on ties
                    orderRows[cursor] = orderRows[cursor - 1];
                    cursor--;
                }
                orderRows[cursor] = selected;
            }
            return new IntIndexedSnapshot(values, orderRows, 0, n, requested, distinct);
        }
        int[] scratch = new int[n];
        for (int width = 1; width < n; width = width >= n - width ? n : width << 1) {
            for (int start = 0; start < n;) {
                // Long arithmetic excludes int overflow near JVM array limits.
                int mid = (int) Math.min((long) n, (long) start + width);
                int end = (int) Math.min((long) n, (long) mid + width);
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
        return new IntIndexedSnapshot(values, orderRows, 0, n, requested, distinct);
    }
    /** Sorted state retains first occurrence of each comparator-equal value. */
    public IntIndexedSnapshot distinctSorted() {
        if (order == Order.ENCOUNTER) throw new IllegalStateException("sort before distinct");
        if (distinct) return this;
        int n = size();
        if (n < 2) return new IntIndexedSnapshot(values, rows, from, to, order, true);
        // Two primitive passes trade an extra linear scan for exact sizing:
        // strictly monotone inputs need no row allocation whatsoever.
        int unique = 1;
        int previous = values[rows[from]];
        for (int i = from + 1; i < to; i++) {
            int next = values[rows[i]];
            if (next != previous) unique++;
            previous = next;
        }
        if (unique == n) {
            return new IntIndexedSnapshot(values, rows, from, to, order, true);
        }
        int[] admitted = new int[unique];
        int used = 0;
        for (int i = from; i < to; i++) {
            int row = rows[i];
            if (used == 0 || values[admitted[used - 1]] != values[row]) {
                admitted[used++] = row;
            }
        }
        return new IntIndexedSnapshot(values, admitted, 0, unique, order, true);
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
    /** Binary upper rank, excluding values comparator-equal to the requested value. */
    public int upperBound(int value) {
        if (order == Order.ENCOUNTER) throw new IllegalStateException("not sorted");
        int low = from, high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            int comparison = Integer.compare(values[rows[mid]], value);
            if (order == Order.DESCENDING) comparison = -comparison;
            if (comparison <= 0) low = mid + 1; else high = mid;
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
    @Override public IntIterator iterator() {
        return new IntIterator() {
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
