// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;

/**
 * Optional batch JNI accelerator for primitive lower and upper bounds.
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
     * Batched upper bounds over a signed sorted lane (LeetCode 34 pattern).
     * Older native libraries lacking the additive symbol use the Java kernel;
     * small batches stay on-heap to avoid a JNI transition.
     */
    static int[] upperBounds(long[] sorted, long[] queries) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        int[] results = new int[queries.length];
        if (sorted.length == 0 || queries.length == 0) {
            return results;
        }
        if (queries.length >= NATIVE_MIN_BATCH && nativeUpperBoundsAvailable()) {
            upperBounds0(sorted, queries, results);
            return results;
        }
        for (int index = 0; index < queries.length; index++) {
            results[index] = M3LaneSearch.upperBound(sorted, 0, sorted.length, queries[index]);
        }
        return results;
    }

    /** Capability is distinct from the older lower-bound native ABI. */
    static boolean nativeUpperBoundsAvailable() {
        return AVAILABLE && NativeUpperBounds.AVAILABLE;
    }

    private static final class NativeUpperBounds {
        private static final boolean AVAILABLE = available();
        private static boolean available() {
            try {
                return upperBoundsAbi0() == 1;
            } catch (UnsatisfiedLinkError legacyNativeLibrary) {
                return false;
            }
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
    private static native int upperBoundsAbi0();
    private static native void upperBounds0(long[] sorted, long[] queries, int[] results);
}
