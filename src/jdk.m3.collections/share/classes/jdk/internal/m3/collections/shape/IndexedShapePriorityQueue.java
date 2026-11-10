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
package jdk.internal.m3.collections.shape;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.ObjLongConsumer;
import jdk.internal.m3.collections.packed.DenseIndexedLongHeap;

/**
 * Reuses the primitive indexed heap; heap repairs move IDs, not payload references.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/IndexedShapePriorityQueue.java}
 * blob {@code 1506c70a1e21302cf8864fa84aacd3a0a6ec7092} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class IndexedShapePriorityQueue<E> extends ShapeGuard implements ShapePriorityQueue<E> {
    private static final Object[] EMPTY_OBJECTS = new Object[0];
    private static final int[] EMPTY_INTS = new int[0];
    private Object[] values = EMPTY_OBJECTS;
    private int[] freeNext = EMPTY_INTS;
    private DenseIndexedLongHeap heap;
    private int highWater;
    private int free = -1;
    IndexedShapePriorityQueue(CollectionRequirements required) { super(required); }
    @Override public int size() { return heap == null ? 0 : heap.size(); }
    @Override public void add(E value, long priority) {
        writable(); element(value); growing(size());
        int row = free < 0 ? highWater : free;
        if (row == values.length) { reserve(grown(values.length, row + 1, required.maximumSize())); }
        if (free < 0) { highWater++; } else { free = freeNext[row]; }
        values[row] = value; heap.put(row, priority);
    }
    private void reserve(int capacity) {
        Object[] newValues = Arrays.copyOf(values, capacity);
        int[] newFree = Arrays.copyOf(freeNext, capacity);
        DenseIndexedLongHeap replacement = new DenseIndexedLongHeap(capacity, true);
        if (heap != null) {
            int[] ids = new int[heap.size()];
            long[] priorities = new long[ids.length];
            int count = 0;
            for (int row = 0; row < highWater; row++) {
                if (heap.contains(row)) { ids[count] = row; priorities[count++] = heap.priorityOf(row); }
            }
            replacement.putAll(ids, priorities);
        }
        values = newValues; freeNext = newFree; heap = replacement;
    }
    private int firstId() {
        if (heap == null) { throw new NoSuchElementException("empty priority queue"); }
        return heap.peekId();
    }
    @Override public long firstPriority() { int id = firstId(); return heap.priorityOf(id); }
    @Override public E first() { return read(values, firstId()); }
    @Override public E removeFirst() {
        writable(); int row = firstId(); E old = read(values, row);
        heap.remove(row); values[row] = null; freeNext[row] = free; free = row;
        return old;
    }
    @Override public void forEach(ObjLongConsumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try {
            if (heap != null) { heap.forEachId(id -> action.accept(read(values, id), heap.priorityOf(id))); }
        } finally { leave(); }
    }
    @Override public void clear() {
        writable(); values = EMPTY_OBJECTS; freeNext = EMPTY_INTS; heap = null; highWater = 0; free = -1;
    }
    @Override public ShapeStorage storage() {
        return new ShapeStorage(values.length, 20L * values.length, values.length == 0 ? 0 : 5, 2 * values.length);
    }
}
