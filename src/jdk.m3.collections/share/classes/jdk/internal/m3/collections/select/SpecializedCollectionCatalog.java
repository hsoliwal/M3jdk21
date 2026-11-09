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

import jdk.internal.m3.collections.PackedLongLongBiMap;
import jdk.internal.m3.collections.PackedLongLongTable;
import jdk.internal.m3.collections.bits.PackedLongBitSet;
import jdk.internal.m3.collections.packed.PackedIntLongSparseArray;
import jdk.internal.m3.collections.packed.PackedLongByteHashMap;
import jdk.internal.m3.collections.packed.PackedLongHashBag;
import jdk.internal.m3.collections.packed.PackedLongLinearSet;
import jdk.internal.m3.collections.packed.PackedLongLongLinearMap;
import jdk.internal.m3.collections.packed.PackedLongOrderedSet;
import jdk.internal.m3.collections.packed.PackedLongPairs;
import jdk.internal.m3.collections.packed.PackedLongSortedBag;
import jdk.internal.m3.collections.packed.PackedUnsignedByteArray;
import jdk.internal.m3.collections.range.PackedLongRangeMap;
import jdk.internal.m3.collections.range.PackedLongRangeSet;
import jdk.internal.m3.collections.select.CollectionRequest.Order;
import jdk.m3.collections.LongArrayDeque;
import jdk.m3.collections.LongArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Additional native shapes with storage formulas tied to the source layout.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/SpecializedCollectionCatalog.java}
 * blob {@code f0b267cd7736afb60764d811e3aaf7ead310fd7c} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections catalogue chain after API_UNION U3 (v6.2): identifiers and package only; bytecode equal to the source modulo names).
 */
final class SpecializedCollectionCatalog {
    static final int ARRAY_MAX = Integer.MAX_VALUE - 8;
    static final int INDEX_MAX = (int) ((1L << 30) * 13 / 20);
    static final int HASH_MAX = (int) ((1L << 30) * (double) .65f);
    static final String VERSION = "native-shapes-heuristic-v1";
    private SpecializedCollectionCatalog() {}

