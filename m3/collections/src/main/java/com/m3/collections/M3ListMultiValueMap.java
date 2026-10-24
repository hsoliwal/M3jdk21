/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Generated from canonical Synexia M3 source-sealed recipe.
 */
package com.m3.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Native M3 map-of-lists, distinct from pair-counted M3IndexedMultiValueMap.
 * {@code size()} counts KEYS, not value occurrences; absent keys, empty lists
 * and null mapped lists are distinct states. Caller-supplied lists are retained
 * by reference, while internally created lists use M3ArrayList's Object[] lane.
 * The backing key directory is the existing array-indexed M3LinkedHashMap.
 * Single-threaded; neither Spring nor Guava runtime is used by this owner.
 */
public final class M3ListMultiValueMap<K, V> extends AbstractMap<K, List<V>> {
    private final M3LinkedHashMap<K, List<V>> data = new M3LinkedHashMap<>();

    public M3ListMultiValueMap() { }
    @Override public int size() { return data.size(); }
    @Override public boolean containsKey(Object key) { return data.containsKey(key); }
    @Override public List<V> get(Object key) { return data.get(key); }
    @Override public List<V> put(K key, List<V> values) { return data.put(key, values); }
    @Override public List<V> remove(Object key) { return data.remove(key); }
    @Override public void clear() { data.clear(); }
    @Override public Set<Entry<K, List<V>>> entrySet() { return data.entrySet(); }

    /** Lazily create an M3 dynamic-array list; existing caller lists stay aliased. */
    public void add(K key, V value) {
        List<V> values = data.get(key);
        if (values == null) {
            values = new M3ArrayList<>();
            data.put(key, values);
        }
        values.add(value);
    }

    public void addAll(K key, List<? extends V> additions) {
        Objects.requireNonNull(additions, "additions");
        List<V> values = data.get(key);
        if (values == null) {
            values = new M3ArrayList<>();
            data.put(key, values);
        }
        values.addAll(additions);
    }

    /** Replace one entire mapped list using an M3-owned, one-value array list. */
    public void set(K key, V value) {
        M3ArrayList<V> values = new M3ArrayList<>();
        values.add(value);
        data.put(key, values);
    }

    public V getFirst(Object key) {
        List<V> values = data.get(key);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    /** Independent M3 scalar index, unlike the live asSingleValueMap view. */
    public M3LinkedHashMap<K, V> toSingleValueMap() {
        M3LinkedHashMap<K, V> single = new M3LinkedHashMap<>();
        data.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) single.put(key, values.get(0));
        });
        return single;
    }

    /** Live key-count Map projection; updates write through the same M3 owner. */
    public Map<K, V> asSingleValueMap() {
        return new AbstractMap<>() {
            @Override public int size() { return data.size(); }
            @Override public boolean containsKey(Object key) { return data.containsKey(key); }
            @Override public V get(Object key) { return getFirst(key); }
            @Override public V put(K key, V value) {
                V old = getFirst(key);
                set(key, value);
                return old;
            }
            @Override public V remove(Object key) {
                V old = getFirst(key);
                data.remove(key);
                return old;
            }
            @Override public void clear() { data.clear(); }
            @Override public Set<Map.Entry<K, V>> entrySet() {
                return new AbstractSet<>() {
                    @Override public int size() { return data.size(); }
                    @Override public void clear() { data.clear(); }
                    @Override public Iterator<Map.Entry<K, V>> iterator() {
                        Iterator<K> keys = data.keySet().iterator();
                        return new Iterator<>() {
                            @Override public boolean hasNext() { return keys.hasNext(); }
                            @Override public Map.Entry<K, V> next() {
                                K key = keys.next();
                                return new SimpleImmutableEntry<>(key, getFirst(key));
                            }
                            @Override public void remove() { keys.remove(); }
                        };
                    }
                };
            }
        };
    }
}
