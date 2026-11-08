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
package jdk.internal.m3.collections.range;

import jdk.internal.m3.collections.bits.SegmentedBitLane28;

/**
 * Stable slot allocator over the 28-bit segmented address space.
 *
 * <p>Handles pack a 32-bit generation in the high half and the 28-bit logical slot in the low
 * half. Reusing a slot increments its generation, so stale handles fail closed.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/SegmentedSlotAllocator28.java}
 * blob {@code 7f94f20d1abea960f3608da5021f025090ece9ec} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SegmentedSlotAllocator28 {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public SegmentedSlotAllocator28() {
    }

    private final SegmentedIntLane28 generations = new SegmentedIntLane28();
    private final SegmentedIntLane28 freeNextEncoded = new SegmentedIntLane28();
    private final SegmentedBitLane28 live = new SegmentedBitLane28();

    private int highWater;
    private int freeHeadEncoded;
    private int size;

    public long allocate() {
        final int slot;
        if (freeHeadEncoded != 0) {
            slot = freeHeadEncoded - 1;
            freeHeadEncoded = freeNextEncoded.get(slot);
            freeNextEncoded.clear(slot);
        } else {
            if (highWater == SegmentedAddress28.MAX_SLOTS) {
                throw new IllegalStateException("segmented slot space exhausted");
            }
            slot = highWater++;
            generations.set(slot, 1);
        }

        int generation = generations.get(slot);
        if (generation == 0) {
            throw new IllegalStateException("zero slot generation");
        }
        live.set(slot);
        size++;
        return handle(slot, generation);
    }

    public void release(long handle) {
        int slot = requireLiveSlot(handle);
        int generation = generations.get(slot);
        if (generation == -1) {
            throw new IllegalStateException(
                    "slot generation exhausted; refusing stale-handle wrap");
        }

        live.clear(slot);
        generations.set(slot, generation + 1);
        freeNextEncoded.set(slot, freeHeadEncoded);
        freeHeadEncoded = slot + 1;
        size--;
    }

    public boolean isLive(long handle) {
        int slot = slot(handle);
        if (slot < 0 || slot >= highWater || !live.get(slot)) {
            return false;
        }
        return generation(handle) == generations.get(slot);
    }

    public int requireLiveSlot(long handle) {
        int slot = slot(handle);
        if (slot < 0 || slot >= highWater) {
            throw new IllegalStateException("handle slot outside allocator: " + slot);
        }
        if (!live.get(slot)) {
            throw new IllegalStateException("handle refers to a free slot: " + slot);
        }
        if (generation(handle) != generations.get(slot)) {
            throw new IllegalStateException("stale segmented handle for slot " + slot);
        }
        return slot;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int highWaterMark() {
        return highWater;
    }

    public int freeSlotCount() {
        return highWater - size;
    }

    public int generationAtSlot(int slot) {
        if (slot < 0 || slot >= highWater) {
            throw new IndexOutOfBoundsException("slot=" + slot);
        }
        return generations.get(slot);
    }

    public boolean isSlotLive(int slot) {
        if (slot < 0 || slot >= highWater) {
            return false;
        }
        return live.get(slot);
    }

    /** Finds the next live physical slot, or -1, without scanning every free slot. */
    public int nextLiveSlot(int from) {
        SegmentedAddress28.checkRange(from, SegmentedAddress28.MAX_SLOTS);
        return from >= highWater ? -1 : live.nextSetBit(from);
    }

    public long handleForLiveSlot(int slot) {
        if (!isSlotLive(slot)) {
            throw new IllegalStateException("slot is not live: " + slot);
        }
        return handle(slot, generations.get(slot));
    }

    public long payloadBytes() {
        return generations.payloadBytes()
                + freeNextEncoded.payloadBytes()
                + live.payloadBytes();
    }

    public int allocatedBlockCount() {
        return generations.allocatedBlockCount()
                + freeNextEncoded.allocatedBlockCount()
                + live.allocatedBlockCount();
    }

    public static int slot(long handle) {
        long raw = handle & 0xffffffffL;
        if (raw >= SegmentedAddress28.MAX_SLOTS) {
            return -1;
        }
        return (int) raw;
    }

    public static int generation(long handle) {
        return (int) (handle >>> 32);
    }

    public static long handle(int slot, int generation) {
        SegmentedAddress28.checkSlot(slot);
        if (generation == 0) {
            throw new IllegalArgumentException("generation zero is reserved");
        }
        return ((long) generation << 32) | Integer.toUnsignedLong(slot);
    }
}
