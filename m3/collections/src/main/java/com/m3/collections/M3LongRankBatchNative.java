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
    private static final int NATIVE_MIN_INTERSECTION = 128;
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

    /**
     * Batched half-open interval counts for immutable signed-long sorted data.
     *
     * <p>Pairs are represented by parallel primitive arrays. Each [from, to)
     * query is validated before the first output write, including empty-source
     * queries. Pure primitive batches use one JNI transition above the existing
     * minimum batch threshold; missing native support uses the Java oracle.
     * No interval object, JNI callback, or native pointer escapes.</p>
     */
    static int[] rangeCounts(long[] sorted, long[] fromInclusive, long[] toExclusive) {
        Objects.requireNonNull(fromInclusive, "fromInclusive");
        Objects.requireNonNull(toExclusive, "toExclusive");
        if (fromInclusive.length != toExclusive.length) {
            throw new IllegalArgumentException("interval endpoint vector length mismatch");
        }
        int[] output = new int[fromInclusive.length];
        rangeCountInto(sorted, fromInclusive, toExclusive, 0, output.length, output, 0);
        return output;
    }

    static void rangeCountInto(long[] sorted, long[] fromInclusive, long[] toExclusive,
                               int queryFrom, int length, int[] output, int outputFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(fromInclusive, "fromInclusive");
        Objects.requireNonNull(toExclusive, "toExclusive");
        Objects.requireNonNull(output, "output");
        if (fromInclusive.length != toExclusive.length) {
            throw new IllegalArgumentException("interval endpoint vector length mismatch");
        }
        Objects.checkFromIndexSize(queryFrom, length, fromInclusive.length);
        Objects.checkFromIndexSize(outputFrom, length, output.length);
        // Prevalidate every interval before any output write. Strict [lo, hi):
        // equal endpoints count zero; no hi+1 overflow at Long.MAX_VALUE.
        for (int i = 0; i < length; i++) {
            int at = queryFrom + i;
            if (fromInclusive[at] > toExclusive[at]) {
                throw new IllegalArgumentException("inverted half-open interval at " + at);
            }
        }
        if (length == 0) return;
        if (sorted.length == 0) {
            Arrays.fill(output, outputFrom, outputFrom + length, 0);
            return;
        }
        if (AVAILABLE && length >= NATIVE_MIN_BATCH) {
            rangeCountInto0(sorted, fromInclusive, toExclusive,
                    queryFrom, length, output, outputFrom);
            return;
        }
        for (int i = 0; i < length; i++) {
            int at = queryFrom + i;
            output[outputFrom + i] = bound(sorted, toExclusive[at], false)
                    - bound(sorted, fromInclusive[at], false);
        }
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

    /**
     * LeetCode #350 category: multiset intersection cardinality over two
     * already-sorted signed-long owners. Two physical cursors retain duplicate
     * occurrence multiplicities without a Map.Entry or boxed result.
     *
     * <p>Both inputs must be immutable, sorted snapshots for the duration of
     * the call. The optional native owner is the existing m3rankbatch library:
     * one JNI transition and two fixed 256-long scratch pages, not retained
     * array pointers or a copied full payload.</p>
     */
    static long intersectionCount(long[] left, long[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length == 0 || right.length == 0) return 0L;
        if (AVAILABLE && Math.min(left.length, right.length) >= NATIVE_MIN_INTERSECTION) {
            return intersectionCount0(left, right);
        }
        int i = 0;
        int j = 0;
        long matches = 0L;
        while (i < left.length && j < right.length) {
            if (left[i] < right[j]) {
                i++;
            } else if (left[i] > right[j]) {
                j++;
            } else {
                matches++;
                i++;
                j++;
            }
        }
        return matches;
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

    private static native void rangeCountInto0(long[] sorted, long[] fromInclusive,
            long[] toExclusive, int queryFrom, int length, int[] output, int outputFrom);

    private static native long intersectionCount0(long[] left, long[] right);
}
