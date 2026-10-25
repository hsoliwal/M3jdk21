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

    /** Java or explicitly enabled JNI batched lower bounds, without per-query boxing. */
    public int[] lowerBounds(long[] queries) {
        return M3LongRankBatchNative.lowerBounds(values, queries);
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

    /** Primitive stream over the immutable sorted snapshot, with no boxed values. */
    public LongStream longStream() {
        Spliterator.OfLong spliterator = Spliterators.spliterator(
                values, Spliterator.ORDERED | Spliterator.SORTED | Spliterator.IMMUTABLE
                        | Spliterator.NONNULL);
        return StreamSupport.longStream(spliterator, false);
    }
}
