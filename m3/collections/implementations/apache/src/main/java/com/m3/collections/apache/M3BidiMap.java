/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// Modified 2026: Apache dual-map protocol over owned M3 array/index storage and API views.
package com.m3.collections.apache;

import com.m3.collections.M3LinkedHashMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.ResettableIterator;

/**
 * Bidirectional insertion-order map with two owned M3 payload/index owners.
 *
 * <p>Mutation, inverse and view protocols are adapted from Apache Commons Collections 4.5
 * AbstractDualBidiMap. Null keys and values are supported. A put evicts any other key holding
 * its value. The normal and inverse directions have their own insertion orders and share the
 * same two owners. This map is not thread-safe.</p>
 *
 * <p>The pinned provider's setter identity check is intentional. Entry.setValue returns the
 * installed value, while MapIterator.setValue returns the previous value. The values view
 * also retains the provider's collection-identity equality/hash behavior despite its Set API.
 * Entry objects are created only by the entry-set API; the map iterator holds a scalar key
 * and a validity flag, and reads its current value from the owner. As in the pinned
 * provider, entry-set array overloads expose raw owner API entries; their setValue writes
 * only the forward owner and bypasses reverse maintenance. Iterator/stream/forEach entries
 * retain the guarded setter protocol.</p>
 *
 * <p>Entries and iterators used after external structural mutation are outside this candidate's
 * qualification boundary. No serialization or complete Commons Collections library coverage
 * is claimed. Public M3 owner signatures and storage internals are unchanged.</p>
 */
public final class M3BidiMap<K, V> implements BidiMap<K, V> {
    private final M3LinkedHashMap<K, V> forward;
    private final M3LinkedHashMap<V, K> reverse;
    private M3BidiMap<V, K> inverse;
    private Set<K> keys;
    private Set<V> values;
    private Set<Map.Entry<K, V>> entries;

    /** Construct an empty insertion-order bidirectional owner. */
    public M3BidiMap() {
        this(new M3LinkedHashMap<>(), new M3LinkedHashMap<>(), null);
    }

    /** Copy mappings in the source's entry iteration order using the ordinary put protocol. */
    public M3BidiMap(final Map<? extends K, ? extends V> source) {
        this();
        putAll(source);
    }

    private M3BidiMap(final M3LinkedHashMap<K, V> forward,
            final M3LinkedHashMap<V, K> reverse, final M3BidiMap<V, K> inverse) {
        this.forward = forward;
        this.reverse = reverse;
        this.inverse = inverse;
    }

    @Override
    public int size() {
        return forward.size();
    }

    @Override
    public boolean isEmpty() {
        return forward.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return forward.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return reverse.containsKey(value);
    }

    @Override
    public V get(final Object key) {
        return forward.get(key);
    }

    @Override
    public K getKey(final Object value) {
        return reverse.get(value);
    }

