// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Immutable long-to-long sorted-map snapshot over two parallel primitive value lanes.
 *
 * <p>Key/value payloads are copied once at admission; only an {@code int[]} physical-index
 * directory is sorted or filtered. Views retain an immutable half-open [from,to) interval
 * into that directory, so key ranges and logical slices allocate no element arrays or
 * per-entry objects. Mask selection composes over the current logical index range.
 *
 * <p>Keys use signed-long natural order and must be unique. Unlike a mutable JDK TreeMap,
 * this snapshot rejects duplicate keys instead of silently selecting one of their values.
 * It is an independent M3 proving backend, not a java.util.Map or java.base replacement.
 * External arrays are never retained. Lookup/navigation is O(log n), sorted admission
 * O(n log n) with O(n) primitive scratch, slices O(1), mask selection O(n).
 *
 * <p>An optional native selection is delegated only to the existing
 * {@link M3IndexMaskCompactor}; JNI receives primitive physical indices and flag bytes,
 * never Java comparator/predicate callbacks or heap addresses retained after return.
 */
public final class M3LongLongIndexedSortedView {
    private final long[] keys;
    private final long[] values;
    private final int[] order;
    private final int from;
    private final int to;

    private M3LongLongIndexedSortedView(long[] keys, long[] values,
                                        int[] order, int from, int to) {
        this.keys = keys;
        this.values = values;
        this.order = order;
        this.from = from;
        this.to = to;
    }

