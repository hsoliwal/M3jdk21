/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Bounded least-recently-used map over open-addressed reference slot lanes, a cached-hash lane and a recency link lane.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET, leaf LS4):
 * <ul>
 *   <li>COMMONS {@code LRUMap}: fixed {@code maxSize} (>= 1); {@code get} moves the key to the MRU end,
 *       {@code get(key, false)} and {@code containsKey} do not; {@code put} of a present key replaces the value and moves
 *       it to MRU; {@code put} of a new key into a full map evicts the LRU entry first; {@code firstKey}/{@code lastKey}
 *       are the LRU/MRU ends ({@link NoSuchElementException} when empty); null keys and values are allowed; iteration
 *       runs LRU to MRU.</li>
 * </ul>
 * Qualified only against Commons {@code LRUMap}; no equivalence with other caches is inferred.
 *
 * <p>DOOP mapping: lane kind OBJECT; slot lanes {@code keyLane}/{@code valueLane}, {@code hashIndexLane} (cached spread
 * hash per slot), recency link lanes {@code prevLane}/{@code nextLane} with {@code head} (LRU) / {@code tail} (MRU)
 * offsets, per-slot state bits (2 bits per slot) in {@code stateMaskWords}; linear probing with backward-shift
 * deletion. Stored nodes: NONE. Per-element allocation: 0 for get/put/evict/remove (lanes are allocated once). API
 * entry objects: TRANSIENT_FACADE only — {@link #asMap()} iteration hands out slot-bound {@code Map.Entry} views, never
 * stored. Donor: first-party Synexia {@code PackedLongLongLruCache} (blob 3f4b2be64db5), adapted to references.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class M3LruMap<K, V> {
    /** Slot state bit {@code OCCUPIED}. */
    static final long STATE_OCCUPIED = 1L << 0;
    /** Slot state bit {@code DELETED}: reserved; backward-shift deletion never leaves tombstones. */
    static final long STATE_DELETED = 1L << 1;
    private static final int NIL = -1;
    private static final Object NULL_KEY = new Object();

    /** Storage lane {@code keyLane}: key per slot (null key stored as a private sentinel). */
    private final Object[] keyLane;
    /** Storage lane {@code valueLane}: value per slot. */
    private final Object[] valueLane;
    /** Recency lane {@code prevLane}. */
    private final int[] prevLane;
    /** Recency lane {@code nextLane}. */
    private final int[] nextLane;
    /** Cached spread hash per slot. */
    private final int[] hashIndexLane;
    /** Per-slot state words: 2 bits per slot (OCCUPIED, DELETED). */
    private final long[] stateMaskWords;
    private final int mask;
    private final int maxSize;
    /** Live slot count. */
    private int size;
    private int head = NIL;
    private int tail = NIL;
    private int modCount;

    /** Commons LRUMap default: {@code maxSize} 100. */
    public M3LruMap() {
        this(100);
    }

    /** Bounded map with the given {@code maxSize} (>= 1). */
    public M3LruMap(int maxSize) {
        if (maxSize < 1 || maxSize > (1 << 29)) throw new IllegalArgumentException("maxSize must be in [1, 2^29]: " + maxSize);
        int capacity = Integer.highestOneBit(Math.max(4, maxSize * 2 - 1)) << 1;
        this.keyLane = new Object[capacity];
        this.valueLane = new Object[capacity];
        this.prevLane = new int[capacity];
        this.nextLane = new int[capacity];
        this.hashIndexLane = new int[capacity];
        this.stateMaskWords = new long[(capacity + 31) >>> 5];
        this.mask = capacity - 1;
        this.maxSize = maxSize;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3LruMap|LRU_MAP|OBJECT|OCCUPIED,DELETED|Object[] keyLane;Object[] valueLane;int[] prevLane;int[] nextLane;int[] hashIndexLane|RECEIVED";
    }

    /** LRUMap.maxSize. */
    public int maxSize() {
        return maxSize;
    }

    /** LRUMap.isFull. */
    public boolean isFull() {
        return size >= maxSize;
    }

    /** LRUMap.get: a hit moves the key to the MRU end. */
    public V get(Object key) {
        return get(key, true);
    }

    /** LRUMap.get(key, updateToMRU). */
    public V get(Object key, boolean updateToMRU) {
        int slot = find(key);
        if (slot < 0) return null;
        if (updateToMRU) moveToTail(slot);
        return value(slot);
    }

    /** LRUMap.put: previous value or null; evicts the LRU entry when full and the key is new. */
    public V put(K key, V value) {
        int slot = find(key);
        if (slot >= 0) {
            V previous = value(slot);
            valueLane[slot] = value;
            moveToTail(slot);
            return previous;
        }
        if (size >= maxSize) deleteSlot(head);
        int hash = spread(key);
        slot = hash & mask;
        while (occupied(slot)) slot = (slot + 1) & mask;
        keyLane[slot] = key == null ? NULL_KEY : key;
        valueLane[slot] = value;
        hashIndexLane[slot] = hash;
        setOccupied(slot, true);
        linkLast(slot);
        size++;
        modCount++;
        return null;
    }

    /** LRUMap.remove: previous value or null. */
    public V remove(Object key) {
        int slot = find(key);
        if (slot < 0) return null;
        V previous = value(slot);
        deleteSlot(slot);
        return previous;
    }

    /** Map.containsKey; recency unchanged. */
    public boolean containsKey(Object key) {
        return find(key) >= 0;
    }

    /** OrderedMap.firstKey: least-recently-used key. */
    public K firstKey() {
        if (size == 0) throw new NoSuchElementException();
        return key(head);
    }

    /** OrderedMap.lastKey: most-recently-used key. */
    public K lastKey() {
        if (size == 0) throw new NoSuchElementException();
        return key(tail);
    }

    /** Live {@code java.util.Map} facade (LRU-to-MRU iteration; Map.get moves to MRU as LRUMap.get). */
    public Map<K, V> asMap() {
        return new MapView();
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
        Arrays.fill(keyLane, null);
        Arrays.fill(valueLane, null);
        Arrays.fill(stateMaskWords, 0L);
        size = 0;
        head = NIL;
        tail = NIL;
        modCount++;
    }

    @SuppressWarnings("unchecked")
    private K key(int slot) {
        Object key = keyLane[slot];
        return key == NULL_KEY ? null : (K) key;
    }

    @SuppressWarnings("unchecked")
    private V value(int slot) {
        return (V) valueLane[slot];
    }

    private int spread(Object key) {
        int h = key == null ? 0 : key.hashCode();
        h *= 0x9E3779B9;
        return h ^ (h >>> 16);
    }

    private int find(Object key) {
        int hash = spread(key);
        Object probe = key == null ? NULL_KEY : key;
        for (int slot = hash & mask; occupied(slot); slot = (slot + 1) & mask) {
            if (hashIndexLane[slot] == hash && (keyLane[slot] == probe || probe != NULL_KEY && probe.equals(keyLane[slot]))) return slot;
        }
        return -1;
    }

    private boolean occupied(int slot) {
        return (stateMaskWords[slot >>> 5] & (STATE_OCCUPIED << ((slot & 31) << 1))) != 0;
    }

    private void setOccupied(int slot, boolean on) {
        long bit = STATE_OCCUPIED << ((slot & 31) << 1);
        if (on) stateMaskWords[slot >>> 5] |= bit;
        else stateMaskWords[slot >>> 5] &= ~bit;
    }

    private void linkLast(int slot) {
        prevLane[slot] = tail;
        nextLane[slot] = NIL;
        if (tail != NIL) nextLane[tail] = slot;
        else head = slot;
        tail = slot;
    }

    private void unlink(int slot) {
        int before = prevLane[slot], after = nextLane[slot];
        if (before != NIL) nextLane[before] = after;
        else head = after;
        if (after != NIL) prevLane[after] = before;
        else tail = before;
    }

    private void moveToTail(int slot) {
        if (slot == tail) return;
        unlink(slot);
        linkLast(slot);
        modCount++;
    }

    private void deleteSlot(int slot) {
        unlink(slot);
        int gap = slot;
        for (int probe = (gap + 1) & mask; occupied(probe); probe = (probe + 1) & mask) {
            int origin = hashIndexLane[probe] & mask;
            boolean stays = gap <= probe ? gap < origin && origin <= probe : gap < origin || origin <= probe;
            if (stays) continue;
            relocate(probe, gap);
            gap = probe;
        }
        keyLane[gap] = null;
        valueLane[gap] = null;
        setOccupied(gap, false);
        size--;
        modCount++;
    }

    private void relocate(int from, int to) {
        keyLane[to] = keyLane[from];
        valueLane[to] = valueLane[from];
        hashIndexLane[to] = hashIndexLane[from];
        int before = prevLane[from], after = nextLane[from];
        prevLane[to] = before;
        nextLane[to] = after;
        if (before != NIL) nextLane[before] = to;
        else head = to;
        if (after != NIL) prevLane[after] = to;
        else tail = to;
    }

    /** Transient facade over the owner; holds no entries. */
    private final class MapView extends AbstractMap<K, V> {
        @Override public int size() { return size; }
        @Override public V get(Object key) { return M3LruMap.this.get(key); }
        @Override public boolean containsKey(Object key) { return M3LruMap.this.containsKey(key); }
        @Override public V put(K key, V value) { return M3LruMap.this.put(key, value); }
        @Override public V remove(Object key) { return M3LruMap.this.remove(key); }
        @Override public void clear() { M3LruMap.this.clear(); }

        @Override public Set<Map.Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return size; }

                @Override public Iterator<Map.Entry<K, V>> iterator() {
                    return new Iterator<>() {
                        private int next = head;
                        private int last = NIL;
                        private int expected = modCount;

                        @Override public boolean hasNext() { return next != NIL; }

                        @Override public Map.Entry<K, V> next() {
                            if (modCount != expected) throw new ConcurrentModificationException();
                            if (next == NIL) throw new NoSuchElementException();
                            last = next;
                            next = nextLane[next];
                            return new SlotEntry(last, expected);
                        }

                        @Override public void remove() {
                            if (last == NIL) throw new IllegalStateException();
                            if (modCount != expected) throw new ConcurrentModificationException();
                            K key = key(last);
                            int following = next == NIL ? NIL : next;
                            Object followingKey = following == NIL ? null : keyLane[following];
                            M3LruMap.this.remove(key);
                            next = following == NIL ? NIL : find(followingKey == NULL_KEY ? null : followingKey);
                            last = NIL;
                            expected = modCount;
                        }
                    };
                }
            };
        }
    }

    /** Slot-bound transient entry facade; valid until the next structural modification. */
    private final class SlotEntry implements Map.Entry<K, V> {
        private final int slot;
        private final int stamp;

        SlotEntry(int slot, int stamp) {
            this.slot = slot;
            this.stamp = stamp;
        }

        private void check() {
            if (modCount != stamp) throw new ConcurrentModificationException();
        }

        @Override public K getKey() { check(); return key(slot); }
        @Override public V getValue() { check(); return value(slot); }

        @Override public V setValue(V value) {
            check();
            V previous = value(slot);
            valueLane[slot] = value;
            return previous;
        }

        @Override public boolean equals(Object other) {
            return other instanceof Map.Entry<?, ?> e && Objects.equals(getKey(), e.getKey()) && Objects.equals(getValue(), e.getValue());
        }

        @Override public int hashCode() {
            return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
        }

        @Override public String toString() {
            return getKey() + "=" + getValue();
        }
    }
}
