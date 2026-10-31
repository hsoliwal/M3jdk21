// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;

/**
 * Stable-row multi-lane long store over lazily allocated segmented blocks.
 *
 * <p>Unlike dense swap-moving row stores, rows never swap-move. A live row is addressed by a
 * generation-safe 64-bit handle. Every data lane uses the same logical slot mapping.
 */
public final class M3LongLaneStore28 {
    private final M3SlotAllocator28 allocator = new M3SlotAllocator28();
    private final M3LongLane28[] lanes;

    public M3LongLaneStore28(int laneCount) {
        if (laneCount <= 0) {
            throw new IllegalArgumentException("laneCount");
        }
        lanes = new M3LongLane28[laneCount];
        for (int lane = 0; lane < laneCount; lane++) {
            lanes[lane] = new M3LongLane28();
        }
    }

    public int laneCount() {
        return lanes.length;
    }

    public int size() {
        return allocator.size();
    }

    public boolean isEmpty() {
        return allocator.isEmpty();
    }

    public long allocate() {
        return allocator.allocate();
    }

    public long allocate1(long first) {
        requireLaneCount(1);
        long handle = allocator.allocate();
        lanes[0].set(M3SlotAllocator28.slot(handle), first);
        return handle;
    }

    public long allocate2(long first, long second) {
        requireLaneCount(2);
        long handle = allocator.allocate();
        int slot = M3SlotAllocator28.slot(handle);
        lanes[0].set(slot, first);
        lanes[1].set(slot, second);
        return handle;
    }

    public long allocate3(long first, long second, long third) {
        requireLaneCount(3);
        long handle = allocator.allocate();
        int slot = M3SlotAllocator28.slot(handle);
        lanes[0].set(slot, first);
        lanes[1].set(slot, second);
        lanes[2].set(slot, third);
        return handle;
    }

    public long get(long handle, int lane) {
        checkLane(lane);
        return lanes[lane].get(allocator.requireLiveSlot(handle));
    }

    public void set(long handle, int lane, long value) {
        checkLane(lane);
        lanes[lane].set(allocator.requireLiveSlot(handle), value);
    }

    public void release(long handle) {
        int slot = allocator.requireLiveSlot(handle);
        for (M3LongLane28 lane : lanes) {
            lane.clear(slot);
        }
        allocator.release(handle);
    }

    public boolean isLive(long handle) {
        return allocator.isLive(handle);
    }

    public int highWaterMark() {
        return allocator.highWaterMark();
    }

    public int allocatedDataBlockCount() {
        int blocks = 0;
        for (M3LongLane28 lane : lanes) {
            blocks = Math.addExact(blocks, lane.allocatedBlockCount());
        }
        return blocks;
    }

    public int allocatedAllocatorBlockCount() {
        return allocator.allocatedBlockCount();
    }

    public long payloadBytes() {
        long bytes = allocator.payloadBytes();
        for (M3LongLane28 lane : lanes) {
            bytes = Math.addExact(bytes, lane.payloadBytes());
        }
        return bytes;
    }

    public void forEachLive(HandleConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int slot = allocator.nextLiveSlot(0); slot >= 0;
                slot = allocator.nextLiveSlot(slot + 1)) {
            consumer.accept(allocator.handleForLiveSlot(slot));
        }
    }

    /** Copies one lane of live rows in ascending slot order into caller-owned storage. */
    public int copyLiveLane(int lane, long[] output, int outputFrom) {
        checkLane(lane);
        Objects.requireNonNull(output, "output");
        Objects.checkFromIndexSize(outputFrom, size(), output.length);
        int start = outputFrom;
        for (int slot = allocator.nextLiveSlot(0); slot >= 0;
                slot = allocator.nextLiveSlot(slot + 1)) {
            output[outputFrom++] = lanes[lane].get(slot);
        }
        return outputFrom - start;
    }

    private void checkLane(int lane) {
        Objects.checkIndex(lane, lanes.length);
    }

    private void requireLaneCount(int expected) {
        if (lanes.length != expected) {
            throw new IllegalStateException(
                    "operation requires " + expected + " lanes, store has " + lanes.length);
        }
    }

    @FunctionalInterface
    public interface HandleConsumer {
        void accept(long handle);
    }
}
