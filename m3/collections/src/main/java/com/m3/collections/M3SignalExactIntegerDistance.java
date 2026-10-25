/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/**
 * Exact, nonnegative weighted-L1 distance for original M3 integer/long signal lanes.
 *
 * <p>The existing {@link M3SignalColumns} owns all values, validity masks, and
 * physical coordinates. This plan retains only primitive dimension/weight arrays
 * and a source epoch, and uses a bounded primitive worst-first heap for Top-K.
 * It never converts a full-width signed long through {@code double}, allocates an
 * Entry/Node for any candidate, or treats exact numeric distance over declared
 * source coordinates as proof of semantic or Java-object equality.</p>
 *
 * <p>Only caller-declared {@code EXACT_FACT} dimensions may participate. This
 * verifies their numeric representation, <em>not</em> the producer's external
 * truthfulness or an equivalence/correctness certificate. Dynamic/reflected/
 * native changes must be reported to the original dependency-epoch owner.</p>
 */
public final class M3SignalExactIntegerDistance {
    private static final int MAX_DIMENSIONS = 128;
    private static final int MAX_WEIGHT = 1_000_000;

    /** Unsigned, exact 128-bit magnitude. Materialization occurs at an API boundary only. */
    public record Distance(long high, long low) implements Comparable<Distance> {
        @Override public int compareTo(Distance other) {
            Objects.requireNonNull(other, "other");
            int order = Long.compareUnsigned(high, other.high);
            return order != 0 ? order : Long.compareUnsigned(low, other.low);
        }
        public BigInteger toBigInteger() {
            return new BigInteger(Long.toUnsignedString(high)).shiftLeft(Long.SIZE)
                    .add(new BigInteger(Long.toUnsignedString(low)));
        }
    }

    private final M3SignalQualitySchema schema;
    private final M3SignalColumns columns;
    private final M3SignalDependencyEpochs invalidations;
    private final int[] dimensions;
    private final int[] weights;
    private final long builtEpoch;

    private M3SignalExactIntegerDistance(M3SignalQualitySchema source,
            int[] selectedDimensions, int[] selectedWeights) {
        schema = Objects.requireNonNull(source, "source");
        Objects.requireNonNull(selectedDimensions, "dimensions");
        Objects.requireNonNull(selectedWeights, "weights");
        if (selectedDimensions.length == 0 || selectedDimensions.length > MAX_DIMENSIONS
                || selectedDimensions.length != selectedWeights.length) {
            throw new IllegalArgumentException("integer metric dimensions outside [1,128]");
        }
        columns = schema.valueColumnsOwner();
        invalidations = schema.invalidationEpochsOwner();
        long initialEpoch = invalidations.epoch();
        boolean[] used = new boolean[schema.dimensionCount()];
        for (int i = 0; i < selectedDimensions.length; i++) {
            int dimension = Objects.checkIndex(selectedDimensions[i], used.length);
            if (used[dimension]) throw new IllegalArgumentException("duplicate integer metric dimension");
            used[dimension] = true;
            if (schema.reliability(dimension) != M3SignalQualitySchema.Reliability.EXACT_FACT) {
                throw new IllegalArgumentException("metric input must be an exact source-coordinate claim");
            }
            if (schema.kind(dimension) == M3SignalColumns.Kind.DOUBLE) {
                throw new IllegalArgumentException("nonintegral/double metric requires a separate policy");
            }
            if (selectedWeights[i] < 1 || selectedWeights[i] > MAX_WEIGHT) {
                throw new IllegalArgumentException("weight outside [1,1000000]");
            }
        }
        this.dimensions = selectedDimensions.clone();
        this.weights = selectedWeights.clone();
        if (initialEpoch != invalidations.epoch()) {
            throw new IllegalStateException("source changed during metric plan preparation");
        }
        builtEpoch = initialEpoch;
    }

    /** Reuse the original typed lanes without copying any row or signal values. */
    public static M3SignalExactIntegerDistance of(M3SignalQualitySchema source,
            int[] dimensions, int[] positiveWeights) {
        return new M3SignalExactIntegerDistance(source, dimensions, positiveWeights);
    }

    public int dimensionCount() { return dimensions.length; }

    /** Exact distance between two physical rows; rejects missing selected facts. */
    public Distance distance(int leftRow, int rightRow, long version, String sourceSha256) {
        requireFresh(version, sourceSha256);
        Objects.checkIndex(leftRow, schema.rowCount());
        Objects.checkIndex(rightRow, schema.rowCount());
        Accumulator accumulator = new Accumulator();
        accumulate(leftRow, rightRow, accumulator);
        requireFresh(version, sourceSha256);
        return new Distance(accumulator.high, accumulator.low);
    }

    /** Stable exact nearest-neighbor ordering over caller-specified physical row occurrences. */
    public int[] nearest(int sourceRow, int[] candidates, int limit,
            long version, String sourceSha256) {
        return nearest(sourceRow, candidates, limit, version, sourceSha256, null);
    }

