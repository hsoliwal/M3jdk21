// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/**
 * Entry points for lazily segmented, stable-ID primitive storage.
 *
 * <p>This factory does not choose collection semantics. It exposes the storage substrate used by
 * lean lists, arenas, graphs, trees and index rows when sparse growth/stable logical addresses are
 * required. Dense packed collections remain the better default for small dense workloads.
 */
public final class M3Collections {
    private M3Collections() {}

    public static M3LongLane28 longLane() {
        return new M3LongLane28();
    }

    public static M3IntLane28 intLane() {
        return new M3IntLane28();
    }

    public static M3BitLane28 bitLane() {
        return new M3BitLane28();
    }

    public static M3SlotAllocator28 stableSlots() {
        return new M3SlotAllocator28();
    }

    public static M3LongLaneStore28 longRows(int laneCount) {
        return new M3LongLaneStore28(laneCount);
    }

    public static M3LongArena28 longArena() {
        return new M3LongArena28();
    }

    public static M3LongLinkedList28 longLinkedList() {
        return new M3LongLinkedList28();
    }
    /** Concurrent primitive values or metadata, with explicit per-slot atomic operations. */
    public static M3ConcurrentLongLane28 concurrentLongLane() { return new M3ConcurrentLongLane28(); }

    /** Concurrent int metadata; allocating pages is separate from prepared-page operations. */
    public static M3ConcurrentIntLane28 concurrentIntLane() { return new M3ConcurrentIntLane28(); }
}
