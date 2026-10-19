/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * Bounded primitive long-to-long LFU cache with O(1) get, put and eviction on parallel lanes and
 * no per-entry node (LeetCode 460 mechanics; catalogue row {@code lfu-cache}, leaf
 * GAP-LC-catalogue-gaps, idea-only: nothing is copied from any editorial).
 *
 * <p>Table lanes, indexed by slot of an open-addressed linear-probing table sized once for the
 * capacity: {@code keys}, {@code values}, {@code frequencies}, the per-bucket order links
 * {@code previousInBucket}/{@code nextInBucket} and {@code bucketOf}. Per-slot state is two
 * bitmasks in {@code java.util.BitSet} layout: {@code occupied} (bit set = live mapping) and
 * {@code tombstones} (bit set = vacated slot that keeps probe runs intact). Frequency buckets live
 * in an arena of {@code capacity + 1} int lanes ({@code bucketFrequency}, {@code bucketPrevious},
 * {@code bucketNext}, {@code bucketHead}, {@code bucketTail}) threaded as a list ascending by
 * frequency, so the minimum frequency is {@code firstBucket} and the eviction victim is that
 * bucket's head (its least recently used slot); a touched slot moves to the bucket of
 * {@code frequency + 1}, created in place when absent, and a drained bucket returns to a free
 * list. Every operation is O(1); per-element allocation: 0 (all lanes are allocated by the
 * constructor; a tombstone sweep re-allocates lanes of the same size, never per element). Every
 * {@code long} is a legal key and value. {@code get} counts as a structural modification for the
 * fail-fast iterator, as {@code java.util.LinkedHashMap} access order does. Frequencies saturate
 * at {@code Integer.MAX_VALUE}. Proving-ground backend; not a {@code java.util.Map}.</p>
 */
public final class M3LongLfuCache {
    private static final int MIN_TABLE = 8;
    private static final int MAX_CAPACITY = 1 << 29;
    private static final int NONE = -1;

    private final int capacity;
    private final int sweepThreshold;
    private long[] keys;
    private long[] values;
    private int[] frequencies;
    private long[] occupied;
    private long[] tombstones;
    private int[] previousInBucket;
    private int[] nextInBucket;
    private int[] bucketOf;
    private final int[] bucketFrequency;
    private final int[] bucketPrevious;
    private final int[] bucketNext;
    private final int[] bucketHead;
    private final int[] bucketTail;
    private int firstBucket = NONE;
    private int freeBucket = NONE;
    private int bucketsAllocated;
    private int size;
    private int occupiedCount;
    private long evictions;
    private long lastEvictedKey;
    private long lastEvictedValue;
    private int modCount;

    public M3LongLfuCache(int capacity) {
        if (capacity < 1 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("capacity must be in 1.." + MAX_CAPACITY);
        }
        this.capacity = capacity;
        int table = tableSizeFor(Math.max(MIN_TABLE, (int) (((long) capacity * 3 + 1) / 2)));
        sweepThreshold = table - (table >>> 2);
        keys = new long[table];
        values = new long[table];
        frequencies = new int[table];
        occupied = new long[words(table)];
        tombstones = new long[words(table)];
        previousInBucket = new int[table];
        nextInBucket = new int[table];
        bucketOf = new int[table];
        bucketFrequency = new int[capacity + 1];
        bucketPrevious = new int[capacity + 1];
        bucketNext = new int[capacity + 1];
        bucketHead = new int[capacity + 1];
        bucketTail = new int[capacity + 1];
    }

    /**
     * Inserts or updates; returns {@code true} when a new mapping was created. A present key keeps
     * its frequency count plus one. When the cache is full the least frequently used mapping, ties
     * broken by least recent use, is evicted first (see {@link #lastEvictedKey()}).
     */
    public boolean put(long key, long value) {
        int slot = findSlot(key);
        if (slot != NONE) {
            values[slot] = value;
            touch(slot);
            return false;
        }
        if (size == capacity) {
            evictOne();
        }
        if (occupiedCount + 1 > sweepThreshold) {
            sweep();
        }
        slot = claimSlot(key);
        values[slot] = value;
        frequencies[slot] = 1;
        attach(slot, bucketAfter(NONE, 1));
        size++;
        modCount++;
        return true;
    }

    /** Value for {@code key}, counting one use, or {@code defaultValue} when absent. */
    public long getOrDefault(long key, long defaultValue) {
        int slot = findSlot(key);
        if (slot == NONE) {
            return defaultValue;
        }
        touch(slot);
        return values[slot];
    }

    /** Value for {@code key} without counting a use, or {@code defaultValue} when absent. */
    public long peekOrDefault(long key, long defaultValue) {
        int slot = findSlot(key);
        return slot == NONE ? defaultValue : values[slot];
    }

    /** Membership without counting a use. */
    public boolean containsKey(long key) {
        return findSlot(key) >= 0;
    }

    /** Use count of {@code key}, or {@code 0} when absent (a present key counts at least {@code 1}). */
    public int frequencyOf(long key) {
        int slot = findSlot(key);
        return slot == NONE ? 0 : frequencies[slot];
    }

