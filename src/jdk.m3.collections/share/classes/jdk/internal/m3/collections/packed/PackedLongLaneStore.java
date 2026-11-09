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

import jdk.internal.m3.collections.LongLaneRowPredicate;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Dense shared-payload row store for composing primitive collection semantics.
 *
 * <p>Every row is only an ordinal across parallel {@code long[]} lanes. Hash, sorted, heap
 * and filter semantics are auxiliary primitive indexes over those rows. Removing a row
 * swap-moves the physical tail and mechanically relocates every registered index before the
 * lane values move. Row ordinals are therefore internal physical addresses, not stable IDs.
 *
 * <p>Not thread-safe. Mutation from callbacks is rejected.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLaneStore.java}
 * blob {@code bd1680a4d527598d9bd18b7c8c5034d8a9c26fbc} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections lane batch (v6, after U2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLaneStore {
    private long[][] lanes;
    private PackedLongLaneIndex[] indexes = new PackedLongLaneIndex[4];
    private int indexCount;
    private int size;
    private int callbacks;
    private boolean mutating;

    public PackedLongLaneStore(int laneCount) {
        this(laneCount, 8);
    }

    public PackedLongLaneStore(int laneCount, int expectedRows) {
        if (laneCount <= 0) {
            throw new IllegalArgumentException("laneCount");
        }
        if (expectedRows < 0) {
            throw new IllegalArgumentException("expectedRows");
        }
        int capacity = Math.max(1, expectedRows);
        lanes = new long[laneCount][];
        for (int lane = 0; lane < laneCount; lane++) {
            lanes[lane] = new long[capacity];
        }
    }

    public int laneCount() {
        return lanes.length;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return lanes[0].length;
    }

    public long get(int row, int lane) {
        checkRow(row);
        checkLane(lane);
        return lanes[lane][row];
    }

    public void set(int row, int lane, long value) {
        writable();
        checkRow(row);
        checkLane(lane);
        long previous = lanes[lane][row];
        if (previous == value) {
            return;
        }
        mutating = true;
        try {
            for (int i = 0; i < indexCount; i++) {
                indexes[i].validateUpdate(row, lane, value);
            }
            for (int i = 0; i < indexCount; i++) {
                indexes[i].beforeUpdate(row, lane);
            }
            lanes[lane][row] = value;
            for (int i = 0; i < indexCount; i++) {
                indexes[i].afterUpdate(row, lane);
            }
        } finally {
            mutating = false;
        }
    }

    /** Appends one row from a reusable caller-owned lane array. */
    public int appendRow(long[] values) {
        writable();
        Objects.requireNonNull(values, "values");
        if (values.length != lanes.length) {
            throw new IllegalArgumentException(
                    "expected " + lanes.length + " lane values, got " + values.length);
        }
        ensureCapacity(Math.addExact(size, 1));
        int row = size;
        for (int lane = 0; lane < lanes.length; lane++) {
            lanes[lane][row] = values[lane];
        }
        commitInsert(row);
        return row;
    }

    /** Allocation-free convenience for two-lane stores. */
    public int append2(long first, long second) {
        writable();
        requireLaneCount(2);
        ensureCapacity(Math.addExact(size, 1));
        int row = size;
        lanes[0][row] = first;
        lanes[1][row] = second;
        commitInsert(row);
        return row;
    }

    /** Allocation-free convenience for three-lane stores. */
    public int append3(long first, long second, long third) {
        writable();
        requireLaneCount(3);
        ensureCapacity(Math.addExact(size, 1));
        int row = size;
        lanes[0][row] = first;
        lanes[1][row] = second;
        lanes[2][row] = third;
        commitInsert(row);
        return row;
    }

    /**
     * Removes one physical row by swap-moving the current tail into its slot.
     *
     * @return the old tail row ordinal, or {@code row} when no relocation occurred
     */
    public int removeSwap(int row) {
        writable();
        checkRow(row);
        mutating = true;
        try {
            int tail = size - 1;
            for (int i = 0; i < indexCount; i++) {
                indexes[i].remove(row);
            }
            if (row != tail) {
                for (int i = 0; i < indexCount; i++) {
                    indexes[i].relocate(tail, row);
                }
                for (long[] lane : lanes) {
                    lane[row] = lane[tail];
                }
            }
            size = tail;
            return tail;
        } finally {
            mutating = false;
        }
    }

    /**
     * Strong-callback filter: predicates are fully evaluated before any row is removed.
     * A throwing predicate leaves the store and all indexes unchanged.
     */
    public int filterInPlace(LongLaneRowPredicate predicate) {
        writable();
        Objects.requireNonNull(predicate, "predicate");
        long[] rejected = new long[(size + 63) >>> 6];
        int removed = 0;
        callbacks++;
        try {
            for (int row = 0; row < size; row++) {
                if (!predicate.test(this, row)) {
                    rejected[row >>> 6] |= 1L << (row & 63);
                    removed++;
                }
            }
        } finally {
            callbacks--;
        }
        if (removed == 0) {
            return 0;
        }
        for (int row = size - 1; row >= 0; row--) {
            if ((rejected[row >>> 6] & (1L << (row & 63))) != 0L) {
                removeSwap(row);
            }
        }
        return removed;
    }

    public void clear() {
        writable();
        mutating = true;
        try {
            for (int i = 0; i < indexCount; i++) {
                indexes[i].clear();
            }
            size = 0;
        } finally {
            mutating = false;
        }
    }

    public long[] laneToArray(int lane) {
        checkLane(lane);
        return Arrays.copyOf(lanes[lane], size);
    }

    public PackedLongLaneSnapshot freeze() {
        return new PackedLongLaneSnapshot(lanes, size);
    }

    public PackedLongLaneHashIndex uniqueHashIndex(int lane) {
        checkLane(lane);
        return register(new PackedLongLaneHashIndex(this, lane, -1));
    }

    public PackedLongLaneHashIndex uniqueHashIndex(int firstLane, int secondLane) {
        checkLane(firstLane);
        checkLane(secondLane);
        if (firstLane == secondLane) {
            throw new IllegalArgumentException("pair key lanes must differ");
        }
        return register(new PackedLongLaneHashIndex(this, firstLane, secondLane));
    }

    public PackedLongLaneSortedIndex sortedIndex(int lane, boolean unique) {
        return sortedIndex(lane, unique, false);
    }

    public PackedLongLaneSortedIndex sortedIndex(
            int lane, boolean unique, boolean unsigned) {
        checkLane(lane);
        return register(new PackedLongLaneSortedIndex(this, lane, unique, unsigned));
    }

    public PackedLongLaneHeapIndex heapIndex(int lane, boolean minHeap) {
        return heapIndex(lane, minHeap, false);
    }

    public PackedLongLaneHeapIndex heapIndex(
            int lane, boolean minHeap, boolean unsigned) {
        checkLane(lane);
        return register(new PackedLongLaneHeapIndex(this, lane, minHeap, unsigned));
    }

    public PackedLongLaneSelection selection(int lane, LongPredicate predicate) {
        checkLane(lane);
        Objects.requireNonNull(predicate, "predicate");
        return register(new PackedLongLaneSelection(this, lane, predicate));
    }

    /** Primitive retained payload only, excluding array/object headers. */
    public long payloadBytes() {
        long bytes = 8L * lanes.length * capacity();
        for (int i = 0; i < indexCount; i++) {
            bytes += indexes[i].payloadBytes();
        }
        return bytes;
    }

    long valueAtUnchecked(int row, int lane) {
        return lanes[lane][row];
    }

    int physicalSize() {
        return size;
    }

    private void commitInsert(int row) {
        mutating = true;
        try {
            for (int i = 0; i < indexCount; i++) {
                indexes[i].validateInsert(row);
            }
            for (int i = 0; i < indexCount; i++) {
                indexes[i].insert(row);
            }
            size++;
        } finally {
            mutating = false;
        }
    }

    private <T extends PackedLongLaneIndex> T register(T index) {
        writable();
        index.ensureRowCapacity(capacity());
        for (int row = 0; row < size; row++) {
            index.validateInsert(row);
            index.insert(row);
        }
        if (indexCount == indexes.length) {
            indexes = Arrays.copyOf(indexes, indexes.length << 1);
        }
        indexes[indexCount++] = index;
        return index;
    }

    private void ensureCapacity(int required) {
        if (required <= capacity()) {
            return;
        }
        int next = PackedSupport.growArrayCapacity(capacity(), required);
        for (int lane = 0; lane < lanes.length; lane++) {
            lanes[lane] = Arrays.copyOf(lanes[lane], next);
        }
        for (int i = 0; i < indexCount; i++) {
            indexes[i].ensureRowCapacity(next);
        }
    }

    private void writable() {
        if (callbacks != 0 || mutating) {
            throw new IllegalStateException("reentrant mutation of lane store");
        }
    }

    private void checkRow(int row) {
        Objects.checkIndex(row, size);
    }

    private void checkLane(int lane) {
        Objects.checkIndex(lane, lanes.length);
    }

    private void requireLaneCount(int required) {
        if (lanes.length != required) {
            throw new IllegalStateException(
                    "operation requires " + required + " lanes, store has " + lanes.length);
        }
    }
}
