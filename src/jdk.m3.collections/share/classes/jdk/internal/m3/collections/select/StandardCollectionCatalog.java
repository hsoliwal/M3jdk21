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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import jdk.internal.m3.collections.PackedLongLongOrderedMap;
import jdk.m3.collections.BitLane28;
import jdk.internal.m3.collections.packed.PackedLongCompactHashSet;
import jdk.internal.m3.collections.packed.PackedLongHashSet;
import jdk.internal.m3.collections.packed.PackedLongHeap;
import jdk.internal.m3.collections.packed.PackedLongInlineList;
import jdk.internal.m3.collections.packed.PackedLongLinkedList;
import jdk.internal.m3.collections.packed.PackedLongLongCompactHashMap;
import jdk.internal.m3.collections.packed.PackedLongLongHashMap;
import jdk.internal.m3.collections.packed.PackedLongLongPagedSortedMap;
import jdk.internal.m3.collections.packed.PackedLongLongQuadraticHashMap;
import jdk.internal.m3.collections.packed.PackedLongLongRobinHoodHashMap;
import jdk.internal.m3.collections.packed.PackedLongLongSortedMap;
import jdk.internal.m3.collections.packed.PackedLongLongSwissHashMap;
import jdk.internal.m3.collections.packed.PackedLongPagedSortedSet;
import jdk.internal.m3.collections.packed.PackedLongQuadraticHashSet;
import jdk.internal.m3.collections.packed.PackedLongRobinHoodHashSet;
import jdk.internal.m3.collections.packed.PackedLongSortedSet;
import jdk.internal.m3.collections.packed.PackedLongSwissHashSet;
import jdk.internal.m3.collections.range.SegmentedIntLane28;
import jdk.internal.m3.collections.range.SegmentedLongArena28;
import jdk.internal.m3.collections.range.SegmentedLongLane28;
import jdk.internal.m3.collections.range.SegmentedLongLinkedList28;
import jdk.internal.m3.collections.select.CollectionRequest.Order;
import jdk.m3.collections.LongArrayDeque;
import jdk.m3.collections.LongArrayList;

/**
 * Versioned structural heuristics. Numeric fits are not benchmark measurements.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/StandardCollectionCatalog.java}
 * blob {@code a6cfdc79762e4feecb0907847ad10221a1f9088d} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections catalogue chain after API_UNION U3 (v6.2): identifiers and package only; bytecode equal to the source modulo names).
 */
final class StandardCollectionCatalog {
    private static final String VERSION = "structural-heuristic-v1";
    private static final int ARRAY_MAX = Integer.MAX_VALUE - 8;
    private static final int TABLE_MAX = 1 << 30;
    private static final int SLOT_MAX = 1 << 28;
    private static final FuzzyCollectionFactory FACTORY = build();

    private StandardCollectionCatalog() {}
    static FuzzyCollectionFactory factory() { return FACTORY; }

