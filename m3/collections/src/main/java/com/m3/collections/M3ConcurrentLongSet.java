/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Read-optimized concurrent primitive long set.
 *
 * <p>Readers perform one acquire-load and binary search an immutable sorted
 * {@code long[]} snapshot. Writers copy the primitive array and CAS-publish the
 * replacement. There are no per-entry nodes or wrapper values.
 *
 * <p>Best for read-heavy, small/medium cardinality state. Write cost is O(n).
 */
public final class M3ConcurrentLongSet implements M3LongCollection {
    /** Creates an empty instance with no populated storage. */
    public M3ConcurrentLongSet() {}

    private static final long[] EMPTY = new long[0];
    private static final VarHandle VALUES;

    static {
        try {
            VALUES = MethodHandles.lookup().findVarHandle(
                    M3ConcurrentLongSet.class, "values", long[].class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @SuppressWarnings("unused")
    private volatile long[] values = EMPTY;

    private long[] current() {
        return (long[]) VALUES.getAcquire(this);
    }

    public boolean add(long value) {
        for (;;) {
            long[] before = current();
            int found = Arrays.binarySearch(before, value);
            if (found >= 0) return false;
            int insert = -found - 1;
            long[] after = new long[before.length + 1];
            System.arraycopy(before, 0, after, 0, insert);
            after[insert] = value;
            System.arraycopy(before, insert, after, insert + 1, before.length - insert);
            if (VALUES.compareAndSet(this, before, after)) return true;
        }
    }

    public boolean remove(long value) {
        for (;;) {
            long[] before = current();
            int found = Arrays.binarySearch(before, value);
            if (found < 0) return false;
            if (before.length == 1) {
                if (VALUES.compareAndSet(this, before, EMPTY)) return true;
                continue;
            }
            long[] after = new long[before.length - 1];
            System.arraycopy(before, 0, after, 0, found);
            System.arraycopy(before, found + 1, after, found, before.length - found - 1);
            if (VALUES.compareAndSet(this, before, after)) return true;
        }
    }

    @Override public boolean contains(long value) {
        return Arrays.binarySearch(current(), value) >= 0;
    }

    @Override public int size() {
        return current().length;
    }

    @Override public void clear() {
        for (;;) {
            long[] before = current();
            if (before.length == 0) return;
            if (VALUES.compareAndSet(this, before, EMPTY)) return;
        }
    }

    @Override public long[] toArray() {
        return current().clone();
    }

    /** Primitive splitting over one immutable captured array; no element copy is made. */
    @Override public java.util.Spliterator.OfLong longSpliterator() {
        long[] snapshot = current();
        return java.util.Spliterators.spliterator(snapshot, java.util.Spliterator.ORDERED
                | java.util.Spliterator.DISTINCT | java.util.Spliterator.IMMUTABLE);
    }

    @Override public M3LongIterator iterator() {
        long[] snapshot = current();
        return new M3LongIterator() {
            private int index;
            @Override public boolean hasNext() { return index < snapshot.length; }
            @Override public long nextLong() {
                if (!hasNext()) throw new NoSuchElementException();
                return snapshot[index++];
            }
        };
    }
}
