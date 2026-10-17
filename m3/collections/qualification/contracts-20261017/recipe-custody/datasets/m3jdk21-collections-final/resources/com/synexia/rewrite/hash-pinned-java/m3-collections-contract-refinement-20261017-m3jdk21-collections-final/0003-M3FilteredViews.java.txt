/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
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
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
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
        return new SortedMapView<>(source, predicate);
    }

    public static <K, V> NavigableMap<K, V> filterEntries(NavigableMap<K, V> source,
            BiPredicate<? super K, ? super V> predicate) {
        return new NavigableMapView<>(source, predicate);
    }

    private static <E> Iterator<E> filtered(Iterator<E> source,
            Predicate<? super E> predicate) {
        return new Iterator<>() {
            private E next;
            private boolean ready;
            private boolean exhausted;

            @Override public boolean hasNext() {
                if (ready) return true;
                if (exhausted) return false;
                while (source.hasNext()) {
                    E candidate = source.next();
                    if (predicate.test(candidate)) {
                        next = candidate;
                        ready = true;
                        return true;
                    }
                }
                exhausted = true;
                return false;
            }

            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                E result = next;
                next = null;
                ready = false;
                return result;
            }

            @Override public void remove() { throw readonly(); }
        };
    }

    private static UnsupportedOperationException readonly() {
        return new UnsupportedOperationException("read-only filtered view");
    }

    /**
     * Reuse the Synexia bounded-anchor atom for both read-only and mutable views.
     * A legal navigation query may be outside a bounded owner; only a stored
     * neighbor may become the endpoint of the subsequent linear skip scan.
     * Known M3 visibility owners are opened only for the boundary lookup;
     * their predicates remain on the original traversal path. Opaque owners
     * retain the ordinary range path whenever that endpoint is legal.
     */
    static <E> E seek(NavigableSet<E> source, Predicate<? super E> predicate,
            E element, boolean inclusive, boolean descending) {
        NavigableSet<E> range;
        try {
            // Keep the former path and its callback demand whenever the endpoint is legal.
            range = descending ? source.headSet(element, inclusive) : source.tailSet(element, inclusive);
        } catch (IllegalArgumentException outsideRange) {
            NavigableSet<E> owner = navigationOwner(source);
            checkQuery(owner.comparator(), element);
            E anchor = descending
                    ? (inclusive ? owner.floor(element) : owner.lower(element))
                    : (inclusive ? owner.ceiling(element) : owner.higher(element));
            if (anchor == null && !nullIsAnchor(owner, element, inclusive, descending)) return null;
            range = descending ? source.headSet(anchor, true) : source.tailSet(anchor, true);
        }
        Iterator<E> selected = filtered(descending ? range.descendingIterator() : range.iterator(), predicate);
        return selected.hasNext() ? selected.next() : null;
    }

    /** Peel known M3 permission/visibility facades for boundary lookup only; traversal stays wrapped. */
    @SuppressWarnings("unchecked")
    private static <E> NavigableSet<E> navigationOwner(NavigableSet<E> source) {
        while (true) {
            NavigableSet<E> next;
            if (source instanceof NavigableSetView<?> view) {
                next = ((NavigableSetView<E>) view).nav;
            } else if (source instanceof M3FilteredNavigableSet<?> view) {
                next = ((M3FilteredNavigableSet<E>) view).navigationOwner();
            } else {
                next = M3MutableFilteredViews.navigationOwner(source);
            }
            if (next == source) return source;
            source = next;
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> NavigableMap<K, V> navigationOwner(NavigableMap<K, V> source) {
        while (true) {
            NavigableMap<K, V> next = source instanceof NavigableMapView<?, ?> view
                    ? ((NavigableMapView<K, V>) view).nav : M3MutableFilteredViews.navigationOwner(source);
            if (next == source) return source;
            source = next;
        }
    }

    /** Retain the former range-construction type/null check, including empty sets. */
    @SuppressWarnings("unchecked")
    private static <E> void checkQuery(Comparator<? super E> order, E element) {
        if (order == null) {
            ((Comparable<? super E>) Objects.requireNonNull(element)).compareTo(element);
        } else {
            order.compare(element, element);
        }
    }

    /** Probe only an actual stored extreme, never a fabricated null unsupported by the owner. */
    private static <E> boolean nullIsAnchor(NavigableSet<E> source, E element,
            boolean inclusive, boolean descending) {
        Comparator<? super E> order = source.comparator();
        if (order == null) return false;
        Iterator<E> extreme = descending ? source.iterator() : source.descendingIterator();
        if (!extreme.hasNext()) return false;
        int relation = order.compare(extreme.next(), element);
        return descending ? relation < 0 || (inclusive && relation == 0)
                : relation > 0 || (inclusive && relation == 0);
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

    /**
     * Persistent binary-carry predicate composition reused by the filtered List owner.
     * Only M3-created conjunctions are opened; caller predicates remain opaque.
     * Order, duplicate occurrences, short circuiting and thrown objects survive.
     * A perfect subtree becomes an immutable prefix on append; only the smaller
     * suffix is rebuilt. For d leaves, height is ceil(log2(d)), retained storage
     * is O(d), append is O(log d), and testing allocates no traversal buffer.
     * This bounds M3 composition frames, not recursion inside a caller predicate.
     */
    @SuppressWarnings("unchecked")
    static <E> Predicate<E> orderedAnd(
            Predicate<? super E> first, Predicate<? super E> next) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(next, "next");
        FilterNode<Predicate<? super E>> prefix = first instanceof OrderedPredicate<?> old
                ? ((OrderedPredicate<E>) old).root : new FilterNode<>(first);
        return new OrderedPredicate<>(appendFilter(prefix, new FilterNode<>(next)));
    }

    private static final class FilterNode<P> {
        private final P leaf;
        private final FilterNode<P> left;
        private final FilterNode<P> right;
        private final int height;
        private final boolean perfect;

        private FilterNode(P leaf) {
            this.leaf = leaf;
            this.left = null;
            this.right = null;
            this.height = 0;
            this.perfect = true;
        }

        private FilterNode(FilterNode<P> left, FilterNode<P> right) {
            this.leaf = null;
            this.left = left;
            this.right = right;
            this.height = Math.max(left.height, right.height) + 1;
            this.perfect = left.perfect && right.perfect && left.height == right.height;
        }
    }

    private static <P> FilterNode<P> appendFilter(FilterNode<P> prefix, FilterNode<P> leaf) {
        if (prefix.perfect) return new FilterNode<>(prefix, leaf);
        return new FilterNode<>(prefix.left, appendFilter(prefix.right, leaf));
    }

    private static final class OrderedPredicate<E> implements Predicate<E> {
        private final FilterNode<Predicate<? super E>> root;

        private OrderedPredicate(FilterNode<Predicate<? super E>> root) {
            this.root = root;
        }

        @Override
        public boolean test(E element) {
            return evaluate(root, element);
        }

        private static <E> boolean evaluate(FilterNode<Predicate<? super E>> node, E element) {
            if (node.leaf != null) return node.leaf.test(element);
            return evaluate(node.left, element) && evaluate(node.right, element);
        }
    }

    /** Open only private read-only owners whose iterator is exactly the same filter decorator. */
    @SuppressWarnings("unchecked")
    private static <E> CollectionView<E> iterationView(Collection<E> source) {
        if (source instanceof M3FilteredNavigableSet<?> facade) {
            source = ((M3FilteredNavigableSet<E>) facade).navigationOwner();
        }
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

        CollectionView(Collection<E> source, Predicate<? super E> predicate) {
            this.source = Objects.requireNonNull(source, "source");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
        }

        @Override public Iterator<E> iterator() {
            if (iterationView(source) == null) return filtered(source.iterator(), predicate);
            // A depth-sized array replaces nested iterator frames, never backing elements.
            ArrayList<Predicate<? super E>> filters = new ArrayList<>();
            CollectionView<E> layer = this;
            Collection<E> backing;
            do {
                filters.add(layer.predicate);
                backing = layer.source;
                layer = iterationView(backing);
            } while (layer != null);
            return filtered(backing.iterator(), value -> {
                for (int index = filters.size() - 1; index >= 0; index--) {
                    if (!filters.get(index).test(value)) return false;
                }
                return true;
            });
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
            ArrayList<CollectionView<E>> pending = new ArrayList<>();
            CollectionView<E> layer = this;
            CollectionView<E> inner;
            while ((inner = membershipView(layer.source)) != null) {
                pending.add(layer);
                layer = inner;
            }
            if (!visible(layer.source, layer.predicate, value)) return false;
            for (int index = pending.size() - 1; index >= 0; index--) {
                layer = pending.get(index);
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
        @Override public java.util.Spliterator<E> spliterator() { return source.spliterator(); }
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

    private static <K, V> SortedMap<K, V> readOnlySortedMap(SortedMap<K, V> source) {
        return source instanceof SortedMapView<?, ?> || source instanceof ReadOnlySortedMap<?, ?>
                ? source : new ReadOnlySortedMap<>(source);
    }

    /** Only permission checks are added: all reads use the original reversed/range owner. */
    private static final class ReadOnlySortedMap<K, V> extends AbstractMap<K, V> implements SortedMap<K, V> {
        private final SortedMap<K, V> source;
        private ReadOnlySortedMap(SortedMap<K, V> source) { this.source = source; }
        @Override public int size() { return source.size(); }
        @Override public boolean isEmpty() { return source.isEmpty(); }
        @Override public boolean containsKey(Object key) { return source.containsKey(key); }
        @Override public boolean containsValue(Object value) { return source.containsValue(value); }
        @Override public V get(Object key) { return source.get(key); }
        @Override public V getOrDefault(Object key, V defaultValue) { return source.getOrDefault(key, defaultValue); }
        @Override public void forEach(java.util.function.BiConsumer<? super K, ? super V> action) {
            source.forEach(action);
        }
        @Override public boolean equals(Object other) { return other == this || source.equals(other); }
        @Override public int hashCode() { return source.hashCode(); }
        @Override public String toString() { return source.toString(); }
        @Override public Comparator<? super K> comparator() { return source.comparator(); }
        @Override public K firstKey() { return source.firstKey(); }
        @Override public K lastKey() { return source.lastKey(); }
        @Override public Entry<K, V> firstEntry() { return source.firstEntry(); }
        @Override public Entry<K, V> lastEntry() { return source.lastEntry(); }
        @Override public Set<K> keySet() { return Collections.unmodifiableSet(source.keySet()); }
        @Override public Collection<V> values() { return Collections.unmodifiableCollection(source.values()); }
        @Override public Set<Entry<K, V>> entrySet() { return Collections.unmodifiableMap(source).entrySet(); }
        @Override public SequencedSet<K> sequencedKeySet() {
            return Collections.unmodifiableSequencedSet(source.sequencedKeySet());
        }
        @Override public SequencedCollection<V> sequencedValues() {
            return Collections.unmodifiableSequencedCollection(source.sequencedValues());
        }
        @Override public SequencedSet<Entry<K, V>> sequencedEntrySet() {
            return Collections.unmodifiableSequencedSet(source.sequencedEntrySet());
        }
        @Override public SortedMap<K, V> subMap(K from, K to) { return readOnlySortedMap(source.subMap(from, to)); }
        @Override public SortedMap<K, V> headMap(K to) { return readOnlySortedMap(source.headMap(to)); }
        @Override public SortedMap<K, V> tailMap(K from) { return readOnlySortedMap(source.tailMap(from)); }
        @Override public SortedMap<K, V> reversed() { return readOnlySortedMap(source.reversed()); }
        @Override public V put(K key, V value) { throw readonly(); }
        @Override public V remove(Object key) { throw readonly(); }
        @Override public void putAll(Map<? extends K, ? extends V> values) { throw readonly(); }
        @Override public void clear() { throw readonly(); }
        @Override public V putIfAbsent(K key, V value) { throw readonly(); }
        @Override public boolean remove(Object key, Object value) { throw readonly(); }
        @Override public boolean replace(K key, V oldValue, V newValue) { throw readonly(); }
        @Override public V replace(K key, V value) { throw readonly(); }
        @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) { throw readonly(); }
        @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> function) { throw readonly(); }
        @Override public V computeIfPresent(K key,
                BiFunction<? super K, ? super V, ? extends V> function) { throw readonly(); }
        @Override public V compute(K key,
                BiFunction<? super K, ? super V, ? extends V> function) { throw readonly(); }
        @Override public V merge(K key, V value,
                BiFunction<? super V, ? super V, ? extends V> function) { throw readonly(); }
        @Override public V putFirst(K key, V value) { throw readonly(); }
        @Override public V putLast(K key, V value) { throw readonly(); }
        @Override public Entry<K, V> pollFirstEntry() { throw readonly(); }
        @Override public Entry<K, V> pollLastEntry() { throw readonly(); }
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
        @Override public SortedSet<E> reversed() {
            return readOnlySortedSet(SortedSet.super.reversed());
        }
        // An explicit read-only boundary refuses even empty endpoint mutations.
        @Override public void addFirst(E value) { throw readonly(); }
        @Override public void addLast(E value) { throw readonly(); }
        @Override public E removeFirst() { throw readonly(); }
        @Override public E removeLast() { throw readonly(); }
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
        @Override public E lower(E e) {
            return seek(nav, predicate, e, false, true);
        }
        @Override public E floor(E e) {
            return seek(nav, predicate, e, true, true);
        }
        @Override public E ceiling(E e) {
            return seek(nav, predicate, e, true, false);
        }
        @Override public E higher(E e) {
            return seek(nav, predicate, e, false, false);
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
        @Override public NavigableSet<E> reversed() { return descendingSet(); }
        @Override public NavigableSet<E> descendingSet() {
            return new NavigableSetView<>(nav.descendingSet(), predicate);
        }
        @Override public Iterator<E> descendingIterator() { return descendingSet().iterator(); }
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
        @Override public boolean isEmpty() { return !entrySet().iterator().hasNext(); }
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
            return Collections.unmodifiableSet(new AbstractSet<>() {
                @Override public int size() { return SortedMapView.this.size(); }
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
            });
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
        @Override public SortedMap<K, V> reversed() {
            return readOnlySortedMap(SortedMap.super.reversed());
        }
        @Override public Set<K> keySet() {
            return Collections.unmodifiableSet(super.keySet());
        }
        @Override public Collection<V> values() {
            return Collections.unmodifiableCollection(super.values());
        }
        @Override public SequencedSet<K> sequencedKeySet() {
            return Collections.unmodifiableSequencedSet(SortedMap.super.sequencedKeySet());
        }
        @Override public SequencedCollection<V> sequencedValues() {
            return Collections.unmodifiableSequencedCollection(SortedMap.super.sequencedValues());
        }
        @Override public SequencedSet<Entry<K, V>> sequencedEntrySet() {
            return Collections.unmodifiableSequencedSet(SortedMap.super.sequencedEntrySet());
        }
        // Refuse before lookups, callbacks or argument validation, including no-op calls.
        @Override public V putIfAbsent(K key, V value) { throw readonly(); }
        @Override public boolean remove(Object key, Object value) { throw readonly(); }
        @Override public boolean replace(K key, V oldValue, V newValue) { throw readonly(); }
        @Override public V replace(K key, V value) { throw readonly(); }
        @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            throw readonly();
        }
        @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> function) {
            throw readonly();
        }
        @Override public V computeIfPresent(K key,
                BiFunction<? super K, ? super V, ? extends V> function) { throw readonly(); }
        @Override public V compute(K key,
                BiFunction<? super K, ? super V, ? extends V> function) { throw readonly(); }
        @Override public V merge(K key, V value,
                BiFunction<? super V, ? super V, ? extends V> function) { throw readonly(); }
        @Override public V putFirst(K key, V value) { throw readonly(); }
        @Override public V putLast(K key, V value) { throw readonly(); }
        @Override public Entry<K, V> pollFirstEntry() { throw readonly(); }
        @Override public Entry<K, V> pollLastEntry() { throw readonly(); }
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
        /** Map.Entry supplies presence independently from a possibly null stored key. */
        private Entry<K, V> seek(K key, boolean inclusive, boolean descending) {
            NavigableMap<K, V> range;
            try {
                range = descending ? nav.headMap(key, inclusive) : nav.tailMap(key, inclusive);
            } catch (IllegalArgumentException outsideRange) {
                NavigableMap<K, V> owner = navigationOwner(nav);
                checkQuery(owner.comparator(), key);
                Entry<K, V> anchor = descending
                        ? (inclusive ? owner.floorEntry(key) : owner.lowerEntry(key))
                        : (inclusive ? owner.ceilingEntry(key) : owner.higherEntry(key));
                if (anchor == null) return null;
                range = descending ? nav.headMap(anchor.getKey(), true) : nav.tailMap(anchor.getKey(), true);
            }
            if (descending) range = range.descendingMap();
            return firstVisible(range.entrySet().iterator());
        }
        @Override public Entry<K, V> lowerEntry(K key) {
            return seek(key, false, true);
        }
        @Override public K lowerKey(K key) { return keyOf(lowerEntry(key)); }
        @Override public Entry<K, V> floorEntry(K key) {
            return seek(key, true, true);
        }
        @Override public K floorKey(K key) { return keyOf(floorEntry(key)); }
        @Override public Entry<K, V> ceilingEntry(K key) {
            return seek(key, true, false);
        }
        @Override public K ceilingKey(K key) { return keyOf(ceilingEntry(key)); }
        @Override public Entry<K, V> higherEntry(K key) {
            return seek(key, false, false);
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
        @Override public NavigableMap<K, V> reversed() { return descendingMap(); }
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
