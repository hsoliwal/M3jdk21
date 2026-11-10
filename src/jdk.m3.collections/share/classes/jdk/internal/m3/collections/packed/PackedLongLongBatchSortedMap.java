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
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongLongConsumer;
import jdk.m3.collections.LongLongPredicate;
import jdk.m3.collections.LongLongSortedMap;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Sorted long-to-long map with bounded unsorted pending lanes and periodic in-place merge.
 *
 * <p>Hot new-key insertion appends to primitive pending lanes. Ordered operations flush by sorting
 * key/value lanes together and backward-merging into the dense sorted lanes. No mapping object is
 * retained. Pending lookup is O(batchSize); dense lookup is O(log n).
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongBatchSortedMap.java}
 * blob {@code 4a70383f7d2e73e00893f00e8061c4f269182beb} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (U1 batch (v6): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongBatchSortedMap implements LongLongSortedMap {
    private long[] sortedKeys;
    private long[] sortedValues;
    private int sortedSize;
    private long[] pendingKeys;
    private long[] pendingValues;
    private int pendingSize;
    private boolean pendingOrdered = true;
    private final int batchSize;
    private final LongLongPredicate admission;
    private int callbacks;

    public PackedLongLongBatchSortedMap(int expectedSize, int batchSize) {
        this(expectedSize, batchSize, (key, value) -> true);
    }

    public PackedLongLongBatchSortedMap(
            int expectedSize,
            int batchSize,
            LongLongPredicate admission) {
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize");
        if (batchSize <= 0) throw new IllegalArgumentException("batchSize");
        this.batchSize = batchSize;
        this.admission = Objects.requireNonNull(admission, "admission");
        sortedKeys = new long[Math.max(1, expectedSize)];
        sortedValues = new long[sortedKeys.length];
        pendingKeys = new long[Math.max(1, batchSize)];
        pendingValues = new long[pendingKeys.length];
    }

    public int batchSize() { return batchSize; }
    public int pendingSize() { return pendingSize; }
    public int sortedSize() { return sortedSize; }
    public long backingArrayPayloadBytes() {
        return 16L * (sortedKeys.length + pendingKeys.length);
    }
    @Override public int size() { return sortedSize + pendingSize; }
    @Override public boolean isEmpty() { return size() == 0; }

    @Override
    public boolean put(long key, long value) {
        writable();
        if (!admit(key, value)) return false;
        int sorted = Arrays.binarySearch(sortedKeys, 0, sortedSize, key);
        if (sorted >= 0) { sortedValues[sorted] = value; return false; }
        int pending = pendingIndex(key);
        if (pending >= 0) { pendingValues[pending] = value; return false; }

        ensurePendingCapacity(PackedArrayCapacity.required(pendingSize, 1));
        if (pendingSize > 0 && pendingKeys[pendingSize - 1] > key) pendingOrdered = false;
        pendingKeys[pendingSize] = key; pendingValues[pendingSize] = value; pendingSize++;
        if (pendingSize >= batchSize) flush();
        return true;
    }

    public int putAll(long[] keys, long[] values) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(values, "values");
        if (keys.length != values.length) throw new IllegalArgumentException("key/value length mismatch");
        int added = 0;
        for (int i = 0; i < keys.length; i++) if (put(keys[i], values[i])) added++;
        return added;
    }

    @Override public boolean containsKey(long key) { return findSorted(key) >= 0 || pendingIndex(key) >= 0; }
    @Override public long getOrDefault(long key, long fallback) {
        int sorted = findSorted(key);
        if (sorted >= 0) return sortedValues[sorted];
        int pending = pendingIndex(key);
        return pending < 0 ? fallback : pendingValues[pending];
    }
    @Override public long getOrThrow(long key) {
        int sorted = findSorted(key);
        if (sorted >= 0) return sortedValues[sorted];
        int pending = pendingIndex(key);
        if (pending < 0) throw new IllegalArgumentException("missing key: " + key);
        return pendingValues[pending];
    }

    @Override
    public boolean remove(long key) {
        writable();
        int sorted = findSorted(key);
        if (sorted >= 0) {
            int moved = sortedSize - sorted - 1;
            if (moved > 0) {
                System.arraycopy(sortedKeys, sorted + 1, sortedKeys, sorted, moved);
                System.arraycopy(sortedValues, sorted + 1, sortedValues, sorted, moved);
            }
            sortedSize--; return true;
        }
        int pending = pendingIndex(key);
        if (pending < 0) return false;
        int moved = pendingSize - pending - 1;
        if (moved > 0) {
            System.arraycopy(pendingKeys, pending + 1, pendingKeys, pending, moved);
            System.arraycopy(pendingValues, pending + 1, pendingValues, pending, moved);
        }
        pendingSize--;
        if (pendingSize < 2) pendingOrdered = true;
        return true;
    }

    /** Sorts pending key/value lanes together and backward-merges them into sorted storage. */
    public void flush() {
        if (pendingSize == 0) return;
        writable();
        if (!pendingOrdered) sortPending();
        int required = PackedArrayCapacity.required(sortedSize, pendingSize);
        ensureSortedCapacity(required);

        int left = sortedSize - 1, right = pendingSize - 1, write = required - 1;
        while (left >= 0 && right >= 0) {
            long a = sortedKeys[left], b = pendingKeys[right];
            if (a > b) {
                sortedKeys[write] = a; sortedValues[write--] = sortedValues[left--];
            } else if (b > a) {
                sortedKeys[write] = b; sortedValues[write--] = pendingValues[right--];
            } else {
                sortedKeys[write] = b; sortedValues[write--] = pendingValues[right--]; left--;
            }
        }
        while (right >= 0) {
            sortedKeys[write] = pendingKeys[right];
            sortedValues[write--] = pendingValues[right--];
        }
        sortedSize = required;
        pendingSize = 0;
        pendingOrdered = true;
    }

    @Override public long firstKey() {
        flush(); if (sortedSize == 0) throw new NoSuchElementException(); return sortedKeys[0];
    }
    @Override public long lastKey() {
        flush(); if (sortedSize == 0) throw new NoSuchElementException(); return sortedKeys[sortedSize - 1];
    }
    @Override public long floorKey(long key, long fallback) {
        flush(); int found = Arrays.binarySearch(sortedKeys, 0, sortedSize, key);
        int candidate = found >= 0 ? found : -found - 2;
        return candidate < 0 ? fallback : sortedKeys[candidate];
    }
    @Override public long ceilingKey(long key, long fallback) {
        flush(); int found = Arrays.binarySearch(sortedKeys, 0, sortedSize, key);
        int candidate = found >= 0 ? found : -found - 1;
        return candidate >= sortedSize ? fallback : sortedKeys[candidate];
    }
    @Override public long lowerKey(long key, long fallback) {
        flush(); int found = Arrays.binarySearch(sortedKeys, 0, sortedSize, key);
        int candidate = found >= 0 ? found - 1 : -found - 2;
        return candidate < 0 ? fallback : sortedKeys[candidate];
    }
    @Override public long higherKey(long key, long fallback) {
        flush(); int found = Arrays.binarySearch(sortedKeys, 0, sortedSize, key);
        int candidate = found >= 0 ? found + 1 : -found - 1;
        return candidate >= sortedSize ? fallback : sortedKeys[candidate];
    }

    @Override
    public int filterInPlace(LongLongPredicate predicate) {
        writable(); Objects.requireNonNull(predicate, "predicate"); flush();
        long[] keys = Arrays.copyOf(sortedKeys, sortedSize);
        long[] values = Arrays.copyOf(sortedValues, sortedSize);
        int write = 0; callbacks++;
        try {
            for (int read = 0; read < keys.length; read++) {
                if (predicate.test(keys[read], values[read])) {
                    keys[write] = keys[read]; values[write++] = values[read];
                }
            }
        } finally { callbacks--; }
        int removed = sortedSize - write;
        if (removed != 0) {
            System.arraycopy(keys, 0, sortedKeys, 0, write);
            System.arraycopy(values, 0, sortedValues, 0, write);
            sortedSize = write;
        }
        return removed;
    }

    @Override
    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); flush(); callbacks++;
        try {
            for (int i = 0; i < sortedSize; i++) consumer.accept(sortedKeys[i], sortedValues[i]);
        } finally { callbacks--; }
    }

    @Override public void clear() {
        writable(); sortedSize = 0; pendingSize = 0; pendingOrdered = true;
    }

    public void trimToSize() {
        writable(); flush();
        sortedKeys = Arrays.copyOf(sortedKeys, Math.max(1, sortedSize));
        sortedValues = Arrays.copyOf(sortedValues, sortedKeys.length);
        pendingKeys = new long[Math.max(1, batchSize)];
        pendingValues = new long[pendingKeys.length];
    }

    @Override public long[] keysToArray() {
        flush(); return Arrays.copyOf(sortedKeys, sortedSize);
    }
    @Override public long[] valuesToArray() {
        flush(); return Arrays.copyOf(sortedValues, sortedSize);
    }

    private boolean admit(long key, long value) {
        boolean accepted; callbacks++;
        try { accepted = admission.test(key, value); } finally { callbacks--; }
        return accepted;
    }
    private void writable() {
        if (callbacks != 0) throw new IllegalStateException("mutation during callback");
    }
    private int findSorted(long key) {
        return Arrays.binarySearch(sortedKeys, 0, sortedSize, key);
    }
    private int pendingIndex(long key) {
        for (int i = 0; i < pendingSize; i++) if (pendingKeys[i] == key) return i;
        return -1;
    }

    private void ensureSortedCapacity(int required) {
        if (required <= sortedKeys.length) return;
        int capacity = PackedFlatArrays.growthCapacity(sortedKeys.length, required);
        long[] keys = Arrays.copyOf(sortedKeys, capacity);
        long[] values = Arrays.copyOf(sortedValues, capacity);
        sortedKeys = keys; sortedValues = values;
    }
    private void ensurePendingCapacity(int required) {
        if (required <= pendingKeys.length) return;
        int capacity = PackedFlatArrays.growthCapacity(pendingKeys.length, required);
        long[] keys = Arrays.copyOf(pendingKeys, capacity);
        long[] values = Arrays.copyOf(pendingValues, capacity);
        pendingKeys = keys; pendingValues = values;
    }

    private void sortPending() {
        for (int parent = (pendingSize >>> 1) - 1; parent >= 0; parent--) siftDown(parent, pendingSize);
        for (int end = pendingSize - 1; end > 0; end--) {
            swap(0, end); siftDown(0, end);
        }
    }
    private void siftDown(int index, int size) {
        long key = pendingKeys[index], value = pendingValues[index];
        int half = size >>> 1;
        while (index < half) {
            int child = (index << 1) + 1, right = child + 1;
            if (right < size && pendingKeys[right] > pendingKeys[child]) child = right;
            if (key >= pendingKeys[child]) break;
            pendingKeys[index] = pendingKeys[child];
            pendingValues[index] = pendingValues[child];
            index = child;
        }
        pendingKeys[index] = key; pendingValues[index] = value;
    }
    private void swap(int left, int right) {
        long key = pendingKeys[left]; pendingKeys[left] = pendingKeys[right]; pendingKeys[right] = key;
        long value = pendingValues[left]; pendingValues[left] = pendingValues[right]; pendingValues[right] = value;
    }

    void checkInvariants() {
        if (sortedKeys.length != sortedValues.length || pendingKeys.length != pendingValues.length
                || sortedSize < 0 || sortedSize > sortedKeys.length
                || pendingSize < 0 || pendingSize > pendingKeys.length) {
            throw new IllegalStateException("invalid batch-map lanes");
        }
        for (int i = 1; i < sortedSize; i++) {
            if (sortedKeys[i - 1] >= sortedKeys[i]) throw new IllegalStateException("sorted order");
        }
        for (int i = 0; i < pendingSize; i++) {
            if (Arrays.binarySearch(sortedKeys, 0, sortedSize, pendingKeys[i]) >= 0) {
                throw new IllegalStateException("pending/sorted overlap");
            }
            for (int j = i + 1; j < pendingSize; j++) {
                if (pendingKeys[i] == pendingKeys[j]) throw new IllegalStateException("pending duplicate");
            }
        }
    }
}
