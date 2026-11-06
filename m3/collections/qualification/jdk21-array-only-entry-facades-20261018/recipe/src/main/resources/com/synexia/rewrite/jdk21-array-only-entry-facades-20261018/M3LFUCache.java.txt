/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Generated through the canonical Synexia array-only facade recipe.
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongFunction;

/**
 * Bounded, single-owner LFU cache for optional API-created facades.
 *
 * <p>Its only resident storage is parallel primitive arrays plus one {@code Object[]}
 * for the cached facade references. Long cache handles may include the upstream
 * owner's generation, so recycling a physical slot does not imply reusing a stale
 * facade. Open-addressing is through an {@code int[]} of slot+1 buckets with
 * independent live byte masks. There are no retained Entry/Node/Cell objects for
 * cache bookkeeping. No existing collection uses this as authoritative storage.
 *
 * <p>Reads are expected O(1), LFU/LRU-tie eviction is O(cache capacity), insertion
 * amortized O(1), and optional compaction O(capacity). This is intentionally
 * bounded for small API facade caches rather than a generic data-store replacement.
 * Cache loss never loses the owning collection's canonical keys or values.
 * Not thread-safe: the collection owner supplies synchronization where needed.
 */
public final class M3LFUCache<T> {
    private static final byte LIVE = 1;
    private static final int MAX_ARRAY = Integer.MAX_VALUE - 8;

    private long[] handles = new long[0];
    private Object[] facades = new Object[0];
    private int[] counts = new int[0];
    private long[] lastUsed = new long[0];
    private byte[] masks = new byte[0];
    private int[] bucketOfSlot = new int[0];
    private int[] freeNext = new int[0];
    /** 0 = never used, -1 = tombstone, otherwise physical cache-slot + 1. */
    private int[] buckets = new int[0];
    private int maxEntries;
    private int used;
    private int free = -1;
    private int size;
    private int tombstones;
    private long clock;
    private long hits;
    private long misses;
    private long evictions;
    private boolean inFactory;

    public M3LFUCache(int maxEntries) {
        requireCapacity(maxEntries);
        this.maxEntries = maxEntries;
    }

    public int size() { return size; }
    public int maximumEntries() { return maxEntries; }
    public long hits() { return hits; }
    public long misses() { return misses; }
    public long evictions() { return evictions; }
    public int retainedArraySlots() { return handles.length; }

    /** Returns the cached reference without changing its use count. */
    public T peek(long handle) {
        assertOutsideFactory();
        int slot = findSlot(handle);
        return slot < 0 ? null : facadeAt(slot);
    }

    /** An LFU touch increments the saturating count and recency clock. */
    public T get(long handle) {
        assertOutsideFactory();
        int slot = findSlot(handle);
        if (slot < 0) { misses++; return null; }
        hits++;
        touch(slot);
        return facadeAt(slot);
    }

    public boolean contains(long handle) {
        assertOutsideFactory();
        return findSlot(handle) >= 0;
    }

    /**
     * Create exactly one facade on a miss. The callback cannot reenter this cache,
     * and an exception leaves it unchanged. Capacity zero creates but retains none.
     */
    public T getOrCreate(long handle, LongFunction<? extends T> factory) {
        Objects.requireNonNull(factory, "factory");
        assertOutsideFactory();
        int slot = findSlot(handle);
        if (slot >= 0) { hits++; touch(slot); return facadeAt(slot); }
        misses++;
        T result;
        inFactory = true;
        try { result = Objects.requireNonNull(factory.apply(handle), "facade"); }
        finally { inFactory = false; }
        put(handle, result);
        return result;
    }

    /** Insert or replace one cached facade; no per-handle Entry object is created. */
    public T put(long handle, T facade) {
        Objects.requireNonNull(facade, "facade");
        assertOutsideFactory();
        if (maxEntries == 0) return null;
        int found = findSlot(handle);
        if (found >= 0) {
            T previous = facadeAt(found);
            facades[found] = facade;
            touch(found);
            return previous;
        }
        if (size == maxEntries) evictOne();
        if (free < 0) ensureSlots(used + 1);
        ensureBuckets(size + 1);
        int slot;
        if (free >= 0) { slot = free; free = freeNext[slot]; }
        else { slot = used++; }
        handles[slot] = handle;
        facades[slot] = facade;
        masks[slot] = LIVE;
        counts[slot] = 0;
        insertBucket(slot);
        size++;
        touch(slot);
        return null;
    }