    static void addTo(List<CollectionProvider<?>> providers) {
        add(providers, "map.linear", CollectionFamilies.LONG_MAP, caps(Order.ANY, true, ARRAY_MAX),
                r -> estimate(0, 16L * r.expectedSize(), linearFit(r), linearFit(r), linearFit(r), 1000, 0, 900,
                        "Two dense long lanes; expected size pre-reserved logically; O(n) lookup; swap-delete."),
                r -> new PackedLongLongLinearMap(r.expectedSize()));
        add(providers, "set.linear", CollectionFamilies.LONG_SET, caps(Order.ANY, true, ARRAY_MAX),
                r -> estimate(0, 8L * r.expectedSize(), linearFit(r), linearFit(r), linearFit(r), 1000, 0, 950,
                        "One dense long lane; O(n) membership; swap-delete; no state or index lane."),
                r -> new PackedLongLinearSet(r.expectedSize()));
        add(providers, "bag.sorted", CollectionFamilies.LONG_BAG, caps(Order.SORTED, false, ARRAY_MAX),
                r -> flat(r, 16, 850, linearFit(r), linearFit(r), 1000, 0,
                        "Sorted value/count lanes; expected size means distinct values; exact overflow preflight."),
                r -> new PackedLongSortedBag(r.expectedSize()));
        add(providers, "bag.hashed", CollectionFamilies.LONG_BAG, caps(Order.ANY, true, HASH_MAX),
                r -> estimate(0, r.expectedSize() == 0 ? 0 : 17L * hashSlots(r.expectedSize()),
                        750, 820, 750, 750, 0, 850,
                        "Lazy long value/count hash lanes; exact arithmetic preflight; expected size is distinct values."),
                r -> new PackedLongHashBag(r.expectedSize()));
        add(providers, "bits.packed", CollectionFamilies.BIT_SET, caps(Order.SORTED, false, Integer.MAX_VALUE - 63),
                r -> {
                    long bytes = 8L * Math.max(1, (r.expectedSize() + 63L) / 64);
                    return estimate(bytes, bytes, 1000, 1000, 1000, 1000, 1000, 900,
                            "Expected size is initial bit span; one long per 64 positions; growth slack excluded.");
                }, r -> new PackedLongBitSet(r.expectedSize()));
        add(providers, "map.long-byte", CollectionFamilies.LONG_BYTE_MAP, caps(Order.ANY, false, HASH_MAX),
                r -> {
                    long bytes = 10L * hashSlots(r.expectedSize());
                    return estimate(bytes, bytes, 750, 800, 700, 750, 0, 800,
                            "Long keys, unsigned byte values, byte states; values restricted to 0..255.");
                }, r -> new PackedLongByteHashMap(r.expectedSize()));
        add(providers, "set.ordered", CollectionFamilies.ORDERED_LONG_SET, caps(Order.INSERTION, false, HASH_MAX),
                r -> {
                    long order = 8L * Math.max(1, r.expectedSize());
                    long membership = r.expectedSize() == 0 ? 0 : 9L * hashSlots(r.expectedSize());
                    return estimate(order, order + membership, 800, 800, linearFit(r), 1000, 0, 700,
                            "Order lane plus hash membership duplicates values; removal shifts the order lane.");
                }, r -> new PackedLongOrderedSet(r.expectedSize()));
        add(providers, "map.int-sparse", CollectionFamilies.INT_LONG_SPARSE_MAP, caps(Order.SORTED, false, ARRAY_MAX),
                r -> flat(r, 12, 850, linearFit(r), linearFit(r), 1000, 1000,
                        "Sorted nonnegative int indices and long values; ordinal access differs from index lookup."),
                r -> new PackedIntLongSparseArray(r.expectedSize()));
        add(providers, "pairs.packed", CollectionFamilies.LONG_PAIRS, caps(Order.INSERTION, false, ARRAY_MAX),
                r -> flat(r, 16, 0, 1000, linearFit(r), 1000, 1000, "Two primitive lanes, no pair objects."),
                r -> new PackedLongPairs(r.expectedSize()));
        add(providers, "stack.packed", CollectionFamilies.LONG_STACK, caps(Order.ANY, false, ARRAY_MAX),
                r -> flat(r, 8, 200, 1000, 1000, 1000, 0, "LIFO operations use the existing packed list kernel."),
                r -> new LongArrayList(r.expectedSize()));
        add(providers, "fifo.circular", CollectionFamilies.LONG_FIFO, caps(Order.INSERTION, false, 1 << 30),
                r -> {
                    long bytes = 8L * powerOfTwo(Math.max(2, r.expectedSize()));
                    return estimate(bytes, bytes, 200, 1000, 1000, 1000, 0, 850,
                            "Growable FIFO using the packed deque; no fixed bound implied.");
                }, r -> new LongArrayDeque(r.expectedSize()));
        add(providers, "relation.bimap", CollectionFamilies.LONG_BIMAP, caps(Order.ANY, false, INDEX_MAX),
                r -> {
                    long bytes = 16L * Math.max(1, r.expectedSize()) + 8L * indexSlots(r.expectedSize());
                    return estimate(bytes, bytes, 850, 750, 850, 1000, 0, 700,
                            "Two long lanes and two int row indexes; one-to-one values; swap-delete.");
                }, r -> new PackedLongLongBiMap(r.expectedSize()));
        add(providers, "relation.table", CollectionFamilies.LONG_TABLE, caps(Order.ANY, false, INDEX_MAX),
                r -> {
                    long bytes = 24L * Math.max(1, r.expectedSize()) + 4L * indexSlots(r.expectedSize());
                    return estimate(bytes, bytes, 850, 750, 850, 1000, 0, 700,
                            "Row/column/value lanes plus one int index; no nested per-row maps.");
                }, r -> new PackedLongLongTable(r.expectedSize()));
        add(providers, "interval.map", CollectionFamilies.LONG_RANGE_MAP, caps(Order.SORTED, true, ARRAY_MAX - 2),
                r -> estimate(0, r.expectedSize() == 0 ? 0 : 24L * (r.expectedSize() + 2L),
                        850, linearFit(r), linearFit(r), 1000, 0, 100,
                        "Expected size is interval count; staged start/end/value arrays on updates; transients excluded."),
                r -> new PackedLongRangeMap());
        add(providers, "interval.set", CollectionFamilies.LONG_RANGE_SET, caps(Order.SORTED, true, ARRAY_MAX - 2),
                r -> estimate(0, r.expectedSize() == 0 ? 0 : 16L * (r.expectedSize() + 2L),
                        850, linearFit(r), linearFit(r), 1000, 0, 100,
                        "Two endpoint lanes; coalesced inclusive intervals; staged updates; no dummy value lane."),
                r -> new PackedLongRangeSet());
        add(providers, "sequence.unsigned-byte", CollectionFamilies.UNSIGNED_BYTES, caps(Order.INSERTION, false, ARRAY_MAX),
                r -> flat(r, 1, 200, 1000, linearFit(r), 1000, 1000, "One byte per value; unsigned 0..255 API."),
                r -> new PackedUnsignedByteArray(r.expectedSize()));
    }

    static CollectionCapabilities caps(Order order, boolean lazy, int max) {
        return new CollectionCapabilities(order, false, false, lazy, false, max, 0, 0, 0);
    }
    static long hashSlots(int size) { return powerOfTwo(Math.max(2L, (long) Math.ceil(size / (double) .65f))); }
    static long indexSlots(int size) {
        long slots = 4; while (slots * 13 / 20 < size) { slots <<= 1; } return slots;
    }
    static long powerOfTwo(long size) { long n = 1; while (n < size) { n <<= 1; } return n; }
    private static int linearFit(CollectionRequest<?> request) { return (int) (16000L / (16L + request.expectedSize())); }
    private static CollectionEstimate flat(CollectionRequest<?> r, int bytesPerSlot, int lookup, int insert,
            int remove, int scan, int indexed, String assumptions) {
        long bytes = (long) bytesPerSlot * Math.max(1, r.expectedSize());
        return estimate(bytes, bytes, lookup, insert, remove, scan, indexed, 800, assumptions);
    }
    static CollectionEstimate estimate(long initial, long expected, int lookup, int insert, int remove,
            int scan, int indexed, int allocation, String assumptions) {
        return new CollectionEstimate(initial, expected, lookup, insert, remove, scan, indexed, allocation,
                "Uncalibrated heuristic; primitive payload only, excludes headers/references. " + assumptions);
    }
    private static <T> void add(List<CollectionProvider<?>> providers, String id, CollectionFamily<T> family,
            CollectionCapabilities capabilities, Function<CollectionRequest<?>, CollectionEstimate> model,
            Function<CollectionRequest<?>, ? extends T> create) {
        providers.add(new CollectionProvider<>(id, VERSION, family, capabilities, model, create));
    }
}
