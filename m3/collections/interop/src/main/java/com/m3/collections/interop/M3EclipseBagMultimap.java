/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 * An M3-owned Eclipse Collections API implementation, not a renamed vendor class.
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.multimap.bag.AbstractMutableBagMultimap;

/**
 * Eclipse MutableBagMultimap backed by an M3 key directory and a per-key
 * M3ObjectCounts column through existing M3EclipseBag. Duplicates consume
 * primitive counters, not a retained object node for every occurrence.
 * Eclipse's own abstract bag-multimap governs its pair-count and view contracts.
 */
public final class M3EclipseBagMultimap<K, V> extends AbstractMutableBagMultimap<K, V> {
    public M3EclipseBagMultimap() { super(); }

    @Override protected MutableMap<K, MutableBag<V>> createMap() {
        return MapAdapter.adapt(new M3LinkedHashMap<K, MutableBag<V>>());
    }

    @Override protected MutableMap<K, MutableBag<V>> createMapWithKeyCount(int keyCount) {
        if (keyCount < 0) throw new IllegalArgumentException("negative key count");
        return createMap();
    }

    @Override protected MutableBag<V> createCollection() {
        return new M3EclipseBag<>();
    }

    @Override public M3EclipseBagMultimap<K, V> newEmpty() {
        return new M3EclipseBagMultimap<>();
    }

    @Override public M3EclipseBagMultimap<K, V> toMutable() {
        M3EclipseBagMultimap<K, V> copy = newEmpty();
        copy.putAll(this);
        return copy;
    }

    @Override public M3EclipseBagMultimap<V, K> flip() {
        M3EclipseBagMultimap<V, K> inverse = new M3EclipseBagMultimap<>();
        forEachKeyValue((key, value) -> inverse.put(value, key));
        return inverse;
    }
}
