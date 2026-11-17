/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Array-owned small map with up to three inline slots, followed by a primitive
 * slot-addressed hash regime. No per-entry nodes or retained entry facades.
 *
 * <p>Null keys and values are supported. Hash mode is retained until clear or
 * the final removal, avoiding repeated representation conversions. This is a
 * single-threaded M3 SDK map, not a replacement for {@code java.util.HashMap}.
 * Uses the existing {@link SlotArrays} hash and capacity kernels.</p>
 */
public final class Flat3Map<K, V> {
    /** State-mask bit {@code OCCUPIED} (per-slot state word, bit 0). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code REGIME_FLAT} (per-slot state word, bit 1). */
    static final long STATE_REGIME_FLAT = 1L << 1;
    /** State-mask bit {@code REGIME_HASH} (per-slot state word, bit 2). */
    static final long STATE_REGIME_HASH = 1L << 2;
    /**
     * Hashed-regime key-lane sentinels: a never-used slot holds EMPTY, a removed one REMOVED. null
     * stays a real key; the sentinels never escape (only live slots are returned or compared).
     */
    //FLLDCIM: LEETCODIFY - EMPTY sentinel replaces the packed two-bit state lane
    private static final Object EMPTY = new Object();
    private static final Object REMOVED = new Object();
    private static final int INLINE_CAPACITY = 3;
    private static final int INITIAL_HASH_CAPACITY = 8;
    private static final String NOT_LIVE = "M3Flat3Map: slot is not live";

    /** Storage lane {@code keyLane}: no per-entry objects are retained. */
    private Object[] keyLane = new Object[0];
    /** Storage lane {@code valueLane}: aligned with {@code keyLane}. */
    private Object[] valueLane = new Object[0];
    /** Live slot count. */
    private int size;
    /** Number of hashed slots occupied or tombstoned since last rehash. */
    private int occupied;
    private boolean hashed;

