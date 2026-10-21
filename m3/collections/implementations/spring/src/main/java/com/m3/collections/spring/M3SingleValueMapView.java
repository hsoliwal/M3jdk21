/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// SPDX-License-Identifier: Apache-2.0
// Modified 2026 by Hitesh Soliwal and contributors: owned scalar-view body,
// a private serializable one-element Object[] list delta, and a target widened to any list-valued map.
package com.m3.collections.spring;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Live first-value Map protocol over a list-valued map: an owned M3MultiValueMap, any Spring
 * MultiValueMap (including an M3MultiValueMapAdapter) or a plain {@code Map<K, List<V>>}.
 *
 * <p>As in Spring 6.2, get/put/remove adapt null or empty lists to null, while values,
 * entries and forEach explicitly read index zero and retain the donor's null/empty
 * failures. Entry iteration returns immutable scalar snapshots. Hash/string use
 * the underlying list-valued map; they deliberately retain that provider quirk.</p>
 */
public final class M3SingleValueMapView<K, V> implements Map<K, V>, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    // Object streams validate the supplied target graph at serialization time, as before the widening.
    @SuppressWarnings("serial")
    private final Map<K, List<V>> targetMap;
    private transient Collection<V> values;
    private transient Set<Map.Entry<K, V>> entries;

    /**
     * Views {@code targetMap}; the view reads only the {@code Map<K, List<V>>} protocol, so the target
     * needs no particular class. Existing callers passing an M3MultiValueMap compile unchanged.
     */
    public M3SingleValueMapView(final Map<K, List<V>> targetMap) {
        if (targetMap == null) {
            throw new IllegalArgumentException("'targetMap' must not be null");
        }
        this.targetMap = targetMap;
    }

    @Override
    public int size() {
        return targetMap.size();
    }

    @Override
    public boolean isEmpty() {
        return targetMap.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return targetMap.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        final Iterator<Map.Entry<K, V>> iterator = entrySet().iterator();
        if (value == null) {
            while (iterator.hasNext()) {
                if (iterator.next().getValue() == null) {
                    return true;
                }
            }
        } else {
            while (iterator.hasNext()) {
                if (value.equals(iterator.next().getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V get(final Object key) {
        return first(targetMap.get(key));
    }

    @Override
    public V put(final K key, final V value) {
        return first(targetMap.put(key, singleton(value)));
    }

    @Override
    public V remove(final Object key) {
        return first(targetMap.remove(key));
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> source) {
        for (final Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        targetMap.clear();
    }

    @Override
    public Set<K> keySet() {
        return targetMap.keySet();
    }

    @Override
    public Collection<V> values() {
        Collection<V> view = values;
        if (view == null) {
            final Collection<List<V>> targetValues = targetMap.values();
            view = new AbstractCollection<>() {
                @Override
                public Iterator<V> iterator() {
                    final Iterator<List<V>> targetIterator = targetValues.iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return targetIterator.hasNext();
                        }

                        @Override
                        public V next() {
                            return targetIterator.next().get(0);
                        }
                    };
                }

                @Override
                public int size() {
                    return targetValues.size();
                }
            };
            values = view;
        }
        return view;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> view = entries;
        if (view == null) {
            final Set<Map.Entry<K, List<V>>> targetEntries = targetMap.entrySet();
            view = new AbstractSet<>() {
                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    final Iterator<Map.Entry<K, List<V>>> targetIterator = targetEntries.iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return targetIterator.hasNext();
                        }

                        @Override
                        public Map.Entry<K, V> next() {
                            final Map.Entry<K, List<V>> entry = targetIterator.next();
                            return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().get(0));
                        }
                    };
                }

                @Override
                public int size() {
                    return targetEntries.size();
                }
            };
            entries = view;
        }
        return view;
    }

    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) {
        targetMap.forEach((key, list) -> action.accept(key, list.get(0)));
    }

    private V first(final List<V> list) {
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    private List<V> singleton(final V value) {
        return value != null ? new SingleElementList<>(value) : null;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Map<?, ?> otherMap && size() == otherMap.size()) {
            try {
                for (final Map.Entry<K, V> entry : entrySet()) {
                    final K key = entry.getKey();
                    final V value = entry.getValue();
                    if (value == null) {
                        if (otherMap.get(key) != null || !otherMap.containsKey(key)) {
                            return false;
                        }
                    } else if (!value.equals(otherMap.get(key))) {
                        return false;
                    }
                }
                return true;
            } catch (ClassCastException | NullPointerException ignored) {
                // Same donor refusal/fall-through behavior; empty-list failures are not swallowed.
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return targetMap.hashCode();
    }

    @Override
    public String toString() {
        return targetMap.toString();
    }

    /** The uncovered one-element immutable list protocol; no GPL implementation is copied. */
    private static final class SingleElementList<E> extends AbstractList<E>
            implements RandomAccess, Serializable {
        @Serial private static final long serialVersionUID = 1L;
        // Object streams validate the supplied element graph at serialization time.
        @SuppressWarnings("serial")
        private final Object[] element;

        SingleElementList(final E value) {
            element = new Object[] {value};
        }

        @SuppressWarnings("unchecked")
        @Override
        public E get(final int index) {
            Objects.checkIndex(index, 1);
            return (E) element[0];
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {
                private int index;

                @Override
                public boolean hasNext() {
                    return index == 0;
                }

                @Override
                public E next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    index = 1;
                    return get(0);
                }
            };
        }

        @Override
        public Spliterator<E> spliterator() {
            final int flags = Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE
                    | (element[0] != null ? Spliterator.NONNULL : 0);
            return Spliterators.spliterator(element, flags);
        }

        @Override
        public boolean removeIf(final Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(final UnaryOperator<E> action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort(final Comparator<? super E> comparator) {
            // One element is already sorted; no comparator evaluation.
        }

        @Serial
        private void readObject(final ObjectInputStream input) throws IOException, ClassNotFoundException {
            input.defaultReadObject();
            if (element == null || element.length != 1) {
                throw new InvalidObjectException("invalid singleton payload");
            }
        }
    }
}
