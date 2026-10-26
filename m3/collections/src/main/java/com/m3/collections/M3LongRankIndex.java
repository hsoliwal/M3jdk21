// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

/**
 * Immutable, signed-long, duplicate-aware order-statistics snapshot.
 *
 * <p>Capture is one encounter-order traversal of an {@link M3LongCollection} without
 * consulting its size or calling its toArray method. The resulting primitive array is
 * sorted once, privately owned, and never exposed to callers. Unlike M3's backed filtered
 * and sorted views, subsequent source mutations do not change this index. A predicate
 * must not mutate the source while it is being traversed.</p>
 *
 * <p>Build is O(n log n) worst case with Java's primitive sort. Lower/upper bound,
 * membership, frequency, and value-range counts are O(log n); select is O(1).
 * Duplicate occurrences are retained unless distinct capture is requested. None of
 * the operations allocates a per-value Long or Map.Entry.</p>
 */
public final class M3LongRankIndex {
    private static final long[] EMPTY = new long[0];
    private static final int INITIAL_CAPACITY = 16;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private final long[] values;
    private final boolean distinct;
    // Immutable snapshot fact sidecar. Benign concurrent first reads may
    // recompute, but volatile publication makes subsequent reads allocation-free.
    // Nonnegative packing permits -1L as an unequivocal UNCOMPUTED sentinel.
    private volatile long cachedRunStatistics = -1L;

    private M3LongRankIndex(long[] owned, boolean distinct) {
        values = owned;
        this.distinct = distinct;
    }

    /** Captures all occurrences, including duplicates. */
    public static M3LongRankIndex snapshot(M3LongCollection source) {
        return capture(source, value -> true, false);
    }

    /** Captures matching occurrences, including duplicates. */
    public static M3LongRankIndex snapshotFiltered(
            M3LongCollection source, LongPredicate predicate) {
        return capture(source, predicate, false);
    }

    /** Captures each distinct value once, for dense ranks and sorted-set projections. */
    public static M3LongRankIndex snapshotDistinct(M3LongCollection source) {
        return capture(source, value -> true, true);
    }

    /** Captures distinct matching values in signed order. */
    public static M3LongRankIndex snapshotFilteredDistinct(
            M3LongCollection source, LongPredicate predicate) {
        return capture(source, predicate, true);
    }

    /** Copies an arbitrary source array; caller mutations cannot alter the snapshot. */
    public static M3LongRankIndex copyOf(long[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) {
            return new M3LongRankIndex(EMPTY, false);
        }
        long[] sorted = source.clone();
        Arrays.sort(sorted);
        return new M3LongRankIndex(sorted, false);
    }

