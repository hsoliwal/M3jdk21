// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Atomic ConcurrentMap operations over a packed map under one shared monitor.
 * This is a blocking alternative, not a lock-free implementation. Nulls are rejected.
 * Iterators snapshot parallel key/value lanes and conditionally remove observed mappings.
 * Callbacks run under the monitor; avoid externally conflicting lock orders.
 */
public class M3LockedMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    final M3SlotMap<K, V> map;
    final Object mutex;
    /** Empty hash-backed concurrent map. */
    public M3LockedMap() { this(new M3LinkedHashMap<>(), new Object()); }
    M3LockedMap(M3SlotMap<K, V> map, Object mutex) { this.map = map; this.mutex = mutex; }
    static <T> T nonnull(T value) { return Objects.requireNonNull(value); }
    @Override public int size() { synchronized (mutex) { return map.size(); } }
    @Override public boolean isEmpty() { synchronized (mutex) { return map.isEmpty(); } }
    @Override public V get(Object key) { synchronized (mutex) { return map.get(nonnull(key)); } }
    @Override public V getOrDefault(Object key, V fallback) { synchronized (mutex) { return map.getOrDefault(nonnull(key), fallback); } }
    @Override public boolean containsKey(Object key) { synchronized (mutex) { return map.containsKey(nonnull(key)); } }
    @Override public boolean containsValue(Object value) { synchronized (mutex) { return map.containsValue(nonnull(value)); } }
    @Override public V put(K key, V value) { synchronized (mutex) { return map.put(nonnull(key), nonnull(value)); } }
    @Override public V putIfAbsent(K key, V value) { synchronized (mutex) { return map.putIfAbsent(nonnull(key), nonnull(value)); } }
    @Override public V remove(Object key) { synchronized (mutex) { return map.remove(nonnull(key)); } }
    @Override public boolean remove(Object key, Object value) {
        nonnull(key); if (value == null) { return false; }
        synchronized (mutex) { return map.remove(key, value); }
    }
    @Override public V replace(K key, V value) { synchronized (mutex) { return map.replace(nonnull(key), nonnull(value)); } }
    @Override public boolean replace(K key, V old, V value) { synchronized (mutex) { return map.replace(nonnull(key), nonnull(old), nonnull(value)); } }
    @Override public void clear() { synchronized (mutex) { map.clear(); } }
    @Override public void putAll(Map<? extends K, ? extends V> source) { source.forEach(this::put); }
    @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> action) {
        synchronized (mutex) { return map.computeIfAbsent(nonnull(key), nonnull(action)); }
    }
    @Override public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> action) {
        synchronized (mutex) { return map.computeIfPresent(nonnull(key), nonnull(action)); }
    }
    @Override public V compute(K key, BiFunction<? super K, ? super V, ? extends V> action) {
        synchronized (mutex) { return map.compute(nonnull(key), nonnull(action)); }
    }
    @Override public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> action) {
        synchronized (mutex) { return map.merge(nonnull(key), nonnull(value), nonnull(action)); }
    }
    @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> action) {
        nonnull(action); synchronized (mutex) { map.replaceAll((k, v) -> nonnull(action.apply(k, v))); }
    }
    @Override public void forEach(BiConsumer<? super K, ? super V> action) { synchronized (mutex) { map.forEach(nonnull(action)); } }
    @Override public int hashCode() { synchronized (mutex) { return map.hashCode(); } }
    @Override public boolean equals(Object other) { if (other == this) { return true; } synchronized (mutex) { return map.equals(other); } }
    @Override public String toString() {
        synchronized (mutex) {
            StringBuilder text = new StringBuilder("{"); boolean comma = false;
            for (int s = map.firstSlot(); s >= 0; s = map.nextSlot(s)) {
                if (comma) { text.append(", "); } comma = true;
                K key = map.keyAt(s); V value = map.valueAt(s);
                text.append(key == this ? "(this Map)" : key).append('=').append(value == this ? "(this Map)" : value);
            }
            return text.append('}').toString();
        }
    }
    private Object[] snapshot() {
        synchronized (mutex) {
            Object[] lanes = new Object[Math.multiplyExact(map.size(), 2)]; int i = 0;
            for (int s = map.firstSlot(); s >= 0; s = map.nextSlot(s)) { lanes[i++] = map.keyAt(s); lanes[i++] = map.valueAt(s); }
            return lanes;
        }
    }
    final <T> Iterator<T> iterator(int kind) {
        Object[] lanes = snapshot();
        return new Iterator<>() {
            private int at, last = -1;
            @Override public boolean hasNext() { return at < lanes.length; }
            @Override @SuppressWarnings("unchecked") public T next() {
                if (!hasNext()) { throw new NoSuchElementException(); }
                last = at; K key = M3SlotArrays.get(lanes, at++); V value = M3SlotArrays.get(lanes, at++);
                Object result = kind == 0 ? key : kind == 1 ? value : new SnapshotEntry(key, value);
                return (T) result;
            }
            @Override public void remove() {
                if (last < 0) { throw new IllegalStateException(); }
                M3LockedMap.this.remove(lanes[last], lanes[last + 1]); last = -1;
            }
        };
    }
    private final class SnapshotEntry implements Entry<K, V> {
        private final K key;
        private V value;
        SnapshotEntry(K key, V value) { this.key = key; this.value = value; }
        @Override public K getKey() { return key; }
        @Override public V getValue() { return value; }
        @Override public V setValue(V replacement) {
            nonnull(replacement); V old = value; M3LockedMap.this.put(key, replacement); value = replacement; return old;
        }
        @Override public int hashCode() { return Objects.hashCode(key) ^ Objects.hashCode(value); }
        @Override public boolean equals(Object o) { return o instanceof Entry<?, ?> e && Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue()); }
        @Override public String toString() { return key + "=" + value; }
    }
    @Override public Set<K> keySet() {
        return new AbstractSet<>() {
            @Override public int size() { return M3LockedMap.this.size(); }
            @Override public boolean contains(Object k) { return containsKey(k); }
            @Override public boolean remove(Object k) { return M3LockedMap.this.remove(k) != null; }
            @Override public void clear() { M3LockedMap.this.clear(); }
            @Override public Spliterator<K> spliterator() { return M3SlotSpliterators.concurrent(iterator(), false, true); }
            @Override public Iterator<K> iterator() { return M3LockedMap.this.iterator(0); }
        };
    }
    @Override public Collection<V> values() {
        return new AbstractCollection<>() {
            @Override public int size() { return M3LockedMap.this.size(); }
            @Override public boolean contains(Object v) { return containsValue(v); }
            @Override public void clear() { M3LockedMap.this.clear(); }
            @Override public Spliterator<V> spliterator() { return M3SlotSpliterators.concurrent(iterator(), false, false); }
            @Override public Iterator<V> iterator() { return M3LockedMap.this.iterator(1); }
        };
    }
    @Override public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return M3LockedMap.this.size(); }
            @Override public void clear() { M3LockedMap.this.clear(); }
            @Override public Spliterator<Entry<K, V>> spliterator() { return M3SlotSpliterators.concurrent(iterator(), false, true); }
            @Override public Iterator<Entry<K, V>> iterator() { return M3LockedMap.this.iterator(2); }
            @Override public boolean contains(Object o) {
                if (!(o instanceof Entry<?, ?> e) || e.getKey() == null || e.getValue() == null) { return false; }
                return Objects.equals(M3LockedMap.this.get(e.getKey()), e.getValue());
            }
            @Override public boolean remove(Object o) {
                return o instanceof Entry<?, ?> e && e.getKey() != null && M3LockedMap.this.remove(e.getKey(), e.getValue());
            }
        };
    }
}
