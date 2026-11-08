/*******************************************************************************
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
import java.util.function.LongUnaryOperator;

/**
 * Memory-minimal primitive {@code long -> long} hash map.
 *
 * <p>Uses sentinel-empty linear probing and backward-shift deletion. Key {@code 0L}
 * is stored in collection-level fields, eliminating the per-slot state byte.
 *
 * <p>Retained per hash slot: {@code long key + long value} = 16 bytes payload.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongCompactHashMap.java}
 * blob {@code 8bb3b7f96f36fedd71ba8f171a29e9bb6851410c} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongLongCompactHashMap implements M3PackedLongLongMap {
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private long[] keys;
    private long[] values;
    private int size;
    private boolean hasZeroKey;
    private long zeroValue;
    private final float loadFactor;
    private int threshold;

    public M3PackedLongLongCompactHashMap() {
        this(8, DEFAULT_LOAD_FACTOR);
    }

    public M3PackedLongLongCompactHashMap(int expectedSize) {
        this(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    public M3PackedLongLongCompactHashMap(int expectedSize, float loadFactor) {
        this.loadFactor = validateLoadFactor(loadFactor);
        int capacity = M3PackedSupport.tableSizeForExpected(expectedSize, loadFactor);
        keys = new long[capacity];
        values = new long[capacity];
        threshold = threshold(capacity);
    }

    @Override
    public boolean put(long key, long value) {
        if (key == 0L) {
            boolean inserted = !hasZeroKey;
            hasZeroKey = true;
            zeroValue = value;
            if (inserted) {
                size++;
            }
            return inserted;
        }

        if (nonZeroSize() + 1 > threshold) {
            rehash(M3PackedSupport.nextTableCapacity(keys.length));
        }

        int mask = keys.length - 1;
        int index = M3PackedSupport.mix(key) & mask;
        while (true) {
            long existing = keys[index];
            if (existing == 0L) {
                keys[index] = key;
                values[index] = value;
                size++;
                return true;
            }
            if (existing == key) {
                values[index] = value;
                return false;
            }
            index = (index + 1) & mask;
        }
    }

    @Override
    public boolean containsKey(long key) {
        return key == 0L ? hasZeroKey : findIndex(key) >= 0;
    }

    @Override
    public long getOrDefault(long key, long defaultValue) {
        if (key == 0L) {
            return hasZeroKey ? zeroValue : defaultValue;
        }
        int index = findIndex(key);
        return index < 0 ? defaultValue : values[index];
    }

    @Override
    public long getOrThrow(long key) {
        if (key == 0L) {
            if (!hasZeroKey) {
                throw new IllegalArgumentException("missing key: 0");
            }
            return zeroValue;
        }
        int index = findIndex(key);
        if (index < 0) {
            throw new IllegalArgumentException("missing key: " + key);
        }
        return values[index];
    }

    public boolean replace(long key, long expectedValue, long newValue) {
        if (key == 0L) {
            if (!hasZeroKey || zeroValue != expectedValue) {
                return false;
            }
            zeroValue = newValue;
            return true;
        }
        int index = findIndex(key);
        if (index < 0 || values[index] != expectedValue) {
            return false;
        }
        values[index] = newValue;
        return true;
    }

    public long computeIfAbsent(long key, LongUnaryOperator mapping) {
        Objects.requireNonNull(mapping, "mapping");
        if (containsKey(key)) {
            return getOrThrow(key);
        }
        long value = mapping.applyAsLong(key);
        put(key, value);
        return value;
    }

    public long addTo(long key, long delta, long initialValue) {
        if (key == 0L) {
            if (hasZeroKey) {
                zeroValue += delta;
                return zeroValue;
            }
            long value = initialValue + delta;
            put(0L, value);
            return value;
        }

        int index = findIndex(key);
        if (index >= 0) {
            values[index] += delta;
            return values[index];
        }
        long value = initialValue + delta;
        put(key, value);
        return value;
    }

    @Override
    public boolean remove(long key) {
        if (key == 0L) {
            if (!hasZeroKey) {
                return false;
            }
            hasZeroKey = false;
            zeroValue = 0L;
            size--;
            return true;
        }

        int index = findIndex(key);
        if (index < 0) {
            return false;
        }
        shiftKeys(index);
        size--;
        return true;
    }

    @Override
    public int filterInPlace(M3LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;

        if (hasZeroKey && !predicate.test(0L, zeroValue)) {
            hasZeroKey = false;
            zeroValue = 0L;
            size--;
        }

        for (int slot = 0; slot < keys.length; ) {
            long key = keys[slot];
            if (key != 0L && !predicate.test(key, values[slot])) {
                shiftKeys(slot);
                size--;
            } else {
                slot++;
            }
        }
        return before - size;
    }

    @Override
    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (hasZeroKey) {
            consumer.accept(0L, zeroValue);
        }
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != 0L) {
                consumer.accept(keys[i], values[i]);
            }
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int capacity() {
        return keys.length;
    }

    public float loadFactor() {
        return loadFactor;
    }

    @Override
    public void clear() {
        Arrays.fill(keys, 0L);
        Arrays.fill(values, 0L);
        hasZeroKey = false;
        zeroValue = 0L;
        size = 0;
    }

    @Override
    public void trimToSize() {
        int target = M3PackedSupport.tableSizeForExpected(nonZeroSize(), loadFactor);
        if (target < keys.length) {
            rehash(target);
        }
    }

    @Override
    public long[] keysToArray() {
        long[] result = new long[size];
        int write = 0;
        if (hasZeroKey) {
            result[write++] = 0L;
        }
        for (long key : keys) {
            if (key != 0L) {
                result[write++] = key;
            }
        }
        return result;
    }

    @Override
    public long[] valuesToArray() {
        long[] result = new long[size];
        int write = 0;
        if (hasZeroKey) {
            result[write++] = zeroValue;
        }
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != 0L) {
                result[write++] = values[i];
            }
        }
        return result;
    }

    private int findIndex(long key) {
        int mask = keys.length - 1;
        int index = M3PackedSupport.mix(key) & mask;
        while (true) {
            long existing = keys[index];
            if (existing == 0L) {
                return -1;
            }
            if (existing == key) {
                return index;
            }
            index = (index + 1) & mask;
        }
    }

    private void shiftKeys(int gap) {
        int mask = keys.length - 1;
        int last;
        int slot;
        long current;

        while (true) {
            gap = ((last = gap) + 1) & mask;
            while (true) {
                current = keys[gap];
                if (current == 0L) {
                    keys[last] = 0L;
                    values[last] = 0L;
                    return;
                }
                slot = M3PackedSupport.mix(current) & mask;
                if (last <= gap
                        ? last >= slot || slot > gap
                        : last >= slot && slot > gap) {
                    break;
                }
                gap = (gap + 1) & mask;
            }
            keys[last] = current;
            values[last] = values[gap];
        }
    }

    private void rehash(int capacity) {
        long[] oldKeys = keys;
        long[] oldValues = values;
        keys = new long[capacity];
        values = new long[capacity];
        threshold = threshold(capacity);

        int mask = capacity - 1;
        for (int i = 0; i < oldKeys.length; i++) {
            long key = oldKeys[i];
            if (key == 0L) {
                continue;
            }
            int index = M3PackedSupport.mix(key) & mask;
            while (keys[index] != 0L) {
                index = (index + 1) & mask;
            }
            keys[index] = key;
            values[index] = oldValues[i];
        }
    }

    private int nonZeroSize() {
        return size - (hasZeroKey ? 1 : 0);
    }

    private int threshold(int capacity) {
        return Math.min(capacity - 1, Math.max(1, (int) Math.ceil(capacity * loadFactor)));
    }

    private static float validateLoadFactor(float loadFactor) {
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor");
        }
        return loadFactor;
    }
}
