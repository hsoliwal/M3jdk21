/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Compact struct-of-arrays table with stable 64-bit generational handles.
 *
 * <p>Rows may move through swap-removal while four primitive index lanes keep
 * handle identity aligned: slot->row, row->slot, generation and free-list next.
 * No handle or row object is retained.
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
        int row = rows.size();
        ensureRowCapacity(row + 1);
        int slot = allocateSlot();
        rows.appendRow(rowBits, state, rowVersion);
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

    private int allocateSlot() {
        if (freeHead >= 0) {
            int slot = freeHead;
            freeHead = nextFree[slot];
            nextFree[slot] = -1;
            return slot;
        }
        ensureSlotCapacity(slotCount + 1);
        int slot = slotCount++;
        if (generations[slot] == 0) generations[slot] = 1;
        return slot;
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

    private void ensureRowCapacity(int required) {
        if (required <= rowToSlot.length) return;
        int old = rowToSlot.length;
        int next = grow(old, required);
        rowToSlot = Arrays.copyOf(rowToSlot, next);
        Arrays.fill(rowToSlot, old, next, -1);
    }

    private void ensureSlotCapacity(int required) {
        if (required <= slotToRow.length) return;
        int old = slotToRow.length;
        int next = grow(old, required);
        slotToRow = Arrays.copyOf(slotToRow, next);
        generations = Arrays.copyOf(generations, next);
        nextFree = Arrays.copyOf(nextFree, next);
        Arrays.fill(slotToRow, old, next, -1);
        Arrays.fill(nextFree, old, next, -1);
    }

    private static int grow(int current, int required) {
        int next = current + (current >>> 1) + 1;
        return Math.max(next, required);
    }

    private static long handle(int slot, int generation) {
        return (Integer.toUnsignedLong(generation) << 32) | Integer.toUnsignedLong(slot);
    }

    private static int slot(long handle) { return (int) handle; }
    private static int generation(long handle) { return (int) (handle >>> 32); }
}
