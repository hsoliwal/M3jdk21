/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

/** Purpose-owned wrapper atoms. Invoked through {@link M3CollectionWrappers}. */
final class M3BoundedLists {
    private M3BoundedLists() {
    }


    static <E> List<E> of(List<E> source, int maximumSize) {
        Objects.requireNonNull(source, "source");
        if (maximumSize < 0 || source.size() > maximumSize) {
            throw new IllegalArgumentException("invalid maximum size");
        }
        return source instanceof RandomAccess
                ? new BoundedRandomAccessList<>(source, maximumSize)
                : new BoundedList<>(source, maximumSize);
    }

    private static class BoundedList<E> extends AbstractList<E> {
        protected final List<E> source;
        private final int maximumSize;

        private BoundedList(List<E> source, int maximumSize) {
            this.source = source;
            this.maximumSize = maximumSize;
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
            return source.set(index, element);
        }

        @Override
        public void add(int index, E element) {
            validatePosition(index);
            requireCapacity(1);
            source.add(index, element);
            modCount++;
        }

        @Override
        public E remove(int index) {
            E old = source.remove(index);
            modCount++;
            return old;
        }

        @Override
        public boolean addAll(Collection<? extends E> elements) {
            return addAll(size(), elements);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> elements) {
            validatePosition(index);
            Collection<? extends E> input = Objects.requireNonNull(elements, "elements");
            requireCapacity(input.size());
            ArrayList<E> copy = new ArrayList<>(input);
            requireCapacity(copy.size());
            boolean changed = source.addAll(index, copy);
            if (changed) {
                modCount++;
            }
            return changed;
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
                public void set(E element) {
                    iterator.set(element);
                }

                @Override
                public void add(E element) {
                    requireCapacity(1);
                    iterator.add(element);
                    modCount++;
                }
            };
        }

        private void validatePosition(int index) {
            if (index < 0 || index > source.size()) {
                throw new IndexOutOfBoundsException(index);
            }
        }

        private void requireCapacity(int incoming) {
            if ((long) source.size() + incoming > maximumSize) {
                throw new IllegalStateException("bounded list capacity exceeded");
            }
        }
    }

    private static final class BoundedRandomAccessList<E> extends BoundedList<E>
            implements RandomAccess {
        private BoundedRandomAccessList(List<E> source, int maximumSize) {
            super(source, maximumSize);
        }
    }

}
