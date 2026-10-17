/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Original M3 adapter; inherits Apache Commons Collections' own API and implementation
 * contracts without copying Apache source. Backing storage is first-party M3.
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import com.m3.collections.M3LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.AbstractListValuedMap;
import org.apache.commons.collections4.multimap.AbstractSetValuedMap;

/**
 * Apache Commons Collections 4.5.0 API adapters over existing M3 slot/index owners.
 *
 * <p>The authoritative key directory is an M3 linked hash map (parallel slot
 * arrays and order indexes); each present key's value lane is a native M3 linked
 * list or linked hash set (indexed arrays, not retained per-value Entry nodes).
 * All Apache live get/asMap/keySet/keys/entries/mapIterator behaviors remain
 * the upstream Abstract*ValuedMap implementations. The adapter neither copies
 * the Apache code nor substitutes Guava or Spring's different multimap contracts.
 *
 * <p>Each factory owns a fresh map. The instances are mutable, single-threaded
 * and not advertised as Serializable. Third-party Apache code is an optional
 * dependency of the interop artifact, not M3's java.base-only core. API-boundary
 * entry and iterator adapter allocations remain permitted and unmeasured.
 */
public final class M3ApacheCollections {
    private M3ApacheCollections() { }

    /** Duplicates allowed, insertion-ordered keys and values; Apache ListValuedMap. */
    public static <K, V> ListValuedMap<K, V> listValuedMap() {
        return new M3ApacheListValuedMap<>();
    }

    /** Unique values per key, insertion-ordered keys and values; Apache SetValuedMap. */
    public static <K, V> SetValuedMap<K, V> setValuedMap() {
        return new M3ApacheSetValuedMap<>();
    }

    /** Concrete adapter delegates Apache algorithms to an indexed M3 key/value owner. */
    private static final class M3ApacheListValuedMap<K, V> extends AbstractListValuedMap<K, V> {
        M3ApacheListValuedMap() {
            super(new M3LinkedHashMap<K, List<V>>());
        }

        @Override protected List<V> createCollection() {
            return new M3LinkedList<>();
        }
    }

    /** Apache SetValuedMap with duplicate suppression in existing M3 key-only slots. */
    private static final class M3ApacheSetValuedMap<K, V> extends AbstractSetValuedMap<K, V> {
        M3ApacheSetValuedMap() {
            super(new M3LinkedHashMap<K, Set<V>>());
        }

        @Override protected Set<V> createCollection() {
            return new M3LinkedHashSet<>();
        }
    }
}
