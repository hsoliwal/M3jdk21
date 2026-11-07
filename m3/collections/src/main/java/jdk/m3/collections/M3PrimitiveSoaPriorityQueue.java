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
package jdk.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Priority queue for multi-lane primitive rows.
 *
 * <p>The heap stores rows in one {@link M3PrimitiveSoaStore}. Every heap swap moves
 * all data/state/id/version lanes together; there is no heap-node object.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveSoaPriorityQueue.java}
 * blob {@code 28c04fbb255c8e955287737d55e21c6624a3a711} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveSoaPriorityQueue {
    private final M3PrimitiveSoaStore store;
    private final int priorityLane;
    private final PrimitiveKind priorityKind;
    private final boolean minFirst;

    public M3PrimitiveSoaPriorityQueue(
            int initialCapacity,
            int metadataMask,
            int priorityLane,
            boolean minFirst,
            PrimitiveKind... laneKinds) {
        Objects.requireNonNull(laneKinds, "laneKinds");
        if (priorityLane < 0 || priorityLane >= laneKinds.length) {
            throw new IndexOutOfBoundsException(priorityLane);
        }
        store = new M3PrimitiveSoaStore(initialCapacity, metadataMask, laneKinds);
        this.priorityLane = priorityLane;
        priorityKind = laneKinds[priorityLane];
        this.minFirst = minFirst;
    }

    public synchronized int size() { return store.size(); }
    public synchronized boolean isEmpty() { return store.isEmpty(); }
    public int laneCount() { return store.laneCount(); }
    public PrimitiveKind laneKind(int lane) { return store.laneKind(lane); }
    public int priorityLane() { return priorityLane; }
    public boolean minFirst() { return minFirst; }
    public synchronized long payloadBytes() { return store.payloadBytes(); }

    public synchronized void offer(long[] rowBits) {
        offer(rowBits, (byte) 0, 0L);
    }

    public synchronized void offer(long[] rowBits, byte state, long rowVersion) {
        int row = store.appendRow(rowBits, state, rowVersion);
        siftUp(row);
    }

    public synchronized long peekBits(int lane) {
        if (store.isEmpty()) throw new NoSuchElementException();
        return store.getBits(lane, 0);
    }

    public synchronized byte peekState() {
        if (store.isEmpty()) throw new NoSuchElementException();
        return store.stateAt(0);
    }

    public synchronized int peekRowId() {
        if (store.isEmpty()) throw new NoSuchElementException();
        return store.rowIdAt(0);
    }

    public synchronized long peekRowVersion() {
        if (store.isEmpty()) throw new NoSuchElementException();
        return store.rowVersionAt(0);
    }

    /**
     * Copies the root row into caller-owned storage and removes it.
     * Metadata lanes move with the heap row during all internal swaps.
     */
    public synchronized void pollInto(long[] target) {
        Objects.requireNonNull(target, "target");
        if (store.isEmpty()) throw new NoSuchElementException();
        store.readRow(0, target);
        int last = store.size() - 1;
        if (last == 0) {
            store.removeAt(0);
            return;
        }
        store.swapRows(0, last);
        store.removeAt(last);
        siftDown(0);
    }

    public synchronized M3PrimitiveSoaStore snapshot() {
        int size = store.size();
        int[] rows = new int[size];
        for (int i = 0; i < size; i++) rows[i] = i;
        return store.copyRows(rows);
    }

    public synchronized void clear() {
        store.clear();
    }

    private void siftUp(int row) {
        int index = row;
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (!before(index, parent)) break;
            store.swapRows(index, parent);
            index = parent;
        }
    }

    private void siftDown(int row) {
        int size = store.size();
        int index = row;
        int half = size >>> 1;
        while (index < half) {
            int child = (index << 1) + 1;
            int right = child + 1;
            if (right < size && before(right, child)) child = right;
            if (!before(child, index)) break;
            store.swapRows(index, child);
            index = child;
        }
    }

    private boolean before(int leftRow, int rightRow) {
        int comparison = priorityKind.compareBits(
                store.getBits(priorityLane, leftRow),
                store.getBits(priorityLane, rightRow));
        return minFirst ? comparison < 0 : comparison > 0;
    }
}
