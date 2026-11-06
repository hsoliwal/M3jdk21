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
    //FLLDCIM: MRL - census P2-073, crate m3jdk21-lc719-pair-distance-java-20261026 (donor blob 83ce0911f53f): LC 719 pair-distance batch thresholds and ABI flag
    private static final int NATIVE_MIN_PAIR_DISTANCE_BATCH = 64;
    private static final int NATIVE_MIN_PAIR_DISTANCE_OWNER = 128;
    private static volatile boolean PAIR_DISTANCE_ABI_MISSING;
    private static final boolean PAIR_DISTANCE_NATIVE_OPT_IN =
            Boolean.getBoolean("m3.collections.rank.pairdistance.native");
    //FLLDCIM: MRL - census P2-073, crate m3jdk21-lc315-count-smaller-20261026 (donor blob 7871ab51e2ec): LC 315 ABI flag
    private static volatile boolean COUNT_SMALLER_ABI_MISSING;

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

    //FLLDCIM: MRL - census P2-073, crate m3jdk21-lc719-pair-distance-java-20261026 (donor blob 83ce0911f53f): LC 719 pair-distance counts (Java oracle + optional JNI batch)
    /**
     * LeetCode #719 count-within-distance predicate over an immutable, sorted
     * signed-long lane. Each i < j occurrence is counted independently. Input
     * thresholds are nonnegative signed longs; full mathematical differences
     * spanning Long.MIN_VALUE..Long.MAX_VALUE use unsigned subtraction.
     *
     * <p>JNI batches sufficiently large inputs in the original m3rankbatch
     * native owner only when a separately benchmark-admitted native property
     * is explicitly enabled, using fixed array-region pages, no Java callback and no
     * retained GC address. Older optional native binaries fall back to Java;
     * native-required requests never hide a missing ABI.</p>
     */
    static long[] pairDistanceCounts(long[] sorted, long[] thresholds) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(thresholds, "thresholds");
        validatePairDistances(thresholds);
        long[] result = new long[thresholds.length];
        if (sorted.length < 2 || thresholds.length == 0) return result;
        if (AVAILABLE && PAIR_DISTANCE_NATIVE_OPT_IN && !PAIR_DISTANCE_ABI_MISSING
                && sorted.length >= NATIVE_MIN_PAIR_DISTANCE_OWNER
                && thresholds.length >= NATIVE_MIN_PAIR_DISTANCE_BATCH) {
            try {
                pairDistanceCounts0(sorted, thresholds, result);
                return result;
            } catch (UnsatisfiedLinkError olderNativeLibrary) {
                PAIR_DISTANCE_ABI_MISSING = true;
            }
        }
        for (int i = 0; i < thresholds.length; i++) {
            result[i] = pairDistanceCountJava(sorted, thresholds[i]);
        }
        return result;
    }

    /** Force the exact original JNI ABI for parity; missing libraries fail closed. */
    static long[] pairDistanceCountsNativeRequired(long[] sorted, long[] thresholds) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(thresholds, "thresholds");
        validatePairDistances(thresholds);
        if (!AVAILABLE) {
            throw new UnsatisfiedLinkError("m3rankbatch pair-distance JNI unavailable");
        }
        long[] result = new long[thresholds.length];
        pairDistanceCounts0(sorted, thresholds, result);
        return result;
    }

    /** Independent original Java two-pointer oracle: O(n), O(1) extra. */
    static long pairDistanceCountJava(long[] sorted, long maxDistance) {
        Objects.requireNonNull(sorted, "sorted");
        if (maxDistance < 0) {
            throw new IllegalArgumentException("distance threshold must be nonnegative");
        }
        int left = 0;
        long count = 0L;
        for (int right = 1; right < sorted.length; right++) {
            while (left < right && Long.compareUnsigned(
                    sorted[right] - sorted[left], maxDistance) > 0) {
                left++;
            }
            count += right - left;
        }
        return count;
    }

    private static void validatePairDistances(long[] thresholds) {
        // Verify the full batch before allocating or mutating result storage.
        for (long distance : thresholds) {
            if (distance < 0L) {
                throw new IllegalArgumentException("distance threshold must be nonnegative");
            }
        }
    }

    //FLLDCIM: MRL - census P2-073, crate m3jdk21-lc315-count-smaller-20261026 (donor blob 7871ab51e2ec): LC 315 count smaller to the right (merge inversion + optional JNI)
    /**
     * LC #315: strictly smaller values to the RIGHT of each original index.
     * Uses stable primitive-index merge inversion counts; duplicate values do
     * not contribute. The input long[] remains entirely untouched.
     *
     * <p>One JNI transition covers an eligible batch, without Java callbacks
     * or per-entry objects. Optional JNI dispatch falls back only for an
     * unavailable ABI; a native-required call must never silently downgrade.
     */
    public static int[] countSmallerRight(long[] values) {
        Objects.requireNonNull(values, "values");
        int[] result = new int[values.length];
        if (values.length < 2) return result;
        if (AVAILABLE && !COUNT_SMALLER_ABI_MISSING && values.length >= NATIVE_MIN_BATCH) {
            try {
                countSmallerRight0(values, result);
                return result;
            } catch (UnsatisfiedLinkError olderNativeAbi) {
                COUNT_SMALLER_ABI_MISSING = true;
            }
        }
        countSmallerRightJavaInto(values, result);
        return result;
    }

    /** Proof-only mandatory JNI variant; fails closed if no current native ABI. */
    static int[] countSmallerRightNativeRequired(long[] values) {
        Objects.requireNonNull(values, "values");
        if (!AVAILABLE) throw new UnsatisfiedLinkError("m3rankbatch count-smaller JNI unavailable");
        int[] result = new int[values.length];
        countSmallerRight0(values, result);
        return result;
    }

    /** Independent Java 21 oracle for all native and ABI-compatibility tests. */
    static int[] countSmallerRightJava(long[] values) {
        Objects.requireNonNull(values, "values");
        int[] result = new int[values.length];
        countSmallerRightJavaInto(values, result);
        return result;
    }

    private static void countSmallerRightJavaInto(long[] values, int[] result) {
        int n = values.length;
        if (n < 2) return;
        int[] indices = new int[n];
        int[] scratch = new int[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        int[] from = indices, to = scratch;
        // long widths prevent an overflowing int doubling at maximal lengths.
        for (long width = 1; width < n; width <<= 1) {
            for (long base = 0; base < n; base += width * 2) {
                int middle = (int) Math.min(n, base + width);
                int end = (int) Math.min(n, base + width * 2);
                int left = (int) base, right = middle, write = (int) base;
                int rightMoved = 0;
                while (left < middle && right < end) {
                    int originalLeft = from[left];
                    int originalRight = from[right];
                    if (values[originalLeft] <= values[originalRight]) {
                        result[originalLeft] += rightMoved;
                        to[write++] = originalLeft;
                        left++;
                    } else {
                        to[write++] = originalRight;
                        right++;
                        rightMoved++;
                    }
                }
                while (left < middle) {
                    int originalLeft = from[left++];
                    result[originalLeft] += rightMoved;
                    to[write++] = originalLeft;
                }
                while (right < end) to[write++] = from[right++];
            }
            int[] swap = from; from = to; to = swap;
        }
    }

    /** Appended primitive JNI ABI in the existing original m3rankbatch owner. */
    private static native void countSmallerRight0(long[] input, int[] output);

    //FLLDCIM: MRL - census P2-073, crate m3jdk21-lc719-kth-pair-distance-20261026 (donor blob 022cefecdee7): LC 719 k-th smallest pair distance (bisection over the pair counts + optional JNI)
    // LeetCode #719: smallest distance pair, lifted to signed-long M3 lanes.
    // All coordinates are physical positions in a private sorted snapshot.
    private static final int NATIVE_MIN_KTH_PAIR_SIZE = 256;
    private static final int NATIVE_MIN_KTH_PAIR_BATCH = 8;
    private static volatile boolean KTH_PAIR_ABI_MISSING;

    /** One-based selection; reject a mathematical distance > Long.MAX_VALUE. */
    static long kthPairDistance(long[] sorted, long rank) {
        Objects.requireNonNull(sorted, "sorted");
        long total = (long) sorted.length * (sorted.length - 1) / 2;
        if (rank < 1 || rank > total) {
            throw new IndexOutOfBoundsException("pair rank is outside [1, n*(n-1)/2]");
        }
        long representable = pairDistanceCountJava(sorted, Long.MAX_VALUE);
        if (rank > representable) {
            throw new ArithmeticException("selected pair distance exceeds Long.MAX_VALUE");
        }
        if (AVAILABLE && !KTH_PAIR_ABI_MISSING
                && sorted.length >= NATIVE_MIN_KTH_PAIR_SIZE) {
            long[] target = {rank};
            long[] output = new long[1];
            try {
                kthPairDistancesInto0(sorted, target, 0, 1, output, 0);
                return output[0];
            } catch (UnsatisfiedLinkError olderNativeLibrary) {
                KTH_PAIR_ABI_MISSING = true;
            }
        }
        return kthPairDistanceJava(sorted, rank);
    }

    /** One-based LeetCode #719 query vector; no result-boxing or payload copy. */
    static long[] kthPairDistances(long[] sorted, long[] ranks) {
        Objects.requireNonNull(ranks, "ranks");
        long[] output = new long[ranks.length];
        kthPairDistancesInto(sorted, ranks, 0, ranks.length, output, 0, false);
        return output;
    }

    /** Explicit JNI tests may require the new native export without fallback. */
    static long[] kthPairDistancesNativeRequired(long[] sorted, long[] ranks) {
        Objects.requireNonNull(ranks, "ranks");
        long[] output = new long[ranks.length];
        kthPairDistancesInto(sorted, ranks, 0, ranks.length, output, 0, true);
        return output;
    }

    /**
     * Validates the entire rank window before writing any result. Rejects
     * aliases to source or rank arrays, including overlap with a caller's
     * output range. The original m3rankbatch JNI owner uses bounded pages.
     */
    static void kthPairDistancesInto(long[] sorted, long[] ranks, int rankFrom,
            int length, long[] output, int outputFrom, boolean nativeRequired) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(ranks, "ranks");
        Objects.requireNonNull(output, "output");
        Objects.checkFromIndexSize(rankFrom, length, ranks.length);
        Objects.checkFromIndexSize(outputFrom, length, output.length);
        if (output == sorted || output == ranks) {
            throw new IllegalArgumentException("pair-selection output aliases input");
        }
        long total = (long) sorted.length * (sorted.length - 1) / 2;
        for (int i = 0; i < length; i++) {
            long rank = ranks[rankFrom + i];
            if (rank < 1 || rank > total) {
                throw new IndexOutOfBoundsException("pair rank is outside [1, n*(n-1)/2]");
            }
        }
        // A signed long cannot encode every distance across the complete
        // Long.MIN_VALUE..Long.MAX_VALUE domain. Check before ANY write.
        if (length != 0) {
            long representable = pairDistanceCountJava(sorted, Long.MAX_VALUE);
            for (int i = 0; i < length; i++) {
                if (ranks[rankFrom + i] > representable) {
                    throw new ArithmeticException("selected pair distance exceeds Long.MAX_VALUE");
                }
            }
        }
        if (nativeRequired) {
            if (!AVAILABLE) {
                throw new UnsatisfiedLinkError("m3rankbatch k-th pair distance ABI unavailable");
            }
            if (length != 0) {
                kthPairDistancesInto0(sorted, ranks, rankFrom, length, output, outputFrom);
            }
            return;
        }
        if (AVAILABLE && !KTH_PAIR_ABI_MISSING
                && sorted.length >= NATIVE_MIN_KTH_PAIR_SIZE
                && length >= NATIVE_MIN_KTH_PAIR_BATCH) {
            try {
                kthPairDistancesInto0(sorted, ranks, rankFrom, length, output, outputFrom);
                return;
            } catch (UnsatisfiedLinkError olderNativeLibrary) {
                KTH_PAIR_ABI_MISSING = true;
            }
        }
        for (int i = 0; i < length; i++) {
            output[outputFrom + i] = kthPairDistanceJava(sorted, ranks[rankFrom + i]);
        }
    }

    /** Java reference kernel: monotone two-pointer count and 63-bit bisection. */
    private static long kthPairDistanceJava(long[] sorted, long rank) {
        int n = sorted.length;
        long span = sorted[n - 1] - sorted[0];
        long high = Long.compareUnsigned(span, Long.MAX_VALUE) > 0
                ? Long.MAX_VALUE : span;
        long low = 0L;
        while (low < high) {
            long midpoint = low + ((high - low) >>> 1);
            if (pairDistanceCountJava(sorted, midpoint) >= rank) {
                high = midpoint;
            } else {
                low = midpoint + 1;
            }
        }
        return low;
    }

    /** Existing m3rankbatch shared JNI library; no new native module. */
    private static native void kthPairDistancesInto0(long[] sorted, long[] ranks,
            int rankFrom, int length, long[] output, int outputFrom);

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
    //FLLDCIM: MRL - census P2-073, crate m3jdk21-lc719-pair-distance-java-20261026 (donor blob 83ce0911f53f): LC 719 pair-distance JNI entry
    /** Additive LC #719 JNI batch predicate over signed sorted long lanes. */
    static native void pairDistanceCounts0(long[] sorted, long[] thresholds, long[] results);

    /** Additive LC #347 source-owned m3rankbatch JNI symbol. */
    private static native long[] topKFrequent0(long[] sorted, int k);


    private static native void lowerBounds0(long[] sorted, long[] queries, int[] results);

    /** Capability bit 0 signals the fused signed-long pair operation. */
    private static native int capabilities0();

    private static native void lowerUpperBounds0(long[] sorted, long[] queries, int[] pairs);
}
