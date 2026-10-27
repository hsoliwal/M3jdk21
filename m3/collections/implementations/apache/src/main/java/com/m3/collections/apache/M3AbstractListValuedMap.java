// Modified 2026 by Hitesh Soliwal and Synexia contributors: owned M3 array storage, pinned bounded helper closure and documented query-contract repairs; counted list writes, counted sub-list views and counter-preserving group removal.
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
package com.m3.collections.apache;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.apache.commons.collections4.ListValuedMap;

/**
 * Abstract implementation of the {@link ListValuedMap} interface to simplify
 * the creation of subclass implementations.
 * <p>
 * Subclasses specify a Map implementation to use as the internal storage and
 * the List implementation to use as values.
 * </p>
 *
 * @param <K> the type of the keys in this map
 * @param <V> the type of the values in this map
 * @since 4.1
 */
public abstract class M3AbstractListValuedMap<K, V> extends M3AbstractMultiValuedMap<K, V>
        implements ListValuedMap<K, V> {

    /** Values ListIterator */
    private final class ValuesListIterator implements ListIterator<V> {

        private final K key;
        private List<V> values;
        private ListIterator<V> iterator;

        ValuesListIterator(final K key) {
            this.key = key;
            this.values = M3ApacheListUtils.emptyIfNull(listGroup(key));
            this.iterator = values.listIterator();
        }

        ValuesListIterator(final K key, final int index) {
            this.key = key;
            this.values = M3ApacheListUtils.emptyIfNull(listGroup(key));
            this.iterator = values.listIterator(index);
        }

        //FLLDCIM: ALG - LC904 distinct-count: cursor add counted only on the attached group
        @Override
        public void add(final V value) {
            final long ticket = open();
            int before = 0;
            try {
                if (listGroup(key) == null) {
                    final List<V> list = createCollection();
                    putGroup(key, list);
                    values = list;
                    iterator = list.listIterator();
                }
                before = values.size();
                iterator.add(value);
            } finally {
                closeHeld(ticket, key, values, before);
            }
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public boolean hasPrevious() {
            return iterator.hasPrevious();
        }

        @Override
        public V next() {
            return iterator.next();
        }

        @Override
        public int nextIndex() {
            return iterator.nextIndex();
        }

        @Override
        public V previous() {
            return iterator.previous();
        }

        @Override
        public int previousIndex() {
            return iterator.previousIndex();
        }

        //FLLDCIM: ALG - LC904 distinct-count: cursor removal counted, empty group then detached
        @Override
        public void remove() {
            final long ticket = open();
            final int before = values.size();
            try {
                iterator.remove();
            } finally {
                closeHeld(ticket, key, values, before);
            }
            if (values.isEmpty()) {
                detach(key);
            }
        }

        @Override
        public void set(final V value) {
            iterator.set(value);
        }

    }

    /**
     * Wrapped list to handle add and remove on the list returned by get(object)
     */
    private final class WrappedList extends WrappedCollection implements List<V> {

        WrappedList(final K key) {
            super(key);
        }

        //FLLDCIM: ALG - LC904 distinct-count: indexed add counted at the wrapper write
        @Override
        public void add(final int index, final V value) {
            final long ticket = open();
            List<V> list = null;
            int before = 0;
            try {
                list = getMapping();
                if (list == null) {
                    list = createCollection();
                    putGroup(key, list);
                } else {
                    before = list.size();
                }
                list.add(index, value);
            } finally {
                close(ticket, before, list == null ? 0 : list.size());
            }
        }

        //FLLDCIM: ALG - LC904 distinct-count: a new group is counted only once it is attached
        @Override
        public boolean addAll(final int index, final Collection<? extends V> c) {
            final long ticket = open();
            List<V> list = null;
            int before = 0;
            try {
                final List<V> mapped = getMapping();
                if (mapped == null) {
                    final List<V> created = createCollection();
                    final boolean changed = created.addAll(index, c);
                    if (changed) {
                        putGroup(key, created);
                        list = created;
                    }
                    return changed;
                }
                list = mapped;
                before = list.size();
                return list.addAll(index, c);
            } finally {
                close(ticket, before, list == null ? 0 : list.size());
            }
        }

        @Override
        public boolean equals(final Object other) {
            final List<V> list = getMapping();
            if (list == null) {
                return Collections.emptyList().equals(other);
            }
            if (!(other instanceof List)) {
                return false;
            }
            final List<?> otherList = (List<?>) other;
            return M3ApacheListUtils.isEqualList(list, otherList);
        }

        @Override
        public V get(final int index) {
            final List<V> list = M3ApacheListUtils.emptyIfNull(getMapping());
            return list.get(index);
        }

        //FLLDCIM: ALG - owned read: does not open the getMap() epoch guard
        @Override
        protected List<V> getMapping() {
            return listGroup(key);
        }

        @Override
        public int hashCode() {
            final List<V> list = getMapping();
            return M3ApacheListUtils.hashCodeForList(list);
        }

        @Override
        public int indexOf(final Object o) {
            final List<V> list = M3ApacheListUtils.emptyIfNull(getMapping());
            return list.indexOf(o);
        }

        @Override
        public int lastIndexOf(final Object o) {
            final List<V> list = M3ApacheListUtils.emptyIfNull(getMapping());
            return list.lastIndexOf(o);
        }

        @Override
        public ListIterator<V> listIterator() {
            return new ValuesListIterator(key);
        }

        @Override
        public ListIterator<V> listIterator(final int index) {
            return new ValuesListIterator(key, index);
        }

        //FLLDCIM: ALG - LC904 distinct-count: 1->0 transition counted before the detach
        @Override
        public V remove(final int index) {
            final long ticket = open();
            List<V> list = null;
            int before = 0;
            final V value;
            try {
                list = M3ApacheListUtils.emptyIfNull(getMapping());
                before = list.size();
                value = list.remove(index);
            } finally {
                close(ticket, before, list == null ? 0 : list.size());
            }
            if (list.isEmpty()) {
                M3AbstractListValuedMap.this.remove(key);
            }
            return value;
        }

        @Override
        public V set(final int index, final V value) {
            final List<V> list = M3ApacheListUtils.emptyIfNull(getMapping());
            return list.set(index, value);
        }

        /**
         * Returns a live view of the attached group. Writes through it (and through its
         * iterators, nested sub-lists and reversed views) keep the counters exact; an emptied
         * group stays attached as a private empty owner, as before.
         */
        //FLLDCIM: ALG - aliasing guard: the raw sub-list is wrapped so alias writes are counted
        @Override
        public List<V> subList(final int fromIndex, final int toIndex) {
            final List<V> list = getMapping();
            if (list == null) {
                return Collections.<V>emptyList().subList(fromIndex, toIndex);
            }
            return new CountedListView(key, list, list.subList(fromIndex, toIndex));
        }

    }

    /**
     * Delegating view (sub-list or reversed view) of one group. Every read, iteration and
     * fail-fast check is the backing view's own; each structural write is bracketed by an
     * owned open/close so the map counters follow the group's size change. Writes on a group
     * that was detached in the meantime change no counter.
     */
    //FLLDCIM: ALG - aliasing guard: Guava WrappedList ancestor pattern (idea only), size-delta counting
    private final class CountedListView implements List<V>, RandomAccess {
        private final K key;
        private final List<V> group;
        private final List<V> view;

        CountedListView(final K key, final List<V> group, final List<V> view) {
            this.key = key;
            this.group = group;
            this.view = view;
        }

        @Override public int size() { return view.size(); }
        @Override public boolean isEmpty() { return view.isEmpty(); }
        @Override public boolean contains(final Object o) { return view.contains(o); }
        @Override public Object[] toArray() { return view.toArray(); }
        @Override public <T> T[] toArray(final T[] a) { return view.toArray(a); }
        @Override public boolean containsAll(final Collection<?> c) { return view.containsAll(c); }
        @Override public V get(final int index) { return view.get(index); }
        @Override public int indexOf(final Object o) { return view.indexOf(o); }
        @Override public int lastIndexOf(final Object o) { return view.lastIndexOf(o); }
        @Override public V set(final int index, final V element) { return view.set(index, element); }
        @Override public V getFirst() { return view.getFirst(); }
        @Override public V getLast() { return view.getLast(); }
        @Override public void replaceAll(final UnaryOperator<V> operator) { view.replaceAll(operator); }
        @Override public void sort(final Comparator<? super V> c) { view.sort(c); }
        @Override public Spliterator<V> spliterator() { return view.spliterator(); }
        @Override public boolean equals(final Object o) { return o == this || view.equals(o); }
        @Override public int hashCode() { return view.hashCode(); }
        @Override public String toString() { return view.toString(); }
        @Override public Iterator<V> iterator() { return new CountedIterator(view.iterator()); }
        @Override public ListIterator<V> listIterator() { return new CountedListIterator(view.listIterator()); }
        @Override public ListIterator<V> listIterator(final int index) {
            return new CountedListIterator(view.listIterator(index));
        }
        @Override public List<V> subList(final int fromIndex, final int toIndex) {
            return new CountedListView(key, group, view.subList(fromIndex, toIndex));
        }
        @Override public List<V> reversed() { return new CountedListView(key, group, view.reversed()); }

        @Override public boolean add(final V e) {
            final long ticket = open();
            final int before = group.size();
            try { return view.add(e); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public void add(final int index, final V element) {
            final long ticket = open();
            final int before = group.size();
            try { view.add(index, element); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public boolean addAll(final Collection<? extends V> c) {
            final long ticket = open();
            final int before = group.size();
            try { return view.addAll(c); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public boolean addAll(final int index, final Collection<? extends V> c) {
            final long ticket = open();
            final int before = group.size();
            try { return view.addAll(index, c); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public void addFirst(final V e) {
            final long ticket = open();
            final int before = group.size();
            try { view.addFirst(e); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public void addLast(final V e) {
            final long ticket = open();
            final int before = group.size();
            try { view.addLast(e); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public boolean remove(final Object o) {
            final long ticket = open();
            final int before = group.size();
            try { return view.remove(o); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public V remove(final int index) {
            final long ticket = open();
            final int before = group.size();
            try { return view.remove(index); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public V removeFirst() {
            final long ticket = open();
            final int before = group.size();
            try { return view.removeFirst(); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public V removeLast() {
            final long ticket = open();
            final int before = group.size();
            try { return view.removeLast(); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public boolean removeAll(final Collection<?> c) {
            final long ticket = open();
            final int before = group.size();
            try { return view.removeAll(c); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public boolean retainAll(final Collection<?> c) {
            final long ticket = open();
            final int before = group.size();
            try { return view.retainAll(c); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public boolean removeIf(final Predicate<? super V> filter) {
            final long ticket = open();
            final int before = group.size();
            try { return view.removeIf(filter); } finally { closeHeld(ticket, key, group, before); }
        }
        @Override public void clear() {
            final long ticket = open();
            final int before = group.size();
            try { view.clear(); } finally { closeHeld(ticket, key, group, before); }
        }

        /** Backing iterator with counted removal. */
        private final class CountedIterator implements Iterator<V> {
            private final Iterator<V> cursor;

            CountedIterator(final Iterator<V> cursor) {
                this.cursor = cursor;
            }

            @Override public boolean hasNext() { return cursor.hasNext(); }
            @Override public V next() { return cursor.next(); }
            @Override public void forEachRemaining(final Consumer<? super V> action) { cursor.forEachRemaining(action); }
            @Override public void remove() {
                final long ticket = open();
                final int before = group.size();
                try { cursor.remove(); } finally { closeHeld(ticket, key, group, before); }
            }
        }

        /** Backing list iterator with counted add and remove. */
        private final class CountedListIterator implements ListIterator<V> {
            private final ListIterator<V> cursor;

            CountedListIterator(final ListIterator<V> cursor) {
                this.cursor = cursor;
            }

            @Override public boolean hasNext() { return cursor.hasNext(); }
            @Override public V next() { return cursor.next(); }
            @Override public boolean hasPrevious() { return cursor.hasPrevious(); }
            @Override public V previous() { return cursor.previous(); }
            @Override public int nextIndex() { return cursor.nextIndex(); }
            @Override public int previousIndex() { return cursor.previousIndex(); }
            @Override public void set(final V e) { cursor.set(e); }
            @Override public void forEachRemaining(final Consumer<? super V> action) { cursor.forEachRemaining(action); }
            @Override public void remove() {
                final long ticket = open();
                final int before = group.size();
                try { cursor.remove(); } finally { closeHeld(ticket, key, group, before); }
            }
            @Override public void add(final V e) {
                final long ticket = open();
                final int before = group.size();
                try { cursor.add(e); } finally { closeHeld(ticket, key, group, before); }
            }
        }
    }

    /**
     * Constructor needed for subclass serialization.
     */
    protected M3AbstractListValuedMap() {
    }

    /**
     * A constructor that wraps, not copies
     *
     * @param map  the map to wrap, must not be null
     * @throws NullPointerException if the map is null
     */
    protected M3AbstractListValuedMap(final Map<K, ? extends List<V>> map) {
        super(map);
    }

    /**
     * Creates a new value collection using the provided factory.
     * @return a new list
     */
    @Override
    protected abstract List<V> createCollection();

    /**
     * Gets the list of values associated with the specified key. This would
     * return an empty list in case the mapping is not present
     *
     * @param key  the key to retrieve
     * @return the {@code List} of values, will return an empty {@link List} for no mapping
     */
    @Override
    public List<V> get(final K key) {
        return wrappedCollection(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<K, List<V>> getMap() {
        return (Map<K, List<V>>) super.getMap();
    }

    /**
     * Removes all values associated with the specified key.
     * <p>
     * A subsequent {@code get(Object)} would return an empty list.
     * </p>
     *
     * @param key  the key to remove values from
     * @return the {@code List} of values removed, will return an empty,
     *   unmodifiable list for no mapping found.
     */
    //FLLDCIM: ALG - LC904 distinct-count: removed group counted out through detach
    @Override
    public List<V> remove(final Object key) {
        final List<V> removed = (List<V>) detach(key);
        return removed == null || removed.isEmpty() ? Collections.emptyList() : removed;
    }

    @Override
    List<V> wrappedCollection(final K key) {
        return new WrappedList(key);
    }

    /** Owned read of one list group; unlike {@link #getMap()} it does not invalidate the counters. */
    private List<V> listGroup(final Object key) {
        return (List<V>) group(key);
    }

}
