// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SegmentedPrimitiveStorageTest {
    @Test
    void addressRoundTripsAllBoundaries() {
        int[] slots = {
            0,
            1,
            4095,
            4096,
            (1 << 20) - 1,
            1 << 20,
            M3Address28.MAX_SLOT
        };
        for (int slot : slots) {
            int region = M3Address28.region(slot);
            int block = M3Address28.block(slot);
            int offset = M3Address28.offset(slot);
            assertEquals(slot, M3Address28.compose(region, block, offset));
        }
        assertThrows(IndexOutOfBoundsException.class,
            () -> M3Address28.region(-1));
        assertThrows(IndexOutOfBoundsException.class,
            () -> M3Address28.offset(M3Address28.MAX_SLOTS));
    }

    @Test
    void longIntAndBitLanesAllocateOnlyTouchedPaths() {
        M3LongLane28 longs = new M3LongLane28();
        M3IntLane28 ints = new M3IntLane28();
        M3BitLane28 bits = new M3BitLane28();

        int[] slots = {0, 4096, 1 << 20, M3Address28.MAX_SLOT};
        for (int i = 0; i < slots.length; i++) {
            longs.set(slots[i], 100L + i);
            ints.set(slots[i], 200 + i);
            bits.set(slots[i]);
        }

        assertEquals(slots.length, longs.allocatedBlockCount());
        assertEquals(slots.length, ints.allocatedBlockCount());
        assertEquals(slots.length, bits.allocatedBlockCount());
        assertEquals(3, longs.allocatedRegionCount());
        assertEquals(3, ints.allocatedRegionCount());
        assertEquals(3, bits.allocatedRegionCount());

        for (int i = 0; i < slots.length; i++) {
            assertEquals(100L + i, longs.get(slots[i]));
            assertEquals(200 + i, ints.get(slots[i]));
            assertTrue(bits.get(slots[i]));
        }
        assertEquals(0L, longs.get(1));
        assertEquals(0, ints.get(1));
        assertFalse(bits.get(1));

        longs.clear(0);
        ints.clear(0);
        bits.clear(0);
        assertEquals(0L, longs.get(0));
        assertEquals(0, ints.get(0));
        assertFalse(bits.get(0));
        assertEquals(3, bits.cardinality());
    }

    @Test
    void allocatorRejectsStaleHandlesAfterReuse() {
        M3SlotAllocator28 allocator = new M3SlotAllocator28();
        long first = allocator.allocate();
        int slot = M3SlotAllocator28.slot(first);
        int generation = M3SlotAllocator28.generation(first);
        assertTrue(allocator.isLive(first));

        allocator.release(first);
        assertFalse(allocator.isLive(first));
        assertThrows(IllegalStateException.class,
            () -> allocator.requireLiveSlot(first));

        long second = allocator.allocate();
        assertEquals(slot, M3SlotAllocator28.slot(second));
        assertEquals(generation + 1, M3SlotAllocator28.generation(second));
        assertNotEquals(first, second);
        assertTrue(allocator.isLive(second));
        assertEquals(1, allocator.highWaterMark());
        assertEquals(1, allocator.size());
    }

    @Test
    void segmentedArenaCrossesLeafBoundaryWithoutCopyingExistingOffsets() {
        M3LongArena28 arena = new M3LongArena28();
        for (int i = 0; i < M3Address28.VALUES_PER_BLOCK + 17; i++) {
            assertEquals(i, arena.append(i * 3L));
        }

        assertEquals(2, arena.allocatedBlockCount());
        assertEquals(0L, arena.get(0));
        assertEquals(4095L * 3L, arena.get(4095));
        assertEquals(4096L * 3L, arena.get(4096));
        assertArrayEquals(
            new long[] {4094L * 3L, 4095L * 3L, 4096L * 3L, 4097L * 3L},
            arena.copyRange(4094, 4098));
    }

    @Test
    void linkedListKeepsOrderAcrossStableSlotReuse() {
        M3LongLinkedList28 list = new M3LongLinkedList28();
        long ten = list.addLast(10);
        long thirty = list.addLast(30);
        long twenty = list.insertBefore(thirty, 20);
        long five = list.addFirst(5);

        assertArrayEquals(new long[] {5, 10, 20, 30}, list.toArray());
        assertEquals(five, list.firstHandle());
        assertEquals(thirty, list.lastHandle());
        assertEquals(twenty, list.nextHandle(ten));
        assertEquals(ten, list.previousHandle(twenty));

        assertEquals(20, list.remove(twenty));
        assertFalse(list.isLive(twenty));
        assertArrayEquals(new long[] {5, 10, 30}, list.toArray());

        long fifteen = list.insertAfter(ten, 15);
        assertArrayEquals(new long[] {5, 10, 15, 30}, list.toArray());
        assertEquals(
            M3SlotAllocator28.slot(twenty),
            M3SlotAllocator28.slot(fifteen));
        assertNotEquals(twenty, fifteen);
        assertThrows(IllegalStateException.class, () -> list.value(twenty));

        assertEquals(5, list.removeFirstLong());
        assertEquals(30, list.removeLastLong());
        assertArrayEquals(new long[] {10, 15}, list.toArray());

        List<Long> visited = new ArrayList<>();
        list.forEachLong(visited::add);
        assertEquals(List.of(10L, 15L), visited);
        list.clear();
        assertTrue(list.isEmpty());
        assertThrows(java.util.NoSuchElementException.class, list::firstHandle);
    }

    @Test
    void leanFactoryExposesStorageWithoutChoosingSemantics() {
        assertNotNull(M3Collections.longLane());
        assertNotNull(M3Collections.intLane());
        assertNotNull(M3Collections.bitLane());
        assertNotNull(M3Collections.stableSlots());
        assertEquals(4, M3Collections.longRows(4).laneCount());
        assertTrue(M3Collections.longArena().isEmpty());
        assertTrue(M3Collections.longLinkedList().isEmpty());
    }

    @Test
    void multiLaneStoreDoesNotMoveOtherHandlesOnRemoval() {
        M3LongLaneStore28 store = new M3LongLaneStore28(2);
        long a = store.allocate2(1, 11);
        long b = store.allocate2(2, 22);
        long c = store.allocate2(3, 33);

        store.release(b);
        assertTrue(store.isLive(a));
        assertTrue(store.isLive(c));
        assertEquals(1, store.get(a, 0));
        assertEquals(11, store.get(a, 1));
        assertEquals(3, store.get(c, 0));
        assertEquals(33, store.get(c, 1));

        long reused = store.allocate2(4, 44);
        assertEquals(
            M3SlotAllocator28.slot(b),
            M3SlotAllocator28.slot(reused));
        assertFalse(store.isLive(b));
        assertEquals(4, store.get(reused, 0));
        assertEquals(44, store.get(reused, 1));
    }
}
