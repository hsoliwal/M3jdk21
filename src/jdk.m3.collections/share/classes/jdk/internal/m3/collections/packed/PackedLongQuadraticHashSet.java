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

import jdk.m3.collections.LongIterator;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Primitive long hash set using triangular/quadratic probing.
 *
 * <p>A power-of-two table plus successive increments 1,2,3,... visits every slot.
 * One control byte records EMPTY/OCCUPIED/DELETED state, so all long values are valid.
 * This trades one byte per slot for less primary clustering than linear probing.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongQuadraticHashSet.java}
 * blob {@code 1e5fc45020957e0b115fffd6cbcf1cf8b367adb4} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongQuadraticHashSet implements PackedMutableLongSet {
    private static final byte EMPTY = PackedQuadraticProbeAtoms.EMPTY;
    private static final byte OCCUPIED = PackedQuadraticProbeAtoms.OCCUPIED;
    private static final byte DELETED = PackedQuadraticProbeAtoms.DELETED;

    public static final float DEFAULT_LOAD_FACTOR = 0.65f;

    private long[] keys;
    private byte[] states;
    private int size;
    private int used;
    private final float loadFactor;
    private int threshold;

    public PackedLongQuadraticHashSet() {
        this(8, DEFAULT_LOAD_FACTOR);
    }

    public PackedLongQuadraticHashSet(int expectedSize) {
        this(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    public PackedLongQuadraticHashSet(int expectedSize, float loadFactor) {
        this.loadFactor = PackedQuadraticProbeAtoms.validateLoadFactor(loadFactor);
        int capacity = PackedSupport.tableSizeForExpected(expectedSize, loadFactor);
        keys = new long[capacity];
        states = new byte[capacity];
        threshold = PackedQuadraticProbeAtoms.threshold(capacity, loadFactor);
    }

    @Override
    public boolean add(long value) {
        if (used + 1 > threshold) {
            if (contains(value)) {
                return false;
            }
            int capacity = size + 1 <= threshold
                    ? keys.length
                    : PackedSupport.nextTableCapacity(keys.length);
            rehash(capacity);
        }

        int slot = PackedQuadraticProbeAtoms.findOrInsertionIndex(keys, states, value);
        if (slot >= 0) {
            return false;
        }
        int target = ~slot;
        boolean reusedDeleted = states[target] == DELETED;
        keys[target] = value;
        states[target] = OCCUPIED;
        size++;
        if (!reusedDeleted) {
            used++;
        }
        return true;
    }

    public boolean addIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate.test(value) && add(value);
    }

    @Override
    public boolean contains(long value) {
        return findIndex(value) >= 0;
    }

    @Override
    public boolean remove(long value) {
        int index = findIndex(value);
        if (index < 0) {
            return false;
        }
        states[index] = DELETED;
        size--;
        compactAfterRemoval();
        return true;
    }

    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED && !predicate.test(keys[i])) {
                states[i] = DELETED;
                size--;
            }
        }
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            used = 0;
        } else if (size != before) {
            rehash(PackedSupport.tableSizeForExpected(size, loadFactor));
        }
        return before - size;
    }

    @Override
    public int size() {
        return size;
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
        int target = PackedSupport.tableSizeForExpected(size, loadFactor);
        if (target != keys.length || used != size) {
            rehash(target);
        }
    }

    @Override
    public long[] toArray() {
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
    public LongIterator iterator() {
        return new LongIterator() {
            private int index;
            private int emitted;

            @Override
            public boolean hasNext() {
                return emitted < size;
            }

            @Override
            public long nextLong() {
                while (index < states.length) {
                    int current = index++;
                    if (states[current] == OCCUPIED) {
                        emitted++;
                        return keys[current];
                    }
                }
                throw new NoSuchElementException();
            }
        };
    }

    private int findIndex(long value) {
        return PackedQuadraticProbeAtoms.findIndex(keys, states, value);
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
        byte[] oldStates = states;
        keys = new long[capacity];
        states = new byte[capacity];
        threshold = PackedQuadraticProbeAtoms.threshold(capacity, loadFactor);
        size = 0;
        used = 0;

        for (int i = 0; i < oldStates.length; i++) {
            if (oldStates[i] != OCCUPIED) {
                continue;
            }
            long key = oldKeys[i];
            int index = PackedQuadraticProbeAtoms.rehashInsertionIndex(keys, states, key);
            keys[index] = key;
            states[index] = OCCUPIED;
            size++;
            used++;
        }
    }

}
