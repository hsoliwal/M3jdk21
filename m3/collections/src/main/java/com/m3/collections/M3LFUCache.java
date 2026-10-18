// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded LFU/LRU cache for API objects materialized from an M3 physical slot.
 *
 * <p>The cache is addressed only by primitive {@code (slot, revision)} coordinates.
 * It never calls user key {@code hashCode()} or {@code equals()} and never allocates
 * a key tuple, bucket, node, or cache-entry object. Slot identity and lifecycle live
 * in {@code int[]} lanes; the only object references retained are cached API values.</p>
 *
 * <p>Eviction drops the cache's reference; an externally retained API object is never
 * repurposed for another coordinate. This class is package-private and unsynchronized.</p>
 */
final class M3LFUCache<V> {
    private static final byte EMPTY = 0;
    private static final byte USED = 1;
    private static final byte DELETED = 2;
    private static final int MIN_TABLE_CAPACITY = 16;
    private static final int MAX_FREQUENCY = Integer.MAX_VALUE;

    private final int maximumSize;
    private final int mask;
    private final int[] slots;
    private final int[] revisions;
    private final Object[] values;
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
        slots = new int[capacity];
        revisions = new int[capacity];
        values = new Object[capacity];
        frequencies = new int[capacity];
        touched = new long[capacity];
        states = new byte[capacity];
        mask = capacity - 1;
    }

    @SuppressWarnings("unchecked")
    V get(int slot, int revision) {
        if (slot < 0) {
            throw new IllegalArgumentException("negative physical slot");
        }
        int index = find(slot, revision);
        if (index < 0) {
            return null;
        }
        touch(index);
        return (V) values[index];
    }

    void put(int slot, int revision, V value) {
        if (slot < 0) {
            throw new IllegalArgumentException("negative physical slot");
        }
        Objects.requireNonNull(value, "value");
        int existing = find(slot, revision);
        if (existing >= 0) {
            values[existing] = value;
            touch(existing);
            return;
        }
        if (size == maximumSize) {
            evictLeastFrequentlyUsed();
        }
        if ((long) occupied + 1L >= (long) states.length * 2L / 3L) {
            rehash();
        }
        int hash = hash(slot, revision);
        int index = hash & mask;
        int firstDeleted = -1;
        while (states[index] != EMPTY) {
            if (states[index] == DELETED && firstDeleted < 0) {
                firstDeleted = index;
            }
            index = (index + 1) & mask;
        }
        int target = firstDeleted >= 0 ? firstDeleted : index;
        if (states[target] == EMPTY) {
            occupied++;
        }
        slots[target] = slot;
        revisions[target] = revision;
        values[target] = value;
        frequencies[target] = 1;
        states[target] = USED;
        touched[target] = nextTick();
        size++;
    }

    /** Remove every cached revision for a physical slot before it is reused. */
    boolean removeSlot(int slot) {
        if (slot < 0) {
            return false;
        }
        boolean removed = false;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == USED && slots[i] == slot) {
                deleteAt(i);
                removed = true;
            }
        }
        if (size == 0) {
            Arrays.fill(states, EMPTY);
            occupied = 0;
        } else if ((long) occupied > (long) size * 2L) {
            rehash();
        }
        return removed;
    }

    void clear() {
        Arrays.fill(slots, 0);
        Arrays.fill(revisions, 0);
        Arrays.fill(values, null);
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

    private int find(int slot, int revision) {
        int hash = hash(slot, revision);
        int index = hash & mask;
        while (states[index] != EMPTY) {
            if (states[index] == USED && slots[index] == slot && revisions[index] == revision) {
                return index;
            }
            index = (index + 1) & mask;
        }
        return -1;
    }

    private void touch(int index) {
        if (frequencies[index] < MAX_FREQUENCY) {
            frequencies[index]++;
        }
        touched[index] = nextTick();
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

    private void deleteAt(int index) {
        slots[index] = 0;
        revisions[index] = 0;
        values[index] = null;
        frequencies[index] = 0;
        touched[index] = 0L;
        states[index] = DELETED;
        size--;
    }

    private void rehash() {
        int capacity = states.length;
        int[] oldSlots = Arrays.copyOf(slots, capacity);
        int[] oldRevisions = Arrays.copyOf(revisions, capacity);
        Object[] oldValues = Arrays.copyOf(values, capacity);
        int[] oldFrequencies = Arrays.copyOf(frequencies, capacity);
        long[] oldTouched = Arrays.copyOf(touched, capacity);
        byte[] oldStates = Arrays.copyOf(states, capacity);
        Arrays.fill(slots, 0);
        Arrays.fill(revisions, 0);
        Arrays.fill(values, null);
        Arrays.fill(frequencies, 0);
        Arrays.fill(touched, 0L);
        Arrays.fill(states, EMPTY);
        size = 0;
        occupied = 0;
        for (int i = 0; i < capacity; i++) {
            if (oldStates[i] != USED) {
                continue;
            }
            int index = hash(oldSlots[i], oldRevisions[i]) & mask;
            while (states[index] == USED) {
                index = (index + 1) & mask;
            }
            slots[index] = oldSlots[i];
            revisions[index] = oldRevisions[i];
            values[index] = oldValues[i];
            frequencies[index] = oldFrequencies[i];
            touched[index] = oldTouched[i];
            states[index] = USED;
            size++;
            occupied++;
        }
    }

    private long nextTick() {
        if (clock == Long.MAX_VALUE) {
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

    private static int hash(int slot, int revision) {
        int h = slot * 0x9E3779B9;
        h ^= Integer.rotateLeft(revision * 0x85EBCA6B, 13);
        h ^= h >>> 16;
        return h;
    }
}
