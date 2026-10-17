/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: M3 receiver namespace, runtime-capability dispatch,
 * direct owner membership/removal and single-pass sorted snapshot acquisition.
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
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
 * The generic selection iterator deliberately does not expose removal. Filtered
 * List views additionally implement indexed and ListIterator mutations against
 * the authoritative backing list. Bulk mutations and explicit {@code remove}
 * write through when supported.
 *
 * <p>All live wrappers keep the supplied owner authoritative, including nested
 * filters and sorted ranges. They neither sort the owner nor build a second
 * element store. An arbitrary value predicate still requires values to compute
 * exact size; no predicate results are cached without validity evidence. Stream
 * traversal uses an unsized selection cursor rather than an eager counting pass.
 * The separate sortedSnapshot method intentionally creates independent storage.
 */
public final class M3CollectionViews {
    private M3CollectionViews() {
    }

    /** Returns a live view, preserving runtime set/sorted/navigable capabilities. */
    @SuppressWarnings("unchecked")
    public static <E> Collection<E> filtered(
            Collection<E> backing, Predicate<? super E> predicate) {
        if (backing instanceof NavigableSet<?>) return filtered((NavigableSet<E>) backing, predicate);
        if (backing instanceof SortedSet<?>) return filtered((SortedSet<E>) backing, predicate);
        if (backing instanceof Set<?>) return filtered((Set<E>) backing, predicate);
        if (backing instanceof List<?>) return filtered((List<E>) backing, predicate);
        return new FilteredCollection<>(backing, predicate);
    }

