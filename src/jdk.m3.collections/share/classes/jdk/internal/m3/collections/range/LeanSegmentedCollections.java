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

import jdk.m3.collections.BitLane28;
import jdk.m3.collections.ConcurrentIntLane28;
import jdk.m3.collections.ConcurrentLongLane28;

/**
 * Entry points for lazily segmented, stable-ID primitive storage.
 *
 * <p>This factory does not choose collection semantics. It exposes the storage substrate used by
 * lean lists, arenas, graphs, trees and index rows when sparse growth/stable logical addresses are
 * required. Dense packed collections remain the better default for small dense workloads.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/LeanSegmentedCollections.java}
 * blob {@code edc5883d81bb666fb542b4bc2606646871d2bfe0} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections disjoint-set + segmented lane batch after API_UNION U4 (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LeanSegmentedCollections {
    private LeanSegmentedCollections() {}

    public static SegmentedLongLane28 longLane() {
        return new SegmentedLongLane28();
    }

    public static SegmentedIntLane28 intLane() {
        return new SegmentedIntLane28();
    }

    public static SegmentedShortLane28 shortLane() {
        return new SegmentedShortLane28();
    }

    public static BitLane28 bitLane() {
        return new BitLane28();
    }

    public static SegmentedSlotAllocator28 stableSlots() {
        return new SegmentedSlotAllocator28();
    }

    public static SegmentedLongLaneStore28 longRows(int laneCount) {
        return new SegmentedLongLaneStore28(laneCount);
    }

    public static SegmentedLongArena28 longArena() {
        return new SegmentedLongArena28();
    }

    public static SegmentedLongLinkedList28 longLinkedList() {
        return new SegmentedLongLinkedList28();
    }
    /** Concurrent primitive values or metadata, with explicit per-slot atomic operations. */
    public static ConcurrentLongLane28 concurrentLongLane() { return new ConcurrentLongLane28(); }

    /** Concurrent int metadata; allocating pages is separate from prepared-page operations. */
    public static ConcurrentIntLane28 concurrentIntLane() { return new ConcurrentIntLane28(); }
}
