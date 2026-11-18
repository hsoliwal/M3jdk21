/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Copy-on-write object list with immutable epoch arrays and snapshot cursors.
 *
 * <p>Each successful structural write publishes one freshly sized parent-owned
 * Object[] lane. Readers capture its volatile reference once; concurrent
 * mutations cannot modify the captured epoch. The only retained objects are
 * user elements and the array itself, not one wrapper per list slot. An
 * auxiliary packed bitset records frozen live positions, 64 per word.</p>
 *
 * <p>Reads and snapshot-iterator creation are O(1); equality search is O(n);
 * writes copy O(n) references by design. A public snapshot returns a detached
 * array, never the immutable internal lane. The class is an M3-specific
 * subset of JDK CopyOnWriteArrayList operations, not its full interface.</p>
 */
public final class CowList<E> {
    static final long STATE_FROZEN_EPOCH = 1L << 0;
    private static final Object[] EMPTY = new Object[0];

    /** Current immutable, exact-length epoch. Published with volatile release. */
    private volatile Object[] valueLane = EMPTY;
    /** Frozen positions in the current epoch; one bit per element. */
    private volatile long[] stateMaskWords = new long[0];
    private volatile long epoch;
    /** Private monitor; public method reflection flags remain unchanged. */
    private final Object writeLock = new Object();

    public CowList() { }

    /** Historic inventory descriptor is retained byte-for-byte. */
    public static String scaffoldDescriptor() {
        return "M3CowList|COW_LIST|OBJECT|FROZEN_EPOCH|Object[] valueLane";
    }

    /** A lock-free snapshot read, including Java's array bounds exceptions. */
    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) valueLane[index];
    }

    /** Copy once and publish the appended element atomically. */
    public boolean add(E element) {
        synchronized (writeLock) {
            Object[] previous = valueLane;
            Object[] next = Arrays.copyOf(previous, previous.length + 1);
            next[previous.length] = element;
            publish(next);
            return true;
        }
    }

    /** Copy once and insert at the requested logical position, including size. */
    public void add(int index, E element) {
        synchronized (writeLock) {
            Object[] previous = valueLane;
            int size = previous.length;
            checkPosition(index, size);
            Object[] next = new Object[size + 1];
            System.arraycopy(previous, 0, next, 0, index);
            next[index] = element;
            System.arraycopy(previous, index, next, index + 1, size - index);
            publish(next);
        }
    }

    /** Replace one element; same-reference replacement needs no array copy. */
    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        synchronized (writeLock) {
            Object[] previous = valueLane;
            E old = (E) previous[index];
            if (old != element) {
                Object[] next = previous.clone();
                next[index] = element;
                publish(next);
            } else {
                // Preserve release publication even for an equal-reference write.
                valueLane = previous;
            }
            return old;
        }
    }

    /** Remove a ranked occurrence and immediately drop its reference. */
    @SuppressWarnings("unchecked")
    public E remove(int index) {
        synchronized (writeLock) {
            Object[] previous = valueLane;
            E old = (E) previous[index];
            Object[] next = new Object[previous.length - 1];
            System.arraycopy(previous, 0, next, 0, index);
            System.arraycopy(previous, index + 1, next, index,
                    previous.length - index - 1);
            publish(next);
            return old;
        }
    }

    /**
     * Search outside the mutation lock. If user-defined equals reenters the
     * owner or another thread writes, replay the search on the new epoch
     * before publishing; never overwrite a concurrent structural update.
     */
    public boolean remove(Object element) {
        while (true) {
            Object[] observed = valueLane;
            int rank = firstIndexOf(element, observed);
            synchronized (writeLock) {
                if (observed != valueLane) continue;
                if (rank < 0) return false;
                Object[] next = new Object[observed.length - 1];
                System.arraycopy(observed, 0, next, 0, rank);
                System.arraycopy(observed, rank + 1, next, rank,
                        observed.length - rank - 1);
                publish(next);
                return true;
            }
        }
    }

    /** Append only if no equal element exists in the current stable epoch. */
    public boolean addIfAbsent(E element) {
        while (true) {
            Object[] observed = valueLane;
            boolean exists = firstIndexOf(element, observed) >= 0;
            synchronized (writeLock) {
                if (observed != valueLane) continue;
                if (exists) return false;
                Object[] next = Arrays.copyOf(observed, observed.length + 1);
                next[observed.length] = element;
                publish(next);
                return true;
            }
        }
    }

    /** A stable, equality-based search of exactly one observed epoch. */
    public int indexOf(Object element) {
        return firstIndexOf(element, valueLane);
    }

    public boolean contains(Object element) {
        return indexOf(element) >= 0;
    }

    /** Detached, mutable-to-caller compatibility copy of the current lane. */
    public Object[] snapshot() {
        return valueLane.clone();
    }

    /** Snapshot iterator; later writes cannot change its observed elements. */
    public Iterator<E> iterator() {
        final Object[] observed = valueLane;
        return new Iterator<>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < observed.length;
            }

            @SuppressWarnings("unchecked")
            @Override
            public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                return (E) observed[cursor++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public int size() {
        return valueLane.length;
    }

    public boolean isEmpty() {
        return valueLane.length == 0;
    }

    /** Release references by publishing the canonical empty epoch. */
    public void clear() {
        synchronized (writeLock) {
            if (valueLane.length == 0) return;
            publish(EMPTY);
        }
    }

    private void publish(Object[] next) {
        int count = next.length;
        int words = (int) (((long) count + 63L) >>> 6);
        long[] frozen = new long[words];
        Arrays.fill(frozen, -1L);
        if (words != 0 && (count & 63) != 0)
            frozen[words - 1] = (1L << (count & 63)) - 1L;
        stateMaskWords = frozen;
        epoch++;
        valueLane = next;
    }

    private static int firstIndexOf(Object target, Object[] elements) {
        for (int i = 0; i < elements.length; i++)
            if (Objects.equals(target, elements[i])) return i;
        return -1;
    }

    private static void checkPosition(int index, int size) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
    }
}