    public Flat3Map() { }

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
        if (hashed) removeHashed(slot); else removeInline(slot);
        return prior;
    }

    //FLLDCIM: MRL - L075 method split: remove(Object) and removeSlot(int) share the two removal bodies
    private void removeHashed(int slot) {
        keyLane[slot] = REMOVED;
        valueLane[slot] = null;
        size--;
        if (size == 0) reset();
    }

    private void removeInline(int slot) {
        int moved = size - slot - 1;
        if (moved > 0) {
            System.arraycopy(keyLane, slot + 1, keyLane, slot, moved);
            System.arraycopy(valueLane, slot + 1, valueLane, slot, moved);
        }
        --size;
        keyLane[size] = null;
        valueLane[size] = null;
    }

    public boolean containsKey(Object key) { return findSlot(key) >= 0; }

    public boolean containsValue(Object value) {
        if (!hashed) {
            for (int i = 0; i < size; i++) {
                if (Objects.equals(value, valueLane[i])) return true;
            }
        } else {
            for (int i = 0; i < keyLane.length; i++) {
                if (live(keyLane[i]) && Objects.equals(value, valueLane[i])) return true;
            }
        }
        return false;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() { reset(); }

    //FLLDCIM: MRL - L075 single owner: slot cursor for the Commons facade jdk.m3.collections.apache.M3Flat3Map
    /**
     * Returns the first live slot at or after {@code from}, or -1 when there is none. Ascending slot
     * order is this map's iteration order: insertion order while inline (a removal shifts the later
     * slots down), table order once hashed. A cursor stays valid across {@link #setSlotValue} and
     * {@link #removeSlot}; any other structural change ends it.
     */
    public int nextSlot(int from) {
        int end = hashed ? keyLane.length : size;
        for (int slot = Math.max(from, 0); slot < end; slot++) {
            if (!hashed || live(keyLane[slot])) return slot;
        }
        return -1;
    }

    /** Returns the key of live slot {@code slot}; IllegalStateException when the slot is not live. */
    @SuppressWarnings("unchecked")
    public K slotKey(int slot) { return (K) keyLane[liveSlot(slot)]; }

    /** Returns the value of live slot {@code slot}; IllegalStateException when the slot is not live. */
    public V slotValue(int slot) { return valueAt(liveSlot(slot)); }

    /** Replaces the value of live slot {@code slot} without a structural change; returns the old value. */
    public V setSlotValue(int slot, V value) {
        V previous = valueAt(liveSlot(slot));
        valueLane[slot] = value;
        return previous;
    }

    /**
     * Removes the mapping of live slot {@code slot} without a key lookup and returns the slot a cursor
     * resumes from: the same slot while inline (the later slots shift down), the next one once hashed.
     */
    public int removeSlot(int slot) {
        if (!hashed) {
            removeInline(liveSlot(slot));
            return slot;
        }
        removeHashed(liveSlot(slot));
        return slot + 1;
    }

    /** Returns an independent map with the same mappings, regime and slot (iteration) order. */
    public Flat3Map<K, V> copy() {
        Flat3Map<K, V> copy = new Flat3Map<>();
        copy.keyLane = keyLane.clone();
        copy.valueLane = valueLane.clone();
        copy.size = size;
        copy.occupied = occupied;
        copy.hashed = hashed;
        return copy;
    }

    private int liveSlot(int slot) {
        if (slot < 0 || slot >= (hashed ? keyLane.length : size)
                || hashed && !live(keyLane[slot])) {
            throw new IllegalStateException(NOT_LIVE);
        }
        return slot;
    }

    private void reset() {
        keyLane = SlotArrays.EMPTY_OBJECTS;
        valueLane = SlotArrays.EMPTY_OBJECTS;
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
        int index = SlotArrays.hash(key) & (keyLane.length - 1);
        for (;;) {
            Object k = keyLane[index];
            if (k == EMPTY) return -1;
            if (k != REMOVED && Objects.equals(key, k)) return index;
            index = (index + 1) & (keyLane.length - 1);
        }
    }

    private void transitionToHash(K newKey, V newValue) {
        Object[] keys = emptyKeys(INITIAL_HASH_CAPACITY);
        Object[] values = new Object[INITIAL_HASH_CAPACITY];
        for (int i = 0; i < size; i++) {
            insertUnoccupied(keys, values, SlotArrays.hash(keyLane[i]), keyLane[i], valueLane[i]);
        }
        insertUnoccupied(keys, values, SlotArrays.hash(newKey), newKey, newValue);
        // Rehash can invoke user-defined hashCode: do not publish partial lanes.
        keyLane = keys;
        valueLane = values;
        hashed = true;
        size++;
        occupied = size;
    }

    private V putHashed(K key, V value) {
        int hash = SlotArrays.hash(key);
        int mask = keyLane.length - 1;
        int index = hash & mask;
        int deleted = -1;
        for (;;) {
            Object k = keyLane[index];
            if (k == EMPTY) break;
            if (k != REMOVED) {
                if (Objects.equals(key, k)) {
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
            rehash(SlotArrays.doubled(keyLane.length));
            mask = keyLane.length - 1;
            index = hash & mask;
            while (keyLane[index] != EMPTY) index = (index + 1) & mask;
        }
        if (keyLane[index] == EMPTY) occupied++;
        keyLane[index] = key;
        valueLane[index] = value;
        size++;
        return null;
    }

    private void rehash(int capacity) {
        Object[] keys = emptyKeys(capacity);
        Object[] values = new Object[capacity];
        for (int i = 0; i < keyLane.length; i++) {
            if (live(keyLane[i])) {
                insertUnoccupied(keys, values, SlotArrays.hash(keyLane[i]), keyLane[i],
                        valueLane[i]);
            }
        }
        keyLane = keys;
        valueLane = values;
        occupied = size;
    }

    private static void insertUnoccupied(Object[] keys, Object[] values, int hash, Object key,
                                         Object value) {
        int mask = keys.length - 1;
        int index = hash & mask;
        while (keys[index] != EMPTY) index = (index + 1) & mask;
        keys[index] = key;
        values[index] = value;
    }

    /** A hashed key lane: every slot starts EMPTY (MASK addressing; capacity is a power of two). */
    private static Object[] emptyKeys(int capacity) {
        Object[] keys = new Object[capacity];
        Arrays.fill(keys, EMPTY);
        return keys;
    }

    /** A hashed slot is live when it holds neither sentinel (a live key may be null). */
    private static boolean live(Object key) {
        return key != EMPTY && key != REMOVED;
    }

    @SuppressWarnings("unchecked")
    private V valueAt(int slot) { return (V) valueLane[slot]; }
}
