// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded LRU owner for shared immutable collection metadata in one composite index.
 * Slots, hash buckets and recency links are primitive arrays; there is one metadata
 * object per cached snapshot, never one entry per collection occurrence.
 *
 * <p>The byte budget covers retained occurrence-index primitive arrays. Fixed cache
 * arrays, object headers, source arenas and caller-owned weights/derived indexes are
 * separate. Admission checks a conservative all-distinct bound before sort scratch
 * allocation; a heavily repeated lane can therefore be rejected conservatively.
 * Building additionally needs one temporary long per occurrence in a lane.
 * This owner and its composite index require external synchronization.
 */
public final class MIndexCollectionPrecompute {
    private final MIndexCompositeIndex index;
    private final long maxPrimitiveBytes;
    private final MIndexCollectionMetadata[] values;
    private final int[] keys;
    private final int[] buckets;
    private final int[] previous;
    private final int[] next;
    private final int[] free;
    private int freeCount;
    private int newest = -1;
    private int oldest = -1;
    private long primitiveBytes;
    private long hits;
    private long misses;

    public MIndexCollectionPrecompute(MIndexCompositeIndex index, int maxEntries, long maxPrimitiveBytes) {
        this.index = Objects.requireNonNull(index, "index");
        if (maxEntries < 0 || maxEntries > (1 << 28) || maxPrimitiveBytes < 0) {
            throw new IllegalArgumentException("invalid precompute capacity/budget");
        }
        this.maxPrimitiveBytes = maxPrimitiveBytes;
        values = new MIndexCollectionMetadata[maxEntries];
        keys = new int[maxEntries];
        previous = new int[maxEntries];
        next = new int[maxEntries];
        free = new int[maxEntries];
        int capacity = 1;
        while (capacity < (long) maxEntries * 2) { capacity <<= 1; }
        buckets = new int[capacity];
        clear();
    }

    public MIndexCompositeIndex compositeIndex() { return index; }
    public int cachedSize() { return values.length - freeCount; }
    public long primitiveBytes() { return primitiveBytes; }
    public long hitCount() { return hits; }
    public long missCount() { return misses; }

    public MIndexCollectionMetadata prepare(MIndexCanonicalCollection source) {
        Objects.requireNonNull(source, "source");
        if (source.compositeIndex() != index) {
            throw new IllegalArgumentException("collection belongs to another composite index");
        }
        // Reject custom implementations even if they claim a cached built-in coordinate.
        long estimate = MIndexCollectionMetadata.estimatedPrimitiveBytes(source);
        int key = source.canonicalId();
        int bucket = bucket(key);
        if (buckets[bucket] != 0) {
            int slot = buckets[bucket] - 1;
            touch(slot);
            hits++;
            return values[slot];
        }
        if (estimate > maxPrimitiveBytes) {
            throw new IllegalArgumentException("collection exceeds precompute admission budget");
        }
        MIndexCollectionMetadata prepared = new MIndexCollectionMetadata(source);
        misses++;
        if (values.length == 0) { return prepared; }
        while (freeCount == 0 || prepared.primitiveBytes() > maxPrimitiveBytes - primitiveBytes) {
            evictOldest();
        }
        int slot = free[--freeCount];
        keys[slot] = key;
        values[slot] = prepared;
        buckets[bucket(key)] = slot + 1;
        primitiveBytes += prepared.primitiveBytes();
        linkNewest(slot);
        return prepared;
    }

    /** Releases cache ownership; already returned metadata stays valid. Counters are cumulative. */
    public void clear() {
        Arrays.fill(values, null);
        Arrays.fill(buckets, 0);
        Arrays.fill(previous, -1);
        Arrays.fill(next, -1);
        freeCount = free.length;
        for (int slot = 0; slot < free.length; slot++) { free[slot] = free.length - slot - 1; }
        newest = -1;
        oldest = -1;
        primitiveBytes = 0;
    }

    private int bucket(int key) {
        int mixed = key ^ (key >>> 16);
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        int bucket = mixed & (buckets.length - 1);
        while (buckets[bucket] != 0 && keys[buckets[bucket] - 1] != key) {
            bucket = (bucket + 1) & (buckets.length - 1);
        }
        return bucket;
    }

    private void evictOldest() {
        int slot = oldest;
        unlink(slot);
        primitiveBytes -= values[slot].primitiveBytes();
        values[slot] = null;
        int bucket = bucket(keys[slot]);
        buckets[bucket] = 0;
        // Reinsert the following probe cluster so deletion cannot hide a displaced key.
        bucket = (bucket + 1) & (buckets.length - 1);
        while (buckets[bucket] != 0) {
            int displaced = buckets[bucket];
            buckets[bucket] = 0;
            buckets[bucket(keys[displaced - 1])] = displaced;
            bucket = (bucket + 1) & (buckets.length - 1);
        }
        free[freeCount++] = slot;
    }

    private void touch(int slot) {
        if (slot != newest) { unlink(slot); linkNewest(slot); }
    }

    private void unlink(int slot) {
        int before = previous[slot];
        int after = next[slot];
        if (before < 0) { oldest = after; } else { next[before] = after; }
        if (after < 0) { newest = before; } else { previous[after] = before; }
    }

    private void linkNewest(int slot) {
        previous[slot] = newest;
        next[slot] = -1;
        if (newest < 0) { oldest = slot; } else { next[newest] = slot; }
        newest = slot;
    }
}
