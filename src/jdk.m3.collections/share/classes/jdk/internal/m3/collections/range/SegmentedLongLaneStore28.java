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
package jdk.internal.m3.collections.range;

import java.util.Objects;

/**
 * Stable-row multi-lane long store over lazily allocated segmented blocks.
 *
 * <p>Unlike {@code PackedLongLaneStore}, rows never swap-move. A live row is addressed by a
 * generation-safe 64-bit handle. Every data lane uses the same logical slot mapping.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/SegmentedLongLaneStore28.java}
 * blob {@code 8059b275964c3e95e4644b2e21de8ed98f6577a0} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SegmentedLongLaneStore28 {
    private final SegmentedSlotAllocator28 allocator = new SegmentedSlotAllocator28();
    private final SegmentedLongLane28[] lanes;

    public SegmentedLongLaneStore28(int laneCount) {
        if (laneCount <= 0) {
            throw new IllegalArgumentException("laneCount");
        }
        lanes = new SegmentedLongLane28[laneCount];
        for (int lane = 0; lane < laneCount; lane++) {
            lanes[lane] = new SegmentedLongLane28();
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
        lanes[0].set(SegmentedSlotAllocator28.slot(handle), first);
        return handle;
    }

    public long allocate2(long first, long second) {
        requireLaneCount(2);
        long handle = allocator.allocate();
        int slot = SegmentedSlotAllocator28.slot(handle);
        lanes[0].set(slot, first);
        lanes[1].set(slot, second);
        return handle;
    }

    public long allocate3(long first, long second, long third) {
        requireLaneCount(3);
        long handle = allocator.allocate();
        int slot = SegmentedSlotAllocator28.slot(handle);
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
        for (SegmentedLongLane28 lane : lanes) {
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
        for (SegmentedLongLane28 lane : lanes) {
            blocks = Math.addExact(blocks, lane.allocatedBlockCount());
        }
        return blocks;
    }

    public int allocatedAllocatorBlockCount() {
        return allocator.allocatedBlockCount();
    }

    public long payloadBytes() {
        long bytes = allocator.payloadBytes();
        for (SegmentedLongLane28 lane : lanes) {
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
