// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

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
    // A previously installed m3rankbatch can implement lowerBounds0 without
    // the later fused-bounds JNI ABI. Keep that original native route usable.
    private static final boolean FUSED_AVAILABLE = detectFusedCapability();
    private static final int NATIVE_MIN_FREQUENCY_TOP_K = 256;
    private static volatile boolean TOP_K_FREQUENCY_ABI_MISSING;

    private M3LongRankBatchNative() { }

    public static boolean nativeAvailable() {
        return AVAILABLE;
    }

    /** Whether this loaded library implements the fused rank-bounds ABI. */
    public static boolean fusedNativeAvailable() {
        return FUSED_AVAILABLE;
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
     * LC #347 sorted run-frequency Top-K, using the existing M3SignalTopK
     * worst-first primitive heap pattern. The input is an immutable
     * signed-long sorted snapshot, not a caller-owned arbitrary unsorted array.
     * Rank tie-break: smaller signed value wins at equal frequency.
     *
     * <p>No per-element Entry objects, caller callbacks, copied owner payload,
     * retained JNI pointers or per-key native transitions. The original JNI
     * rank symbols remain ABI-compatible; older native libraries fall back
     * only for optional dispatch, never for explicitly required native mode.
     */
    static long[] topKFrequent(long[] sorted, int k) {
        return topKFrequent(sorted, k, false);
    }

    /** Package-level native-required proof seam: never silently downgrade. */
    static long[] topKFrequent(long[] sorted, int k, boolean nativeRequired) {
        Objects.requireNonNull(sorted, "sorted");
        if (k < 0) throw new IllegalArgumentException("negative top-K frequency limit");
        if (nativeRequired) {
            if (!AVAILABLE) {
                throw new UnsatisfiedLinkError("m3rankbatch top-K frequency JNI unavailable");
            }
            return topKFrequent0(sorted, k);
        }
        if (sorted.length == 0 || k == 0) return new long[0];
        if (AVAILABLE && !TOP_K_FREQUENCY_ABI_MISSING
                && sorted.length >= NATIVE_MIN_FREQUENCY_TOP_K) {
            try {
                return topKFrequent0(sorted, k);
            } catch (UnsatisfiedLinkError olderNativeAbi) {
                TOP_K_FREQUENCY_ABI_MISSING = true;
            }
        }
        return topKFrequentJava(sorted, k);
    }

    /** Exact Java fallback/oracle; two primitive heap lanes, O(min(k,n)) memory. */
    static long[] topKFrequentJava(long[] sorted, int k) {
        Objects.requireNonNull(sorted, "sorted");
        if (k < 0) throw new IllegalArgumentException("negative top-K frequency limit");
        int capacity = Math.min(k, sorted.length);
        if (capacity == 0) return new long[0];
        long[] keys = new long[capacity];
        int[] frequencies = new int[capacity];
        int size = 0;
        long previous = sorted[0];
        int count = 1;
        for (int i = 1; i < sorted.length; i++) {
            long value = sorted[i];
            if (value == previous) {
                count++;
            } else {
                size = topKOffer(keys, frequencies, size, previous, count);
                previous = value;
                count = 1;
            }
        }
        size = topKOffer(keys, frequencies, size, previous, count);
        // In-place worst-first extraction leaves best-ranked values first.
        for (int end = size - 1; end > 0; end--) {
            topKSwap(keys, frequencies, 0, end);
            topKSiftDown(keys, frequencies, 0, end);
        }
        return Arrays.copyOf(keys, size);
    }

    private static boolean topKWorse(int countA, long keyA, int countB, long keyB) {
        return countA < countB || (countA == countB && keyA > keyB);
    }

    private static void topKSwap(long[] keys, int[] frequencies, int a, int b) {
        long key = keys[a]; keys[a] = keys[b]; keys[b] = key;
        int count = frequencies[a]; frequencies[a] = frequencies[b]; frequencies[b] = count;
    }

    private static void topKSiftDown(long[] keys, int[] frequencies, int at, int size) {
        while (at < size / 2) {
            int child = at * 2 + 1;
            if (child >= size) break;
            if (child + 1 < size && topKWorse(
                    frequencies[child + 1], keys[child + 1],
                    frequencies[child], keys[child])) child++;
            if (!topKWorse(frequencies[child], keys[child],
                    frequencies[at], keys[at])) return;
            topKSwap(keys, frequencies, child, at);
            at = child;
        }
    }

    private static int topKOffer(long[] keys, int[] frequencies,
                                 int size, long key, int count) {
        if (size < keys.length) {
            int at = size++;
            keys[at] = key;
            frequencies[at] = count;
            while (at > 0) {
                int parent = (at - 1) >>> 1;
                if (!topKWorse(frequencies[at], keys[at],
                        frequencies[parent], keys[parent])) break;
                topKSwap(keys, frequencies, at, parent);
                at = parent;
            }
        } else if (topKWorse(frequencies[0], keys[0], count, key)) {
            keys[0] = key;
            frequencies[0] = count;
            topKSiftDown(keys, frequencies, 0, size);
        }
        return size;
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
        if (FUSED_AVAILABLE && queries.length >= NATIVE_MIN_BATCH) {
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

    // Resolve a feature bit once, before any large input crosses JNI.
    // Old libraries without capabilities0 remain valid for lowerBounds0.
    private static boolean detectFusedCapability() {
        if (!AVAILABLE) {
            return false;
        }
        try {
            return (capabilities0() & 1) != 0;
        } catch (UnsatisfiedLinkError | SecurityException legacyLibrary) {
            return false;
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
    /** Additive LC #347 source-owned m3rankbatch JNI symbol. */
    private static native long[] topKFrequent0(long[] sorted, int k);


    private static native void lowerBounds0(long[] sorted, long[] queries, int[] results);

    /** Capability bit 0 signals the fused signed-long pair operation. */
    private static native int capabilities0();

    private static native void lowerUpperBounds0(long[] sorted, long[] queries, int[] pairs);
}
