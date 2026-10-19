/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package jdk.internal.m3;

import java.util.AbstractList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Predicate;

/**
 * A read-only, live view of an M3 array. Filtering and stable sorting use a
 * lazily built {@code int[]} of slot indexes, not entry objects or copies of
 * payloads. Iterator snapshots fail fast when the backing store changes.
 */
final class M3IndexedView<E> extends AbstractList<E> implements RandomAccess {
    private final M3IndexedArray<E> backing;
    private final Predicate<? super E> predicate;
    private final Comparator<? super E> comparator;
    private int[] indexes = new int[0];
    private long indexedVersion = Long.MIN_VALUE;

    M3IndexedView(M3IndexedArray<E> backing, Predicate<? super E> predicate,
                  Comparator<? super E> comparator) {
        this.backing = Objects.requireNonNull(backing);
        this.predicate = predicate;
        this.comparator = comparator;
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
            return backing.viewValue(indexedVersion, indexes[index]);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        final long expected;
        synchronized (backing) {
            refresh();
            Objects.checkFromToIndex(index, index, indexes.length);
            expected = indexedVersion;
        }
        return new ListIterator<>() {
            private int cursor = index;

            private void check() {
                if (backing.version() != expected) {
                    throw new ConcurrentModificationException("M3 view backing changed");
                }
            }

            @Override
            public boolean hasNext() {
                synchronized (backing) {
                    check();
                    return cursor < indexes.length;
                }
            }

            @Override
            public E next() {
                synchronized (backing) {
                    check();
                    if (cursor >= indexes.length) {
                        throw new NoSuchElementException();
                    }
                    return backing.viewValue(expected, indexes[cursor++]);
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
                    return backing.viewValue(expected, indexes[--cursor]);
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

    private void refresh() {
        final long current = backing.version();
        if (indexedVersion == current) {
            return;
        }
        final int[] selected = backing.select(predicate, comparator);
        indexes = selected;
        indexedVersion = backing.version();
        modCount++;
    }
}
