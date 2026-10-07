// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/**
 * Stable slot allocator over the 28-bit segmented address space.
 *
 * <p>Handles pack a 32-bit generation in the high half and the 28-bit logical slot in the low
 * half. Reusing a slot increments its generation, so stale handles fail closed.
 */
public final class M3SlotAllocator28 {
    /** Creates an empty instance with no populated storage. */
    public M3SlotAllocator28() {}

    private final M3IntLane28 generations = new M3IntLane28();
    private final M3IntLane28 freeNextEncoded = new M3IntLane28();
    private final M3BitLane28 live = new M3BitLane28();

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
            if (highWater == M3Address28.MAX_SLOTS) {
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
        M3Address28.checkRange(from, M3Address28.MAX_SLOTS);
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
        if (raw >= M3Address28.MAX_SLOTS) {
            return -1;
        }
        return (int) raw;
    }

    public static int generation(long handle) {
        return (int) (handle >>> 32);
    }

    public static long handle(int slot, int generation) {
        M3Address28.checkSlot(slot);
        if (generation == 0) {
            throw new IllegalArgumentException("generation zero is reserved");
        }
        return ((long) generation << 32) | Integer.toUnsignedLong(slot);
    }
}
