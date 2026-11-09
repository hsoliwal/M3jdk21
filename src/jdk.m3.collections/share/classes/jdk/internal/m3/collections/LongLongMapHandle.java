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
package jdk.internal.m3.collections;

import jdk.internal.m3.collections.packed.PackedLongLongHashMap;
import jdk.internal.m3.collections.packed.PackedLongLongMap;
import jdk.internal.m3.collections.packed.PackedLongLongPagedSortedMap;
import jdk.internal.m3.collections.packed.PackedLongLongRobinHoodHashMap;
import jdk.internal.m3.collections.packed.PackedLongLongSortedMap;
import jdk.internal.m3.collections.packed.PackedLongLongSwissHashMap;
import jdk.m3.collections.LongLongConsumer;
import jdk.m3.collections.LongLongSortedMap;
import java.util.Objects;

/**
 * Primitive client handle over one compiled long-to-long map.
 *
 * <p>The return contract of {@link #put(long, long)} is representation independent:
 * INSERTED means a new key; REPLACED means an existing key was updated.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongLongMapHandle.java}
 * blob {@code 17d666d0930e78ce3b784dc55621b2198b0f6f27} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.collections v6 batch (no-sprawl): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongLongMapHandle {
    public enum PutResult {
        INSERTED,
        REPLACED
    }

    private final LongLongMapPlan plan;
    private final Object storage;

    LongLongMapHandle(LongLongMapPlan plan) {
        this.plan = Objects.requireNonNull(plan, "plan");
        storage = switch (plan.representation()) {
            case DENSE_SORTED -> new PackedLongLongSortedMap(plan.spec().expectedSize());
            case PAGED_SORTED -> new PackedLongLongPagedSortedMap(
                    plan.spec().expectedSize(), plan.spec().pageSize());
            case LINEAR_HASH -> new PackedLongLongHashMap(plan.spec().expectedSize());
            case ROBIN_HOOD_HASH -> new PackedLongLongRobinHoodHashMap(plan.spec().expectedSize());
            case SWISS_HASH -> new PackedLongLongSwissHashMap(plan.spec().expectedSize());
            case INSERTION_ORDERED -> new PackedLongLongOrderedMap(
                    plan.spec().expectedSize(), false);
            case ACCESS_ORDERED -> new PackedLongLongOrderedMap(
                    plan.spec().expectedSize(), true);
        };
    }

    public LongLongMapPlan plan() {
        return plan;
    }

    public LongLongMapRepresentation representation() {
        return plan.representation();
    }

    public PutResult put(long key, long value) {
        boolean inserted;
        if (storage instanceof PackedLongLongMap map) {
            inserted = map.put(key, value);
        } else if (storage instanceof LongLongSortedMap map) {
            inserted = map.put(key, value);
        } else {
            inserted = ((LongLongOrderedMap) storage).put(key, value);
        }
        return inserted ? PutResult.INSERTED : PutResult.REPLACED;
    }

    public boolean containsKey(long key) {
        if (storage instanceof PackedLongLongMap map) {
            return map.containsKey(key);
        }
        if (storage instanceof LongLongSortedMap map) {
            return map.containsKey(key);
        }
        return ((LongLongOrderedMap) storage).containsKey(key);
    }

    public long getOrDefault(long key, long fallback) {
        if (storage instanceof PackedLongLongMap map) {
            return map.getOrDefault(key, fallback);
        }
        if (storage instanceof LongLongSortedMap map) {
            return map.getOrDefault(key, fallback);
        }
        return ((LongLongOrderedMap) storage).getOrDefault(key, fallback);
    }

    public long getOrThrow(long key) {
        if (storage instanceof PackedLongLongMap map) {
            return map.getOrThrow(key);
        }
        if (storage instanceof LongLongSortedMap map) {
            return map.getOrThrow(key);
        }
        return ((LongLongOrderedMap) storage).getOrThrow(key);
    }

    public boolean remove(long key) {
        if (storage instanceof PackedLongLongMap map) {
            return map.remove(key);
        }
        if (storage instanceof LongLongSortedMap map) {
            return map.remove(key);
        }
        return ((LongLongOrderedMap) storage).remove(key);
    }

    public int size() {
        if (storage instanceof PackedLongLongMap map) {
            return map.size();
        }
        if (storage instanceof LongLongSortedMap map) {
            return map.size();
        }
        return ((LongLongOrderedMap) storage).size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void clear() {
        if (storage instanceof PackedLongLongMap map) {
            map.clear();
        } else if (storage instanceof LongLongSortedMap map) {
            map.clear();
        } else {
            ((LongLongOrderedMap) storage).clear();
        }
    }

    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (storage instanceof PackedLongLongMap map) {
            map.forEach(consumer);
        } else if (storage instanceof LongLongSortedMap map) {
            map.forEach(consumer);
        } else {
            ((LongLongOrderedMap) storage).forEach(consumer);
        }
    }

    public long[] keysToArray() {
        if (storage instanceof PackedLongLongMap map) {
            return map.keysToArray();
        }
        if (storage instanceof LongLongSortedMap map) {
            return map.keysToArray();
        }
        return ((LongLongOrderedMap) storage).keysToArray();
    }

    public long[] valuesToArray() {
        if (storage instanceof PackedLongLongMap map) {
            return map.valuesToArray();
        }
        if (storage instanceof LongLongSortedMap map) {
            return map.valuesToArray();
        }
        return ((LongLongOrderedMap) storage).valuesToArray();
    }

    public long firstKey() {
        if (storage instanceof LongLongSortedMap map) {
            return map.firstKey();
        }
        if (storage instanceof PackedLongLongOrderedMap map) {
            return map.firstKey();
        }
        throw new UnsupportedOperationException("map has no first-key order");
    }

    public long lastKey() {
        if (storage instanceof LongLongSortedMap map) {
            return map.lastKey();
        }
        if (storage instanceof PackedLongLongOrderedMap map) {
            return map.lastKey();
        }
        throw new UnsupportedOperationException("map has no last-key order");
    }

    public long floorKey(long key, long fallback) {
        if (storage instanceof LongLongSortedMap map) {
            return map.floorKey(key, fallback);
        }
        throw new UnsupportedOperationException("map was not compiled with sorted semantics");
    }

    public long ceilingKey(long key, long fallback) {
        if (storage instanceof LongLongSortedMap map) {
            return map.ceilingKey(key, fallback);
        }
        throw new UnsupportedOperationException("map was not compiled with sorted semantics");
    }
}
