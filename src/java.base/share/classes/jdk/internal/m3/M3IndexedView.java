/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * Modified 2026 by Hitesh Soliwal and contributors: bind range and split
 * cursors to the backing version after lazy index selection.
 */
package jdk.internal.m3;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A read-only, live view of an M3 array. Filtering and stable sorting use a
 * lazily built {@code int[]} of slot indexes, not entry objects or copies of
 * payloads. Iterators, bound spliterators and sublists fail fast when the
 * backing store changes. Spliterators bind lazily; sublists retain their
 * index window and backing version without copying or pinning an old index
 * array after the root view refreshes.
 */
final class M3IndexedView<E> extends AbstractList<E> implements RandomAccess {
    private final M3IndexedArray<E> backing;
    private final Predicate<? super E> predicate;
    private final Comparator<? super E> comparator;
    private final int requiredMask;
    private final int forbiddenMask;
    private final boolean stateAware;
    private int[] indexes = new int[0];
    private long indexedVersion = Long.MIN_VALUE;

    M3IndexedView(M3IndexedArray<E> backing, Predicate<? super E> predicate,
                  Comparator<? super E> comparator) {
        this(backing, predicate, comparator, 0, 0, false);
    }

    M3IndexedView(M3IndexedArray<E> backing, Predicate<? super E> predicate,
                  Comparator<? super E> comparator, int requiredMask,
                  int forbiddenMask, boolean stateAware) {
        this.backing = Objects.requireNonNull(backing);
        this.predicate = predicate;
        this.comparator = comparator;
        this.requiredMask = requiredMask;
        this.forbiddenMask = forbiddenMask;
        this.stateAware = stateAware;
    }

    @Override
    public int size() {
        synchronized (backing) {
            refresh();
            return indexes.length;
        }
    }

    @Override
    public E get(int index) {
        synchronized (backing) {
            refresh();
            Objects.checkIndex(index, indexes.length);
            return viewValue(indexedVersion, indexes[index]);
        }
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        synchronized (backing) {
            refresh();
            return range(indexedVersion, 0, indexes.length, fromIndex, toIndex);
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new IndexSpliterator();
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        synchronized (backing) {
            refresh();
            Objects.checkFromToIndex(index, index, indexes.length);
            return iteratorFor(indexedVersion, 0, indexes.length, index);
        }
    }

    private ListIterator<E> iteratorFor(long expected, int origin, int count, int index) {
        return new ListIterator<>() {
            private int cursor = index;

            private void check() {
                checkVersion(expected);
            }

            @Override
            public boolean hasNext() {
                synchronized (backing) {
                    check();
                    return cursor < count;
                }
            }

            @Override
            public E next() {
                synchronized (backing) {
                    check();
                    if (cursor >= count) {
                        throw new NoSuchElementException();
                    }
                    return viewValue(expected, indexes[origin + cursor++]);
                }
            }

            @Override
            public boolean hasPrevious() {
                synchronized (backing) {
                    check();
                    return cursor > 0;
                }
            }

            @Override
            public E previous() {
                synchronized (backing) {
                    check();
                    if (cursor == 0) {
                        throw new NoSuchElementException();
                    }
                    return viewValue(expected, indexes[origin + --cursor]);
                }
            }

            @Override
            public int nextIndex() {
                synchronized (backing) {
                    check();
                    return cursor;
                }
            }

            @Override
            public int previousIndex() {
                synchronized (backing) {
                    check();
                    return cursor - 1;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("read-only M3 view");
            }

            @Override
            public void set(E value) {
                throw new UnsupportedOperationException("read-only M3 view");
            }

            @Override
            public void add(E value) {
                throw new UnsupportedOperationException("read-only M3 view");
            }
        };
    }

    private List<E> range(long expected, int origin, int count, int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if (toIndex > count) {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex
                    + ") > toIndex(" + toIndex + ")");
        }
        return new IndexRange(expected, origin + fromIndex, toIndex - fromIndex);
    }

    /** One API range facade; nested ranges retain only absolute root coordinates. */
    private final class IndexRange extends AbstractList<E> implements RandomAccess {
        private final long expected;
        private final int origin;
        private final int count;

        private IndexRange(long expected, int origin, int count) {
            this.expected = expected;
            this.origin = origin;
            this.count = count;
        }

        @Override
        public int size() {
            synchronized (backing) {
                checkVersion(expected);
                return count;
            }
        }

        @Override
        public E get(int index) {
            Objects.checkIndex(index, count);
            synchronized (backing) {
                checkVersion(expected);
                return viewValue(expected, indexes[origin + index]);
            }
        }

        @Override
        public E set(int index, E element) {
            Objects.checkIndex(index, count);
            synchronized (backing) {
                checkVersion(expected);
                return M3IndexedView.this.set(origin + index, element);
            }
        }

        @Override
        public void add(int index, E element) {
            checkAddIndex(index);
            synchronized (backing) {
                checkVersion(expected);
                M3IndexedView.this.add(origin + index, element);
            }
        }

        @Override
        public E remove(int index) {
            Objects.checkIndex(index, count);
            synchronized (backing) {
                checkVersion(expected);
                return M3IndexedView.this.remove(origin + index);
            }
        }

        @Override
        public boolean addAll(Collection<? extends E> values) {
            return addAll(count, values);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> values) {
            checkAddIndex(index);
            // AbstractList.SubList consults size before the epoch and owner.
            // A reported empty batch neither acquires its iterator nor mutates.
            if (values.size() == 0) return false;
            synchronized (backing) {
                checkVersion(expected);
            }
            M3IndexedView.this.addAll(origin + index, values);
            return true;
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            synchronized (backing) {
                checkVersion(expected);
                M3IndexedView.this.removeRange(origin + fromIndex, origin + toIndex);
            }
        }

        private void checkAddIndex(int index) {
            if (index < 0 || index > count) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + count);
            }
        }

