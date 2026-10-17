/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: M3 receiver namespace, runtime-capability dispatch,
 * direct owner membership/removal and single-pass sorted snapshot acquisition.
 * Modified 2026-10-17: stack-safe deferred predicate plans for nested filters.
 * Modified 2026-10-17: java.base receiver; sole implementation behind legacy facade.
 * Modified 2026-10-17: preserve typed traversal metadata and saturating size.
 * Apache notice retained; core integration license review remains a promotion gate.
 */
package java.util;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Composable backed filter views and explicit sorted snapshots of JDK collections.
 *
 * <p>These are reference collection adapters, not replacements for the existing
 * primitive admission filters or primitive indexed views. A filtered view
 * retains only its backing collection and predicate, never one object per entry.
 * Filter predicates should be pure and stable with respect to the equality and
 * ordering semantics of the backing collection.
 *
 * <p>Backed views are not automatically synchronized, serializable or snapshots.
 * Iterator removal is deliberately unsupported; bulk mutations and explicit
 * {@code remove} do write through to the underlying collection when supported.
 *
 * <p>All live wrappers keep the supplied owner authoritative, including nested
 * filters and sorted ranges. They neither sort the owner nor build a second
 * element store. An arbitrary value predicate still requires values to compute
 * exact size; no predicate results are cached without validity evidence. Stream
 * traversal uses an unsized selection cursor rather than an eager counting pass.
 * The separate sortedSnapshot method intentionally creates independent storage.
 */
public final class CollectionViews {
    private CollectionViews() {
    }

    /** Returns a live view, preserving runtime set/sorted/navigable capabilities. */
    @SuppressWarnings("unchecked")
    public static <E> Collection<E> filtered(
            Collection<E> backing, Predicate<? super E> predicate) {
        if (backing instanceof NavigableSet<?>) return filtered((NavigableSet<E>) backing, predicate);
        if (backing instanceof SortedSet<?>) return filtered((SortedSet<E>) backing, predicate);
        if (backing instanceof Set<?>) return filtered((Set<E>) backing, predicate);
        return new FilteredCollection<>(backing, predicate);
    }

    /** Retains runtime sorted capability and set equality in the live view. */
    @SuppressWarnings("unchecked")
    public static <E> Set<E> filtered(Set<E> backing, Predicate<? super E> predicate) {
        if (backing instanceof NavigableSet<?>) return filtered((NavigableSet<E>) backing, predicate);
        if (backing instanceof SortedSet<?>) return filtered((SortedSet<E>) backing, predicate);
        return new FilteredSet<>(backing, predicate);
    }

    /** Retains the comparator, range views and runtime navigable capability. */
    @SuppressWarnings("unchecked")
    public static <E> SortedSet<E> filtered(
            SortedSet<E> backing, Predicate<? super E> predicate) {
        if (backing instanceof NavigableSet<?>) return filtered((NavigableSet<E>) backing, predicate);
        return new FilteredSortedSet<>(backing, predicate);
    }

    /** Retains navigable operations, descending order and range-view contracts. */
    public static <E> NavigableSet<E> filtered(
            NavigableSet<E> backing, Predicate<? super E> predicate) {
        return new FilteredNavigableSet<>(backing, predicate);
    }

