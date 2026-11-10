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

import java.util.Arrays;
import java.util.Objects;

/**
 * Sorted sparse long-to-byte map backed by parallel long[] and byte[] lanes.
 *
 * <p>Keys remain sorted. Lookups are binary search; insertion/removal is a
 * bounded pair of array copies. Sorted batches merge linearly. This is intended
 * for read-heavy sparse tables where hash capacity slack is less attractive
 * than compact ordered storage.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongByteSortedMap.java}
 * blob {@code 7f13cc94eeb18082adf0dcd1ec0c65808c759abc} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongByteSortedMap {
    @FunctionalInterface
    public interface Consumer {
        void accept(long key, byte value);
    }

    private long[] keys;
    private byte[] values;
    private int size;

    public LongByteSortedMap() {
        this(8);
    }

    public LongByteSortedMap(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        int capacity = Math.max(1, initialCapacity);
        keys = new long[capacity];
        values = new byte[capacity];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return keys.length; }
    public long payloadBytes() { return (long) keys.length * Long.BYTES + values.length; }

    public boolean containsKey(long key) {
        return Arrays.binarySearch(keys, 0, size, key) >= 0;
    }

    public byte get(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        return index < 0 ? 0 : values[index];
    }

    public int getUnsigned(long key) {
        return Byte.toUnsignedInt(get(key));
    }

    public byte getOrDefault(long key, byte defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        return index < 0 ? defaultValue : values[index];
    }

    public byte put(long key, byte value) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) {
            byte old = values[index];
            values[index] = value;
            return old;
        }

        int insertion = -index - 1;
        ensure(size + 1);
        System.arraycopy(keys, insertion, keys, insertion + 1, size - insertion);
        System.arraycopy(values, insertion, values, insertion + 1, size - insertion);
        keys[insertion] = key;
        values[insertion] = value;
        size++;
        return 0;
    }

    public byte putUnsigned(long key, int value) {
        if ((value & ~0xff) != 0) throw new IllegalArgumentException("value must be 0..255");
        return put(key, (byte) value);
    }

    /**
     * Linear-time batch merge. Incoming keys must be strictly increasing.
     * Existing keys are replaced by the incoming value without creating entries.
     *
     * @return number of newly inserted keys
     */
    public int putAllSorted(long[] incomingKeys, byte[] incomingValues) {
        Objects.requireNonNull(incomingKeys, "incomingKeys");
        Objects.requireNonNull(incomingValues, "incomingValues");
        if (incomingKeys.length != incomingValues.length) {
            throw new IllegalArgumentException("key/value length mismatch");
        }
        validateStrictlyIncreasing(incomingKeys);
        if (incomingKeys.length == 0) return 0;

        int oldSize = size;
        int capacity = grownCapacity(size + incomingKeys.length);
        long[] mergedKeys = new long[capacity];
        byte[] mergedValues = new byte[capacity];
        int left = 0;
        int right = 0;
        int out = 0;

        while (left < size && right < incomingKeys.length) {
            long currentKey = keys[left];
            long incomingKey = incomingKeys[right];
            if (currentKey < incomingKey) {
                mergedKeys[out] = currentKey;
                mergedValues[out++] = values[left++];
            } else if (currentKey > incomingKey) {
                mergedKeys[out] = incomingKey;
                mergedValues[out++] = incomingValues[right++];
            } else {
                mergedKeys[out] = incomingKey;
                mergedValues[out++] = incomingValues[right++];
                left++;
            }
        }
        while (left < size) {
            mergedKeys[out] = keys[left];
            mergedValues[out++] = values[left++];
        }
        while (right < incomingKeys.length) {
            mergedKeys[out] = incomingKeys[right];
            mergedValues[out++] = incomingValues[right++];
        }

        keys = mergedKeys;
        values = mergedValues;
        size = out;
        return size - oldSize;
    }

    public byte remove(long key) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index < 0) return 0;
        byte old = values[index];
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(keys, index + 1, keys, index, moved);
            System.arraycopy(values, index + 1, values, index, moved);
        }
        size--;
        keys[size] = 0L;
        values[size] = 0;
        return old;
    }

    public long firstKey() {
        if (size == 0) throw new java.util.NoSuchElementException();
        return keys[0];
    }

    public long lastKey() {
        if (size == 0) throw new java.util.NoSuchElementException();
        return keys[size - 1];
    }

    public long floorKeyOrDefault(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) return keys[index];
        int insertion = -index - 1;
        return insertion == 0 ? defaultValue : keys[insertion - 1];
    }

    public long ceilingKeyOrDefault(long key, long defaultValue) {
        int index = Arrays.binarySearch(keys, 0, size, key);
        if (index >= 0) return keys[index];
        int insertion = -index - 1;
        return insertion == size ? defaultValue : keys[insertion];
    }

    public void clear() {
        Arrays.fill(keys, 0, size, 0L);
        Arrays.fill(values, 0, size, (byte) 0);
        size = 0;
    }

    public void forEach(Consumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) consumer.accept(keys[i], values[i]);
    }

    public long[] keysArray() {
        return Arrays.copyOf(keys, size);
    }

    public byte[] valuesArray() {
        return Arrays.copyOf(values, size);
    }

    private void ensure(int required) {
        if (required <= keys.length) return;
        int capacity = grownCapacity(required);
        keys = Arrays.copyOf(keys, capacity);
        values = Arrays.copyOf(values, capacity);
    }

    private int grownCapacity(int required) {
        if (required <= keys.length) return keys.length;
        int capacity = keys.length + (keys.length >>> 1) + 1;
        return Math.max(required, capacity);
    }

    private static void validateStrictlyIncreasing(long[] source) {
        for (int i = 1; i < source.length; i++) {
            if (source[i - 1] >= source[i]) {
                throw new IllegalArgumentException("incoming keys must be strictly increasing");
            }
        }
    }
}
