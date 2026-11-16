/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Immutable encounter-ordered list over a private dense object lane.
 *
 * <p>Admission copies the source structure, rejects null elements, and preserves
 * duplicates and element identities. Null queries find no element. Array exports
 * are detached copies; list and iterator facades share this immutable owner.
 * Mutators always throw, including requests that would make no change.
 *
 * <p>This implements the selected List.copyOf and Guava ImmutableList operation
 * contracts of the existing internal M3 owner. Null queries follow Guava's absence
 * semantics. It is not a replacement for a public JDK or Guava class. The original
 * JEP269/ImmutableList lineage and compatibility descriptor remain authoritative
 * history; no donor implementation source is copied.
 */
public final class FrozenList<E> {
    static final long STATE_FROZEN = 1L << 0;
    private static final Object[] EMPTY = new Object[0];

    private final Object[] valueLane;
    private final long[] stateMaskWords;
    private final int size;

    /** Creates a valid immutable empty owner. */
    public FrozenList() { this(EMPTY); }

    private FrozenList(Object[] values) {
        valueLane = values;
        size = values.length;
        int tail = size & 63;
        stateMaskWords = new long[(size >>> 6) + (tail == 0 ? 0 : 1)];
        Arrays.fill(stateMaskWords, -1L);
        if (tail != 0) stateMaskWords[stateMaskWords.length - 1] = (1L << tail) - 1;
    }

    /** Retained compatibility descriptor; it does not describe admission status. */
    public static String scaffoldDescriptor() {
        return "M3FrozenList|FROZEN_LIST|OBJECT|FROZEN|Object[] valueLane";
    }

    /** Copies the actual traversal once; no owner is published on admission failure. */
    public static <E> FrozenList<E> copyOf(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        ArrayList<Object> values = new ArrayList<>();
        for (E element : source) values.add(Objects.requireNonNull(element, "element"));
        return new FrozenList<>(values.toArray());
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) valueLane[Objects.checkIndex(index, size)];
    }

    public int indexOf(Object element) {
        if (element == null) return -1;
        for (int index = 0; index < size; index++) {
            if (element.equals(valueLane[index])) return index;
        }
        return -1;
    }

    public boolean contains(Object element) { return indexOf(element) >= 0; }
    public Object[] toArray() { return valueLane.clone(); }

    /** The facade owns no element storage and cannot mutate this snapshot. */
    public List<E> asList() { return new ListView(0, size, false, null); }

    private final class ListView extends AbstractList<E> implements RandomAccess {
        private final int from;
        private final int to;
        private final boolean reverse;
        private final ListView reverseOf;

        ListView(int from, int to, boolean reverse, ListView reverseOf) {
            this.from = from;
            this.to = to;
            this.reverse = reverse;
            this.reverseOf = reverseOf;
        }

        @Override public int size() { return to - from; }
        @Override public E get(int index) {
            Objects.checkIndex(index, size());
            return FrozenList.this.get(reverse ? to - 1 - index : from + index);
        }
        @Override public int indexOf(Object element) {
            if (element == null) return -1;
            for (int index = 0; index < size(); index++) {
                if (element.equals(get(index))) return index;
            }
            return -1;
        }
        @Override public boolean contains(Object element) { return indexOf(element) >= 0; }
        @Override public Object[] toArray() {
            Object[] values = new Object[size()];
            for (int index = 0; index < values.length; index++) values[index] = get(index);
            return values;
        }
        @Override public List<E> subList(int fromIndex, int toIndex) {
            if (fromIndex < 0 || toIndex > size()) throw new IndexOutOfBoundsException();
            if (fromIndex > toIndex) throw new IllegalArgumentException("fromIndex > toIndex");
            return reverse
                    ? new ListView(to - toIndex, to - fromIndex, true, null)
                    : new ListView(from + fromIndex, from + toIndex, false, null);
        }
        @Override public List<E> reversed() {
            return reverseOf != null ? reverseOf : new ListView(from, to, !reverse, this);
        }
        @Override public Iterator<E> iterator() { return listIterator(0); }
        @Override public ListIterator<E> listIterator(int index) {
            if (index < 0 || index > size()) throw new IndexOutOfBoundsException(index);
            return new ListIterator<>() {
                private int cursor = index;
                @Override public boolean hasNext() { return cursor < size(); }
                @Override public E next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return get(cursor++);
                }
                @Override public boolean hasPrevious() { return cursor > 0; }
                @Override public E previous() {
                    if (!hasPrevious()) throw new NoSuchElementException();
                    return get(--cursor);
                }
                @Override public int nextIndex() { return cursor; }
                @Override public int previousIndex() { return cursor - 1; }
                @Override public void remove() { throw immutable(); }
                @Override public void set(E value) { throw immutable(); }
                @Override public void add(E value) { throw immutable(); }
            };
        }
        @Override public Spliterator<E> spliterator() {
            return Spliterators.spliterator(iterator(), size(),
                    Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL);
        }

        // Guard mutation at the view itself: JDK21's default reversed view can
        // return without calling its unmodifiable base for an empty bulk edit.
        @Override public boolean add(E element) { throw immutable(); }
        @Override public void add(int index, E element) { throw immutable(); }
        @Override public boolean addAll(Collection<? extends E> values) { throw immutable(); }
        @Override public boolean addAll(int index, Collection<? extends E> values) { throw immutable(); }
        @Override public E set(int index, E element) { throw immutable(); }
        @Override public E remove(int index) { throw immutable(); }
        @Override public boolean remove(Object value) { throw immutable(); }
        @Override public boolean removeAll(Collection<?> values) { throw immutable(); }
        @Override public boolean retainAll(Collection<?> values) { throw immutable(); }
        @Override public boolean removeIf(Predicate<? super E> filter) { throw immutable(); }
        @Override public void replaceAll(UnaryOperator<E> operator) { throw immutable(); }
        @Override public void sort(Comparator<? super E> comparator) { throw immutable(); }
        @Override public void clear() { throw immutable(); }
        @Override public void addFirst(E element) { throw immutable(); }
        @Override public void addLast(E element) { throw immutable(); }
        @Override public E removeFirst() { throw immutable(); }
        @Override public E removeLast() { throw immutable(); }
    }

    public Iterator<E> iterator() { return asList().iterator(); }

    private static UnsupportedOperationException immutable() {
        return new UnsupportedOperationException("immutable list");
    }

    public boolean add(E element) {
        throw immutable();
    }

    public E set(int index, E element) {
        throw immutable();
    }
}
