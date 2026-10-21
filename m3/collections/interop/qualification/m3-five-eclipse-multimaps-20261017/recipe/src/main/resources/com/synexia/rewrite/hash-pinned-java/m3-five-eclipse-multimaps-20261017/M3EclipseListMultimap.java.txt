/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.multimap.bag.HashBagMultimap;
import org.eclipse.collections.impl.multimap.list.AbstractMutableListMultimap;

/**
 * An Eclipse Collections MutableListMultimap backed by the existing M3
 * indexed map and M3 indexed-link list owners. No per-occurrence M3 Entry/Node
 * layer is introduced. The inherited Eclipse implementation owns the API
 * semantics (counting, live views, mutable rich operations). Operations whose
 * contracts require independent results allocate new M3-backed collections;
 * flip's distinct bag-multimap result is owned by Eclipse itself.
 *
 * <p>This is not Guava ListMultimap, Apache ListValuedMap or Spring
 * MultiValueMap; those keep their different public API contracts.</p>
 */
public final class M3EclipseListMultimap<K, V> extends AbstractMutableListMultimap<K, V> {
    public M3EclipseListMultimap() { super(); }

    @Override protected MutableMap<K, MutableList<V>> createMap() {
        return MapAdapter.adapt(new M3LinkedHashMap<>());
    }

    @Override protected MutableMap<K, MutableList<V>> createMapWithKeyCount(int expectedKeys) {
        if (expectedKeys < 0) throw new IllegalArgumentException("negative key count");
        return createMap();
    }

    @Override protected MutableList<V> createCollection() {
        return ListAdapter.adapt(new M3LinkedList<>());
    }

    @Override public M3EclipseListMultimap<K, V> newEmpty() {
        return new M3EclipseListMultimap<>();
    }

    @Override public MutableBagMultimap<V, K> flip() {
        HashBagMultimap<V, K> result = HashBagMultimap.newMultimap();
        this.forEachKeyValue((key, value) -> result.put(value, key));
        return result;
    }

    @Override public M3EclipseListMultimap<K, V> selectKeysValues(
            Predicate2<? super K, ? super V> predicate) {
        M3EclipseListMultimap<K, V> result = newEmpty();
        this.forEachKeyValue((key, value) -> { if (predicate.accept(key, value)) result.put(key, value); });
        return result;
    }

    @Override public M3EclipseListMultimap<K, V> rejectKeysValues(
            Predicate2<? super K, ? super V> predicate) {
        M3EclipseListMultimap<K, V> result = newEmpty();
        this.forEachKeyValue((key, value) -> { if (!predicate.accept(key, value)) result.put(key, value); });
        return result;
    }

    @Override public M3EclipseListMultimap<K, V> selectKeysMultiValues(
            Predicate2<? super K, ? super RichIterable<V>> predicate) {
        M3EclipseListMultimap<K, V> result = newEmpty();
        this.forEachKeyMutableList((key, values) -> {
            if (predicate.accept(key, values)) result.putAll(key, values);
        });
        return result;
    }

    @Override public M3EclipseListMultimap<K, V> rejectKeysMultiValues(
            Predicate2<? super K, ? super RichIterable<V>> predicate) {
        M3EclipseListMultimap<K, V> result = newEmpty();
        this.forEachKeyMutableList((key, values) -> {
            if (!predicate.accept(key, values)) result.putAll(key, values);
        });
        return result;
    }
}
