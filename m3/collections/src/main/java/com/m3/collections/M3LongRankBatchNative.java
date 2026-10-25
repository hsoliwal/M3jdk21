// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Optional batch JNI accelerator for primitive lower/upper bounds and occurrence counts.
 *
 * <p>Native code is never required for correctness or JDK boot. Explicitly opt in with
 * {@code -Dm3.collections.rank.native=true} and a matching library on java.library.path.
 * A missing library uses the Java implementation; normal small batches stay in Java to
 * avoid native transition overhead. No native pointers or GC addresses escape to callers.</p>
 */
public final class M3LongRankBatchNative {
    private static final int NATIVE_MIN_BATCH = 64;
    private static final int MODE_UPPER = 1;
    private static final int MODE_COUNT = 2;
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
     * Flat primitive [lower, upper) index pairs, one pair per query, in encounter order.
     * Both endpoints are insertion coordinates even when no value matches.
     * A single JNI transition handles prepared batches; small batches stay in Java.
     * No comparator or user callback is invoked by the native kernel.
     */
    static int[] equalRanges(long[] sorted, long[] queries) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        int[] ranges = new int[Math.multiplyExact(queries.length, 2)];
        if (sorted.length == 0 || queries.length == 0) {
            return ranges;
        }
        if (AVAILABLE && queries.length >= NATIVE_MIN_BATCH) {
            equalRanges0(sorted, queries, ranges);
            return ranges;
        }
        for (int index = 0; index < queries.length; index++) {
            long value = queries[index];
            int lo = 0;
            int hi = sorted.length;
            while (lo < hi) {
                int middle = lo + ((hi - lo) >>> 1);
                if (sorted[middle] < value) lo = middle + 1;
                else hi = middle;
            }
            int first = lo;
            hi = sorted.length;
            while (lo < hi) {
                int middle = lo + ((hi - lo) >>> 1);
                if (sorted[middle] <= value) lo = middle + 1;
                else hi = middle;
            }
            int pair = index << 1;
            ranges[pair] = first;
            ranges[pair + 1] = lo;
        }
        return ranges;
    }

    static int[] upperBounds(long[] sorted, long[] queries) {
        Objects.requireNonNull(queries, "queries");
        int[] output = new int[queries.length];
        boundBatchInto(sorted, queries, 0, queries.length, output, 0, MODE_UPPER);
        return output;
    }

    static int[] counts(long[] sorted, long[] queries) {
        Objects.requireNonNull(queries, "queries");
        int[] output = new int[queries.length];
        countInto(sorted, queries, 0, queries.length, output, 0);
        return output;
    }

    static void countInto(long[] sorted, long[] queries, int queryFrom, int length,
                          int[] output, int outputFrom) {
        boundBatchInto(sorted, queries, queryFrom, length, output, outputFrom, MODE_COUNT);
    }

    private static void boundBatchInto(long[] sorted, long[] queries, int queryFrom,
                                       int length, int[] output, int outputFrom, int mode) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.requireNonNull(output, "output");
        Objects.checkFromIndexSize(queryFrom, length, queries.length);
        Objects.checkFromIndexSize(outputFrom, length, output.length);
        if (length == 0) return;
        if (sorted.length == 0) {
            Arrays.fill(output, outputFrom, outputFrom + length, 0);
            return;
        }
        if (AVAILABLE && length >= NATIVE_MIN_BATCH) {
            boundBatchInto0(sorted, queries, queryFrom, length, output, outputFrom, mode);
            return;
        }
        for (int i = 0; i < length; i++) {
            long target = queries[queryFrom + i];
            int upper = bound(sorted, target, true);
            output[outputFrom + i] = mode == MODE_COUNT
                    ? upper - bound(sorted, target, false) : upper;
        }
    }

    private static int bound(long[] sorted, long target, boolean upper) {
        int lo = 0, hi = sorted.length;
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (upper ? sorted[mid] <= target : sorted[mid] < target) lo = mid + 1;
            else hi = mid;
        }
        return lo;
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

    private static native void equalRanges0(long[] sorted, long[] queries, int[] ranges);

    private static native void boundBatchInto0(long[] sorted, long[] queries, int queryFrom,
            int length, int[] output, int outputFrom, int mode);
}
