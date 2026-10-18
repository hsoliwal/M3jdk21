// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded LFU/LRU cache for objects materialized at an API boundary.
 *
 * <p>All cache bookkeeping is stored in parallel arrays. The cache uses no
 * per-cache-entry node, bucket, or record objects. A cached value may be an
 * API facade (for example, a {@code Map.Entry}); it is not canonical collection
 * storage. Values are never retargeted to a different key when a slot is reused.
 *
 * <p>On eviction, the cache drops its references. A client that still holds the
 * old facade keeps its object, but that object is never recycled for another key.
 * This class is package-private and unsynchronized; its owners follow the
 * thread-safety policy of the corresponding collection.
 */
final class M3LFUCache<K, V> {
    private static final byte EMPTY = 0;
    private static final byte USED = 1;
    private static final byte DELETED = 2;
    private static final int MIN_TABLE_CAPACITY = 16;
    private static final int MAX_FREQUENCY = Integer.MAX_VALUE;

    private final int maximumSize;
    private final int mask;
    private final Object[] keys;
    private final Object[] values;
    private final int[] hashes;
    private final int[] frequencies;
    private final long[] touched;
    private final byte[] states;
    private int size;
    private int occupied;
    private long clock;

    M3LFUCache(int maximumSize) {
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }
        this.maximumSize = maximumSize;
        int capacity = MIN_TABLE_CAPACITY;
        long minimum = Math.max((long) MIN_TABLE_CAPACITY, (long) maximumSize * 2L);
        while (capacity < minimum) {
            if (capacity >= (1 << 30)) {
                throw new IllegalArgumentException("maximumSize is too large");
            }
            capacity <<= 1;
        }
        keys = new Object[capacity];
        values = new Object[capacity];
        hashes = new int[capacity];
        frequencies = new int[capacity];
        touched = new long[capacity];
        states = new byte[capacity];
        mask = capacity - 1;
    }

    @SuppressWarnings("unchecked")
    V get(Object key) {
        int hash = hash(key);
        int slot = find(key, hash);
        if (slot < 0) {
            return null;
        }
        touch(slot);
        return (V) values[slot];
    }

    void put(K key, V value) {
        Objects.requireNonNull(value, "value");
        int hash = hash(key);
        int existing = find(key, hash);
        if (existing >= 0) {
            values[existing] = value;
            touch(existing);
            return;
        }
        if (size == maximumSize) {
            evictLeastFrequentlyUsed();
        }
        // Deletions leave probe markers. Rebuild before those markers can
        // consume the open-addressed table, preserving LFU ranks and touch order.
        if ((long) occupied + 1L >= (long) states.length * 2L / 3L) {
            rehash();
        }
        int slot = hash & mask;
        int firstDeleted = -1;
        while (states[slot] != EMPTY) {
            if (states[slot] == DELETED && firstDeleted < 0) {
                firstDeleted = slot;
            }
            slot = (slot + 1) & mask;
        }
        int target = firstDeleted >= 0 ? firstDeleted : slot;
        if (states[target] == EMPTY) {
            occupied++;
        }
        keys[target] = key;
        values[target] = value;
        hashes[target] = hash;
        frequencies[target] = 1;
        states[target] = USED;
        touched[target] = nextTick();
        size++;
    }

    boolean remove(Object key) {
        int slot = find(key, hash(key));
        if (slot < 0) {
            return false;
        }
        deleteAt(slot);
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            occupied = 0;
        } else if ((long) occupied > (long) size * 2L) {
            rehash();
        }
        return true;
    }

    void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        Arrays.fill(hashes, 0);
        Arrays.fill(frequencies, 0);
        Arrays.fill(touched, 0L);
        Arrays.fill(states, EMPTY);
        size = 0;
        occupied = 0;
        clock = 0L;
    }

    int size() {
        return size;
    }

    int capacity() {
        return maximumSize;
    }

    private int find(Object key, int hash) {
        int slot = hash & mask;
        while (states[slot] != EMPTY) {
            if (states[slot] == USED && hashes[slot] == hash
                    && Objects.equals(key, keys[slot])) {
                return slot;
            }
            slot = (slot + 1) & mask;
        }
        return -1;
    }

    private void touch(int slot) {
        if (frequencies[slot] < MAX_FREQUENCY) {
            frequencies[slot]++;
        }
        touched[slot] = nextTick();
    }

    private void evictLeastFrequentlyUsed() {
        int victim = -1;
        for (int i = 0; i < states.length; i++) {
            if (states[i] != USED) {
                continue;
            }
            if (victim < 0 || frequencies[i] < frequencies[victim]
                    || frequencies[i] == frequencies[victim] && touched[i] < touched[victim]) {
                victim = i;
            }
        }
        if (victim < 0) {
            throw new AssertionError("cache size and occupancy diverged");
        }
        deleteAt(victim);
    }

    private void deleteAt(int slot) {
        keys[slot] = null;
        values[slot] = null;
        hashes[slot] = 0;
        frequencies[slot] = 0;
        touched[slot] = 0L;
        states[slot] = DELETED;
        size--;
    }

    private void rehash() {
        int capacity = states.length;
        Object[] oldKeys = Arrays.copyOf(keys, capacity);
        Object[] oldValues = Arrays.copyOf(values, capacity);
        int[] oldHashes = Arrays.copyOf(hashes, capacity);
        int[] oldFrequencies = Arrays.copyOf(frequencies, capacity);
        long[] oldTouched = Arrays.copyOf(touched, capacity);
        byte[] oldStates = Arrays.copyOf(states, capacity);
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        Arrays.fill(hashes, 0);
        Arrays.fill(frequencies, 0);
        Arrays.fill(touched, 0L);
        Arrays.fill(states, EMPTY);
        size = 0;
        occupied = 0;
        for (int i = 0; i < capacity; i++) {
            if (oldStates[i] != USED) {
                continue;
            }
            int slot = oldHashes[i] & mask;
            while (states[slot] == USED) {
                slot = (slot + 1) & mask;
            }
            keys[slot] = oldKeys[i];
            values[slot] = oldValues[i];
            hashes[slot] = oldHashes[i];
            frequencies[slot] = oldFrequencies[i];
            touched[slot] = oldTouched[i];
            states[slot] = USED;
            size++;
            occupied++;
        }
    }

    private long nextTick() {
        if (clock == Long.MAX_VALUE) {
            // Pathological lifetime-only case: retain a valid deterministic
            // recency ordering without allocating a helper object.
            long next = 0L;
            for (int i = 0; i < states.length; i++) {
                if (states[i] == USED) {
                    touched[i] = ++next;
                }
            }
            clock = next;
        }
        return ++clock;
    }

    private static int hash(Object key) {
        int h = Objects.hashCode(key);
        return h ^ (h >>> 16);
    }
}
