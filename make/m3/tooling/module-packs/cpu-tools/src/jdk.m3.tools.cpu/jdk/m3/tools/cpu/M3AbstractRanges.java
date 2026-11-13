// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

/** Sound, bounded, non-relational intervals for signed Java int arithmetic. */
public final class M3AbstractRanges {
    public static final int UNKNOWN = -1;
    public static final int FALSE = 0;
    public static final int TRUE = 1;
    private static final long MIN = Integer.MIN_VALUE;
    private static final long MAX = Integer.MAX_VALUE;

    private M3AbstractRanges() { }

    /** A fresh, canonical empty set. */
    public static long[] bottom() { return new long[] {1L, 0L}; }

    /** A fresh signed-32-bit universe. */
    public static long[] top() { return new long[] {MIN, MAX}; }

    public static long[] constant(int value) { return new long[] {value, value}; }

    /** Validate canonical bottom or an inclusive nonempty signed-int interval. */
    public static void validate(long[] range) {
        if (range == null || range.length != 2) {
            throw new IllegalArgumentException("range must have exactly two endpoints");
        }
        if (range[0] == 1L && range[1] == 0L) { return; }
        if (range[0] < MIN || range[1] > MAX || range[0] > range[1]) {
            throw new IllegalArgumentException("invalid int interval");
        }
    }

    private static boolean empty(long[] x) { return x[0] == 1L && x[1] == 0L; }
    private static long[] copy(long[] x) { return new long[] {x[0], x[1]}; }
    private static long[] bounded(long lo, long hi) {
        return lo < MIN || hi > MAX ? top() : new long[] {lo, hi};
    }

    /** Least interval containing both abstract values. */
    public static long[] join(long[] left, long[] right) {
        validate(left); validate(right);
        if (empty(left)) { return copy(right); }
        if (empty(right)) { return copy(left); }
        return new long[] {Math.min(left[0], right[0]), Math.max(left[1], right[1])};
    }

    /** Exact intersection in the interval domain. */
    public static long[] meet(long[] left, long[] right) {
        validate(left); validate(right);
        if (empty(left) || empty(right)) { return bottom(); }
        long lo = Math.max(left[0], right[0]);
        long hi = Math.min(left[1], right[1]);
        return lo > hi ? bottom() : new long[] {lo, hi};
    }

    /** Sound Java-int addition; possible signed wrap conservatively gives TOP. */
    public static long[] add(long[] left, long[] right) {
        validate(left); validate(right);
        if (empty(left) || empty(right)) { return bottom(); }
        return bounded(left[0] + right[0], left[1] + right[1]);
    }

    /** Sound Java-int subtraction; possible signed wrap conservatively gives TOP. */
    public static long[] subtract(long[] left, long[] right) {
        validate(left); validate(right);
        if (empty(left) || empty(right)) { return bottom(); }
        return bounded(left[0] - right[1], left[1] - right[0]);
    }

    /** Sound Java-int multiplication; the four endpoint products fit in long. */
    public static long[] multiply(long[] left, long[] right) {
        validate(left); validate(right);
        if (empty(left) || empty(right)) { return bottom(); }
        long a = left[0] * right[0];
        long b = left[0] * right[1];
        long c = left[1] * right[0];
        long d = left[1] * right[1];
        long lo = Math.min(Math.min(a, b), Math.min(c, d));
        long hi = Math.max(Math.max(a, b), Math.max(c, d));
        return bounded(lo, hi);
    }

    /** Sound Java-int unary negation, including MIN_VALUE wraparound. */
    public static long[] negate(long[] range) {
        validate(range);
        if (empty(range)) { return bottom(); }
        return range[0] == MIN ? top() : new long[] {-range[1], -range[0]};
    }

    /** Interval widening, preserving upper-bound monotonicity. */
    public static long[] widen(long[] previous, long[] next) {
        validate(previous); validate(next);
        if (empty(previous)) { return copy(next); }
        if (empty(next)) { return copy(previous); }
        return new long[] {next[0] < previous[0] ? MIN : previous[0],
                next[1] > previous[1] ? MAX : previous[1]};
    }

    /** Three-valued universal comparison; empty-domain comparisons are unknown. */
    public static int lessThan(long[] left, long[] right) {
        validate(left); validate(right);
        if (empty(left) || empty(right)) { return UNKNOWN; }
        if (left[1] < right[0]) { return TRUE; }
        if (left[0] >= right[1]) { return FALSE; }
        return UNKNOWN;
    }

    /** Cardinality of the concretization; TOP has 2^32 integers. */
    public static long cardinality(long[] range) {
        validate(range);
        return empty(range) ? 0L : range[1] - range[0] + 1L;
    }
}
