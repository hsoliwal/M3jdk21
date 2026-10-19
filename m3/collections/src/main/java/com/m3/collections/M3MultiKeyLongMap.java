/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;

/**
 * {@code (long, long) -> long} map over parallel key lanes, open addressing and per-slot state bits.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET, leaf LS6):
 * <ul>
 *   <li>COMMONS {@code MultiKeyMap} with two-element keys: {@code put(k1, k2, v)} returns the previous value,
 *       {@code get(k1, k2)}, {@code containsKey(k1, k2)}, {@code removeMultiKey(k1, k2)}, {@code removeAll(k1)} (every
 *       mapping whose first key equals {@code k1}), {@code size}, {@code clear}. Iteration order is NOT claimed.
 *       Primitive returns use the configured {@code missingValue} where Commons returns {@code null}.</li>
 * </ul>
 * Qualified only against Commons {@code MultiKeyMap}; no equivalence with Guava {@code Table} is inferred.
 *
 * <p>DOOP mapping: lane kind LONG; slot lanes {@code key1Lane}, {@code key2Lane}, {@code valueLane}; combined-key hash
 * probe (linear probing, power-of-two capacity, load <= 1/2) with backward-shift deletion; 2-bit per-slot state words
 * (OCCUPIED, DELETED reserved) in {@code stateMaskWords}. Stored nodes: NONE (no MultiKey objects). Per-element
 * allocation: 0 in steady state (growth doubles the lanes, amortised). Not a {@code java.util} replacement.
 */
public final class M3MultiKeyLongMap {
    /** Slot state bit {@code OCCUPIED}. */
    static final long STATE_OCCUPIED = 1L << 0;
    /** Slot state bit {@code DELETED}: reserved; backward-shift deletion never leaves tombstones. */
    static final long STATE_DELETED = 1L << 1;

    /** Storage lane {@code key1Lane}. */
    private long[] key1Lane;
    /** Storage lane {@code key2Lane}. */
    private long[] key2Lane;
    /** Storage lane {@code valueLane}. */
    private long[] valueLane;
    /** Per-slot state words: 2 bits per slot. */
    private long[] stateMaskWords;
    private int mask;
    private final long missingValue;
    /** Live slot count. */
    private int size;

    /** Empty map, missing value 0. */
    public M3MultiKeyLongMap() {
        this(0L);
    }

    /** Empty map returning {@code missingValue} for absent keys. */
    public M3MultiKeyLongMap(long missingValue) {
        this.missingValue = missingValue;
        allocate(16);
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3MultiKeyLongMap|MULTIKEY_LONG_MAP|LONG|OCCUPIED,DELETED|long[] key1Lane;long[] key2Lane;long[] valueLane|RECEIVED";
    }

    /** MultiKeyMap.get(k1, k2) with a default. */
    public long getOrDefault(long key1, long key2, long defaultValue) {
        int slot = find(key1, key2);
        return slot < 0 ? defaultValue : valueLane[slot];
    }

    /** MultiKeyMap.containsKey(k1, k2). */
    public boolean containsKey(long key1, long key2) {
        return find(key1, key2) >= 0;
    }

    /** MultiKeyMap.put(k1, k2, v): previous value or the missing value. */
    public long put(long key1, long key2, long value) {
        int slot = find(key1, key2);
        if (slot >= 0) {
            long previous = valueLane[slot];
            valueLane[slot] = value;
            return previous;
        }
        if ((size + 1) * 2 > mask + 1) grow();
        insert(key1, key2, value);
        size++;
        return missingValue;
    }

    /** MultiKeyMap.removeMultiKey(k1, k2): previous value or the missing value. */
    public long remove(long key1, long key2) {
        int slot = find(key1, key2);
        if (slot < 0) return missingValue;
        long previous = valueLane[slot];
        deleteSlot(slot);
        return previous;
    }

    /** MultiKeyMap.removeAll(k1): removes every mapping whose first key is {@code key1}; true if any was removed. */
    public boolean removeAll(long key1) {
        boolean removed = false;
        for (int slot = 0; slot <= mask; slot++) {
            while (occupied(slot) && key1Lane[slot] == key1) {
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
        Arrays.fill(stateMaskWords, 0L);
        size = 0;
    }

    private void allocate(int capacity) {
        key1Lane = new long[capacity];
        key2Lane = new long[capacity];
        valueLane = new long[capacity];
        stateMaskWords = new long[(capacity + 31) >>> 5];
        mask = capacity - 1;
    }

    private void grow() {
        long[] k1 = key1Lane, k2 = key2Lane, v = valueLane, states = stateMaskWords;
        int oldCapacity = mask + 1;
        allocate(oldCapacity << 1);
        for (int slot = 0; slot < oldCapacity; slot++) {
            if ((states[slot >>> 5] & (STATE_OCCUPIED << ((slot & 31) << 1))) != 0) insert(k1[slot], k2[slot], v[slot]);
        }
    }

    private void insert(long key1, long key2, long value) {
        int slot = home(key1, key2);
        while (occupied(slot)) slot = (slot + 1) & mask;
        key1Lane[slot] = key1;
        key2Lane[slot] = key2;
        valueLane[slot] = value;
        stateMaskWords[slot >>> 5] |= STATE_OCCUPIED << ((slot & 31) << 1);
    }

    private int home(long key1, long key2) {
        long mixed = (key1 * 0x9E3779B97F4A7C15L) ^ (key2 * 0xC2B2AE3D27D4EB4FL);
        mixed ^= mixed >>> 31;
        return (int) (mixed ^ (mixed >>> 32)) & mask;
    }

    private int find(long key1, long key2) {
        for (int slot = home(key1, key2); occupied(slot); slot = (slot + 1) & mask) {
            if (key1Lane[slot] == key1 && key2Lane[slot] == key2) return slot;
        }
        return -1;
    }

    private boolean occupied(int slot) {
        return (stateMaskWords[slot >>> 5] & (STATE_OCCUPIED << ((slot & 31) << 1))) != 0;
    }

    private void deleteSlot(int slot) {
        int gap = slot;
        for (int probe = (gap + 1) & mask; occupied(probe); probe = (probe + 1) & mask) {
            int origin = home(key1Lane[probe], key2Lane[probe]);
            boolean stays = gap <= probe ? gap < origin && origin <= probe : gap < origin || origin <= probe;
            if (stays) continue;
            key1Lane[gap] = key1Lane[probe];
            key2Lane[gap] = key2Lane[probe];
            valueLane[gap] = valueLane[probe];
            gap = probe;
        }
        stateMaskWords[gap >>> 5] &= ~(STATE_OCCUPIED << ((gap & 31) << 1));
        size--;
    }
}
