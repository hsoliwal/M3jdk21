/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Ordered primitive long-to-long map on two sorted parallel lanes with no per-entry node.
 *
 * <p>Keys live in one sorted {@code long[]} lane and values in a parallel {@code long[]} lane, so
 * every long, including zero and the extrema, is a legal key and a legal value, and iteration is a
 * plain lane walk in natural signed order. The mechanics are those of the first-party Synexia donor
 * {@code PackedLongLongSortedMap} under the {@code LongLongSortedMap} contract: single updates use
 * binary search plus {@code System.arraycopy}; nondecreasing batches collapse duplicate keys (last
 * value wins) and merge in one forward pass; growth is 1.5x with a floor of eight slots and
 * compaction is the explicit {@link #trimToSize()}. Lookups and the navigation calls are O(log n);
 * a single put or remove is an O(log n) search plus an O(n) lane shift, exactly as in the donor.
 * Navigation takes a caller default as {@link java.util.NavigableMap} would return null;
 * {@code firstKey}, {@code lastKey} and the poll calls throw {@link NoSuchElementException} on an
 * empty map; the key iterator and the forEach walks are fail-fast. This is an M3 proving-ground
 * backend, not a {@code java.util.TreeMap} replacement.</p>
 */
public final class M3LongLongTreeMap implements M3LongLongSortedMap {
    private static final int MIN_CAPACITY = 8;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private long[] keys;
    private long[] values;
    private int size;
    private int modCount;

    public M3LongLongTreeMap() {
        this(0);
    }

    public M3LongLongTreeMap(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        int capacity = Math.max(MIN_CAPACITY, expectedSize);
        keys = new long[capacity];
        values = new long[capacity];
    }

    /** Puts a value and returns true only when a new key was inserted. */
    @Override
    public boolean put(long key, long value) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) {
            values[index] = value;
            return false;
        }
        insertAt(-index - 1, key, value);
        return true;
    }

    /**
     * Merges a batch of nondecreasing keys in one forward pass. Duplicate incoming keys collapse
     * with the last value winning and incoming values replace present keys; returns the number of
     * new keys. Unsorted keys or mismatched lane lengths are refused before any state changes.
     */
    public int putAllSorted(long[] sourceKeys, long[] sourceValues) {
        Objects.requireNonNull(sourceKeys, "sourceKeys");
        Objects.requireNonNull(sourceValues, "sourceValues");
        if (sourceKeys.length != sourceValues.length) {
            throw new IllegalArgumentException("key/value length mismatch");
        }
        if (sourceKeys.length == 0) {
            return 0;
        }
        long[] batchKeys = new long[sourceKeys.length];
        long[] batchValues = new long[sourceValues.length];
        int unique = collapseSorted(sourceKeys, sourceValues, batchKeys, batchValues);
        int before = size;
        merge(batchKeys, batchValues, unique);
        if (size != before) {
            modCount++;
        }
        return size - before;
    }

    @Override
    public boolean containsKey(long key) {
        return Arrays.binarySearch(keys, 0, size, key) >= 0;
    }

    @Override
    public long getOrDefault(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        return index < 0 ? defaultValue : values[index];
    }

    /** Returns the mapped value; an absent key is refused as in the donor. */
    @Override
    public long getOrThrow(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index < 0) {
            throw new IllegalArgumentException("missing key: " + key);
        }
        return values[index];
    }

    @Override
    public boolean remove(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index < 0) {
            return false;
        }
        removeAt(index);
        return true;
    }

    @Override
    public long firstKey() {
        return keys[checkedIndex(0)];
    }

    @Override
    public long lastKey() {
        return keys[checkedIndex(size - 1)];
    }

    /** Removes the least entry and returns its key; throws when the map is empty. */
    public long pollFirstKey() {
        long key = firstKey();
        removeAt(0);
        return key;
    }

    /** Removes the greatest entry and returns its key; throws when the map is empty. */
    public long pollLastKey() {
        long key = lastKey();
        removeAt(size - 1);
        return key;
    }

    @Override
    public long floorKey(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) {
            return keys[index];
        }
        int insertionPoint = -index - 1;
        return insertionPoint == 0 ? defaultValue : keys[insertionPoint - 1];
    }

    @Override
    public long ceilingKey(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) {
            return keys[index];
        }
        int insertionPoint = -index - 1;
        return insertionPoint == size ? defaultValue : keys[insertionPoint];
    }

    @Override
    public long lowerKey(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        int candidate = index >= 0 ? index - 1 : -index - 2;
        return candidate < 0 ? defaultValue : keys[candidate];
    }

    @Override
    public long higherKey(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        int candidate = index >= 0 ? index + 1 : -index - 1;
        return candidate >= size ? defaultValue : keys[candidate];
    }

    @Override
    public void forEach(M3LongLongConsumer consumer) {
        walk(0, size, consumer);
    }

    /** Visits the entries whose keys fall in {@code [fromInclusive, toExclusive)} in ascending order. */
    public void forEachInRange(long fromInclusive, long toExclusive, M3LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (fromInclusive >= toExclusive) {
            return;
        }
        walk(ceilingIndex(fromInclusive), ceilingIndex(toExclusive), consumer);
    }

    /** Returns a fail-fast primitive iterator over the keys in ascending order. */
    public M3LongIterator keyIterator() {
        int expectedModCount = modCount;
        return new M3LongIterator() {
            private int cursor;

            @Override
            public boolean hasNext() {
                checkForComodification(expectedModCount);
                return cursor < size;
            }

            @Override
            public long nextLong() {
                checkForComodification(expectedModCount);
                if (cursor >= size) {
                    throw new NoSuchElementException();
                }
                return keys[cursor++];
            }
        };
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return keys.length;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        size = 0;
        modCount++;
    }

    /** Compacts both lanes to the live entry count (floor of eight slots); order is unchanged. */
    public void trimToSize() {
        int capacity = Math.max(MIN_CAPACITY, size);
        if (capacity != keys.length) {
            keys = Arrays.copyOf(keys, capacity);
            values = Arrays.copyOf(values, capacity);
        }
    }

    @Override
    public long[] keysToArray() {
        return Arrays.copyOf(keys, size);
    }

    @Override
    public long[] valuesToArray() {
        return Arrays.copyOf(values, size);
    }

    private void insertAt(int index, long key, long value) {
        ensureCapacity(size + 1);
        System.arraycopy(keys, index, keys, index + 1, size - index);
        System.arraycopy(values, index, values, index + 1, size - index);
        keys[index] = key;
        values[index] = value;
        size++;
        modCount++;
    }

    private void removeAt(int index) {
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(keys, index + 1, keys, index, moved);
            System.arraycopy(values, index + 1, values, index, moved);
        }
        size--;
        modCount++;
    }

    private int checkedIndex(int index) {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return index;
    }

    private int ceilingIndex(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        return index >= 0 ? index : -index - 1;
    }

    private void walk(int from, int to, M3LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int expectedModCount = modCount;
        for (int i = from; i < to; i++) {
            consumer.accept(keys[i], values[i]);
            checkForComodification(expectedModCount);
        }
    }

    private void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity <= keys.length) {
            return;
        }
        int capacity = growCapacity(keys.length, minimumCapacity);
        keys = Arrays.copyOf(keys, capacity);
        values = Arrays.copyOf(values, capacity);
    }

    private static int growCapacity(int current, int minimum) {
        if (minimum < 0 || minimum > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("required array size too large");
        }
        long grown = current < MIN_CAPACITY ? MIN_CAPACITY : (long) current + (current >>> 1);
        if (grown < minimum) {
            grown = minimum;
        }
        return (int) Math.min(grown, MAX_ARRAY_SIZE);
    }

    private static int collapseSorted(long[] sourceKeys, long[] sourceValues,
            long[] batchKeys, long[] batchValues) {
        int unique = 0;
        for (int i = 0; i < sourceKeys.length; i++) {
            long key = sourceKeys[i];
            if (i > 0 && key < sourceKeys[i - 1]) {
                throw new IllegalArgumentException("source keys must be sorted");
            }
            if (unique > 0 && batchKeys[unique - 1] == key) {
                batchValues[unique - 1] = sourceValues[i];
            } else {
                batchKeys[unique] = key;
                batchValues[unique] = sourceValues[i];
                unique++;
            }
        }
        return unique;
    }

    private void merge(long[] batchKeys, long[] batchValues, int unique) {
        int capacity = Math.max(keys.length, size + unique);
        long[] mergedKeys = new long[capacity];
        long[] mergedValues = new long[capacity];
        int left = 0;
        int right = 0;
        int write = 0;
        while (left < size && right < unique) {
            long existing = keys[left];
            long incoming = batchKeys[right];
            if (existing < incoming) {
                mergedKeys[write] = existing;
                mergedValues[write++] = values[left++];
            } else {
                if (existing == incoming) {
                    left++;
                }
                mergedKeys[write] = incoming;
                mergedValues[write++] = batchValues[right++];
            }
        }
        while (left < size) {
            mergedKeys[write] = keys[left];
            mergedValues[write++] = values[left++];
        }
        while (right < unique) {
            mergedKeys[write] = batchKeys[right];
            mergedValues[write++] = batchValues[right++];
        }
        keys = mergedKeys;
        values = mergedValues;
        size = write;
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
}
