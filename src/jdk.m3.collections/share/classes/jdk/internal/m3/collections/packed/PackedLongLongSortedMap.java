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
 * Sorted primitive {@code long -> long} map using parallel sorted arrays.
 *
 * <p>No tree nodes and no retained entry objects. Single updates use binary search
 * plus arraycopy; sorted batches use one merge pass and incoming values win on duplicate keys.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongSortedMap.java}
 * blob {@code c37609d444f6a3200c018604719e2246cadc65a0} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (U1 batch (v6): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongSortedMap implements LongLongSortedMap {
    private long[] keys;
    private long[] values;
    private int size;

    public PackedLongLongSortedMap() {
        this(8);
    }

    public PackedLongLongSortedMap(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        int capacity = Math.max(1, expectedSize);
        keys = new long[capacity];
        values = new long[capacity];
    }

    /**
     * @return true when a new key is inserted, false when an existing value is replaced
     */
    public boolean put(long key, long value) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) {
            values[index] = value;
            return false;
        }
        int insertionPoint = -index - 1;
        ensureCapacity(size + 1);
        System.arraycopy(keys, insertionPoint, keys, insertionPoint + 1, size - insertionPoint);
        System.arraycopy(values, insertionPoint, values, insertionPoint + 1, size - insertionPoint);
        keys[insertionPoint] = key;
        values[insertionPoint] = value;
        size++;
        return true;
    }

    public int putAll(long[] sourceKeys, long[] sourceValues) {
        Objects.requireNonNull(sourceKeys, "sourceKeys");
        Objects.requireNonNull(sourceValues, "sourceValues");
        if (sourceKeys.length != sourceValues.length) {
            throw new IllegalArgumentException("key/value length mismatch");
        }
        int before = size;
        for (int i = 0; i < sourceKeys.length; i++) {
            put(sourceKeys[i], sourceValues[i]);
        }
        return size - before;
    }

    /**
     * Batch path for nondecreasing keys. Duplicate incoming keys collapse with last value winning.
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

        int before = size;
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
            } else if (incoming < existing) {
                mergedKeys[write] = incoming;
                mergedValues[write++] = batchValues[right++];
            } else {
                mergedKeys[write] = incoming;
                mergedValues[write++] = batchValues[right++];
                left++;
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
        return size - before;
    }

    public boolean containsKey(long key) {
        return Arrays.binarySearch(keys, 0, size, key) >= 0;
    }

    public long getOrDefault(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        return index < 0 ? defaultValue : values[index];
    }

    public long getOrThrow(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index < 0) {
            throw new IllegalArgumentException("missing key: " + key);
        }
        return values[index];
    }

    public boolean remove(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index < 0) {
            return false;
        }
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(keys, index + 1, keys, index, moved);
            System.arraycopy(values, index + 1, values, index, moved);
        }
        size--;
        return true;
    }

    public long firstKey() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return keys[0];
    }

    public long lastKey() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return keys[size - 1];
    }

    public long floorKey(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) {
            return keys[index];
        }
        int insertionPoint = -index - 1;
        return insertionPoint == 0 ? defaultValue : keys[insertionPoint - 1];
    }

    public long ceilingKey(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) {
            return keys[index];
        }
        int insertionPoint = -index - 1;
        return insertionPoint == size ? defaultValue : keys[insertionPoint];
    }

    public long lowerKey(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        int candidate = index >= 0 ? index - 1 : -index - 2;
        return candidate < 0 ? defaultValue : keys[candidate];
    }

    public long higherKey(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        int candidate = index >= 0 ? index + 1 : -index - 1;
        return candidate >= size ? defaultValue : keys[candidate];
    }

    /** Returns key/value pairs in the half-open key range [fromInclusive, toExclusive). */
    public PackedLongPairs range(long fromInclusive, long toExclusive) {
        PackedLongPairs result = new PackedLongPairs();
        if (fromInclusive >= toExclusive || size == 0) {
            return result;
        }
        int start = Arrays.binarySearch(keys, 0, size, fromInclusive);
        start = start >= 0 ? start : -start - 1;
        int end = Arrays.binarySearch(keys, 0, size, toExclusive);
        end = end >= 0 ? end : -end - 1;
        for (int i = start; i < end; i++) {
            result.add(keys[i], values[i]);
        }
        return result;
    }

    public int filterInPlace(LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int write = 0;
        for (int read = 0; read < size; read++) {
            if (predicate.test(keys[read], values[read])) {
                keys[write] = keys[read];
                values[write] = values[read];
                write++;
            }
        }
        int removed = size - write;
        size = write;
        return removed;
    }

    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) {
            consumer.accept(keys[i], values[i]);
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return keys.length;
    }

    public void clear() {
        size = 0;
    }

    public void trimToSize() {
        int capacity = Math.max(1, size);
        keys = Arrays.copyOf(keys, capacity);
        values = Arrays.copyOf(values, capacity);
    }

    public long[] keysToArray() {
        return Arrays.copyOf(keys, size);
    }

    public long[] valuesToArray() {
        return Arrays.copyOf(values, size);
    }

    private void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity <= keys.length) {
            return;
        }
        int capacity = PackedSupport.growArrayCapacity(keys.length, minimumCapacity);
        keys = Arrays.copyOf(keys, capacity);
        values = Arrays.copyOf(values, capacity);
    }
}
