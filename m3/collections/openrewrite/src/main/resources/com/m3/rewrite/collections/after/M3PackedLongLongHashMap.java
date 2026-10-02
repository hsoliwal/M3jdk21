/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.PackedLongLongHashMap
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0.
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongUnaryOperator;

/**
 * Open-addressed primitive long-to-long map with flat primitive retained state.
 *
 * <p>This is an explicit Route-A M3 primitive API. It is not a Map&lt;Long,Long&gt; replacement and
 * makes no claim that generic Java collection boundaries eliminate boxing.</p>
 */
public final class M3PackedLongLongHashMap implements M3LongLongMap {
    private static final byte EMPTY = M3PackedHashAtoms.EMPTY;
    private static final byte OCCUPIED = M3PackedHashAtoms.OCCUPIED;
    private static final byte DELETED = M3PackedHashAtoms.DELETED;
    private static final long[] EMPTY_LONGS = new long[0];
    private static final byte[] EMPTY_STATES = new byte[0];

    private long[] keys;
    private long[] values;
    private byte[] states;
    private int size;
    private int used;
    private int reservedCapacity;

    public M3PackedLongLongHashMap() {
        this(8);
    }

    public M3PackedLongLongHashMap(int expectedSize) {
        int capacity = M3PackedSupport.tableSizeForExpected(expectedSize);
        keys = EMPTY_LONGS;
        values = EMPTY_LONGS;
        states = EMPTY_STATES;
        reservedCapacity = capacity;
    }

    @Override
    public boolean put(long key, long value) {
        allocateIfNeeded();
        if (M3PackedHashAtoms.pressure(used, reservedCapacity)) {
            int existing = M3PackedHashAtoms.findIndex(keys, states, key);
            if (existing >= 0) {
                values[existing] = value;
                return false;
            }
            rehash(M3PackedHashAtoms.capacityAfterPressure(size, keys.length));
        }
        int slot = M3PackedHashAtoms.findOrInsertionIndex(keys, states, key);
        if (slot >= 0) {
            values[slot] = value;
            return false;
        }
        int target = ~slot;
        boolean reusedDeleted = states[target] == DELETED;
        keys[target] = key;
        values[target] = value;
        states[target] = OCCUPIED;
        size++;
        if (!reusedDeleted) {
            used++;
        }
        return true;
    }

    @Override
    public boolean containsKey(long key) {
        return findIndex(key) >= 0;
    }

    @Override
    public long getOrDefault(long key, long defaultValue) {
        int index = findIndex(key);
        return index < 0 ? defaultValue : values[index];
    }

    @Override
    public long getOrThrow(long key) {
        int index = findIndex(key);
        if (index < 0) {
            throw new IllegalArgumentException("missing key: " + key);
        }
        return values[index];
    }

    public boolean replace(long key, long expectedValue, long newValue) {
        int index = findIndex(key);
        if (index < 0 || values[index] != expectedValue) {
            return false;
        }
        values[index] = newValue;
        return true;
    }

    public long computeIfAbsent(long key, LongUnaryOperator mapping) {
        Objects.requireNonNull(mapping, "mapping");
        int index = findIndex(key);
        if (index >= 0) {
            return values[index];
        }
        long value = mapping.applyAsLong(key);
        put(key, value);
        return value;
    }

    public long addTo(long key, long delta, long initialValue) {
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
        int index = findIndex(key);
        if (index < 0) {
            return false;
        }
        states[index] = DELETED;
        size--;
        compactAfterRemoval();
        return true;
    }

    public boolean remove(long key, long expectedValue) {
        int index = findIndex(key);
        if (index < 0 || values[index] != expectedValue) {
            return false;
        }
        states[index] = DELETED;
        size--;
        compactAfterRemoval();
        return true;
    }

    @Override
    public int filterInPlace(M3LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;
        for (int index = 0; index < states.length; index++) {
            if (states[index] == OCCUPIED && !predicate.test(keys[index], values[index])) {
                states[index] = DELETED;
                size--;
            }
        }
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            used = 0;
        } else if (size != before) {
            rehash(M3PackedSupport.tableSizeForExpected(size));
        }
        return before - size;
    }

    @Override
    public void forEach(M3LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int index = 0; index < states.length; index++) {
            if (states[index] == OCCUPIED) {
                consumer.accept(keys[index], values[index]);
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
        return reservedCapacity;
    }

    @Override
    public void clear() {
        Arrays.fill(states, EMPTY);
        size = 0;
        used = 0;
    }

    @Override
    public void trimToSize() {
        int target = M3PackedSupport.tableSizeForExpected(size);
        if (size == 0) {
            keys = EMPTY_LONGS;
            values = EMPTY_LONGS;
            states = EMPTY_STATES;
            reservedCapacity = target;
            used = 0;
        } else if (target != reservedCapacity || used != size) {
            rehash(target);
        }
    }

    @Override
    public long[] keysToArray() {
        long[] result = new long[size];
        int write = 0;
        for (int index = 0; index < states.length; index++) {
            if (states[index] == OCCUPIED) {
                result[write++] = keys[index];
            }
        }
        return result;
    }

    @Override
    public long[] valuesToArray() {
        long[] result = new long[size];
        int write = 0;
        for (int index = 0; index < states.length; index++) {
            if (states[index] == OCCUPIED) {
                result[write++] = values[index];
            }
        }
        return result;
    }

    private int findIndex(long key) {
        return size == 0 ? -1 : M3PackedHashAtoms.findIndex(keys, states, key);
    }

    private void compactAfterRemoval() {
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            used = 0;
        } else if (M3PackedHashAtoms.compactTombstones(size, used)) {
            rehash(keys.length);
        }
    }

    private void allocateIfNeeded() {
        if (keys.length != 0) {
            return;
        }
        long[] newKeys = new long[reservedCapacity];
        long[] newValues = new long[reservedCapacity];
        byte[] newStates = new byte[reservedCapacity];
        keys = newKeys;
        values = newValues;
        states = newStates;
    }

    private void rehash(int capacity) {
        long[] oldKeys = keys;
        long[] oldValues = values;
        byte[] oldStates = states;
        keys = new long[capacity];
        values = new long[capacity];
        states = new byte[capacity];
        reservedCapacity = capacity;
        size = 0;
        used = 0;
        for (int index = 0; index < oldStates.length; index++) {
            if (oldStates[index] == OCCUPIED) {
                putRehashed(oldKeys[index], oldValues[index]);
            }
        }
    }

    private void putRehashed(long key, long value) {
        int index = M3PackedHashAtoms.rehashInsertionIndex(keys, states, key);
        keys[index] = key;
        values[index] = value;
        states[index] = OCCUPIED;
        size++;
        used++;
    }
}
