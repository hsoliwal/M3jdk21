// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Predicate;

/**
 * Read-only, live filtered view over a NavigableSet.  It deliberately does not
 * change java.util, its contracts, or the backing set's comparator/identity.
 * The predicate should be stable during an operation; a mutable predicate
 * changes view membership on the next query, as for other dynamic filtered views.
 * External concurrent mutation obeys the backing collection's own rules.
 */
public final class M3FilteredNavigableSet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final NavigableSet<E> backing;
    private final Predicate<? super E> predicate;

    private M3FilteredNavigableSet(NavigableSet<E> backing, Predicate<? super E> predicate) {
        this.backing = Objects.requireNonNull(backing, "backing");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
    }

    public static <E> NavigableSet<E> view(NavigableSet<E> backing, Predicate<? super E> predicate) {
        return new M3FilteredNavigableSet<>(backing, predicate);
    }

    @Override
    public Comparator<? super E> comparator() {
        return backing.comparator();
    }

    @Override
    public Iterator<E> iterator() {
        return filteredIterator(backing.iterator());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return filteredIterator(backing.descendingIterator());
    }

    private Iterator<E> filteredIterator(Iterator<E> delegate) {
        return new Iterator<>() {
            private E next;
            private boolean prepared;
            private boolean available;

            @Override
            public boolean hasNext() {
                if (!prepared) {
                    available = false;
                    while (delegate.hasNext()) {
                        E value = delegate.next();
                        if (predicate.test(value)) {
                            next = value;
                            available = true;
                            break;
                        }
                    }
                    prepared = true;
                }
                return available;
            }

            @Override
            public E next() {
                if (!hasNext()) throw new java.util.NoSuchElementException();
                E value = next;
                prepared = false;
                next = null;
                return value;
            }
        };
    }

    @Override
    public int size() {
        int count = 0;
        for (E ignored : this) count++;
        return count;
    }

    @Override
    public boolean isEmpty() {
        return !iterator().hasNext();
    }

    @Override
    public boolean contains(Object value) {
        // Preserve backing-set type/comparator exceptions and avoid unsafe casts.
        if (!backing.contains(value)) return false;
        // A TreeSet's comparator, not Object.equals, defines set membership.
        // Resolve the representative from the backing set before testing the predicate.
        @SuppressWarnings("unchecked")
        E key = (E) value;
        E member = backing.ceiling(key);
        if (member == null) return false;
        Comparator<? super E> order = backing.comparator();
        if (order != null) return order.compare(member, key) == 0 && predicate.test(member);
        @SuppressWarnings("unchecked")
        Comparable<? super E> natural = (Comparable<? super E>) member;
        return natural.compareTo(key) == 0 && predicate.test(member);
    }

    @Override
    public E first() {
        Iterator<E> it = iterator();
        if (!it.hasNext()) throw new java.util.NoSuchElementException();
        return it.next();
    }

    @Override
    public E last() {
        Iterator<E> it = descendingIterator();
        if (!it.hasNext()) throw new java.util.NoSuchElementException();
        return it.next();
    }

    private E firstOrNull(Iterator<E> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }

    /** Resolve the first stored predecessor before creating an in-range cursor. */
    private E predecessor(E probe, boolean inclusive) {
        E first = inclusive ? backing.floor(probe) : backing.lower(probe);
        if (first == null) return null;
        return firstOrNull(filteredIterator(backing.headSet(first, true).descendingIterator()));
    }

    /** Resolve the first stored successor before creating an in-range cursor. */
    private E successor(E probe, boolean inclusive) {
        E first = inclusive ? backing.ceiling(probe) : backing.higher(probe);
        if (first == null) return null;
        return firstOrNull(filteredIterator(backing.tailSet(first, true).iterator()));
    }

    @Override
    public E lower(E e) {
        return predecessor(e, false);
    }

    @Override
    public E floor(E e) {
        return predecessor(e, true);
    }

    @Override
    public E ceiling(E e) {
        return successor(e, true);
    }

    @Override
    public E higher(E e) {
        return successor(e, false);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return view(backing.descendingSet(), predicate);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return view(backing.subSet(fromElement, fromInclusive, toElement, toInclusive), predicate);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return view(backing.headSet(toElement, inclusive), predicate);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return view(backing.tailSet(fromElement, inclusive), predicate);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("read-only view");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("read-only view");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("read-only view");
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("read-only view");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("read-only view");
    }

    @Override
    public boolean removeAll(java.util.Collection<?> values) {
        throw new UnsupportedOperationException("read-only view");
    }

    @Override
    public boolean retainAll(java.util.Collection<?> values) {
        throw new UnsupportedOperationException("read-only view");
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException("read-only view");
    }

    @Override
    public boolean addAll(java.util.Collection<? extends E> values) {
        throw new UnsupportedOperationException("read-only view");
    }
}
