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

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Sorted row permutation over one shared long lane.
 *
 * <p>Binary search is O(log n). Insert/delete/update use arraycopy and are O(n) worst case.
 * No payload value is duplicated into the index.
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
            remove(row);
        }
    }

    @Override
    public void afterUpdate(int row, int updatedLane) {
        if (updatedLane == lane) {
            insert(row);
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
}