    /** Defensively snapshots two aligned arrays, sorting only the physical row IDs. */
    public static M3LongLongIndexedSortedView snapshot(long[] keys, long[] values) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        if (keys.length != values.length) {
            throw new IllegalArgumentException("key and value array sizes differ");
        }
        long[] detachedKeys = keys.clone();
        long[] detachedValues = values.clone();
        int[] ordered = new int[detachedKeys.length];
        for (int i = 0; i < ordered.length; i++) ordered[i] = i;
        sortPhysicalIndices(detachedKeys, ordered);
        for (int i = 1; i < ordered.length; i++) {
            if (detachedKeys[ordered[i - 1]] == detachedKeys[ordered[i]]) {
                throw new IllegalArgumentException("duplicate key: " + detachedKeys[ordered[i]]);
            }
        }
        return new M3LongLongIndexedSortedView(detachedKeys, detachedValues,
                ordered, 0, ordered.length);
    }

    public int size() { return to - from; }
    public boolean isEmpty() { return from == to; }

    /** Original physical row address, not a current logical rank. */
    public int physicalIndex(int ordinal) {
        return order[from + Objects.checkIndex(ordinal, size())];
    }

    public long keyAt(int ordinal) { return keys[physicalIndex(ordinal)]; }
    public long valueAt(int ordinal) { return values[physicalIndex(ordinal)]; }

    /** Signed-long lower-bound rank in this view (0..size). */
    public int rank(long key) { return lowerBound(key) - from; }

    /** Exact matching key rank or -1; absence is distinct from any long value. */
    public int indexOfKey(long key) {
        int location = lowerBound(key);
        return location < to && keys[order[location]] == key ? location - from : -1;
    }

    public boolean containsKey(long key) { return indexOfKey(key) >= 0; }

    /** Returns the mapped long or caller-provided absence sentinel. */
    public long getOrDefault(long key, long fallback) {
        int position = indexOfKey(key);
        return position < 0 ? fallback : valueAt(position);
    }

    public long firstKey() {
        if (isEmpty()) throw new NoSuchElementException();
        return keys[order[from]];
    }

    public long lastKey() {
        if (isEmpty()) throw new NoSuchElementException();
        return keys[order[to - 1]];
    }

    public long floorKey(long pivot, long fallback) {
        int pos = upperBound(pivot) - 1;
        return pos < from ? fallback : keys[order[pos]];
    }

    public long ceilingKey(long pivot, long fallback) {
        int pos = lowerBound(pivot);
        return pos >= to ? fallback : keys[order[pos]];
    }

    public long lowerKey(long pivot, long fallback) {
        int pos = lowerBound(pivot) - 1;
        return pos < from ? fallback : keys[order[pos]];
    }

    public long higherKey(long pivot, long fallback) {
        int pos = upperBound(pivot);
        return pos >= to ? fallback : keys[order[pos]];
    }

    /** O(1) immutable logical slice retaining both payload lanes and their index directory. */
    public M3LongLongIndexedSortedView slice(int offset, int length) {
        Objects.checkFromIndexSize(offset, length, size());
        return new M3LongLongIndexedSortedView(keys, values, order,
                from + offset, from + offset + length);
    }

    /** Half-open key range [lowInclusive, highExclusive); view shares the same arrays. */
    public M3LongLongIndexedSortedView subMap(long lowInclusive, long highExclusive) {
        if (lowInclusive > highExclusive) {
            throw new IllegalArgumentException("lowInclusive > highExclusive");
        }
        return new M3LongLongIndexedSortedView(keys, values, order,
                lowerBound(lowInclusive), lowerBound(highExclusive));
    }

    public M3LongLongIndexedSortedView headMap(long highExclusive) {
        return new M3LongLongIndexedSortedView(keys, values, order, from, lowerBound(highExclusive));
    }

    public M3LongLongIndexedSortedView tailMap(long lowInclusive) {
        return new M3LongLongIndexedSortedView(keys, values, order, lowerBound(lowInclusive), to);
    }

    /**
     * Selects rows whose aligned logical-state byte contains every bit in requiredMask.
     * Composes with any preceding sorting, slicing, key-range or mask selection.
     * Native selection uses the current bounded logical index window only.
     */
    public M3LongLongIndexedSortedView filtered(byte[] alignedStates, int requiredMask,
                                                 M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(backend, "backend");
        M3IndexMaskCompactor.validate(alignedStates, size(), requiredMask);
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            int[] selected = M3IndexMaskCompactor.select(
                    Arrays.copyOfRange(order, from, to), alignedStates, requiredMask, backend);
            return new M3LongLongIndexedSortedView(keys, values, selected, 0, selected.length);
        }
        int n = M3IndexMaskCompactor.selectedCount(alignedStates, requiredMask);
        if (n == size()) return this;
        int[] selected = new int[n];
        int cursor = 0;
        for (int i = 0; i < size(); i++) {
            if (!M3IndexMaskCompactor.matches(alignedStates[i], requiredMask)) continue;
            if (cursor >= selected.length) {
                throw new ConcurrentModificationException("aligned selection flags changed");
            }
            selected[cursor++] = order[from + i];
        }
        if (cursor != selected.length) {
            throw new ConcurrentModificationException("aligned selection flags changed");
        }
        return new M3LongLongIndexedSortedView(keys, values, selected, 0, selected.length);
    }

    public M3LongLongIndexedSortedView filtered(byte[] alignedStates, int requiredMask) {
        return filtered(alignedStates, requiredMask, M3IndexMaskCompactor.Backend.JAVA);
    }

    /** Materialization boundaries: fresh arrays in this view's sorted logical key order. */
    public long[] keys() {
        long[] result = new long[size()];
        for (int i = 0; i < result.length; i++) result[i] = keyAt(i);
        return result;
    }

    public long[] values() {
        long[] result = new long[size()];
        for (int i = 0; i < result.length; i++) result[i] = valueAt(i);
        return result;
    }

    public int[] physicalIndices() { return Arrays.copyOfRange(order, from, to); }

    /** Primitive retained arrays, excluding JVM object/array headers and discarded sort scratch. */
    public long retainedPrimitiveBytes() {
        return (long) keys.length * Long.BYTES + (long) values.length * Long.BYTES
                + (long) order.length * Integer.BYTES;
    }

    private int lowerBound(long key) {
        int low = from, high = to;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (keys[order[middle]] < key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private int upperBound(long key) {
        int low = from, high = to;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (keys[order[middle]] <= key) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private static void sortPhysicalIndices(long[] keys, int[] order) {
        final int count = order.length;
        if (count < 2) return;
        int[] original = order;
        int[] scratch = new int[count];
        for (long width = 1; width < count; width <<= 1) {
            for (long start = 0; start < count; start += width * 2) {
                int left = (int) start;
                int middle = (int) Math.min(start + width, count);
                int right = (int) Math.min(start + width * 2, count);
                int a = left, b = middle, dst = left;
                while (a < middle && b < right) {
                    if (keys[order[a]] <= keys[order[b]]) scratch[dst++] = order[a++];
                    else scratch[dst++] = order[b++];
                }
                while (a < middle) scratch[dst++] = order[a++];
                while (b < right) scratch[dst++] = order[b++];
            }
            int[] tmp = order; order = scratch; scratch = tmp;
        }
        // Bottom-up sort swaps working arrays; publish the sorted index in the
        // originally owned directory regardless of the parity of merge levels.
        if (order != original) System.arraycopy(order, 0, original, 0, count);
    }
}
