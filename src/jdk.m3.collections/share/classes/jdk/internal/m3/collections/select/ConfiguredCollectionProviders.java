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
package jdk.internal.m3.collections.select;

import jdk.internal.m3.collections.LongLongMultiMap;
import jdk.internal.m3.collections.packed.PackedLongLaneStore;
import jdk.internal.m3.collections.packed.PackedLongLongLruCache;
import jdk.internal.m3.collections.packed.PackedLongLongMultiMap;
import jdk.internal.m3.collections.packed.PackedLongRingBuffer;
import jdk.internal.m3.collections.packed.PackedLongTopK;
import jdk.internal.m3.collections.range.SegmentedLongLaneStore28;
import jdk.internal.m3.collections.select.CollectionRequest.Order;
import java.util.Objects;
import java.util.function.Function;

/**
 * Explicit semantic configuration for bounded or multi-lane shapes. Configuration
 * belongs to the family identity, so ranking can never silently change a bound,
 * top-K direction or lane count. Expected size remains only a retained-size hint.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/ConfiguredCollectionProviders.java}
 * blob {@code 6211405637bbf4fabe9bfe47b874e22e0cbb92c9} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections catalogue chain after API_UNION U3 (v6.2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ConfiguredCollectionProviders {
    public record Bound(int maximumEntries) {
        public Bound {
            if (maximumEntries < 0 || maximumEntries > SpecializedCollectionCatalog.ARRAY_MAX) {
                throw new IllegalArgumentException("maximumEntries");
            }
        }
    }
    public record Rows(int laneCount) {
        public Rows {
            if (laneCount < 1 || laneCount > 1024) { throw new IllegalArgumentException("laneCount must be 1..1024"); }
        }
    }
    public record MultiValues(int expectedDistinctKeys) {
        public MultiValues {
            if (expectedDistinctKeys < 0 || expectedDistinctKeys > SpecializedCollectionCatalog.HASH_MAX) {
                throw new IllegalArgumentException("expectedDistinctKeys");
            }
        }
    }
    public enum Direction { LARGEST, SMALLEST }
    private ConfiguredCollectionProviders() {}

    public static CollectionProvider<PackedLongLongLruCache> lru(Bound bound) {
        int limit = bound.maximumEntries();
        if (limit > SpecializedCollectionCatalog.INDEX_MAX) { throw new IllegalArgumentException("cache bound exceeds index range"); }
        long bytes = 24L * Math.max(1, limit) + 4L * SpecializedCollectionCatalog.indexSlots(limit);
        return provider("cache.lru.limit-" + limit, PackedLongLongLruCache.class,
                fixed(Order.ACCESS, limit), r -> fit(bytes, bytes, 800, 850, 850, 850, 0, 1000,
                        "Explicit LRU entry bound=" + limit + "; no expiry/weak references/loading/concurrency."),
                r -> new PackedLongLongLruCache(limit));
    }

    public static CollectionProvider<PackedLongRingBuffer> ring(Bound bound) {
        int limit = bound.maximumEntries();
        if (limit < 1 || limit > 1 << 30) { throw new IllegalArgumentException("ring bound must be 1..2^30"); }
        return provider("queue.ring.limit-" + limit, PackedLongRingBuffer.class, fixed(Order.INSERTION, limit),
                r -> fit(8L * limit, 8L * limit, 200, 1000, 1000, 1000, 1000, 1000,
                        "Offer rejects when full; addOverwrite is an explicit separate operation."),
                r -> new PackedLongRingBuffer(limit));
    }

    public static CollectionProvider<PackedLongTopK> topK(Bound bound, Direction direction) {
        Objects.requireNonNull(direction, "direction");
        int limit = bound.maximumEntries();
        boolean largest = direction == Direction.LARGEST;
        long bytes = 8L * Math.max(1, limit);
        return provider("selection.top-k." + (largest ? "largest" : "smallest") + ".limit-" + limit,
                PackedLongTopK.class, fixed(Order.ANY, limit),
                r -> fit(bytes, bytes, 200, 750, 0, 850, 0, 1000,
                        "Fixed K and direction; duplicates count; input stream may be unbounded; snapshots use heap order."),
                r -> largest ? PackedLongTopK.largest(limit) : PackedLongTopK.smallest(limit));
    }

    public static CollectionProvider<PackedLongLaneStore> packedRows(Rows layout) {
        int lanes = layout.laneCount();
        return provider("rows.packed.lanes-" + lanes, PackedLongLaneStore.class,
                SpecializedCollectionCatalog.caps(Order.ANY, false, SpecializedCollectionCatalog.ARRAY_MAX),
                r -> {
                    long bytes = 8L * lanes * Math.max(1, r.expectedSize());
                    return fit(bytes, bytes, 0, 950, 950, 1000, 1000, 800,
                            "Parallel lanes; swap-removal moves the final row; optional indexes are not included.");
                }, r -> new PackedLongLaneStore(lanes, r.expectedSize()));
    }

    public static CollectionProvider<SegmentedLongLaneStore28> stableRows(Rows layout) {
        int lanes = layout.laneCount();
        var caps = new CollectionCapabilities(Order.ANY, false, true, true, true,
                1 << 28, 4096, 4096, 4096);
        return provider("rows.stable-28.lanes-" + lanes, SegmentedLongLaneStore28.class, caps,
                r -> fit(0, (r.expectedSize() + 4095L) / 4096 * ((8L * lanes + 8) * 4096 + 512),
                        0, 900, 900, 650, 850, 1000,
                        "Stable generation handles; full nonzero leaves and allocator/free-list metadata modeled; headers excluded."),
                r -> new SegmentedLongLaneStore28(lanes));
    }

    public static CollectionProvider<LongLongMultiMap> multimap(MultiValues sizing) {
        int keys = sizing.expectedDistinctKeys();
        return provider("relation.multimap.keys-" + keys, LongLongMultiMap.class,
                SpecializedCollectionCatalog.caps(Order.ANY, false, SpecializedCollectionCatalog.ARRAY_MAX),
                r -> {
                    long initial = 21L * Math.max(1, r.expectedSize());
                    long hashes = r.expectedSize() == 0 ? 0 : 51L * SpecializedCollectionCatalog.hashSlots(keys);
                    return fit(initial, initial + hashes, 700, 750, 500, 900, 0, 650,
                            "Expected size means total values; declared distinct-key sizing feeds three hash indexes;"
                            + " shared key/value/next/state lanes; no per-key value collection; churn growth excluded.");
                }, r -> new PackedLongLongMultiMap(keys, r.expectedSize()));
    }

    /** A convenient one-provider plan; callers can also register the provider in a larger immutable factory. */
    public static <T> CollectionPlan<T> plan(CollectionProvider<T> provider, int expectedSize) {
        return new FuzzyCollectionFactory(java.util.List.of(provider)).select(
                CollectionRequest.forFamily(provider.family()).withExpectedSize(expectedSize));
    }

    private static CollectionCapabilities fixed(Order order, int limit) {
        return new CollectionCapabilities(order, false, false, false, true, limit, 0, 0, 0);
    }
    private static CollectionEstimate fit(long initial, long expected, int lookup, int insert, int remove,
            int scan, int indexed, int allocation, String assumptions) {
        return SpecializedCollectionCatalog.estimate(initial, expected, lookup, insert, remove, scan,
                indexed, allocation, assumptions);
    }
    private static <T> CollectionProvider<T> provider(String id, Class<T> type, CollectionCapabilities caps,
            Function<CollectionRequest<?>, CollectionEstimate> model, Function<CollectionRequest<?>, T> creator) {
        return new CollectionProvider<>(id, SpecializedCollectionCatalog.VERSION,
                new CollectionFamily<>(id, type), caps, model, creator);
    }
}