    /**
     * Returns an independent, stable, read-only list sorted by the comparator.
     * Equal-comparator elements are not deduplicated. Unlike a backed filter,
     * this operation copies references and does not reflect later source changes.
     */
    public static <E> List<E> sortedSnapshot(
            Collection<? extends E> source, Comparator<? super E> comparator) {
        Objects.requireNonNull(source, "source");
        ArrayList<E> sorted = new ArrayList<>();
        for (E element : source) sorted.add(element);
        sorted.sort(comparator);
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Additive collection API: detached encounter-order snapshot with one predicate
     * evaluation per source occurrence. Unlike a backed filter, no later source
     * additions/removals are reflected. Nulls and duplicate references survive.
     * This never calls source.size(), performs no filter size prepass, and does
     * not change Collection.toArray or any existing JDK/Synexia contract.
     */
    public static <E> List<E> filteredSnapshot(
            Collection<? extends E> source, Predicate<? super E> predicate) {
        return Collections.unmodifiableList(selectedCopy(source, predicate));
    }

    /**
     * Additive collection API: fused filter and stable sort over one detached array
     * owner. Comparator-equivalent elements remain distinct and preserve their
     * original encounter order. A null comparator means natural ordering, as
     * specified by List.sort; mutable element objects are not deep-copied.
     */
    public static <E> List<E> filteredSortedSnapshot(
            Collection<? extends E> source, Predicate<? super E> predicate,
            Comparator<? super E> comparator) {
        ArrayList<E> selected = selectedCopy(source, predicate);
        selected.sort(comparator);
        return Collections.unmodifiableList(selected);
    }

    /** A cohesive one-pass behavioral atom shared by the snapshot APIs. */
    private static <E> ArrayList<E> selectedCopy(
            Collection<? extends E> source, Predicate<? super E> predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        ArrayList<E> selected = new ArrayList<>();
        for (E element : source) {
            if (predicate.test(element)) selected.add(element);
        }
        return selected;
    }

    /**
     * Persistent predicate metadata, not a chain of recursive callback invocations.
     * Each extension shares immutable terms, never an older view or its compiled
     * plan. Construction is O(1); the first test prepares an O(depth) ordered
     * predicate directory. Warm tests use constant stack and allocate no scratch.
     * Only executable steps are cached, never values or predicate outcomes.
     */
    private static final class FilterChain<E> implements Predicate<E> {
        private final FilterTerm<E> tail;
        private final int depth;
        private volatile Predicate<? super E>[] plan;

        @SuppressWarnings("unchecked")
        private FilterChain(Predicate<? super E> existing, Predicate<? super E> added) {
            if (existing instanceof FilterChain<?> nested) {
                FilterChain<E> previous = (FilterChain<E>) nested;
                this.depth = Math.addExact(previous.depth, 1);
                this.tail = new FilterTerm<>(previous.tail, added);
            } else {
                this.depth = 2;
                this.tail = new FilterTerm<>(new FilterTerm<>(null, existing), added);
            }
        }

        @Override
        public boolean test(E element) {
            Predicate<? super E>[] steps = plan;
            if (steps == null) steps = prepare();
            for (Predicate<? super E> step : steps) {
                if (!step.test(element)) return false;
            }
            return true;
        }

        /** User callbacks run outside the publication monitor, in original order. */
        @SuppressWarnings("unchecked")
        private synchronized Predicate<? super E>[] prepare() {
            Predicate<? super E>[] steps = plan;
            if (steps == null) {
                steps = (Predicate<? super E>[]) new Predicate<?>[depth];
                FilterTerm<E> current = tail;
                for (int i = depth - 1; i >= 0; i--) {
                    steps[i] = current.predicate;
                    current = current.previous;
                }
                plan = steps;
            }
            return steps;
        }
    }

    /** Prefix sharing does not retain compiled plans of intermediate wrappers. */
    private static final class FilterTerm<E> {
        private final FilterTerm<E> previous;
        private final Predicate<? super E> predicate;

        private FilterTerm(FilterTerm<E> previous, Predicate<? super E> predicate) {
            this.previous = previous;
            this.predicate = predicate;
        }
    }

    /** A live filter without per-element retained adapters. */
    private static class FilteredCollection<E> extends AbstractCollection<E> {
        protected final Collection<E> backing;
        protected final Predicate<? super E> predicate;

        @SuppressWarnings("unchecked")
        private FilteredCollection(Collection<E> source, Predicate<? super E> filter) {
            Objects.requireNonNull(source, "backing");
            Objects.requireNonNull(filter, "predicate");
            if (source instanceof FilteredCollection<?> nested) {
                FilteredCollection<E> previous = (FilteredCollection<E>) nested;
                this.backing = previous.backing;
                this.predicate = new FilterChain<>(previous.predicate, filter);
            } else {
                this.backing = source;
                this.predicate = filter;
            }
        }

        private boolean visible(E element) {
            return predicate.test(element);
        }

        @Override
        public Iterator<E> iterator() {
            return selected(backing.iterator(), predicate);
        }

        /**
         * Lazy selection over the existing owner's traversal. Metadata requests do
         * not call this view's size() or evaluate its predicate. The upstream
         * estimate is an upper estimate, never an exact filtered cardinality.
         * Binding, splitting and interference behavior follow the source; a source
         * may itself buffer or traverse during splitting. No such buffer is added
         * here. Predicates must be stable and safe for the selected traversal mode.
         */
        @Override
        public Spliterator<E> spliterator() {
            return new SelectionSpliterator<>(backing.spliterator(), predicate);
        }

        @Override
        public int size() {
            int count = 0;
            for (E element : backing) {
                if (visible(element)) {
                    if (count != Integer.MAX_VALUE) count++;
                }
            }
            return count;
        }

        @Override
        public boolean isEmpty() {
            return !iterator().hasNext();
        }

        @Override
        public boolean contains(Object candidate) {
            // Preserve the owner's membership semantics (including identity sets)
            // before evaluating the predicate on its actual stored occurrences.
            if (!backing.contains(candidate)) return false;
            if (backing instanceof Set<?>) {
                E equivalent = null;
                boolean found = false;
                for (E stored : backing) {
                    if (stored == candidate) return visible(stored);
                    if (!found && Objects.equals(candidate, stored)) {
                        equivalent = stored;
                        found = true;
                    }
                }
                return found && visible(equivalent);
            }
            for (E stored : backing) {
                if (Objects.equals(candidate, stored) && visible(stored)) return true;
            }
            return false;
        }

        @Override
        public boolean add(E element) {
            if (!visible(element)) {
                throw new IllegalArgumentException("element rejected by view predicate");
            }
            return backing.add(element);
        }

        @Override
        public boolean addAll(Collection<? extends E> elements) {
            Objects.requireNonNull(elements, "elements");
            for (E element : elements) {
                if (!visible(element)) {
                    throw new IllegalArgumentException("element rejected by view predicate");
                }
            }
            return backing.addAll(elements);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean remove(Object candidate) {
            if (!backing.contains(candidate)) return false;
            if (backing instanceof List<?>) {
                // Remove the visible physical occurrence, not an earlier hidden
                // equals-duplicate. Indexed removal also supports COW lists.
                List<E> list = (List<E>) backing;
                int physical = 0;
                for (E stored : list) {
                    if (Objects.equals(candidate, stored) && visible(stored)) {
                        list.remove(physical);
                        return true;
                    }
                    physical++;
                }
                return false;
            }
            if (backing instanceof Set<?>) {
                E equivalent = null;
                boolean found = false;
                for (E stored : backing) {
                    if (stored == candidate) {
                        return visible(stored) && backing.remove(stored);
                    }
                    if (!found && Objects.equals(candidate, stored)) {
                        equivalent = stored;
                        found = true;
                    }
                }
                return found && visible(equivalent) && backing.remove(equivalent);
            }
            Iterator<E> iterator = backing.iterator();
            boolean skippedHiddenEqual = false;
            while (iterator.hasNext()) {
                E stored = iterator.next();
                if (Objects.equals(candidate, stored)) {
                    if (visible(stored)) {
                        try {
                            iterator.remove();
                            return true;
                        } catch (UnsupportedOperationException unsupported) {
                            if (skippedHiddenEqual) throw unsupported;
                            return backing.remove(stored);
                        }
                    }
                    skippedHiddenEqual = true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            removeMatching(element -> true);
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            Objects.requireNonNull(filter, "filter");
            return removeMatching(filter);
        }

        @Override
        public boolean removeAll(Collection<?> elements) {
            Objects.requireNonNull(elements, "elements");
            return removeMatching(elements::contains);
        }

        @Override
        public boolean retainAll(Collection<?> elements) {
            Objects.requireNonNull(elements, "elements");
            return removeMatching(element -> !elements.contains(element));
        }

        private boolean removeMatching(Predicate<? super E> condition) {
            boolean changed = false;
            Iterator<E> iterator = backing.iterator();
            while (iterator.hasNext()) {
                E element = iterator.next();
                if (visible(element) && condition.test(element)) {
                    iterator.remove();
                    changed = true;
                }
            }
            return changed;
        }
    }

    /** Reuses all collection mutations while implementing the Set value contract. */
    private static class FilteredSet<E> extends FilteredCollection<E> implements Set<E> {
        private FilteredSet(Set<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
        }

        @Override
        public Spliterator<E> spliterator() {
            return new SelectionSpliterator<>(backing.spliterator(), predicate,
                    Spliterator.DISTINCT, null);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Set<?> otherSet)) {
                return false;
            }
            try {
                return size() == otherSet.size() && containsAll(otherSet);
            } catch (ClassCastException | NullPointerException ex) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (E element : this) {
                if (element != null) {
                    hash += element.hashCode();
                }
            }
            return hash;
        }
    }

    /** Filtered sorted range views remain backed and inherit source comparators. */
    private static class FilteredSortedSet<E> extends FilteredSet<E> implements SortedSet<E> {
        private final SortedSet<E> sorted;

        private FilteredSortedSet(SortedSet<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
            this.sorted = (SortedSet<E>) super.backing;
        }

        // M3_SORTED_COMPARATOR_EQUIVALENCE_V1
        /**
         * A SortedSet locates elements by comparator equivalence, not Object.equals.
         * Test visibility on the stored representative rather than the probe.
         */
        @Override
        public boolean contains(Object probe) {
            if (!sorted.contains(probe)) {
                return false;
            }
            return super.predicate.test(storedRepresentative(probe));
        }

        @Override
        public boolean remove(Object probe) {
            if (!sorted.contains(probe)) {
                return false;
            }
            E stored = storedRepresentative(probe);
            return super.predicate.test(stored) && sorted.remove(stored);
        }

        @SuppressWarnings("unchecked")
        private E storedRepresentative(Object probe) {
            // contains() has already validated this exact comparator class.
            return sorted.tailSet((E) probe).first();
        }

        @Override
        public Comparator<? super E> comparator() {
            return sorted.comparator();
        }

        /** Owner order is authoritative, including bounded and descending views. */
        @Override
        public Spliterator<E> spliterator() {
            return new SelectionSpliterator<>(sorted.spliterator(), super.predicate,
                    Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED,
                    sorted.comparator());
        }

        @Override
        public SortedSet<E> subSet(E fromInclusive, E toExclusive) {
            return filtered(sorted.subSet(fromInclusive, toExclusive), super.predicate);
        }

        @Override
        public SortedSet<E> headSet(E toExclusive) {
            return filtered(sorted.headSet(toExclusive), super.predicate);
        }

        @Override
        public SortedSet<E> tailSet(E fromInclusive) {
            return filtered(sorted.tailSet(fromInclusive), super.predicate);
        }

        @Override
        public E first() {
            Iterator<E> iterator = iterator();
            if (!iterator.hasNext()) {
                throw new NoSuchElementException();
            }
            return iterator.next();
        }

        @Override
        public E last() {
            Iterator<E> iterator = iterator();
            if (!iterator.hasNext()) {
                throw new NoSuchElementException();
            }
            E current;
            do {
                current = iterator.next();
            } while (iterator.hasNext());
            return current;
        }
    }

    /** Filtered navigation seeks through ordered source ranges without an index copy. */
    private static final class FilteredNavigableSet<E> extends FilteredSortedSet<E>
            implements NavigableSet<E> {
        private final NavigableSet<E> navigable;

        private FilteredNavigableSet(NavigableSet<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
            this.navigable = (NavigableSet<E>) super.backing;
        }

        @Override
        public E lower(E element) {
            return seek(element, false, true);
        }

        @Override
        public E floor(E element) {
            return seek(element, true, true);
        }

        @Override
        public E ceiling(E element) {
            return seek(element, true, false);
        }

        @Override
        public E higher(E element) {
            return seek(element, false, false);
        }

        /**
         * A navigation key need not lie inside a backing range. Only construct a
         * new range when the key lies between its current first/last members;
         * otherwise use the whole ordered cursor or an empty result. This keeps
         * O(1) scratch and the backing seek plus rejected-member scan, without
         * treating a legitimate null member as an exhaustion sentinel.
         */
        private E seek(E element, boolean inclusive, boolean descending) {
            NavigableSet<E> ordered = descending ? navigable.descendingSet() : navigable;
            Comparator<? super E> order = ordered.comparator();
            compareKey(element, element, order); // Keep key-domain admission on empty sets.
            Iterator<E> iterator = ordered.iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            int fromFirst = compareKey(element, iterator.next(), order);
            if (fromFirst < 0 || fromFirst == 0 && inclusive) {
                return firstOrNull(selected(ordered.iterator(), super.predicate));
            }
            int fromLast = compareKey(element, ordered.last(), order);
            if (fromLast > 0 || fromLast == 0 && !inclusive) {
                return null;
            }
            return firstOrNull(selected(ordered.tailSet(element, inclusive).iterator(), super.predicate));
        }

        @SuppressWarnings("unchecked")
        private int compareKey(E left, E right, Comparator<? super E> order) {
            return order == null ? ((Comparable<? super E>) left).compareTo(right) : order.compare(left, right);
        }

        @Override
        public E last() {
            Iterator<E> iterator = descendingIterator();
            if (!iterator.hasNext()) {
                throw new NoSuchElementException();
            }
            return iterator.next();
        }

        @Override
        public E pollFirst() {
            return removeFirst(iterator());
        }

        @Override
        public E pollLast() {
            return removeFirst(descendingIterator());
        }

        private E removeFirst(Iterator<E> iterator) {
            if (!iterator.hasNext()) {
                return null;
            }
            E selected = iterator.next();
            navigable.remove(selected);
            return selected;
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return filtered(navigable.descendingSet(), super.predicate);
        }

        @Override
        public Iterator<E> descendingIterator() {
            return selected(navigable.descendingIterator(), super.predicate);
        }

        @Override
        public NavigableSet<E> subSet(
                E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return filtered(navigable.subSet(
                    fromElement, fromInclusive, toElement, toInclusive), super.predicate);
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return filtered(navigable.headSet(toElement, inclusive), super.predicate);
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return filtered(navigable.tailSet(fromElement, inclusive), super.predicate);
        }
    }

    /** Per-traversal state only; no cached element, copied index or result array. */
    private static final class SelectionSpliterator<E> implements Spliterator<E>, Consumer<E> {
        private final Spliterator<E> source;
        private final Predicate<? super E> predicate;
        private final int guaranteed;
        private final Comparator<? super E> ownerComparator;
        private Consumer<? super E> action;
        private boolean delivered;

        private SelectionSpliterator(Spliterator<E> source, Predicate<? super E> predicate) {
            this(source, predicate, 0, null);
        }

        private SelectionSpliterator(Spliterator<E> source, Predicate<? super E> predicate,
                int guaranteed, Comparator<? super E> ownerComparator) {
            this.source = source;
            this.predicate = predicate;
            this.guaranteed = guaranteed;
            this.ownerComparator = ownerComparator;
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer, "action");
            action = consumer;
            delivered = false;
            try {
                while (!delivered && source.tryAdvance(this)) {
                    // Advance past rejected values without a second scan or buffer.
                }
                return delivered;
            } finally {
                action = null;
            }
        }

        @Override
        public void accept(E element) {
            if (predicate.test(element)) {
                delivered = true;
                action.accept(element);
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer, "action");
            // The callback is operation-local, never one allocation per element.
            source.forEachRemaining(element -> {
                if (predicate.test(element)) consumer.accept(element);
            });
        }

        @Override
        public Spliterator<E> trySplit() {
            Spliterator<E> prefix = source.trySplit();
            return prefix == null ? null : new SelectionSpliterator<>(prefix, predicate, guaranteed, ownerComparator);
        }

        @Override
        public long estimateSize() {
            return source.estimateSize();
        }

        @Override
        public int characteristics() {
            return (source.characteristics() & (ORDERED | DISTINCT | SORTED | NONNULL | IMMUTABLE)) | guaranteed;
        }

        @Override
        public Comparator<? super E> getComparator() {
            return (guaranteed & SORTED) != 0 ? ownerComparator : source.getComparator();
        }
    }

    private static <E> E firstOrNull(Iterator<E> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }

    private static <E> Iterator<E> selected(
            Iterator<E> source, Predicate<? super E> predicate) {
        return new Iterator<>() {
            private boolean prepared;
            private boolean found;
            private E next;

            @Override
            public boolean hasNext() {
                if (!prepared) {
                    prepared = true;
                    while (source.hasNext()) {
                        E candidate = source.next();
                        if (predicate.test(candidate)) {
                            next = candidate;
                            found = true;
                            break;
                        }
                    }
                }
                return found;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                E result = next;
                next = null;
                prepared = false;
                found = false;
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("filtered iterator removal");
            }
        };
    }
}
