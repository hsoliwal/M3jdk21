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
    private static final int NATIVE_MIN_KDIFF = 256;
    private static volatile boolean KDIFF_ABI_MISSING;
    private static final int NATIVE_MIN_PAIR_DISTANCE_BATCH = 64;
    private static final int NATIVE_MIN_PAIR_DISTANCE_OWNER = 128;
    private static volatile boolean PAIR_DISTANCE_ABI_MISSING;
    private static final int NATIVE_MIN_INTERSECTION = 128;
    private static final int MODE_UPPER = 1;
    private static final int MODE_COUNT = 2;
    private static volatile boolean WINDOW_ABI_MISSING;
    private static volatile boolean EQUAL_RANGES_WINDOW_ABI_MISSING;
    private static volatile boolean INTERSECTION_VALUES_ABI_MISSING;
    private static volatile boolean UNION_VALUES_ABI_MISSING;
    private static volatile boolean RUN_STATISTICS_ABI_MISSING;
    private static final int NATIVE_MIN_RUN_STATISTICS = 256;
    private static final int NATIVE_MIN_FREQUENCY_TOP_K = 256;
    private static volatile boolean TOP_K_FREQUENCY_ABI_MISSING;
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
        equalRangesInto(sorted, queries, 0, queries.length, ranges, 0, false);
        return ranges;
    }

    /**
     * LeetCode #34/#35 insertion boundaries in caller-owned primitive storage.
     * Offset is measured in <em>int slots</em>, with two slots per query.
     * Validation happens before any result write; caller-owned padding survives.
     * The old equalRanges0 JNI symbol remains exported for older consumers.
     * Optional new native ABI failures fall back to exactly the Java oracle.
     */
    static void equalRangesInto(long[] sorted, long[] queries, int queryFrom,
            int length, int[] ranges, int rangeFrom) {
        equalRangesInto(sorted, queries, queryFrom, length, ranges, rangeFrom, false);
    }

    /** Explicit native requests never silently fall back to Java or an old ABI. */
    static void equalRangesInto(long[] sorted, long[] queries, int queryFrom,
            int length, int[] ranges, int rangeFrom, boolean nativeRequired) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.requireNonNull(ranges, "ranges");
        Objects.checkFromIndexSize(queryFrom, length, queries.length);
        int outputLength = Math.multiplyExact(length, 2);
        Objects.checkFromIndexSize(rangeFrom, outputLength, ranges.length);
        if (nativeRequired) {
            if (!AVAILABLE) {
                throw new UnsatisfiedLinkError("m3rankbatch equal-range-window JNI unavailable");
            }
            equalRangesWindow0(sorted, queries, queryFrom, length, ranges, rangeFrom);
            return;
        }
        if (length == 0) return;
        if (sorted.length == 0) {
            Arrays.fill(ranges, rangeFrom, rangeFrom + outputLength, 0);
            return;
        }
        if (AVAILABLE && !EQUAL_RANGES_WINDOW_ABI_MISSING
                && length >= NATIVE_MIN_BATCH) {
            try {
                equalRangesWindow0(sorted, queries, queryFrom, length, ranges, rangeFrom);
                return;
            } catch (UnsatisfiedLinkError olderNativeLibrary) {
                // Existing m3rankbatch deployments may not export this additive ABI.
                EQUAL_RANGES_WINDOW_ABI_MISSING = true;
            }
        }
        for (int i = 0; i < length; i++) {
            long key = queries[queryFrom + i];
            int lo = 0, hi = sorted.length;
            while (lo < hi) {
                int mid = lo + ((hi - lo) >>> 1);
                if (sorted[mid] < key) lo = mid + 1;
                else hi = mid;
            }
            int first = lo;
            hi = sorted.length;
            while (lo < hi) {
                int mid = lo + ((hi - lo) >>> 1);
                if (sorted[mid] <= key) lo = mid + 1;
                else hi = mid;
            }
            int pair = rangeFrom + (i << 1);
            ranges[pair] = first;
            ranges[pair + 1] = lo;
        }
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

    /**
     * LeetCode #532 category: number of distinct unordered signed-long value
     * pairs at a nonnegative absolute distance. This operates on an already
     * sorted immutable owner; equal physical occurrences are NOT separately
     * counted. At distance zero, a value needs at least two occurrences.
     *
     * <p>The two-cursor Java oracle is O(n), O(1) memory, and checks the
     * unsigned difference to avoid overflow at the signed long extremes.
     * For large owners the existing m3rankbatch JNI provider uses two fixed
     * 256-long GetLongArrayRegion pages; no callback, per-entry object, heap
     * payload copy or retained Java pointer crosses the native boundary.
     * Older optional native libraries fall back to this same Java oracle.</p>
     */
    static long distinctDifferencePairCount(long[] sorted, long distance) {
        Objects.requireNonNull(sorted, "sorted");
        if (distance < 0) throw new IllegalArgumentException("distance must be nonnegative");
        if (sorted.length < 2) return 0L;
        if (AVAILABLE && !KDIFF_ABI_MISSING && sorted.length >= NATIVE_MIN_KDIFF) {
            try {
                return distinctDifferencePairCount0(sorted, distance);
            } catch (UnsatisfiedLinkError oldNative) {
                KDIFF_ABI_MISSING = true;
            }
        }
        return distinctDifferencePairCountJava(sorted, distance);
    }

    /** Explicit native qualification NEVER masks an unavailable ABI. */
    static long distinctDifferencePairCountNativeRequired(long[] sorted, long distance) {
        Objects.requireNonNull(sorted, "sorted");
        if (distance < 0) throw new IllegalArgumentException("distance must be nonnegative");
        if (!AVAILABLE) {
            throw new UnsatisfiedLinkError("m3rankbatch distinct K-diff ABI unavailable");
        }
        return distinctDifferencePairCount0(sorted, distance);
    }

    /** First-party two-pointer scalar oracle for signed long[] snapshots. */
    static long distinctDifferencePairCountJava(long[] sorted, long distance) {
        Objects.requireNonNull(sorted, "sorted");
        if (distance < 0) throw new IllegalArgumentException("distance must be nonnegative");
        int n = sorted.length;
        if (n < 2) return 0L;
        long total = 0;
        if (distance == 0) {
            for (int i = 1; i < n;) {
                if (sorted[i] == sorted[i - 1]) {
                    total++;
                    long current = sorted[i];
                    do { i++; } while (i < n && sorted[i] == current);
                } else {
                    i++;
                }
            }
            return total;
        }
        int left = 0, right = 1;
        while (left < n && right < n) {
            if (right <= left) {
                right = left + 1;
                continue;
            }
            // For signed sorted values right>=left: unsigned subtraction is
            // their full mathematical distance, including >Long.MAX_VALUE.
            int order = Long.compareUnsigned(sorted[right] - sorted[left], distance);
            if (order < 0) {
                right++;
            } else {
                if (order == 0) total++;
                long previous = sorted[left];
                do { left++; } while (left < n && sorted[left] == previous);
            }
        }
        return total;
    }

    /**
     * LeetCode #719 count-within-distance predicate over an immutable, sorted
     * signed-long lane. Each i < j occurrence is counted independently. Input
     * thresholds are nonnegative signed longs; full mathematical differences
     * spanning Long.MIN_VALUE..Long.MAX_VALUE use unsigned subtraction.
     *
     * <p>JNI batches sufficiently large inputs in the original m3rankbatch
     * native owner using fixed array-region pages, no Java callback and no
     * retained GC address. Older optional native binaries fall back to Java;
     * native-required requests never hide a missing ABI.</p>
     */
    static long[] pairDistanceCounts(long[] sorted, long[] thresholds) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(thresholds, "thresholds");
        validatePairDistances(thresholds);
        long[] result = new long[thresholds.length];
        if (sorted.length < 2 || thresholds.length == 0) return result;
        if (AVAILABLE && !PAIR_DISTANCE_ABI_MISSING
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

    /** Additive LC #719 JNI batch predicate over signed sorted long lanes. */
    static native void pairDistanceCounts0(long[] sorted, long[] thresholds, long[] results);

    /** Additive LC #347 source-owned m3rankbatch JNI symbol. */
    private static native long[] topKFrequent0(long[] sorted, int k);

    /** Same existing m3rankbatch JNI library: bounded native run-statistic scan. */
    private static native long runStatistics0(long[] sorted);

    /** Additive primitive-only JNI ABI for the LC #532 pair-count category. */
    private static native long distinctDifferencePairCount0(long[] sorted, long distance);

    private static native void lowerBounds0(long[] sorted, long[] queries, int[] results);

    private static native void equalRanges0(long[] sorted, long[] queries, int[] ranges);

    /** Additive same-library ABI: bounded 256-key pages and no full-array JNI copies. */
    private static native void equalRangesWindow0(long[] sorted, long[] queries,
            int queryFrom, int length, int[] ranges, int rangeFrom);

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
