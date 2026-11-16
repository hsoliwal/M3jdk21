/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Live concatenation of collection owners, with only one parent-owned
 * {@code Object[]} of backing references. No per-entry Node, Cell or Entry
 * objects, copied element arrays, or permanent iterator chains are retained.
 * The view is not synchronized; nested collections own their own mutations.
 */
public final class CompositeCollectionView<E> implements Iterable<E> {
    static final long STATE_SELECTED = 1L << 0;
    private Object[] backingLane = new Object[0];
    private int backingCount;

    public CompositeCollectionView() { }

    public static String scaffoldDescriptor() {
        return "M3CompositeCollectionView|COMPOSITE_COLLECTION|OBJECT|SELECTED|Object[] backingLane";
    }

    /** Null backings are ignored; repeated backing references remain distinct. */
    public void addComposited(Collection<E> collection) {
        if (collection == null) return;
        if (backingCount == backingLane.length) {
            long next = backingCount == 0 ? 4L : (long) backingCount + (backingCount >>> 1) + 1;
            backingLane = Arrays.copyOf(backingLane, (int) Math.min(next, Integer.MAX_VALUE));
        }
        backingLane[backingCount++] = collection;
    }

    /** Removes the first collection that compares equal, as List.remove does. */
    public void removeComposited(Collection<E> collection) {
        for (int i = 0; i < backingCount; i++) {
            Object current = backingLane[i];
            if (collection == null ? current == null : collection.equals(current)) {
                int shift = backingCount - i - 1;
                if (shift > 0) {
                    System.arraycopy(backingLane, i + 1, backingLane, i, shift);
                }
                backingLane[--backingCount] = null;
                return;
            }
        }
    }

    /** Matches Commons CompositeCollection's default lack of a mutator. */
    public boolean add(E element) {
        throw new UnsupportedOperationException(
                "add() is not supported on CompositeCollection without a CollectionMutator strategy");
    }

    /** Removal needs a configured composite mutator and is rejected by default. */
    public boolean remove(Object element) {
        throw new UnsupportedOperationException(
                "remove() is not supported on CompositeCollection without a CollectionMutator strategy");
    }

    /** Each child retains its own membership semantics and callback order. */
    @SuppressWarnings("unchecked")
    public boolean contains(Object element) {
        for (int i = 0; i < backingCount; i++) {
            if (((Collection<E>) backingLane[i]).contains(element)) return true;
        }
        return false;
    }

    /** Capture iterator references only while traversal is in progress. */
    @SuppressWarnings("unchecked")
    @Override public Iterator<E> iterator() {
        return new Iterator<>() {
            private int backingIndex;
            private Iterator<E> current = Collections.emptyIterator();
            private Iterator<E> lastUsed;
            private boolean canRemove;

            @Override public boolean hasNext() {
                while (!current.hasNext() && backingIndex < backingCount) {
                    current = ((Collection<E>) backingLane[backingIndex++]).iterator();
                }
                return current.hasNext();
            }

            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                E next = current.next();
                lastUsed = current;
                canRemove = true;
                return next;
            }

            @Override public void remove() {
                if (!canRemove) throw new IllegalStateException();
                lastUsed.remove();
                canRemove = false;
            }
        };
    }

    /** Recompute live cardinality; adding/removing from a child is observable. */
    @SuppressWarnings("unchecked")
    public int size() {
        long count = 0;
        for (int i = 0; i < backingCount; i++) {
            count += ((Collection<E>) backingLane[i]).size();
            if (count >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) count;
    }

    @SuppressWarnings("unchecked")
    public boolean isEmpty() {
        for (int i = 0; i < backingCount; i++) {
            if (!((Collection<E>) backingLane[i]).isEmpty()) return false;
        }
        return true;
    }
}
