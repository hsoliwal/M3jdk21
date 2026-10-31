// Modified 2026 by Hitesh Soliwal and contributors: preserve comparator metadata across filtered splits.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.Predicate;

/**
 * Compatibility facade for the canonical read-only {@link M3FilteredViews}
 * NavigableSet implementation.
 *
 * <p>The public type and factory remain available for existing M3 callers, but filtering,
 * representative resolution, ordered navigation and range semantics have a single implementation
 * owner.  Range and descending operations return this facade again so callers which intentionally
 * retain the M3 compatibility type do not fall through to an unrelated wrapper class.
 *
 * <p>No element payload, index, mask or predicate outcome is copied or cached.  The delegate is a
 * live backed view.  Every mutation refuses, including no-op bulk methods and iterator removal.
 * Arbitrary Java predicates and comparators remain in Java; primitive state-mask/JNI owners are
 * separate precomputed-coordinate contracts.
 */
public final class M3FilteredNavigableSet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final NavigableSet<E> delegate;

    private M3FilteredNavigableSet(NavigableSet<E> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static <E> NavigableSet<E> view(
            NavigableSet<E> backing, Predicate<? super E> predicate) {
        Objects.requireNonNull(backing, "backing");
        Objects.requireNonNull(predicate, "predicate");
        return new M3FilteredNavigableSet<>(M3FilteredViews.filter(backing, predicate));
    }

    private static <E> NavigableSet<E> wrap(NavigableSet<E> view) {
        return new M3FilteredNavigableSet<>(view);
    }

    @Override public Comparator<? super E> comparator() { return delegate.comparator(); }
    @Override public Iterator<E> iterator() { return delegate.iterator(); }
    @Override public Spliterator<E> spliterator() { return delegate.spliterator(); }
    @Override public Iterator<E> descendingIterator() { return delegate.descendingIterator(); }
    @Override public int size() { return delegate.size(); }
    @Override public boolean isEmpty() { return delegate.isEmpty(); }
    @Override public boolean contains(Object value) { return delegate.contains(value); }
    @Override public E first() { return delegate.first(); }
    @Override public E last() { return delegate.last(); }
    @Override public E lower(E value) { return delegate.lower(value); }
    @Override public E floor(E value) { return delegate.floor(value); }
    @Override public E ceiling(E value) { return delegate.ceiling(value); }
    @Override public E higher(E value) { return delegate.higher(value); }

    @Override public NavigableSet<E> descendingSet() {
        return wrap(delegate.descendingSet());
    }

    @Override public NavigableSet<E> subSet(
            E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return wrap(delegate.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    @Override public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return wrap(delegate.headSet(toElement, inclusive));
    }

    @Override public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return wrap(delegate.tailSet(fromElement, inclusive));
    }

    @Override public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    private static UnsupportedOperationException readonly() {
        return new UnsupportedOperationException("read-only view");
    }

    @Override public boolean add(E value) { throw readonly(); }
    @Override public boolean remove(Object value) { throw readonly(); }
    @Override public boolean addAll(Collection<? extends E> values) { throw readonly(); }
    @Override public boolean removeAll(Collection<?> values) { throw readonly(); }
    @Override public boolean retainAll(Collection<?> values) { throw readonly(); }
    @Override public boolean removeIf(Predicate<? super E> predicate) { throw readonly(); }
    @Override public void clear() { throw readonly(); }
    @Override public E pollFirst() { throw readonly(); }
    @Override public E pollLast() { throw readonly(); }
}