    private static M3LongRankIndex capture(
            M3LongCollection source, LongPredicate predicate, boolean distinct) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        M3LongIterator cursor = source.iterator();
        long[] buffer = EMPTY;
        int count = 0;
        while (cursor.hasNext()) {
            long value = cursor.nextLong();
            if (!predicate.test(value)) {
                continue;
            }
            if (count == buffer.length) {
                buffer = Arrays.copyOf(buffer, nextCapacity(buffer.length));
            }
            buffer[count++] = value;
        }
        if (count == 0) {
            return new M3LongRankIndex(EMPTY, distinct);
        }
        Arrays.sort(buffer, 0, count);
        if (distinct) {
            int write = 1;
            for (int read = 1; read < count; read++) {
                if (buffer[read] != buffer[write - 1]) {
                    buffer[write++] = buffer[read];
                }
            }
            count = write;
        }
        return new M3LongRankIndex(
                count == buffer.length ? buffer : Arrays.copyOf(buffer, count), distinct);
    }

    private static int nextCapacity(int previous) {
        if (previous == 0) {
            return INITIAL_CAPACITY;
        }
        if (previous >= MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("M3LongRankIndex exceeds array size limit");
        }
        long grown = (long) previous + (previous >>> 1) + 1L;
        return (int) Math.min(grown, MAX_ARRAY_SIZE);
    }

    public int size() {
        return values.length;
    }

    public boolean isEmpty() {
        return values.length == 0;
    }

    public boolean isDistinct() {
        return distinct;
    }

    /** First index of a value greater than or equal to target; in [0, size]. */
    public int lowerBound(long target) {
        int lo = 0;
        int hi = values.length;
        return M3LaneSearch.lowerBound(values, lo, hi, target);
    }

    /** First index of a value strictly greater than target; in [0, size]. */
    public int upperBound(long target) {
        int lo = 0;
        int hi = values.length;
        return M3LaneSearch.upperBound(values, lo, hi, target);
    }

    /** Zero-based positional select in signed sorted order, duplicates included. */
    public long select(int index) {
        return values[Objects.checkIndex(index, values.length)];
    }

    public boolean contains(long target) {
        int i = lowerBound(target);
        return i < values.length && values[i] == target;
    }

    public int count(long target) {
        return upperBound(target) - lowerBound(target);
    }

    /** Number of values below target, counting duplicates. */
    public int rankLessThan(long target) {
        return lowerBound(target);
    }

    /** Number of values at or below target, counting duplicates. */
    public int rankLessOrEqual(long target) {
        return upperBound(target);
    }

    /** Counts all occurrences in [fromInclusive, toExclusive), with no +1 overflow. */
    public int rangeCount(long fromInclusive, long toExclusive) {
        if (fromInclusive > toExclusive) {
            throw new IllegalArgumentException("fromInclusive > toExclusive");
        }
        return lowerBound(toExclusive) - lowerBound(fromInclusive);
    }

    /** Counts all occurrences in [fromInclusive, toInclusive]. */
    public int rangeCountInclusive(long fromInclusive, long toInclusive) {
        if (fromInclusive > toInclusive) {
            throw new IllegalArgumentException("fromInclusive > toInclusive");
        }
        return upperBound(toInclusive) - lowerBound(fromInclusive);
    }

    /**
     * Dense descending rank: 1 + distinct values strictly above the query.
     * Requires a distinct snapshot; duplicate-aware snapshots retain frequency instead.
     */
    public int denseRankDescending(long value) {
        if (!distinct) {
            throw new IllegalStateException("dense ranking requires distinct snapshot");
        }
        return 1 + values.length - upperBound(value);
    }

    /**
     * LeetCode #315 primitive count-smaller-after-self on original encounter order.
     * Unlike this type's sorted snapshots, input here may be completely unordered.
     * The first-party batch kernel uses primitive physical indexes and has no
     * per-element entries or caller-visible payload mutation.
     */
    public static int[] countSmallerAfterSelf(long[] encounterOrder) {
        return M3LongRankBatchNative.countSmallerRight(encounterOrder);
    }

    /** Java or explicitly enabled JNI batched lower bounds, without per-query boxing. */
    public int[] lowerBounds(long[] queries) {
        return M3LongRankBatchNative.lowerBounds(values, queries);
    }

    /**
     * LC #4 order-statistic category: select the zero-based rank from the
     * multiset merge of two immutable sorted M3 long snapshots. The original
     * long[] owners never move or materialize into a combined array.
     * Worst case O(log(rank + 1)) time, O(1) auxiliary space.
     */
    public long selectMerged(M3LongRankIndex other, long rank) {
        Objects.requireNonNull(other, "other");
        return M3LongRankBatchNative.selectMerged(values, other.values, rank);
    }

    /**
     * One detached result array for a batch of global zero-based ranks in the
     * sorted multiset union. Duplicates retain their occurrence positions.
     */
    public long[] selectMergedBatch(M3LongRankIndex other, long[] ranks) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(ranks, "ranks");
        long[] result = new long[ranks.length];
        M3LongRankBatchNative.selectMergedInto(values, other.values,
                ranks, 0, ranks.length, result, 0, false);
        return result;
    }

    /**
     * Write a checked rank window without an output allocation. The caller's
     * output must not alias ranks; all invalid ranks fail before the first write.
     */
    public void selectMergedInto(M3LongRankIndex other, long[] ranks,
            int rankFrom, int length, long[] output, int outputFrom) {
        Objects.requireNonNull(other, "other");
        M3LongRankBatchNative.selectMergedInto(values, other.values,
                ranks, rankFrom, length, output, outputFrom, false);
    }

    public long[] toArray() {
        return values.clone();
    }

    /**
     * Batched half-open [lower, upper) coordinates in a flat primitive int[].
     * Pair i resides at offsets 2*i and 2*i+1, including insertion coordinates
     * when a value is absent. Explicit JNI batches never retain any entry object
     * or invoke a caller comparator.
     */
    public int[] equalRanges(long[] queries) {
        return M3LongRankBatchNative.equalRanges(values, queries);
    }

    /**
     * Writes the batched half-open insertion endpoints into a caller-owned
     * primitive int[] window. Each query consumes two consecutive int slots
     * beginning at {@code outputFrom}; no per-query object or new result array
     * is allocated. Query and output windows are fully checked before writing.
     *
     * <p>The native LeetCode #34/#35/#88 kernel is optional, reuses the
     * existing m3rankbatch library, and releases all JNI array pins by page.
     */
    public void equalRangesInto(long[] queries, int queryFrom, int length,
                                int[] output, int outputFrom) {
        M3LongRankBatchNative.equalRangesInto(
                values, queries, queryFrom, length, output, outputFrom);
    }

    /**
     * Batched upper insertion coordinates for a signed-long query vector.
     * JNI is optional and handles large batches without user callbacks.
     */
    public int[] upperBounds(long[] queries) {
        return M3LongRankBatchNative.upperBounds(values, queries);
    }

    /**
     * Batched occurrence frequency for each query. Equivalent to
     * upperBound(query) - lowerBound(query) with no Long.MAX_VALUE arithmetic.
     */
    public int[] counts(long[] queries) {
        return M3LongRankBatchNative.counts(values, queries);
    }

    /**
     * Writes occurrence frequencies into a caller-owned int[] window.
     * Both windows are checked before any write; no boxed Entry is allocated.
     */
    public void countInto(long[] queries, int queryFrom, int length,
                          int[] output, int outputFrom) {
        M3LongRankBatchNative.countInto(values, queries, queryFrom, length, output, outputFrom);
    }

    /**
     * Primitive counts of values within parallel half-open signed-long intervals.
     *
     * <p>LeetCode #986 (interval boundaries) and #34/#35 (binary insertion
     * positions) are problem categories, not copied challenge code. Each
     * interval [fromInclusive[i], toExclusive[i]) is checked before any
     * result is written; duplicates count separately and no endpoint is
     * incremented. The owned sorted snapshot is never exposed or mutated.</p>
     */
    public int[] rangeCounts(long[] fromInclusive, long[] toExclusive) {
        return M3LongRankBatchNative.rangeCounts(values, fromInclusive, toExclusive);
    }

    /** Writes interval counts into one caller-owned primitive result window. */
    public void rangeCountInto(long[] fromInclusive, long[] toExclusive, int queryFrom,
                               int length, int[] output, int outputFrom) {
        M3LongRankBatchNative.rangeCountInto(values, fromInclusive, toExclusive,
                queryFrom, length, output, outputFrom);
    }

    /**
     * LeetCode #350 sorted-two-pointer category: occurrence-aware intersection
     * cardinality for immutable M3 rank snapshots. Each matching duplicate
     * consumes one occurrence from each owner, so the result is the sum of
     * minimum frequencies. No sorting, boxing or per-entry allocations occur.
     *
     * <p>The Java atom is the default; explicitly enabled large batches use
     * the existing bounded-page m3rankbatch JNI library.</p>
     */
    public long intersectionCount(M3LongRankIndex other) {
        Objects.requireNonNull(other, "other");
        return M3LongRankBatchNative.intersectionCount(values, other.values);
    }

    /**
     * Returns detached signed-sorted intersection values with no boxing.
     * Distinct mode implements the LeetCode #349 set-intersection category;
     * duplicate-preserving mode implements #350 with min occurrence counts.
     * The existing parent-owned snapshots never move or expose their arrays.
     * Large prepared primitive intersections use the existing optional JNI
     * m3rankbatch owner; missing native support remains a Java fallback.
     */
    public long[] intersectionValues(M3LongRankIndex other, boolean distinctValues) {
        Objects.requireNonNull(other, "other");
        return M3LongRankBatchNative.intersectionValues(values, other.values, distinctValues);
    }

    /**
     * Detached sorted primitive union. In occurrence-preserving mode the result
     * retains the maximum frequency of each signed long across both snapshots;
     * distinct mode emits each value only once (LeetCode #88 + #26 categories).
     * No source payload moves or per-element object allocation occurs.
     * The existing m3rankbatch JNI provider is optional for large inputs.
     */
    public long[] unionValues(M3LongRankIndex other, boolean distinctValues) {
        Objects.requireNonNull(other, "other");
        return M3LongRankBatchNative.unionValues(values, other.values, distinctValues);
    }

    /**
     * Multiset union cardinality, counting each value's maximum multiplicity.
     * Long arithmetic is required because two legal int-sized owners can have
     * a combined cardinality exceeding Integer.MAX_VALUE.
     */
    public long unionCount(M3LongRankIndex other) {
        Objects.requireNonNull(other, "other");
        return (long) values.length + other.values.length - intersectionCount(other);
    }

    /**
     * LeetCode #2215 / #350 category: sorted difference of this immutable
     * rank snapshot and another. Distinct mode emits values not present in
     * other; multiset mode emits max(this frequency - other frequency, 0)
     * occurrences. Detached primitive results, no retained Entry objects.
     */
    public long[] differenceValues(M3LongRankIndex other, boolean distinctValues) {
        Objects.requireNonNull(other, "other");
        return M3LongRankBatchNative.differenceValues(values, other.values, distinctValues);
    }

    /**
     * Sorted symmetric difference: distinct key XOR, or absolute difference
     * of occurrence multiplicities. Reuses the same JNI provider as rank
     * batches, without moving either immutable source payload.
     */
    public long[] symmetricDifferenceValues(M3LongRankIndex other, boolean distinctValues) {
        Objects.requireNonNull(other, "other");
        return M3LongRankBatchNative.symmetricDifferenceValues(
                values, other.values, distinctValues);
    }


    /**
     * LeetCode #347 Top K Frequent Elements over immutable sorted primitive
     * payloads. Values are ordered by descending occurrence frequency, then
     * ascending signed-long value; fewer than k distinct values return all.
     *
     * <p>The existing native m3rankbatch batch owner can compute large
     * prepared snapshots with bounded 256-long JNI pages. Smaller inputs and
     * missing optional native binaries use the same primitive-array Java
     * oracle; the owner snapshot and its cached run facts remain untouched.
     */
    public long[] topKFrequent(int k) {
        return M3LongRankBatchNative.topKFrequent(values, k);
    }

    /**
     * Number of different signed-long values in this immutable snapshot.
     * LeetCode #26 (sorted duplicate runs) supplies the problem category,
     * not transplanted challenge code. No mutation of the owning long[].
     */
    public int distinctValueCount() {
        return distinct ? values.length : (int) (runStatistics() >>> 32);
    }

    /**
     * Maximum number of distinct consecutive values in signed-long order.
     * Duplicate occurrences do not extend a run; the Long.MAX_VALUE boundary
     * never wraps to Long.MIN_VALUE. LC #128 is the catalogued problem type.
     * One lazily cached packed scalar is shared by both queries.
     */
    public int longestConsecutiveRun() {
        return (int) runStatistics();
    }

    private long runStatistics() {
        long packed = cachedRunStatistics;
        if (packed < 0L) {
            packed = M3LongRankBatchNative.runStatistics(values);
            cachedRunStatistics = packed;
        }
        return packed;
    }

    /**
     * Counts distinct unordered signed-long value pairs separated by an exact,
     * nonnegative absolute distance (LeetCode #532 category). For distance
     * zero each counted value must occur at least twice. The immutable sorted
     * snapshot is never exposed, reordered or materialized for JNI.
     *
     * <p>Native is an optional same-provider batch kernel above the normal
     * size threshold, with a Java two-pointer oracle and no callback bridge.
     * The result is long to retain a primitive order-statistic contract.</p>
     */
    public long distinctDifferencePairCount(long distance) {
        return M3LongRankBatchNative.distinctDifferencePairCount(values, distance);
    }

    /**
     * LeetCode #719 distance-predicate count over all unordered pairs of
     * occurrence indexes i &lt; j in the immutable sorted owner. Each duplicate
     * occurrence participates; output is long to avoid int overflow.
     * Nonnegative thresholds are signed-long distances. Differences greater
     * than Long.MAX_VALUE are excluded, without subtract/abs overflow.
     */
    public long pairCountWithinDistance(long maxDistance) {
        return M3LongRankBatchNative.pairDistanceCountJava(values, maxDistance);
    }

    /**
     * Batched distance-predicate counts. The original m3rankbatch JNI kernel
     * is optionally used for large batches; each threshold produces one long.
     * No per-pair objects or Java callbacks cross the JNI boundary.
     */
    public long[] pairCountsWithinDistances(long[] maxDistances) {
        return M3LongRankBatchNative.pairDistanceCounts(values, maxDistances);
    }


    /**
     * LeetCode #658: primitive starting coordinates of the k nearest elements
     * for each query, over this original immutable signed-long index.
     * If distances tie, the lower value wins; equal-value windows use the
     * earliest identical physical start. No per-entry allocation.
     */
    public int[] closestWindowStarts(long[] queries, int k) {
        return M3LongRankBatchNative.closestWindowStarts(values, queries, k);
    }

    /**
     * Writes k-closest starting coordinates to caller-owned primitive storage.
     * Query/output ranges and k are validated before the first output write.
     */
    public void closestWindowStartsInto(long[] queries, int queryFrom, int length,
            int k, int[] output, int outputFrom) {
        M3LongRankBatchNative.closestWindowStartsInto(
                values, queries, queryFrom, length, k, output, outputFrom, false);
    }

    /** Primitive stream over the immutable sorted snapshot, with no boxed values. */
    public LongStream longStream() {
        Spliterator.OfLong spliterator = Spliterators.spliterator(
                values, Spliterator.ORDERED | Spliterator.SORTED | Spliterator.IMMUTABLE
                        | Spliterator.NONNULL);
        return StreamSupport.longStream(spliterator, false);
    }
}