    public boolean remove(long key) {
        int slot = findSlot(key);
        if (slot == NONE) {
            return false;
        }
        removeSlot(slot);
        return true;
    }

    /** Smallest use count present, or {@code 0} when empty. */
    public int minFrequency() {
        return size == 0 ? 0 : bucketFrequency[firstBucket];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return capacity;
    }

    /** Fixed open-addressed table size (a power of two of at least 1.5 x capacity). */
    public int tableCapacity() {
        return keys.length;
    }

    /** Number of evictions since construction or the last {@link #clear()}. */
    public long evictionCount() {
        return evictions;
    }

    /** Key of the most recent eviction; throws {@link IllegalStateException} when none happened. */
    public long lastEvictedKey() {
        requireEviction();
        return lastEvictedKey;
    }

    /** Value of the most recent eviction; throws {@link IllegalStateException} when none happened. */
    public long lastEvictedValue() {
        requireEviction();
        return lastEvictedValue;
    }

    /** Retained primitive payload: table lanes, both state masks and the bucket arena. */
    public long payloadBytes() {
        long table = keys.length;
        return table * (2L * Long.BYTES + 4L * Integer.BYTES)
                + 2L * occupied.length * Long.BYTES
                + 5L * bucketFrequency.length * Integer.BYTES;
    }

    public void clear() {
        if (size == 0 && occupiedCount == 0) {
            return;
        }
        Arrays.fill(keys, 0L);
        Arrays.fill(values, 0L);
        Arrays.fill(frequencies, 0);
        Arrays.fill(occupied, 0L);
        Arrays.fill(tombstones, 0L);
        firstBucket = NONE;
        freeBucket = NONE;
        bucketsAllocated = 0;
        size = 0;
        occupiedCount = 0;
        evictions = 0;
        modCount++;
    }

    /** Keys in eviction order: ascending frequency, least recently used first within a frequency. */
    public long[] keysByEviction() {
        long[] result = new long[size];
        int cursor = 0;
        for (int bucket = firstBucket; bucket != NONE; bucket = bucketNext[bucket]) {
            for (int slot = bucketHead[bucket]; slot != NONE; slot = nextInBucket[slot]) {
                result[cursor++] = keys[slot];
            }
        }
        return result;
    }

    /** Eviction-order key iterator; does not count uses; fail-fast against any modification including {@code get}. */
    public M3LongIterator keyIterator() {
        int expectedModCount = modCount;
        return new M3LongIterator() {
            private int bucket = firstBucket;
            private int slot = firstBucket == NONE ? NONE : bucketHead[firstBucket];

            @Override
            public boolean hasNext() {
                checkForComodification(expectedModCount);
                return slot != NONE;
            }

            @Override
            public long nextLong() {
                checkForComodification(expectedModCount);
                if (slot == NONE) {
                    throw new NoSuchElementException();
                }
                long key = keys[slot];
                slot = nextInBucket[slot];
                while (slot == NONE && bucket != NONE) {
                    bucket = bucketNext[bucket];
                    slot = bucket == NONE ? NONE : bucketHead[bucket];
                }
                return key;
            }
        };
    }

    private void touch(int slot) {
        int frequency = frequencies[slot];
        if (frequency == Integer.MAX_VALUE) {
            moveToTail(slot);
            modCount++;
            return;
        }
        int bucket = bucketOf[slot];
        int target = bucketAfter(bucket, frequency + 1);
        detach(slot);
        frequencies[slot] = frequency + 1;
        attach(slot, target);
        modCount++;
    }

    private void moveToTail(int slot) {
        int bucket = bucketOf[slot];
        if (bucketTail[bucket] == slot) {
            return;
        }
        int before = previousInBucket[slot];
        int after = nextInBucket[slot];
        if (before == NONE) {
            bucketHead[bucket] = after;
        } else {
            nextInBucket[before] = after;
        }
        previousInBucket[after] = before;
        attach(slot, bucket);
    }

    private void evictOne() {
        int slot = bucketHead[firstBucket];
        lastEvictedKey = keys[slot];
        lastEvictedValue = values[slot];
        evictions++;
        removeSlot(slot);
    }

    private void removeSlot(int slot) {
        detach(slot);
        clearBit(occupied, slot);
        setBit(tombstones, slot);
        keys[slot] = 0L;
        values[slot] = 0L;
        frequencies[slot] = 0;
        size--;
        modCount++;
        if (size == 0) {
            Arrays.fill(occupied, 0L);
            Arrays.fill(tombstones, 0L);
            occupiedCount = 0;
        } else if (occupiedCount > size * 2 && occupiedCount > MIN_TABLE) {
            sweep();
        }
    }

    /** Returns the bucket holding exactly {@code frequency} right after {@code previousBucket} ({@code NONE} = list head), creating it when absent. */
    private int bucketAfter(int previousBucket, int frequency) {
        int candidate = previousBucket == NONE ? firstBucket : bucketNext[previousBucket];
        if (candidate != NONE && bucketFrequency[candidate] == frequency) {
            return candidate;
        }
        int bucket = allocateBucket(frequency);
        bucketPrevious[bucket] = previousBucket;
        bucketNext[bucket] = candidate;
        if (previousBucket == NONE) {
            firstBucket = bucket;
        } else {
            bucketNext[previousBucket] = bucket;
        }
        if (candidate != NONE) {
            bucketPrevious[candidate] = bucket;
        }
        return bucket;
    }