        @Override
        public Iterator<E> iterator() {
            return listIterator();
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            synchronized (backing) {
                checkVersion(expected);
                checkAddIndex(index);
                return iteratorFor(expected, origin, count, index);
            }
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            synchronized (backing) {
                checkVersion(expected);
                return range(expected, origin, count, fromIndex, toIndex);
            }
        }

        @Override
        public Spliterator<E> spliterator() {
            return new IndexSpliterator(expected, origin, origin + count);
        }
    }

    /** Split cursors reuse the root index directory and allocate no element buffer. */
    private final class IndexSpliterator implements Spliterator<E> {
        private long expected;
        private int cursor;
        private int end = -1;

        private IndexSpliterator() { }

        private IndexSpliterator(long expected, int cursor, int end) {
            this.expected = expected;
            this.cursor = cursor;
            this.end = end;
        }

        /** Called with the backing monitor held, after any lazy index rebuild. */
        private void bind() {
            if (end < 0) {
                refresh();
                expected = indexedVersion;
                end = indexes.length;
            }
            checkVersion(expected);
        }

        @Override
        public Spliterator<E> trySplit() {
            synchronized (backing) {
                bind();
                final int middle = (cursor + end) >>> 1;
                if (middle == cursor) return null;
                final Spliterator<E> prefix =
                        new IndexSpliterator(expected, cursor, middle);
                cursor = middle;
                return prefix;
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final E value;
            synchronized (backing) {
                bind();
                if (cursor == end) return false;
                value = viewValue(expected, indexes[cursor++]);
            }
            action.accept(value);
            synchronized (backing) {
                checkVersion(expected);
            }
            return true;
        }

        @Override
        public long estimateSize() {
            synchronized (backing) {
                bind();
                return end - cursor;
            }
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED
                    | (comparator != null ? Spliterator.SORTED : 0);
        }

        @Override
        public Comparator<? super E> getComparator() {
            if (comparator == null) {
                throw new IllegalStateException("M3 indexed view is not sorted");
            }
            return comparator;
        }
    }

    private long currentVersion() {
        return stateAware ? backing.stateVersion() : backing.version();
    }

    private E viewValue(long expected, int slot) {
        return backing.viewValue(expected, slot, stateAware);
    }

    private void checkVersion(long expected) {
        if (currentVersion() != expected) {
            throw new ConcurrentModificationException("M3 view backing changed");
        }
    }

    private void refresh() {
        final long current = currentVersion();
        if (indexedVersion == current) {
            return;
        }
        final int[] selected = stateAware
                ? backing.select(predicate, comparator, requiredMask, forbiddenMask, true)
                : backing.select(predicate, comparator);
        indexes = selected;
        indexedVersion = currentVersion();
        modCount++;
    }
}