    /** Invalidate on upstream content/generation changes; freed refs are cleared. */
    public T invalidate(long handle) {
        assertOutsideFactory();
        int slot = findSlot(handle);
        return slot < 0 ? null : delete(slot);
    }

    /** Evicts all derived facades. Canonical collection payload is never touched. */
    public void clear() {
        assertOutsideFactory();
        Arrays.fill(facades, null);
        Arrays.fill(handles, 0L);
        Arrays.fill(masks, (byte) 0);
        Arrays.fill(counts, 0);
        Arrays.fill(lastUsed, 0L);
        Arrays.fill(bucketOfSlot, 0);
        Arrays.fill(freeNext, 0);
        Arrays.fill(buckets, 0);
        used = size = tombstones = 0;
        free = -1;
        clock = 0;
    }

    /** Downsize an optional cache under memory pressure; evictions are LFU then LRU. */
    public void setMaximumEntries(int newMaximum) {
        assertOutsideFactory();
        requireCapacity(newMaximum);
        maxEntries = newMaximum;
        while (size > maxEntries) evictOne();
    }

    /** Release spare arrays and tombstones, preserving live frequency/recency facts. */
    public void compactStorage() {
        assertOutsideFactory();
        int n = size;
        long[] newHandles = new long[n];
        Object[] newFacades = new Object[n];
        int[] newCounts = new int[n];
        long[] newLastUsed = new long[n];
        byte[] newMasks = new byte[n];
        int[] newBucketSlot = new int[n];
        int[] newFreeNext = new int[n];
        int at = 0;
        for (int slot = 0; slot < used; slot++) {
            if (masks[slot] == LIVE) {
                newHandles[at] = handles[slot];
                newFacades[at] = facades[slot];
                newCounts[at] = counts[slot];
                newLastUsed[at] = lastUsed[slot];
                newMasks[at] = LIVE;
                at++;
            }
        }
        if (at != n) throw new IllegalStateException("cache live-slot count");
        handles = newHandles; facades = newFacades; counts = newCounts;
        lastUsed = newLastUsed; masks = newMasks;
        bucketOfSlot = newBucketSlot; freeNext = newFreeNext;
        used = n; free = -1;
        rebuildBuckets(tableCapacity(n));
    }

    /** Expensive diagnostic: no stale bucket, duplicate key or retained cleared slot. */
    public void verifyInvariants() {
        assertOutsideFactory();
        int live = 0;
        for (int i = 0; i < used; i++) {
            if (masks[i] == LIVE) {
                live++;
                if (facades[i] == null || counts[i] <= 0 || findSlot(handles[i]) != i)
                    throw new AssertionError("live LFU array slot");
            } else if (facades[i] != null) throw new AssertionError("cleared LFU reference retained");
        }
        if (live != size || size > maxEntries) throw new AssertionError("LFU cardinality");
        int bucketsLive = 0;
        for (int value : buckets) if (value > 0) bucketsLive++;
        if (bucketsLive != size) throw new AssertionError("LFU bucket cardinality");
    }

    @SuppressWarnings("unchecked")
    private T facadeAt(int slot) { return (T) facades[slot]; }

    private void evictOne() {
        int victim = -1;
        for (int i = 0; i < used; i++) {
            if (masks[i] != LIVE) continue;
            if (victim < 0 || counts[i] < counts[victim] ||
                    counts[i] == counts[victim] && lastUsed[i] < lastUsed[victim]) {
                victim = i;
            }
        }
        if (victim < 0) throw new IllegalStateException("LFU eviction of empty cache");
        delete(victim);
        evictions++;
    }