    private static FuzzyCollectionFactory build() {
        List<CollectionProvider<?>> providers = new ArrayList<>();
        hash(providers, "map.compact", CollectionFamilies.LONG_MAP, 16, .75f, 2, false,
                new Fits(720, 760, 650, 780, 0, 800), r -> new PackedLongLongCompactHashMap(r.expectedSize()));
        hash(providers, "map.stateful", CollectionFamilies.LONG_MAP, 17, .65f, 2, true,
                new Fits(700, 820, 760, 720, 0, 850), r -> new PackedLongLongHashMap(r.expectedSize()));
        hash(providers, "map.quadratic", CollectionFamilies.LONG_MAP, 17, .65f, 2, false,
                new Fits(780, 750, 650, 700, 0, 750), r -> new PackedLongLongQuadraticHashMap(r.expectedSize()));
        hash(providers, "map.robin-hood", CollectionFamilies.LONG_MAP, 17, .75f, 2, false,
                new Fits(900, 650, 700, 720, 0, 700), r -> new PackedLongLongRobinHoodHashMap(r.expectedSize()));
        hash(providers, "map.swiss", CollectionFamilies.LONG_MAP, 17, .875f, 8, false,
                new Fits(850, 700, 600, 850, 0, 700), r -> new PackedLongLongSwissHashMap(r.expectedSize()));
        hash(providers, "set.compact", CollectionFamilies.LONG_SET, 8, .75f, 2, false,
                new Fits(720, 760, 650, 780, 0, 800), r -> new PackedLongCompactHashSet(r.expectedSize()));
        hash(providers, "set.stateful", CollectionFamilies.LONG_SET, 9, .65f, 2, true,
                new Fits(700, 820, 760, 720, 0, 850), r -> new PackedLongHashSet(r.expectedSize()));
        hash(providers, "set.quadratic", CollectionFamilies.LONG_SET, 9, .65f, 2, false,
                new Fits(780, 750, 650, 700, 0, 750), r -> new PackedLongQuadraticHashSet(r.expectedSize()));
        hash(providers, "set.robin-hood", CollectionFamilies.LONG_SET, 9, .75f, 2, false,
                new Fits(900, 650, 700, 720, 0, 700), r -> new PackedLongRobinHoodHashSet(r.expectedSize()));
        hash(providers, "set.swiss", CollectionFamilies.LONG_SET, 9, .875f, 8, false,
                new Fits(850, 700, 600, 850, 0, 700), r -> new PackedLongSwissHashSet(r.expectedSize()));

        sorted(providers, "sorted-map.flat", CollectionFamilies.SORTED_LONG_MAP, 16, false,
                r -> new PackedLongLongSortedMap(r.expectedSize()));
        sorted(providers, "sorted-map.paged", CollectionFamilies.SORTED_LONG_MAP, 16, true,
                r -> new PackedLongLongPagedSortedMap(r.expectedSize(), block(r, 64)));
        sorted(providers, "sorted-set.flat", CollectionFamilies.SORTED_LONG_SET, 8, false,
                r -> new PackedLongSortedSet(r.expectedSize()));
        sorted(providers, "sorted-set.paged", CollectionFamilies.SORTED_LONG_SET, 8, true,
                r -> new PackedLongPagedSortedSet(r.expectedSize(), block(r, 64)));

        for (boolean access : new boolean[] {false, true}) {
            var caps = flat(access ? Order.ACCESS : Order.INSERTION, false,
                    (int) ((long) TABLE_MAX * 13 / 20));
            Function<CollectionRequest<?>, CollectionEstimate> model = r -> {
                        long capacity = 4;
                        while (capacity * 13 / 20 < r.expectedSize()) { capacity <<= 1; }
                        long bytes = 24L * Math.max(1, r.expectedSize()) + 4 * capacity;
                        return new Fits(750, 700, 700, 900, 0, 650).estimate(bytes, bytes,
                                "Dense rows plus primitive hash index; no churn high-water excess modeled.");
                    };
            if (access) {
                add(providers, "ordered-map.access", CollectionFamilies.ACCESS_ORDERED_LONG_MAP,
                        caps, model, r -> new PackedLongLongOrderedMap(r.expectedSize(), true));
            } else {
                add(providers, "ordered-map.insertion", CollectionFamilies.ORDERED_LONG_MAP,
                        caps, model, r -> new PackedLongLongOrderedMap(r.expectedSize(), false));
            }
        }

        add(providers, "list.inline4", CollectionFamilies.LONG_LIST, flat(Order.INSERTION, false, ARRAY_MAX),
                r -> {
                    long bytes = r.expectedSize() <= PackedLongInlineList.INLINE_CAPACITY
                            ? 0L
                            : 8L * r.expectedSize();
                    int allocation = r.expectedSize() <= PackedLongInlineList.INLINE_CAPACITY ? 1000 : 760;
                    return new CollectionEstimate(
                            bytes,
                            bytes,
                            200,
                            shiftingFit(r.expectedSize()),
                            shiftingFit(r.expectedSize()),
                            1000,
                            1000,
                            allocation,
                            "Uncalibrated structural heuristic. Four scalar long lanes retain no "
                                    + "separate primitive array; larger instances spill once to long[].");
                }, r -> new PackedLongInlineList(r.expectedSize()));
        add(providers, "list.packed", CollectionFamilies.LONG_LIST, flat(Order.INSERTION, false, ARRAY_MAX),
                r -> {
                    long bytes = 8L * Math.max(1, r.expectedSize());
                    return new Fits(200, shiftingFit(r.expectedSize()), shiftingFit(r.expectedSize()),
                            1000, 1000, 800).estimate(bytes, bytes,
                            "Pre-sized long array; insertion/removal fit assumes arbitrary positions.");
                }, r -> new LongArrayList(r.expectedSize()));
        add(providers, "deque.circular", CollectionFamilies.LONG_DEQUE, flat(Order.INSERTION, false, TABLE_MAX),
                r -> {
                    long bytes = 8 * powerOfTwo(Math.max(2, r.expectedSize()));
                    return new Fits(200, 1000, 1000, 950, 1000, 850).estimate(bytes, bytes,
                            "Power-of-two circular array; insert/remove refer to ends.");
                }, r -> new LongArrayDeque(r.expectedSize()));
        for (boolean min : new boolean[] {true, false}) {
            add(providers, min ? "heap.min" : "heap.max", CollectionFamilies.LONG_PRIORITY_QUEUE,
                    flat(min ? Order.MIN_FIRST : Order.MAX_FIRST, false, ARRAY_MAX), r -> {
                        long bytes = 8L * Math.max(1, r.expectedSize());
                        return new Fits(200, 700, 700, 900, 0, 800).estimate(bytes, bytes,
                                "Pre-sized heap; lookup means arbitrary membership, removal means poll.");
                    }, r -> new PackedLongHeap(min, r.expectedSize()));
        }

        var linkedCaps = new CollectionCapabilities(Order.INSERTION, false, true, true, true,
                ARRAY_MAX, 1, 1 << 20, 1024);
        add(providers, "linked.segmented", CollectionFamilies.LONG_LINKED_LIST, linkedCaps,
                r -> new Fits(150, 1000, 1000, 650, 150, 1000).estimate(0,
                        20L * roundedSlots(r.expectedSize(), block(r, 1024)),
                        "20 bytes/slot; fresh sequential high-water; insert/remove use known handles;"
                        + " clear retains segments; directory references excluded."),
                r -> new PackedLongLinkedList(block(r, 1024)));

        var linked28 = segmented(Order.INSERTION, true);
        add(providers, "linked.hierarchy-28", CollectionFamilies.LINKED_LIST_28, linked28,
                r -> new Fits(150, 900, 900, 600, 0, 1000).estimate(0,
                        ((long) r.expectedSize() + 4095) / 4096 * (32L * 4096 + 512),
                        "Conservative full-block data+allocator payload, including free links;"
                        + " fresh high-water; no node objects; headers/directories excluded."),
                r -> new SegmentedLongLinkedList28());
        add(providers, "arena.hierarchy-28", CollectionFamilies.LONG_ARENA_28,
                segmented(Order.INSERTION, false),
                r -> new Fits(0, 1000, 0, 850, 850, 1000).estimate(0,
                        8L * roundedSlots(r.expectedSize(), 4096),
                        "Sequential append; assumes nonzero data touches every leaf; stable offsets"
                        + " are not generation-safe handles; no removal API."),
                r -> new SegmentedLongArena28());
        lane(providers, "lane.long-28", CollectionFamilies.LONG_LANE_28, 32768,
                r -> new SegmentedLongLane28());
        lane(providers, "lane.int-28", CollectionFamilies.INT_LANE_28, 16384,
                r -> new SegmentedIntLane28());
        lane(providers, "lane.bit-28", CollectionFamilies.BIT_LANE_28, 512,
                r -> new BitLane28());
        SpecializedCollectionCatalog.addTo(providers);
        return new FuzzyCollectionFactory(providers);
    }

