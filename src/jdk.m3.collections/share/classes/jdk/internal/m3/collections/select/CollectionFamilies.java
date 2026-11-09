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
package jdk.internal.m3.collections.select;

import jdk.internal.m3.collections.IntLongSparseMap;
import jdk.internal.m3.collections.LongByteMap;
import jdk.internal.m3.collections.LongLinkedSequence;
import jdk.internal.m3.collections.LongLongOrderedMap;
import jdk.internal.m3.collections.LongOrderedSet;
import jdk.internal.m3.collections.LongPairs;
import jdk.internal.m3.collections.PackedLongLongBiMap;
import jdk.internal.m3.collections.PackedLongLongOrderedMap;
import jdk.internal.m3.collections.PackedLongLongTable;
import jdk.m3.collections.BitLane28;
import jdk.internal.m3.collections.packed.PackedLongBagContract;
import jdk.internal.m3.collections.packed.PackedLongLongMap;
import jdk.internal.m3.collections.packed.PackedMutableLongSet;
import jdk.internal.m3.collections.packed.UnsignedByteSequence;
import jdk.internal.m3.collections.range.PackedLongRangeMap;
import jdk.internal.m3.collections.range.PackedLongRangeSet;
import jdk.internal.m3.collections.range.SegmentedIntLane28;
import jdk.internal.m3.collections.range.SegmentedLongArena28;
import jdk.internal.m3.collections.range.SegmentedLongLane28;
import jdk.internal.m3.collections.range.SegmentedLongLinkedList28;
import jdk.m3.collections.LongDeque;
import jdk.m3.collections.LongLongSortedMap;
import jdk.m3.collections.LongPriorityQueue;
import jdk.m3.collections.LongQueue;
import jdk.m3.collections.LongSequence;
import jdk.m3.collections.LongSortedSet;
import jdk.m3.collections.LongStack;
import jdk.m3.collections.bits.LongBitSet;

/**
 * Contracts stay distinct when their semantics differ, even if their storage is similar.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/CollectionFamilies.java}
 * blob {@code 81270c9b8f6fdff56ad6b555d80fdbce4d32099c} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections catalogue chain after API_UNION U3 (v6.2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CollectionFamilies {
    public static final CollectionFamily<PackedLongLongMap> LONG_MAP =
            new CollectionFamily<>("long-map", PackedLongLongMap.class);
    public static final CollectionFamily<PackedMutableLongSet> LONG_SET =
            new CollectionFamily<>("long-set", PackedMutableLongSet.class);
    public static final CollectionFamily<LongLongOrderedMap> ORDERED_LONG_MAP =
            new CollectionFamily<>("ordered-long-map", LongLongOrderedMap.class);
    /** Explicit opt-in: reads can reorder entries, unlike the insertion-order interface contract. */
    public static final CollectionFamily<PackedLongLongOrderedMap> ACCESS_ORDERED_LONG_MAP =
            new CollectionFamily<>("access-ordered-long-map", PackedLongLongOrderedMap.class);
    public static final CollectionFamily<LongLongSortedMap> SORTED_LONG_MAP =
            new CollectionFamily<>("sorted-long-map", LongLongSortedMap.class);
    public static final CollectionFamily<LongSortedSet> SORTED_LONG_SET =
            new CollectionFamily<>("sorted-long-set", LongSortedSet.class);
    public static final CollectionFamily<LongSequence> LONG_LIST =
            new CollectionFamily<>("long-list", LongSequence.class);
    public static final CollectionFamily<LongDeque> LONG_DEQUE =
            new CollectionFamily<>("long-deque", LongDeque.class);
    public static final CollectionFamily<LongPriorityQueue> LONG_PRIORITY_QUEUE =
            new CollectionFamily<>("long-priority-queue", LongPriorityQueue.class);
    public static final CollectionFamily<LongLinkedSequence> LONG_LINKED_LIST =
            new CollectionFamily<>("long-linked-list", LongLinkedSequence.class);
    public static final CollectionFamily<SegmentedLongLinkedList28> LINKED_LIST_28 =
            new CollectionFamily<>("linked-list-28", SegmentedLongLinkedList28.class);
    public static final CollectionFamily<SegmentedLongArena28> LONG_ARENA_28 =
            new CollectionFamily<>("long-arena-28", SegmentedLongArena28.class);
    // Lane reads have zero/false-default semantics, not map presence semantics.
    public static final CollectionFamily<SegmentedLongLane28> LONG_LANE_28 =
            new CollectionFamily<>("long-lane-28", SegmentedLongLane28.class);
    public static final CollectionFamily<SegmentedIntLane28> INT_LANE_28 =
            new CollectionFamily<>("int-lane-28", SegmentedIntLane28.class);
    public static final CollectionFamily<BitLane28> BIT_LANE_28 =
            new CollectionFamily<>("bit-lane-28", BitLane28.class);

    public static final CollectionFamily<PackedLongBagContract> LONG_BAG = new CollectionFamily<>("long-bag", PackedLongBagContract.class);
    public static final CollectionFamily<LongBitSet> BIT_SET = new CollectionFamily<>("bit-set", LongBitSet.class);
    public static final CollectionFamily<LongByteMap> LONG_BYTE_MAP = new CollectionFamily<>("long-byte-map", LongByteMap.class);
    public static final CollectionFamily<LongOrderedSet> ORDERED_LONG_SET =
            new CollectionFamily<>("ordered-long-set", LongOrderedSet.class);
    public static final CollectionFamily<IntLongSparseMap> INT_LONG_SPARSE_MAP =
            new CollectionFamily<>("int-long-sparse-map", IntLongSparseMap.class);
    public static final CollectionFamily<LongPairs> LONG_PAIRS = new CollectionFamily<>("long-pairs", LongPairs.class);
    public static final CollectionFamily<LongStack> LONG_STACK = new CollectionFamily<>("long-stack", LongStack.class);
    public static final CollectionFamily<LongQueue> LONG_FIFO = new CollectionFamily<>("long-fifo", LongQueue.class);
    public static final CollectionFamily<PackedLongLongBiMap> LONG_BIMAP =
            new CollectionFamily<>("long-bimap", PackedLongLongBiMap.class);
    public static final CollectionFamily<PackedLongLongTable> LONG_TABLE =
            new CollectionFamily<>("long-table", PackedLongLongTable.class);
    public static final CollectionFamily<PackedLongRangeMap> LONG_RANGE_MAP =
            new CollectionFamily<>("long-range-map", PackedLongRangeMap.class);
    public static final CollectionFamily<PackedLongRangeSet> LONG_RANGE_SET =
            new CollectionFamily<>("long-range-set", PackedLongRangeSet.class);
    public static final CollectionFamily<UnsignedByteSequence> UNSIGNED_BYTES =
            new CollectionFamily<>("unsigned-bytes", UnsignedByteSequence.class);

    private CollectionFamilies() {}
}
