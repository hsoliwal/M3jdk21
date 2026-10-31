/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * Primitive-long open-addressed hash set with JDK-shaped set semantics.
 *
 * <p>All {@code long} values, including zero and the extrema, are legal keys. Occupancy is kept
 * in a byte lane so no sentinel value is reserved and no per-entry node or {@link Long} wrapper
 * is retained. This is an M3 proving-ground backend, not a public {@code HashSet} replacement.</p>
 */
public final class M3LongHashSet implements M3LongCollection {
    private static final byte EMPTY = 0;
    private static final byte USED = 1;
    private static final byte DELETED = 2;
    private static final int MIN_CAPACITY = 8;
    private static final int LOAD_NUMERATOR = 2;
    private static final int LOAD_DENOMINATOR = 3;

    private long[] keys;
    private byte[] states;
    private int size;
    private int occupied;
    private int modCount;

    public M3LongHashSet() {
        this(0);
    }

    public M3LongHashSet(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        int capacity = capacityFor(expectedSize);
        keys = new long[capacity];
        states = new byte[capacity];
    }

    public boolean add(long key) {
        if (findIndex(key) >= 0) {
            return false;
        }
        if (needsGrowth(occupied + 1, keys.length)) {
            if (keys.length >= (1 << 30)) {
                throw new IllegalStateException("maximum hash-table capacity reached");
            }
            rehash(keys.length << 1);
        }
        int mask = keys.length - 1;
        int index = mix(key) & mask;
        int firstDeleted = -1;
        for (;;) {
            byte state = states[index];
            if (state == EMPTY) {
                int target = firstDeleted >= 0 ? firstDeleted : index;
                keys[target] = key;
                if (states[target] == EMPTY) {
                    occupied++;
                }
                states[target] = USED;
                size++;
                modCount++;
                return true;
            }
            if (state == DELETED && firstDeleted < 0) {
                firstDeleted = index;
            }
            index = (index + 1) & mask;
        }
    }

    public boolean remove(long key) {
        int index = findIndex(key);
        if (index < 0) {
            return false;
        }
        states[index] = DELETED;
        keys[index] = 0L;
        size--;
        modCount++;
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            occupied = 0;
        } else if ((long) occupied > (long) size * 2 && keys.length > MIN_CAPACITY) {
            rehash(keys.length);
        }
        return true;
    }

    @Override
    public boolean contains(long key) {
        return findIndex(key) >= 0;
    }

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return keys.length;
    }

    @Override
    public void clear() {
        if (size == 0 && occupied == 0) {
            return;
        }
        Arrays.fill(keys, 0L);
        Arrays.fill(states, EMPTY);
        size = 0;
        occupied = 0;
        modCount++;
    }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
        int cursor = 0;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == USED) {
                result[cursor++] = keys[i];
            }
        }
        return result;
    }

    @Override
    public M3LongIterator iterator() {
        int expectedModCount = modCount;
        return new M3LongIterator() {
            private int cursor;
            private int emitted;

            @Override
            public boolean hasNext() {
                checkForComodification(expectedModCount);
                return emitted < size;
            }

            @Override
            public long nextLong() {
                checkForComodification(expectedModCount);
                while (cursor < states.length && states[cursor] != USED) {
                    cursor++;
                }
                if (cursor >= states.length) {
                    throw new NoSuchElementException();
                }
                emitted++;
                return keys[cursor++];
            }
        };
    }

    private int findIndex(long key) {
        int mask = keys.length - 1;
        int index = mix(key) & mask;
        for (;;) {
            byte state = states[index];
            if (state == EMPTY) {
                return -1;
            }
            if (state == USED && keys[index] == key) {
                return index;
            }
            index = (index + 1) & mask;
        }
    }

    private void rehash(int requestedCapacity) {
        int capacity = tableSizeFor(Math.max(MIN_CAPACITY, requestedCapacity));
        long[] oldKeys = keys;
        byte[] oldStates = states;
        keys = new long[capacity];
        states = new byte[capacity];
        int previousSize = size;
        size = 0;
        occupied = 0;
        for (int i = 0; i < oldStates.length; i++) {
            if (oldStates[i] == USED) {
                insertRehashed(oldKeys[i]);
            }
        }
        if (size != previousSize) {
            throw new IllegalStateException("rehash lost set entries");
        }
    }

    private void insertRehashed(long key) {
        int mask = keys.length - 1;
        int index = mix(key) & mask;
        while (states[index] == USED) {
            index = (index + 1) & mask;
        }
        keys[index] = key;
        states[index] = USED;
        size++;
        occupied++;
    }

    private static int capacityFor(int expectedSize) {
        if (expectedSize == 0) {
            return MIN_CAPACITY;
        }
        long needed = ((long) expectedSize * LOAD_DENOMINATOR + LOAD_NUMERATOR - 1)
                / LOAD_NUMERATOR;
        if (needed > (1L << 30)) {
            throw new IllegalArgumentException("expectedSize too large");
        }
        return tableSizeFor((int) Math.max(MIN_CAPACITY, needed));
    }

    private static boolean needsGrowth(int occupied, int capacity) {
        return (long) occupied * LOAD_DENOMINATOR > (long) capacity * LOAD_NUMERATOR;
    }

    private static int tableSizeFor(int requested) {
        if (requested >= (1 << 30)) {
            return 1 << 30;
        }
        int normalized = Math.max(MIN_CAPACITY, requested);
        if ((normalized & (normalized - 1)) == 0) {
            return normalized;
        }
        return Integer.highestOneBit(normalized) << 1;
    }

    private static int mix(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdl;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53l;
        x ^= x >>> 33;
        return (int) x;
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
}