    private static <T> void hash(List<CollectionProvider<?>> providers, String id, CollectionFamily<T> family,
            int bytesPerSlot, float load, int minimum, boolean lazy, Fits fits,
            Function<CollectionRequest<?>, ? extends T> creator) {
        add(providers, id, family, flat(Order.ANY, lazy, (int) (TABLE_MAX * (double) load)), r -> {
            long slots = powerOfTwo(Math.max(minimum, (long) Math.ceil(r.expectedSize() / (double) load)));
            long initial = lazy ? 0 : slots * bytesPerSlot;
            long populated = lazy && r.expectedSize() == 0 ? 0 : slots * bytesPerSlot;
            return fits.estimate(initial, populated,
                    "Power-of-two table, load=" + load + "; no churn/rehash peak modeled; primitive arrays only.");
        }, creator);
    }

    private static <T> void sorted(List<CollectionProvider<?>> providers, String id,
            CollectionFamily<T> family, int bytesPerSlot, boolean paged,
            Function<CollectionRequest<?>, ? extends T> creator) {
        // Limit reserves room for the existing int ceiling-division and page-size rounding.
        var caps = paged ? new CollectionCapabilities(Order.SORTED, false, false, false, false,
                Integer.MAX_VALUE - (1 << 20), 4, 1 << 20, 64)
                : flat(Order.SORTED, false, ARRAY_MAX);
        add(providers, id, family, caps, r -> {
            int block = paged ? block(r, 64) : 1;
            long slots = roundedSlots(Math.max(1, r.expectedSize()), block);
            long initial = slots * bytesPerSlot + (paged ? slots / block * 12 : 0);
            long expected = paged ? initial * 2 : initial;
            int shifts = shiftingFit(paged ? Math.min(block, r.expectedSize()) : r.expectedSize());
            return new Fits(paged ? 800 : 900, shifts, shifts, paged ? 900 : 1000, 0, 750)
                    .estimate(initial, expected, paged
                            ? "Flat arrays containing pages, not lazy segments; expected model doubles"
                              + " initial payload for half-full pages; growth slack is not a bound."
                            : "Pre-sized parallel sorted arrays; mutation fit declines with shifting length.");
        }, creator);
    }

