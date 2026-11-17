/* *****************************************************************************
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

import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongSortedSet;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Batched sorted primitive set.
 *
 * <p>Hot inserts append to an unsorted primitive pending lane. When the lane reaches
 * the configured batch size, it is sorted/deduplicated once and merged into the dense
 * sorted lane in a single forward pass. This avoids one arraycopy for every interior
 * insert while retaining a completely node-free representation.
 *
 * <p>An optional collection-level filter is evaluated before retention.
 *
 * <p>Pending lookup scans up to 32 values; past that a lane-ordinal slot index (power-of-two
 * mask, linear probe, load at most 1/2) answers in expected O(1), worst case O(pending). The
 * pending lane keeps insertion order, so flush and every observable result are unchanged.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongBatchSortedSet.java}
 * blob {@code bd23b4cbb87277cc5bf19ab3154ecf7a17e21fbd} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongBatchSortedSet implements LongSortedSet {
    private static final int[] NO_SLOTS = new int[0];
    private static final int PENDING_SCAN_LIMIT = 32;
    private static final int MAX_INDEXED_PENDING = 1 << 29;
    private long[] sorted;
    private int sortedSize;
    private long[] pending;
    private int pendingSize;
    // Conservative owner-local fact. No cache or per-entry metadata survives outside this lane.
    private boolean pendingOrdered = true;
    // Lane-ordinal slot index over pending[0, pendingSize); trusted only while pendingIndexed.
    private int[] pendingSlots = NO_SLOTS;
    private boolean pendingIndexed;
    private final int batchSize;
    private final LongPredicate filter;

    public PackedLongBatchSortedSet(int expectedSize, int batchSize) {
        this(expectedSize, batchSize, value -> true);
    }

    public PackedLongBatchSortedSet(
            int expectedSize,
            int batchSize,
            LongPredicate filter) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize");
        }
        this.batchSize = batchSize;
        this.filter = Objects.requireNonNull(filter, "filter");
        this.sorted = new long[Math.max(1, expectedSize)];
        this.pending = new long[Math.max(1, batchSize)];
    }

    public boolean add(long value) {
        if (!filter.test(value)) {
            return false;
        }
        if (Arrays.binarySearch(sorted, 0, sortedSize, value) >= 0) {
            return false;
        }
        if (pendingIndex(value) >= 0) {
            return false;
        }
        appendPending(value);
        if (pendingSize >= batchSize) {
            flush();
        }
        return true;
    }

    public int addAll(long[] values) {
        Objects.requireNonNull(values, "values");
        int added = 0;
        for (long value : values) {
            if (add(value)) {
                added++;
            }
        }
        return added;
    }

    /**
     * Sorts/deduplicates pending values and performs one dense merge.
     */
    public void flush() {
        if (pendingSize == 0) {
            return;
        }
        // The sort below permutes pending; the slot index is rebuilt by a later append.
        pendingIndexed = false;
        // add() already proves uniqueness and disjointness from the sorted lane.
        int unique = pendingOrdered
                ? pendingSize : PackedLongArrayOps.sortUniqueUnchecked(pending, pendingSize);
        mergePending(unique, PackedArrayCapacity.required(sortedSize, unique));
        pendingSize = 0;
        pendingOrdered = true;
    }

    private void mergePending(int unique, int required) {
        if (sortedSize == 0 || sorted[sortedSize - 1] < pending[0]) {
            if (required > sorted.length) { sorted = Arrays.copyOf(sorted, required); }
            System.arraycopy(pending, 0, sorted, sortedSize, unique);
            sortedSize = required;
        } else if (pending[unique - 1] < sorted[0]) {
            if (required > sorted.length) { sorted = Arrays.copyOf(sorted, required); }
            System.arraycopy(sorted, 0, sorted, unique, sortedSize);
            System.arraycopy(pending, 0, sorted, 0, unique);
            sortedSize = required;
        } else {
            long[] merged = new long[Math.max(sorted.length, required)];
            sortedSize = PackedLongArrayOps.unionUniqueUnchecked(sorted, sortedSize, pending, unique, merged);
            sorted = merged;
        }
    }

    @Override
    public boolean contains(long value) {
        if (Arrays.binarySearch(sorted, 0, sortedSize, value) >= 0) {
            return true;
        }
        return pendingIndex(value) >= 0;
    }

    public boolean remove(long value) {
        int sortedIndex = Arrays.binarySearch(sorted, 0, sortedSize, value);
        if (sortedIndex >= 0) {
            int moved = sortedSize - sortedIndex - 1;
            if (moved > 0) {
                System.arraycopy(sorted, sortedIndex + 1, sorted, sortedIndex, moved);
            }
            sortedSize--;
            return true;
        }
        int index = pendingIndex(value);
        if (index < 0) {
            return false;
        }
        removePendingAt(index);
        return true;
    }

    /**
     * Flushes pending values and keeps matching values in sorted order.
     * On predicate failure, prior rejections are committed, but the throwing
     * value and untested suffix remain intact. The predicate must not mutate
     * this collection. A null predicate is rejected before flushing.
     *
     * @param predicate values to retain
     * @return number of removed values
     */
    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        flush();
        int originalSize = sortedSize;
        int read = 0;
        int write = 0;
        try {
            for (; read < originalSize; read++) {
                long value = sorted[read];
                if (predicate.test(value)) {
                    sorted[write++] = value;
                }
            }
        } finally {
            // Retain the throwing value and unread suffix if the callback fails.
            System.arraycopy(sorted, read, sorted, write, originalSize - read);
            sortedSize = write + originalSize - read;
        }
        return originalSize - sortedSize;
    }

    public long first() {
        flush();
        if (sortedSize == 0) {
            throw new NoSuchElementException();
        }
        return sorted[0];
    }

    public long last() {
        flush();
        if (sortedSize == 0) {
            throw new NoSuchElementException();
        }
        return sorted[sortedSize - 1];
    }

    public long floor(long value, long defaultValue) {
        flush();
        int index = Arrays.binarySearch(sorted, 0, sortedSize, value);
        if (index >= 0) {
            return sorted[index];
        }
        int insertionPoint = -index - 1;
        return insertionPoint == 0 ? defaultValue : sorted[insertionPoint - 1];
    }

    public long ceiling(long value, long defaultValue) {
        flush();
        int index = Arrays.binarySearch(sorted, 0, sortedSize, value);
        if (index >= 0) {
            return sorted[index];
        }
        int insertionPoint = -index - 1;
        return insertionPoint == sortedSize ? defaultValue : sorted[insertionPoint];
    }

    @Override
    public long lower(long value, long defaultValue) {
        flush();
        int index = Arrays.binarySearch(sorted, 0, sortedSize, value);
        int candidate = index >= 0 ? index - 1 : -index - 2;
        return candidate < 0 ? defaultValue : sorted[candidate];
    }

    @Override
    public long higher(long value, long defaultValue) {
        flush();
        int index = Arrays.binarySearch(sorted, 0, sortedSize, value);
        int candidate = index >= 0 ? index + 1 : -index - 1;
        return candidate >= sortedSize ? defaultValue : sorted[candidate];
    }

    public int pendingSize() {
        return pendingSize;
    }

    public int batchSize() {
        return batchSize;
    }

    @Override
    public int size() {
        flush();
        return sortedSize;
    }

    @Override
    public void clear() {
        sortedSize = 0;
        pendingSize = 0;
        pendingOrdered = true;
        pendingIndexed = false;
    }

    public void trimToSize() {
        flush();
        sorted = Arrays.copyOf(sorted, Math.max(1, sortedSize));
        pending = new long[Math.max(1, batchSize)];
        pendingSlots = NO_SLOTS;
        pendingIndexed = false;
    }

    @Override
    public long[] toArray() {
        flush();
        return Arrays.copyOf(sorted, sortedSize);
    }

    @Override
    public LongIterator iterator() {
        flush();
        return new LongIterator() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < sortedSize;
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return sorted[index++];
            }
        };
    }

    /** Pending lane index of {@code value} or -1: a short scan, else the slot index. */
    private int pendingIndex(long value) {
        if (pendingIndexed) {
            return PackedHashAtoms.findLaneIndex(pendingSlots, pending, value);
        }
        for (int i = 0; i < pendingSize; i++) {
            if (pending[i] == value) {
                return i;
            }
        }
        return -1;
    }

    /** Appends one proven-new value; the slot index covers the lane once it outgrows a scan. */
    private void appendPending(long value) {
        ensurePendingCapacity(PackedArrayCapacity.required(pendingSize, 1));
        preparePendingIndex(pendingSize + 1);
        if (pendingSize > 0 && pending[pendingSize - 1] > value) { pendingOrdered = false; }
        pending[pendingSize++] = value;
        if (pendingIndexed) {
            PackedHashAtoms.placeLaneIndex(pendingSlots, pending, pendingSize - 1);
        }
    }

    /** Builds or grows the slot index before the lane holds {@code count} values. */
    private void preparePendingIndex(int count) {
        // A batch-closing append flushes at once, so it neither builds nor keeps an index.
        if (count >= batchSize || count > MAX_INDEXED_PENDING
                || (!pendingIndexed && count <= PENDING_SCAN_LIMIT)) {
            pendingIndexed = false;
            return;
        }
        if (!pendingIndexed || count > (pendingSlots.length >>> 1)) {
            pendingSlots = PackedHashAtoms.rebuildLaneIndex(
                    pendingSlots, pending, pendingSize, PackedHashAtoms.laneIndexLength(count));
            pendingIndexed = true;
        }
    }

    /** Removes pending[index] keeping lane order; the slot index is rebased in step. */
    private void removePendingAt(int index) {
        if (pendingIndexed) {
            PackedHashAtoms.deleteLaneIndex(pendingSlots, pending, index);
        }
        int moved = pendingSize - index - 1;
        if (moved > 0) {
            System.arraycopy(pending, index + 1, pending, index, moved);
            if (pendingIndexed) {
                PackedHashAtoms.shiftLaneIndexes(pendingSlots, index);
            }
        }
        pendingSize--;
        if (pendingSize < 2) { pendingOrdered = true; }
    }

    private void ensurePendingCapacity(int minimumCapacity) {
        if (minimumCapacity <= pending.length) {
            return;
        }
        pending = Arrays.copyOf(
                pending,
                PackedSupport.growArrayCapacity(pending.length, minimumCapacity));
    }
}
