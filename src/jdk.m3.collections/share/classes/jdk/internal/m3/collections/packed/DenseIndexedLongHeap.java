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
import java.util.function.IntConsumer;
import jdk.internal.m3.collections.IntLongIndexedPriorityQueue;

/**
 * Indexed priority queue: int[] heap IDs, int[] inverse positions, long[] priorities.
 *
 * <p>The ID universe is [0, capacity). Priority updates and arbitrary removal are
 * O(log n), root inspection O(1), batch heapification O(capacity + batch size) in
 * the worst case. Signed priorities retain every long value; no float conversion,
 * boxed priority or per-entry object is used. Lower ID wins priority ties.
 * Single-threaded. Capacity accounts for the entire ID universe, not just live IDs.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseIndexedLongHeap.java}
 * blob {@code ed5a8a4d530f14bde50ec5425e18719cc071e751} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DenseIndexedLongHeap implements IntLongIndexedPriorityQueue {
    private final int[] heap;
    private final int[] positions;
    private final long[] priorities;
    private final boolean minimumFirst;
    private int size;

    public DenseIndexedLongHeap(int capacity, boolean minimumFirst) {
        if (capacity < 0 || capacity > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("capacity");
        }
        this.heap = new int[capacity];
        this.positions = new int[capacity];
        this.priorities = new long[capacity];
        this.minimumFirst = minimumFirst;
        Arrays.fill(positions, -1);
    }

    public int size() { return size; }
    public int capacity() { return positions.length; }
    public boolean isEmpty() { return size == 0; }

    public boolean contains(int id) {
        Objects.checkIndex(id, positions.length);
        return positions[id] >= 0;
    }

    public long priorityOf(int id) {
        if (!contains(id)) { throw new NoSuchElementException("absent ID: " + id); }
        return priorities[id];
    }

    private boolean better(int left, int right) {
        int comparison = Long.compare(priorities[left], priorities[right]);
        if (!minimumFirst) { comparison = -comparison; }
        return comparison < 0 || comparison == 0 && left < right;
    }

    /** Insert an absent ID or change an existing priority without duplicate queue entries. */
    public void put(int id, long priority) {
        Objects.checkIndex(id, positions.length);
        int position = positions[id];
        priorities[id] = priority;
        if (position < 0) {
            position = size++;
            heap[position] = id;
            positions[id] = position;
        }
        fix(position);
    }

    /** Validate all IDs first; duplicate input IDs use the last supplied priority. */
    public void putAll(int[] ids, long[] values) {
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(values, "values");
        if (ids.length != values.length) { throw new IllegalArgumentException("lane lengths differ"); }
        for (int id : ids) { Objects.checkIndex(id, positions.length); }
        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
            priorities[id] = values[i];
            if (positions[id] < 0) {
                positions[id] = size;
                heap[size++] = id;
            }
        }
        for (int i = PackedHeapAtoms.lastParent(size); i >= 0; i--) { siftDown(i); }
    }

    private void fix(int index) {
        if (index > 0 && better(heap[index], heap[PackedHeapAtoms.parent(index)])) { siftUp(index); }
        else { siftDown(index); }
    }

    private void siftUp(int index) {
        int id = heap[index];
        while (index > 0) {
            int parent = PackedHeapAtoms.parent(index);
            if (!better(id, heap[parent])) { break; }
            heap[index] = heap[parent];
            positions[heap[index]] = index;
            index = parent;
        }
        heap[index] = id;
        positions[id] = index;
    }

    private void siftDown(int index) {
        int id = heap[index];
        while (PackedHeapAtoms.hasChildren(index, size)) {
            int child = PackedHeapAtoms.left(index);
            if (child + 1 < size && better(heap[child + 1], heap[child])) { child++; }
            if (!better(heap[child], id)) { break; }
            heap[index] = heap[child];
            positions[heap[index]] = index;
            index = child;
        }
        heap[index] = id;
        positions[id] = index;
    }

    public int peekId() {
        if (size == 0) { throw new NoSuchElementException("empty heap"); }
        return heap[0];
    }

    public int pollId() {
        int id = peekId();
        remove(id);
        return id;
    }

    public boolean remove(int id) {
        if (!contains(id)) { return false; }
        int position = positions[id];
        int last = heap[--size];
        positions[id] = -1;
        priorities[id] = 0L;
        heap[size] = 0;
        if (position < size) {
            heap[position] = last;
            positions[last] = position;
            fix(position);
        }
        return true;
    }

    /**
     * Visit each live ID once in unspecified heap order without boxing or a snapshot.
     * The action must not mutate this heap; priorities remain readable by ID.
     */
    public void forEachId(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) { action.accept(heap[i]); }
    }

    public void clear() {
        Arrays.fill(heap, 0);
        Arrays.fill(positions, -1);
        Arrays.fill(priorities, 0L);
        size = 0;
    }
}
