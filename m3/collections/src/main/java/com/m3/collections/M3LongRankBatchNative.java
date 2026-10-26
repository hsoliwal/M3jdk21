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
    private static volatile boolean WINDOW_ABI_MISSING;
    private static volatile boolean INTERSECTION_VALUES_ABI_MISSING;
    private static volatile boolean UNION_VALUES_ABI_MISSING;
    private static volatile boolean RUN_STATISTICS_ABI_MISSING;
    private static final int NATIVE_MIN_RUN_STATISTICS = 256;
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

    /**
     * LeetCode #349 / #350 problem category: intersect sorted, signed-long
     * physical windows using two primitive cursors. A distinct intersection
     * counts each matched value once; multiset mode counts min frequencies.
     *
     * <p>No array slice, entry object, sort, callback, or JNI pointer is
     * retained. Both sources must be nondecreasing and remain immutable for
     * this call. Unlike a JDK Set, duplicate physical positions remain valid.
     * The native path is optional for sufficiently large windows; old native
     * binaries lacking this additive symbol use the exact Java oracle.
     */
    static long intersectionWindowCount(long[] left, int leftFrom, int leftLength,
                                        long[] right, int rightFrom, int rightLength,
                                        boolean distinct) {
        return intersectionWindowCount(left, leftFrom, leftLength, right, rightFrom,
                                       rightLength, distinct, false);
    }

    /** Explicit JNI requests fail closed if the native ABI is unavailable. */
    static long intersectionWindowCount(long[] left, int leftFrom, int leftLength,
                                        long[] right, int rightFrom, int rightLength,
                                        boolean distinct, boolean nativeRequired) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.checkFromIndexSize(leftFrom, leftLength, left.length);
        Objects.checkFromIndexSize(rightFrom, rightLength, right.length);
        if (nativeRequired) {
            if (!AVAILABLE) throw new UnsatisfiedLinkError("m3rankbatch window ABI unavailable");
            return intersectionWindowCount0(left, leftFrom, leftLength,
                    right, rightFrom, rightLength, distinct);
        }
        if (leftLength == 0 || rightLength == 0) return 0L;
        if (AVAILABLE && !WINDOW_ABI_MISSING
                && Math.min(leftLength, rightLength) >= NATIVE_MIN_INTERSECTION) {
            try {
                return intersectionWindowCount0(left, leftFrom, leftLength,
                        right, rightFrom, rightLength, distinct);
            } catch (UnsatisfiedLinkError olderNativeLibrary) {
                // Legacy m3rankbatch continues serving its original entrypoints.
                WINDOW_ABI_MISSING = true;
            }
        }
        int i = leftFrom, j = rightFrom;
        int leftEnd = leftFrom + leftLength;
        int rightEnd = rightFrom + rightLength;
        long count = 0L, last = 0L;
        boolean matched = false;
        while (i < leftEnd && j < rightEnd) {
            long a = left[i], b = right[j];
            if (a < b) i++;
            else if (a > b) j++;
            else {
                if (!distinct || !matched || last != a) {
                    count++;
                    last = a;
                    matched = true;
                }
                i++; j++;
            }
        }
        return count;
    }

    /**
     * LeetCode #349/#350 categories: detached sorted intersection values over
     * two immutable primitive lanes. In distinct mode each shared value appears
     * once; otherwise min(freq(left), freq(right)) occurrences are retained.
     * No per-element object, callback, retained JNI pointer or payload sort.
     *
     * <p>Existing native libraries without the new symbol safely use the Java
     * two-pointer oracle. Native-required calls never hide a missing symbol.</p>
     */
    static long[] intersectionValues(long[] left, long[] right, boolean distinct) {
        return intersectionValues(left, right, distinct, false);
    }

    static long[] intersectionValues(long[] left, long[] right,
                                     boolean distinct, boolean nativeRequired) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (nativeRequired) {
            if (!AVAILABLE) throw new UnsatisfiedLinkError("m3rankbatch intersection-value ABI unavailable");
            return intersectionValues0(left, right, distinct);
        }
        if (left.length == 0 || right.length == 0) return new long[0];
        if (AVAILABLE && !INTERSECTION_VALUES_ABI_MISSING
                && Math.min(left.length, right.length) >= NATIVE_MIN_INTERSECTION) {
            try {
                return intersectionValues0(left, right, distinct);
            } catch (UnsatisfiedLinkError olderNativeLibrary) {
                INTERSECTION_VALUES_ABI_MISSING = true;
            }
        }
        int count = intersectionValuesJavaScan(left, right, distinct, null);
        long[] result = new long[count];
        if (intersectionValuesJavaScan(left, right, distinct, result) != count) {
            throw new IllegalStateException("sorted intersection changed between count and emission");
        }
        return result;
    }

    private static int intersectionValuesJavaScan(long[] left, long[] right,
                                                   boolean distinct, long[] result) {
        int i = 0, j = 0, write = 0;
        long previous = 0L;
        boolean seen = false;
        while (i < left.length && j < right.length) {
            long a = left[i], b = right[j];
            if (a < b) i++;
            else if (a > b) j++;
            else {
                if (!distinct || !seen || previous != a) {
                    if (result != null) {
                        if (write >= result.length) {
                            throw new IllegalStateException(
                                    "sorted intersection grew between count and emission");
                        }
                        result[write] = a;
                    }
                    write++;
                    previous = a;
                    seen = true;
                }
                i++; j++;
            }
        }
        return write;
    }


    /**
     * LeetCode #88 sorted-merge and #26 distinct-run algorithm categories:
     * detached primitive multiset union (maximum occurrence multiplicity).
     * Optional distinct mode emits every shared or unshared value exactly once.
     *
     * <p>Both source lanes are already-sorted immutable snapshot payloads.
     * The JNI batch reuses m3rankbatch and bounded array-region pages. No
     * comparator, boxed Long, Map.Entry, heap pointer or JNI callback escapes.
     * An old native library without this additive symbol falls back to Java;
     * an explicit required-native invocation refuses the missing export.</p>
     */
    static long[] unionValues(long[] left, long[] right, boolean distinct) {
        return unionValues(left, right, distinct, false);
    }

    static long[] unionValues(long[] left, long[] right,
                              boolean distinct, boolean nativeRequired) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (nativeRequired) {
            if (!AVAILABLE) {
                throw new UnsatisfiedLinkError("m3rankbatch union-value ABI unavailable");
            }
            return unionValues0(left, right, distinct);
        }
        if (AVAILABLE && !UNION_VALUES_ABI_MISSING
                && Math.min(left.length, right.length) >= NATIVE_MIN_INTERSECTION) {
            try {
                return unionValues0(left, right, distinct);
            } catch (UnsatisfiedLinkError olderNativeLibrary) {
                UNION_VALUES_ABI_MISSING = true;
            }
        }
        long count = unionValuesJavaScan(left, right, distinct, null);
        if (count > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("sorted union exceeds Java array size");
        }
        long[] result = new long[(int) count];
        if (unionValuesJavaScan(left, right, distinct, result) != count) {
            throw new IllegalStateException("sorted union changed between count and emission");
        }
        return result;
    }

    private static long unionValuesJavaScan(long[] left, long[] right,
                                            boolean distinct, long[] output) {
        int i = 0, j = 0;
        long written = 0L, previous = 0L;
        boolean seen = false;
        while (i < left.length || j < right.length) {
            long next;
            if (i == left.length) {
                next = right[j++];
            } else if (j == right.length) {
                next = left[i++];
            } else {
                long a = left[i], b = right[j];
                if (a < b) {
                    next = a;
                    i++;
                } else if (a > b) {
                    next = b;
                    j++;
                } else {
                    next = a;
                    i++;
                    j++;
                }
            }
            if (!distinct || !seen || previous != next) {
                if (output != null) {
                    if (written >= output.length) {
                        throw new IllegalStateException(
                                "sorted union grew between count and emission");
                    }
                    output[(int) written] = next;
                }
                written++;
                previous = next;
                seen = true;
            }
        }
        return written;
    }

    private static volatile boolean DIFFERENCE_VALUES_ABI_MISSING;

    /**
     * LeetCode #2215 and #350: original primitive sorted set/multiset difference.
     * Distinct mode includes a key only if it is absent from the other owner;
     * occurrence mode subtracts multiplicities. Optional JNI reuses m3rankbatch.
     */
    static long[] differenceValues(long[] left, long[] right, boolean distinct) {
        return differenceValues(left, right, distinct, false, false);
    }

    /** Sorted symmetric difference: set XOR or absolute frequency difference. */
    static long[] symmetricDifferenceValues(long[] left, long[] right, boolean distinct) {
        return differenceValues(left, right, distinct, true, false);
    }

    /**
     * Package-local strict JNI test seam. Explicit native requests never fall back
     * to Java when a legacy native image lacks the additive entry point.
     */
    static long[] differenceValues(long[] left, long[] right, boolean distinct,
                                   boolean symmetric, boolean nativeRequired) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (nativeRequired) {
            if (!AVAILABLE) throw new UnsatisfiedLinkError("m3rankbatch difference ABI unavailable");
            return differenceValues0(left, right, distinct, symmetric);
        }
        if (AVAILABLE && !DIFFERENCE_VALUES_ABI_MISSING
                && Math.min(left.length, right.length) >= NATIVE_MIN_INTERSECTION) {
            try {
                return differenceValues0(left, right, distinct, symmetric);
            } catch (UnsatisfiedLinkError oldLibrary) {
                DIFFERENCE_VALUES_ABI_MISSING = true;
            }
        }
        long size = differenceJavaScan(left, right, distinct, symmetric, null);
        if (size > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("sorted difference exceeds Java array size");
        }
        long[] output = new long[(int) size];
        if (differenceJavaScan(left, right, distinct, symmetric, output) != size) {
            throw new IllegalStateException("sorted difference changed between counting and emission");
        }
        return output;
    }

    private static long differenceJavaScan(long[] left, long[] right,
                                           boolean distinct, boolean symmetric, long[] result) {
        int i = 0, j = 0;
        long written = 0;
        while (i < left.length || j < right.length) {
            long key = i == left.length ? right[j]
                    : j == right.length ? left[i] : Math.min(left[i], right[j]);
            long leftCount = 0, rightCount = 0;
            while (i < left.length && left[i] == key) { i++; leftCount++; }
            while (j < right.length && right[j] == key) { j++; rightCount++; }
            long copies;
            if (distinct) {
                copies = symmetric ? ((leftCount > 0) != (rightCount > 0) ? 1 : 0)
                        : leftCount > 0 && rightCount == 0 ? 1 : 0;
            } else if (symmetric) {
                copies = Math.abs(leftCount - rightCount);
            } else {
                copies = Math.max(0L, leftCount - rightCount);
            }
            if (copies > Integer.MAX_VALUE - written) {
                throw new OutOfMemoryError("sorted difference exceeds Java array size");
            }
            if (result != null) {
                if (written + copies > result.length) {
                    throw new IllegalStateException("sorted difference grew between counting and emission");
                }
                Arrays.fill(result, (int) written, (int) (written + copies), key);
            }
            written += copies;
        }
        return written;
    }

    /**
     * LeetCode #26/#128 categories: distinct cardinality and longest run of
     * consecutive signed-long values in an immutable sorted primitive lane.
     * Packed result: high 32 bits = distinct count, low 32 bits = run length.
     * No source mutation, Map.Entry, object per row or JNI callback.
     *
     * <p>JNI scans fixed 256-long array-region pages in one native transition.
     * An older optional m3rankbatch binary without the additive entrypoint
     * uses the same Java oracle; an explicit native requirement fails closed.
     */
    static long runStatistics(long[] sorted) {
        Objects.requireNonNull(sorted, "sorted");
        if (sorted.length == 0) return 0L;
        if (AVAILABLE && !RUN_STATISTICS_ABI_MISSING
                && sorted.length >= NATIVE_MIN_RUN_STATISTICS) {
            try {
                return runStatistics0(sorted);
            } catch (UnsatisfiedLinkError oldNativeAbi) {
                RUN_STATISTICS_ABI_MISSING = true;
            }
        }
        return runStatisticsJava(sorted);
    }

    /** Strict JNI parity entrypoint: never downgrade a requested native call. */
    static long runStatisticsNativeRequired(long[] sorted) {
        Objects.requireNonNull(sorted, "sorted");
        if (!AVAILABLE) {
            throw new UnsatisfiedLinkError("m3rankbatch run statistics JNI unavailable");
        }
        return runStatistics0(sorted);
    }

    static long runStatisticsJava(long[] sorted) {
        Objects.requireNonNull(sorted, "sorted");
        if (sorted.length == 0) return 0L;
        int distinct = 1, current = 1, best = 1;
        long previous = sorted[0];
        for (int i = 1; i < sorted.length; i++) {
            long value = sorted[i];
            if (value == previous) continue;
            distinct++;
            // Signed increasing order excludes a Long.MAX_VALUE successor.
            current = value > previous && value - previous == 1L ? current + 1 : 1;
            if (current > best) best = current;
            previous = value;
        }
        return ((long) distinct << 32) | (best & 0xffffffffL);
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

    /** Same existing m3rankbatch JNI library: bounded native run-statistic scan. */
    private static native long runStatistics0(long[] sorted);

    private static native void lowerBounds0(long[] sorted, long[] queries, int[] results);

    private static native void equalRanges0(long[] sorted, long[] queries, int[] ranges);

    private static native void boundBatchInto0(long[] sorted, long[] queries, int queryFrom,
            int length, int[] output, int outputFrom, int mode);

    private static native void rangeCountInto0(long[] sorted, long[] fromInclusive,
            long[] toExclusive, int queryFrom, int length, int[] output, int outputFrom);

    private static native long intersectionCount0(long[] left, long[] right);

    /** Package-scope ABI for exact JNI contract tests; not a public collection API. */
    static native long intersectionWindowCount0(long[] left, int leftFrom,
            int leftLength, long[] right, int rightFrom, int rightLength,
            boolean distinct);

    /** Additive same-library JNI ABI: count and emit values in 256-long pages. */
    static native long[] intersectionValues0(long[] left, long[] right, boolean distinct);

    /** Additive same-library JNI ABI for sorted multiset union values. */
    static native long[] unionValues0(long[] left, long[] right, boolean distinct);

    /** Additive same-library JNI ABI: bounded pages, no per-Entry allocation. */
    static native long[] differenceValues0(long[] left, long[] right,
                                          boolean distinct, boolean symmetric);
}
