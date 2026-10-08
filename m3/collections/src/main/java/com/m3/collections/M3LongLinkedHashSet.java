/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * Insertion-ordered primitive-long hash set: the {@link M3LongHashSet} open-addressed table plus
 * two primitive link lanes, with no per-element node.
 *
 * <p>All {@code long} values, including zero and the extrema, are legal keys. Membership lives in
 * the key and occupancy lanes exactly as in {@link M3LongHashSet}; encounter order lives in
 * {@code previous}/{@code next} lanes indexed by table slot with {@code first}/{@code last}
 * endpoints, adapted from the first-party Synexia donor {@code DenseOrderedSet} (stable slots with
 * primitive index/link lanes) and the {@code PackedLongOrderedSet} contract. Add, remove and
 * contains are expected O(1); unlinking touches only the two neighbours; rehash re-inserts in
 * encounter order so iteration order survives growth and compaction. The sequenced surface
 * mirrors {@link java.util.LinkedHashSet} as a {@code SequencedSet}: {@code addFirst}/{@code
 * addLast} move a present key to the end, {@code first}/{@code last}/{@code removeFirst}/{@code
 * removeLast} throw {@link NoSuchElementException} on an empty set. This is an M3 proving-ground
 * backend, not a {@code java.util.LinkedHashSet} replacement.</p>
 */
public final class M3LongLinkedHashSet implements M3LongCollection {
    private static final byte EMPTY = 0;
    private static final byte USED = 1;
    private static final byte DELETED = 2;
    private static final int MIN_CAPACITY = 8;
    private static final int LOAD_NUMERATOR = 2;
    private static final int LOAD_DENOMINATOR = 3;

    private long[] keys;
    private byte[] states;
    private int[] previous;
    private int[] next;
    private int first = -1;
    private int last = -1;
    private int size;
    private int occupied;
    private int modCount;

    public M3LongLinkedHashSet() {
        this(0);
    }

    public M3LongLinkedHashSet(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        int capacity = capacityFor(expectedSize);
        keys = new long[capacity];
        states = new byte[capacity];
        previous = new int[capacity];
        next = new int[capacity];
    }

    /** Appends {@code key} to the encounter order; returns {@code false} when already present. */
    public boolean add(long key) {
        return insert(key, true);
    }

    /** Appends, or moves a present key to the end ({@code SequencedSet.addLast}). */
    public void addLast(long key) {
        int index = findIndex(key);
        if (index >= 0) {
            if (index != last) {
                unlink(index);
                linkLast(index);
                modCount++;
            }
            return;
        }
        insert(key, true);
    }

    /** Prepends, or moves a present key to the front ({@code SequencedSet.addFirst}). */
    public void addFirst(long key) {
        int index = findIndex(key);
        if (index >= 0) {
            if (index != first) {
                unlink(index);
                linkFirst(index);
                modCount++;
            }
            return;
        }
        insert(key, false);
    }

    public boolean remove(long key) {
        int index = findIndex(key);
        if (index < 0) {
            return false;
        }
        removeAt(index);
        return true;
    }

    /** Oldest key in encounter order. */
    public long first() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return keys[first];
    }

    /** Newest key in encounter order. */
    public long last() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return keys[last];
    }

    public long removeFirst() {
        long key = first();
        removeAt(first);
        return key;
    }

    public long removeLast() {
        long key = last();
        removeAt(last);
        return key;
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

    /** Retained primitive payload: key, occupancy and both link lanes. */
    public long payloadBytes() {
        return (long) keys.length * (Long.BYTES + Byte.BYTES + 2L * Integer.BYTES);
    }

    @Override
    public void clear() {
        if (size == 0 && occupied == 0) {
            return;
        }
        Arrays.fill(keys, 0L);
        Arrays.fill(states, EMPTY);
        first = -1;
        last = -1;
        size = 0;
        occupied = 0;
        modCount++;
    }

    /** Keys in encounter order. */
    @Override
    public long[] toArray() {
        long[] result = new long[size];
        int cursor = 0;
        for (int slot = first; slot >= 0; slot = next[slot]) {
            result[cursor++] = keys[slot];
        }
        return result;
    }

    /** Keys from newest to oldest ({@code SequencedSet.reversed()} view as an array). */
    public long[] toReversedArray() {
        long[] result = new long[size];
        int cursor = 0;
        for (int slot = last; slot >= 0; slot = previous[slot]) {
            result[cursor++] = keys[slot];
        }
        return result;
    }

    /** Encounter-order iterator; fail-fast against structural modification. */
    @Override
    public M3LongIterator iterator() {
        int expectedModCount = modCount;
        return new M3LongIterator() {
            private int following = first;

            @Override
            public boolean hasNext() {
                checkForComodification(expectedModCount);
                return following >= 0;
            }

            @Override
            public long nextLong() {
                checkForComodification(expectedModCount);
                if (following < 0) {
                    throw new NoSuchElementException();
                }
                long key = keys[following];
                following = next[following];
                return key;
            }
        };
    }

    private boolean insert(long key, boolean atEnd) {
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
                if (atEnd) {
                    linkLast(target);
                } else {
                    linkFirst(target);
                }
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

    private void removeAt(int index) {
        unlink(index);
        states[index] = DELETED;
        keys[index] = 0L;
        size--;
        modCount++;
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            occupied = 0;
            first = -1;
            last = -1;
        } else if ((long) occupied > (long) size * 2 && keys.length > MIN_CAPACITY) {
            rehash(keys.length);
        }
    }

    private void linkLast(int slot) {
        previous[slot] = last;
        next[slot] = -1;
        if (last >= 0) {
            next[last] = slot;
        } else {
            first = slot;
        }
        last = slot;
    }

    private void linkFirst(int slot) {
        next[slot] = first;
        previous[slot] = -1;
        if (first >= 0) {
            previous[first] = slot;
        } else {
            last = slot;
        }
        first = slot;
    }

    private void unlink(int slot) {
        int before = previous[slot];
        int after = next[slot];
        if (before >= 0) {
            next[before] = after;
        } else {
            first = after;
        }
        if (after >= 0) {
            previous[after] = before;
        } else {
            last = before;
        }
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

    /** Rebuilds the table and re-links every key in its current encounter order. */
    private void rehash(int requestedCapacity) {
        int capacity = tableSizeFor(Math.max(MIN_CAPACITY, requestedCapacity));
        long[] oldKeys = keys;
        int[] oldNext = next;
        int oldFirst = first;
        int previousSize = size;
        keys = new long[capacity];
        states = new byte[capacity];
        previous = new int[capacity];
        next = new int[capacity];
        first = -1;
        last = -1;
        size = 0;
        occupied = 0;
        for (int slot = oldFirst; slot >= 0; slot = oldNext[slot]) {
            insertRehashed(oldKeys[slot]);
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
        linkLast(index);
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
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (int) x;
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
}
