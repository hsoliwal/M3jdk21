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

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Sorted row permutation over one shared long lane.
 *
 * <p>Binary search is O(log n). Insert/delete/update use arraycopy and are O(n) worst case.
 * No payload value is duplicated into the index. Registration bulk-loads with one stable
 * merge sort (O(n log n)); an update moves its row with one arraycopy of the span between
 * the old and new rank (O(log n + span)); a store filter tombstones ranks and compacts once.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLaneSortedIndex.java}
 * blob {@code 9729e0e5e3bbeb8953bd337d14439c0e428821f2} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections lane batch (v6, after U2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLaneSortedIndex implements PackedLongLaneIndex {
    private final PackedLongLaneStore store;
    private final int lane;
    private final boolean unique;
    private final boolean unsigned;
    private int[] order;
    private int[] positionByRow;
    private int size;

    PackedLongLaneSortedIndex(
            PackedLongLaneStore store,
            int lane,
            boolean unique,
            boolean unsigned) {
        this.store = Objects.requireNonNull(store, "store");
        this.lane = lane;
        this.unique = unique;
        this.unsigned = unsigned;
        order = new int[Math.max(1, store.physicalSize())];
        positionByRow = new int[Math.max(1, store.capacity())];
        Arrays.fill(positionByRow, -1);
    }

    public int lane() {
        return lane;
    }

    public boolean unique() {
        return unique;
    }

    public boolean unsigned() {
        return unsigned;
    }

    public int size() {
        return size;
    }

    public int rowAtRank(int rank) {
        return order[Objects.checkIndex(rank, size)];
    }

    public int rankOfRow(int row) {
        Objects.checkIndex(row, store.physicalSize());
        return positionByRow[row];
    }

    public int rowOf(long value) {
        int rank = lowerBound(value);
        if (rank < size && compareValueAt(order[rank], value) == 0) {
            return order[rank];
        }
        return -1;
    }

    public int lowerBound(long value) {
        int low = 0;
        int high = size;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (compareValueAt(order[middle], value) < 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    public int upperBound(long value) {
        int low = 0;
        int high = size;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (compareValueAt(order[middle], value) <= 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    public int firstRow() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return order[0];
    }

    public int lastRow() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return order[size - 1];
    }

    public int floorRow(long value, int fallback) {
        int rank = upperBound(value) - 1;
        return rank < 0 ? fallback : order[rank];
    }

    public int ceilingRow(long value, int fallback) {
        int rank = lowerBound(value);
        return rank >= size ? fallback : order[rank];
    }

    public int lowerRow(long value, int fallback) {
        int rank = lowerBound(value) - 1;
        return rank < 0 ? fallback : order[rank];
    }

    public int higherRow(long value, int fallback) {
        int rank = upperBound(value);
        return rank >= size ? fallback : order[rank];
    }

    public int[] rowsToArray() {
        return Arrays.copyOf(order, size);
    }

    @Override
    public void ensureRowCapacity(int capacity) {
        if (capacity <= positionByRow.length) {
            return;
        }
        int old = positionByRow.length;
        positionByRow = Arrays.copyOf(positionByRow, capacity);
        Arrays.fill(positionByRow, old, capacity, -1);
    }

    @Override
    public void validateInsert(int row) {
        if (!unique) {
            return;
        }
        long value = store.valueAtUnchecked(row, lane);
        int existing = rowOf(value);
        if (existing >= 0 && existing != row) {
            throw new IllegalArgumentException(
                    "duplicate sorted-index value: " + value);
        }
    }

    @Override
    public void insert(int row) {
        ensureOrderCapacity(size + 1);
        long value = store.valueAtUnchecked(row, lane);
        int rank = upperBound(value);
        int moved = size - rank;
        if (moved > 0) {
            System.arraycopy(order, rank, order, rank + 1, moved);
            for (int i = rank + 1; i <= size; i++) {
                positionByRow[order[i]] = i;
            }
        }
        order[rank] = row;
        positionByRow[row] = rank;
        size++;
    }

    @Override
    public void validateUpdate(int row, int updatedLane, long newValue) {
        if (updatedLane != lane || !unique) {
            return;
        }
        int rank = lowerBound(newValue);
        if (rank < size) {
            int existing = order[rank];
            if (compareValueAt(existing, newValue) == 0
                    && existing != row) {
                throw new IllegalArgumentException(
                        "duplicate sorted-index value: " + newValue);
            }
        }
    }

    @Override
    public void beforeUpdate(int row, int updatedLane) {
        if (updatedLane == lane) {
            position(row); // same missing-row check as remove(row); the row keeps its rank
        }
    }

    @Override
    public void afterUpdate(int row, int updatedLane) {
        if (updatedLane == lane) {
            int from = position(row);
            moveRank(from, upperBoundExcluding(store.valueAtUnchecked(row, lane), from));
        }
    }

    @Override
    public void remove(int row) {
        int rank = position(row);
        int moved = size - rank - 1;
        if (moved > 0) {
            System.arraycopy(order, rank + 1, order, rank, moved);
            for (int i = rank; i < size - 1; i++) {
                positionByRow[order[i]] = i;
            }
        }
        size--;
        positionByRow[row] = -1;
    }

    @Override
    public void relocate(int fromRow, int toRow) {
        int rank = position(fromRow);
        order[rank] = toRow;
        positionByRow[toRow] = rank;
        positionByRow[fromRow] = -1;
    }

    @Override
    public void clear() {
        Arrays.fill(positionByRow, -1);
        size = 0;
    }

    @Override
    public long payloadBytes() {
        return 4L * (order.length + positionByRow.length);
    }

    /** Registration: rows 0..count-1 by key, equal keys in ascending row order. */
    //FLLDCIM: LEETCODIFY — one stable sort replaces count shifting inserts, O(n^2) -> O(n log n)
    void bulkLoad(int count) {
        ensureOrderCapacity(count);
        long[] keys = new long[count];
        for (int row = 0; row < count; row++) {
            order[row] = row;
            keys[row] = sortKey(row);
        }
        sortByKey(keys, count);
        if (unique) {
            requireDistinct(keys, count);
        }
        renumber(0, count);
        size = count;
    }

    /** Store filter: marks the row's rank dead (-1) without shifting. */
    void tombstone(int row) {
        order[position(row)] = -1;
        positionByRow[row] = -1;
    }

    /** Closes every tombstone in one stable pass and renumbers the survivors. */
    //FLLDCIM: LEETCODIFY — COMPACT, O(n) once per filter instead of O(n) per removed row
    void compactTombstones() {
        int write = 0;
        for (int rank = 0; rank < size; rank++) {
            int row = order[rank];
            if (row >= 0) {
                order[write] = row;
                positionByRow[row] = write++;
            }
        }
        size = write;
    }

    private int position(int row) {
        if (row < 0 || row >= positionByRow.length
                || positionByRow[row] < 0) {
            throw new IllegalStateException(
                    "row missing from sorted index: " + row);
        }
        return positionByRow[row];
    }

    private int compareValueAt(int row, long value) {
        long current = store.valueAtUnchecked(row, lane);
        return unsigned
                ? Long.compareUnsigned(current, value)
                : Long.compare(current, value);
    }

    private void ensureOrderCapacity(int required) {
        if (required <= order.length) {
            return;
        }
        order = Arrays.copyOf(
                order,
                PackedSupport.growArrayCapacity(order.length, required));
    }

    /** Signed-order key: unsigned lanes flip the sign bit so one signed compare serves both. */
    private long sortKey(int row) {
        long value = store.valueAtUnchecked(row, lane);
        return unsigned ? value ^ Long.MIN_VALUE : value;
    }

    /** Stable bottom-up merge sort of keys[0..count) with order; ordered pairs are skipped. */
    //FLLDCIM: LEETCODIFY — stable merge sort, O(n) on nondecreasing input
    private void sortByKey(long[] keys, int count) {
        long[] keyBuffer = new long[Integer.highestOneBit(Math.max(1, count - 1))];
        int[] rowBuffer = new int[keyBuffer.length];
        for (int width = 1; width > 0 && width < count; width <<= 1) {
            for (int low = 0; low < count - width; ) {
                int mid = low + width;
                int high = mid + Math.min(width, count - mid);
                if (keys[mid - 1] > keys[mid]) {
                    mergeRuns(keys, keyBuffer, rowBuffer, low, mid, high);
                }
                low = high;
            }
        }
    }

    /** Merges runs [low, mid) and [mid, high) in place; the left run (lower rows) wins ties. */
    private void mergeRuns(long[] keys, long[] keyBuffer, int[] rowBuffer,
            int low, int mid, int high) {
        int length = mid - low;
        System.arraycopy(keys, low, keyBuffer, 0, length);
        System.arraycopy(order, low, rowBuffer, 0, length);
        int left = 0;
        int right = mid;
        int write = low;
        while (left < length && right < high) {
            boolean takeRight = keys[right] < keyBuffer[left];
            keys[write] = takeRight ? keys[right] : keyBuffer[left];
            order[write++] = takeRight ? order[right++] : rowBuffer[left++];
        }
        System.arraycopy(keyBuffer, left, keys, write, length - left);
        System.arraycopy(rowBuffer, left, order, write, length - left);
    }

    /** The row one-by-one registration rejects first: least row after an equal key. */
    private void requireDistinct(long[] keys, int count) {
        int culprit = Integer.MAX_VALUE;
        for (int rank = 1; rank < count; rank++) {
            if (keys[rank - 1] == keys[rank] && order[rank] < culprit) {
                culprit = order[rank];
            }
        }
        if (culprit != Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "duplicate sorted-index value: " + store.valueAtUnchecked(culprit, lane));
        }
    }

    /** upperBound with rank {@code skip} left out: the rank remove+insert would pick. */
    //FLLDCIM: LEETCODIFY — BINARY_SEARCH over a virtual array without the moving row
    private int upperBoundExcluding(long value, int skip) {
        int low = 0;
        int high = size - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            int rank = middle < skip ? middle : middle + 1;
            if (compareValueAt(order[rank], value) <= 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /** Moves the entry at rank {@code from} to rank {@code to} with one arraycopy of the span. */
    //FLLDCIM: LEETCODIFY — one System.arraycopy of |to - from| instead of two full shifts
    private void moveRank(int from, int to) {
        if (from == to) {
            return;
        }
        int row = order[from];
        if (to < from) {
            System.arraycopy(order, to, order, to + 1, from - to);
        } else {
            System.arraycopy(order, from + 1, order, from, to - from);
        }
        order[to] = row;
        renumber(Math.min(from, to), Math.max(from, to) + 1);
    }

    private void renumber(int fromRank, int toRank) {
        for (int rank = fromRank; rank < toRank; rank++) {
            positionByRow[order[rank]] = rank;
        }
    }
}
