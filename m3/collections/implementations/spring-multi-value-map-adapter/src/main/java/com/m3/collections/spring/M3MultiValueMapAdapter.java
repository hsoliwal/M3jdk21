/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Owned M3 body for the public contract of Spring Framework core 6.2.0
 * org.springframework.util.MultiValueMapAdapter. Spring supplies only the MultiValueMap API interface
 * (Apache-2.0); no Spring implementation class, helper, interface default or body is called or copied.
 */
package com.m3.collections.spring;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.springframework.util.MultiValueMap;

/**
 * Owned write-through MultiValueMap over a caller-supplied {@code Map<K, List<V>>}.
 *
 * <p>The adapter contract is write-through: every change lands in the caller's target map and every read
 * observes it, so the adapter keeps no lanes of its own and stores one reference. Value lists it creates
 * ({@code add}, {@code addAll}, {@code set}) are {@link ArrayList}s, as Spring's are; in M3jdk21 that type is
 * itself backed in place by M3 array lanes, so the per-key storage is array-backed without a second list
 * type. Map defaults ({@code compute}, {@code merge}, {@code replaceAll}, ...) dispatch through this
 * adapter exactly as Spring's inherited defaults do.
 *
 * <p>{@link #asSingleValueMap()} returns an owned live first-value view with Spring 6.2's protocol:
 * {@code get/put/remove} adapt null or empty lists to null, while {@code values}, {@code entrySet},
 * {@code forEach} and {@code containsValue} read index zero and keep the provider's failures on null or
 * empty lists; hash and text use the list-valued target. Its walks create no entries except where
 * {@code entrySet()} iteration must return one.
 *
 * @param <K> key type
 * @param <V> value element type
 */
public final class M3MultiValueMapAdapter<K, V> implements MultiValueMap<K, V>, Serializable {
    @Serial private static final long serialVersionUID = 0L;

    @SuppressWarnings("serial")
    private final Map<K, List<V>> targetMap;

    /**
     * Adapts {@code targetMap}; all reads and writes go through to it.
     *
     * @param targetMap the caller's list-valued map
     * @throws IllegalArgumentException when {@code targetMap} is null
     */
    public M3MultiValueMapAdapter(Map<K, List<V>> targetMap) {
        if (targetMap == null) { throw new IllegalArgumentException("'targetMap' must not be null"); }
        this.targetMap = targetMap;
    }

