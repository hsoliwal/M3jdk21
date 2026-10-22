/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3scale.primitive;

import java.util.Arrays;

/**
 * Sorted long set backed by one array plus one movable gap.
 *
 * <p>Lookup is binary search. Insertion moves a contiguous gap with System.arraycopy,
 * avoiding a node/tree object per element. Batch insertion merges sorted input in one pass.</p>
 */
public final class LongGapSortedSet {
    private long[] values;
    private int size;
    private int gapStart;
    private int gapEnd;

    public LongGapSortedSet() { this(16); }

    public LongGapSortedSet(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("negative initialCapacity");
        int capacity = initialCapacity == 0 ? 0 : PrimitiveHashing.nextPowerOfTwo(initialCapacity);
        values = new long[capacity];
        gapStart = 0;
        gapEnd = capacity;
    }

    public int size() { return size; }
    public int capacity() { return values.length; }
    public int gapSize() { return gapEnd - gapStart; }
    public boolean isEmpty() { return size == 0; }

    public boolean contains(long value) { return binarySearch(value) >= 0; }

    public long get(int index) {
        checkIndex(index);
        return logicalValue(index);
    }

    public boolean add(long value) {
        int found = binarySearch(value);
        if (found >= 0) return false;
        int insertion = ~found;
        ensureGap();
        moveGapTo(insertion);
        values[gapStart++] = value;
        size++;
        return true;
    }

    public boolean remove(long value) {
        int found = binarySearch(value);
        if (found < 0) return false;
        moveGapTo(found);
        gapEnd++;
        size--;
        return true;
    }

    /**
     * Merge already-sorted input. Duplicate input and keys already present are collapsed.
     * Returns the number of new unique values admitted.
     */
    public int addAllSorted(long[] sorted, int from, int to) {
        checkRange(sorted.length, from, to);
        validateSorted(sorted, from, to);
        if (from == to) return 0;

        compact();
        int oldSize = size;
        int maxSize = Math.addExact(size, to - from);
        int capacity = capacityFor(maxSize);
        long[] merged = new long[capacity];

        int i = 0;
        int j = from;
        int out = 0;
        boolean haveLast = false;
        long last = 0L;

        while (i < oldSize || j < to) {
            long candidate;
            if (j >= to || (i < oldSize && values[i] < sorted[j])) {
                candidate = values[i++];
            } else if (i >= oldSize || sorted[j] < values[i]) {
                candidate = sorted[j++];
            } else {
                candidate = values[i];
                i++;
                j++;
            }

            while (j < to && sorted[j] == candidate) j++;

            if (!haveLast || candidate != last) {
                merged[out++] = candidate;
                last = candidate;
                haveLast = true;
            }
        }

        values = merged;
        size = out;
        gapStart = out;
        gapEnd = capacity;
        return out - oldSize;
    }

    public long[] toArray() {
        long[] result = new long[size];
        System.arraycopy(values, 0, result, 0, gapStart);
        int suffix = values.length - gapEnd;
        if (suffix > 0) System.arraycopy(values, gapEnd, result, gapStart, suffix);
        return result;
    }

    public void clear() {
        size = 0;
        gapStart = 0;
        gapEnd = values.length;
    }

    public long estimatedPayloadBytes() {
        return (long) values.length * Long.BYTES;
    }

    private int binarySearch(long value) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midValue = logicalValue(mid);
            if (midValue < value) low = mid + 1;
            else if (midValue > value) high = mid - 1;
            else return mid;
        }
        return ~low;
    }

    private long logicalValue(int logicalIndex) {
        int physical = logicalIndex < gapStart ? logicalIndex : logicalIndex + gapSize();
        return values[physical];
    }

    private void ensureGap() {
        if (gapStart < gapEnd) return;
        int oldCapacity = values.length;
        int newCapacity = oldCapacity == 0 ? 16 : Math.min(
                PrimitiveHashing.MAX_CAPACITY,
                oldCapacity > (PrimitiveHashing.MAX_CAPACITY >>> 1)
                        ? PrimitiveHashing.MAX_CAPACITY
                        : oldCapacity << 1);
        if (newCapacity <= oldCapacity) throw new IllegalStateException("maximum capacity reached");

        long[] grown = new long[newCapacity];
        int suffix = oldCapacity - gapEnd;
        System.arraycopy(values, 0, grown, 0, gapStart);
        System.arraycopy(values, gapEnd, grown, newCapacity - suffix, suffix);
        gapEnd = newCapacity - suffix;
        values = grown;
    }

    private void moveGapTo(int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex > size) throw new IndexOutOfBoundsException();
        if (logicalIndex < gapStart) {
            int count = gapStart - logicalIndex;
            System.arraycopy(values, logicalIndex, values, gapEnd - count, count);
            gapStart -= count;
            gapEnd -= count;
        } else if (logicalIndex > gapStart) {
            int count = logicalIndex - gapStart;
            System.arraycopy(values, gapEnd, values, gapStart, count);
            gapStart += count;
            gapEnd += count;
        }
    }

    private void compact() {
        moveGapTo(size);
    }

    private static int capacityFor(int required) {
        if (required == 0) return 0;
        return PrimitiveHashing.nextPowerOfTwo(Math.max(16, required));
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index=" + index);
    }

    private static void checkRange(int length, int from, int to) {
        if (from < 0 || to < from || to > length) throw new IndexOutOfBoundsException();
    }

    private static void validateSorted(long[] values, int from, int to) {
        for (int i = from + 1; i < to; i++) {
            if (values[i] < values[i - 1]) {
                throw new IllegalArgumentException("input must be sorted ascending");
            }
        }
    }
}
