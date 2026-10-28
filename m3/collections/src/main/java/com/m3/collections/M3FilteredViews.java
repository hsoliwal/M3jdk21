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
// Modified 2026 by Hitesh Soliwal and contributors: reconcile merged sequenced guards and retain reversed split ordering.
// Modified 2026 by Hitesh Soliwal and contributors: preserve comparator metadata across filtered splits.
// Reconciled 2026: retain the qualified guarded reverse atom after additive merge.
/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: interior-null navigation through bounded ordered cursors.
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Lazy read-only filtered views over standard JDK collections, sets and maps.
 *
 * <p>These views are backed and live: future operations see backing changes, while an
 * existing iterator follows its underlying iterator's concurrent-modification policy.
 * No data is copied, cached or eagerly predicate-evaluated at construction. The source's
 * comparator, range checks, ordering and object references remain authoritative.
 * Predicates must be stable during a traversal and consistent with equals for Set/Map
 * contracts. No atomic snapshot, thread safety, or serialization is promised.</p>
 *
 * <p>Unlike validation/predicated collection decorators, rejected existing members
 * are invisible. Unlike Guava's mutable filtered views, this API intentionally rejects
 * every mutation (including iterator removal), so callers must edit the owner directly
 * or use an explicitly approved mutable contract.</p>
 */
public final class M3FilteredViews {
    private M3FilteredViews() { }

    public static <E> Collection<E> filter(Collection<E> source,
            Predicate<? super E> predicate) {
        return new CollectionView<>(source, predicate);
    }

    public static <E> Set<E> filter(Set<E> source, Predicate<? super E> predicate) {
        return new SetView<>(source, predicate);
    }

    public static <E> SortedSet<E> filter(SortedSet<E> source,
            Predicate<? super E> predicate) {
        return new SortedSetView<>(source, predicate);
    }

    public static <E> NavigableSet<E> filter(NavigableSet<E> source,
            Predicate<? super E> predicate) {
        return new NavigableSetView<>(source, predicate);
    }

    public static <K, V> SortedMap<K, V> filterEntries(SortedMap<K, V> source,
            BiPredicate<? super K, ? super V> predicate) {
        return Collections.unmodifiableSortedMap(new SortedMapView<>(source, predicate));
    }

    public static <K, V> NavigableMap<K, V> filterEntries(NavigableMap<K, V> source,
            BiPredicate<? super K, ? super V> predicate) {
        return Collections.unmodifiableNavigableMap(new NavigableMapView<>(source, predicate));
    }

