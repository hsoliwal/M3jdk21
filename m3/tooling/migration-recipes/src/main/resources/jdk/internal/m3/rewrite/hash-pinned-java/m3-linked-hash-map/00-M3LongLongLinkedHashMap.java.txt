/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Insertion-ordered primitive long-to-long hash map: the {@link M3LongLongHashMap} open-addressed
 * table plus two primitive link lanes, with no per-entry node.
 *
 * <p>Every {@code long}, including zero and the extrema, is a legal key and a legal value.
 * Membership and values live in the key, value and occupancy lanes exactly as in
 * {@link M3LongLongHashMap}; encounter order lives in {@code previous}/{@code next} lanes indexed
 * by table slot with {@code first}/{@code last} endpoints, adapted from the first-party Synexia
 * donor {@code PackedLongLongOrderedMap} (two long lanes, two integer link lanes, stable slots,
 * O(1) unlink) under the {@code LongLongOrderedMap} contract. Put, get and remove are expected
 * O(1); unlinking touches only the two neighbours; rehash re-inserts in encounter order so
 * iteration order survives growth and compaction. The sequenced surface mirrors
 * {@link java.util.LinkedHashMap} as a {@code SequencedMap}: {@code put} on a present key keeps
 * its position, {@code putFirst}/{@code putLast} move a present key, {@code firstKey}/{@code
 * lastKey}/{@code firstValue}/{@code lastValue}/{@code removeFirst}/{@code removeLast} throw
 * {@link NoSuchElementException} on an empty map; the key iterator and {@code forEach} are
 * fail-fast. This is an M3 proving-ground backend, not a {@code java.util.LinkedHashMap}
 * replacement.</p>
 */
public final class M3LongLongLinkedHashMap {
    private static final byte EMPTY = 0;
    private static final byte USED = 1;
    private static final byte DELETED = 2;
    private static final int MIN_CAPACITY = 8;
    private static final int LOAD_NUMERATOR = 2;
    private static final int LOAD_DENOMINATOR = 3;

    private long[] keys;
    private long[] values;
    private byte[] states;
    private int[] previous;
    private int[] next;
    private int first = -1;
    private int last = -1;
    private int size;
    private int occupied;
    private int modCount;

    public M3LongLongLinkedHashMap() {
        this(0);
    }

    public M3LongLongLinkedHashMap(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        int capacity = capacityFor(expectedSize);
        keys = new long[capacity];
        values = new long[capacity];
        states = new byte[capacity];
        previous = new int[capacity];
        next = new int[capacity];
    }

    /**
     * Appends a new mapping, or replaces the value of a present key in place (its encounter
     * position is kept, as {@code LinkedHashMap.put} does in insertion order). Returns
     * {@code true} only when a new key was inserted.
     */
    public boolean put(long key, long value) {
        int index = findIndex(key);
        if (index >= 0) {
            values[index] = value;
            return false;
        }
        insert(key, value, true);
        return true;
    }

    /** Appends, or moves a present key to the end and replaces its value ({@code SequencedMap.putLast}). */
    public boolean putLast(long key, long value) {
        int index = findIndex(key);
        if (index >= 0) {
            values[index] = value;
            if (index != last) {
                unlink(index);
                linkLast(index);
                modCount++;
            }
            return false;
        }
        insert(key, value, true);
        return true;
    }

    /** Prepends, or moves a present key to the front and replaces its value ({@code SequencedMap.putFirst}). */
    public boolean putFirst(long key, long value) {
        int index = findIndex(key);
        if (index >= 0) {
            values[index] = value;
            if (index != first) {
                unlink(index);
                linkFirst(index);
                modCount++;
            }
            return false;
        }
        insert(key, value, false);
        return true;
    }

    public long getOrDefault(long key, long defaultValue) {
        int index = findIndex(key);
        return index < 0 ? defaultValue : values[index];
    }

    public boolean containsKey(long key) {
        return findIndex(key) >= 0;
    }

    /** Linear scan in encounter order. */
    public boolean containsValue(long value) {
        for (int slot = first; slot >= 0; slot = next[slot]) {
            if (values[slot] == value) {
                return true;
            }
        }
        return false;
    }

    /** Replaces the value only when the key is present with {@code expectedValue}; order is kept. */
    public boolean replace(long key, long expectedValue, long newValue) {
        int index = findIndex(key);
        if (index < 0 || values[index] != expectedValue) {
            return false;
        }
        values[index] = newValue;
        return true;
    }

