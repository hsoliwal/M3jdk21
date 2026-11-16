/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections.range;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import jdk.internal.m3.collections.LaneSearch;
import jdk.internal.m3.collections.LongKeyOrder;

/**
 * Immutable sorted, disjoint closed ranges over the signed-long domain.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-m3-to-public/LIB-SUPERSET):
 * Guava {@code ImmutableRangeSet}, Apache-2.0. Closed-range construction preserves
 * the provider's range boundaries and rejects overlapping ranges. In particular,
 * integer-adjacent ranges remain distinct; enclosing a query requires one range.
 *
 * <p>DOOP mapping: exact paired {@code long[] lowerLane;long[] upperLane}, sorted
 * range ordinals, binary-search offsets and one packed FROZEN bit per range.
 * No range or entry object is retained per ordinal. The descriptor is the
 * original compatibility identity, not an execution-admission receipt.
 *
 * <p>Complement projects open and unbounded provider gaps onto closed signed-long
 * ranges, omitting gaps with no long value. A reciprocal cached complement
 * preserves the original range boundaries on a double complement. No mutable
 * input array is retained or exposed. This owner is not a {@code java.util}
 * replacement.
 */
public final class FrozenLongRangeSet {
    /** State-mask bit {@code FROZEN} (per-slot state word, bit 0). */
    static final long STATE_FROZEN = 1L << 0;

    private final long[] lowerLane;
    private final long[] upperLane;
    private final long[] stateMaskWords;
    private final int size;
    private volatile FrozenLongRangeSet complement;

    /** Creates an empty immutable owner. */
    public FrozenLongRangeSet() {
        this(new long[0], new long[0]);
    }

    private FrozenLongRangeSet(long[] lowerLane, long[] upperLane) {
        this.lowerLane = lowerLane;
        this.upperLane = upperLane;
        size = lowerLane.length;
        int tail = size & 63;
        stateMaskWords = new long[(size >>> 6) + (tail == 0 ? 0 : 1)];
        Arrays.fill(stateMaskWords, -1L);
        if (tail != 0) stateMaskWords[stateMaskWords.length - 1] = (1L << tail) - 1;
    }

    /** Reports the declared state-mask bits and lanes for ledgers. */
    public static String scaffoldDescriptor() {
        return "M3FrozenLongRangeSet|FROZEN_LONG_RANGE_SET|LONG|FROZEN|long[] lowerLane;long[] upperLane";
    }

    /**
     * Copies closed ranges in signed lower-bound order, preserving their boundaries.
     * Null lanes are rejected; lengths must match and every lower must be at most
     * its upper. Overlaps, including duplicates and shared endpoints, are rejected.
     * Integer-adjacent disjoint ranges remain separate, as with closed Guava ranges.
     */
    public static FrozenLongRangeSet copyOf(long[] lowers, long[] uppers) {
        Objects.requireNonNull(lowers, "lowers");
        Objects.requireNonNull(uppers, "uppers");
        if (lowers.length != uppers.length) throw new IllegalArgumentException("lane lengths");
        long[] lowerCopy = lowers.clone();
        long[] upperCopy = uppers.clone();
        for (int i = 0; i < lowerCopy.length; i++) {
            if (lowerCopy[i] > upperCopy[i]) throw new IllegalArgumentException("reversed range");
        }
        int[] order = LongKeyOrder.ascending(lowerCopy, false);
        long[] sortedLower = new long[order.length];
        long[] sortedUpper = new long[order.length];
        for (int i = 0; i < order.length; i++) {
            int source = order[i];
            long lower = lowerCopy[source];
            if (i > 0 && lower <= sortedUpper[i - 1]) {
                throw new IllegalArgumentException("overlapping ranges");
            }
            sortedLower[i] = lower;
            sortedUpper[i] = upperCopy[source];
        }
        return new FrozenLongRangeSet(sortedLower, sortedUpper);
    }

