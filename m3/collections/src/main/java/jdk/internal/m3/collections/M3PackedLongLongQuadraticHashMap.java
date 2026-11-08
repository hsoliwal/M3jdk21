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

/**
 * Primitive {@code long -> long} map using triangular/quadratic probing.
 *
 * <p>Retained per slot: {@code long key + long value + byte state} (17 payload bytes).
 * All long keys and values, including zero, are stored directly.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongQuadraticHashMap.java}
 * blob {@code 3b0342660f3e3e34d8996f495b02ad3991ed77cd} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongLongQuadraticHashMap implements M3PackedLongLongMap {
    private static final byte EMPTY = M3PackedQuadraticProbeAtoms.EMPTY;
    private static final byte OCCUPIED = M3PackedQuadraticProbeAtoms.OCCUPIED;
    private static final byte DELETED = M3PackedQuadraticProbeAtoms.DELETED;

    public static final float DEFAULT_LOAD_FACTOR = 0.65f;

    private long[] keys;
    private long[] values;
    private byte[] states;
    private int size;
    private int used;
    private final float loadFactor;
    private int threshold;

    public M3PackedLongLongQuadraticHashMap() {
        this(8, DEFAULT_LOAD_FACTOR);
    }

    public M3PackedLongLongQuadraticHashMap(int expectedSize) {
        this(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    public M3PackedLongLongQuadraticHashMap(int expectedSize, float loadFactor) {
        this.loadFactor = M3PackedQuadraticProbeAtoms.validateLoadFactor(loadFactor);
        int capacity = M3PackedSupport.tableSizeForExpected(expectedSize, loadFactor);
        keys = new long[capacity];
        values = new long[capacity];
        states = new byte[capacity];
        threshold = M3PackedQuadraticProbeAtoms.threshold(capacity, loadFactor);
    }

    @Override
    public boolean put(long key, long value) {
        if (used + 1 > threshold) {
            int existing = findIndex(key);
            if (existing >= 0) {
                values[existing] = value;
                return false;
            }
            int capacity = size + 1 <= threshold
                    ? keys.length
                    : M3PackedSupport.nextTableCapacity(keys.length);
            rehash(capacity);
        }

        int slot = M3PackedQuadraticProbeAtoms.findOrInsertionIndex(keys, states, key);
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

    @Override
    public int filterInPlace(M3LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED && !predicate.test(keys[i], values[i])) {
                states[i] = DELETED;
                size--;
            }
        }
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            used = 0;
        } else if (size != before) {
            rehash(M3PackedSupport.tableSizeForExpected(size, loadFactor));
        }
        return before - size;
    }

    @Override
    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED) {
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
        Arrays.fill(states, EMPTY);
        size = 0;
        used = 0;
    }

    @Override
    public void trimToSize() {
        int target = M3PackedSupport.tableSizeForExpected(size, loadFactor);
        if (target != keys.length || used != size) {
            rehash(target);
        }
    }

    @Override
    public long[] keysToArray() {
        long[] result = new long[size];
        int write = 0;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED) {
                result[write++] = keys[i];
            }
        }
        return result;
    }

    @Override
    public long[] valuesToArray() {
        long[] result = new long[size];
        int write = 0;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED) {
                result[write++] = values[i];
            }
        }
        return result;
    }

    private int findIndex(long key) {
        return M3PackedQuadraticProbeAtoms.findIndex(keys, states, key);
    }

    private void compactAfterRemoval() {
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            used = 0;
        } else if (M3PackedHashAtoms.compactTombstones(size, used)) {
            rehash(keys.length);
        }
    }

    private void rehash(int capacity) {
        long[] oldKeys = keys;
        long[] oldValues = values;
        byte[] oldStates = states;
        keys = new long[capacity];
        values = new long[capacity];
        states = new byte[capacity];
        threshold = M3PackedQuadraticProbeAtoms.threshold(capacity, loadFactor);
        size = 0;
        used = 0;

        for (int i = 0; i < oldStates.length; i++) {
            if (oldStates[i] != OCCUPIED) {
                continue;
            }
            long key = oldKeys[i];
            int index = M3PackedQuadraticProbeAtoms.rehashInsertionIndex(keys, states, key);
            keys[index] = key;
            values[index] = oldValues[i];
            states[index] = OCCUPIED;
            size++;
            used++;
        }
    }

}
