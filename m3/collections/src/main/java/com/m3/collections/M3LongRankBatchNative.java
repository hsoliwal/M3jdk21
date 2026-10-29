// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Optional batch JNI accelerator for primitive lower bounds.
 *
 * <p>Native code is never required for correctness or JDK boot. Explicitly opt in with
 * {@code -Dm3.collections.rank.native=true} and a matching library on java.library.path.
 * A missing library uses the Java implementation; normal small batches stay in Java to
 * avoid native transition overhead. No native pointers or GC addresses escape to callers.</p>
 */
public final class M3LongRankBatchNative {
    private static final int NATIVE_MIN_BATCH = 64;
    private static final boolean AVAILABLE = loadOptIn();

    private M3LongRankBatchNative() { }

    public static boolean nativeAvailable() {
        return AVAILABLE;
    }

    static int[] lowerBounds(long[] sorted, long[] queries) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        int[] results = new int[queries.length];
        if (sorted.length == 0 || queries.length == 0) {
            return results;
        }
        if (AVAILABLE && queries.length >= NATIVE_MIN_BATCH) {
            lowerBounds0(sorted, queries, results);
            return results;
        }
        for (int index = 0; index < queries.length; index++) {
            long value = queries[index];
            int lo = 0;
            int hi = sorted.length;
            while (lo < hi) {
                int middle = lo + ((hi - lo) >>> 1);
                if (sorted[middle] < value) {
                    lo = middle + 1;
                } else {
                    hi = middle;
                }
            }
            results[index] = lo;
        }
        return results;
    }

    /**
     * One primitive output lane: [lower(q0),upper(q0),lower(q1),upper(q1),...].
     * Upper-bound bisection begins at lower-bound, preserving signed long order.
     * No per-query object allocation or JNI callback is ever required.
     */
    static int[] lowerUpperBounds(long[] sorted, long[] queries) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        int[] pairs = new int[Math.multiplyExact(queries.length, 2)];
        lowerUpperBoundsInto(sorted, queries, pairs);
        return pairs;
    }

    /** Reuses a caller-owned pair lane, requiring exactly twice the query length. */
    static void lowerUpperBoundsInto(long[] sorted, long[] queries, int[] pairs) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.requireNonNull(pairs, "pairs");
        if (queries.length > Integer.MAX_VALUE / 2
                || pairs.length != queries.length * 2) {
            throw new IllegalArgumentException("pair lane must contain two slots per query");
        }
        if (sorted.length == 0 || queries.length == 0) {
            Arrays.fill(pairs, 0);
            return;
        }
        if (AVAILABLE && queries.length >= NATIVE_MIN_BATCH) {
            lowerUpperBounds0(sorted, queries, pairs);
            return;
        }
        for (int i = 0; i < queries.length; i++) {
            int lo = M3LaneSearch.lowerBound(sorted, 0, sorted.length, queries[i]);
            int hi = M3LaneSearch.upperBound(sorted, lo, sorted.length, queries[i]);
            pairs[i << 1] = lo;
            pairs[(i << 1) + 1] = hi;
        }
    }

    private static boolean loadOptIn() {
        if (!Boolean.getBoolean("m3.collections.rank.native")) {
            return false;
        }
        try {
            System.loadLibrary("m3rankbatch");
            return true;
        } catch (UnsatisfiedLinkError | SecurityException unavailable) {
            return false;
        }
    }

    private static native void lowerBounds0(long[] sorted, long[] queries, int[] results);

    private static native void lowerUpperBounds0(long[] sorted, long[] queries, int[] pairs);
}
