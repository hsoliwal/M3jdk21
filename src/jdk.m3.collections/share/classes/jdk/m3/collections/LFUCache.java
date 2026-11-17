/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * Small bounded LFU cache whose storage is primitive/reference arrays, never linked cache nodes.
 *
 * <p>Keys are compared by identity because M3 entry-facade callers pass the actual key reference
 * stored in the owning map slot. An unused key slot holds the private EMPTY sentinel, so a stored
 * {@code null} key is a real key. Frequency selects the eviction victim; a monotonically
 * increasing access stamp breaks
 * ties by evicting the least recently accessed candidate. The cache is single-threaded, matching
 * the strong-key M3 map owners that use it.</p>
 */
final class LFUCache<K, V> {
    /** Key of an unused slot; never a caller's key, never returned. */
    //FLLDCIM: LEETCODIFY - EMPTY sentinel replaces the byte state lane
    private static final Object EMPTY = new Object();
    private final Object[] keys;
    private final Object[] values;
    private final int[] frequencies;
    private final long[] accessStamps;
    private int size;
    private long clock;
    private int lastIdentitySlot = -1;

    LFUCache(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("negative capacity");
        }
        keys = jdk.m3.arrays.ArrayNative.allocateObjectsRequired(capacity);
        values = jdk.m3.arrays.ArrayNative.allocateObjectsRequired(capacity);
        frequencies = jdk.m3.arrays.ArrayNative.allocateIntsRequired(capacity);
        accessStamps = jdk.m3.arrays.ArrayNative.allocateLongsRequired(capacity);
        Arrays.fill(keys, EMPTY);
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
        // A timestamp rebase can allocate temporary primitive scratch.
        // Finish it before publishing any new entry or evicting a live key.
        long stamp = nextStamp();
        int target = victim();
        if (keys[target] == EMPTY) {
            size++;
        }
        lastIdentitySlot = -1;
        keys[target] = key;
        values[target] = created;
        frequencies[target] = 1;
        accessStamps[target] = stamp;
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
        Arrays.fill(keys, EMPTY);
        Arrays.fill(values, null);
        Arrays.fill(frequencies, 0);
        Arrays.fill(accessStamps, 0L);
        size = 0;
        clock = 0L;
        lastIdentitySlot = -1;
    }

    int size() {
        return size;
    }

    int capacity() {
        return keys.length;
    }

    private int findIdentity(Object key) {
        int hinted = lastIdentitySlot;
        if (hinted >= 0 && keys[hinted] == key) { // a caller's key is never EMPTY
            return hinted;
        }
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == key) {
                lastIdentitySlot = i;
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
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == EMPTY) {
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
        // A failed overflow rebase must not increase a live entry frequency.
        long stamp = nextStamp();
        if (frequencies[slot] < Integer.MAX_VALUE) {
            frequencies[slot]++;
        }
        accessStamps[slot] = stamp;
    }

    private long nextStamp() {
        if (clock == Long.MAX_VALUE) {
            long minimum = Long.MAX_VALUE;
            for (int i = 0; i < accessStamps.length; i++) {
                if (keys[i] != EMPTY && accessStamps[i] < minimum) {
                    minimum = accessStamps[i];
                }
            }
            if (minimum == Long.MAX_VALUE) {
                clock = 0L;
                Arrays.fill(accessStamps, 0L);
            } else if (minimum == 0L) {
                // Dense-rank the live timestamps in O(n log n), rather than
                // repeatedly scanning the whole cache for each next distinct
                // stamp (O(n squared)). Only one primitive scratch array is
                // allocated: no per-slot Entry/Node object or boxing.
                //
                // Allocation must finish before any live timestamp is changed.
                long[] sorted = new long[size];
                int count = 0;
                for (int i = 0; i < keys.length; i++) {
                    if (keys[i] != EMPTY) {
                        sorted[count++] = accessStamps[i];
                    }
                }
                Arrays.sort(sorted, 0, count);
                int distinct = 0;
                for (int i = 0; i < count; i++) {
                    if (distinct == 0 || sorted[i] != sorted[distinct - 1]) {
                        sorted[distinct++] = sorted[i];
                    }
                }
                for (int i = 0; i < keys.length; i++) {
                    if (keys[i] != EMPTY) {
                        int rank = Arrays.binarySearch(sorted, 0, distinct, accessStamps[i]);
                        // No negative rank is possible for a complete owner
                        // snapshot. Preserve equal stamps as equal ranks.
                        accessStamps[i] = rank + 1L;
                    }
                }
                clock = distinct;
            } else {
                for (int i = 0; i < accessStamps.length; i++) {
                    if (keys[i] != EMPTY) {
                        accessStamps[i] -= minimum;
                    }
                }
                clock -= minimum;
            }
        }
        return ++clock;
    }

    private void clearSlot(int slot) {
        lastIdentitySlot = -1;
        keys[slot] = EMPTY;
        values[slot] = null;
        frequencies[slot] = 0;
        accessStamps[slot] = 0L;
    }
}