    /** Returns the number of distinct stored ranges. */
    public int rangeCount() {
        return size;
    }

    /** Returns whether no range is stored. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Returns whether a stored closed range contains the value. */
    public boolean contains(long value) {
        return rangeIndexOf(value) >= 0;
    }

    /** Returns whether one range encloses the closed query; reversed queries reject. */
    public boolean encloses(long lower, long upper) {
        if (lower > upper) throw new IllegalArgumentException("reversed range");
        int ordinal = rangeIndexOf(lower);
        return ordinal >= 0 && upper <= upperLane[ordinal];
    }

    /** Returns the containing range ordinal, or {@code -1} if absent. */
    public int rangeIndexOf(long value) {
        int ordinal = LaneSearch.upperBound(lowerLane, 0, size, value) - 1;
        return ordinal >= 0 && value <= upperLane[ordinal] ? ordinal : -1;
    }

    /** Returns the closed lower endpoint at a valid range ordinal. */
    public long lowerAt(int ordinal) {
        return lowerLane[Objects.checkIndex(ordinal, size)];
    }

    /** Returns the closed upper endpoint at a valid range ordinal. */
    public long upperAt(int ordinal) {
        return upperLane[Objects.checkIndex(ordinal, size)];
    }

    /** Returns the first lower endpoint, or throws if empty. */
    public long spanLower() {
        if (size == 0) throw new NoSuchElementException();
        return lowerLane[0];
    }

    /** Returns the last upper endpoint, or throws if empty. */
    public long spanUpper() {
        if (size == 0) throw new NoSuchElementException();
        return upperLane[size - 1];
    }

    /**
     * Returns the immutable complement in the signed-long domain. Open provider
     * gaps become closed successor/predecessor bounds; discrete-empty gaps are
     * omitted. The reciprocal complement retains this owner's original boundaries.
     */
    public FrozenLongRangeSet complement() {
        FrozenLongRangeSet result = complement;
        if (result == null) {
            synchronized (this) {
                result = complement;
                if (result == null) {
                    result = projectedComplement();
                    result.complement = this;
                    complement = result;
                }
            }
        }
        return result;
    }

    private FrozenLongRangeSet projectedComplement() {
        if (size == 0) {
            return new FrozenLongRangeSet(new long[] {Long.MIN_VALUE}, new long[] {Long.MAX_VALUE});
        }
        long gaps = lowerLane[0] > Long.MIN_VALUE ? 1 : 0;
        for (int i = 1; i < size; i++) {
            long previous = upperLane[i - 1];
            if (previous < Long.MAX_VALUE && previous + 1 < lowerLane[i]) gaps++;
        }
        if (upperLane[size - 1] < Long.MAX_VALUE) gaps++;
        if (gaps > Integer.MAX_VALUE) throw new OutOfMemoryError("too many complement ranges");
        long[] lowers = new long[(int) gaps];
        long[] uppers = new long[(int) gaps];
        int at = 0;
        if (lowerLane[0] > Long.MIN_VALUE) {
            lowers[at] = Long.MIN_VALUE;
            uppers[at++] = lowerLane[0] - 1;
        }
        for (int i = 1; i < size; i++) {
            long previous = upperLane[i - 1];
            if (previous < Long.MAX_VALUE && previous + 1 < lowerLane[i]) {
                lowers[at] = previous + 1;
                uppers[at++] = lowerLane[i] - 1;
            }
        }
        if (upperLane[size - 1] < Long.MAX_VALUE) {
            lowers[at] = upperLane[size - 1] + 1;
            uppers[at] = Long.MAX_VALUE;
        }
        return new FrozenLongRangeSet(lowers, uppers);
    }

    /** Always rejects mutation, including no-op and invalid requests. */
    public void add(long lower, long upper) {
        throw new UnsupportedOperationException();
    }

    /** Always rejects mutation, including no-op and invalid requests. */
    public void remove(long lower, long upper) {
        throw new UnsupportedOperationException();
    }
}
