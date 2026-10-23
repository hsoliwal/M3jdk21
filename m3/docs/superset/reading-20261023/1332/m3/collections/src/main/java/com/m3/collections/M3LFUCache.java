<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
=======
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
>>>>>>> 990ae6b6eb9f45e2a08b26b8b7f0c7af5b60814d
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
import java.util.function.Function;

/**
 * Small bounded LFU cache whose storage is primitive/reference arrays, never linked cache nodes.
 *
 * <p>Keys are compared by identity because M3 entry-facade callers pass the actual key reference
 * stored in the owning map slot. A byte mask distinguishes a stored {@code null} key from an empty
 * lane. Frequency selects the eviction victim; a monotonically increasing access stamp breaks
 * ties by evicting the least recently accessed candidate. The cache is single-threaded, matching
 * the strong-key M3 map owners that use it.</p>
 */
final class M3LFUCache<K, V> {
    private static final byte OCCUPIED = 1;
    private final Object[] keys;
    private final Object[] values;
    private final int[] frequencies;
    private final long[] accessStamps;
    private final byte[] states;
    private int size;
    private long clock;

    M3LFUCache(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("negative capacity");
        }
        keys = new Object[capacity];
        values = new Object[capacity];
        frequencies = new int[capacity];
        accessStamps = new long[capacity];
        states = new byte[capacity];
    }

    @SuppressWarnings("unchecked")
    V getOrCreate(K key, Function<? super K, ? extends V> factory) {
        Objects.requireNonNull(factory, "factory");
        int found = findIdentity(key);
        if (found >= 0) {
            touch(found);
            return (V) values[found];
        }

        // Build first. A factory failure must not evict a live cached facade.
        V created = Objects.requireNonNull(factory.apply(key), "cache factory returned null");
        if (keys.length == 0) {
            return created;
        }
        int target = victim();
        if (states[target] == 0) {
            size++;
        }
        keys[target] = key;
        values[target] = created;
        frequencies[target] = 1;
        accessStamps[target] = nextStamp();
        states[target] = OCCUPIED;
        return created;
    }

    void removeIdentity(Object key) {
        int found = findIdentity(key);
        if (found >= 0) {
            clearSlot(found);
            size--;
        }
    }

    void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        Arrays.fill(frequencies, 0);
        Arrays.fill(accessStamps, 0L);
        Arrays.fill(states, (byte) 0);
        size = 0;
        clock = 0L;
    }

    int size() {
        return size;
    }

    int capacity() {
        return keys.length;
    }

    private int findIdentity(Object key) {
        for (int i = 0; i < states.length; i++) {
            if ((states[i] & OCCUPIED) != 0 && keys[i] == key) {
                return i;
            }
        }
        return -1;
    }

    private int victim() {
        int empty = -1;
        int leastFrequency = Integer.MAX_VALUE;
        long leastRecent = Long.MAX_VALUE;
        int selected = -1;
        for (int i = 0; i < states.length; i++) {
            if ((states[i] & OCCUPIED) == 0) {
                empty = i;
                break;
            }
            int frequency = frequencies[i];
            long recent = accessStamps[i];
            if (frequency < leastFrequency
                    || frequency == leastFrequency && recent < leastRecent) {
                selected = i;
                leastFrequency = frequency;
                leastRecent = recent;
            }
        }
        return empty >= 0 ? empty : selected;
    }

    private void touch(int slot) {
        if (frequencies[slot] < Integer.MAX_VALUE) {
            frequencies[slot]++;
        }
        accessStamps[slot] = nextStamp();
    }

    private long nextStamp() {
        if (clock == Long.MAX_VALUE) {
            long minimum = Long.MAX_VALUE;
            for (int i = 0; i < accessStamps.length; i++) {
                if ((states[i] & OCCUPIED) != 0 && accessStamps[i] < minimum) {
                    minimum = accessStamps[i];
                }
            }
            if (minimum == Long.MAX_VALUE) {
                clock = 0L;
                Arrays.fill(accessStamps, 0L);
            } else {
                for (int i = 0; i < accessStamps.length; i++) {
                    if ((states[i] & OCCUPIED) != 0) {
                        accessStamps[i] -= minimum;
                    }
                }
                clock -= minimum;
=======
import java.util.function.IntFunction;

/**
 * Bounded, array-only LFU cache for objects materialized at a Java API boundary.
 *
 * <p>The collection never stores Entry/Node/Cell objects as its structural data.
 * This optional cache holds only already-requested API projections. Slot and
 * generation are primitive lanes; equal-frequency victims are selected by LRU.
 * Evicting a projection drops this cache's reference and leaves any escaped
 * projection untouched. Rebinding an escaped projection to a different row is
 * expressly forbidden. The JVM, not this cache, decides when GC runs.</p>
 *
 * <p>All methods are synchronized; the owning collection still supplies its
 * own synchronization and structural-modification checks. A small bounded
 * linear scan avoids per-entry hash/linked wrapper objects.</p>
 *
 * @param <T> caller-requested API projection, never an internal data entry
 */
public final class M3LFUCache<T> {
    private static final int RETAINED = 1;
    private static final int TOUCHED = 2;
    private static final int[] EMPTY_INTS = new int[0];
    private static final long[] EMPTY_LONGS = new long[0];
    private static final Object[] EMPTY_OBJECTS = new Object[0];

    private final int capacity;
    private Object[] projections = EMPTY_OBJECTS;
    private int[] slots = EMPTY_INTS;
    private int[] generations = EMPTY_INTS;
    private int[] frequencies = EMPTY_INTS;
    private int[] stateMasks = EMPTY_INTS;
    private long[] recency = EMPTY_LONGS;
    private int size;
    private long clock;

    public M3LFUCache(int maximumProjections) {
        if (maximumProjections < 0) {
            throw new IllegalArgumentException("negative cache capacity");
        }
        capacity = maximumProjections;
    }

    public synchronized int size() { return size; }

    public int capacity() { return capacity; }

    /**
     * Return an existing projection or call the factory exactly once on a miss.
     * The factory is never called by core collection operations, only by the
     * method that makes the API object available to the caller.
     */
    @SuppressWarnings("unchecked")
    public synchronized T getOrCreate(int slot, int generation,
            IntFunction<? extends T> materialize) {
        if (slot < 0) { throw new IllegalArgumentException("negative physical slot"); }
        Objects.requireNonNull(materialize, "materialize");
        if (memoryPressure()) {
            clear();
            return Objects.requireNonNull(materialize.apply(slot), "API projection");
        }
        for (int i = 0; i < size; i++) {
            if ((stateMasks[i] & RETAINED) != 0
                    && slots[i] == slot && generations[i] == generation) {
                if (frequencies[i] < Integer.MAX_VALUE) { frequencies[i]++; }
                stateMasks[i] = RETAINED | TOUCHED;
                recency[i] = nextClock();
                return (T) projections[i];
            }
        }

        // A reused physical slot is a different logical row. Release any old
        // cached projection before constructing a new one for that slot.
        invalidate(slot);
        T created = Objects.requireNonNull(materialize.apply(slot), "API projection");
        if (capacity == 0) { return created; }
        int target;
        if (size < capacity) {
            reserve(size + 1);
            target = size++;
        } else {
            target = victim();
        }
        projections[target] = created;
        slots[target] = slot;
        generations[target] = generation;
        frequencies[target] = 1;
        stateMasks[target] = RETAINED;
        recency[target] = nextClock();
        return created;
    }

    /** Remove retained API projections for one physical slot, without mutation. */
    public synchronized void invalidate(int slot) {
        for (int i = 0; i < size;) {
            if (slots[i] == slot) { erase(i); } else { i++; }
        }
    }

    /** Drop cold LFU projections until no more than {@code maximum} remain. */
    public synchronized void trimTo(int maximum) {
        if (maximum < 0) { throw new IllegalArgumentException("negative target"); }
        while (size > maximum) { erase(victim()); }
    }

    /** Drop all retained API references. Slots and array capacity may be reused. */
    public synchronized void clear() {
        Arrays.fill(projections, 0, size, null);
        Arrays.fill(stateMasks, 0, size, 0);
        size = 0;
    }

    private void reserve(int minimum) {
        if (minimum <= projections.length) { return; }
        int next = (int) Math.min((long) capacity,
                Math.max((long) minimum, Math.max(4L, (long) projections.length * 2)));
        projections = Arrays.copyOf(projections, next);
        slots = Arrays.copyOf(slots, next);
        generations = Arrays.copyOf(generations, next);
        frequencies = Arrays.copyOf(frequencies, next);
        stateMasks = Arrays.copyOf(stateMasks, next);
        recency = Arrays.copyOf(recency, next);
    }

    private int victim() {
        int victim = 0;
        for (int i = 1; i < size; i++) {
            if (frequencies[i] < frequencies[victim]
                    || frequencies[i] == frequencies[victim]
                            && recency[i] < recency[victim]) {
                victim = i;
            }
        }
        return victim;
    }

    private void erase(int index) {
        int last = --size;
        if (index != last) {
            projections[index] = projections[last];
            slots[index] = slots[last];
            generations[index] = generations[last];
            frequencies[index] = frequencies[last];
            stateMasks[index] = stateMasks[last];
            recency[index] = recency[last];
        }
        projections[last] = null;
        slots[last] = 0;
        generations[last] = 0;
        frequencies[last] = 0;
        stateMasks[last] = 0;
        recency[last] = 0;
    }

    private long nextClock() {
        if (clock == Long.MAX_VALUE) {
            // Equal-frequency eviction does not depend on absolute timestamp.
            // Normalize relative ordering without allocating comparator objects.
            long smallest = clock;
            for (int i = 0; i < size; i++) { smallest = Math.min(smallest, recency[i]); }
            if (smallest > 0) {
                for (int i = 0; i < size; i++) { recency[i] -= smallest; }
                clock -= smallest;
            } else {
                clear();
                clock = 0;
>>>>>>> 990ae6b6eb9f45e2a08b26b8b7f0c7af5b60814d
            }
        }
        return ++clock;
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    private void clearSlot(int slot) {
        keys[slot] = null;
        values[slot] = null;
        frequencies[slot] = 0;
        accessStamps[slot] = 0L;
        states[slot] = 0;
=======
    private static boolean memoryPressure() {
        Runtime r = Runtime.getRuntime();
        long used = r.totalMemory() - r.freeMemory();
        long headroom = Math.max(0L, r.maxMemory() - used);
        return headroom < Math.max(4L << 20, r.maxMemory() >>> 6);
>>>>>>> 990ae6b6eb9f45e2a08b26b8b7f0c7af5b60814d
    }
}
