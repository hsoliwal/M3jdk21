/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiConsumer;

/** Sorted arrays with live half-open range views. Search O(log n), insert/remove O(n).
 * Implements SortedMap, not NavigableMap. Comparators must remain stable and lawful.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/collections/flat/FlatSortedMap.java}
 * blob {@code c8ac0270a75d44b17ba76e6dce6462bdae2ee87c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FlatSortedMap<K, V> extends AbstractMap<K, V> implements SortedMap<K, V> {
    private final FlatSortedMap<K, V> root;
    private FlatStore<K> keys;
    private FlatStore<V> values;
    private final Comparator<? super K> comparator;
    private final boolean hasLow;
    private final boolean hasHigh;
    private final K low;
    private final K high;
    private int count;
    private int version;

    public FlatSortedMap(StorageType<K> keyType, StorageType<V> valueType) { this(keyType, valueType, null); }
    public FlatSortedMap(StorageType<K> keyType, StorageType<V> valueType, Comparator<? super K> comparator) {
        root = this; keys = new FlatStore<>(keyType, 8); values = new FlatStore<>(valueType, 8);
        this.comparator = comparator; hasLow = hasHigh = false; low = high = null;
    }
    private FlatSortedMap(FlatSortedMap<K, V> parent, boolean hasLow, K low, boolean hasHigh, K high) {
        root = parent.root; comparator = root.comparator;
        this.hasLow = hasLow; this.low = low; this.hasHigh = hasHigh; this.high = high;
    }
    @SuppressWarnings("unchecked")
    private int compare(K a, K b) {
        if (comparator != null) return comparator.compare(a, b);
        return ((Comparable<? super K>) Objects.requireNonNull(a)).compareTo(b);
    }
    private int lowerBound(K key) {
        compare(key, key);
        int a = 0, b = root.count;
        while (a < b) { int m = (a + b) >>> 1; if (compare(root.keys.get(m), key) < 0) a = m + 1; else b = m; }
        return a;
    }
    private int from() { return hasLow ? lowerBound(low) : 0; }
    private int to() { return hasHigh ? lowerBound(high) : root.count; }
    private boolean inRange(K key) {
        return (!hasLow || compare(key, low) >= 0) && (!hasHigh || compare(key, high) < 0);
    }
    @SuppressWarnings("unchecked")
    private int index(Object key) {
        K typed = (K) key;
        int i = lowerBound(typed);
        return i < root.count && inRange(typed) && compare(root.keys.get(i), typed) == 0 ? i : -1;
    }
    @Override public int size() { return Math.max(0, to() - from()); }
    @Override public Comparator<? super K> comparator() { return comparator; }
    @Override public boolean containsKey(Object key) { return index(key) >= 0; }
    @Override public V get(Object key) { int i = index(key); return i < 0 ? null : root.values.get(i); }
    @Override public V put(K key, V value) {
        root.keys.type.validate(key); root.values.type.validate(value); compare(key, key);
        if (!inRange(key)) throw new IllegalArgumentException("Key outside view");
        int i = lowerBound(key);
        if (i < root.count && compare(root.keys.get(i), key) == 0) {
            V old = root.values.get(i); root.values.set(i, value); return old;
        }
        if (root.count == root.keys.capacity) {
            int capacity = FlatStore.grow(root.count, root.count + 1);
            FlatStore<K> newKeys = root.keys.resized(capacity, root.count);
            FlatStore<V> newValues = root.values.resized(capacity, root.count);
            root.keys = newKeys; root.values = newValues;
        }
        root.keys.move(i, i + 1, root.count - i); root.values.move(i, i + 1, root.count - i);
        root.keys.set(i, key); root.values.set(i, value); root.count++; root.version++; return null;
    }
    private void delete(int index) {
        root.keys.move(index + 1, index, root.count - index - 1);
        root.values.move(index + 1, index, root.count - index - 1);
        root.count--; root.keys.clear(root.count); root.values.clear(root.count); root.version++;
    }
    @Override public V remove(Object key) {
        int i = index(key); if (i < 0) return null;
        V old = root.values.get(i); delete(i); return old;
    }
    @Override public K firstKey() { int a = from(); if (a >= to()) throw new NoSuchElementException(); return root.keys.get(a); }
    @Override public K lastKey() { int b = to(); if (b <= from()) throw new NoSuchElementException(); return root.keys.get(b - 1); }
    private void endpoint(K key) {
        compare(key, key);
        if ((hasLow && compare(key, low) < 0) || (hasHigh && compare(key, high) > 0)) {
            throw new IllegalArgumentException("Endpoint outside view");
        }
    }
    @Override public SortedMap<K, V> subMap(K fromKey, K toKey) {
        endpoint(fromKey); endpoint(toKey);
        if (hasHigh && compare(fromKey, high) == 0) throw new IllegalArgumentException("Exclusive high bound");
        if (compare(fromKey, toKey) > 0) throw new IllegalArgumentException("Reversed bounds");
        return new FlatSortedMap<>(this, true, fromKey, true, toKey);
    }
    @Override public SortedMap<K, V> headMap(K toKey) {
        endpoint(toKey); return new FlatSortedMap<>(this, hasLow, low, true, toKey);
    }
    @Override public SortedMap<K, V> tailMap(K fromKey) {
        endpoint(fromKey);
        if (hasHigh && compare(fromKey, high) == 0) throw new IllegalArgumentException("Exclusive high bound");
        return new FlatSortedMap<>(this, true, fromKey, hasHigh, high);
    }
    @Override public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action); int expected = root.version, end = to();
        for (int i = from(); i < end; i++) {
            action.accept(root.keys.get(i), root.values.get(i));
            if (expected != root.version) throw new ConcurrentModificationException();
        }
    }
    @Override public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return FlatSortedMap.this.size(); }
            @Override public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<>() {
                    private int cursor = from();
                    private int returned = -1;
                    private int expected = root.version;
                    private void check() { if (expected != root.version) throw new ConcurrentModificationException(); }
                    @Override public boolean hasNext() { check(); return cursor < to(); }
                    @Override public Map.Entry<K, V> next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        returned = cursor++; K key = root.keys.get(returned);
                        return new Map.Entry<>() {
                            @Override public K getKey() { return key; }
                            @Override public V getValue() { return FlatSortedMap.this.get(key); }
                            @Override public V setValue(V value) {
                                if (!containsKey(key)) throw new IllegalStateException("Entry removed");
                                return put(key, value);
                            }
                            @Override public int hashCode() { return Objects.hashCode(key) ^ Objects.hashCode(getValue()); }
                            @Override public boolean equals(Object other) {
                                return other instanceof Map.Entry<?, ?> e && Objects.equals(key, e.getKey()) && Objects.equals(getValue(), e.getValue());
                            }
                            @Override public String toString() { return key + "=" + getValue(); }
                        };
                    }
                    @Override public void remove() {
                        check(); if (returned < 0) throw new IllegalStateException();
                        delete(returned); cursor--; returned = -1; expected = root.version;
                    }
                };
            }
            @Override public boolean contains(Object value) {
                if (!(value instanceof Map.Entry<?, ?> e)) return false;
                int i = index(e.getKey()); return i >= 0 && root.values.equalsAt(i, e.getValue());
            }
            @Override public boolean remove(Object value) {
                if (!(value instanceof Map.Entry<?, ?> e)) return false;
                int i = index(e.getKey());
                if (i < 0 || !root.values.equalsAt(i, e.getValue())) return false;
                delete(i); return true;
            }
        };
    }
}
