/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.pcollections;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.pcollections.AbstractUnmodifiableSet;
import org.pcollections.PSortedSet;

/**
 * Persistent comparator set over sorted M3 vector coordinates. Ranges are clipped
 * immutable membership snapshots; persistent additions can extend a former range.
 * Descending order is metadata and ranges share vector pages. Comparator-equivalent
 * insertion retains the first element reference. Natural order rejects null;
 * a supplied comparator may admit it. The comparator accessor is never null.
 * Search uses O(log visible-size * log retained-size) vector-backed binary search.
 */
public final class M3PSortedSet<E> extends AbstractUnmodifiableSet<E>
        implements PSortedSet<E>, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    final M3PVector<E> elements;
    // As with TreeSet, serialization requires a serializable comparator.
    @SuppressWarnings("serial") final Comparator<? super E> order;
    final boolean descending;

    M3PSortedSet(M3PVector<E> elements, Comparator<? super E> order, boolean descending) {
        this.elements = Objects.requireNonNull(elements, "elements");
        this.order = Objects.requireNonNull(order, "comparator");
        this.descending = descending;
    }

    public static <E extends Comparable<? super E>> M3PSortedSet<E> empty() {
        return empty(Comparator.naturalOrder());
    }
    public static <E> M3PSortedSet<E> empty(Comparator<? super E> comparator) {
        return new M3PSortedSet<>(M3PVector.empty(), comparator, false);
    }
    public static <E extends Comparable<? super E>> M3PSortedSet<E> singleton(E value) {
        return M3PSortedSet.<E>empty().plus(value);
    }
    public static <E> M3PSortedSet<E> singleton(Comparator<? super E> comparator, E value) {
        return M3PSortedSet.<E>empty(comparator).plus(value);
    }
    public static <E extends Comparable<? super E>> M3PSortedSet<E> from(Collection<? extends E> source) {
        return M3PSortedSet.<E>empty().plusAll(source);
    }
    public static <E> M3PSortedSet<E> from(Comparator<? super E> comparator,
            Collection<? extends E> source) {
        return M3PSortedSet.<E>empty(comparator).plusAll(source);
    }

    @Override public int size() { return elements.size(); }
    @Override public Comparator<? super E> comparator() { return descending ? order.reversed() : order; }

    /** First physical coordinate whose element is >= key, or > key when strict. */
    int bound(E key, boolean strict) {
        // Validate even an empty owner: comparator/null policy is independent of size.
        order.compare(key, key);
        int low = 0;
        int high = size();
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            int compared = order.compare(elements.get(middle), key);
            if (compared < 0 || (strict && compared == 0)) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    @SuppressWarnings("unchecked")
    int position(Object key) {
        E candidate = (E) key;
        int index = bound(candidate, false);
        return index < size() && order.compare(elements.get(index), candidate) == 0 ? index : -1;
    }

    @Override public boolean contains(Object value) { return position(value) >= 0; }
    E logical(int index) { return elements.get(descending ? size() - 1 - index : index); }

    @Override public M3PSortedSet<E> plus(E value) {
        int index = bound(value, false);
        if (index < size() && order.compare(elements.get(index), value) == 0) return this;
        return inserted(index, value);
    }
    M3PSortedSet<E> inserted(int index, E value) {
        return new M3PSortedSet<>(elements.plus(index, value), order, descending);
    }
    M3PSortedSet<E> removed(int index) {
        return new M3PSortedSet<>(elements.minus(index), order, descending);
    }
    @Override public M3PSortedSet<E> plusAll(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        M3PSortedSet<E> result = this;
        for (E value : source) result = result.plus(value);
        return result;
    }
    @Override public M3PSortedSet<E> minus(Object value) {
        int index = position(value);
        return index < 0 ? this : removed(index);
    }
    @Override public M3PSortedSet<E> minusAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        M3PSortedSet<E> result = this;
        for (Object value : source) result = result.minus(value);
        return result;
    }
    @Override public M3PSortedSet<E> intersect(Collection<? extends E> source) {
        return minusAll(minusAll(source));
    }
    @Override public M3PSortedSet<E> minusFirst() {
        if (isEmpty()) throw new NoSuchElementException();
        return removed(descending ? size() - 1 : 0);
    }
    @Override public M3PSortedSet<E> minusLast() {
        if (isEmpty()) throw new NoSuchElementException();
        return removed(descending ? 0 : size() - 1);
    }
    @Override public E first() {
        if (isEmpty()) throw new NoSuchElementException();
        return logical(0);
    }
    @Override public E last() {
        if (isEmpty()) throw new NoSuchElementException();
        return logical(size() - 1);
    }

    // Mode order: lower, floor, ceiling, higher in this view's comparator.
    int neighbor(E key, int mode) {
        if (!descending) return switch (mode) {
            case 0 -> bound(key, false) - 1;
            case 1 -> bound(key, true) - 1;
            case 2 -> bound(key, false);
            case 3 -> bound(key, true);
            default -> throw new AssertionError(mode);
        };
        return switch (mode) {
            case 0 -> bound(key, true);
            case 1 -> bound(key, false);
            case 2 -> bound(key, true) - 1;
            case 3 -> bound(key, false) - 1;
            default -> throw new AssertionError(mode);
        };
    }
    private E neighborValue(E key, int mode) {
        int index = neighbor(key, mode);
        return index < 0 || index >= size() ? null : elements.get(index);
    }
    @Override public E lower(E key) { return neighborValue(key, 0); }
    @Override public E floor(E key) { return neighborValue(key, 1); }
    @Override public E ceiling(E key) { return neighborValue(key, 2); }
    @Override public E higher(E key) { return neighborValue(key, 3); }

    M3PSortedSet<E> slice(int from, int to) {
        int end = Math.max(from, to);
        if (from == 0 && end == size()) return this;
        return new M3PSortedSet<>(elements.subList(from, end), order, descending);
    }
    int headBoundary(E key, boolean inclusive) { return bound(key, descending ? !inclusive : inclusive); }
    int tailBoundary(E key, boolean inclusive) { return bound(key, descending ? inclusive : !inclusive); }

    @Override public M3PSortedSet<E> headSet(E key, boolean inclusive) {
        int boundary = headBoundary(key, inclusive);
        return descending ? slice(boundary, size()) : slice(0, boundary);
    }
    @Override public M3PSortedSet<E> tailSet(E key, boolean inclusive) {
        int boundary = tailBoundary(key, inclusive);
        return descending ? slice(0, boundary) : slice(boundary, size());
    }
    @Override public M3PSortedSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive) {
        if (comparator().compare(from, to) > 0) throw new IllegalArgumentException("reversed endpoints");
        int start = descending ? bound(to, !toInclusive) : bound(from, !fromInclusive);
        int end = descending ? bound(from, fromInclusive) : bound(to, toInclusive);
        return slice(start, end);
    }
    @Override public M3PSortedSet<E> headSet(E key) { return headSet(key, false); }
    @Override public M3PSortedSet<E> tailSet(E key) { return tailSet(key, true); }
    @Override public M3PSortedSet<E> subSet(E from, E to) { return subSet(from, true, to, false); }
    @Override public M3PSortedSet<E> descendingSet() {
        return new M3PSortedSet<>(elements, order, !descending);
    }
    @Override public M3PSortedSet<E> reversed() { return descendingSet(); }
    @Override public Iterator<E> iterator() {
        return new Iterator<>() {
            private int next;
            @Override public boolean hasNext() { return next < size(); }
            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                return logical(next++);
            }
        };
    }
    @Override public Iterator<E> descendingIterator() { return descendingSet().iterator(); }

    @Deprecated @Override public void addFirst(E value) { throw new UnsupportedOperationException(); }
    @Deprecated @Override public void addLast(E value) { throw new UnsupportedOperationException(); }
    @Deprecated @Override public E removeFirst() { throw new UnsupportedOperationException(); }
    @Deprecated @Override public E removeLast() { throw new UnsupportedOperationException(); }
}