    public boolean remove(long key) {
        int index = findIndex(key);
        if (index < 0) {
            return false;
        }
        removeAt(index);
        return true;
    }

    public boolean remove(long key, long expectedValue) {
        int index = findIndex(key);
        if (index < 0 || values[index] != expectedValue) {
            return false;
        }
        removeAt(index);
        return true;
    }

    /** Oldest key in encounter order. */
    public long firstKey() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return keys[first];
    }

    /** Newest key in encounter order. */
    public long lastKey() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return keys[last];
    }

    /** Value mapped to the oldest key. */
    public long firstValue() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return values[first];
    }

    /** Value mapped to the newest key. */
    public long lastValue() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return values[last];
    }

    /** Removes the oldest mapping and returns its key ({@code SequencedMap.pollFirstEntry}). */
    public long removeFirst() {
        long key = firstKey();
        removeAt(first);
        return key;
    }

    /** Removes the newest mapping and returns its key ({@code SequencedMap.pollLastEntry}). */
    public long removeLast() {
        long key = lastKey();
        removeAt(last);
        return key;
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

    /** Retained primitive payload: key, value, occupancy and both link lanes. */
    public long payloadBytes() {
        return (long) keys.length * (2L * Long.BYTES + Byte.BYTES + 2L * Integer.BYTES);
    }

    public void clear() {
        if (size == 0 && occupied == 0) {
            return;
        }
        Arrays.fill(keys, 0L);
        Arrays.fill(values, 0L);
        Arrays.fill(states, EMPTY);
        first = -1;
        last = -1;
        size = 0;
        occupied = 0;
        modCount++;
    }

    /** Visits mappings in encounter order; fail-fast against structural modification. */
    public void forEach(M3LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int expectedModCount = modCount;
        for (int slot = first; slot >= 0; slot = next[slot]) {
            consumer.accept(keys[slot], values[slot]);
            checkForComodification(expectedModCount);
        }
    }

    /** Encounter-order key iterator; fail-fast against structural modification. */
    public M3LongIterator keyIterator() {
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

    /** Keys in encounter order. */
    public long[] keysToArray() {
        return lane(keys, first, next);
    }

    /** Values in encounter order, aligned with {@link #keysToArray()}. */
    public long[] valuesToArray() {
        return lane(values, first, next);
    }

    /** Keys from newest to oldest ({@code SequencedMap.reversed()} key view as an array). */
    public long[] reversedKeysToArray() {
        return lane(keys, last, previous);
    }

    /** Values from newest to oldest, aligned with {@link #reversedKeysToArray()}. */
    public long[] reversedValuesToArray() {
        return lane(values, last, previous);
    }

    private long[] lane(long[] source, int start, int[] links) {
        long[] result = new long[size];
        int cursor = 0;
        for (int slot = start; slot >= 0; slot = links[slot]) {
            result[cursor++] = source[slot];
        }
        return result;
    }

    /** Inserts a key known to be absent, growing first when the load factor would be exceeded. */
    private void insert(long key, long value, boolean atEnd) {
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
                values[target] = value;
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
                return;
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
        values[index] = 0L;
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

    /** Rebuilds the table and re-links every mapping in its current encounter order. */
    private void rehash(int requestedCapacity) {
        int capacity = tableSizeFor(Math.max(MIN_CAPACITY, requestedCapacity));
        long[] oldKeys = keys;
        long[] oldValues = values;
        int[] oldNext = next;
        int oldFirst = first;
        int previousSize = size;
        keys = new long[capacity];
        values = new long[capacity];
        states = new byte[capacity];
        previous = new int[capacity];
        next = new int[capacity];
        first = -1;
        last = -1;
        size = 0;
        occupied = 0;
        for (int slot = oldFirst; slot >= 0; slot = oldNext[slot]) {
            insertRehashed(oldKeys[slot], oldValues[slot]);
        }
        if (size != previousSize) {
            throw new IllegalStateException("rehash lost map entries");
        }
    }

    private void insertRehashed(long key, long value) {
        int mask = keys.length - 1;
        int index = mix(key) & mask;
        while (states[index] == USED) {
            index = (index + 1) & mask;
        }
        keys[index] = key;
        values[index] = value;
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
