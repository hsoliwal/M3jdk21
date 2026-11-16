/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Bounded map that evicts the least recently used mapping, stored in parallel array lanes.
 *
 * <p>A mapping lives in one slot of the key, value and cached-hash lanes. Two int lanes link the slots from the least to
 * the most recently used one, and an open-addressed int lane maps a hash to its slot. A lookup, an update, a removal and
 * an eviction are expected O(1); no node or entry object is stored. A put into a full map writes the new mapping into
 * the slot of the least recently used one. Removing a mapping closes its gap in the hash lane, so the lane never fills
 * with dead entries and is rebuilt only when the slot lanes grow. Null keys and null values are allowed.</p>
 *
 * <p>Provider comparison: Apache Commons Collections {@code LRUMap} (Apache-2.0): the same results, recency order and
 * iterator failure rules for the operations declared here. The slot lanes start small and double up to
 * {@link #maxSize()}; a map holds at most 2<sup>29</sup> mappings. Not thread safe. Not a {@code java.util}
 * replacement.</p>
 */
public final class LruMap<K, V> {
    /** State-mask bit {@code OCCUPIED} (per-slot state word, bit 0). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code DELETED} (per-slot state word, bit 1). */
    static final long STATE_DELETED = 1L << 1;
    /** Size bound of a map created without one. */
    static final int DEFAULT_MAX_SIZE = 100;
    private static final int MIN_SLOTS = 4;
    private static final int MAX_SLOTS = 1 << 29;
    private static final int NONE = -1;
    private static final int GOLDEN_RATIO_32 = 0x9E3779B9;
    private static final int FOLD_SHIFT = 16;

    /** Storage lane {@code keyLane}: the key of each slot. */
    private Object[] keyLane = new Object[0];
    /** Storage lane {@code valueLane}: the value of each slot. */
    private Object[] valueLane = new Object[0];
    /** Storage lane {@code hashLane}: the key hash of each slot, so no key is hashed twice. */
    private int[] hashLane = new int[0];
    /** Storage lane {@code prevLane}: the next less recently used slot, or -1. */
    private int[] prevLane = new int[0];
    /** Storage lane {@code nextLane}: the next more recently used slot, or the next free slot, or -1. */
    private int[] nextLane = new int[0];
    /** Storage lane {@code hashIndexLane}: open-addressed table of slot + 1; 0 is empty; at most half full. */
    private int[] hashIndexLane = new int[0];
    /** Per-slot state words (OCCUPIED,DELETED); one bit set per slot and state. */
    private long[] stateMaskWords = new long[0];
    private final int maxSize;
    /** Live slot count. */
    private int size;
    /** Slots handed out so far; slots at or above it were never used. */
    private int used;
    private int head = NONE;
    private int tail = NONE;
    private int free = NONE;
    private int modCount;
    private Map<K, V> view;

    /** Creates an empty map bounded at {@value #DEFAULT_MAX_SIZE} mappings. */
    public LruMap() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * Creates an empty map bounded at {@code maxSize} mappings.
     *
     * @param maxSize the largest number of mappings kept
     * @throws IllegalArgumentException if {@code maxSize} is less than 1
     */
    public LruMap(int maxSize) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be greater than 0: " + maxSize);
        }
        this.maxSize = maxSize;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "LruMap|LRU_MAP|OBJECT|OCCUPIED,DELETED|Object[] keyLane;Object[] valueLane;int[] prevLane;int[] nextLane;int[] hashIndexLane;int[] hashLane";
    }

    /** Returns the largest number of mappings kept. */
    public int maxSize() {
        return maxSize;
    }

    /** Returns whether the next new key evicts the least recently used mapping. */
    public boolean isFull() {
        return size >= maxSize;
    }

    /** Returns the value of {@code key}, which becomes the most recently used mapping, or null. */
    public V get(Object key) {
        return get(key, true);
    }

    /**
     * Returns the value of {@code key}, or null.
     *
     * @param key the key to look up
     * @param updateToMRU whether a found mapping becomes the most recently used one
     */
    public V get(Object key, boolean updateToMRU) {
        int at = find(key, hash(key));
        if (at < 0) {
            return null;
        }
        int slot = hashIndexLane[at] - 1;
        if (updateToMRU) {
            touch(slot);
        }
        return value(slot);
    }

    /**
     * Maps {@code key} to {@code value} as the most recently used mapping; a new key in a full map replaces the least
     * recently used mapping.
     *
     * @return the value the key had, or null
     */
    public V put(K key, V value) {
        int hash = hash(key);
        int at = find(key, hash);
        if (at < 0) {
            insert(key, value, hash);
            return null;
        }
        int slot = hashIndexLane[at] - 1;
        V old = value(slot);
        touch(slot);
        valueLane[slot] = value;
        return old;
    }

    /**
     * Removes the mapping of {@code key}.
     *
     * @return the value the key had, or null
     */
    public V remove(Object key) {
        int at = find(key, hash(key));
        return at < 0 ? null : erase(at);
    }

    /** Returns whether {@code key} is mapped; the recency order does not change. */
    public boolean containsKey(Object key) {
        return find(key, hash(key)) >= 0;
    }

    /**
     * Returns the least recently used key.
     *
     * @throws NoSuchElementException if the map is empty
     */
    public K firstKey() {
        return key(end(head));
    }

    /**
     * Returns the most recently used key.
     *
     * @throws NoSuchElementException if the map is empty
     */
    public K lastKey() {
        return key(end(tail));
    }

    /**
     * Returns a {@code java.util.Map} view of this map. It iterates from the least to the most recently used mapping,
     * its {@code get} and {@code put} update the recency order, and its iterators fail when the order or the content
     * changes behind them.
     */
    public Map<K, V> asMap() {
        return view == null ? view = new View() : view;
    }

    /** Returns the number of mappings. */
    public int size() {
        return size;
    }

    /** Returns whether there is no mapping. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Removes every mapping and keeps the lanes. */
    public void clear() {
        Arrays.fill(keyLane, 0, used, null);
        Arrays.fill(valueLane, 0, used, null);
        Arrays.fill(stateMaskWords, 0, used, 0L);
        Arrays.fill(hashIndexLane, 0);
        head = tail = free = NONE;
        size = used = 0;
        modCount++;
    }

    private static int hash(Object key) {
        return key == null ? 0 : key.hashCode();
    }

    /** Spreads a hash over the hash lane: one multiplication, then the high half folded onto the low half. */
    private static int mix(int hash) {
        int spread = hash * GOLDEN_RATIO_32;
        return spread ^ spread >>> FOLD_SHIFT;
    }

    @SuppressWarnings("unchecked")
    private K key(int slot) {
        return (K) keyLane[slot];
    }

    @SuppressWarnings("unchecked")
    private V value(int slot) {
        return (V) valueLane[slot];
    }

    private int end(int slot) {
        if (slot < 0) {
            throw new NoSuchElementException("Map is empty");
        }
        return slot;
    }

    /** The hash lane position of {@code key}, or -1; the lookup key is the receiver of {@code equals}. */
    private int find(Object key, int hash) {
        int[] index = hashIndexLane;
        int mask = index.length - 1;
        for (int at = mix(hash) & mask; mask >= 0 && index[at] != 0; at = at + 1 & mask) {
            int slot = index[at] - 1;
            if (hashLane[slot] == hash && (key == keyLane[slot] || key != null && key.equals(keyLane[slot]))) {
                return at;
            }
        }
        return NONE;
    }

    /** The hash lane position that holds {@code slot}. */
    private int positionOf(int slot) {
        int mask = hashIndexLane.length - 1;
        int at = mix(hashLane[slot]) & mask;
        while (hashIndexLane[at] != slot + 1) {
            at = at + 1 & mask;
        }
        return at;
    }

    private void place(int[] index, int slot) {
        int mask = index.length - 1;
        int at = mix(hashLane[slot]) & mask;
        while (index[at] != 0) {
            at = at + 1 & mask;
        }
        index[at] = slot + 1;
    }

    /** Empties position {@code at} and moves later entries of the same run back, so no probe meets a dead entry. */
    private void unindex(int at) {
        int[] index = hashIndexLane;
        int mask = index.length - 1;
        for (int probe = at + 1 & mask; index[probe] != 0; probe = probe + 1 & mask) {
            int home = mix(hashLane[index[probe] - 1]) & mask;
            if ((probe - home & mask) >= (probe - at & mask)) {
                index[at] = index[probe];
                at = probe;
            }
        }
        index[at] = 0;
    }

    private void append(int slot) {
        prevLane[slot] = tail;
        nextLane[slot] = NONE;
        if (tail < 0) {
            head = slot;
        } else {
            nextLane[tail] = slot;
        }
        tail = slot;
    }

    private void unlink(int slot) {
        int before = prevLane[slot];
        int after = nextLane[slot];
        if (before < 0) {
            head = after;
        } else {
            nextLane[before] = after;
        }
        if (after < 0) {
            tail = before;
        } else {
            prevLane[after] = before;
        }
    }

    /** Makes {@code slot} the most recently used one; an order change invalidates running iterators. */
    private void touch(int slot) {
        if (slot != tail) {
            modCount++;
            unlink(slot);
            append(slot);
        }
    }

    private void insert(K key, V value, int hash) {
        int slot = size >= maxSize ? recycled() : vacant();
        keyLane[slot] = key;
        valueLane[slot] = value;
        hashLane[slot] = hash;
        place(hashIndexLane, slot);
        append(slot);
        modCount++;
    }

    /** The slot of the least recently used mapping, taken out of the hash lane and the links for reuse. */
    private int recycled() {
        int slot = head;
        unindex(positionOf(slot));
        unlink(slot);
        return slot;
    }

    /** A free slot, else the next unused one; the lanes grow when none is left. */
    private int vacant() {
        int slot = free;
        if (slot >= 0) {
            free = nextLane[slot];
        } else {
            if (used == keyLane.length) {
                grow();
            }
            slot = used++;
        }
        stateMaskWords[slot] = STATE_OCCUPIED;
        size++;
        return slot;
    }

    /** Doubles the slot lanes up to the size bound; every lane is staged before any is published. */
    private void grow() {
        int slots = widened(keyLane.length);
        Object[] keys = Arrays.copyOf(keyLane, slots);
        Object[] values = Arrays.copyOf(valueLane, slots);
        int[] hashes = Arrays.copyOf(hashLane, slots);
        int[] previous = Arrays.copyOf(prevLane, slots);
        int[] next = Arrays.copyOf(nextLane, slots);
        long[] states = Arrays.copyOf(stateMaskWords, slots);
        int[] index = indexOf(slots);
        keyLane = keys;
        valueLane = values;
        hashLane = hashes;
        prevLane = previous;
        nextLane = next;
        stateMaskWords = states;
        hashIndexLane = index;
    }

    /** The lane length after {@code slots}: doubled, at least the minimum and at most the size bound. */
    private int widened(int slots) {
        if (slots >= MAX_SLOTS) {
            throw new IllegalStateException("more than " + MAX_SLOTS + " mappings");
        }
        return (int) Math.min(Math.min(maxSize, MAX_SLOTS), Math.max(MIN_SLOTS, 2L * slots));
    }

    /** A hash lane for {@code slots} slots, at most half full, holding every linked slot. */
    private int[] indexOf(int slots) {
        int[] index = new int[Integer.highestOneBit(Math.max(2 * slots, 2 * MIN_SLOTS) - 1) << 1];
        for (int slot = head; slot >= 0; slot = nextLane[slot]) {
            place(index, slot);
        }
        return index;
    }

    private V erase(int at) {
        int slot = hashIndexLane[at] - 1;
        V old = value(slot);
        unindex(at);
        unlink(slot);
        keyLane[slot] = null;
        valueLane[slot] = null;
        nextLane[slot] = free;
        free = slot;
        stateMaskWords[slot] = STATE_DELETED;
        size--;
        modCount++;
        return old;
    }

    /** The {@code java.util.Map} view: the keyed operations are the owner's, the rest follows from the entry set. */
    private final class View extends AbstractMap<K, V> {
        private final Set<Map.Entry<K, V>> entries = new AbstractSet<>() {
            @Override public Iterator<Map.Entry<K, V>> iterator() { return size == 0 ? Collections.emptyIterator() : new Walk(); }
            @Override public int size() { return size; }
            @Override public void clear() { LruMap.this.clear(); }
        };

        @Override public int size() { return size; }
        @Override public boolean containsKey(Object key) { return LruMap.this.containsKey(key); }
        @Override public V get(Object key) { return LruMap.this.get(key); }
        @Override public V put(K key, V value) { return LruMap.this.put(key, value); }
        @Override public V remove(Object key) { return LruMap.this.remove(key); }
        @Override public void clear() { LruMap.this.clear(); }
        @Override public Set<Map.Entry<K, V>> entrySet() { return entries; }
    }

    /** Walks the slots from the least to the most recently used one. */
    private final class Walk implements Iterator<Map.Entry<K, V>> {
        private int next = head;
        private int last = NONE;
        private int expected = modCount;

        @Override public boolean hasNext() { return next >= 0; }

        @Override public Map.Entry<K, V> next() {
            unchanged();
            if (next < 0) {
                throw new NoSuchElementException();
            }
            last = next;
            next = nextLane[next];
            return new SlotEntry(last);
        }

        @Override public void remove() {
            if (last < 0) {
                throw new IllegalStateException();
            }
            unchanged();
            erase(positionOf(last));
            last = NONE;
            expected = modCount;
        }

        private void unchanged() {
            if (modCount != expected) {
                throw new ConcurrentModificationException();
            }
        }
    }

    /** A view of one slot, made on demand; it is never the stored form of a mapping. */
    private final class SlotEntry implements Map.Entry<K, V> {
        private final int slot;

        SlotEntry(int slot) { this.slot = slot; }

        @Override public K getKey() { return key(slot); }
        @Override public V getValue() { return value(slot); }

        @Override public V setValue(V value) {
            V old = value(slot);
            valueLane[slot] = value;
            return old;
        }

        @Override public boolean equals(Object other) {
            return other instanceof Map.Entry<?, ?> entry && Objects.equals(getKey(), entry.getKey())
                    && Objects.equals(getValue(), entry.getValue());
        }

        @Override public int hashCode() { return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue()); }
        @Override public String toString() { return getKey() + "=" + getValue(); }
    }
}
