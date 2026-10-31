/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Array-owned small map with up to three inline slots, followed by a primitive
 * slot-addressed hash regime. No per-entry nodes or retained entry facades.
 *
 * <p>Null keys and values are supported. Hash mode is retained until clear or
 * the final removal, avoiding repeated representation conversions. This is a
 * single-threaded M3 SDK map, not a replacement for {@code java.util.HashMap}.
 * Uses the existing {@link M3SlotArrays} hash and capacity kernels.</p>
 */
public final class M3Flat3Map<K, V> {
    /** State-mask bit {@code OCCUPIED} (per-slot state word, bit 0). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code REGIME_FLAT} (per-slot state word, bit 1). */
    static final long STATE_REGIME_FLAT = 1L << 1;
    /** State-mask bit {@code REGIME_HASH} (per-slot state word, bit 2). */
    static final long STATE_REGIME_HASH = 1L << 2;
    /** Each hash slot is encoded using two packed bits: 0 empty, 1 live, 2 deleted. */
    private static final int SLOTS_PER_WORD_SHIFT = 5;
    private static final int SLOTS_PER_WORD = 1 << SLOTS_PER_WORD_SHIFT;
    private static final long PACKED_STATE_MASK = 3L;
    private static final long PACKED_USED = 1L;
    private static final long PACKED_DELETED = 2L;
    private static final int INLINE_CAPACITY = 3;
    private static final int INITIAL_HASH_CAPACITY = 8;
    private static final long[] EMPTY_STATES = new long[0];

    /** Storage lane {@code keyLane}: no per-entry objects are retained. */
    private Object[] keyLane = new Object[0];
    /** Storage lane {@code valueLane}: aligned with {@code keyLane}. */
    private Object[] valueLane = new Object[0];
    /** Packed two-bit hash states; inline regime uses size instead of a state array. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;
    /** Number of hashed slots occupied or tombstoned since last rehash. */
    private int occupied;
    private boolean hashed;

    public M3Flat3Map() { }

    /** Retains the existing scaffold metadata descriptor for compatible tooling. */
    public static String scaffoldDescriptor() {
        return "M3Flat3Map|FLAT3_MAP|OBJECT|OCCUPIED,REGIME_FLAT,REGIME_HASH|Object[] keyLane;Object[] valueLane";
    }

    /** Returns the existing mapping or null (also when a present key maps to null). */
    public V get(Object key) {
        int slot = findSlot(key);
        return slot < 0 ? null : valueAt(slot);
    }

    /** Inserts or replaces one mapping; storage remains inline for at most three keys. */
    public V put(K key, V value) {
        if (hashed) return putHashed(key, value);
        int found = findFlat(key);
        if (found >= 0) {
            V previous = valueAt(found);
            valueLane[found] = value;
            return previous;
        }
        if (size == INLINE_CAPACITY) {
            transitionToHash(key, value);
            return null;
        }
        if (keyLane.length == 0) {
            // Publish all aligned lanes only after every allocation succeeds.
            Object[] keys = new Object[INLINE_CAPACITY];
            Object[] values = new Object[INLINE_CAPACITY];
            keyLane = keys;
            valueLane = values;
        }
        keyLane[size] = key;
        valueLane[size] = value;
        size++;
        return null;
    }

    /** Removes one mapping; a missing key returns null without modification. */
    public V remove(Object key) {
        int slot = findSlot(key);
        if (slot < 0) return null;
        V prior = valueAt(slot);
        if (hashed) {
            keyLane[slot] = null;
            valueLane[slot] = null;
            setState(stateMaskWords, slot, PACKED_DELETED);
            size--;
            if (size == 0) reset();
        } else {
            int moved = size - slot - 1;
            if (moved > 0) {
                System.arraycopy(keyLane, slot + 1, keyLane, slot, moved);
                System.arraycopy(valueLane, slot + 1, valueLane, slot, moved);
            }
            --size;
            keyLane[size] = null;
            valueLane[size] = null;
        }
        return prior;
    }

    public boolean containsKey(Object key) { return findSlot(key) >= 0; }

