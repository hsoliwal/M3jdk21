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
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import jdk.m3.collections.PrimitiveKind;
import jdk.m3.collections.PrimitiveSoaStore;

import java.util.Arrays;
import java.util.Objects;

/**
 * Compact struct-of-arrays table with stable 64-bit generational handles.
 *
 * <p>Rows may move through swap-removal while four primitive index lanes keep
 * handle identity aligned: slot->row, row->slot, generation and free-list next.
 * No handle or row object is retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveHandleTable.java}
 * blob {@code 6547278cfc1fdf02d458d724e6dd27a70a515e93} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (primitives DenseCollections chain after API_UNION U7/U8 (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PrimitiveHandleTable {
    private final PrimitiveSoaStore rows;
    private int[] slotToRow;
    private int[] rowToSlot;
    private int[] generations;
    private int[] nextFree;
    private int slotCount;
    private int freeHead = -1;

    public PrimitiveHandleTable(
            int initialCapacity, int rowMetadataMask, PrimitiveKind... laneKinds) {
        Objects.requireNonNull(laneKinds, "laneKinds");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        int capacity = Math.max(1, initialCapacity);
        rows = new PrimitiveSoaStore(capacity, rowMetadataMask, laneKinds);
        slotToRow = new int[capacity];
        rowToSlot = new int[capacity];
        generations = new int[capacity];
        nextFree = new int[capacity];
        Arrays.fill(slotToRow, -1);
        Arrays.fill(rowToSlot, -1);
        Arrays.fill(nextFree, -1);
    }

    public synchronized int size() { return rows.size(); }
    public synchronized boolean isEmpty() { return rows.isEmpty(); }
    public int laneCount() { return rows.laneCount(); }
    public PrimitiveKind laneKind(int lane) { return rows.laneKind(lane); }

    public synchronized long payloadBytes() {
        return rows.payloadBytes()
                + (long) slotToRow.length * Integer.BYTES
                + (long) rowToSlot.length * Integer.BYTES
                + (long) generations.length * Integer.BYTES
                + (long) nextFree.length * Integer.BYTES;
    }

    public synchronized long add(long[] rowBits) {
        return add(rowBits, (byte) 0, 0L);
    }

    public synchronized long add(long[] rowBits, byte state, long rowVersion) {
        Objects.requireNonNull(rowBits, "rowBits");
        if (rowBits.length != rows.laneCount()) {
            throw new IllegalArgumentException("lane count mismatch");
        }
        int row = rows.size();
        int requiredRows = PrimitiveKind.checkedSize(row, 1);
        boolean reuseSlot = freeHead >= 0;
        int slot = reuseSlot ? freeHead : slotCount;
        int requiredSlots = reuseSlot ? slotCount : PrimitiveKind.checkedSize(slotCount, 1);

        // Stage every index allocation before the row store commits. A rejected row or
        // allocation failure must not consume a free slot or publish partial metadata.
        int[] nextRows = rowToSlot;
        if (requiredRows > rowToSlot.length) {
            int next = PrimitiveKind.grownCapacity(rowToSlot.length, requiredRows);
            nextRows = Arrays.copyOf(rowToSlot, next);
            Arrays.fill(nextRows, rowToSlot.length, next, -1);
        }
        int[] nextSlots = slotToRow;
        int[] nextGenerations = generations;
        int[] nextLinks = nextFree;
        if (requiredSlots > slotToRow.length) {
            int next = PrimitiveKind.grownCapacity(slotToRow.length, requiredSlots);
            nextSlots = Arrays.copyOf(slotToRow, next);
            nextGenerations = Arrays.copyOf(generations, next);
            nextLinks = Arrays.copyOf(nextFree, next);
            Arrays.fill(nextSlots, slotToRow.length, next, -1);
            Arrays.fill(nextLinks, nextFree.length, next, -1);
        }

        // PrimitiveSoaStore validates and stages its own lane growth before publication.
        // After it succeeds, only nonthrowing assignments remain under this monitor.
        rows.appendRow(rowBits, state, rowVersion);
        rowToSlot = nextRows;
        slotToRow = nextSlots;
        generations = nextGenerations;
        nextFree = nextLinks;
        if (reuseSlot) {
            freeHead = nextFree[slot];
            nextFree[slot] = -1;
        } else {
            slotCount = requiredSlots;
            if (generations[slot] == 0) generations[slot] = 1;
        }
        rowToSlot[row] = slot;
        slotToRow[slot] = row;
        return handle(slot, generations[slot]);
    }

    public synchronized boolean contains(long handle) {
        return resolveRow(handle) >= 0;
    }

    public synchronized long getBits(long handle, int lane) {
        int row = requireRow(handle);
        return rows.getBits(lane, row);
    }

    public synchronized void setBits(long handle, int lane, long bits) {
        int row = requireRow(handle);
        rows.setBits(lane, row, bits);
    }

    public synchronized byte state(long handle) {
        return rows.stateAt(requireRow(handle));
    }

    public synchronized void state(long handle, byte state) {
        rows.stateAt(requireRow(handle), state);
    }

    public synchronized long rowVersion(long handle) {
        return rows.rowVersionAt(requireRow(handle));
    }

    public synchronized void rowVersion(long handle, long version) {
        rows.rowVersionAt(requireRow(handle), version);
    }

    /**
     * Removes in O(1) by moving the final complete SoA row into the hole and
     * repairing the two primitive index lanes.
     */
    public synchronized boolean remove(long handle) {
        int row = resolveRow(handle);
        if (row < 0) return false;
        int slot = slot(handle);
        int last = rows.size() - 1;
        if (row != last) {
            int movedSlot = rowToSlot[last];
            rows.removeSwap(row);
            rowToSlot[row] = movedSlot;
            slotToRow[movedSlot] = row;
        } else {
            rows.removeSwap(row);
        }
        rowToSlot[last] = -1;
        releaseSlot(slot);
        return true;
    }

    public synchronized PrimitiveSoaStore snapshotRows() {
        int size = rows.size();
        int[] indexes = new int[size];
        for (int i = 0; i < size; i++) indexes[i] = i;
        return rows.copyRows(indexes);
    }

    public synchronized void clear() {
        rows.clear();
        Arrays.fill(slotToRow, -1);
        Arrays.fill(rowToSlot, -1);
        Arrays.fill(nextFree, -1);
        for (int slot = 0; slot < slotCount; slot++) bumpGeneration(slot);
        freeHead = -1;
        for (int slot = slotCount - 1; slot >= 0; slot--) {
            nextFree[slot] = freeHead;
            freeHead = slot;
        }
    }

    private void releaseSlot(int slot) {
        slotToRow[slot] = -1;
        bumpGeneration(slot);
        nextFree[slot] = freeHead;
        freeHead = slot;
    }

    private void bumpGeneration(int slot) {
        int next = generations[slot] + 1;
        generations[slot] = next == 0 ? 1 : next;
    }

    private int resolveRow(long handle) {
        int slot = slot(handle);
        if (slot < 0 || slot >= slotCount) return -1;
        if (generation(handle) != generations[slot]) return -1;
        return slotToRow[slot];
    }

    private int requireRow(long handle) {
        int row = resolveRow(handle);
        if (row < 0) throw new IllegalArgumentException("stale or invalid handle");
        return row;
    }

    private static long handle(int slot, int generation) {
        return (Integer.toUnsignedLong(generation) << 32) | Integer.toUnsignedLong(slot);
    }

    private static int slot(long handle) { return (int) handle; }
    private static int generation(long handle) { return (int) (handle >>> 32); }
}