    private static <T> void lane(List<CollectionProvider<?>> providers, String id,
            CollectionFamily<T> family, int leafBytes, Function<CollectionRequest<?>, ? extends T> creator) {
        add(providers, id, family, segmented(Order.ANY, false), r ->
                new Fits(850, 850, 850, 0, 850, 1000).estimate(0,
                        (long) Math.min(r.expectedSize(), 65536) * leafBytes,
                        "Expected size means nonzero/true slots; worst dispersion across 65536 leaves."
                        + " Zero/false writes to absent leaves do not allocate; cleared leaves stay allocated."
                        + " Headers and directory references excluded."), creator);
    }

    private static CollectionCapabilities flat(Order order, boolean lazy, int maximum) {
        return new CollectionCapabilities(order, false, false, lazy, false, maximum, 0, 0, 0);
    }

    private static CollectionCapabilities segmented(Order order, boolean handles) {
        return new CollectionCapabilities(order, false, handles, true, true, SLOT_MAX, 4096, 4096, 4096);
    }

    private static int block(CollectionRequest<?> request, int fallback) {
        return request.requirements().blockSize() == 0 ? fallback : request.requirements().blockSize();
    }

    private static long roundedSlots(int size, int block) {
        return ((long) size + block - 1) / block * block;
    }

    private static long powerOfTwo(long size) {
        long result = 1;
        while (result < size) { result <<= 1; }
        return result;
    }

    private static int shiftingFit(int length) {
        return (int) (1000L * 64 / (64L + length));
    }

    private static <T> void add(List<CollectionProvider<?>> providers, String id, CollectionFamily<T> family,
            CollectionCapabilities capabilities, Function<CollectionRequest<?>, CollectionEstimate> model,
            Function<CollectionRequest<?>, ? extends T> creator) {
        providers.add(new CollectionProvider<>(id, VERSION, family, capabilities, model, creator));
    }

    private record Fits(int lookup, int insert, int remove, int scan, int indexed, int allocation) {
        CollectionEstimate estimate(long initial, long expected, String assumptions) {
            return new CollectionEstimate(initial, expected, lookup, insert, remove, scan, indexed,
                    allocation, "Uncalibrated structural heuristic. " + assumptions);
        }
    }
}
