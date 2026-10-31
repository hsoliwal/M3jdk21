/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * A fixed-size, writable primitive-long view backed by the caller's array.
 *
 * <p>Like {@link java.util.Arrays#asList(Object...)}, the view exposes writes in
 * both directions and refuses attempts to change its length. Unlike the JDK
 * generic list, it stores primitive {@code long} values without boxing or
 * per-element objects. The array is never copied on construction; only an
 * explicit {@link #toArray()} makes a detached copy. Not thread-safe.</p>
 *
 * <p>This M3-specific type does not replace {@code java.util.List} or modify
 * any public JDK 21 contract. The original M3 public signatures and catalogue
 * descriptor remain unchanged.</p>
 */
public final class M3FixedSizeLongListView {
    /** The view always has a fixed logical length. */
    static final long STATE_FIXED_SIZE = 1L << 0;
    private static final long[] EMPTY = new long[0];

    /** Authoritative caller-owned storage, with logical index = array offset. */
    private final long[] valueLane;

    /** Empty fixed-size view. */
    public M3FixedSizeLongListView() {
        valueLane = EMPTY;
    }

    private M3FixedSizeLongListView(long[] backing) {
        valueLane = Objects.requireNonNull(backing, "backing");
    }

    /** Preserve the original public provider/owner catalogue identity. */
    public static String scaffoldDescriptor() {
        return "M3FixedSizeLongListView|FIXED_SIZE_LONG_LIST|LONG|FIXED_SIZE|long[] valueLane";
    }

    /** Retain the actual array; external writes are immediately visible. */
    public static M3FixedSizeLongListView of(long[] backing) {
        return new M3FixedSizeLongListView(backing);
    }

    /** O(1) indexed read; out-of-range access throws before reading storage. */
    public long get(int index) {
        return valueLane[Objects.checkIndex(index, valueLane.length)];
    }

    /** O(1) indexed write-through returning the previous primitive value. */
    public long set(int index, long value) {
        int checked = Objects.checkIndex(index, valueLane.length);
        long previous = valueLane[checked];
        valueLane[checked] = value;
        return previous;
    }

    /** First occurrence under signed primitive-long equality, or {@code -1}. */
    public int indexOf(long value) {
        for (int i = 0; i < valueLane.length; i++) {
            if (valueLane[i] == value) return i;
        }
        return -1;
    }

    public boolean contains(long value) {
        return indexOf(value) >= 0;
    }

    /** Detached copy; mutating the result never changes the backing array. */
    public long[] toArray() {
        return valueLane.clone();
    }

    public int size() {
        return valueLane.length;
    }

    /** Fixed-size contract: append is forbidden even when the view is empty. */
    public boolean add(long value) {
        throw new UnsupportedOperationException("fixed-size long array view");
    }
}
