// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026: DOOP array/index/mask HashMap storage port; no stored Entry or Node.
package com.m3.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Hash map whose state is held in parallel reference/primitive lanes, never
 * in per-mapping Entry or Node instances. Keys and values occupy reusable
 * physical offsets. An int[] hash index holds physical offset + 1, with zero
 * for never-used and -1 for tombstones; a byte[] is authoritative for slot
 * occupancy and optional owner-side DOOP flags.
 *
 * <p>Entry objects are created only by inherited explicit Map entry APIs.
 * get/put/remove/iteration over keys and values use offsets exclusively.
 * Empty owners share zero-length arrays. The map is not synchronized.
 * Colliding non-comparable keys have linear worst-case lookup time; JDK
 * tree-bin and serialization-shape equivalence are separate porting gates.
 */
public final class M3HashMap<K, V> extends M3SlotMap<K, V>
        implements Cloneable, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    /** Physical slot is occupied; the other bits are optional parent metadata. */
    public static final int LIVE = 0x01;
    public static final int PINNED = 0x02;
    public static final int COLD = 0x04;
    public static final int DEFERRED = 0x08;
    private static final int STATE_MASK = PINNED | COLD | DEFERRED;

    private Object[] keys = M3SlotArrays.EMPTY_OBJECTS;
    private Object[] values = M3SlotArrays.EMPTY_OBJECTS;
    private int[] hashes = M3SlotArrays.EMPTY_INTS;
    private int[] positions = M3SlotArrays.EMPTY_INTS;
    private int[] freeNext = M3SlotArrays.EMPTY_INTS;
    private byte[] state = M3SlotArrays.EMPTY_BYTES;
    private int[] index = M3SlotArrays.EMPTY_INTS;
    private final boolean keyOnly;
    private final int initialBuckets;
    private int used, free = -1, occupied, size, modifications;

    /** Empty, lazily allocated map. */
    public M3HashMap() { this(16, false); }

    /** Sets a bucket capacity hint without allocating any payload on construction. */
    public M3HashMap(int initialCapacity) { this(initialCapacity, false); }

    /** Copy mappings into independent array storage. */
    public M3HashMap(Map<? extends K, ? extends V> source) {
        this();
        putAll(Objects.requireNonNull(source, "source"));
    }

    M3HashMap(boolean keyOnly) { this(16, keyOnly); }

    private M3HashMap(int initialCapacity, boolean keyOnly) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("negative capacity");
        }
        this.initialBuckets = tableSizeFor(Math.max(4L, initialCapacity));
        this.keyOnly = keyOnly;
    }

    private static int tableSizeFor(long desired) {
        if (desired > (1L << 30)) {
            throw new IllegalArgumentException("hash capacity exceeds maximum");
        }
        int cap = 4;
        while (cap < desired) cap <<= 1;
        return cap;
    }

    private static int hash(Object key) { return M3SlotArrays.hash(key); }

    private int find(Object key, int hash) {
        if (index.length == 0) return -1;
        int mask = index.length - 1;
        for (int p = hash & mask, ref; (ref = index[p]) != 0; p = (p + 1) & mask) {
            if (ref > 0) {
                int s = ref - 1;
                if (hashes[s] == hash && Objects.equals(key, keys[s])) return s;
            }
        }
        return -1;
    }

    @Override int findSlot(Object key) { return find(key, hash(key)); }
    @Override int firstSlot() { return nextSlot(-1); }
    @Override int nextSlot(int previous) {
        for (int s = previous + 1; s < used; s++) {
            if ((state[s] & LIVE) != 0) return s;
        }
        return -1;
    }
    @Override boolean live(int slot) { return (state[slot] & LIVE) != 0; }
    @Override K keyAt(int slot) { return M3SlotArrays.get(keys, slot); }
    @Override V valueAt(int slot) { return keyOnly ? null : M3SlotArrays.get(values, slot); }
    @Override V writeAt(int slot, V value) {
        V previous = valueAt(slot);
        if (!keyOnly) values[slot] = value;
        return previous;
    }
    @Override int revision() { return modifications; }
    @Override public int size() { return size; }
    @Override V eraseAt(int slot) {
        V previous = valueAt(slot);
        index[positions[slot]] = -1;
        keys[slot] = null;
        if (!keyOnly) values[slot] = null;
        state[slot] = 0;
        positions[slot] = -1;
        freeNext[slot] = free;
        free = slot;
        size--;
        modifications++;
        return previous;
    }

    @Override public V put(K key, V value) {
        int h = hash(key), existing = find(key, h);
        if (existing >= 0) return writeAt(existing, value);

        // All allocation happens before claiming a slot or publishing a mapping.
        if (free < 0 && used == keys.length) growPayload();
        if (index.length == 0) {
            rehash(initialBuckets);
        } else if (occupied + 1 >= index.length - (index.length / 3)) {
            int capacity = size + 1 >= index.length - (index.length / 3)
                    ? M3SlotArrays.doubled(index.length) : index.length;
            rehash(capacity);
        }

        int mask = index.length - 1, firstDeleted = -1, p = h & mask;
        while (index[p] != 0) {
            if (index[p] < 0 && firstDeleted < 0) firstDeleted = p;
            p = (p + 1) & mask;
        }
        if (firstDeleted >= 0) p = firstDeleted;
        if (index[p] == 0) occupied++;

        int s;
        if (free < 0) s = used++;
        else { s = free; free = freeNext[s]; }
        keys[s] = key;
        if (!keyOnly) values[s] = value;
        hashes[s] = h;
        state[s] = LIVE;
        positions[s] = p;
        index[p] = s + 1;
        size++;
        modifications++;
        return null;
    }

    private void growPayload() {
        int capacity = M3SlotArrays.grow(keys.length);
        Object[] k = Arrays.copyOf(keys, capacity);
        Object[] v = keyOnly ? values : Arrays.copyOf(values, capacity);
        int[] h = Arrays.copyOf(hashes, capacity);
        int[] p = Arrays.copyOf(positions, capacity);
        int[] f = Arrays.copyOf(freeNext, capacity);
        byte[] flags = Arrays.copyOf(state, capacity);
        keys = k; values = v; hashes = h; positions = p;
        freeNext = f; state = flags;
    }

    private void rehash(int capacity) {
        int[] replacement = new int[capacity];
        int mask = capacity - 1;
        for (int s = 0; s < used; s++) {
            if ((state[s] & LIVE) == 0) continue;
            int p = hashes[s] & mask;
            while (replacement[p] != 0) p = (p + 1) & mask;
            replacement[p] = s + 1;
            positions[s] = p;
        }
        index = replacement;
        occupied = size;
    }

    @Override public void clear() {
        Arrays.fill(keys, 0, used, null);
        if (!keyOnly) Arrays.fill(values, 0, used, null);
        Arrays.fill(hashes, 0, used, 0);
        Arrays.fill(positions, 0, used, 0);
        Arrays.fill(freeNext, 0, used, 0);
        Arrays.fill(state, 0, used, (byte) 0);
        Arrays.fill(index, 0);
        used = occupied = size = 0;
        free = -1;
        modifications++;
    }

    /** Slot flags are metadata, not element reachability or a GC instruction. */
    public int stateBits(Object key) {
        int slot = findSlot(key);
        return slot < 0 ? 0 : Byte.toUnsignedInt(state[slot]);
    }

    /** Change optional metadata without ever clearing the LIVE state bit. */
    public boolean updateState(Object key, int setBits, int clearBits) {
        if (((setBits | clearBits) & ~STATE_MASK) != 0) {
            throw new IllegalArgumentException("unsupported slot state bits");
        }
        int slot = findSlot(key);
        if (slot < 0) return false;
        state[slot] = (byte) ((state[slot] | setBits) & ~clearBits);
        return true;
    }

    /** Retained key/value payload references; entries are never stored. */
    public int allocatedReferenceSlots() {
        return keys.length + values.length;
    }

    /** Diagnostics: array/index/flag and tombstone/free-list consistency. */
    public void verifyInvariants() {
        if ((keys.length != state.length) || hashes.length != state.length
                || positions.length != state.length || freeNext.length != state.length
                || (!keyOnly && values.length != state.length)) {
            throw new AssertionError("parallel lane lengths");
        }
        boolean[] accounted = new boolean[used];
        int counted = 0, occupiedCount = 0;
        for (int p = 0; p < index.length; p++) {
            int ref = index[p];
            if (ref != 0) occupiedCount++;
            if (ref <= 0) continue;
            int s = ref - 1;
            if (s < 0 || s >= used || accounted[s] || !live(s)
                    || positions[s] != p) throw new AssertionError("indexed slot");
            accounted[s] = true;
            counted++;
        }
        if (occupiedCount != occupied || counted != size)
            throw new AssertionError("index count");
        int freeCount = 0;
        for (int s = free; s >= 0; s = freeNext[s]) {
            if (s >= used || accounted[s] || live(s)) throw new AssertionError("free slot");
            accounted[s] = true;
            freeCount++;
            if (freeCount > used) throw new AssertionError("free cycle");
        }
        if (size + freeCount != used) throw new AssertionError("slot ownership");
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override public M3HashMap<K, V> clone() {
        M3HashMap<K, V> copy = new M3HashMap<>(Math.max(initialBuckets, index.length), keyOnly);
        for (int slot = firstSlot(); slot >= 0; slot = nextSlot(slot)) {
            copy.put(keyAt(slot), valueAt(slot));
            copy.updateState(keyAt(slot), state[slot] & STATE_MASK, 0);
        }
        return copy;
    }
}
