/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3scale.primitive;

/**
 * Sorted long-to-long map in parallel arrays with one movable gap.
 * Binary search + System.arraycopy; no tree node or Entry per mapping.
 */
public final class LongLongGapSortedMap {
    private long[] keys;
    private long[] values;
    private int size;
    private int gapStart;
    private int gapEnd;

    public LongLongGapSortedMap() { this(16); }

    public LongLongGapSortedMap(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("negative initialCapacity");
        int capacity = initialCapacity == 0 ? 0 : PrimitiveHashing.nextPowerOfTwo(initialCapacity);
        keys = new long[capacity];
        values = new long[capacity];
        gapEnd = capacity;
    }

    public int size() { return size; }
    public int capacity() { return keys.length; }
    public int gapSize() { return gapEnd - gapStart; }

    public boolean containsKey(long key) { return binarySearch(key) >= 0; }

    public long getOrDefault(long key, long defaultValue) {
        int index = binarySearch(key);
        return index >= 0 ? logicalValue(index) : defaultValue;
    }

    public boolean put(long key, long value) {
        int found = binarySearch(key);
        if (found >= 0) {
            values[physical(found)] = value;
            return false;
        }
        int insertion = ~found;
        ensureGap();
        moveGapTo(insertion);
        keys[gapStart] = key;
        values[gapStart] = value;
        gapStart++;
        size++;
        return true;
    }

    public boolean remove(long key) {
        int found = binarySearch(key);
        if (found < 0) return false;
        moveGapTo(found);
        gapEnd++;
        size--;
        return true;
    }

    public long keyAt(int index) {
        checkIndex(index);
        return logicalKey(index);
    }

    public long valueAt(int index) {
        checkIndex(index);
        return logicalValue(index);
    }

    /**
     * Merge sorted key/value input in O(n+m). Repeated incoming keys keep their last value.
     * Existing mappings with the same key are replaced.
     */
    public void putAllSorted(long[] sortedKeys, long[] sortedValues, int from, int to) {
        if (sortedKeys.length != sortedValues.length) throw new IllegalArgumentException("array length mismatch");
        checkRange(sortedKeys.length, from, to);
        validateSorted(sortedKeys, from, to);
        if (from == to) return;

        compact();
        int oldSize = size;
        int capacity = capacityFor(Math.addExact(oldSize, to - from));
        long[] mergedKeys = new long[capacity];
        long[] mergedValues = new long[capacity];

        int i = 0;
        int j = from;
        int out = 0;

        while (j < to) {
            long incomingKey = sortedKeys[j];
            long incomingValue = sortedValues[j];
            int next = j + 1;
            while (next < to && sortedKeys[next] == incomingKey) {
                incomingValue = sortedValues[next++];
            }

            while (i < oldSize && keys[i] < incomingKey) {
                mergedKeys[out] = keys[i];
                mergedValues[out] = values[i];
                out++;
                i++;
            }

            if (i < oldSize && keys[i] == incomingKey) i++;

            mergedKeys[out] = incomingKey;
            mergedValues[out] = incomingValue;
            out++;
            j = next;
        }

        while (i < oldSize) {
            mergedKeys[out] = keys[i];
            mergedValues[out] = values[i];
            out++;
            i++;
        }

        keys = mergedKeys;
        values = mergedValues;
        size = out;
        gapStart = out;
        gapEnd = capacity;
    }

    public void clear() {
        size = 0;
        gapStart = 0;
        gapEnd = keys.length;
    }

    public long estimatedPayloadBytes() {
        return ((long) keys.length + values.length) * Long.BYTES;
    }

    private int binarySearch(long key) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midKey = logicalKey(mid);
            if (midKey < key) low = mid + 1;
            else if (midKey > key) high = mid - 1;
            else return mid;
        }
        return ~low;
    }

    private long logicalKey(int index) { return keys[physical(index)]; }
    private long logicalValue(int index) { return values[physical(index)]; }

    private int physical(int logicalIndex) {
        return logicalIndex < gapStart ? logicalIndex : logicalIndex + gapSize();
    }

    private void ensureGap() {
        if (gapStart < gapEnd) return;
        int oldCapacity = keys.length;
        int newCapacity = oldCapacity == 0 ? 16 : Math.min(
                PrimitiveHashing.MAX_CAPACITY,
                oldCapacity > (PrimitiveHashing.MAX_CAPACITY >>> 1)
                        ? PrimitiveHashing.MAX_CAPACITY
                        : oldCapacity << 1);
        if (newCapacity <= oldCapacity) throw new IllegalStateException("maximum capacity reached");

        long[] newKeys = new long[newCapacity];
        long[] newValues = new long[newCapacity];
        int suffix = oldCapacity - gapEnd;
        System.arraycopy(keys, 0, newKeys, 0, gapStart);
        System.arraycopy(values, 0, newValues, 0, gapStart);
        System.arraycopy(keys, gapEnd, newKeys, newCapacity - suffix, suffix);
        System.arraycopy(values, gapEnd, newValues, newCapacity - suffix, suffix);
        gapEnd = newCapacity - suffix;
        keys = newKeys;
        values = newValues;
    }

    private void moveGapTo(int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex > size) throw new IndexOutOfBoundsException();
        if (logicalIndex < gapStart) {
            int count = gapStart - logicalIndex;
            System.arraycopy(keys, logicalIndex, keys, gapEnd - count, count);
            System.arraycopy(values, logicalIndex, values, gapEnd - count, count);
            gapStart -= count;
            gapEnd -= count;
        } else if (logicalIndex > gapStart) {
            int count = logicalIndex - gapStart;
            System.arraycopy(keys, gapEnd, keys, gapStart, count);
            System.arraycopy(values, gapEnd, values, gapStart, count);
            gapStart += count;
            gapEnd += count;
        }
    }

    private void compact() { moveGapTo(size); }

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
                throw new IllegalArgumentException("keys must be sorted ascending");
            }
        }
    }
}
