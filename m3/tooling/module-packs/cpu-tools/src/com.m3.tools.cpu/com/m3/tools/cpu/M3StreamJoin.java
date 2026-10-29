// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Deterministic bounded temporal equijoin window over indexed, sorted event times.
 * Pairs are left-major and then right-major; no mutable event objects are retained.
 * This is not a general SQL/stream-processing or out-of-order watermark engine.
 */
public final class M3StreamJoin {
    private M3StreamJoin() { }

    public record Pairs(int[] left, int[] right) {
        public Pairs {
            Objects.requireNonNull(left); Objects.requireNonNull(right);
            if (left.length != right.length) throw new IllegalArgumentException("pair geometry");
            left = left.clone(); right = right.clone();
        }
        @Override public int[] left() { return left.clone(); }
        @Override public int[] right() { return right.clone(); }
        public int size() { return left.length; }
    }

    /** Inclusive relative range [-before,+after], checked long endpoints, explicit pair cap. */
    public static Pairs within(long[] left, long[] right, long before, long after, int maxPairs) {
        Objects.requireNonNull(left); Objects.requireNonNull(right);
        if (before < 0 || after < 0 || maxPairs < 0 || maxPairs > 1_000_000
                || left.length > 1_000_000 || right.length > 1_000_000)
            throw new IllegalArgumentException("join bounds");
        ordered(left); ordered(right);
        int[] a = new int[Math.min(maxPairs, 16)], b = new int[a.length];
        int count = 0;
        for (int i = 0; i < left.length; i++) {
            long low = Math.subtractExact(left[i], before);
            long high = Math.addExact(left[i], after);
            int start = lowerBound(right, low), end = upperBound(right, high);
            if ((long)count + end - start > maxPairs)
                throw new IllegalArgumentException("joined pair budget exceeded");
            for (int j = start; j < end; j++) {
                if (count == a.length) {
                    int capacity = Math.min(maxPairs, Math.max(1, a.length * 2));
                    a = Arrays.copyOf(a, capacity);
                    b = Arrays.copyOf(b, capacity);
                }
                a[count] = i; b[count++] = j;
            }
        }
        return new Pairs(Arrays.copyOf(a, count), Arrays.copyOf(b, count));
    }

    private static void ordered(long[] times) {
        for (int i = 1; i < times.length; i++)
            if (times[i] < times[i - 1]) throw new IllegalArgumentException("unsorted event times");
    }
    private static int lowerBound(long[] a, long x) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] < x) lo = mid + 1; else hi = mid;
        }
        return lo;
    }
    private static int upperBound(long[] a, long x) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] <= x) lo = mid + 1; else hi = mid;
        }
        return lo;
    }
}
