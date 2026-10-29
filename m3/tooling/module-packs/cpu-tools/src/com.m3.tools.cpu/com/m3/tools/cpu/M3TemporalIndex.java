// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Exact event-time tumbling aggregates and interval-overlap signals in primitive arrays.
 * Not a distributed CEP engine, temporal SQL, or a timezone/recurrence library.
 */
public final class M3TemporalIndex {
    private M3TemporalIndex() { }
    public record Windows(long[] bucket, long[] count, long[] sum) {
        public Windows { bucket = bucket.clone(); count = count.clone(); sum = sum.clone(); }
        @Override public long[] bucket() { return bucket.clone(); }
        @Override public long[] count() { return count.clone(); }
        @Override public long[] sum() { return sum.clone(); }
    }

    /** Time input is nondecreasing; buckets are mathematical floor((timestamp-origin)/width).
     * Missing buckets are omitted, and all timestamp/count/sum overflow refuses.
     */
    public static Windows tumble(long[] timestamp, long[] value, long origin, long width) {
        Objects.requireNonNull(timestamp); Objects.requireNonNull(value);
        if (timestamp.length != value.length || width <= 0)
            throw new IllegalArgumentException("event geometry or width");
        long[] buckets = new long[timestamp.length], counts = new long[timestamp.length];
        long[] sums = new long[timestamp.length];
        int used = 0;
        for (int i = 0; i < timestamp.length; i++) {
            if (i != 0 && timestamp[i] < timestamp[i - 1])
                throw new IllegalArgumentException("timestamps not ordered");
            long key = Math.floorDiv(Math.subtractExact(timestamp[i], origin), width);
            if (used == 0 || buckets[used - 1] != key) {
                buckets[used] = key; counts[used] = 1; sums[used] = value[i]; used++;
            } else {
                counts[used - 1] = Math.addExact(counts[used - 1], 1);
                sums[used - 1] = Math.addExact(sums[used - 1], value[i]);
            }
        }
        return new Windows(Arrays.copyOf(buckets, used), Arrays.copyOf(counts, used),
                Arrays.copyOf(sums, used));
    }

    /** Original-index mask for intervals overlapping at least one other nonempty interval.
     * Long half-open [start,end) geometry. Two sorted primitive endpoint lanes permit
     * O(n log n) construction and O(n log n) query; zero-length intervals do not overlap.
     */
    public static long[] overlapMask(long[] start, long[] end) {
        Objects.requireNonNull(start); Objects.requireNonNull(end);
        if (start.length != end.length || start.length > 1_000_000)
            throw new IllegalArgumentException("interval geometry");
        int n = start.length, nonempty = 0;
        for (int i = 0; i < n; i++) {
            if (end[i] < start[i]) throw new IllegalArgumentException("reversed interval");
            if (end[i] > start[i]) nonempty++;
        }
        long[] begin = new long[nonempty], finish = new long[nonempty];
        for (int i = 0, at = 0; i < n; i++) if (start[i] < end[i]) {
            begin[at] = start[i]; finish[at++] = end[i];
        }
        Arrays.sort(begin); Arrays.sort(finish);
        long[] mask = new long[(n + 63) >>> 6];
        for (int i = 0; i < n; i++) if (start[i] < end[i]) {
            // Endpoint counts differ by the number of overlaps, including self.
            int beginningBeforeEnd = lowerBound(begin, end[i]);
            int endingAtOrBeforeStart = upperBound(finish, start[i]);
            if (beginningBeforeEnd - endingAtOrBeforeStart > 1)
                mask[i >>> 6] |= 1L << (i & 63);
        }
        return mask;
    }

    private static int lowerBound(long[] sorted, long value) {
        int low = 0, high = sorted.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (sorted[middle] < value) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private static int upperBound(long[] sorted, long value) {
        int low = 0, high = sorted.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (sorted[middle] <= value) low = middle + 1;
            else high = middle;
        }
        return low;
    }
}