    /**
     * Returns a live filtered List whose indexes are indexes in the visible sequence.
     *
     * <p>No visible-index array is retained. Indexed operations map one requested
     * visible coordinate to the authoritative backing list on demand. A terminal
     * insertion maps to {@code backing.size()}, preserving hidden trailing values.
     * The wrapper advertises {@link RandomAccess} only when the supplied list does.
     */
    public static <E> List<E> filtered(
            List<E> backing, Predicate<? super E> predicate) {
        Objects.requireNonNull(backing, "backing");
        Objects.requireNonNull(predicate, "predicate");
        return backing instanceof RandomAccess
                ? new RandomAccessFilteredList<>(backing, predicate)
                : new FilteredList<>(backing, predicate);
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
                Predicate<? super E> existing = previous.predicate;
                this.predicate = element -> existing.test(element) && filter.test(element);
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
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean isEmpty() {
            return !iterator().hasNext();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean contains(Object candidate) {
            return backing.contains(candidate) && predicate.test((E) candidate);
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
        public boolean remove(Object candidate) {
            return contains(candidate) && backing.remove(candidate);
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

    /**
     * Live List projection with visible-sequence indexes and no retained index.
     *
     * <p>Hidden elements keep their backing positions. For an insertion before an
     * existing visible element, the new value is inserted immediately before that
     * stored representative. The terminal visible boundary maps to the end of the
     * backing list. ListIterator state is maintained in visible coordinates.</p>
     */
    private static class FilteredList<E> extends FilteredCollection<E> implements List<E> {
        protected final List<E> list;

        @SuppressWarnings("unchecked")
        private FilteredList(List<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
            this.list = (List<E>) super.backing;
        }

        @Override
        public E get(int index) {
            return list.get(backingIndex(index));
        }

        @Override
        public E set(int index, E element) {
            requireVisible(element);
            return list.set(backingIndex(index), element);
        }

        @Override
        public void add(int index, E element) {
            requireVisible(element);
            list.add(insertionBoundary(index), element);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> elements) {
            Objects.requireNonNull(elements, "elements");
            int boundary = insertionBoundary(index);
            for (E element : elements) requireVisible(element);
            return list.addAll(boundary, elements);
        }

        @Override
        public E remove(int index) {
            return list.remove(backingIndex(index));
        }

        @Override
        public boolean remove(Object candidate) {
            ListIterator<E> iterator = list.listIterator();
            while (iterator.hasNext()) {
                E stored = iterator.next();
                if (super.predicate.test(stored) && Objects.equals(stored, candidate)) {
                    iterator.remove();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean contains(Object candidate) {
            return indexOf(candidate) >= 0;
        }

        @Override
        public int indexOf(Object candidate) {
            int visible = 0;
            for (E stored : list) {
                if (super.predicate.test(stored)) {
                    if (Objects.equals(stored, candidate)) return visible;
                    visible++;
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object candidate) {
            int visible = 0;
            int result = -1;
            for (E stored : list) {
                if (super.predicate.test(stored)) {
                    if (Objects.equals(stored, candidate)) result = visible;
                    visible++;
                }
            }
            return result;
        }

        @Override
        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            int currentSize = size();
            if (index < 0 || index > currentSize) {
                throw new IndexOutOfBoundsException(
                        "visible index " + index + " outside [0," + currentSize + "]");
            }
            return new FilteredListIterator(index);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            int currentSize = size();
            Objects.checkFromToIndex(fromIndex, toIndex, currentSize);
            int from = insertionBoundary(fromIndex);
            int to = insertionBoundary(toIndex);
            return filtered(list.subList(from, to), super.predicate);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof List<?> otherList) || otherList.size() != size()) return false;
            Iterator<E> left = iterator();
            Iterator<?> right = otherList.iterator();
            while (left.hasNext() && right.hasNext()) {
                if (!Objects.equals(left.next(), right.next())) return false;
            }
            return !left.hasNext() && !right.hasNext();
        }

        @Override
        public int hashCode() {
            int hash = 1;
            for (E element : this) {
                hash = 31 * hash + (element == null ? 0 : element.hashCode());
            }
            return hash;
        }

        private void requireVisible(E element) {
            if (!super.predicate.test(element)) {
                throw new IllegalArgumentException("element rejected by view predicate");
            }
        }

        private int backingIndex(int visibleIndex) {
            if (visibleIndex < 0) {
                throw new IndexOutOfBoundsException("negative visible index: " + visibleIndex);
            }
            int visible = 0;
            ListIterator<E> iterator = list.listIterator();
            while (iterator.hasNext()) {
                int backingIndex = iterator.nextIndex();
                E element = iterator.next();
                if (super.predicate.test(element)) {
                    if (visible == visibleIndex) return backingIndex;
                    visible++;
                }
            }
            throw new IndexOutOfBoundsException(
                    "visible index " + visibleIndex + " >= " + visible);
        }

        private int insertionBoundary(int visibleIndex) {
            if (visibleIndex < 0) {
                throw new IndexOutOfBoundsException("negative visible index: " + visibleIndex);
            }
            int visible = 0;
            ListIterator<E> iterator = list.listIterator();
            while (iterator.hasNext()) {
                int backingIndex = iterator.nextIndex();
                E element = iterator.next();
                if (super.predicate.test(element)) {
                    if (visible == visibleIndex) return backingIndex;
                    visible++;
                }
            }
            if (visible == visibleIndex) return list.size();
            throw new IndexOutOfBoundsException(
                    "visible index " + visibleIndex + " > " + visible);
        }

        private final class FilteredListIterator implements ListIterator<E> {
            private int cursor;
            private int lastReturned = -1;

            private FilteredListIterator(int index) {
                cursor = index;
            }

            @Override
            public boolean hasNext() {
                return cursor < FilteredList.this.size();
            }

            @Override
            public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                E result = FilteredList.this.get(cursor);
                lastReturned = cursor;
                cursor++;
                return result;
            }

            @Override
            public boolean hasPrevious() {
                return cursor > 0;
            }

            @Override
            public E previous() {
                if (!hasPrevious()) throw new NoSuchElementException();
                cursor--;
                E result = FilteredList.this.get(cursor);
                lastReturned = cursor;
                return result;
            }

            @Override
            public int nextIndex() {
                return cursor;
            }

            @Override
            public int previousIndex() {
                return cursor - 1;
            }

            @Override
            public void remove() {
                if (lastReturned < 0) throw new IllegalStateException();
                FilteredList.this.remove(lastReturned);
                if (lastReturned < cursor) cursor--;
                lastReturned = -1;
            }

            @Override
            public void set(E element) {
                if (lastReturned < 0) throw new IllegalStateException();
                FilteredList.this.set(lastReturned, element);
            }

            @Override
            public void add(E element) {
                FilteredList.this.add(cursor, element);
                cursor++;
                lastReturned = -1;
            }
        }
    }

    private static final class RandomAccessFilteredList<E> extends FilteredList<E>
            implements RandomAccess {
        private RandomAccessFilteredList(
                List<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
        }
    }

    /** Reuses all collection mutations while implementing the Set value contract. */
    private static class FilteredSet<E> extends FilteredCollection<E> implements Set<E> {
        private FilteredSet(Set<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
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
        private Consumer<? super E> action;
        private boolean delivered;

        private SelectionSpliterator(Spliterator<E> source, Predicate<? super E> predicate) {
            this.source = source;
            this.predicate = predicate;
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
            return prefix == null ? null : new SelectionSpliterator<>(prefix, predicate);
        }

        @Override
        public long estimateSize() {
            return source.estimateSize();
        }

        @Override
        public int characteristics() {
            return source.characteristics() & (ORDERED | DISTINCT | SORTED | NONNULL | IMMUTABLE);
        }

        @Override
        public Comparator<? super E> getComparator() {
            return source.getComparator();
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
