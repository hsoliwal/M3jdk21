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
 * Open-addressed long hash set backed by {@code long[]} values and {@code byte[]} state.
 *
 * <p>No entry/node object is retained per element. A state byte distinguishes empty,
 * occupied and deleted slots, so every long value, including zero, is valid.
 * Backing lanes are allocated on first insertion. {@link #capacity()} reports the
 * reserved logical capacity while empty storage is shared. Allocation failure may
 * occur at first insertion rather than construction.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongHashSet.java}
 * blob {@code b3c2441eb6c946d60d974add4a12a2f1ba139a35} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongHashSet implements M3PackedMutableLongSet {
    private static final byte EMPTY = M3PackedHashAtoms.EMPTY;
    private static final byte OCCUPIED = M3PackedHashAtoms.OCCUPIED;
    private static final byte DELETED = M3PackedHashAtoms.DELETED;
    private static final long[] EMPTY_VALUES = new long[0];
    private static final byte[] EMPTY_STATES = new byte[0];

    private long[] table;
    private byte[] states;
    private int size;
    private int used;
    private int reservedCapacity;

    public M3PackedLongHashSet() {
        this(8);
    }

    public M3PackedLongHashSet(int expectedSize) {
        int capacity = M3PackedSupport.tableSizeForExpected(expectedSize);
        table = EMPTY_VALUES;
        states = EMPTY_STATES;
        reservedCapacity = capacity;
    }

    public boolean add(long value) {
        allocateIfNeeded();
        if (M3PackedHashAtoms.pressure(used, reservedCapacity)) {
            int existing = M3PackedHashAtoms.findIndex(table, states, value);
            if (existing >= 0) {
                return false;
            }
            rehash(M3PackedHashAtoms.capacityAfterPressure(size, table.length));
        }
        int slot = M3PackedHashAtoms.findOrInsertionIndex(table, states, value);
        if (slot >= 0) {
            return false;
        }
        int target = ~slot;
        boolean reusedDeleted = states[target] == DELETED;
        table[target] = value;
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

    public int addAll(long[] values) {
        Objects.requireNonNull(values, "values");
        int before = size;
        ensureExpectedSize(size + values.length);
        for (long value : values) {
            add(value);
        }
        return size - before;
    }

    @Override
    public boolean contains(long value) {
        return findIndex(value) >= 0;
    }

    public boolean remove(long value) {
        int index = findIndex(value);
        if (index < 0) {
            return false;
        }
        states[index] = DELETED;
        size--;
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            used = 0;
        } else if (M3PackedHashAtoms.compactTombstones(size, used)) {
            rehash(table.length);
        }
        return true;
    }

    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED && !predicate.test(table[i])) {
                states[i] = DELETED;
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

    public void trimToSize() {
        int target = M3PackedSupport.tableSizeForExpected(size);
        if (size == 0) {
            table = EMPTY_VALUES;
            states = EMPTY_STATES;
            reservedCapacity = target;
            used = 0;
        } else if (target != reservedCapacity || used != size) {
            rehash(target);
        }
    }

    public int capacity() {
        return reservedCapacity;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        Arrays.fill(states, EMPTY);
        size = 0;
        used = 0;
    }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
        int write = 0;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == OCCUPIED) {
                result[write++] = table[i];
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
                        return table[current];
                    }
                }
                throw new NoSuchElementException();
            }
        };
    }

    private int findIndex(long value) {
        return size == 0 ? -1 : M3PackedHashAtoms.findIndex(table, states, value);
    }

    private void ensureExpectedSize(int expectedSize) {
        int needed = M3PackedSupport.tableSizeForExpected(expectedSize);
        if (needed > reservedCapacity) {
            if (table.length == 0) {
                reservedCapacity = needed;
            } else {
                rehash(needed);
            }
        }
    }

    private void allocateIfNeeded() {
        if (table.length != 0) {
            return;
        }
        // Publish both lanes only after allocation succeeds.
        long[] newTable = new long[reservedCapacity];
        byte[] newStates = new byte[reservedCapacity];
        table = newTable;
        states = newStates;
    }

    private void rehash(int capacity) {
        long[] oldTable = table;
        byte[] oldStates = states;
        table = new long[capacity];
        states = new byte[capacity];
        reservedCapacity = capacity;
        size = 0;
        used = 0;
        for (int i = 0; i < oldStates.length; i++) {
            if (oldStates[i] == OCCUPIED) {
                addRehashed(oldTable[i]);
            }
        }
    }

    private void addRehashed(long value) {
        int index = M3PackedHashAtoms.rehashInsertionIndex(table, states, value);
        table[index] = value;
        states[index] = OCCUPIED;
        size++;
        used++;
    }
}