    private static <V> V first(List<V> values) {
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    // ------------------------------------------------------------------ MultiValueMap

    @Override public V getFirst(K key) {
        return first(targetMap.get(key));
    }

    @Override public void add(K key, V value) {
        targetMap.computeIfAbsent(key, k -> new ArrayList<>(1)).add(value);
    }

    /** Creates the key's list sized to {@code values} only when absent; a null argument fails as in Spring. */
    @Override public void addAll(K key, List<? extends V> values) {
        targetMap.computeIfAbsent(key, k -> new ArrayList<>(values.size())).addAll(values);
    }

    @Override public void addAll(MultiValueMap<K, V> values) {
        values.forEach(this::addAll);
    }

    @Override public void addIfAbsent(K key, V value) {
        if (!targetMap.containsKey(key)) { add(key, value); }
    }

    @Override public void set(K key, V value) {
        List<V> values = new ArrayList<>(1);
        values.add(value);
        targetMap.put(key, values);
    }

    @Override public void setAll(Map<K, V> values) {
        values.forEach(this::set);
    }

    @Override public Map<K, V> toSingleValueMap() {
        Map<K, V> single = LinkedHashMap.newLinkedHashMap(targetMap.size());
        targetMap.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) { single.put(key, values.get(0)); }
        });
        return single;
    }

    /** A new live first-value view per call, as Spring's interface default returns. */
    @Override public Map<K, V> asSingleValueMap() {
        return new SingleValueView<>(this);
    }

    // ------------------------------------------------------------------ Map (write-through)

    @Override public int size() {
        return targetMap.size();
    }

    @Override public boolean isEmpty() {
        return targetMap.isEmpty();
    }

    @Override public boolean containsKey(Object key) {
        return targetMap.containsKey(key);
    }

    @Override public boolean containsValue(Object value) {
        return targetMap.containsValue(value);
    }

    @Override public List<V> get(Object key) {
        return targetMap.get(key);
    }

    @Override public List<V> put(K key, List<V> value) {
        return targetMap.put(key, value);
    }

    @Override public List<V> putIfAbsent(K key, List<V> value) {
        return targetMap.putIfAbsent(key, value);
    }

    @Override public List<V> remove(Object key) {
        return targetMap.remove(key);
    }

    @Override public void putAll(Map<? extends K, ? extends List<V>> map) {
        targetMap.putAll(map);
    }

    @Override public void clear() {
        targetMap.clear();
    }

    @Override public Set<K> keySet() {
        return targetMap.keySet();
    }

    @Override public Collection<List<V>> values() {
        return targetMap.values();
    }

    @Override public Set<Map.Entry<K, List<V>>> entrySet() {
        return targetMap.entrySet();
    }

    @Override public void forEach(BiConsumer<? super K, ? super List<V>> action) {
        targetMap.forEach(action);
    }

    @Override public boolean equals(Object other) {
        return this == other || targetMap.equals(other);
    }

    @Override public int hashCode() {
        return targetMap.hashCode();
    }

    @Override public String toString() {
        return targetMap.toString();
    }

    // ------------------------------------------------------------------ live first-value view

    /** Spring 6.2's first-value protocol over any MultiValueMap; owned body. */
    private static final class SingleValueView<K, V> implements Map<K, V>, Serializable {
        @Serial private static final long serialVersionUID = 0L;
        @SuppressWarnings("serial")
        private final MultiValueMap<K, V> target;
        private transient Collection<V> values;
        private transient Set<Map.Entry<K, V>> entries;

        SingleValueView(MultiValueMap<K, V> target) {
            this.target = target;
        }

        @Override public int size() {
            return target.size();
        }

        @Override public boolean isEmpty() {
            return target.isEmpty();
        }

        @Override public boolean containsKey(Object key) {
            return target.containsKey(key);
        }

        /** Reads index zero of every list in encounter order; a null or empty list fails as Spring's walk does. */
        @Override public boolean containsValue(Object value) {
            for (List<V> list : target.values()) {
                V v = list.get(0);
                if (value == null ? v == null : value.equals(v)) { return true; }
            }
            return false;
        }

        @Override public V get(Object key) {
            return first(target.get(key));
        }

        @Override public V put(K key, V value) {
            return first(target.put(key, value != null ? Collections.singletonList(value) : null));
        }

        @Override public V remove(Object key) {
            return first(target.remove(key));
        }

        @Override public void putAll(Map<? extends K, ? extends V> map) {
            for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) { put(e.getKey(), e.getValue()); }
        }

        @Override public void clear() {
            target.clear();
        }

        @Override public Set<K> keySet() {
            return target.keySet();
        }

        @Override public Collection<V> values() {
            Collection<V> v = values;
            return v != null ? v : (values = new FirstValues<>(target.values()));
        }

        @Override public Set<Map.Entry<K, V>> entrySet() {
            Set<Map.Entry<K, V>> e = entries;
            return e != null ? e : (entries = new FirstEntries<>(target.entrySet()));
        }

        @Override public void forEach(BiConsumer<? super K, ? super V> action) {
            target.forEach((k, vs) -> action.accept(k, vs.get(0)));
        }

        /** AbstractMap equality over first values; NPE and CCE answer false, an empty list still fails. */
        @Override public boolean equals(Object other) {
            if (this == other) { return true; }
            if (!(other instanceof Map<?, ?> m) || size() != m.size()) { return false; }
            try {
                for (Map.Entry<K, List<V>> e : target.entrySet()) {
                    K key = e.getKey();
                    V value = e.getValue().get(0);
                    if (value == null ? m.get(key) != null || !m.containsKey(key) : !value.equals(m.get(key))) { return false; }
                }
                return true;
            } catch (ClassCastException | NullPointerException unused) {
                return false;
            }
        }

        @Override public int hashCode() {
            return target.hashCode();
        }

        @Override public String toString() {
            return target.toString();
        }
    }

    /** First element of each list, read at {@code next()}; removal is unsupported. */
    private static final class FirstValues<V> extends AbstractCollection<V> {
        private final Collection<List<V>> lists;

        FirstValues(Collection<List<V>> lists) {
            this.lists = lists;
        }

        @Override public Iterator<V> iterator() {
            Iterator<List<V>> it = lists.iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() {
                    return it.hasNext();
                }

                @Override public V next() {
                    return it.next().get(0);
                }
            };
        }

        @Override public int size() {
            return lists.size();
        }
    }

    /** Immutable first-value entries created at {@code next()} (the only boundary entries); removal is unsupported. */
    private static final class FirstEntries<K, V> extends AbstractSet<Map.Entry<K, V>> {
        private final Set<Map.Entry<K, List<V>>> lists;

        FirstEntries(Set<Map.Entry<K, List<V>>> lists) {
            this.lists = lists;
        }

        @Override public Iterator<Map.Entry<K, V>> iterator() {
            Iterator<Map.Entry<K, List<V>>> it = lists.iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() {
                    return it.hasNext();
                }

                @Override public Map.Entry<K, V> next() {
                    Map.Entry<K, List<V>> e = it.next();
                    return new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue().get(0));
                }
            };
        }

        @Override public int size() {
            return lists.size();
        }
    }
}