    /**
     * Bounded O(k) transient primitive heap; stable ties retain candidate encounter
     * order, including repeated physical IDs. Caller retains ownership of inputs.
     * Optional progress monitor reports one completed candidate per unit and may
     * cancel or invalidate the source; no partial results are ever returned.
     */
    public int[] nearest(int sourceRow, int[] candidates, int limit,
            long version, String sourceSha256, M3ProgressMonitor monitor) {
        Objects.requireNonNull(candidates, "candidates");
        if (limit < 0) throw new IllegalArgumentException("negative result limit");
        requireFresh(version, sourceSha256);
        Objects.checkIndex(sourceRow, schema.rowCount());
        for (int row : candidates) Objects.checkIndex(row, schema.rowCount());
        if (monitor != null) monitor.checkCanceled();
        int k = Math.min(limit, candidates.length);
        if (k == 0 || candidates.length == 0) {
            requireFresh(version, sourceSha256);
            return new int[0];
        }
        if (monitor != null) {
            monitor.beginTask("M3 exact integer signal nearest", candidates.length);
            monitor.checkCanceled();
            requireFresh(version, sourceSha256);
        }
        int[] rows = new int[k];
        int[] ordinal = new int[k];
        long[] highs = new long[k], lows = new long[k];
        Accumulator scratch = new Accumulator();
        int filled = 0;
        for (int position = 0; position < candidates.length; position++) {
            if (monitor != null) monitor.checkCanceled();
            requireFresh(version, sourceSha256);
            accumulate(sourceRow, candidates[position], scratch);
            if (filled < k) {
                rows[filled] = candidates[position];
                ordinal[filled] = position;
                highs[filled] = scratch.high;
                lows[filled] = scratch.low;
                siftUp(rows, ordinal, highs, lows, filled++);
            } else if (compare(scratch.high, scratch.low, position,
                    highs[0], lows[0], ordinal[0]) < 0) {
                rows[0] = candidates[position];
                ordinal[0] = position;
                highs[0] = scratch.high;
                lows[0] = scratch.low;
                siftDown(rows, ordinal, highs, lows, 0, filled);
            }
            if (monitor != null) {
                monitor.worked(1);
                monitor.checkCanceled();
            }
        }
        // Primitive max-heap -> ascending exact distance and stable encounter order.
        for (int remaining = filled; remaining > 1; remaining--) {
            swap(rows, ordinal, highs, lows, 0, remaining - 1);
            siftDown(rows, ordinal, highs, lows, 0, remaining - 1);
        }
        if (monitor != null) monitor.checkCanceled();
        requireFresh(version, sourceSha256);
        if (monitor != null) {
            monitor.done();
            monitor.checkCanceled();
            requireFresh(version, sourceSha256);
        }
        return rows;
    }

    /** Reusable pair-local primitive scratch, not a per-candidate object wrapper. */
    private static final class Accumulator {
        private long high, low;
    }

    private void accumulate(int left, int right, Accumulator scratch) {
        scratch.high = 0L;
        scratch.low = 0L;
        for (int i = 0; i < dimensions.length; i++) {
            int column = dimensions[i];
            if (!schema.validAt(column, left) || !schema.validAt(column, right)) {
                throw new IllegalArgumentException("missing exact metric coordinate: " + column);
            }
            long delta;
            if (columns.kind(column) == M3SignalColumns.Kind.INTEGER) {
                long a = columns.exactIntAt(column, left);
                long b = columns.exactIntAt(column, right);
                delta = a >= b ? a - b : b - a;
            } else {
                long a = columns.exactLongAt(column, left);
                long b = columns.exactLongAt(column, right);
                // This is the correct unsigned 0 .. 2^64-1 signed-long separation.
                delta = a >= b ? a - b : b - a;
            }
            long weight = weights[i];
            long addLow = delta * weight;
            long addHigh = Math.unsignedMultiplyHigh(delta, weight);
            long nextLow = scratch.low + addLow;
            long carry = Long.compareUnsigned(nextLow, scratch.low) < 0 ? 1L : 0L;
            long nextHigh = scratch.high + addHigh + carry;
            if (Long.compareUnsigned(nextHigh, scratch.high) < 0) {
                throw new ArithmeticException("unsigned 128-bit distance overflow");
            }
            scratch.high = nextHigh;
            scratch.low = nextLow;
        }
    }

    private void requireFresh(long version, String sha256) {
        schema.requireCurrent(version, sha256);
        if (invalidations.epoch() != builtEpoch) {
            throw new IllegalStateException("source changed since integer metric plan creation");
        }
    }

    private static int compare(long highA, long lowA, int ordinalA,
            long highB, long lowB, int ordinalB) {
        int comparison = Long.compareUnsigned(highA, highB);
        if (comparison != 0) return comparison;
        comparison = Long.compareUnsigned(lowA, lowB);
        return comparison != 0 ? comparison : Integer.compare(ordinalA, ordinalB);
    }

    private static int heapCompare(long[] high, long[] low, int[] ord, int a, int b) {
        return compare(high[a], low[a], ord[a], high[b], low[b], ord[b]);
    }

    private static void siftUp(int[] row, int[] ord, long[] high, long[] low, int pos) {
        while (pos > 0) {
            int parent = (pos - 1) >>> 1;
            if (heapCompare(high, low, ord, pos, parent) <= 0) break;
            swap(row, ord, high, low, pos, parent);
            pos = parent;
        }
    }

    private static void siftDown(int[] row, int[] ord, long[] high, long[] low,
            int index, int bound) {
        while (true) {
            int child = (index << 1) + 1;
            if (child >= bound) return;
            int next = child + 1;
            if (next < bound && heapCompare(high, low, ord, next, child) > 0) child = next;
            if (heapCompare(high, low, ord, index, child) >= 0) return;
            swap(row, ord, high, low, index, child);
            index = child;
        }
    }

    private static void swap(int[] row, int[] ord, long[] high, long[] low, int a, int b) {
        int sr = row[a]; row[a] = row[b]; row[b] = sr;
        int so = ord[a]; ord[a] = ord[b]; ord[b] = so;
        long sh = high[a]; high[a] = high[b]; high[b] = sh;
        long sl = low[a]; low[a] = low[b]; low[b] = sl;
    }
}
