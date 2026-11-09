/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

/**
 * M3 five-library superset {@code CowSet}: API group {@code COW_SET} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-m3-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>JDK CopyOnWriteArrayList/Set: snapshot array per write; iterators never fail [license GPLv2+CPE; NEW_OWNER (adjacent only: m3/filtered-cow-bulk-receiver-20261017 = filtered COW bulk ops, not the list owner); priority P2; matrix owner PROPOSED:M3CowLongArrayList]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind OBJECT; lanes {@code Object[] valueLane}; offset scheme immutable lane swapped per write; linear uniqueness; state-mask bits FROZEN_EPOCH; stored nodes NONE; API entry objects NONE (transient facades over slots, never the stored representation); matrix note: immutable value lane swapped per write; epoch offset / FROZEN per epoch; epoch counter.
 *
 * <p>One canonical volatile array owner is published after each successful mutation. Readers
 * observe complete immutable snapshots; writers serialize on this owner. Neither JDK
 * {@code CopyOnWriteArraySet} nor a second retained per-item node store is required.
 * This is an M3-specific API, not a {@code java.util} replacement.
 */
//FLLDCIM: MRL - census-g2-20261106/apply.py v1 DONOR: postimage blob cddfc9943264 (preimage blob 77ebef975784 = this owner before, under the declared transform) at ca1aafc4d94f
public final class CowSet<E> {
    /** State-mask bit {@code FROZEN_EPOCH} (per-slot state word, bit 0). */
    static final long STATE_FROZEN_EPOCH = 1L << 0;

    /** Empty immutable epoch; no separate slot state is retained. */
    private static final Object[] EMPTY = new Object[0];
    /** Sole storage owner; published snapshots are never modified in place. */
    private volatile Object[] valueLane = EMPTY;

    /** Creates an empty copy-on-write set. */
    public CowSet() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3CowSet|COW_SET|OBJECT|FROZEN_EPOCH|Object[] valueLane";
    }

    /** CopyOnWriteArraySet.add (addIfAbsent). Linear equality and one array copy on success. */
    public boolean add(E element) {
        synchronized (this) {
            Object[] current = valueLane;
            if (indexOf(element, current) >= 0) return false;
            Object[] next = java.util.Arrays.copyOf(current, current.length + 1);
            next[current.length] = element;
            valueLane = next;
            return true;
        }
    }

    /** CopyOnWriteArraySet.remove, stable two-slice compaction with no per-slot nodes. */
    public boolean remove(Object element) {
        synchronized (this) {
            Object[] current = valueLane;
            int index = indexOf(element, current);
            if (index < 0) return false;
            Object[] next = new Object[current.length - 1];
            System.arraycopy(current, 0, next, 0, index);
            System.arraycopy(current, index + 1, next, index, current.length - index - 1);
            valueLane = next;
            return true;
        }
    }

    /** Linear membership lookup over one published snapshot. */
    public boolean contains(Object element) {
        return indexOf(element, valueLane) >= 0;
    }

    /** The iterator keeps its original immutable epoch even after concurrent writers. */
    public java.util.Iterator<E> iterator() {
        final Object[] snapshot = valueLane;
        return new java.util.Iterator<>() {
            private int cursor;

            @Override public boolean hasNext() { return cursor < snapshot.length; }

            @Override @SuppressWarnings("unchecked")
            public E next() {
                if (cursor >= snapshot.length) throw new java.util.NoSuchElementException();
                return (E) snapshot[cursor++];
            }
        };
    }

    /** Number of elements in the current published epoch. */
    public int size() { return valueLane.length; }

    /** True if the current published epoch is empty. */
    public boolean isEmpty() { return valueLane.length == 0; }

    /** Publish the shared empty snapshot; old iterators retain their prior epoch. */
    public void clear() { synchronized (this) { valueLane = EMPTY; } }

    /** Preserve CopyOnWriteArraySet's null-aware, query-directed equality order. */
    private static int indexOf(Object query, Object[] snapshot) {
        for (int index = 0; index < snapshot.length; index++) {
            if (query == snapshot[index]
                    || (query != null && query.equals(snapshot[index]))) return index;
        }
        return -1;
    }
}
