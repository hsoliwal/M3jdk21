/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 * An M3-owned Eclipse Collections API implementation, not a renamed vendor class.
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.multimap.list.AbstractMutableListMultimap;

/**
 * Eclipse MutableListMultimap over M3's existing indexed map and reference-list
 * owners. Each key points to an independently owned M3 linked-index list.
 * Duplicate values and encounter order are retained. No per-pair M3 Entry/Node
 * store is created. Eclipse owns the interface, views and callback contracts.
 *
 * A list returned by inherited get() follows Eclipse's unmodifiable-view
 * semantics; do not substitute Guava's writable get() or Spring's map-of-lists.
 * Instances are not thread-safe and are not Eclipse concrete-class serial clones.
 */
public final class M3EclipseListMultimap<K, V> extends AbstractMutableListMultimap<K, V> {
    public M3EclipseListMultimap() { super(); }

    @Override protected MutableMap<K, MutableList<V>> createMap() {
        return MapAdapter.adapt(new M3LinkedHashMap<K, MutableList<V>>());
    }

    @Override protected MutableMap<K, MutableList<V>> createMapWithKeyCount(int keyCount) {
        if (keyCount < 0) throw new IllegalArgumentException("negative key count");
        return createMap();
    }

    @Override protected MutableList<V> createCollection() {
        return ListAdapter.adapt(new M3LinkedList<V>());
    }

    @Override public M3EclipseListMultimap<K, V> newEmpty() {
        return new M3EclipseListMultimap<>();
    }

    @Override public M3EclipseListMultimap<K, V> toMutable() {
        M3EclipseListMultimap<K, V> copy = newEmpty();
        copy.putAll(this);
        return copy;
    }

    @Override public M3EclipseBagMultimap<V, K> flip() {
        M3EclipseBagMultimap<V, K> inverse = new M3EclipseBagMultimap<>();
        forEachKeyValue((key, value) -> inverse.put(value, key));
        return inverse;
    }
}