    @Override
    public V put(final K key, final V value) {
        if (forward.containsKey(key)) {
            reverse.remove(forward.get(key));
        }
        if (reverse.containsKey(value)) {
            forward.remove(reverse.get(value));
        }
        final V previous = forward.put(key, value);
        reverse.put(value, key);
        return previous;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> source) {
        for (final Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(final Object key) {
        V value = null;
        if (forward.containsKey(key)) {
            value = forward.remove(key);
            reverse.remove(value);
        }
        return value;
    }

    @Override
    public K removeValue(final Object value) {
        K key = null;
        if (reverse.containsKey(value)) {
            key = reverse.remove(value);
            forward.remove(key);
        }
        return key;
    }

    @Override
    public void clear() {
        forward.clear();
        reverse.clear();
    }

    @Override
    public BidiMap<V, K> inverseBidiMap() {
        if (inverse == null) {
            inverse = new M3BidiMap<>(reverse, forward, this);
        }
        return inverse;
    }

    @Override
    public Set<K> keySet() {
        if (keys == null) {
            keys = new KeysView();
        }
        return keys;
    }

    @Override
    public Set<V> values() {
        if (values == null) {
            values = new ValuesView();
        }
        return values;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (entries == null) {
            entries = new EntriesView();
        }
        return entries;
    }

    @Override
    public MapIterator<K, V> mapIterator() {
        return new BidiCursor();
    }

    @Override
    public boolean equals(final Object other) {
        return forward.equals(other);
    }

    @Override
    public int hashCode() {
        return forward.hashCode();
    }

    @Override
    public String toString() {
        return forward.toString();
    }

    private void requireSetterValue(final K key, final V value) {
        // Deliberately retain the donor reference comparison, including equal distinct keys.
        if (reverse.containsKey(value) && reverse.get(value) != key) {
            throw new IllegalArgumentException(
                    "Cannot use setValue() when the object being set is already in the map");
        }
    }

    /** Donor View bulk semantics, with owned JDK view objects instead of provider decorators. */
    private abstract class OwnedView<E> extends AbstractSet<E> {
        final Collection<E> ownerView;

        OwnedView(final Collection<E> ownerView) {
            this.ownerView = Objects.requireNonNull(ownerView, "collection");
        }

        @Override
        public int size() {
            return ownerView.size();
        }

        @Override
        public boolean isEmpty() {
            return ownerView.isEmpty();
        }

        @Override
        public void clear() {
            M3BidiMap.this.clear();
        }

        @Override
        public boolean add(final E element) {
            return ownerView.add(element);
        }

        @Override
        public boolean addAll(final Collection<? extends E> collection) {
            return ownerView.addAll(collection);
        }

        @Override
        public boolean containsAll(final Collection<?> collection) {
            return ownerView.containsAll(collection);
        }

        @Override
        public Object[] toArray() {
            return ownerView.toArray();
        }

        @Override
        public <T> T[] toArray(final T[] array) {
            return ownerView.toArray(array);
        }

        @Override
        public boolean equals(final Object other) {
            return other == this || ownerView.equals(other);
        }

        @Override
        public int hashCode() {
            return ownerView.hashCode();
        }

        @Override
        public String toString() {
            return ownerView.toString();
        }

        @Override
        public boolean removeAll(final Collection<?> collection) {
            if (M3BidiMap.this.isEmpty() || collection.isEmpty()) {
                return false;
            }
            boolean modified = false;
            for (final Object current : collection) {
                modified |= remove(current);
            }
            return modified;
        }

        @Override
        public boolean removeIf(final Predicate<? super E> filter) {
            if (M3BidiMap.this.isEmpty() || Objects.isNull(filter)) {
                return false;
            }
            boolean modified = false;
            final Iterator<E> iterator = iterator();
            while (iterator.hasNext()) {
                if (filter.test(iterator.next())) {
                    iterator.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean retainAll(final Collection<?> collection) {
            if (M3BidiMap.this.isEmpty()) {
                return false;
            }
            if (collection.isEmpty()) {
                M3BidiMap.this.clear();
                return true;
            }
            boolean modified = false;
            final Iterator<E> iterator = iterator();
            while (iterator.hasNext()) {
                if (!collection.contains(iterator.next())) {
                    iterator.remove();
                    modified = true;
                }
            }
            return modified;
        }
    }

    private final class KeysView extends OwnedView<K> {
        KeysView() {
            super(forward.keySet());
        }

        @Override
        public boolean contains(final Object key) {
            return forward.containsKey(key);
        }

        @Override
        public boolean remove(final Object key) {
            if (!forward.containsKey(key)) {
                return false;
            }
            M3BidiMap.this.remove(key);
            return true;
        }

        @Override
        public Iterator<K> iterator() {
            return new KeyCursor();
        }
    }

    private final class ValuesView extends OwnedView<V> {
        // API-required descriptor only; all payload ownership remains in the two M3 maps.
        ValuesView() {
            super(forward.values());
        }

        @Override
        public boolean contains(final Object value) {
            return reverse.containsKey(value);
        }

        @Override
        public boolean remove(final Object value) {
            if (!reverse.containsKey(value)) {
                return false;
            }
            removeValue(value);
            return true;
        }

        @Override
        public Iterator<V> iterator() {
            return new Iterator<>() {
                private final KeyCursor cursor = new KeyCursor();

                @Override
                public boolean hasNext() {
                    return cursor.hasNext();
                }

                @Override
                public V next() {
                    return forward.get(cursor.next());
                }

                @Override
                public void remove() {
                    cursor.remove();
                }
            };
        }

    }

    private final class EntriesView extends OwnedView<Map.Entry<K, V>> {
        EntriesView() {
            super(forward.entrySet());
        }

        @Override
        public boolean contains(final Object value) {
            if (!(value instanceof Map.Entry<?, ?> entry)) {
                return false;
            }
            final Object key = entry.getKey();
            return forward.containsKey(key) && Objects.equals(forward.get(key), entry.getValue());
        }

        @Override
        public boolean remove(final Object value) {
            if (!(value instanceof Map.Entry<?, ?> entry)) {
                return false;
            }
            final Object key = entry.getKey();
            if (!forward.containsKey(key) || !Objects.equals(forward.get(key), entry.getValue())) {
                return false;
            }
            M3BidiMap.this.remove(key);
            return true;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new Iterator<>() {
                private final KeyCursor cursor = new KeyCursor();

                @Override
                public boolean hasNext() {
                    return cursor.hasNext();
                }

                @Override
                public Map.Entry<K, V> next() {
                    return new OwnedEntry(cursor.next());
                }

                @Override
                public void remove() {
                    cursor.remove();
                }
            };
        }
    }

    /** Direct public key-slot cursor; it never requests an owner entry-set iterator. */
    private class KeyCursor implements Iterator<K> {
        private Iterator<K> slots;
        K currentKey;
        boolean current;
        private boolean more;

        KeyCursor() {
            resetCursor();
        }

        final void resetCursor() {
            slots = forward.keySet().iterator();
            more = slots.hasNext();
            currentKey = null;
            current = false;
        }

        @Override
        public boolean hasNext() {
            // LinkedHashMap's hasNext observes its next pointer without checking modification.
            return more;
        }

        @Override
        public K next() {
            final K key = slots.next();
            more = slots.hasNext();
            currentKey = key;
            current = true;
            return key;
        }

        @Override
        public void remove() {
            if (!current) {
                throw new IllegalStateException("Iterator remove() can only be called once after next()");
            }
            final V value = forward.get(currentKey);
            slots.remove();
            reverse.remove(value);
            currentKey = null;
            current = false;
        }
    }

    private final class BidiCursor extends KeyCursor
            implements MapIterator<K, V>, ResettableIterator<K> {
        private void requireCurrent(final String operation) {
            if (!current) {
                throw new IllegalStateException(
                        "Iterator " + operation + "() can only be called after next() and before remove()");
            }
        }

        @Override
        public K getKey() {
            requireCurrent("getKey");
            return currentKey;
        }

        @Override
        public V getValue() {
            requireCurrent("getValue");
            return forward.get(currentKey);
        }

        @Override
        public V setValue(final V value) {
            requireCurrent("setValue");
            requireSetterValue(currentKey, value);
            return put(currentKey, value);
        }

        @Override
        public void reset() {
            resetCursor();
        }

        @Override
        public String toString() {
            return current ? "MapIterator[" + getKey() + "=" + getValue() + "]" : "MapIterator[]";
        }
    }

    private final class OwnedEntry implements Map.Entry<K, V> {
        private final K key;

        OwnedEntry(final K key) {
            this.key = key;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return forward.get(key);
        }

        @Override
        public V setValue(final V value) {
            requireSetterValue(key, value);
            put(key, value);
            // The donor puts first, then its backing entry observes the already installed value.
            return value;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Map.Entry<?, ?> entry
                    && Objects.equals(key, entry.getKey()) && Objects.equals(getValue(), entry.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(getValue());
        }

        @Override
        public String toString() {
            return key + "=" + getValue();
        }
    }
}
