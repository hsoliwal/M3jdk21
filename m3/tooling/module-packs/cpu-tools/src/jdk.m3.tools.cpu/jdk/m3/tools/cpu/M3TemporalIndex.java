// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

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

    /**
     * Prepare repeated event-time queries over a detached nondecreasing long lane.
     * Duplicates retain their original ordinal positions. At most one million events.
     * Construction is O(n); subsequent scalar rank/count queries are O(log n).
     */
    public static EventIndex prepareEvents(long[] timestamp) {
        Objects.requireNonNull(timestamp, "timestamp");
        if (timestamp.length > 1_000_000) throw new IllegalArgumentException("event budget");
        long[] snapshot = timestamp.clone();
        for (int i = 1; i < snapshot.length; i++) if (snapshot[i] < snapshot[i - 1])
            throw new IllegalArgumentException("timestamps not ordered");
        return new EventIndex(snapshot);
    }

    /** Half-open original event-coordinate range, not retained per-event objects. */
    public record EventRange(int from, int to) {
        public EventRange {
            if (from < 0 || to < from) throw new IllegalArgumentException("event range");
        }
        public int size() { return to - from; }
    }

    /** Immutable primitive event owner. Array exports are independent mutable snapshots. */
    public static final class EventIndex {
        private final long[] timestamp;
        private EventIndex(long[] timestamp) { this.timestamp = timestamp; }
        public int size() { return timestamp.length; }
        public long timestampAt(int row) {
            return timestamp[Objects.checkIndex(row, timestamp.length)];
        }
        public long[] timestamps() { return timestamp.clone(); }
        public int lowerBound(long time) { return M3TemporalIndex.lowerBound(timestamp, time); }
        public int upperBound(long time) { return M3TemporalIndex.upperBound(timestamp, time); }
        public int previousOrEqual(long time) { return upperBound(time) - 1; }
        public int nextOrEqual(long time) {
            int row = lowerBound(time);
            return row == timestamp.length ? -1 : row;
        }
        /** Exact [from,to) count; no endpoint increment or decrement is used. */
        public int count(long from, long to) {
            if (to < from) throw new IllegalArgumentException("time range");
            return lowerBound(to) - lowerBound(from);
        }
        public EventRange range(long from, long to) {
            if (to < from) throw new IllegalArgumentException("time range");
            return new EventRange(lowerBound(from), lowerBound(to));
        }
        /** Validate every query before computing a detached count lane. */
        public long[] counts(long[] from, long[] to) {
            Objects.requireNonNull(from, "from"); Objects.requireNonNull(to, "to");
            if (from.length != to.length || from.length > 1_000_000)
                throw new IllegalArgumentException("query geometry");
            for (int i = 0; i < from.length; i++) if (to[i] < from[i])
                throw new IllegalArgumentException("time range");
            long[] result = new long[from.length];
            for (int i = 0; i < from.length; i++) result[i] = count(from[i], to[i]);
            return result;
        }
    }
}
