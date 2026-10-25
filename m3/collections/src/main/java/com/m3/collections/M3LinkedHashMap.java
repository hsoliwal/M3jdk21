/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
// Modified 2026 by Hitesh Soliwal and contributors: adaptive clearing of reusable linked slots.
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// Modified 2026 by Hitesh Soliwal and contributors: lazy expected-key reservation.
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Open-addressed hash index over stable payload slots and primitive encounter links.
 * Null keys/values are supported. Expected O(1) access; O(n) collision worst case.
 * Reversed and sequenced views share storage. No stored Entry or Node objects.
 * Backing arrays are allocated on first insertion; empty owners share zero-length lanes.
 * Erasure back-shifts hash-index coordinates without relocating payload slots;
 * lookup avoids tombstones while deletion scans the affected probe run.
 */
public final class M3LinkedHashMap<K, V> extends M3OrderedSlotMap<K, V> {
    private final Table table;
    private final boolean reversed;
    /** Insertion-order map. */
    public M3LinkedHashMap() { this(false); }
    /**
     * Insertion-order map with an expected distinct-key reservation, applied on first insertion.
     * Construction and empty reads allocate no backing arrays. Very large hints may exhaust
     * allocation only when insertion needs storage; the hint is not an entry-count limit.
     *
     * @param expectedKeys nonnegative expected number of distinct keys
     * @throws IllegalArgumentException if expectedKeys is negative
     */
    public M3LinkedHashMap(int expectedKeys) {
        this(createTable(false, false, expectedKeys), false);
    }
    /** Select insertion order or access order, as with LinkedHashMap. */
    public M3LinkedHashMap(boolean accessOrder) { this(createTable(accessOrder, false, 0), false); }
    M3LinkedHashMap(boolean accessOrder, boolean keyOnly) { this(createTable(accessOrder, keyOnly, 0), false); }
    private M3LinkedHashMap(Table table, boolean reversed) { this.table = table; this.reversed = reversed; }
    @Override int firstSlot() { return reversed ? table.last : table.first; }
    @Override int lastSlot() { return reversed ? table.first : table.last; }
    @Override int nextSlot(int slot) { return reversed ? table.before[slot] : table.after[slot]; }
    @Override int revision() { return table.modifications; }
    @Override int findSlot(Object key) { return table.find(key, M3SlotArrays.hash(key)); }
    @Override K keyAt(int slot) { return M3SlotArrays.get(table.keys, slot); }
    @Override V valueAt(int slot) { return table.keyOnly ? null : M3SlotArrays.get(table.values, slot); }
    @Override V writeAt(int slot, V value) {
        V old = valueAt(slot); if (!table.keyOnly) { table.values[slot] = value; } return old;
    }
    @Override V eraseAt(int slot) { V old = valueAt(slot); forgetEntryFacade(keyAt(slot)); table.erase(slot); return old; }
    @Override void accessed(int slot) { if (table.accessOrder) { table.move(slot, false); } }
    @Override public int size() { return table.size; }
    @Override public V put(K key, V value) {
        int hash = M3SlotArrays.hash(key), s = table.find(key, hash);
        if (s < 0) { table.insert(key, value, hash, false); return null; }
        V old = writeAt(s, value); accessed(s); return old;
    }
    private V positioned(K key, V value, boolean first) {
        int hash = M3SlotArrays.hash(key), s = table.find(key, hash);
        if (s < 0) { table.insert(key, value, hash, first); return null; }
        V old = writeAt(s, value); table.move(s, first); return old;
    }
    @Override public V putFirst(K key, V value) { return positioned(key, value, !reversed); }
    @Override public V putLast(K key, V value) { return positioned(key, value, reversed); }
    @Override public Entry<K, V> firstEntry() { return snapshot(firstSlot()); }
    @Override public Entry<K, V> lastEntry() { return snapshot(lastSlot()); }
    private Entry<K, V> poll(int s) { Entry<K, V> e = snapshot(s); if (s >= 0) { eraseAt(s); } return e; }
    @Override public Entry<K, V> pollFirstEntry() { return poll(firstSlot()); }
    @Override public Entry<K, V> pollLastEntry() { return poll(lastSlot()); }
    @Override public M3LinkedHashMap<K, V> reversed() { return new M3LinkedHashMap<>(table, !reversed); }
    @Override public void clear() { table.clear(); clearEntryFacades(); }
    /** Validate index membership, encounter links and free-slot ownership. */
    public void verifyInvariants() { table.verify(); }

