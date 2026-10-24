/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
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
        keys = com.m3.arrays.M3ArrayNative.allocateObjectsRequired(capacity);
        values = com.m3.arrays.M3ArrayNative.allocateObjectsRequired(capacity);
        frequencies = com.m3.arrays.M3ArrayNative.allocateIntsRequired(capacity);
        accessStamps = com.m3.arrays.M3ArrayNative.allocateLongsRequired(capacity);
        states = com.m3.arrays.M3ArrayNative.allocateBytesRequired(capacity);
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
            if (selected < 0 || frequency < leastFrequency
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
            } else if (minimum == 0L) {
                // Preserve recency and ties without allocating after a cache mutation.
                // Nonnegative live stamps become temporary negative dense ranks.
                long rank = 0L;
                while (true) {
                    boolean found = false;
                    long next = Long.MAX_VALUE;
                    for (int i = 0; i < accessStamps.length; i++) {
                        if ((states[i] & OCCUPIED) != 0 && accessStamps[i] >= 0L
                                && (!found || accessStamps[i] < next)) {
                            next = accessStamps[i];
                            found = true;
                        }
                    }
                    if (!found) {
                        break;
                    }
                    rank++;
                    for (int i = 0; i < accessStamps.length; i++) {
                        if ((states[i] & OCCUPIED) != 0 && accessStamps[i] == next) {
                            accessStamps[i] = -rank;
                        }
                    }
                }
                for (int i = 0; i < accessStamps.length; i++) {
                    if ((states[i] & OCCUPIED) != 0) {
                        accessStamps[i] = -accessStamps[i];
                    }
                }
                clock = rank;
            } else {
                for (int i = 0; i < accessStamps.length; i++) {
                    if ((states[i] & OCCUPIED) != 0) {
                        accessStamps[i] -= minimum;
                    }
                }
                clock -= minimum;
            }
        }
        return ++clock;
    }

    private void clearSlot(int slot) {
        keys[slot] = null;
        values[slot] = null;
        frequencies[slot] = 0;
        accessStamps[slot] = 0L;
        states[slot] = 0;
    }
}