    public boolean containsValue(Object value) {
        if (!hashed) {
            for (int i = 0; i < size; i++) {
                if (Objects.equals(value, valueLane[i])) return true;
            }
        } else {
            for (int i = 0; i < keyLane.length; i++) {
                if (state(stateMaskWords, i) == PACKED_USED
                        && Objects.equals(value, valueLane[i])) return true;
            }
        }
        return false;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() { reset(); }

    private void reset() {
        keyLane = M3SlotArrays.EMPTY_OBJECTS;
        valueLane = M3SlotArrays.EMPTY_OBJECTS;
        stateMaskWords = EMPTY_STATES;
        size = 0;
        occupied = 0;
        hashed = false;
    }

    private int findSlot(Object key) {
        return hashed ? findHashed(key) : findFlat(key);
    }

    private int findFlat(Object key) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(key, keyLane[i])) return i;
        }
        return -1;
    }

    private int findHashed(Object key) {
        int index = M3SlotArrays.hash(key) & (keyLane.length - 1);
        for (;;) {
            long slotState = state(stateMaskWords, index);
            if (slotState == 0) return -1;
            if (slotState == PACKED_USED && Objects.equals(key, keyLane[index])) {
                return index;
            }
            index = (index + 1) & (keyLane.length - 1);
        }
    }

    private void transitionToHash(K newKey, V newValue) {
        Object[] keys = new Object[INITIAL_HASH_CAPACITY];
        Object[] values = new Object[INITIAL_HASH_CAPACITY];
        long[] states = new long[wordsFor(INITIAL_HASH_CAPACITY)];
        for (int i = 0; i < size; i++) {
            insertUnoccupied(keys, values, states, M3SlotArrays.hash(keyLane[i]),
                    keyLane[i], valueLane[i]);
        }
        insertUnoccupied(keys, values, states, M3SlotArrays.hash(newKey), newKey, newValue);
        // Rehash can invoke user-defined hashCode: do not publish partial lanes.
        keyLane = keys;
        valueLane = values;
        stateMaskWords = states;
        hashed = true;
        size++;
        occupied = size;
    }

    private V putHashed(K key, V value) {
        int hash = M3SlotArrays.hash(key);
        int mask = keyLane.length - 1;
        int index = hash & mask;
        int deleted = -1;
        for (;;) {
            long slotState = state(stateMaskWords, index);
            if (slotState == 0) break;
            if (slotState == PACKED_USED) {
                if (Objects.equals(key, keyLane[index])) {
                    V previous = valueAt(index);
                    valueLane[index] = value;
                    return previous;
                }
            } else if (deleted < 0) {
                deleted = index;
            }
            index = (index + 1) & mask;
        }
        if (deleted >= 0) {
            index = deleted;
        } else if ((long) (occupied + 1) * 3 > (long) keyLane.length * 2) {
            rehash(M3SlotArrays.doubled(keyLane.length));
            mask = keyLane.length - 1;
            index = hash & mask;
            while (state(stateMaskWords, index) != 0) index = (index + 1) & mask;
        }
        if (state(stateMaskWords, index) == 0) occupied++;
        keyLane[index] = key;
        valueLane[index] = value;
        setState(stateMaskWords, index, PACKED_USED);
        size++;
        return null;
    }

    private void rehash(int capacity) {
        Object[] keys = new Object[capacity];
        Object[] values = new Object[capacity];
        long[] states = new long[wordsFor(capacity)];
        for (int i = 0; i < keyLane.length; i++) {
            if (state(stateMaskWords, i) == PACKED_USED) {
                insertUnoccupied(keys, values, states, M3SlotArrays.hash(keyLane[i]),
                        keyLane[i], valueLane[i]);
            }
        }
        keyLane = keys;
        valueLane = values;
        stateMaskWords = states;
        occupied = size;
    }

    private static void insertUnoccupied(Object[] keys, Object[] values, long[] states,
                                         int hash, Object key, Object value) {
        int mask = keys.length - 1;
        int index = hash & mask;
        while (state(states, index) != 0) index = (index + 1) & mask;
        keys[index] = key;
        values[index] = value;
        setState(states, index, PACKED_USED);
    }

    private static int wordsFor(int capacity) {
        return (capacity + SLOTS_PER_WORD - 1) >>> SLOTS_PER_WORD_SHIFT;
    }

    private static long state(long[] words, int slot) {
        return (words[slot >>> SLOTS_PER_WORD_SHIFT]
                >>> ((slot & (SLOTS_PER_WORD - 1)) << 1)) & PACKED_STATE_MASK;
    }

    private static void setState(long[] words, int slot, long value) {
        int index = slot >>> SLOTS_PER_WORD_SHIFT;
        int shift = (slot & (SLOTS_PER_WORD - 1)) << 1;
        words[index] = (words[index] & ~(PACKED_STATE_MASK << shift))
                | (value << shift);
    }

    @SuppressWarnings("unchecked")
    private V valueAt(int slot) { return (V) valueLane[slot]; }
}
