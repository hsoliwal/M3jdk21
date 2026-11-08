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
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongLongConsumer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.NoSuchElementException;
import java.util.Objects;
import jdk.internal.m3.collections.LongLongPredicate;
import jdk.internal.m3.collections.PackedLongLongOrderedMap;

/** Bounded primitive LRU cache composed from the repaired stable-slot ordered map.
 * Admission happens before eviction, growth or value replacement. Positive configured capacity
 * reserves initial storage; pressure adjustment may release it and later refill lazily.
 * put/get/remove need no internal per-entry allocation. The configured hard ceiling never changes.
 * No loading, expiry, synchronization, persistence or weak-reference semantics are implied.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongLruCache.java}
 * blob {@code 3f4b2be64db5a9c3e9b34e6e8f6116bb109e1386} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongLruCache {
    private final int capacity;
    private PackedLongLongOrderedMap entries;
    private final LongLongPredicate admission;
    private int residentCapacity;
    private int callbacks;
    private long evictions;

    public PackedLongLongLruCache(int capacity) { this(capacity, null); }
    public PackedLongLongLruCache(int capacity, LongLongPredicate admission) {
        if (capacity < 0) throw new IllegalArgumentException("negative cache capacity");
        this.capacity = capacity; this.residentCapacity = capacity; this.admission = admission;
        entries = capacity == 0 ? null : new PackedLongLongOrderedMap(capacity, true);
    }
    private void writable() {
        if (callbacks != 0) throw new IllegalStateException("mutation during callback");
    }
    /** True when admitted (both new mappings and replacements); false leaves all state intact. */
    public boolean put(long key, long value) {
        writable();
        if (residentCapacity == 0) return false;
        if (admission != null) {
            boolean accepted; callbacks++;
            try { accepted = admission.test(key, value); }
            finally { callbacks--; }
            if (!accepted) return false;
        }
        if (entries == null) {
            entries = new PackedLongLongOrderedMap(Math.min(residentCapacity, 8), true);
        }
        if (!entries.containsKey(key) && entries.size() == residentCapacity) {
            entries.removeFirstKey(); evictions++;
        }
        entries.put(key, value); return true;
    }
    public long getOrDefault(long key, long fallback) {
        writable();
        return entries == null ? fallback : entries.getOrDefault(key, fallback);
    }
    public long peekOrDefault(long key, long fallback) {
        return entries == null ? fallback : entries.peekOrDefault(key, fallback);
    }
    public boolean containsKey(long key) { return entries != null && entries.containsKey(key); }
    public boolean remove(long key) { writable(); return entries != null && entries.remove(key); }
    public void clear() { writable(); if (entries != null) entries.clear(); }
    public int size() { return entries == null ? 0 : entries.size(); }
    /** Configured hard ceiling; pressure adjustment never changes this value. */
    public int capacity() { return capacity; }

    /** Current optional-residency ceiling after pressure adjustment. */
    public int residentCapacity() { return residentCapacity; }

    /**
     * Adjust optional cache residency from a caller-supplied normalized availability ratio.
     *
     * <p>Shrink is immediate and evicts least-recent mappings. Recovery only raises the
     * resident ceiling; evicted mappings are not resurrected.</p>
     *
     * @return number of mappings evicted by this adjustment
     */
    public int readjust(double availabilityRatio) {
        writable();
        if (!Double.isFinite(availabilityRatio)
                || availabilityRatio < 0.0
                || availabilityRatio > 1.0) {
            throw new IllegalArgumentException("availability ratio must be within [0,1]");
        }
        int target = availabilityRatio == 1.0
                ? capacity
                : BigDecimal.valueOf(capacity)
                        .multiply(BigDecimal.valueOf(availabilityRatio))
                        .setScale(0, RoundingMode.FLOOR)
                        .intValueExact();
        if (target == residentCapacity) {
            if (entries != null && entries.size() == 0) {
                entries = null;
            }
            return 0;
        }

        if (target > residentCapacity) {
            residentCapacity = target;
            return 0;
        }

        int currentSize = size();
        int retained = Math.min(currentSize, target);
        int removed = currentSize - retained;

        if (retained == 0) {
            entries = null;
        } else if (entries.capacity() > target || removed > 0) {
            PackedLongLongOrderedMap replacement = new PackedLongLongOrderedMap(retained, true);
            int skip = currentSize - retained;
            int[] position = {0};
            entries.forEach((key, value) -> {
                if (position[0]++ >= skip) {
                    replacement.put(key, value);
                }
            });
            entries = replacement;
        }

        residentCapacity = target;
        evictions += removed;
        return removed;
    }

    public long evictions() { return evictions; }
    public long leastRecentKey() {
        if (entries == null) throw new NoSuchElementException("empty ordered map");
        return entries.firstKey();
    }
    public long mostRecentKey() {
        if (entries == null) throw new NoSuchElementException("empty ordered map");
        return entries.lastKey();
    }
    public long[] keysOldestFirst() {
        return entries == null ? new long[0] : entries.keysToArray();
    }
    public long arrayPayloadBytes() { return entries == null ? 0L : entries.arrayPayloadBytes(); }
    public void forEachOldestFirst(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try { if (entries != null) entries.forEach(consumer); } finally { callbacks--; }
    }
}
