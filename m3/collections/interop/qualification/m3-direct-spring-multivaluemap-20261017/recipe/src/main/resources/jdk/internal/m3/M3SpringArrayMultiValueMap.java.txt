/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import org.springframework.util.MultiValueMap;

/**
 * Direct Spring 6.2 MultiValueMap implementation, not a Spring adapter subclass.
 *
 * <p>One incumbent M3 array/index map owns keys and user-provided list references.
 * New value lists are M3LinkedList instances over integer slot links. External
 * lists passed to put/putAll retain their original reference and mutability,
 * preserving the Spring Map-of-lists alias contract. Null list mappings are
 * distinct from absent keys. Every Map view stays connected to the same directory.
 *
 * <p>Only API-mandated iterator/entry projections allocate transient objects.
 * Neither Java object payloads nor Java callbacks enter JNI. This class is not
 * thread-safe and does not claim Spring concrete serialized-form compatibility.
 */
public final class M3SpringArrayMultiValueMap<K, V> extends AbstractMap<K, List<V>>
        implements MultiValueMap<K, V> {

    private final Map<K, List<V>> directory = new M3LinkedHashMap<>();

    public M3SpringArrayMultiValueMap() { }

    private static <V> V first(List<V> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override public V getFirst(K key) { return first(directory.get(key)); }

    @Override public void add(K key, V value) {
        directory.computeIfAbsent(key, ignored -> new M3LinkedList<>()).add(value);
    }

    @Override public void addAll(K key, List<? extends V> values) {
        // Exactly the mapped-list alias semantics of Spring's Map-of-lists.
        directory.computeIfAbsent(key, ignored -> new M3LinkedList<>()).addAll(values);
    }

    @Override public void addAll(MultiValueMap<K, V> values) {
        values.forEach(this::addAll);
    }

    @Override public void set(K key, V value) {
        M3LinkedList<V> list = new M3LinkedList<>();
        list.add(value);
        directory.put(key, list);
    }

    @Override public void setAll(Map<K, V> values) {
        values.forEach(this::set);
    }

    /** Detached first-value projection, backed by an independent M3 map. */
    @Override public Map<K, V> toSingleValueMap() {
        Map<K, V> result = new M3LinkedHashMap<>();
        directory.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) result.put(key, values.get(0));
        });
        return result;
    }

    /**
     * Direct live first-value view; does not call Spring's
     * MultiToSingleValueMapAdapter. Its input/output mutation semantics are
     * independently matched to Spring 6.2's view contract.
     */
    @Override public Map<K, V> asSingleValueMap() {
        return new FirstValueView();
    }

    @Override public int size() { return directory.size(); }
    @Override public boolean isEmpty() { return directory.isEmpty(); }
    @Override public boolean containsKey(Object key) { return directory.containsKey(key); }
    @Override public boolean containsValue(Object value) { return directory.containsValue(value); }
    @Override public List<V> get(Object key) { return directory.get(key); }
    @Override public List<V> put(K key, List<V> value) { return directory.put(key, value); }
    @Override public List<V> putIfAbsent(K key, List<V> value) { return directory.putIfAbsent(key, value); }
    @Override public List<V> remove(Object key) { return directory.remove(key); }
    @Override public void putAll(Map<? extends K, ? extends List<V>> input) { directory.putAll(input); }
    @Override public void clear() { directory.clear(); }
    @Override public Set<K> keySet() { return directory.keySet(); }
    @Override public Collection<List<V>> values() { return directory.values(); }
    @Override public Set<Entry<K, List<V>>> entrySet() { return directory.entrySet(); }
    @Override public void forEach(BiConsumer<? super K, ? super List<V>> action) {
        directory.forEach(action);
    }
    @Override public boolean equals(Object other) { return this == other || directory.equals(other); }
    @Override public int hashCode() { return directory.hashCode(); }
    @Override public String toString() { return directory.toString(); }

    private final class FirstValueView extends AbstractMap<K, V> {
        @Override public int size() { return directory.size(); }
        @Override public boolean containsKey(Object key) { return directory.containsKey(key); }
        @Override public V get(Object key) { return first(directory.get(key)); }
        @Override public V put(K key, V value) {
            // Spring's view writes a singleton list, or a null list for null.
            List<V> replacement = value == null ? null : Collections.singletonList(value);
            return first(directory.put(key, replacement));
        }
        @Override public V remove(Object key) { return first(directory.remove(key)); }
        @Override public void clear() { directory.clear(); }
        @Override public Set<K> keySet() { return directory.keySet(); }
        @Override public void forEach(BiConsumer<? super K, ? super V> action) {
            Objects.requireNonNull(action, "action");
            directory.forEach((key, list) -> action.accept(key, list.get(0)));
        }
        @Override public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return directory.size(); }
                @Override public Iterator<Entry<K, V>> iterator() {
                    Iterator<Entry<K, List<V>>> source = directory.entrySet().iterator();
                    return new Iterator<>() {
                        @Override public boolean hasNext() { return source.hasNext(); }
                        @Override public Entry<K, V> next() {
                            Entry<K, List<V>> entry = source.next();
                            return new SimpleImmutableEntry<>(entry.getKey(), entry.getValue().get(0));
                        }
                    };
                }
            };
        }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Map<?, ?> map) || map.size() != size()) return false;
            try {
                for (Entry<K, List<V>> e : directory.entrySet()) {
                    V value = e.getValue().get(0);
                    Object peer = map.get(e.getKey());
                    if (!Objects.equals(value, peer)
                            || value == null && peer == null && !map.containsKey(e.getKey())) return false;
                }
                return true;
            } catch (ClassCastException | NullPointerException malformed) {
                return false;
            }
        }
        // Spring 6.2's first-value view retains the underlying multivalue hash/string contract.
        @Override public int hashCode() { return directory.hashCode(); }
        @Override public String toString() { return directory.toString(); }
    }
}
