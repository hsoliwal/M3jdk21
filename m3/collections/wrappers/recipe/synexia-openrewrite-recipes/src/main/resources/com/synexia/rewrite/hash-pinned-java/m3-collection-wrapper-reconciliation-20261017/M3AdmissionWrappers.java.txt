/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.RandomAccess;
import java.util.Set;

/** Purpose-owned wrapper atoms. Invoked through {@link M3CollectionWrappers}. */
final class M3AdmissionWrappers {
    private M3AdmissionWrappers() {
    }


    static <E> Collection<E> collection(Collection<E> source, Predicate<? super E> predicate) {
        return new AdmissionCollection<>(source, predicate);
    }

    static <E> List<E> list(List<E> source, Predicate<? super E> predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        return source instanceof RandomAccess
                ? new AdmissionRandomAccessList<>(source, predicate)
                : new AdmissionList<>(source, predicate);
    }

    static <E> Set<E> set(Set<E> source, Predicate<? super E> predicate) {
        return new AdmissionSet<>(source, predicate);
    }

    private static <E> ArrayList<E> validatedElements(
            Collection<? extends E> elements, Predicate<? super E> admission) {
        Objects.requireNonNull(elements, "elements");
        ArrayList<E> validated = new ArrayList<>(elements.size());
        for (E element : elements) {
            requireAdmitted(admission, element);
            validated.add(element);
        }
        return validated;
    }

    private static <E> void requireAdmitted(Predicate<? super E> predicate, E element) {
        if (!predicate.test(element)) {
            throw new IllegalArgumentException("element rejected by admission predicate");
        }
    }

    private static class AdmissionCollection<E> extends AbstractCollection<E> {
        private final Collection<E> source;
        private final Predicate<? super E> admission;

        private AdmissionCollection(Collection<E> source, Predicate<? super E> admission) {
            this.source = Objects.requireNonNull(source, "source");
            this.admission = Objects.requireNonNull(admission, "admission");
        }

        @Override
        public Iterator<E> iterator() {
            return source.iterator();
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public boolean contains(Object element) {
            return source.contains(element);
        }

        @Override
        public boolean add(E element) {
            requireAdmitted(admission, element);
            return source.add(element);
        }

        @Override
        public boolean addAll(Collection<? extends E> elements) {
            return source.addAll(validatedElements(elements, admission));
        }
    }

    private static final class AdmissionSet<E> extends AbstractSet<E> {
        private final AdmissionCollection<E> delegate;

        private AdmissionSet(Set<E> source, Predicate<? super E> predicate) {
            this.delegate = new AdmissionCollection<>(source, predicate);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean contains(Object element) {
            return delegate.contains(element);
        }

        @Override
        public boolean add(E element) {
            return delegate.add(element);
        }

        @Override
        public boolean addAll(Collection<? extends E> elements) {
            return delegate.addAll(elements);
        }
    }

    private static class AdmissionList<E> extends AbstractList<E> {
        private final List<E> source;
        private final Predicate<? super E> admission;

        private AdmissionList(List<E> source, Predicate<? super E> admission) {
            this.source = Objects.requireNonNull(source, "source");
            this.admission = Objects.requireNonNull(admission, "admission");
        }

        @Override
        public E get(int index) {
            return source.get(index);
        }

        @Override
        public int size() {
            return source.size();
        }

        @Override
        public E set(int index, E element) {
            requireAdmitted(admission, element);
            return source.set(index, element);
        }

        @Override
        public void add(int index, E element) {
            requireAdmitted(admission, element);
            source.add(index, element);
            modCount++;
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> elements) {
            ArrayList<E> validated = validatedElements(elements, admission);
            boolean changed = source.addAll(index, validated);
            if (changed) {
                modCount++;
            }
            return changed;
        }

        @Override
        public boolean addAll(Collection<? extends E> elements) {
            return addAll(size(), elements);
        }

        @Override
        public E remove(int index) {
            E removed = source.remove(index);
            modCount++;
            return removed;
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            ListIterator<E> iterator = source.listIterator(index);
            return new ListIterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public E next() {
                    return iterator.next();
                }

                @Override
                public boolean hasPrevious() {
                    return iterator.hasPrevious();
                }

                @Override
                public E previous() {
                    return iterator.previous();
                }

                @Override
                public int nextIndex() {
                    return iterator.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return iterator.previousIndex();
                }

                @Override
                public void remove() {
                    iterator.remove();
                    modCount++;
                }

                @Override
                public void set(E value) {
                    requireAdmitted(admission, value);
                    iterator.set(value);
                }

                @Override
                public void add(E value) {
                    requireAdmitted(admission, value);
                    iterator.add(value);
                    modCount++;
                }
            };
        }
    }

    private static final class AdmissionRandomAccessList<E> extends AdmissionList<E>
            implements RandomAccess {
        private AdmissionRandomAccessList(List<E> source, Predicate<? super E> admission) {
            super(source, admission);
        }
    }

}
