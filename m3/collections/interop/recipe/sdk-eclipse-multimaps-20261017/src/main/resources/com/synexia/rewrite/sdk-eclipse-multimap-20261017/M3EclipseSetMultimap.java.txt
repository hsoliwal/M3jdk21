/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 * An M3-owned Eclipse Collections API implementation, not a renamed vendor class.
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.multimap.set.AbstractMutableSetMultimap;
import org.eclipse.collections.impl.set.mutable.SetAdapter;

/**
 * Eclipse MutableSetMultimap backed by existing M3 indexed key and set owners.
 * Duplicate key/value pairs are rejected by the Eclipse value-set contract;
 * no separate hash-index implementation or third-party implementation copy.
 */
public final class M3EclipseSetMultimap<K, V> extends AbstractMutableSetMultimap<K, V> {
    public M3EclipseSetMultimap() { super(); }

    @Override protected MutableMap<K, MutableSet<V>> createMap() {
        return MapAdapter.adapt(new M3LinkedHashMap<K, MutableSet<V>>());
    }

    @Override protected MutableMap<K, MutableSet<V>> createMapWithKeyCount(int keyCount) {
        if (keyCount < 0) throw new IllegalArgumentException("negative key count");
        return createMap();
    }

    @Override protected MutableSet<V> createCollection() {
        return SetAdapter.adapt(new M3LinkedHashSet<V>());
    }

    @Override public M3EclipseSetMultimap<K, V> newEmpty() {
        return new M3EclipseSetMultimap<>();
    }

    @Override public M3EclipseSetMultimap<K, V> toMutable() {
        M3EclipseSetMultimap<K, V> copy = newEmpty();
        copy.putAll(this);
        return copy;
    }

    @Override public M3EclipseSetMultimap<V, K> flip() {
        M3EclipseSetMultimap<V, K> inverse = new M3EclipseSetMultimap<>();
        forEachKeyValue((key, value) -> inverse.put(value, key));
        return inverse;
    }
}