    private int allocateBucket(int frequency) {
        int bucket;
        if (freeBucket != NONE) {
            bucket = freeBucket;
            freeBucket = bucketNext[bucket];
        } else {
            bucket = bucketsAllocated++;
        }
        bucketFrequency[bucket] = frequency;
        bucketHead[bucket] = NONE;
        bucketTail[bucket] = NONE;
        return bucket;
    }

    private void releaseBucket(int bucket) {
        int before = bucketPrevious[bucket];
        int after = bucketNext[bucket];
        if (before == NONE) {
            firstBucket = after;
        } else {
            bucketNext[before] = after;
        }
        if (after != NONE) {
            bucketPrevious[after] = before;
        }
        bucketNext[bucket] = freeBucket;
        freeBucket = bucket;
    }

    /** Appends {@code slot} as the most recently used member of {@code bucket}. */
    private void attach(int slot, int bucket) {
        int tail = bucketTail[bucket];
        bucketOf[slot] = bucket;
        previousInBucket[slot] = tail;
        nextInBucket[slot] = NONE;
        if (tail == NONE) {
            bucketHead[bucket] = slot;
        } else {
            nextInBucket[tail] = slot;
        }
        bucketTail[bucket] = slot;
    }

    /** Unlinks {@code slot} from its bucket; a drained bucket leaves the frequency list. */
    private void detach(int slot) {
        int bucket = bucketOf[slot];
        int before = previousInBucket[slot];
        int after = nextInBucket[slot];
        if (before == NONE) {
            bucketHead[bucket] = after;
        } else {
            nextInBucket[before] = after;
        }
        if (after == NONE) {
            bucketTail[bucket] = before;
        } else {
            previousInBucket[after] = before;
        }
        if (bucketHead[bucket] == NONE) {
            releaseBucket(bucket);
        }
    }

    private int findSlot(long key) {
        int mask = keys.length - 1;
        int slot = mix(key) & mask;
        for (;;) {
            if (isSet(occupied, slot)) {
                if (keys[slot] == key) {
                    return slot;
                }
            } else if (!isSet(tombstones, slot)) {
                return NONE;
            }
            slot = (slot + 1) & mask;
        }
    }

    /** Claims the first non-live slot of the probe run for an absent {@code key}. */
    private int claimSlot(long key) {
        int mask = keys.length - 1;
        int slot = mix(key) & mask;
        while (isSet(occupied, slot)) {
            slot = (slot + 1) & mask;
        }
        if (isSet(tombstones, slot)) {
            clearBit(tombstones, slot);
        } else {
            occupiedCount++;
        }
        setBit(occupied, slot);
        keys[slot] = key;
        return slot;
    }

    /** Rebuilds the table lanes at the same size without tombstones, keeping bucket ids and order. */
    private void sweep() {
        long[] oldKeys = keys;
        long[] oldValues = values;
        int[] oldFrequencies = frequencies;
        int[] oldNext = nextInBucket;
        int table = keys.length;
        keys = new long[table];
        values = new long[table];
        frequencies = new int[table];
        occupied = new long[words(table)];
        tombstones = new long[words(table)];
        previousInBucket = new int[table];
        nextInBucket = new int[table];
        bucketOf = new int[table];
        occupiedCount = 0;
        int expected = size;
        size = 0;
        for (int bucket = firstBucket; bucket != NONE; bucket = bucketNext[bucket]) {
            int head = bucketHead[bucket];
            bucketHead[bucket] = NONE;
            bucketTail[bucket] = NONE;
            for (int slot = head; slot != NONE; slot = oldNext[slot]) {
                int target = claimSlot(oldKeys[slot]);
                values[target] = oldValues[slot];
                frequencies[target] = oldFrequencies[slot];
                attach(target, bucket);
                size++;
            }
        }
        if (size != expected) {
            throw new IllegalStateException("sweep lost cache entries");
        }
    }

    private void requireEviction() {
        if (evictions == 0) {
            throw new IllegalStateException("no eviction yet");
        }
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    private static boolean isSet(long[] words, int index) {
        return (words[index >>> 6] & (1L << index)) != 0;
    }

    private static void setBit(long[] words, int index) {
        words[index >>> 6] |= 1L << index;
    }

    private static void clearBit(long[] words, int index) {
        words[index >>> 6] &= ~(1L << index);
    }

    private static int words(int bits) {
        return (bits + 63) >>> 6;
    }

    private static int tableSizeFor(int requested) {
        if (requested >= (1 << 30)) {
            return 1 << 30;
        }
        int normalized = Math.max(MIN_TABLE, requested);
        if ((normalized & (normalized - 1)) == 0) {
            return normalized;
        }
        return Integer.highestOneBit(normalized) << 1;
    }

    private static int mix(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (int) x;
    }
}
