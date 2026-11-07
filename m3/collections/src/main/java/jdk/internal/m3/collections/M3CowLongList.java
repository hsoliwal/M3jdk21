/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;

/**
 * M3 five-library superset primitive COW candidate {@code M3CowLongList}: API group {@code COW_LONG_LIST} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-m3-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>JDK CopyOnWriteArrayList/Set: snapshot array per write; iterators never fail [license GPLv2+CPE; NEW_OWNER (adjacent only: m3/filtered-cow-bulk-receiver-20261017 = filtered COW bulk ops, not the list owner); priority P2; matrix owner PROPOSED:M3CowLongArrayList]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; lanes {@code long[] valueLane}; offset scheme immutable lane swapped per write; epoch counter; state-mask bits FROZEN_EPOCH; stored nodes NONE; API entry objects NONE (transient facades over slots, never the stored representation); matrix note: immutable value lane swapped per write; epoch offset / FROZEN per epoch; epoch counter.
 *
 * <p>Primitive copy-on-write list: writers serialize and publish immutable long[] epochs;
 * readers capture one volatile array reference. Public snapshots are detached copies.
 * M3-only specialization; never a replacement for {@code java.util.concurrent.CopyOnWriteArrayList}.
 */
//FLLDCIM: MRL - census-g2-20261106/apply.py v1 DONOR: postimage blob 1b3e1a6aa954 (preimage blob b73fa19b45eb = this owner before, under the declared transform) at ca1aafc4d94f
public final class M3CowLongList {
    private static final long[] EMPTY = new long[0];
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    /** State-mask bit {@code FROZEN_EPOCH} (per-slot state word, bit 0). */
    static final long STATE_FROZEN_EPOCH = 1L << 0;

    /** Storage lane {@code valueLane}: dynamic primitive/reference array lane. */
    private volatile long[] valueLane = EMPTY;
    /** Reserved schema marker; immutable published arrays encode frozen epochs without a second bitmap. */
    private long[] stateMaskWords = new long[0];
    /** Writer-maintained count; readers derive size from the captured published lane. */
    private int size;

    /** Creates an empty primitive COW list; the current epoch starts as a shared empty array. */
    public M3CowLongList() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3CowLongList|COW_LONG_LIST|LONG|FROZEN_EPOCH|long[] valueLane";
    }

    /** CopyOnWriteArrayList.get (long lane) (primitive copy-on-write). */
    public long get(int index) {
        long[] current = valueLane;
        return current[Objects.checkIndex(index, current.length)];
    }

    /** CopyOnWriteArrayList.add (copies lane) (primitive copy-on-write). */
    public boolean add(long value) {
        synchronized (this) {
            long[] current = valueLane;
            long[] next = Arrays.copyOf(current, nextLength(current.length));
            next[current.length] = value;
            publish(next);
            return true;
        }
    }

    /** CopyOnWriteArrayList.add(index) (primitive copy-on-write). */
    public void add(int index, long value) {
        synchronized (this) {
            long[] current = valueLane;
            Objects.checkIndex(index, nextLength(current.length));
            long[] next = new long[current.length + 1];
            System.arraycopy(current, 0, next, 0, index);
            next[index] = value;
            System.arraycopy(current, index, next, index + 1, current.length - index);
            publish(next);
        }
    }

    /** CopyOnWriteArrayList.set (primitive copy-on-write). */
    public long set(int index, long value) {
        synchronized (this) {
            long[] current = valueLane;
            int position = Objects.checkIndex(index, current.length);
            long previous = current[position];
            long[] next = current.clone();
            next[position] = value;
            publish(next);
            return previous;
        }
    }

    /** CopyOnWriteArrayList.remove(index) (primitive copy-on-write). */
    public long removeAt(int index) {
        synchronized (this) {
            long[] current = valueLane;
            int position = Objects.checkIndex(index, current.length);
            long previous = current[position];
            long[] next = new long[current.length - 1];
            System.arraycopy(current, 0, next, 0, position);
            System.arraycopy(current, position + 1, next, position,
                    current.length - position - 1);
            publish(next);
            return previous;
        }
    }

    /** CopyOnWriteArrayList.remove(Object) (primitive copy-on-write). */
    public boolean remove(long value) {
        synchronized (this) {
            long[] current = valueLane;
            int position = indexOf(current, value);
            if (position < 0) return false;
            long[] next = new long[current.length - 1];
            System.arraycopy(current, 0, next, 0, position);
            System.arraycopy(current, position + 1, next, position,
                    current.length - position - 1);
            publish(next);
            return true;
        }
    }

    /** CopyOnWriteArrayList.addIfAbsent (primitive copy-on-write). */
    public boolean addIfAbsent(long value) {
        synchronized (this) {
            long[] current = valueLane;
            if (indexOf(current, value) >= 0) return false;
            long[] next = Arrays.copyOf(current, nextLength(current.length));
            next[current.length] = value;
            publish(next);
            return true;
        }
    }

    /** CopyOnWriteArrayList.indexOf (primitive copy-on-write). */
    public int indexOf(long value) {
        return indexOf(valueLane, value);
    }

    /** CopyOnWriteArrayList.getArray (current epoch lane) (primitive copy-on-write). */
    public long[] snapshot() {
        return valueLane.clone();
    }

    /** snapshot iterator (never throws CME) (primitive copy-on-write). */
    public java.util.PrimitiveIterator.OfLong iterator() {
        long[] snapshot = valueLane;
        return new PrimitiveIterator.OfLong() {
            private int cursor;
            @Override public boolean hasNext() {
                return cursor < snapshot.length;
            }
            @Override public long nextLong() {
                if (cursor >= snapshot.length) throw new NoSuchElementException();
                return snapshot[cursor++];
            }
        };
    }

    /** List.size (primitive copy-on-write). */
    public int size() {
        return valueLane.length;
    }

    /** List.clear (primitive copy-on-write). */
    public void clear() {
        synchronized (this) {
            publish(EMPTY);
        }
    }
    private static int indexOf(long[] elements, long value) {
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == value) return i;
        }
        return -1;
    }

    private static int nextLength(int previous) {
        if (previous >= MAX_ARRAY_SIZE)
            throw new OutOfMemoryError("M3 COW long list capacity exhausted");
        return previous + 1;
    }

    private void publish(long[] next) {
        size = next.length;
        valueLane = next;
    }

}
