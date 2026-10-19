/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Composite-key map for two- and three-element keys over parallel key lanes, a cached-hash lane and per-slot state bits.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET, leaf LS6):
 * <ul>
 *   <li>COMMONS {@code MultiKeyMap}: {@code put(k1, k2[, k3], v)} returns the previous value or null, {@code get},
 *       {@code containsKey(k1, k2)}, {@code removeMultiKey(k1, k2)}, {@code removeAll(k1)} (every mapping, of any arity,
 *       whose first key equals {@code k1}); keys of different arity are distinct; null key elements and null values are
 *       allowed; key hash = Commons {@code MultiKeyMap.hash}. Iteration order is NOT claimed.</li>
 * </ul>
 * Qualified only against Commons {@code MultiKeyMap}; no equivalence with Guava {@code Table} is inferred.
 *
 * <p>DOOP mapping: lane kind OBJECT; slot lanes {@code key1Lane}, {@code key2Lane}, {@code key3Lane} (a private sentinel
 * marks two-element keys), {@code valueLane}, {@code hashIndexLane} (cached combined hash); linear probing, power-of-two
 * capacity, load <= 1/2, backward-shift deletion; 2-bit per-slot state words in {@code stateMaskWords}.
 * Stored nodes: NONE (no MultiKey objects are stored or created). Per-element allocation: 0 in steady state (growth
 * doubles the lanes, amortised). Not a {@code java.util} replacement.
 *
 * @param <K> key element type
 * @param <V> value type
 */
public final class M3MultiKeyMap<K, V> {
    /** Slot state bit {@code OCCUPIED}. */
    static final long STATE_OCCUPIED = 1L << 0;
    /** Slot state bit {@code DELETED}: reserved; backward-shift deletion never leaves tombstones. */
    static final long STATE_DELETED = 1L << 1;
    private static final Object TWO = new Object();

    /** Storage lane {@code key1Lane}. */
    private Object[] key1Lane;
    /** Storage lane {@code key2Lane}. */
    private Object[] key2Lane;
    /** Storage lane {@code key3Lane}; {@code TWO} sentinel for two-element keys. */
    private Object[] key3Lane;
    /** Storage lane {@code valueLane}. */
    private Object[] valueLane;
    /** Cached Commons combined hash per slot. */
    private int[] hashIndexLane;
    /** Per-slot state words: 2 bits per slot. */
    private long[] stateMaskWords;
    private int mask;
    /** Live slot count. */
    private int size;

    /** Empty map. */
    public M3MultiKeyMap() {
        allocate(16);
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3MultiKeyMap|MULTIKEY_MAP|OBJECT|OCCUPIED,DELETED|Object[] key1Lane;Object[] key2Lane;Object[] key3Lane;Object[] valueLane;int[] hashIndexLane|RECEIVED";
    }

    /** MultiKeyMap.get(k1, k2). */
    public V get(Object key1, Object key2) {
        int slot = find(key1, key2, TWO);
        return slot < 0 ? null : value(slot);
    }

    /** MultiKeyMap.get(k1, k2, k3). */
    public V get(Object key1, Object key2, Object key3) {
        int slot = find(key1, key2, key3);
        return slot < 0 ? null : value(slot);
    }

    /** MultiKeyMap.containsKey(k1, k2). */
    public boolean containsKey(Object key1, Object key2) {
        return find(key1, key2, TWO) >= 0;
    }

    /** MultiKeyMap.put(k1, k2, v). */
    public V put(K key1, K key2, V value) {
        return store(key1, key2, TWO, value);
    }

    /** MultiKeyMap.put(k1, k2, k3, v). */
    public V put(K key1, K key2, K key3, V value) {
        return store(key1, key2, key3, value);
    }

    /** MultiKeyMap.removeMultiKey(k1, k2): previous value or null. */
    public V removeMultiKey(Object key1, Object key2) {
        int slot = find(key1, key2, TWO);
        if (slot < 0) return null;
        V previous = value(slot);
        deleteSlot(slot);
        return previous;
    }

    /** MultiKeyMap.removeAll(k1): every mapping whose first key equals {@code key1}; true if any was removed. */
    public boolean removeAll(Object key1) {
        boolean removed = false;
        for (int slot = 0; slot <= mask; slot++) {
            while (occupied(slot) && Objects.equals(key1Lane[slot], key1)) {
                deleteSlot(slot);
                removed = true;
            }
        }
        return removed;
    }

    /** Map.size. */
    public int size() {
        return size;
    }

