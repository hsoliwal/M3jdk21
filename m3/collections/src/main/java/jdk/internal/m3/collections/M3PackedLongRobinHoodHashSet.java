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

import jdk.m3.collections.LongIterator;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Primitive long Robin-Hood hash set with backward-shift deletion.
 *
 * <p>Probe distance is computed from the resident key when needed, so retained metadata
 * is only one occupancy byte per slot. Lookups terminate early when the current probe
 * distance exceeds the resident's distance. Deletion leaves no tombstones.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongRobinHoodHashSet.java}
 * blob {@code cb2eb10d43519e3b548dd07ee6a683b7037b12a7} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongRobinHoodHashSet implements M3PackedMutableLongSet {
    private static final byte EMPTY = M3PackedRobinHoodProbeAtoms.EMPTY;
    private static final byte OCCUPIED = M3PackedRobinHoodProbeAtoms.OCCUPIED;

    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private long[] keys;
    private byte[] states;
    private int size;
    private final float loadFactor;
    private int threshold;

    public M3PackedLongRobinHoodHashSet() {
        this(8, DEFAULT_LOAD_FACTOR);
    }

    public M3PackedLongRobinHoodHashSet(int expectedSize) {
        this(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    public M3PackedLongRobinHoodHashSet(int expectedSize, float loadFactor) {
        this.loadFactor = M3PackedRobinHoodProbeAtoms.validateLoadFactor(loadFactor);
        int capacity = M3PackedSupport.tableSizeForExpected(expectedSize, loadFactor);
        keys = new long[capacity];
        states = new byte[capacity];
        threshold = M3PackedRobinHoodProbeAtoms.threshold(capacity, loadFactor);
    }

    @Override
    public boolean add(long value) {
        if (findIndex(value) >= 0) {
            return false;
        }
        if (size + 1 > threshold) {
            rehash(M3PackedSupport.nextTableCapacity(keys.length));
        }
        insertNew(value);
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
        deleteAt(index);
        size--;
        return true;
    }

    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;
        for (int slot = 0; slot < states.length; ) {
            if (states[slot] == OCCUPIED && !predicate.test(keys[slot])) {
                deleteAt(slot);
                size--;
            } else {
                slot++;
            }
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
    }

    @Override
    public void trimToSize() {
        int target = M3PackedSupport.tableSizeForExpected(size, loadFactor);
        if (target < keys.length) {
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
        return M3PackedRobinHoodProbeAtoms.findIndex(keys, states, value);
    }

    private void insertNew(long value) {
        int mask = keys.length - 1;
        long candidate = value;
        int index = M3PackedSupport.mix(candidate) & mask;
        int distance = 0;

        while (states[index] == OCCUPIED) {
            long resident = keys[index];
            int residentDistance = M3PackedRobinHoodProbeAtoms.probeDistance(index, resident, mask);
            if (residentDistance < distance) {
                keys[index] = candidate;
                candidate = resident;
                distance = residentDistance;
            }
            index = (index + 1) & mask;
            distance++;
        }

        keys[index] = candidate;
        states[index] = OCCUPIED;
        size++;
    }

    private void deleteAt(int gap) {
        int mask = keys.length - 1;
        int next = (gap + 1) & mask;

        while (states[next] == OCCUPIED) {
            long key = keys[next];
            if (M3PackedRobinHoodProbeAtoms.probeDistance(next, key, mask) == 0) {
                break;
            }
            keys[gap] = key;
            states[gap] = OCCUPIED;
            gap = next;
            next = (next + 1) & mask;
        }

        keys[gap] = 0L;
        states[gap] = EMPTY;
    }

    private void rehash(int capacity) {
        long[] oldKeys = keys;
        byte[] oldStates = states;
        keys = new long[capacity];
        states = new byte[capacity];
        threshold = M3PackedRobinHoodProbeAtoms.threshold(capacity, loadFactor);
        int oldSize = size;
        size = 0;

        for (int i = 0; i < oldStates.length; i++) {
            if (oldStates[i] == OCCUPIED) {
                insertNew(oldKeys[i]);
            }
        }
        if (size != oldSize) {
            throw new IllegalStateException("rehash size mismatch");
        }
    }

}
