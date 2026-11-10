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
 * Binary heap of row ordinals over one shared priority lane.
 *
 * <p>The priority value remains in the lane store. A second int[] maps row->heap position so
 * arbitrary row removal, priority update and store swap-relocation remain O(log n)/O(1).
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLaneHeapIndex.java}
 * blob {@code c63a34369ca223af61ed0e374c16ff089ae7ee5e} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections lane batch (v6, after U2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLaneHeapIndex implements PackedLongLaneIndex {
    private final PackedLongLaneStore store;
    private final int lane;
    private final boolean minHeap;
    private final boolean unsigned;
    private int[] heap;
    private int[] positionByRow;
    private int size;

    PackedLongLaneHeapIndex(
            PackedLongLaneStore store,
            int lane,
            boolean minHeap,
            boolean unsigned) {
        this.store = Objects.requireNonNull(store, "store");
        this.lane = lane;
        this.minHeap = minHeap;
        this.unsigned = unsigned;
        heap = new int[Math.max(1, store.physicalSize())];
        positionByRow = new int[Math.max(1, store.capacity())];
        Arrays.fill(positionByRow, -1);
    }

    public int lane() {
        return lane;
    }

    public boolean isMinHeap() {
        return minHeap;
    }

    public boolean unsigned() {
        return unsigned;
    }

    public int size() {
        return size;
    }

    public int peekRow() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return heap[0];
    }

    public long peekPriority() {
        return store.get(peekRow(), lane);
    }

    /** Removes the top logical row from the underlying shared store. */
    public int pollRow() {
        int row = peekRow();
        store.removeSwap(row);
        return row;
    }

    public int positionOfRow(int row) {
        Objects.checkIndex(row, store.physicalSize());
        return positionByRow[row];
    }

    public int[] heapRowsToArray() {
        return Arrays.copyOf(heap, size);
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
        // No uniqueness constraint.
    }

    @Override
    public void insert(int row) {
        ensureHeapCapacity(size + 1);
        heap[size] = row;
        positionByRow[row] = size;
        siftUp(size++);
    }

    @Override
    public void validateUpdate(int row, int updatedLane, long newValue) {
        // Every long priority is valid.
    }

    @Override
    public void beforeUpdate(int row, int updatedLane) {
        // Keep the row in place until the new priority is visible.
    }

    @Override
    public void afterUpdate(int row, int updatedLane) {
        if (updatedLane == lane) {
            fix(position(row));
        }
    }

    @Override
    public void remove(int row) {
        int position = position(row);
        int last = --size;
        positionByRow[row] = -1;
        if (position == last) {
            return;
        }
        int movedRow = heap[last];
        heap[position] = movedRow;
        positionByRow[movedRow] = position;
        fix(position);
    }

    @Override
    public void relocate(int fromRow, int toRow) {
        int position = position(fromRow);
        heap[position] = toRow;
        positionByRow[toRow] = position;
        positionByRow[fromRow] = -1;
    }

    @Override
    public void clear() {
        Arrays.fill(positionByRow, -1);
        size = 0;
    }

    @Override
    public long payloadBytes() {
        return 4L * (heap.length + positionByRow.length);
    }

    private int position(int row) {
        if (row < 0 || row >= positionByRow.length
                || positionByRow[row] < 0) {
            throw new IllegalStateException(
                    "row missing from heap index: " + row);
        }
        return positionByRow[row];
    }

    private void fix(int position) {
        if (position > 0) {
            int parent = PackedHeapAtoms.parent(position);
            if (before(heap[position], heap[parent])) {
                siftUp(position);
                return;
            }
        }
        siftDown(position);
    }

    private void siftUp(int position) {
        int row = heap[position];
        while (position > 0) {
            int parent = PackedHeapAtoms.parent(position);
            int parentRow = heap[parent];
            if (!before(row, parentRow)) {
                break;
            }
            heap[position] = parentRow;
            positionByRow[parentRow] = position;
            position = parent;
        }
        heap[position] = row;
        positionByRow[row] = position;
    }

    private void siftDown(int position) {
        int row = heap[position];
        int half = PackedHeapAtoms.firstLeaf(size);
        while (position < half) {
            int child = PackedHeapAtoms.left(position);
            int right = PackedHeapAtoms.right(position);
            if (right < size && before(heap[right], heap[child])) {
                child = right;
            }
            int childRow = heap[child];
            if (!before(childRow, row)) {
                break;
            }
            heap[position] = childRow;
            positionByRow[childRow] = position;
            position = child;
        }
        heap[position] = row;
        positionByRow[row] = position;
    }

    private boolean before(int leftRow, int rightRow) {
        long left = store.valueAtUnchecked(leftRow, lane);
        long right = store.valueAtUnchecked(rightRow, lane);
        int compared = unsigned
                ? Long.compareUnsigned(left, right)
                : Long.compare(left, right);
        return minHeap ? compared < 0 : compared > 0;
    }

    private void ensureHeapCapacity(int required) {
        if (required <= heap.length) {
            return;
        }
        heap = Arrays.copyOf(
                heap,
                PackedSupport.growArrayCapacity(heap.length, required));
    }
}