    private static <E> Iterator<E> filtered(Iterator<E> source,
            Predicate<? super E> predicate) {
        return new Iterator<>() {
            private static final int READY = 1;
            private static final int EXHAUSTED = 1 << 1;
            private E next;
            private int state;

            @Override public boolean hasNext() {
                if ((state & READY) != 0) return true;
                if ((state & EXHAUSTED) != 0) return false;
                while (source.hasNext()) {
                    E candidate = source.next();
                    if (predicate.test(candidate)) {
                        next = candidate;
                        state |= READY;
                        return true;
                    }
                }
                state |= EXHAUSTED;
                return false;
            }

            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                E result = next;
                next = null;
                state &= ~READY;
                return result;
            }

            @Override public void remove() { throw readonly(); }
        };
    }

    private static UnsupportedOperationException readonly() {
        return new UnsupportedOperationException("read-only filtered view");
    }

    /** Natural or owner-comparator order, including comparator-admitted null keys. */
    @SuppressWarnings("unchecked")
    private static <T> int compareStored(T stored, T query,
            Comparator<? super T> comparator) {
        return comparator != null ? comparator.compare(stored, query)
                : ((Comparable<? super T>) stored).compareTo(query);
    }

    /**
     * Apply visibility to the stored representative, never to a lookup alias.
     * An identity-backed Set may retain equals-equal distinct references: give
     * the actual reference priority before the normal Collection equality scan.
     * No visible-element snapshot or per-element entry wrapper is retained.
     */
    private static <E> boolean visible(Collection<E> source,
            Predicate<? super E> predicate, Object value) {
        if (!source.contains(value)) return false;
        return visibleMember(source, predicate, value);
    }

    /** Membership has already succeeded; preserve the original representative probe order. */
    private static <E> boolean visibleMember(Collection<E> source,
            Predicate<? super E> predicate, Object value) {
        if (source instanceof Set<?>) {
            for (E owned : source) {
                if (owned == value) return predicate.test(owned);
            }
        }
        for (E owned : source) {
            if (Objects.equals(value, owned) && predicate.test(owned)) return true;
        }
        return false;
    }

    /** Open only private read-only owners whose iterator is exactly the same filter decorator. */
    @SuppressWarnings("unchecked")
    private static <E> CollectionView<E> iterationView(Collection<E> source) {
        if (source instanceof CollectionView<?> view) return (CollectionView<E>) view;
        return source instanceof SetView<?> view ? ((SetView<E>) view).view : null;
    }

    /** Sorted membership has a different representative probe and remains an opaque boundary. */
    @SuppressWarnings("unchecked")
    private static <E> CollectionView<E> membershipView(Collection<E> source) {
        if (source instanceof CollectionView<?> view) return (CollectionView<E>) view;
        return source.getClass() == SetView.class ? ((SetView<E>) source).view : null;
    }

    private static class CollectionView<E> extends AbstractCollection<E> {
        private final Collection<E> source;
        private final Predicate<? super E> predicate;
        private volatile IterationPlan<E> iterationPlan;

        CollectionView(Collection<E> source, Predicate<? super E> predicate) {
            this.source = Objects.requireNonNull(source, "source");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
        }

        /**
         * Retain only immutable predicate coordinates; never cache element
         * membership or backing iterators. The backing remains live.
         */
        @Override public Iterator<E> iterator() {
            if (iterationView(source) == null) return filtered(source.iterator(), predicate);
            IterationPlan<E> plan = iterationPlan();
            return filtered(plan.backing.iterator(), value -> {
                for (Predicate<? super E> filter : plan.filters) {
                    if (!filter.test(value)) return false;
                }
                return true;
            });
        }

        private IterationPlan<E> iterationPlan() {
            IterationPlan<E> current = iterationPlan;
            if (current != null) return current;
            synchronized (this) {
                current = iterationPlan;
                if (current == null) {
                    current = flatten(this);
                    iterationPlan = current;
                }
            }
            return current;
        }

        private static <E> IterationPlan<E> flatten(CollectionView<E> outer) {
            int depth = 0;
            CollectionView<E> layer = outer;
            Collection<E> backing;
            do {
                depth++;
                backing = layer.source;
                layer = iterationView(backing);
            } while (layer != null);

            @SuppressWarnings("unchecked")
            Predicate<? super E>[] filters = (Predicate<? super E>[]) new Predicate<?>[depth];
            layer = outer;
            for (int index = depth - 1; index >= 0; index--) {
                filters[index] = layer.predicate;
                layer = iterationView(layer.source);
            }
            return new IterationPlan<>(backing, filters);
        }

        private static final class IterationPlan<E> {
            private final Collection<E> backing;
            private final Predicate<? super E>[] filters;

            private IterationPlan(Collection<E> backing, Predicate<? super E>[] filters) {
                this.backing = backing;
                this.filters = filters;
            }
        }
        @Override public int size() {
            int count = 0;
            Iterator<E> cursor = iterator();
            while (cursor.hasNext()) { cursor.next(); count++; }
            return count;
        }
        @Override public boolean isEmpty() { return !iterator().hasNext(); }
        @Override public boolean contains(Object value) {
            if (membershipView(source) == null) return visible(source, predicate, value);
            // Replay the existing inner membership and outer representative scans in order.
            int depth = 0;
            CollectionView<E> layer = this;
            CollectionView<E> inner;
            while ((inner = membershipView(layer.source)) != null) {
                depth++;
                layer = inner;
            }
            @SuppressWarnings("unchecked")
            CollectionView<E>[] pending = (CollectionView<E>[]) new CollectionView<?>[depth];
            layer = this;
            for (int index = 0; index < pending.length; index++) {
                pending[index] = layer;
                layer = membershipView(layer.source);
            }
            if (!visible(layer.source, layer.predicate, value)) return false;
            for (int index = pending.length - 1; index >= 0; index--) {
                layer = pending[index];
                if (!visibleMember(layer.source, layer.predicate, value)) return false;
            }
            return true;
        }
        @Override public boolean add(E value) { throw readonly(); }
        @Override public boolean remove(Object value) { throw readonly(); }
        @Override public boolean addAll(Collection<? extends E> values) { throw readonly(); }
        @Override public boolean removeAll(Collection<?> values) { throw readonly(); }
        @Override public boolean retainAll(Collection<?> values) { throw readonly(); }
        @Override public boolean removeIf(Predicate<? super E> filter) { throw readonly(); }
        @Override public void clear() { throw readonly(); }
    }

    private static class SetView<E> extends AbstractSet<E> {
        private final CollectionView<E> view;
        SetView(Set<E> source, Predicate<? super E> predicate) {
            this.view = new CollectionView<>(source, predicate);
        }
        @Override public int size() { return view.size(); }
        @Override public boolean isEmpty() { return view.isEmpty(); }
        @Override public boolean contains(Object value) { return view.contains(value); }
        @Override public Iterator<E> iterator() { return view.iterator(); }
        @Override public boolean add(E value) { throw readonly(); }
        @Override public boolean remove(Object value) { throw readonly(); }
        @Override public boolean addAll(Collection<? extends E> values) { throw readonly(); }
        @Override public boolean removeAll(Collection<?> values) { throw readonly(); }
        @Override public boolean retainAll(Collection<?> values) { throw readonly(); }
        @Override public boolean removeIf(Predicate<? super E> filter) { throw readonly(); }
        @Override public void clear() { throw readonly(); }
    }

    /** Guard the existing JDK reversed traversal without rebuilding its predicate pipeline. */
    private static <E> SortedSet<E> readOnlySortedSet(SortedSet<E> source) {
        return source instanceof SortedSetView<?> || source instanceof ReadOnlySortedSet<?>
                ? source : new ReadOnlySortedSet<>(source);
    }

    private static final class ReadOnlySortedSet<E> extends AbstractSet<E> implements SortedSet<E> {
        private final SortedSet<E> source;
        private ReadOnlySortedSet(SortedSet<E> source) { this.source = source; }
        @Override public int size() { return source.size(); }
        @Override public boolean isEmpty() { return source.isEmpty(); }
        @Override public boolean contains(Object value) { return source.contains(value); }
        @Override public boolean containsAll(Collection<?> values) { return source.containsAll(values); }
        @Override public Iterator<E> iterator() { return Collections.unmodifiableSet(source).iterator(); }
        /** Preserve the reverse owner's iterator/size demand and every split's ordering. */
        @Override public Spliterator<E> spliterator() { return M3SlotSpliterators.sorted(source); }
        @Override public Object[] toArray() { return source.toArray(); }
        @Override public <T> T[] toArray(T[] target) { return source.toArray(target); }
        @Override public <T> T[] toArray(java.util.function.IntFunction<T[]> factory) {
            return source.toArray(factory);
        }
        @Override public void forEach(java.util.function.Consumer<? super E> action) { source.forEach(action); }
        @Override public boolean equals(Object other) { return other == this || source.equals(other); }
        @Override public int hashCode() { return source.hashCode(); }
        @Override public String toString() { return source.toString(); }
        @Override public Comparator<? super E> comparator() { return source.comparator(); }
        @Override public E first() { return source.first(); }
        @Override public E last() { return source.last(); }
        @Override public SortedSet<E> subSet(E from, E to) { return readOnlySortedSet(source.subSet(from, to)); }
        @Override public SortedSet<E> headSet(E to) { return readOnlySortedSet(source.headSet(to)); }
        @Override public SortedSet<E> tailSet(E from) { return readOnlySortedSet(source.tailSet(from)); }
        @Override public SortedSet<E> reversed() { return readOnlySortedSet(source.reversed()); }
        @Override public boolean add(E value) { throw readonly(); }
        @Override public boolean remove(Object value) { throw readonly(); }
        @Override public boolean addAll(Collection<? extends E> values) { throw readonly(); }
        @Override public boolean removeAll(Collection<?> values) { throw readonly(); }
        @Override public boolean retainAll(Collection<?> values) { throw readonly(); }
        @Override public boolean removeIf(Predicate<? super E> filter) { throw readonly(); }
        @Override public void clear() { throw readonly(); }
        @Override public void addFirst(E value) { throw readonly(); }
        @Override public void addLast(E value) { throw readonly(); }
        @Override public E removeFirst() { throw readonly(); }
        @Override public E removeLast() { throw readonly(); }
    }

    private static class SortedSetView<E> extends SetView<E> implements SortedSet<E> {
        protected final SortedSet<E> sorted;
        protected final Predicate<? super E> predicate;

        SortedSetView(SortedSet<E> sorted, Predicate<? super E> predicate) {
            super(sorted, predicate);
            this.sorted = Objects.requireNonNull(sorted, "sorted");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
        }

        @Override public Comparator<? super E> comparator() { return sorted.comparator(); }
        /** Preserve custom ordering on every recursively split child. */
        @Override public Spliterator<E> spliterator() {
            return M3SlotSpliterators.sorted(this);
        }
        @Override public boolean contains(Object value) {
            if (!sorted.contains(value)) return false;
            @SuppressWarnings("unchecked") E alias = (E) value;
            // contains() already established comparator equivalence, so the
            // first tail representative is the exact stored element.
            return predicate.test(sorted.tailSet(alias).first());
        }
        @Override public SortedSet<E> subSet(E from, E to) {
            return new SortedSetView<>(sorted.subSet(from, to), predicate);
        }
        @Override public SortedSet<E> headSet(E to) {
            return new SortedSetView<>(sorted.headSet(to), predicate);
        }
        @Override public SortedSet<E> tailSet(E from) {
            return new SortedSetView<>(sorted.tailSet(from), predicate);
        }
        // An explicit read-only boundary refuses even empty endpoint mutations.
        @Override public void addFirst(E value) { throw readonly(); }
        @Override public void addLast(E value) { throw readonly(); }
        @Override public E first() { return iterator().next(); }
        @Override public E last() {
            Iterator<E> iterator = iterator();
            if (!iterator.hasNext()) throw new NoSuchElementException();
            E last = iterator.next();
            while (iterator.hasNext()) last = iterator.next();
            return last;
        }
    }

    private static final class NavigableSetView<E> extends SortedSetView<E>
            implements NavigableSet<E> {
        private final NavigableSet<E> nav;
        NavigableSetView(NavigableSet<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
            nav = source;
        }
        @Override public boolean contains(Object value) {
            if (!nav.contains(value)) return false;
            @SuppressWarnings("unchecked") E alias = (E) value;
            return predicate.test(nav.ceiling(alias));
        }
        /**
         * Disambiguate a null navigation result using iterator presence. An owner's
         * comparator may place null anywhere, including between two non-null values.
         * Reuse the ordered-cursor range rule from M3CollectionViews: construct a
         * tail range only inside actual member bounds, then skip rejected values.
         * This preserves bounded-view queries and O(log n + rejected) tree work.
         */
        private E nullCandidate(E query, boolean forward, boolean inclusive) {
            // Natural order cannot admit a null member. Preserve its no-result
            // path without extra Comparable callbacks, including reversed views.
            if (nav.comparator() == null) return null;
            NavigableSet<E> reverse = nav.descendingSet();
            if (reverse.comparator() == null) return null;
            NavigableSet<E> ordered = forward ? nav : reverse;
            Iterator<E> cursor = ordered.iterator();
            if (!cursor.hasNext()) return null;
            Comparator<? super E> order = ordered.comparator();
            int fromFirst = compareStored(query, cursor.next(), order);
            if (fromFirst < 0 || (fromFirst == 0 && inclusive)) {
                return firstVisible(ordered.iterator());
            }
            int fromLast = compareStored(query, ordered.last(), order);
            if (fromLast > 0 || (fromLast == 0 && !inclusive)) return null;
            return firstVisible(ordered.tailSet(query, inclusive).iterator());
        }
        private E firstVisible(Iterator<E> cursor) {
            Iterator<E> selected = M3FilteredViews.filtered(cursor, predicate);
            return selected.hasNext() ? selected.next() : null;
        }
        private E navigate(E candidate, E query, boolean forward, boolean inclusive) {
            if (candidate == null) return nullCandidate(query, forward, inclusive);
            if (predicate.test(candidate)) return candidate;
            // The candidate is an actual member within this backing range; strict
            // successor/predecessor views are therefore legal even on nested subSets.
            Iterator<E> remaining = forward ? nav.tailSet(candidate, false).iterator()
                    : nav.headSet(candidate, false).descendingIterator();
            return firstVisible(remaining);
        }
        @Override public E lower(E e) {
            return navigate(nav.lower(e), e, false, false);
        }
        @Override public E floor(E e) {
            return navigate(nav.floor(e), e, false, true);
        }
        @Override public E ceiling(E e) {
            return navigate(nav.ceiling(e), e, true, true);
        }
        @Override public E higher(E e) {
            return navigate(nav.higher(e), e, true, false);
        }
        @Override public E first() {
            Iterator<E> values = iterator();
            if (!values.hasNext()) throw new NoSuchElementException();
            return values.next();
        }
        @Override public E last() {
            Iterator<E> values = descendingIterator();
            if (!values.hasNext()) throw new NoSuchElementException();
            return values.next();
        }
        @Override public E pollFirst() { throw readonly(); }
        @Override public E pollLast() { throw readonly(); }
        @Override public NavigableSet<E> descendingSet() {
            return new NavigableSetView<>(nav.descendingSet(), predicate);
        }
        @Override public Iterator<E> descendingIterator() {
            return filtered(nav.descendingSet().iterator(), predicate);
        }
        @Override public NavigableSet<E> subSet(E from, boolean inclusiveFrom,
                E to, boolean inclusiveTo) {
            return new NavigableSetView<>(nav.subSet(from, inclusiveFrom, to, inclusiveTo), predicate);
        }
        @Override public NavigableSet<E> headSet(E to, boolean inclusive) {
            return new NavigableSetView<>(nav.headSet(to, inclusive), predicate);
        }
        @Override public NavigableSet<E> tailSet(E from, boolean inclusive) {
            return new NavigableSetView<>(nav.tailSet(from, inclusive), predicate);
        }
        @Override public NavigableSet<E> subSet(E from, E to) {
            return subSet(from, true, to, false);
        }
        @Override public NavigableSet<E> headSet(E to) { return headSet(to, false); }
        @Override public NavigableSet<E> tailSet(E from) { return tailSet(from, true); }
    }

    private static class SortedMapView<K, V> extends AbstractMap<K, V>
            implements SortedMap<K, V> {
        protected final SortedMap<K, V> sorted;
        protected final BiPredicate<? super K, ? super V> predicate;

        SortedMapView(SortedMap<K, V> sorted, BiPredicate<? super K, ? super V> predicate) {
            this.sorted = Objects.requireNonNull(sorted, "sorted");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
        }
        @Override public Comparator<? super K> comparator() { return sorted.comparator(); }
        @Override public int size() {
            int count = 0;
            for (Entry<K, V> entry : sorted.entrySet())
                if (predicate.test(entry.getKey(), entry.getValue())) count++;
            return count;
        }
        @Override public boolean isEmpty() {
            return !filtered(sorted.entrySet().iterator(),
                    item -> predicate.test(item.getKey(), item.getValue())).hasNext();
        }
        /**
         * Called only after containsKey has established membership. The
         * comparator's tail range exposes the actual retained key, including
         * when the query is a distinct comparator-equivalent object.
         */
        protected K storedKey(Object query) {
            @SuppressWarnings("unchecked") K alias = (K) query;
            return sorted.tailMap(alias).firstKey();
        }
        @Override public boolean containsKey(Object key) {
            if (!sorted.containsKey(key)) return false;
            return predicate.test(storedKey(key), sorted.get(key));
        }
        @Override public V get(Object key) {
            if (!sorted.containsKey(key)) return null;
            V value = sorted.get(key);
            return predicate.test(storedKey(key), value) ? value : null;
        }
        @Override public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return SortedMapView.this.size(); }
                @Override public boolean contains(Object candidate) {
                    if (!(candidate instanceof Entry<?, ?> probe)) return false;
                    Object key = probe.getKey();
                    if (!sorted.containsKey(key)) return false;
                    V value = sorted.get(key);
                    // Reuse comparator membership and the existing stored-key
                    // atom. A nullable mapping is distinct from an absent key.
                    return predicate.test(storedKey(key), value)
                            && Objects.equals(value, probe.getValue());
                }
                @Override public Iterator<Entry<K, V>> iterator() {
                    Iterator<Entry<K, V>> cursor = sorted.entrySet().iterator();
                    Iterator<Entry<K, V>> filtered = M3FilteredViews.filtered(cursor,
                            item -> predicate.test(item.getKey(), item.getValue()));
                    return new Iterator<>() {
                        @Override public boolean hasNext() { return filtered.hasNext(); }
                        @Override public Entry<K, V> next() {
                            return new SimpleImmutableEntry<>(filtered.next());
                        }
                        @Override public void remove() { throw readonly(); }
                    };
                }
                @Override public boolean add(Entry<K, V> entry) { throw readonly(); }
                @Override public boolean remove(Object entry) { throw readonly(); }
                @Override public void clear() { throw readonly(); }
            };
        }
        @Override public K firstKey() {
            Iterator<Entry<K, V>> cursor = entrySet().iterator();
            if (!cursor.hasNext()) throw new NoSuchElementException();
            return cursor.next().getKey();
        }
        @Override public K lastKey() {
            Iterator<Entry<K, V>> cursor = entrySet().iterator();
            if (!cursor.hasNext()) throw new NoSuchElementException();
            K key = cursor.next().getKey();
            while (cursor.hasNext()) key = cursor.next().getKey();
            return key;
        }
        @Override public SortedMap<K, V> subMap(K from, K to) {
            return new SortedMapView<>(sorted.subMap(from, to), predicate);
        }
        @Override public SortedMap<K, V> headMap(K to) {
            return new SortedMapView<>(sorted.headMap(to), predicate);
        }
        @Override public SortedMap<K, V> tailMap(K from) {
            return new SortedMapView<>(sorted.tailMap(from), predicate);
        }
        @Override public V put(K key, V value) { throw readonly(); }
        @Override public V remove(Object key) { throw readonly(); }
        @Override public void putAll(Map<? extends K, ? extends V> map) { throw readonly(); }
        @Override public void clear() { throw readonly(); }
    }

    private static final class NavigableMapView<K, V> extends SortedMapView<K, V>
            implements NavigableMap<K, V> {
        private final NavigableMap<K, V> nav;
        NavigableMapView(NavigableMap<K, V> source,
                BiPredicate<? super K, ? super V> predicate) {
            super(source, predicate);
            nav = source;
        }
        @Override protected K storedKey(Object key) {
            @SuppressWarnings("unchecked") K alias = (K) key;
            // A matching key exists (checked by containsKey); floorEntry
            // retrieves its stored comparator-equivalent representative.
            return nav.floorEntry(alias).getKey();
        }
        /**
         * Select the first visible entry from a bounded backing iterator and detach it
         * as an immutable Map.Entry.  Range creation pays the tree search once; rejected
         * entries are then skipped by iterator advancement instead of repeated lookup.
         */
        private Entry<K, V> firstVisible(Iterator<Entry<K, V>> cursor) {
            Iterator<Entry<K, V>> selected = M3FilteredViews.filtered(cursor,
                    entry -> predicate.test(entry.getKey(), entry.getValue()));
            return selected.hasNext() ? new SimpleImmutableEntry<>(selected.next()) : null;
        }
        /**
         * Do not turn an out-of-range query on a live bounded map into a
         * subMap/headMap/tailMap construction exception. Only the exceptional
         * recovery path scans owner entries; ordinary queries keep the
         * comparator-backed bounded iterator, retaining each stored key/value.
         */
        private Entry<K, V> boundedEntry(K query, boolean forward, boolean inclusive) {
            NavigableMap<K, V> range;
            if (nav instanceof M3TreeMap<K, V> owner) {
                range = owner.rangeOrNull(query, forward, inclusive);
                if (range == null) return boundedFallback(query, forward, inclusive);
            } else {
                // A navigation query is legal outside a bounded JDK map. Calling
                // the owner's near-entry primitive also preserves user comparator
                // exceptions; catching every IllegalArgumentException from a
                // headMap/tailMap construction would silently swallow those.
                Entry<K, V> nearest = forward
                        ? (inclusive ? nav.ceilingEntry(query) : nav.higherEntry(query))
                        : (inclusive ? nav.floorEntry(query) : nav.lowerEntry(query));
                if (nearest == null) return null;
                if (predicate.test(nearest.getKey(), nearest.getValue())) {
                    return new SimpleImmutableEntry<>(nearest);
                }
                // The stored representative is already inside the bounded view.
                // Skip it and scan the remaining range in encounter order.
                range = forward ? nav.tailMap(nearest.getKey(), false)
                        : nav.headMap(nearest.getKey(), false);
            }
            Iterator<Entry<K, V>> cursor = forward ? range.entrySet().iterator()
                    : range.descendingMap().entrySet().iterator();
            return firstVisible(cursor);
        }
        private Entry<K, V> boundedFallback(K query, boolean forward, boolean inclusive) {
            Iterator<Entry<K, V>> bounded = forward
                    ? nav.entrySet().iterator() : nav.descendingMap().entrySet().iterator();
            Comparator<? super K> comparator = nav.comparator();
            while (bounded.hasNext()) {
                Entry<K, V> stored = bounded.next();
                int relation = compareStored(stored.getKey(), query, comparator);
                if ((forward ? relation > 0 || (inclusive && relation == 0)
                        : relation < 0 || (inclusive && relation == 0))
                        && predicate.test(stored.getKey(), stored.getValue())) {
                    return new SimpleImmutableEntry<>(stored);
                }
            }
            return null;
        }

        @Override public Entry<K, V> lowerEntry(K key) {
            return boundedEntry(key, false, false);
        }
        @Override public K lowerKey(K key) { return keyOf(lowerEntry(key)); }
        @Override public Entry<K, V> floorEntry(K key) {
            return boundedEntry(key, false, true);
        }
        @Override public K floorKey(K key) { return keyOf(floorEntry(key)); }
        @Override public Entry<K, V> ceilingEntry(K key) {
            return boundedEntry(key, true, true);
        }
        @Override public K ceilingKey(K key) { return keyOf(ceilingEntry(key)); }
        @Override public Entry<K, V> higherEntry(K key) {
            return boundedEntry(key, true, false);
        }
        @Override public K higherKey(K key) { return keyOf(higherEntry(key)); }
        @Override public Entry<K, V> firstEntry() {
            return firstVisible(nav.entrySet().iterator());
        }
        @Override public Entry<K, V> lastEntry() {
            return firstVisible(nav.descendingMap().entrySet().iterator());
        }
        @Override public K firstKey() {
            Entry<K, V> e = firstEntry();
            if (e == null) throw new NoSuchElementException();
            return e.getKey();
        }
        @Override public K lastKey() {
            Entry<K, V> e = lastEntry();
            if (e == null) throw new NoSuchElementException();
            return e.getKey();
        }
        private K keyOf(Entry<K, V> entry) { return entry == null ? null : entry.getKey(); }
        @Override public Entry<K, V> pollFirstEntry() { throw readonly(); }
        @Override public Entry<K, V> pollLastEntry() { throw readonly(); }
        @Override public NavigableMap<K, V> descendingMap() {
            return new NavigableMapView<>(nav.descendingMap(), predicate);
        }
        @Override public NavigableSet<K> navigableKeySet() {
            return new NavigableSetView<>(nav.navigableKeySet(), this::containsKey);
        }
        @Override public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }
        @Override public NavigableMap<K, V> subMap(K from, boolean fromInclusive,
                K to, boolean toInclusive) {
            return new NavigableMapView<>(nav.subMap(from, fromInclusive, to, toInclusive), predicate);
        }
        @Override public NavigableMap<K, V> headMap(K to, boolean inclusive) {
            return new NavigableMapView<>(nav.headMap(to, inclusive), predicate);
        }
        @Override public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
            return new NavigableMapView<>(nav.tailMap(from, inclusive), predicate);
        }
        @Override public NavigableMap<K, V> subMap(K from, K to) {
            return subMap(from, true, to, false);
        }
        @Override public NavigableMap<K, V> headMap(K to) { return headMap(to, false); }
        @Override public NavigableMap<K, V> tailMap(K from) { return tailMap(from, true); }
    }
}