    private static Table createTable(boolean accessOrder, boolean keyOnly, int expectedKeys) {
        if (expectedKeys < 0) { throw new IllegalArgumentException("negative expectedKeys"); }
        if (!com.m3.arrays.M3ArrayNative.isLoaded()) {
            com.m3.arrays.M3ArrayNative.allocateObjectsRequired(0);
        }
        return new Table(accessOrder, keyOnly, expectedKeys);
    }

    private static final class Table {
        private Object[] keys = M3SlotArrays.EMPTY_OBJECTS, values = M3SlotArrays.EMPTY_OBJECTS;
        private int[] hashes = M3SlotArrays.EMPTY_INTS, before = M3SlotArrays.EMPTY_INTS,
                after = M3SlotArrays.EMPTY_INTS, positions = M3SlotArrays.EMPTY_INTS;
        private int[] index = M3SlotArrays.EMPTY_INTS;
        private int first = -1, last = -1, free = -1, used, size, modifications;
        private final boolean accessOrder, keyOnly;
        private final int expectedKeys;
        Table(boolean accessOrder, boolean keyOnly) { this(accessOrder, keyOnly, 0); }
        Table(boolean accessOrder, boolean keyOnly, int expectedKeys) {
            if (expectedKeys < 0) { throw new IllegalArgumentException("negative expectedKeys"); }
            this.accessOrder = accessOrder; this.keyOnly = keyOnly; this.expectedKeys = expectedKeys;
        }
        int find(Object key, int hash) {
            if (index.length == 0) { return -1; }
            for (int p = hash & (index.length - 1); index[p] != 0; p = (p + 1) & (index.length - 1)) {
                int s = index[p] - 1;
                if (s >= 0 && hashes[s] == hash && Objects.equals(key, keys[s])) { return s; }
            }
            return -1;
        }
        int vacant(int hash) {
            int p = hash & (index.length - 1);
            while (index[p] != 0) { p = (p + 1) & (index.length - 1); } return p;
        }
        void rehash(int capacity) {
            int[] replacement = com.m3.arrays.M3ArrayNative.allocateIntsRequired(capacity);
            for (int s = first; s >= 0; s = after[s]) {
                int p = hashes[s] & (capacity - 1);
                while (replacement[p] != 0) { p = (p + 1) & (capacity - 1); }
                replacement[p] = s + 1; positions[s] = p;
            }
            index = replacement;
        }
        private int initialPayloadCapacity() {
            return Math.max(8, Math.min(expectedKeys, Integer.MAX_VALUE - 8));
        }
        private int initialIndexCapacity() {
            int capacity = 16;
            // Insert grows at equality, so the complete expected count must remain below it.
            while (capacity < (1 << 30)
                    && (long) expectedKeys >= (long) capacity - capacity / 3) {
                capacity <<= 1;
            }
            return capacity;
        }
        private static Object[] copyObjects(Object[] source, int length) {
            Object[] target = com.m3.arrays.M3ArrayNative.allocateObjectsRequired(length);
            System.arraycopy(source, 0, target, 0, Math.min(source.length, length));
            return target;
        }
        private static int[] copyInts(int[] source, int length) {
            int[] target = com.m3.arrays.M3ArrayNative.allocateIntsRequired(length);
            System.arraycopy(source, 0, target, 0, Math.min(source.length, length));
            return target;
        }
        void grow() {
            int n = keys.length == 0 ? initialPayloadCapacity() : M3SlotArrays.grow(keys.length);
            Object[] k = copyObjects(keys, n), v = keyOnly ? values : copyObjects(values, n);
            int[] h = copyInts(hashes, n), b = copyInts(before, n), a = copyInts(after, n), p = copyInts(positions, n);
            int[] buckets = index.length == 0 ? com.m3.arrays.M3ArrayNative.allocateIntsRequired(initialIndexCapacity()) : index;
            // Publish only after every payload, metadata and first index allocation succeeds.
            keys = k; values = v; hashes = h; before = b; after = a; positions = p; index = buckets;
        }
        void insert(Object key, Object value, int hash, boolean atFirst) {
            if (free < 0 && used == keys.length) { grow(); }
            if (index.length == 0) { rehash(16); }
            else if (size + 1 >= index.length - index.length / 3) {
                rehash(M3SlotArrays.doubled(index.length));
            }
            int p = vacant(hash), s;
            if (free < 0) { s = used++; } else { s = free; free = after[s]; }
            keys[s] = key; if (!keyOnly) { values[s] = value; } hashes[s] = hash;
            index[p] = s + 1; positions[s] = p; link(s, atFirst); size++; modifications++;
        }
        void link(int s, boolean atFirst) {
            before[s] = atFirst ? -1 : last; after[s] = atFirst ? first : -1;
            if (atFirst) { if (first < 0) { last = s; } else { before[first] = s; } first = s; }
            else { if (last < 0) { first = s; } else { after[last] = s; } last = s; }
        }
        void unlink(int s) {
            int b = before[s], a = after[s];
            if (b < 0) { first = a; } else { after[b] = a; }
            if (a < 0) { last = b; } else { before[a] = b; }
        }
        void move(int s, boolean atFirst) {
            if (s == (atFirst ? first : last)) { return; }
            unlink(s); link(s, atFirst); modifications++;
        }
        void erase(int s) {
            unlink(s);
            // Move hash bucket coordinates only. Payload slot s and all other
            // stable payload slots remain fixed for sequenced API views.
            int mask = index.length - 1;
            int hole = positions[s];
            for (int scan = (hole + 1) & mask; index[scan] != 0; scan = (scan + 1) & mask) {
                int shifted = index[scan] - 1;
                int home = hashes[shifted] & mask;
                if (((scan - home) & mask) >= ((scan - hole) & mask)) {
                    index[hole] = index[scan];
                    positions[shifted] = hole;
                    hole = scan;
                }
            }
            index[hole] = 0;
            keys[s] = null;
            if (!keyOnly) { values[s] = null; }
            before[s] = -1; positions[s] = -1; after[s] = free; free = s;
            size--; modifications++;
        }
        void clear() {
            if (size == 0) { return; }
            // Free slots already hold null; reuse encounter links when few slots remain.
            if (size <= (keys.length >>> 6)) {
                for (int s = first; s >= 0; s = after[s]) {
                    keys[s] = null;
                    if (!keyOnly) { values[s] = null; }
                }
            } else {
                Arrays.fill(keys, null); Arrays.fill(values, null);
            }
            // All hash buckets are emptied; physical payload slot ownership is unchanged.
            Arrays.fill(index, 0);
            first = -1; last = -1; free = -1; used = 0; size = 0; modifications++;
        }
        void verify() {
            boolean[] seen = new boolean[used]; int count = 0, previous = -1, buckets = 0;
            for (int s = first; s >= 0; s = after[s]) {
                if (s >= used || seen[s] || before[s] != previous || index[positions[s]] != s + 1 || find(keys[s], hashes[s]) != s) { throw new AssertionError("hash links"); }
                seen[s] = true; previous = s; count++;
            }
            if (count != size || previous != last) { throw new AssertionError("hash size"); }
            for (int p = 0; p < index.length; p++) {
                if (index[p] < 0) { throw new AssertionError("tombstone"); }
                if (index[p] > 0) {
                    buckets++;
                    if (positions[index[p] - 1] != p) { throw new AssertionError("bucket"); }
                }
            }
            if (buckets != size) { throw new AssertionError("buckets"); }
            for (int s = free; s >= 0; s = after[s]) {
                if (s >= used || seen[s] || keys[s] != null || !keyOnly && values[s] != null) { throw new AssertionError("hash free"); }
                seen[s] = true; count++;
            }
            if (count != used) { throw new AssertionError("hash ownership"); }
        }
    }
}