    /** Map.isEmpty. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Map.clear. */
    public void clear() {
        Arrays.fill(key1Lane, null);
        Arrays.fill(key2Lane, null);
        Arrays.fill(key3Lane, null);
        Arrays.fill(valueLane, null);
        Arrays.fill(stateMaskWords, 0L);
        size = 0;
    }

    private V store(Object key1, Object key2, Object key3, V value) {
        int slot = find(key1, key2, key3);
        if (slot >= 0) {
            V previous = value(slot);
            valueLane[slot] = value;
            return previous;
        }
        if ((size + 1) * 2 > mask + 1) grow();
        insert(key1, key2, key3, value, hash(key1, key2, key3));
        size++;
        return null;
    }

    @SuppressWarnings("unchecked")
    private V value(int slot) {
        return (V) valueLane[slot];
    }

    /** Commons MultiKeyMap.hash for two or three elements. */
    private static int hash(Object key1, Object key2, Object key3) {
        int h = 0;
        if (key1 != null) h ^= key1.hashCode();
        if (key2 != null) h ^= key2.hashCode();
        if (key3 != TWO && key3 != null) h ^= key3.hashCode();
        h += ~(h << 9);
        h ^= h >>> 14;
        h += h << 4;
        h ^= h >>> 10;
        return h;
    }

    private int home(int hash) {
        int spread = hash * 0x9E3779B9;
        return (spread ^ (spread >>> 16)) & mask;
    }

    private int find(Object key1, Object key2, Object key3) {
        int hash = hash(key1, key2, key3);
        for (int slot = home(hash); occupied(slot); slot = (slot + 1) & mask) {
            if (hashIndexLane[slot] == hash && Objects.equals(key1Lane[slot], key1) && Objects.equals(key2Lane[slot], key2)
                    && (key3Lane[slot] == TWO ? key3 == TWO : key3 != TWO && Objects.equals(key3Lane[slot], key3))) {
                return slot;
            }
        }
        return -1;
    }

    private void allocate(int capacity) {
        key1Lane = new Object[capacity];
        key2Lane = new Object[capacity];
        key3Lane = new Object[capacity];
        valueLane = new Object[capacity];
        hashIndexLane = new int[capacity];
        stateMaskWords = new long[(capacity + 31) >>> 5];
        mask = capacity - 1;
    }

    private void grow() {
        Object[] k1 = key1Lane, k2 = key2Lane, k3 = key3Lane, v = valueLane;
        int[] hashes = hashIndexLane;
        long[] states = stateMaskWords;
        int oldCapacity = mask + 1;
        allocate(oldCapacity << 1);
        for (int slot = 0; slot < oldCapacity; slot++) {
            if ((states[slot >>> 5] & (STATE_OCCUPIED << ((slot & 31) << 1))) != 0) {
                insert(k1[slot], k2[slot], k3[slot], v[slot], hashes[slot]);
            }
        }
    }

    private void insert(Object key1, Object key2, Object key3, Object value, int hash) {
        int slot = home(hash);
        while (occupied(slot)) slot = (slot + 1) & mask;
        key1Lane[slot] = key1;
        key2Lane[slot] = key2;
        key3Lane[slot] = key3;
        valueLane[slot] = value;
        hashIndexLane[slot] = hash;
        stateMaskWords[slot >>> 5] |= STATE_OCCUPIED << ((slot & 31) << 1);
    }

    private boolean occupied(int slot) {
        return (stateMaskWords[slot >>> 5] & (STATE_OCCUPIED << ((slot & 31) << 1))) != 0;
    }

    private void deleteSlot(int slot) {
        int gap = slot;
        for (int probe = (gap + 1) & mask; occupied(probe); probe = (probe + 1) & mask) {
            int origin = home(hashIndexLane[probe]);
            boolean stays = gap <= probe ? gap < origin && origin <= probe : gap < origin || origin <= probe;
            if (stays) continue;
            key1Lane[gap] = key1Lane[probe];
            key2Lane[gap] = key2Lane[probe];
            key3Lane[gap] = key3Lane[probe];
            valueLane[gap] = valueLane[probe];
            hashIndexLane[gap] = hashIndexLane[probe];
            gap = probe;
        }
        key1Lane[gap] = null;
        key2Lane[gap] = null;
        key3Lane[gap] = null;
        valueLane[gap] = null;
        stateMaskWords[gap >>> 5] &= ~(STATE_OCCUPIED << ((gap & 31) << 1));
        size--;
    }
}
