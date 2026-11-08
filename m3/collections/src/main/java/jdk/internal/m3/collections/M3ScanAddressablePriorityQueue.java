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
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

/**
 * Constant-time ID edits and linear root search over dense live IDs, never high-water holes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ScanAddressablePriorityQueue.java}
 * blob {@code 7fe1d1f725671acfec4c077fe723a4c7ca5c8585} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3ScanAddressablePriorityQueue<E> extends M3AbstractAddressablePriorityQueue<E> {
    private int[] ids = new int[0];
    private int[] positions = new int[0];
    private long[] priorities = new long[0];
    private int count;
    M3ScanAddressablePriorityQueue(M3CollectionRequirements required) { super(required); }
    @Override void reserveIndex(int capacity) {
        int[] newIds = Arrays.copyOf(ids, capacity);
        int[] newPositions = Arrays.copyOf(positions, capacity);
        long[] newPriorities = Arrays.copyOf(priorities, capacity);
        Arrays.fill(newPositions, positions.length, capacity, -1);
        ids = newIds; positions = newPositions; priorities = newPriorities;
    }
    @Override void putPriority(int id, long priority) {
        if (positions[id] < 0) { positions[id] = count; ids[count++] = id; }
        priorities[id] = priority;
    }
    @Override long priority(int id) { return priorities[id]; }
    @Override int firstId() {
        if (count == 0) { throw new NoSuchElementException("empty priority queue"); }
        int best = ids[0];
        for (int i = 1; i < count; i++) {
            int id = ids[i]; int comparison = Long.compare(priorities[id], priorities[best]);
            if (comparison < 0 || comparison == 0 && id < best) { best = id; }
        }
        return best;
    }
    @Override void removeId(int id) {
        int at = positions[id]; int last = ids[--count];
        if (at < count) { ids[at] = last; positions[last] = at; }
        ids[count] = 0; positions[id] = -1; priorities[id] = 0;
    }
    @Override void walkIds(IntConsumer action) {
        for (int i = 0; i < count; i++) { action.accept(ids[i]); }
    }
    @Override void clearIndex() {
        Arrays.fill(ids, 0); Arrays.fill(positions, -1); Arrays.fill(priorities, 0); count = 0;
    }
}
