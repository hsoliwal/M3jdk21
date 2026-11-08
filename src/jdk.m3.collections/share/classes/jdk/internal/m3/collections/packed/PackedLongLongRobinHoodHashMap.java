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
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongLongConsumer;

import java.util.Arrays;
import java.util.Objects;
import jdk.m3.collections.LongLongPredicate;

/**
 * Primitive {@code long -> long} Robin-Hood hash map with backward-shift deletion.
 *
 * <p>Retained per slot: parallel long key/value lanes plus one occupancy byte.
 * There are no tombstones and misses can terminate early based on probe distance.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongRobinHoodHashMap.java}
 * blob {@code 82404e3f40b2c4fde1d68407d4ae12f2c5f107c7} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongRobinHoodHashMap implements PackedLongLongMap {
    private static final byte EMPTY = PackedRobinHoodProbeAtoms.EMPTY;
    private static final byte OCCUPIED = PackedRobinHoodProbeAtoms.OCCUPIED;

    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private long[] keys;
    private long[] values;
    private byte[] states;
    private int size;
    private final float loadFactor;
    private int threshold;

    public PackedLongLongRobinHoodHashMap() {
        this(8, DEFAULT_LOAD_FACTOR);
    }

    public PackedLongLongRobinHoodHashMap(int expectedSize) {
        this(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    public PackedLongLongRobinHoodHashMap(int expectedSize, float loadFactor) {
        this.loadFactor = PackedRobinHoodProbeAtoms.validateLoadFactor(loadFactor);
        int capacity = PackedSupport.tableSizeForExpected(expectedSize, loadFactor);
        keys = new long[capacity];
        values = new long[capacity];
        states = new byte[capacity];
        threshold = PackedRobinHoodProbeAtoms.threshold(capacity, loadFactor);
    }

    @Override
    public boolean put(long key, long value) {
        int existing = findIndex(key);
        if (existing >= 0) {
            values[existing] = value;
            return false;
        }
        if (size + 1 > threshold) {
            rehash(PackedSupport.nextTableCapacity(keys.length));
        }
        insertNew(key, value);
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
        deleteAt(index);
        size--;
        return true;
    }

    @Override
    public int filterInPlace(LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;
        for (int slot = 0; slot < states.length; ) {
            if (states[slot] == OCCUPIED && !predicate.test(keys[slot], values[slot])) {
                deleteAt(slot);
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
    }

    @Override
    public void trimToSize() {
        int target = PackedSupport.tableSizeForExpected(size, loadFactor);
        if (target < keys.length) {
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
        return PackedRobinHoodProbeAtoms.findIndex(keys, states, key);
    }

    private void insertNew(long key, long value) {
        int mask = keys.length - 1;
        long candidateKey = key;
        long candidateValue = value;
        int index = PackedSupport.mix(candidateKey) & mask;
        int distance = 0;

        while (states[index] == OCCUPIED) {
            long residentKey = keys[index];
            int residentDistance = PackedRobinHoodProbeAtoms.probeDistance(index, residentKey, mask);
            if (residentDistance < distance) {
                long residentValue = values[index];
                keys[index] = candidateKey;
                values[index] = candidateValue;
                candidateKey = residentKey;
                candidateValue = residentValue;
                distance = residentDistance;
            }
            index = (index + 1) & mask;
            distance++;
        }

        keys[index] = candidateKey;
        values[index] = candidateValue;
        states[index] = OCCUPIED;
        size++;
    }

    private void deleteAt(int gap) {
        int mask = keys.length - 1;
        int next = (gap + 1) & mask;

        while (states[next] == OCCUPIED) {
            long key = keys[next];
            if (PackedRobinHoodProbeAtoms.probeDistance(next, key, mask) == 0) {
                break;
            }
            keys[gap] = key;
            values[gap] = values[next];
            states[gap] = OCCUPIED;
            gap = next;
            next = (next + 1) & mask;
        }

        keys[gap] = 0L;
        values[gap] = 0L;
        states[gap] = EMPTY;
    }

    private void rehash(int capacity) {
        long[] oldKeys = keys;
        long[] oldValues = values;
        byte[] oldStates = states;
        keys = new long[capacity];
        values = new long[capacity];
        states = new byte[capacity];
        threshold = PackedRobinHoodProbeAtoms.threshold(capacity, loadFactor);
        int oldSize = size;
        size = 0;

        for (int i = 0; i < oldStates.length; i++) {
            if (oldStates[i] == OCCUPIED) {
                insertNew(oldKeys[i], oldValues[i]);
            }
        }
        if (size != oldSize) {
            throw new IllegalStateException("rehash size mismatch");
        }
    }

}