    private T delete(int slot) {
        T previous = facadeAt(slot);
        buckets[bucketOfSlot[slot]] = -1;
        tombstones++;
        handles[slot] = 0L;
        facades[slot] = null;
        counts[slot] = 0;
        lastUsed[slot] = 0;
        masks[slot] = 0;
        bucketOfSlot[slot] = 0;
        freeNext[slot] = free;
        free = slot;
        size--;
        if (size == 0) {
            Arrays.fill(buckets, 0);
            tombstones = 0;
        }
        return previous;
    }

    private void touch(int slot) {
        if (counts[slot] < Integer.MAX_VALUE) counts[slot]++;
        if (clock == Long.MAX_VALUE) renumberClock();
        lastUsed[slot] = ++clock;
    }

    private void renumberClock() {
        long[] ordered = new long[size];
        int at = 0;
        for (int i = 0; i < used; i++) if (masks[i] == LIVE) ordered[at++] = lastUsed[i];
        Arrays.sort(ordered);
        for (int i = 0; i < used; i++) {
            if (masks[i] == LIVE) lastUsed[i] = Arrays.binarySearch(ordered, lastUsed[i]) + 1L;
        }
        clock = size;
    }

    private int findSlot(long handle) {
        if (buckets.length == 0) return -1;
        int mask = buckets.length - 1;
        int at = mix(handle) & mask;
        for (int attempts = 0; attempts < buckets.length; attempts++) {
            int bucket = buckets[at];
            if (bucket == 0) return -1;
            if (bucket > 0) {
                int slot = bucket - 1;
                if (masks[slot] == LIVE && handles[slot] == handle) return slot;
            }
            at = (at + 1) & mask;
        }
        return -1;
    }

    private void insertBucket(int slot) {
        int mask = buckets.length - 1;
        int at = mix(handles[slot]) & mask;
        int tombstone = -1;
        for (int tries = 0; tries < buckets.length; tries++) {
            int bucket = buckets[at];
            if (bucket == 0) {
                int found = tombstone >= 0 ? tombstone : at;
                if (tombstone >= 0) tombstones--;
                buckets[found] = slot + 1;
                bucketOfSlot[slot] = found;
                return;
            }
            if (bucket == -1 && tombstone < 0) tombstone = at;
            at = (at + 1) & mask;
        }
        if (tombstone >= 0) {
            buckets[tombstone] = slot + 1;
            bucketOfSlot[slot] = tombstone;
            tombstones--;
            return;
        }
        throw new IllegalStateException("LFU hash index has no insertion position");
    }

    private void ensureBuckets(int wantedSize) {
        if (buckets.length == 0 || (long) (wantedSize + tombstones) * 2 >= buckets.length) {
            rebuildBuckets(tableCapacity(wantedSize));
        }
    }

    private void rebuildBuckets(int capacity) {
        buckets = new int[capacity];
        tombstones = 0;
        for (int i = 0; i < used; i++) {
            if (masks[i] == LIVE) insertBucket(i);
        }
    }

    private static int tableCapacity(int n) {
        if (n == 0) return 0;
        long desired = Math.max(8L, ((long) n + 1) * 4);
        if (desired > 1L << 30) throw new OutOfMemoryError("LFU index too large");
        int table = 8;
        while (table < desired) table <<= 1;
        return table;
    }

    private void ensureSlots(int need) {
        if (need <= handles.length) return;
        long grown = Math.min(maxEntries, Math.max((long) need,
                Math.max(8L, handles.length + (long) (handles.length >>> 1) + 1)));
        int n = (int) grown;
        handles = Arrays.copyOf(handles, n);
        facades = Arrays.copyOf(facades, n);
        counts = Arrays.copyOf(counts, n);
        lastUsed = Arrays.copyOf(lastUsed, n);
        masks = Arrays.copyOf(masks, n);
        bucketOfSlot = Arrays.copyOf(bucketOfSlot, n);
        freeNext = Arrays.copyOf(freeNext, n);
    }

    private static int mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (int) value;
    }

    private static void requireCapacity(int n) {
        if (n < 0 || n > MAX_ARRAY) throw new IllegalArgumentException("invalid LFU capacity");
    }

    private void assertOutsideFactory() {
        if (inFactory) throw new IllegalStateException("LFU factory must not reenter the cache");
    }
}
