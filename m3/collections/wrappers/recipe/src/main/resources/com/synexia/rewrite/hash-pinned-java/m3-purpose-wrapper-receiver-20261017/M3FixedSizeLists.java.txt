/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

/** Purpose-owned wrapper atoms. Invoked through {@link M3CollectionWrappers}. */
final class M3FixedSizeLists {
    private M3FixedSizeLists() {
    }


    static <E> List<E> of(List<E> source) {
        Objects.requireNonNull(source, "source");
        return source instanceof RandomAccess
                ? new FixedSizeRandomAccessList<>(source) : new FixedSizeList<>(source);
    }

    private static class FixedSizeList<E> extends AbstractList<E> {
        private final List<E> source;

        private FixedSizeList(List<E> source) {
            this.source = source;
        }

        @Override
        public E get(int index) {
            return source.get(index);
        }

        @Override
        public E set(int index, E element) {
            return source.set(index, element);
        }

        @Override
        public int size() {
            return source.size();
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
                    throw new UnsupportedOperationException("fixed-size list");
                }

                @Override
                public void set(E value) {
                    iterator.set(value);
                }

                @Override
                public void add(E value) {
                    throw new UnsupportedOperationException("fixed-size list");
                }
            };
        }
    }

    private static final class FixedSizeRandomAccessList<E> extends FixedSizeList<E>
            implements RandomAccess {
        private FixedSizeRandomAccessList(List<E> source) {
            super(source);
        }
    }

}
