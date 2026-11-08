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
import jdk.internal.m3.collections.LongByteMap;
import jdk.m3.collections.LongLongPredicate;

/**
 * Open-addressed {@code long -> unsigned byte} map.
 *
 * <p>This is the compact signal/strength layout: {@code long[] keys},
 * {@code byte[] values}, {@code byte[] states}. Values are exposed as 0..255 ints;
 * no Integer/Long wrapper or entry object is retained per mapping.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongByteHashMap.java}
 * blob {@code 138a1b1591c07dec3f77bf9cfaa64146e75b0521} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongByteHashMap implements LongByteMap {
    private static final byte EMPTY = PackedHashAtoms.EMPTY;
    private static final byte OCCUPIED = PackedHashAtoms.OCCUPIED;
    private static final byte DELETED = PackedHashAtoms.DELETED;

    private long[] keys;
    private byte[] values;
    private byte[] states;
    private int size;
    private int used;
    private int threshold;

    public PackedLongByteHashMap() {
        this(8);
    }

    public PackedLongByteHashMap(int expectedSize) {
        int capacity = PackedSupport.tableSizeForExpected(expectedSize);
        keys = new long[capacity];
        values = new byte[capacity];
        states = new byte[capacity];
        threshold = PackedHashAtoms.threshold(capacity);
    }

    /**
     * Associates key with an unsigned 8-bit value.
     *
     * @return true for a new key, false when replacing an existing value
     */
    public boolean putUnsigned(long key, int unsignedValue) {
        int checked = PackedSupport.checkedUnsignedByte(unsignedValue);
        if (used + 1 > threshold) {
            int existing = PackedHashAtoms.findIndex(keys, states, key);
            if (existing >= 0) {
                values[existing] = (byte) checked;
                return false;
            }
            rehash(PackedHashAtoms.capacityAfterPressure(size, keys.length));
        }
        int slot = PackedHashAtoms.findOrInsertionIndex(keys, states, key);
        if (slot >= 0) {
            values[slot] = (byte) checked;
            return false;
        }
        int target = ~slot;
        boolean reusedDeleted = states[target] == DELETED;
        keys[target] = key;
        values[target] = (byte) checked;
        states[target] = OCCUPIED;
        size++;
        if (!reusedDeleted) {
            used++;
        }
        return true;
    }

    public boolean containsKey(long key) {
        return findIndex(key) >= 0;
    }

    public int getUnsignedOrDefault(long key, int defaultValue) {
        PackedSupport.checkedUnsignedByte(defaultValue);
        int index = findIndex(key);
        return index < 0 ? defaultValue : Byte.toUnsignedInt(values[index]);
    }

    public int getUnsignedOrThrow(long key) {
        int index = findIndex(key);
        if (index < 0) {
            throw new IllegalArgumentException("missing key: " + key);
        }
        return Byte.toUnsignedInt(values[index]);
    }

    /**
     * Adds delta to the current unsigned value and clamps the result to 0..255.
     * Missing keys start from {@code initialValue}.
     */
    public int addClamped(long key, int delta, int initialValue) {
        PackedSupport.checkedUnsignedByte(initialValue);
        int index = findIndex(key);
        int current = index < 0 ? initialValue : Byte.toUnsignedInt(values[index]);
        long candidate = (long) current + delta;
        int next = (int) Math.max(0L, Math.min(255L, candidate));
        putUnsigned(key, next);
        return next;
    }

    public boolean replaceUnsigned(long key, int expectedValue, int newValue) {
        PackedSupport.checkedUnsignedByte(expectedValue);
        PackedSupport.checkedUnsignedByte(newValue);
        int index = findIndex(key);
        if (index < 0 || Byte.toUnsignedInt(values[index]) != expectedValue) {
            return false;
        }
        values[index] = (byte) newValue;
        return true;
    }

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

    /**
     * Predicate receives key and unsigned value as longs, preserving primitive dispatch.
     */
    public int filterInPlace(LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED
                    && !predicate.test(keys[i], Byte.toUnsignedInt(values[i]))) {
                states[i] = DELETED;
                size--;
            }
        }
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            used = 0;
        } else if (size != before) {
            rehash(PackedSupport.tableSizeForExpected(size));
        }
        return before - size;
    }

    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED) {
                consumer.accept(keys[i], Byte.toUnsignedInt(values[i]));
            }
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
        Arrays.fill(states, EMPTY);
        size = 0;
        used = 0;
    }

    public void trimToSize() {
        int target = PackedSupport.tableSizeForExpected(size);
        if (target != keys.length || used != size) {
            rehash(target);
        }
    }

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

    /**
     * Values are emitted in the same slot-scan order as {@link #keysToArray()}.
     */
    public byte[] valuesToByteArray() {
        byte[] result = new byte[size];
        int write = 0;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED) {
                result[write++] = values[i];
            }
        }
        return result;
    }

    private int findIndex(long key) {
        return PackedHashAtoms.findIndex(keys, states, key);
    }

    private void compactAfterRemoval() {
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            used = 0;
        } else if (PackedHashAtoms.compactTombstones(size, used)) {
            rehash(keys.length);
        }
    }

    private void rehash(int capacity) {
        long[] oldKeys = keys;
        byte[] oldValues = values;
        byte[] oldStates = states;
        keys = new long[capacity];
        values = new byte[capacity];
        states = new byte[capacity];
        threshold = PackedHashAtoms.threshold(capacity);
        size = 0;
        used = 0;
        for (int i = 0; i < oldStates.length; i++) {
            if (oldStates[i] == OCCUPIED) {
                putRehashed(oldKeys[i], oldValues[i]);
            }
        }
    }

    private void putRehashed(long key, byte value) {
        int index = PackedHashAtoms.rehashInsertionIndex(keys, states, key);
        keys[index] = key;
        values[index] = value;
        states[index] = OCCUPIED;
        size++;
        used++;
    }
}
