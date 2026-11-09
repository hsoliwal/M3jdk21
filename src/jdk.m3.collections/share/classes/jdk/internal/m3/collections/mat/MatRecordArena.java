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
package jdk.internal.m3.collections.mat;

import java.util.Arrays;
import java.util.Objects;

/**
 * Mutable single-owner structure-of-arrays arena with generational stable handles.
 *
 * <p>Handle zero is NULL. Slot reuse increments the generation so a stale handle cannot alias a
 * newly allocated record. Live record fields stay in one lane-major primitive array.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatRecordArena.java}
 * blob {@code f0f525599a9d5d5d761c9ee81ecec8bb821cd481} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatRecordArena {
    private static final int DEFAULT_CAPACITY = 16;

    private final int lanes;
    private int capacity;
    private int size;
    private int highWater;
    private int freeHead = -1;
    private long[] data;
    private int[] generations;
    private int[] freeNext;
    private long[] occupied;

    public MatRecordArena(int lanes) {
        this(lanes, DEFAULT_CAPACITY);
    }

    public MatRecordArena(int lanes, int initialCapacity) {
        if (lanes < 1 || lanes > 1_024) {
            throw new IllegalArgumentException("lanes must be in [1,1024]");
        }
        if (initialCapacity < 1 || initialCapacity > maximumCapacity(lanes)) {
            throw new IllegalArgumentException("invalid initial capacity");
        }
        this.lanes = lanes;
        this.capacity = initialCapacity;
        this.data = new long[Math.multiplyExact(lanes, initialCapacity)];
        this.generations = new int[initialCapacity];
        this.freeNext = new int[initialCapacity];
        Arrays.fill(freeNext, -1);
        this.occupied = new long[(initialCapacity + 63) >>> 6];
    }

    public int lanes() { return lanes; }
    public int size() { return size; }
    public int capacity() { return capacity; }
    public boolean isEmpty() { return size == 0; }

    public long allocate() {
        int slot;
        if (freeHead >= 0) {
            slot = freeHead;
            freeHead = freeNext[slot];
            freeNext[slot] = -1;
        } else {
            if (highWater == capacity) grow();
            slot = highWater++;
            generations[slot] = 1;
        }
        setOccupied(slot, true);
        size = Math.addExact(size, 1);
        return MatPointerHandle.of(slot, generations[slot]);
    }

    public void free(long handle) {
        int slot = requireSlot(handle);
        if (generations[slot] == -1) {
            throw new IllegalStateException("arena live generation is already exhausted");
        }
        for (int lane = 0; lane < lanes; lane++) {
            data[lane * capacity + slot] = 0L;
        }
        setOccupied(slot, false);
        size--;

        // -1 is the retired sentinel. Retire before 32-bit generation wrap can reach zero.
        if (generations[slot] == -2) {
            generations[slot] = -1;
            freeNext[slot] = -1;
            return;
        }
        generations[slot]++;
        freeNext[slot] = freeHead;
        freeHead = slot;
    }

    public boolean isAlive(long handle) {
        if (handle == MatPointerHandle.NULL) return false;
        long encoded = handle & 0xffff_ffffL;
        if (encoded == 0L || encoded > highWater) return false;
        int slot = (int) encoded - 1;
        return slot >= 0
                && slot < highWater
                && occupied(slot)
                && generations[slot] == (int) (handle >>> 32)
                && generations[slot] != 0;
    }

    public long get(long handle, int lane) {
        int slot = requireSlot(handle);
        Objects.checkIndex(lane, lanes);
        return data[lane * capacity + slot];
    }

    public void set(long handle, int lane, long value) {
        int slot = requireSlot(handle);
        Objects.checkIndex(lane, lanes);
        data[lane * capacity + slot] = value;
    }

    public void clear(long handle) {
        int slot = requireSlot(handle);
        for (int lane = 0; lane < lanes; lane++) {
            data[lane * capacity + slot] = 0L;
        }
    }

    public long[] liveHandles() {
        long[] handles = new long[size];
        int row = 0;
        for (int slot = 0; slot < highWater; slot++) {
            if (occupied(slot)) {
                handles[row++] = MatPointerHandle.of(slot, generations[slot]);
            }
        }
        if (row != handles.length) throw new IllegalStateException("arena liveness drift");
        return handles;
    }

    public MatRecordSnapshot snapshot() {
        long[] handles = liveHandles();
        long[] values = new long[Math.multiplyExact(lanes, handles.length)];
        for (int row = 0; row < handles.length; row++) {
            int slot = MatPointerHandle.slot(handles[row]);
            for (int lane = 0; lane < lanes; lane++) {
                values[lane * handles.length + row] = data[lane * capacity + slot];
            }
        }
        return new MatRecordSnapshot(lanes, handles, values);
    }

    public long primitivePayloadBytes() {
        return 8L * data.length
                + 4L * generations.length
                + 4L * freeNext.length
                + 8L * occupied.length;
    }

    private int requireSlot(long handle) {
        if (!isAlive(handle)) throw new IllegalStateException("stale arena handle");
        return MatPointerHandle.slot(handle);
    }

    private void grow() {
        int maximum = maximumCapacity(lanes);
        if (capacity == maximum) throw new IllegalStateException("arena capacity exhausted");
        int next = (int) Math.min((long) maximum, Math.max((long) capacity + 1L, capacity * 2L));

        long[] nextData = new long[Math.multiplyExact(lanes, next)];
        for (int lane = 0; lane < lanes; lane++) {
            System.arraycopy(data, lane * capacity, nextData, lane * next, highWater);
        }
        data = nextData;
        generations = Arrays.copyOf(generations, next);

        int oldCapacity = capacity;
        freeNext = Arrays.copyOf(freeNext, next);
        Arrays.fill(freeNext, oldCapacity, next, -1);
        occupied = Arrays.copyOf(occupied, (next + 63) >>> 6);
        capacity = next;
    }

    private static int maximumCapacity(int lanes) {
        return Math.min(Integer.MAX_VALUE - 8, (Integer.MAX_VALUE - 8) / lanes);
    }

    private boolean occupied(int slot) {
        return (occupied[slot >>> 6] & (1L << (slot & 63))) != 0L;
    }

    private void setOccupied(int slot, boolean value) {
        long bit = 1L << (slot & 63);
        int word = slot >>> 6;
        if (value) occupied[word] |= bit;
        else occupied[word] &= ~bit;
    }
}
