/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Bounded least-recently-used {@code long -> long} map over open-addressed slot lanes and a recency link lane.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET, leaf LS4):
 * <ul>
 *   <li>COMMONS {@code LRUMap}: fixed {@code maxSize} (>= 1); {@code get} moves the key to the most-recently-used end,
 *       {@code get(key, false)} (here {@link #peekOrDefault}) and {@code containsKey} do not; {@code put} of a present
 *       key replaces the value and moves it to MRU; {@code put} of a new key into a full map evicts the
 *       least-recently-used entry first; {@code firstKey} = LRU end, {@code lastKey} = MRU end (both
 *       {@link NoSuchElementException} when empty); iteration runs LRU to MRU.</li>
 * </ul>
 * Qualified only against Commons {@code LRUMap} itself; no equivalence with Guava caches, Spring
 * {@code ConcurrentLruCache} or {@code LinkedHashMap} is inferred. Primitive returns use the configured
 * {@code missingValue} where Commons returns {@code null}.
 *
 * <p>DOOP mapping: lane kind LONG; slot lanes {@code keyLane}/{@code valueLane} (linear probing, power-of-two table of at
 * least {@code 2 * maxSize} slots), recency link lanes {@code prevLane}/{@code nextLane} with {@code head} (LRU) and
 * {@code tail} (MRU) slot offsets, and per-slot state bits (2 bits per slot, 32 slots per word) in
 * {@code stateMaskWords}. Deletion uses backward-shift (no tombstones): moved slots re-point their neighbours' links.
 * Stored nodes: NONE. Per-element allocation: 0 (all lanes are allocated once by the constructor; get, put, evict,
 * remove and forEach allocate nothing). Donor: first-party Synexia {@code PackedLongLongLruCache} (blob 3f4b2be64db5),
 * adapted. Not a {@code java.util} replacement.
 */
public final class M3LruLongLongMap {
    /** Slot state bit {@code OCCUPIED}. */
    static final long STATE_OCCUPIED = 1L << 0;
    /** Slot state bit {@code DELETED}: reserved; backward-shift deletion never leaves tombstones. */
    static final long STATE_DELETED = 1L << 1;
    private static final int NIL = -1;
    private static final int DEFAULT_MAX_SIZE = 100;

    /** Storage lane {@code keyLane}: key per slot. */
    private final long[] keyLane;
    /** Storage lane {@code valueLane}: value per slot. */
    private final long[] valueLane;
    /** Recency lane {@code prevLane}: slot of the next-older entry, or -1. */
    private final int[] prevLane;
    /** Recency lane {@code nextLane}: slot of the next-newer entry, or -1. */
    private final int[] nextLane;
    /** Per-slot state words: 2 bits per slot (OCCUPIED, DELETED). */
    private final long[] stateMaskWords;
    private final int mask;
    private final int maxSize;
    private final long missingValue;
    /** Live slot count. */
    private int size;
    private int head = NIL;
    private int tail = NIL;
    private int modCount;

    /** Commons LRUMap default: {@code maxSize} 100, missing value 0. */
    public M3LruLongLongMap() {
        this(DEFAULT_MAX_SIZE, 0L);
    }

    /** Bounded map with the given {@code maxSize} (>= 1), missing value 0. */
    public M3LruLongLongMap(int maxSize) {
        this(maxSize, 0L);
    }

    /** Bounded map with the given {@code maxSize} (>= 1) and the value returned for absent keys. */
    public M3LruLongLongMap(int maxSize, long missingValue) {
        if (maxSize < 1 || maxSize > (1 << 29)) throw new IllegalArgumentException("maxSize must be in [1, 2^29]: " + maxSize);
        int capacity = Integer.highestOneBit(Math.max(4, maxSize * 2 - 1)) << 1;
        this.keyLane = new long[capacity];
        this.valueLane = new long[capacity];
        this.prevLane = new int[capacity];
        this.nextLane = new int[capacity];
        this.stateMaskWords = new long[(capacity + 31) >>> 5];
        this.mask = capacity - 1;
        this.maxSize = maxSize;
        this.missingValue = missingValue;
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3LruLongLongMap|LRU_LONG_MAP|LONG|OCCUPIED,DELETED|long[] keyLane;long[] valueLane;int[] prevLane;int[] nextLane|RECEIVED";
    }

    /** LRUMap.maxSize. */
    public int maxSize() {
        return maxSize;
    }

    /** LRUMap.isFull. */
    public boolean isFull() {
        return size >= maxSize;
    }

    /** LRUMap.get: value or {@code defaultValue}; a hit moves the key to the MRU end. */
    public long getOrDefault(long key, long defaultValue) {
        int slot = find(key);
        if (slot < 0) return defaultValue;
        moveToTail(slot);
        return valueLane[slot];
    }

    /** LRUMap.get(key, false): value or {@code defaultValue}, recency unchanged. */
    public long peekOrDefault(long key, long defaultValue) {
        int slot = find(key);
        return slot < 0 ? defaultValue : valueLane[slot];
    }

    /** LRUMap.put: previous value (or the missing value); evicts the LRU entry when full and the key is new. */
    public long put(long key, long value) {
        int slot = find(key);
        if (slot >= 0) {
            long previous = valueLane[slot];
            valueLane[slot] = value;
            moveToTail(slot);
            return previous;
        }
        if (size >= maxSize) deleteSlot(head);
        slot = home(key);
        while (occupied(slot)) slot = (slot + 1) & mask;
        keyLane[slot] = key;
        valueLane[slot] = value;
        setOccupied(slot, true);
        linkLast(slot);
        size++;
        modCount++;
        return missingValue;
    }

    /** LRUMap.remove: previous value (or the missing value). */
    public long remove(long key) {
        int slot = find(key);
        if (slot < 0) return missingValue;
        long previous = valueLane[slot];
        deleteSlot(slot);
        return previous;
    }

    /** Map.containsKey; recency unchanged. */
    public boolean containsKey(long key) {
        return find(key) >= 0;
    }

    /** OrderedMap.firstKey: least-recently-used key. */
    public long firstKey() {
        if (size == 0) throw new NoSuchElementException();
        return keyLane[head];
    }

    /** OrderedMap.lastKey: most-recently-used key. */
    public long lastKey() {
        if (size == 0) throw new NoSuchElementException();
        return keyLane[tail];
    }

    /** Visits entries from LRU to MRU; fails fast if the map is structurally modified during the walk. */
    public void forEach(M3LongLongConsumer action) {
        Objects.requireNonNull(action, "action");
        int expected = modCount;
        for (int slot = head; slot != NIL; slot = nextLane[slot]) {
            action.accept(keyLane[slot], valueLane[slot]);
            if (modCount != expected) throw new ConcurrentModificationException();
        }
    }

    /** Map.size. */
    public int size() {
        return size;
    }

    /** Map.clear. */
    public void clear() {
        Arrays.fill(stateMaskWords, 0L);
        size = 0;
        head = NIL;
        tail = NIL;
        modCount++;
    }

    private int home(long key) {
        long mixed = key * 0x9E3779B97F4A7C15L;
        return (int) (mixed ^ (mixed >>> 32)) & mask;
    }

    private int find(long key) {
        for (int slot = home(key); occupied(slot); slot = (slot + 1) & mask) {
            if (keyLane[slot] == key) return slot;
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

    /** Unlinks the slot and closes the probe gap by backward shift, re-pointing the links of moved slots. */
    private void deleteSlot(int slot) {
        unlink(slot);
        int gap = slot;
        for (int probe = (gap + 1) & mask; occupied(probe); probe = (probe + 1) & mask) {
            int origin = home(keyLane[probe]);
            boolean stays = gap <= probe ? gap < origin && origin <= probe : gap < origin || origin <= probe;
            if (stays) continue;
            relocate(probe, gap);
            gap = probe;
        }
        setOccupied(gap, false);
        size--;
        modCount++;
    }

    private void relocate(int from, int to) {
        keyLane[to] = keyLane[from];
        valueLane[to] = valueLane[from];
        int before = prevLane[from], after = nextLane[from];
        prevLane[to] = before;
        nextLane[to] = after;
        if (before != NIL) nextLane[before] = to;
        else head = to;
        if (after != NIL) prevLane[after] = to;
        else tail = to;
    }
}
