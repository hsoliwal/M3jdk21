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
// Modified 2026 by Hitesh Soliwal and contributors: reuse direct first-match selection without private iterator adapters.
// Modified 2026 by Hitesh Soliwal and contributors: reconcile merged sequenced guards and retain reversed split ordering.
// Modified 2026 by Hitesh Soliwal and contributors: preserve comparator metadata across filtered splits.
// Reconciled 2026: retain the qualified guarded reverse atom after additive merge.
// Modified 2026 by Hitesh Soliwal and contributors: fail-fast owner epoch on filtered cursors over M3 sorted owners (census2 P2-098).
/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: interior-null navigation through bounded ordered cursors.
 */
package jdk.m3.collections;

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
 * Over an M3TreeMap or M3TreeSet owner that policy is fail-fast at every cursor call: the
 * owner revision is read before the backing cursor opens and checked before and after each
 * step, so prepared and exhausted cursors and mutating predicates or comparators also throw
 * ConcurrentModificationException.
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

    //FLLDCIM: MRL - census2 P2-098, recipe FILTERED_VIEWS_OWNER v1 (crate m3jdk21-20261019, donor blob a6d82d9a076f = retained variant pr1463): owner epoch before the cursor opens
    /** The array-backed AVL owner of an M3 sorted map or set (bounds and direction included), else null. */
    private static M3TreeMap<?, ?> sortedOwner(Object source) {
        if (source instanceof M3TreeMap<?, ?> map) return map;
        return source instanceof M3TreeSet<?> set ? set.indexedOwner() : null;
    }

    private static int revisionOf(M3TreeMap<?, ?> owner) { return owner == null ? 0 : owner.revision(); }

    /**
     * Check a sorted M3 owner's revision, read before {@code cursor} was opened, at every call
     * and after every step; any other source keeps its own iterator policy. Callbacks are
     * unchanged, and a predicate exception still precedes the check of the step that raised it.
     */
    private static <E> Iterator<E> ownerChecked(M3TreeMap<?, ?> owner, int expected,
            Iterator<E> cursor) {
        if (owner == null) return cursor;
        return new Iterator<>() {
            @Override public boolean hasNext() {
                owner.check(expected);
                boolean ready = cursor.hasNext();
                owner.check(expected);
                return ready;
            }
            @Override public E next() {
                owner.check(expected);
                E result = cursor.next();
                owner.check(expected);
                return result;
            }
            @Override public void remove() { throw readonly(); }
        };
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

    //FLLDCIM: MRL - mergeloss2 leaf 27: sorted-owner membership restored from origin 01952e935de (PR #1619), dropped by retain merge 1633da209622; recipe qualification/mergeloss2-filtered-views-20261107
    /**
     * Resolve only our own private, identity-preserving view layers. The backing
     * SortedSet owns comparator equivalence; equality-based alias scans do not.
     * This is iterative to avoid recursive wrapper-depth growth.
     */
    @SuppressWarnings("unchecked")
    private static <E> SortedSet<E> sortedMembershipOwner(Collection<E> source) {
        Collection<E> owner = source;
        while (true) {
            if (owner instanceof SortedSet<?> sorted) return (SortedSet<E>) sorted;
            if (owner.getClass() == SetView.class) {
                owner = ((SetView<E>) owner).view.source;
            } else if (owner.getClass() == CollectionView.class) {
                owner = ((CollectionView<E>) owner).source;
            } else {
                return null;
            }
        }
    }

    /**
     * Membership has already succeeded: a sorted owner (directly or under private view layers)
     * evaluates its retained representative; any other source keeps the original representative
     * probe order.
     */
    private static <E> boolean visibleMember(Collection<E> source,
            Predicate<? super E> predicate, Object value) {
        SortedSet<E> sorted = sortedMembershipOwner(source);
        if (sorted != null) {
            @SuppressWarnings("unchecked") E alias = (E) value;
            if (sorted instanceof NavigableSet<?> nav) {
                // ceiling uses the stored representative without a new tail-range view.
                @SuppressWarnings("unchecked") NavigableSet<E> navigable = (NavigableSet<E>) nav;
                return predicate.test(navigable.ceiling(alias));
            }
            return predicate.test(sorted.tailSet(alias).first());
        }
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
            if (iterationView(source) == null) {
                M3TreeMap<?, ?> owner = sortedOwner(source);
                int expected = revisionOf(owner);
                return ownerChecked(owner, expected, filtered(source.iterator(), predicate));
            }
            IterationPlan<E> plan = iterationPlan();
            M3TreeMap<?, ?> owner = sortedOwner(plan.backing);
            int expected = revisionOf(owner);
            return ownerChecked(owner, expected, filtered(plan.backing.iterator(), value -> {
                for (Predicate<? super E> filter : plan.filters) {
                    if (!filter.test(value)) return false;
                }
                return true;
            }));
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
        /**
         * Bounded views admit navigation queries outside their own endpoints,
         * whereas subSet/headSet/tailSet may reject those same query endpoints.
         * Start from the backing navigational result, never the query alias.
         * A null result can also designate a real comparator-admitted null member.
         */
        private boolean nullIsCandidate(E query, boolean forward, boolean inclusive) {
            Comparator<? super E> order = nav.comparator();
            // Natural order and its standard reverse cannot admit null. Keep
            // their qualified no-result path free of extra Comparable callbacks.
            if (order == null || order == Collections.reverseOrder() || nav.isEmpty()) return false;
            // Preserve the qualified empty/first/conditional-last provider calls.
            // A null member need not be an extreme: a stored directional boundary
            // proves candidate presence without fabricating a null comparison.
            E first = nav.first();
            E last = first == null ? null : nav.last();
            E boundary = first == null || last == null ? null : (forward ? last : first);
            int direction = order.compare(boundary, query);
            return forward ? (inclusive ? direction >= 0 : direction > 0)
                    : (inclusive ? direction <= 0 : direction < 0);
        }
        private E firstVisible(Iterator<E> cursor) {
            while (cursor.hasNext()) {
                E candidate = cursor.next();
                if (predicate.test(candidate)) return candidate;
            }
            return null;
        }
        private E navigate(E candidate, E query, boolean forward, boolean inclusive) {
            if (candidate == null && !nullIsCandidate(query, forward, inclusive)) return null;
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
        @Override public NavigableSet<E> reversed() { return descendingSet(); }
        //FLLDCIM: MRL - mergeloss2 leaf 42: deep descending iteration restored from origin 793988c0c6a (PR #1368), dropped by retain merge 082b54fa4ab7; recipe qualification/mergeloss2-filtered-views-20261107
        @Override public Iterator<E> descendingIterator() {
            if (!(nav instanceof NavigableSetView<?>)) {
                M3TreeMap<?, ?> owner = sortedOwner(nav);
                int expected = revisionOf(owner);
                return ownerChecked(owner, expected, filtered(nav.descendingSet().iterator(), predicate));
            }
            // Reuse the ascending iterator's depth-sized predicate plan, opening
            // the original descending owner only after the private chain is peeled.
            ArrayList<Predicate<? super E>> filters = new ArrayList<>();
            NavigableSetView<E> layer = this;
            NavigableSet<E> backing;
            while (true) {
                filters.add(layer.predicate);
                backing = layer.nav;
                if (!(backing instanceof NavigableSetView<?> nested)) break;
                @SuppressWarnings("unchecked")
                NavigableSetView<E> inner = (NavigableSetView<E>) nested;
                layer = inner;
            }
            Collection<E> descending = backing.descendingSet();
            for (int index = filters.size() - 1; index >= 0; index--) {
                descending = new CollectionView<>(descending, filters.get(index));
            }
            return descending.iterator();
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
        /**
         * Walk only M3TreeMap's authoritative slot lanes. The order of checks,
         * next-slot advancement and key lookup matches its public entry iterator:
         * the public LiveEntry.getValue performs a fresh key lookup and invokes
         * user comparators. Omitting that lookup changes observable callbacks.
         *
         * For arbitrary SortedMap providers retain the original entry iterator.
         * This is a private traversal optimization, not a public Map.Entry change.
         */
        private int countTreeSlots(M3TreeMap<K, V> owner, boolean stopAtFirst) {
            int slot = owner.firstSlot();
            int expected = owner.revision();
            int count = 0;
            while (slot >= 0) {
                owner.check(expected);
                K key = owner.keyAt(slot);
                owner.check(expected);
                int following = owner.nextSlot(slot);
                owner.cacheEntryFacades();
                int resolved = owner.entrySlot(key, -1, -1);
                V value = resolved < 0 ? null : owner.valueAt(resolved);
                if (predicate.test(key, value)) {
                    count++;
                    if (stopAtFirst) return count;
                }
                slot = following;
            }
            owner.check(expected);
            return count;
        }
        @Override public int size() {
            if (sorted instanceof M3TreeMap<K, V> owner) {
                return countTreeSlots(owner, false);
            }
            int count = 0;
            for (Entry<K, V> entry : sorted.entrySet())
                if (predicate.test(entry.getKey(), entry.getValue())) count++;
            return count;
        }
        @Override public boolean isEmpty() {
            if (sorted instanceof M3TreeMap<K, V> owner) {
                return countTreeSlots(owner, true) == 0;
            }
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
                    M3TreeMap<?, ?> owner = sortedOwner(sorted);
                    int expected = revisionOf(owner);
                    Iterator<Entry<K, V>> cursor = sorted.entrySet().iterator();
                    Iterator<Entry<K, V>> filtered = M3FilteredViews.filtered(cursor,
                            item -> predicate.test(item.getKey(), item.getValue()));
                    // The entry copy reads the value through the owner (a comparator lookup):
                    // the owner check after next() also covers that callback.
                    return ownerChecked(owner, expected, new Iterator<Entry<K, V>>() {
                        @Override public boolean hasNext() { return filtered.hasNext(); }
                        @Override public Entry<K, V> next() {
                            return new SimpleImmutableEntry<>(filtered.next());
                        }
                        @Override public void remove() { throw readonly(); }
                    });
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
            while (cursor.hasNext()) {
                Entry<K, V> candidate = cursor.next();
                if (predicate.test(candidate.getKey(), candidate.getValue())) {
                    return new SimpleImmutableEntry<>(candidate);
                }
            }
            return null;
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
