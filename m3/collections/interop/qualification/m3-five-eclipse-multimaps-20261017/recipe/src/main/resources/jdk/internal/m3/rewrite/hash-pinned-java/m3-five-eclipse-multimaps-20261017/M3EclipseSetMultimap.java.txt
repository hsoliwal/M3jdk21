/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors. */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.multimap.set.AbstractMutableSetMultimap;

/**
 * An Eclipse Collections MutableSetMultimap over M3 indexed map/set storage.
 * The M3LinkedHashMap owns the canonical group keys and each key owns exactly
 * one M3LinkedHashSet; no additional M3 Node/Entry objects are retained per
 * key/value pair. The inherited Eclipse collection API supplies all views,
 * equality and occurrence cardinality. This is distinct from the Guava and
 * Apache SetValuedMap/SetMultimap contracts.
 */
public final class M3EclipseSetMultimap<K,V> extends AbstractMutableSetMultimap<K,V> {
    public M3EclipseSetMultimap() { super(); }

    @Override protected MutableMap<K, MutableSet<V>> createMap() {
        return MapAdapter.adapt(new M3LinkedHashMap<>());
    }

    @Override protected MutableMap<K, MutableSet<V>> createMapWithKeyCount(int expectedKeys) {
        if (expectedKeys < 0) throw new IllegalArgumentException("negative key count");
        return createMap();
    }

    @Override protected MutableSet<V> createCollection() {
        return SetAdapter.adapt(new M3LinkedHashSet<>());
    }

    @Override public M3EclipseSetMultimap<K,V> newEmpty() {
        return new M3EclipseSetMultimap<>();
    }

    @Override public M3EclipseSetMultimap<V,K> flip() {
        M3EclipseSetMultimap<V,K> result = new M3EclipseSetMultimap<>();
        this.forEachKeyValue((key,value)->result.put(value,key));
        return result;
    }

    @Override public M3EclipseSetMultimap<K,V> selectKeysValues(
            Predicate2<? super K,? super V> predicate) {
        M3EclipseSetMultimap<K,V> result=newEmpty();
        this.forEachKeyValue((key,value)->{if(predicate.accept(key,value))result.put(key,value);});
        return result;
    }

    @Override public M3EclipseSetMultimap<K,V> selectKeysMultiValues(
            Predicate2<? super K,? super RichIterable<V>> predicate) {
        M3EclipseSetMultimap<K,V> result=newEmpty();
        this.forEachKeyMutableSet((key,values)->{
            if(predicate.accept(key,values))result.putAll(key,values);
        });
        return result;
    }

    @Override public M3EclipseSetMultimap<K,V> rejectKeysValues(
            Predicate2<? super K,? super V> predicate) {
        M3EclipseSetMultimap<K,V> result=newEmpty();
        this.forEachKeyValue((key,value)->{if(!predicate.accept(key,value))result.put(key,value);});
        return result;
    }

    @Override public M3EclipseSetMultimap<K,V> rejectKeysMultiValues(
            Predicate2<? super K,? super RichIterable<V>> predicate) {
        M3EclipseSetMultimap<K,V> result=newEmpty();
        this.forEachKeyMutableSet((key,values)->{
            if(!predicate.accept(key,values))result.putAll(key,values);
        });
        return result;
    }
}
