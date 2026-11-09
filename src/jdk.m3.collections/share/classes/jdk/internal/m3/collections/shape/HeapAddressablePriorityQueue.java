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

import java.util.NoSuchElementException;
import java.util.function.IntConsumer;
import jdk.internal.m3.collections.packed.DenseIndexedLongHeap;

/**
 * Generation-checked payload slots over the existing primitive indexed heap owner.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/HeapAddressablePriorityQueue.java}
 * blob {@code 115dad6a5f544bd5d3a0a848c9a7cc91280756df} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class HeapAddressablePriorityQueue<E> extends AbstractAddressablePriorityQueue<E> {
    private DenseIndexedLongHeap heap;
    HeapAddressablePriorityQueue(CollectionRequirements required) { super(required); }
    @Override void reserveIndex(int capacity) {
        DenseIndexedLongHeap replacement = new DenseIndexedLongHeap(capacity, true);
        if (heap != null) {
            int[] ids = new int[heap.size()];
            long[] priorities = new long[ids.length];
            int count = 0;
            for (int id = 0; id < heap.capacity(); id++) {
                if (heap.contains(id)) { ids[count] = id; priorities[count++] = heap.priorityOf(id); }
            }
            replacement.putAll(ids, priorities);
        }
        heap = replacement;
    }
    @Override void putPriority(int id, long priority) { heap.put(id, priority); }
    @Override long priority(int id) { return heap.priorityOf(id); }
    @Override int firstId() {
        if (heap == null) { throw new NoSuchElementException("empty priority queue"); }
        return heap.peekId();
    }
    @Override void removeId(int id) { heap.remove(id); }
    @Override void walkIds(IntConsumer action) { if (heap != null) { heap.forEachId(action); } }
    @Override void clearIndex() { if (heap != null) { heap.clear(); } }
}
