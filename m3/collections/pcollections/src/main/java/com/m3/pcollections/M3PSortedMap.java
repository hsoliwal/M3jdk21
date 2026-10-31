/*******************************************************************************
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
package com.m3.pcollections;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import org.pcollections.AbstractUnmodifiableMap;
import org.pcollections.AbstractUnmodifiableSet;
import org.pcollections.PSortedMap;

/**
 * Persistent sorted map with aligned M3 key/value vector lanes. There is no retained
 * entry object per mapping. Comparator-equivalent updates preserve the original key
 * and store the supplied value reference. Ranges are membership snapshots without
 * fences; descending views change only order metadata. A key set shares only keys.
 * Null values and comparator-admitted null keys are supported. Empty firstEntry and
 * lastEntry return null, as specified by NavigableMap.
 */
public final class M3PSortedMap<K, V> extends AbstractUnmodifiableMap<K, V>
        implements PSortedMap<K, V>, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final M3PSortedSet<K> keys;
    private final M3PVector<V> values;

    private M3PSortedMap(M3PSortedSet<K> keys, M3PVector<V> values) {
        this.keys = keys;
        this.values = values;
    }
    public static <K extends Comparable<? super K>, V> M3PSortedMap<K, V> empty() {
        return empty(Comparator.naturalOrder());
    }
    public static <K, V> M3PSortedMap<K, V> empty(Comparator<? super K> comparator) {
        return new M3PSortedMap<>(M3PSortedSet.empty(comparator), M3PVector.empty());
    }
    public static <K extends Comparable<? super K>, V> M3PSortedMap<K, V> singleton(K key, V value) {
        return M3PSortedMap.<K, V>empty().plus(key, value);
    }
    public static <K, V> M3PSortedMap<K, V> singleton(Comparator<? super K> comparator, K key, V value) {
        return M3PSortedMap.<K, V>empty(comparator).plus(key, value);
    }
    public static <K extends Comparable<? super K>, V> M3PSortedMap<K, V> from(Map<? extends K, ? extends V> source) {
        return M3PSortedMap.<K, V>empty().plusAll(source);
    }
    public static <K, V> M3PSortedMap<K, V> from(Comparator<? super K> comparator,
            Map<? extends K, ? extends V> source) {
        return M3PSortedMap.<K, V>empty(comparator).plusAll(source);
    }

    @Override public int size() { return keys.size(); }
    @Override public Comparator<? super K> comparator() { return keys.comparator(); }
    @Override public boolean containsKey(Object key) { return keys.position(key) >= 0; }
    @Override public V get(Object key) {
        int index = keys.position(key);
        return index < 0 ? null : values.get(index);
    }
    @Override public M3PSortedMap<K, V> plus(K key, V value) {
        int index = keys.bound(key, false);
        if (index < size() && keys.order.compare(keys.elements.get(index), key) == 0) {
            M3PVector<V> changed = values.with(index, value);
            return changed == values ? this : new M3PSortedMap<>(keys, changed);
        }
        return new M3PSortedMap<>(keys.inserted(index, key), values.plus(index, value));
    }
    @Override public M3PSortedMap<K, V> plusAll(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        M3PSortedMap<K, V> result = this;
        for (Entry<? extends K, ? extends V> entry : source.entrySet())
            result = result.plus(entry.getKey(), entry.getValue());
        return result;
    }
    private M3PSortedMap<K, V> removed(int index) {
        return new M3PSortedMap<>(keys.removed(index), values.minus(index));
    }
    @Override public M3PSortedMap<K, V> minus(Object key) {
        int index = keys.position(key);
        return index < 0 ? this : removed(index);
    }
    @Override public M3PSortedMap<K, V> minusAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        M3PSortedMap<K, V> result = this;
        for (Object key : source) result = result.minus(key);
        return result;
    }
    @Override public M3PSortedMap<K, V> minusFirstEntry() {
        if (isEmpty()) throw new NoSuchElementException();
        return removed(keys.descending ? size() - 1 : 0);
    }
    @Override public M3PSortedMap<K, V> minusLastEntry() {
        if (isEmpty()) throw new NoSuchElementException();
        return removed(keys.descending ? 0 : size() - 1);
    }

    private Entry<K, V> entryAt(int index) {
        return index < 0 || index >= size() ? null
                : new SimpleImmutableEntry<>(keys.elements.get(index), values.get(index));
    }
    private Entry<K, V> neighbor(K key, int mode) { return entryAt(keys.neighbor(key, mode)); }
    private K neighborKey(K key, int mode) {
        int index = keys.neighbor(key, mode);
        return index < 0 || index >= size() ? null : keys.elements.get(index);
    }
    @Override public Entry<K, V> lowerEntry(K key) { return neighbor(key, 0); }
    @Override public Entry<K, V> floorEntry(K key) { return neighbor(key, 1); }
    @Override public Entry<K, V> ceilingEntry(K key) { return neighbor(key, 2); }
    @Override public Entry<K, V> higherEntry(K key) { return neighbor(key, 3); }
    @Override public K lowerKey(K key) { return neighborKey(key, 0); }
    @Override public K floorKey(K key) { return neighborKey(key, 1); }
    @Override public K ceilingKey(K key) { return neighborKey(key, 2); }
    @Override public K higherKey(K key) { return neighborKey(key, 3); }
    @Override public K firstKey() { return keys.first(); }
    @Override public K lastKey() { return keys.last(); }
    @Override public Entry<K, V> firstEntry() { return entryAt(keys.descending ? size() - 1 : 0); }
    @Override public Entry<K, V> lastEntry() { return entryAt(keys.descending ? 0 : size() - 1); }

    private M3PSortedMap<K, V> slice(int from, int to) {
        int end = Math.max(from, to);
        if (from == 0 && end == size()) return this;
        return new M3PSortedMap<>(keys.slice(from, end), values.subList(from, end));
    }
    @Override public M3PSortedMap<K, V> headMap(K key, boolean inclusive) {
        int boundary = keys.headBoundary(key, inclusive);
        return keys.descending ? slice(boundary, size()) : slice(0, boundary);
    }
    @Override public M3PSortedMap<K, V> tailMap(K key, boolean inclusive) {
        int boundary = keys.tailBoundary(key, inclusive);
        return keys.descending ? slice(0, boundary) : slice(boundary, size());
    }
    @Override public M3PSortedMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive) {
        if (comparator().compare(from, to) > 0) throw new IllegalArgumentException("reversed endpoints");
        int start = keys.descending ? keys.bound(to, !toInclusive) : keys.bound(from, !fromInclusive);
        int end = keys.descending ? keys.bound(from, fromInclusive) : keys.bound(to, toInclusive);
        return slice(start, end);
    }
    @Override public M3PSortedMap<K, V> headMap(K key) { return headMap(key, false); }
    @Override public M3PSortedMap<K, V> tailMap(K key) { return tailMap(key, true); }
    @Override public M3PSortedMap<K, V> subMap(K from, K to) { return subMap(from, true, to, false); }
    @Override public M3PSortedMap<K, V> descendingMap() { return new M3PSortedMap<>(keys.descendingSet(), values); }
    @Override public M3PSortedMap<K, V> reversed() { return descendingMap(); }
    @Override public M3PSortedSet<K> keySet() { return keys; }
    @Override public M3PSortedSet<K> navigableKeySet() { return keys; }
    @Override public M3PSortedSet<K> descendingKeySet() { return keys.descendingSet(); }
    @Override public Collection<V> values() {
        return Collections.unmodifiableCollection(keys.descending ? values.reversed() : values);
    }
    @Override public M3PSortedSet<K> sequencedKeySet() { return keys; }
    @Override public SequencedCollection<V> sequencedValues() {
        return Collections.unmodifiableSequencedCollection(PSortedMap.super.sequencedValues());
    }
    @Override public SequencedSet<Entry<K, V>> sequencedEntrySet() {
        return Collections.unmodifiableSequencedSet(PSortedMap.super.sequencedEntrySet());
    }

    @Override public Set<Entry<K, V>> entrySet() {
        return new AbstractUnmodifiableSet<>() {
            @Override public int size() { return M3PSortedMap.this.size(); }
            @Override public Spliterator<Entry<K, V>> spliterator() {
                return Spliterators.spliterator(this,
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE);
            }
            @Override public boolean contains(Object value) {
                if (!(value instanceof Entry<?, ?> entry)) return false;
                int index = keys.position(entry.getKey());
                return index >= 0 && Objects.equals(values.get(index), entry.getValue());
            }
            @Override public Iterator<Entry<K, V>> iterator() {
                return new Iterator<>() {
                    private int next;
                    @Override public boolean hasNext() { return next < size(); }
                    @Override public Entry<K, V> next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        int index = keys.descending ? size() - 1 - next++ : next++;
                        return entryAt(index);
                    }
                };
            }
        };
    }
    @Deprecated @Override public boolean remove(Object key, Object value) { throw new UnsupportedOperationException(); }
    @Deprecated @Override public boolean replace(K key, V oldValue, V newValue) { throw new UnsupportedOperationException(); }
}
