/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import jdk.m3.collections.LongLongConsumer;

/**
 * Bounded primitive LRU map over the existing parent-owned ordered long-key
 * and long-value lanes, integer slot offsets, recycled slots and compact
 * open-addressed index. No boxed key/value or retained per-entry node.
 *
 * <p>Reads via {@link #getOrDefault} promote a key to most recently used;
 * {@link #peekOrDefault} and {@link #containsKey} preserve order. A new key at
 * full capacity evicts the least recently used key. This is not thread-safe.
 */
public final class LruLongLongMap {
    static final long STATE_OCCUPIED = 1L;
    static final long STATE_DELETED = 2L;
    private static final int DEFAULT_MAX_SIZE = 100;

    private final int maxSize;
    private final PackedLongLongOrderedMap entries;

    public LruLongLongMap() { this(DEFAULT_MAX_SIZE); }

    /** An additive explicit bound; the original no-arg constructor remains. */
    public LruLongLongMap(int maxSize) {
        if (maxSize < 1) throw new IllegalArgumentException("maxSize must be positive");
        this.maxSize = maxSize;
        this.entries = new PackedLongLongOrderedMap(Math.min(maxSize, 8), true);
    }

    public static String scaffoldDescriptor() {
        return "M3LruLongLongMap|LRU_LONG_MAP|LONG|OCCUPIED,DELETED|long[] keyLane;long[] valueLane;int[] prevLane;int[] nextLane";
    }

    public int maxSize() { return maxSize; }
    public boolean isFull() { return entries.size() == maxSize; }
    public long getOrDefault(long key, long defaultValue) {
        return entries.getOrDefault(key, defaultValue);
    }
    public long peekOrDefault(long key, long defaultValue) {
        return entries.peekOrDefault(key, defaultValue);
    }

    /** Return the previous value, or zero if the key was not present. */
    public long put(long key, long value) {
        boolean present = entries.containsKey(key);
        long old = present ? entries.peekOrDefault(key, 0L) : 0L;
        if (!present && entries.size() == maxSize) entries.removeFirstKey();
        entries.put(key, value);
        return old;
    }

    /** Return the removed value, or zero if absent. */
    public long remove(long key) {
        long old = entries.peekOrDefault(key, 0L);
        entries.remove(key);
        return old;
    }

    public boolean containsKey(long key) { return entries.containsKey(key); }
    public long firstKey() { return entries.firstKey(); }
    public long lastKey() { return entries.lastKey(); }
    public void forEach(LongLongConsumer action) { entries.forEach(action); }
    public int size() { return entries.size(); }
    public void clear() { entries.clear(); }
}
