/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import java.util.Objects;

/**
 * Primitive, caller-buffer lower/upper-bound kernels. No boxed value or per-query object.
 * Scalar calls require an ascending range; batch calls check order once before writing.
 * Batch queries reuse the previous exact bound: equal queries do no further search,
 * nearby increasing queries gallop forward, and decreasing queries search only the proven prefix.
 * Floating-point order is Float.compare/Double.compare (negative zero first, NaNs last).
 * Arrays must not be concurrently mutated. Returned indexes are absolute, not range-relative.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedPrimitiveArrayOps.java}
 * blob {@code 626e25fb7f50a89bff9e3ad3a0073c4c4310dcb3} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedPrimitiveArrayOps {
    private static final int LOCAL_GALLOP_DISTANCE = 16;

    private PackedPrimitiveArrayOps() { }

    private static void output(Object source, Object queries, int[] output, int offset, int count) {
        Objects.requireNonNull(output, "output");
        Objects.checkFromIndexSize(offset, count, output.length);
        if (output == source || output == queries) {
            throw new IllegalArgumentException("output must not alias input");
        }
    }

    private static void output(Object source, Object queries, long[] output, int offset, int count) {
        Objects.requireNonNull(output, "output");
        Objects.checkFromIndexSize(offset, count, output.length);
        if (output == source || output == queries) {
            throw new IllegalArgumentException("output must not alias input");
        }
    }

    /** Lower component of one packed half-open equal range. */
    public static int equalRangeLower(long packedRange) {
        return (int) (packedRange >>> 32);
    }

    /** Upper component of one packed half-open equal range. */
    public static int equalRangeUpper(long packedRange) {
        return (int) packedRange;
    }

    private static long packRange(int lower, int upper) {
        return ((long) lower << 32) | (upper & 0xffff_ffffL);
    }

    /** Exact half-open duplicate/insertion range over one checked primitive subrange. */
    public static long equalRangePacked(byte[] sorted, int from, int to, byte value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        int lower = bound(sorted, from, to, value, false);
        int upper = lower < to && Byte.compare(sorted[lower], value) == 0
                ? bound(sorted, lower, to, value, true) : lower;
        return packRange(lower, upper);
    }

    /**
     * Writes packed half-open equal ranges into caller-owned output.
     * The sorted range is validated before any output write.
     */
    public static int equalRangesPacked(byte[] sorted, int from, int to, byte[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        int count = queryTo - queryFrom;
        output(sorted, queries, result, resultFrom, count);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Byte.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        for (int i = 0; i < count; i++) {
            byte value = queries[queryFrom + i];
            int lower = bound(sorted, from, to, value, false);
            int upper = lower < to && Byte.compare(sorted[lower], value) == 0
                    ? bound(sorted, lower, to, value, true) : lower;
            result[resultFrom + i] = packRange(lower, upper);
        }
        return count;
    }

    /** Exact half-open duplicate/insertion range over one checked primitive subrange. */
    public static long equalRangePacked(short[] sorted, int from, int to, short value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        int lower = bound(sorted, from, to, value, false);
        int upper = lower < to && Short.compare(sorted[lower], value) == 0
                ? bound(sorted, lower, to, value, true) : lower;
        return packRange(lower, upper);
    }

    /**
     * Writes packed half-open equal ranges into caller-owned output.
     * The sorted range is validated before any output write.
     */
    public static int equalRangesPacked(short[] sorted, int from, int to, short[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        int count = queryTo - queryFrom;
        output(sorted, queries, result, resultFrom, count);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Short.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        for (int i = 0; i < count; i++) {
            short value = queries[queryFrom + i];
            int lower = bound(sorted, from, to, value, false);
            int upper = lower < to && Short.compare(sorted[lower], value) == 0
                    ? bound(sorted, lower, to, value, true) : lower;
            result[resultFrom + i] = packRange(lower, upper);
        }
        return count;
    }

    /** Exact half-open duplicate/insertion range over one checked primitive subrange. */
    public static long equalRangePacked(char[] sorted, int from, int to, char value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        int lower = bound(sorted, from, to, value, false);
        int upper = lower < to && Character.compare(sorted[lower], value) == 0
                ? bound(sorted, lower, to, value, true) : lower;
        return packRange(lower, upper);
    }

    /**
     * Writes packed half-open equal ranges into caller-owned output.
     * The sorted range is validated before any output write.
     */
    public static int equalRangesPacked(char[] sorted, int from, int to, char[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        int count = queryTo - queryFrom;
        output(sorted, queries, result, resultFrom, count);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Character.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        for (int i = 0; i < count; i++) {
            char value = queries[queryFrom + i];
            int lower = bound(sorted, from, to, value, false);
            int upper = lower < to && Character.compare(sorted[lower], value) == 0
                    ? bound(sorted, lower, to, value, true) : lower;
            result[resultFrom + i] = packRange(lower, upper);
        }
        return count;
    }

    /** Exact half-open duplicate/insertion range over one checked primitive subrange. */
    public static long equalRangePacked(int[] sorted, int from, int to, int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        int lower = bound(sorted, from, to, value, false);
        int upper = lower < to && Integer.compare(sorted[lower], value) == 0
                ? bound(sorted, lower, to, value, true) : lower;
        return packRange(lower, upper);
    }

    /**
     * Writes packed half-open equal ranges into caller-owned output.
     * The sorted range is validated before any output write.
     */
    public static int equalRangesPacked(int[] sorted, int from, int to, int[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        int count = queryTo - queryFrom;
        output(sorted, queries, result, resultFrom, count);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Integer.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        for (int i = 0; i < count; i++) {
            int value = queries[queryFrom + i];
            int lower = bound(sorted, from, to, value, false);
            int upper = lower < to && Integer.compare(sorted[lower], value) == 0
                    ? bound(sorted, lower, to, value, true) : lower;
            result[resultFrom + i] = packRange(lower, upper);
        }
        return count;
    }

    /** Exact half-open duplicate/insertion range over one checked primitive subrange. */
    public static long equalRangePacked(long[] sorted, int from, int to, long value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        int lower = bound(sorted, from, to, value, false);
        int upper = lower < to && Long.compare(sorted[lower], value) == 0
                ? bound(sorted, lower, to, value, true) : lower;
        return packRange(lower, upper);
    }

    /**
     * Writes packed half-open equal ranges into caller-owned output.
     * The sorted range is validated before any output write.
     */
    public static int equalRangesPacked(long[] sorted, int from, int to, long[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        int count = queryTo - queryFrom;
        output(sorted, queries, result, resultFrom, count);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Long.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        for (int i = 0; i < count; i++) {
            long value = queries[queryFrom + i];
            int lower = bound(sorted, from, to, value, false);
            int upper = lower < to && Long.compare(sorted[lower], value) == 0
                    ? bound(sorted, lower, to, value, true) : lower;
            result[resultFrom + i] = packRange(lower, upper);
        }
        return count;
    }

    /** Exact half-open duplicate/insertion range over one checked primitive subrange. */
    public static long equalRangePacked(float[] sorted, int from, int to, float value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        int lower = bound(sorted, from, to, value, false);
        int upper = lower < to && Float.compare(sorted[lower], value) == 0
                ? bound(sorted, lower, to, value, true) : lower;
        return packRange(lower, upper);
    }

    /**
     * Writes packed half-open equal ranges into caller-owned output.
     * The sorted range is validated before any output write.
     */
    public static int equalRangesPacked(float[] sorted, int from, int to, float[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        int count = queryTo - queryFrom;
        output(sorted, queries, result, resultFrom, count);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Float.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        for (int i = 0; i < count; i++) {
            float value = queries[queryFrom + i];
            int lower = bound(sorted, from, to, value, false);
            int upper = lower < to && Float.compare(sorted[lower], value) == 0
                    ? bound(sorted, lower, to, value, true) : lower;
            result[resultFrom + i] = packRange(lower, upper);
        }
        return count;
    }

    /** Exact half-open duplicate/insertion range over one checked primitive subrange. */
    public static long equalRangePacked(double[] sorted, int from, int to, double value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        int lower = bound(sorted, from, to, value, false);
        int upper = lower < to && Double.compare(sorted[lower], value) == 0
                ? bound(sorted, lower, to, value, true) : lower;
        return packRange(lower, upper);
    }

    /**
     * Writes packed half-open equal ranges into caller-owned output.
     * The sorted range is validated before any output write.
     */
    public static int equalRangesPacked(double[] sorted, int from, int to, double[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        int count = queryTo - queryFrom;
        output(sorted, queries, result, resultFrom, count);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Double.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        for (int i = 0; i < count; i++) {
            double value = queries[queryFrom + i];
            int lower = bound(sorted, from, to, value, false);
            int upper = lower < to && Double.compare(sorted[lower], value) == 0
                    ? bound(sorted, lower, to, value, true) : lower;
            result[resultFrom + i] = packRange(lower, upper);
        }
        return count;
    }

    /** Exact half-open duplicate/insertion range over one checked primitive subrange. */
    public static long equalRangePacked(boolean[] sorted, int from, int to, boolean value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        int lower = bound(sorted, from, to, value, false);
        int upper = lower < to && Boolean.compare(sorted[lower], value) == 0
                ? bound(sorted, lower, to, value, true) : lower;
        return packRange(lower, upper);
    }

    /**
     * Writes packed half-open equal ranges into caller-owned output.
     * The sorted range is validated before any output write.
     */
    public static int equalRangesPacked(boolean[] sorted, int from, int to, boolean[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        int count = queryTo - queryFrom;
        output(sorted, queries, result, resultFrom, count);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Boolean.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        for (int i = 0; i < count; i++) {
            boolean value = queries[queryFrom + i];
            int lower = bound(sorted, from, to, value, false);
            int upper = lower < to && Boolean.compare(sorted[lower], value) == 0
                    ? bound(sorted, lower, to, value, true) : lower;
            result[resultFrom + i] = packRange(lower, upper);
        }
        return count;
    }

    // Exact lookup is a projection of the canonical bound/equal-range facts.

    public static int exactIndex(byte[] sorted, int from, int to, byte value) {
        long range = equalRangePacked(sorted, from, to, value);
        int lower = equalRangeLower(range);
        return lower < equalRangeUpper(range) ? lower : -1;
    }

    public static int exactIndexes(byte[] sorted, int from, int to, byte[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom) {
        bounds(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, false);
        int count = queryTo - queryFrom;
        for (int i = 0; i < count; i++) {
            int at = result[resultFrom + i];
            if (at >= to || Byte.compare(sorted[at], queries[queryFrom + i]) != 0) {
                result[resultFrom + i] = -1;
            }
        }
        return count;
    }

    public static int exactIndex(short[] sorted, int from, int to, short value) {
        long range = equalRangePacked(sorted, from, to, value);
        int lower = equalRangeLower(range);
        return lower < equalRangeUpper(range) ? lower : -1;
    }

    public static int exactIndexes(short[] sorted, int from, int to, short[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom) {
        bounds(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, false);
        int count = queryTo - queryFrom;
        for (int i = 0; i < count; i++) {
            int at = result[resultFrom + i];
            if (at >= to || Short.compare(sorted[at], queries[queryFrom + i]) != 0) {
                result[resultFrom + i] = -1;
            }
        }
        return count;
    }

    public static int exactIndex(char[] sorted, int from, int to, char value) {
        long range = equalRangePacked(sorted, from, to, value);
        int lower = equalRangeLower(range);
        return lower < equalRangeUpper(range) ? lower : -1;
    }

    public static int exactIndexes(char[] sorted, int from, int to, char[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom) {
        bounds(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, false);
        int count = queryTo - queryFrom;
        for (int i = 0; i < count; i++) {
            int at = result[resultFrom + i];
            if (at >= to || Character.compare(sorted[at], queries[queryFrom + i]) != 0) {
                result[resultFrom + i] = -1;
            }
        }
        return count;
    }

    public static int exactIndex(int[] sorted, int from, int to, int value) {
        long range = equalRangePacked(sorted, from, to, value);
        int lower = equalRangeLower(range);
        return lower < equalRangeUpper(range) ? lower : -1;
    }

    public static int exactIndexes(int[] sorted, int from, int to, int[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom) {
        bounds(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, false);
        int count = queryTo - queryFrom;
        for (int i = 0; i < count; i++) {
            int at = result[resultFrom + i];
            if (at >= to || Integer.compare(sorted[at], queries[queryFrom + i]) != 0) {
                result[resultFrom + i] = -1;
            }
        }
        return count;
    }

    public static int exactIndex(long[] sorted, int from, int to, long value) {
        long range = equalRangePacked(sorted, from, to, value);
        int lower = equalRangeLower(range);
        return lower < equalRangeUpper(range) ? lower : -1;
    }

    public static int exactIndexes(long[] sorted, int from, int to, long[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom) {
        bounds(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, false);
        int count = queryTo - queryFrom;
        for (int i = 0; i < count; i++) {
            int at = result[resultFrom + i];
            if (at >= to || Long.compare(sorted[at], queries[queryFrom + i]) != 0) {
                result[resultFrom + i] = -1;
            }
        }
        return count;
    }

    public static int exactIndex(float[] sorted, int from, int to, float value) {
        long range = equalRangePacked(sorted, from, to, value);
        int lower = equalRangeLower(range);
        return lower < equalRangeUpper(range) ? lower : -1;
    }

    public static int exactIndexes(float[] sorted, int from, int to, float[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom) {
        bounds(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, false);
        int count = queryTo - queryFrom;
        for (int i = 0; i < count; i++) {
            int at = result[resultFrom + i];
            if (at >= to || Float.compare(sorted[at], queries[queryFrom + i]) != 0) {
                result[resultFrom + i] = -1;
            }
        }
        return count;
    }

    public static int exactIndex(double[] sorted, int from, int to, double value) {
        long range = equalRangePacked(sorted, from, to, value);
        int lower = equalRangeLower(range);
        return lower < equalRangeUpper(range) ? lower : -1;
    }

    public static int exactIndexes(double[] sorted, int from, int to, double[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom) {
        bounds(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, false);
        int count = queryTo - queryFrom;
        for (int i = 0; i < count; i++) {
            int at = result[resultFrom + i];
            if (at >= to || Double.compare(sorted[at], queries[queryFrom + i]) != 0) {
                result[resultFrom + i] = -1;
            }
        }
        return count;
    }

    public static int exactIndex(boolean[] sorted, int from, int to, boolean value) {
        long range = equalRangePacked(sorted, from, to, value);
        int lower = equalRangeLower(range);
        return lower < equalRangeUpper(range) ? lower : -1;
    }

    public static int exactIndexes(boolean[] sorted, int from, int to, boolean[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom) {
        bounds(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, false);
        int count = queryTo - queryFrom;
        for (int i = 0; i < count; i++) {
            int at = result[resultFrom + i];
            if (at >= to || Boolean.compare(sorted[at], queries[queryFrom + i]) != 0) {
                result[resultFrom + i] = -1;
            }
        }
        return count;
    }

    public static int lowerBound(byte[] sorted, int from, int to, byte value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, false);
    }

    public static int upperBound(byte[] sorted, int from, int to, byte value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, true);
    }

    /** Rejects invalid ranges, aliases and descending inputs before writing output. */
    public static void bounds(byte[] sorted, int from, int to, byte[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        output(sorted, queries, result, resultFrom, queryTo - queryFrom);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Byte.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        if (!coherentQueryPrefix(queries, queryFrom, queryTo)) {
            for (int i = queryFrom; i < queryTo; i++) {
                result[resultFrom + i - queryFrom] = bound(sorted, from, to, queries[i], upper);
            }
            return;
        }
        int previous = from;
        int previousAdvance = Integer.MAX_VALUE;
        for (int i = queryFrom; i < queryTo; i++) {
            if (i == queryFrom) {
                previous = bound(sorted, from, to, queries[i], upper);
            } else {
                int direction = Byte.compare(queries[i - 1], queries[i]);
                if (direction < 0) {
                    int start = previous;
                    previous = previousAdvance <= LOCAL_GALLOP_DISTANCE
                            ? gallopBound(sorted, previous, to, queries[i], upper)
                            : bound(sorted, previous, to, queries[i], upper);
                    previousAdvance = previous - start;
                } else if (direction > 0) {
                    previous = bound(sorted, from, previous, queries[i], upper);
                    previousAdvance = Integer.MAX_VALUE;
                }
            }
            result[resultFrom + i - queryFrom] = previous;
        }
    }

    /** Dispatch hint only: every answer still goes through an exact ordered-bound kernel. */
    private static boolean coherentQueryPrefix(byte[] queries, int from, int to) {
        int direction = 0;
        int sample = Math.min(8, to - from);
        for (int i = 1; i < sample; i++) {
            int next = Integer.signum(Byte.compare(queries[from + i - 1], queries[from + i]));
            if (next != 0) {
                if (direction != 0 && direction != next) { return false; }
                direction = next;
            }
        }
        return true;
    }

    /** Trusted sorted range; exponential bracketing reuses the existing binary kernel. */
    static int gallopBound(byte[] sorted, int from, int to, byte value, boolean upper) {
        if (from == to) { return to; }
        int comparison = Byte.compare(sorted[from], value);
        if (comparison > 0 || (!upper && comparison == 0)) { return from; }
        int low = from + 1;
        // Long offsets prevent overflow at the signed-int array-index boundary.
        for (long offset = 1; (long) from + offset < to; offset = (offset << 1) + 1) {
            int probe = (int) ((long) from + offset);
            comparison = Byte.compare(sorted[probe], value);
            if (comparison > 0 || (!upper && comparison == 0)) {
                return bound(sorted, low, probe, value, upper);
            }
            low = probe + 1;
        }
        return bound(sorted, low, to, value, upper);
    }

    private static int bound(byte[] sorted, int from, int to, byte value, boolean upper) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            int comparison = Byte.compare(sorted[mid], value);
            if (comparison < 0 || (upper && comparison == 0)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int lowerBound(short[] sorted, int from, int to, short value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, false);
    }

    public static int upperBound(short[] sorted, int from, int to, short value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, true);
    }

    /** Rejects invalid ranges, aliases and descending inputs before writing output. */
    public static void bounds(short[] sorted, int from, int to, short[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        output(sorted, queries, result, resultFrom, queryTo - queryFrom);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Short.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        if (!coherentQueryPrefix(queries, queryFrom, queryTo)) {
            for (int i = queryFrom; i < queryTo; i++) {
                result[resultFrom + i - queryFrom] = bound(sorted, from, to, queries[i], upper);
            }
            return;
        }
        int previous = from;
        int previousAdvance = Integer.MAX_VALUE;
        for (int i = queryFrom; i < queryTo; i++) {
            if (i == queryFrom) {
                previous = bound(sorted, from, to, queries[i], upper);
            } else {
                int direction = Short.compare(queries[i - 1], queries[i]);
                if (direction < 0) {
                    int start = previous;
                    previous = previousAdvance <= LOCAL_GALLOP_DISTANCE
                            ? gallopBound(sorted, previous, to, queries[i], upper)
                            : bound(sorted, previous, to, queries[i], upper);
                    previousAdvance = previous - start;
                } else if (direction > 0) {
                    previous = bound(sorted, from, previous, queries[i], upper);
                    previousAdvance = Integer.MAX_VALUE;
                }
            }
            result[resultFrom + i - queryFrom] = previous;
        }
    }

    /** Dispatch hint only: every answer still goes through an exact ordered-bound kernel. */
    private static boolean coherentQueryPrefix(short[] queries, int from, int to) {
        int direction = 0;
        int sample = Math.min(8, to - from);
        for (int i = 1; i < sample; i++) {
            int next = Integer.signum(Short.compare(queries[from + i - 1], queries[from + i]));
            if (next != 0) {
                if (direction != 0 && direction != next) { return false; }
                direction = next;
            }
        }
        return true;
    }

    /** Trusted sorted range; exponential bracketing reuses the existing binary kernel. */
    static int gallopBound(short[] sorted, int from, int to, short value, boolean upper) {
        if (from == to) { return to; }
        int comparison = Short.compare(sorted[from], value);
        if (comparison > 0 || (!upper && comparison == 0)) { return from; }
        int low = from + 1;
        // Long offsets prevent overflow at the signed-int array-index boundary.
        for (long offset = 1; (long) from + offset < to; offset = (offset << 1) + 1) {
            int probe = (int) ((long) from + offset);
            comparison = Short.compare(sorted[probe], value);
            if (comparison > 0 || (!upper && comparison == 0)) {
                return bound(sorted, low, probe, value, upper);
            }
            low = probe + 1;
        }
        return bound(sorted, low, to, value, upper);
    }

    private static int bound(short[] sorted, int from, int to, short value, boolean upper) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            int comparison = Short.compare(sorted[mid], value);
            if (comparison < 0 || (upper && comparison == 0)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int lowerBound(char[] sorted, int from, int to, char value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, false);
    }

    public static int upperBound(char[] sorted, int from, int to, char value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, true);
    }

    /** Rejects invalid ranges, aliases and descending inputs before writing output. */
    public static void bounds(char[] sorted, int from, int to, char[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        output(sorted, queries, result, resultFrom, queryTo - queryFrom);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Character.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        if (!coherentQueryPrefix(queries, queryFrom, queryTo)) {
            for (int i = queryFrom; i < queryTo; i++) {
                result[resultFrom + i - queryFrom] = bound(sorted, from, to, queries[i], upper);
            }
            return;
        }
        int previous = from;
        int previousAdvance = Integer.MAX_VALUE;
        for (int i = queryFrom; i < queryTo; i++) {
            if (i == queryFrom) {
                previous = bound(sorted, from, to, queries[i], upper);
            } else {
                int direction = Character.compare(queries[i - 1], queries[i]);
                if (direction < 0) {
                    int start = previous;
                    previous = previousAdvance <= LOCAL_GALLOP_DISTANCE
                            ? gallopBound(sorted, previous, to, queries[i], upper)
                            : bound(sorted, previous, to, queries[i], upper);
                    previousAdvance = previous - start;
                } else if (direction > 0) {
                    previous = bound(sorted, from, previous, queries[i], upper);
                    previousAdvance = Integer.MAX_VALUE;
                }
            }
            result[resultFrom + i - queryFrom] = previous;
        }
    }

    /** Dispatch hint only: every answer still goes through an exact ordered-bound kernel. */
    private static boolean coherentQueryPrefix(char[] queries, int from, int to) {
        int direction = 0;
        int sample = Math.min(8, to - from);
        for (int i = 1; i < sample; i++) {
            int next = Integer.signum(Character.compare(queries[from + i - 1], queries[from + i]));
            if (next != 0) {
                if (direction != 0 && direction != next) { return false; }
                direction = next;
            }
        }
        return true;
    }

    /** Trusted sorted range; exponential bracketing reuses the existing binary kernel. */
    static int gallopBound(char[] sorted, int from, int to, char value, boolean upper) {
        if (from == to) { return to; }
        int comparison = Character.compare(sorted[from], value);
        if (comparison > 0 || (!upper && comparison == 0)) { return from; }
        int low = from + 1;
        // Long offsets prevent overflow at the signed-int array-index boundary.
        for (long offset = 1; (long) from + offset < to; offset = (offset << 1) + 1) {
            int probe = (int) ((long) from + offset);
            comparison = Character.compare(sorted[probe], value);
            if (comparison > 0 || (!upper && comparison == 0)) {
                return bound(sorted, low, probe, value, upper);
            }
            low = probe + 1;
        }
        return bound(sorted, low, to, value, upper);
    }

    private static int bound(char[] sorted, int from, int to, char value, boolean upper) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            int comparison = Character.compare(sorted[mid], value);
            if (comparison < 0 || (upper && comparison == 0)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int lowerBound(int[] sorted, int from, int to, int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, false);
    }

    public static int upperBound(int[] sorted, int from, int to, int value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, true);
    }

    /** Rejects invalid ranges, aliases and descending inputs before writing output. */
    public static void bounds(int[] sorted, int from, int to, int[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        output(sorted, queries, result, resultFrom, queryTo - queryFrom);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Integer.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        if (!coherentQueryPrefix(queries, queryFrom, queryTo)) {
            for (int i = queryFrom; i < queryTo; i++) {
                result[resultFrom + i - queryFrom] = bound(sorted, from, to, queries[i], upper);
            }
            return;
        }
        int previous = from;
        int previousAdvance = Integer.MAX_VALUE;
        for (int i = queryFrom; i < queryTo; i++) {
            if (i == queryFrom) {
                previous = bound(sorted, from, to, queries[i], upper);
            } else {
                int direction = Integer.compare(queries[i - 1], queries[i]);
                if (direction < 0) {
                    int start = previous;
                    previous = previousAdvance <= LOCAL_GALLOP_DISTANCE
                            ? gallopBound(sorted, previous, to, queries[i], upper)
                            : bound(sorted, previous, to, queries[i], upper);
                    previousAdvance = previous - start;
                } else if (direction > 0) {
                    previous = bound(sorted, from, previous, queries[i], upper);
                    previousAdvance = Integer.MAX_VALUE;
                }
            }
            result[resultFrom + i - queryFrom] = previous;
        }
    }

    /** Dispatch hint only: every answer still goes through an exact ordered-bound kernel. */
    private static boolean coherentQueryPrefix(int[] queries, int from, int to) {
        int direction = 0;
        int sample = Math.min(8, to - from);
        for (int i = 1; i < sample; i++) {
            int next = Integer.signum(Integer.compare(queries[from + i - 1], queries[from + i]));
            if (next != 0) {
                if (direction != 0 && direction != next) { return false; }
                direction = next;
            }
        }
        return true;
    }

    /** Trusted sorted range; exponential bracketing reuses the existing binary kernel. */
    static int gallopBound(int[] sorted, int from, int to, int value, boolean upper) {
        if (from == to) { return to; }
        int comparison = Integer.compare(sorted[from], value);
        if (comparison > 0 || (!upper && comparison == 0)) { return from; }
        int low = from + 1;
        // Long offsets prevent overflow at the signed-int array-index boundary.
        for (long offset = 1; (long) from + offset < to; offset = (offset << 1) + 1) {
            int probe = (int) ((long) from + offset);
            comparison = Integer.compare(sorted[probe], value);
            if (comparison > 0 || (!upper && comparison == 0)) {
                return bound(sorted, low, probe, value, upper);
            }
            low = probe + 1;
        }
        return bound(sorted, low, to, value, upper);
    }

    private static int bound(int[] sorted, int from, int to, int value, boolean upper) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            int comparison = Integer.compare(sorted[mid], value);
            if (comparison < 0 || (upper && comparison == 0)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int lowerBound(long[] sorted, int from, int to, long value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, false);
    }

    public static int upperBound(long[] sorted, int from, int to, long value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, true);
    }

    /** Rejects invalid ranges, aliases and descending inputs before writing output. */
    public static void bounds(long[] sorted, int from, int to, long[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        output(sorted, queries, result, resultFrom, queryTo - queryFrom);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Long.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        if (!coherentQueryPrefix(queries, queryFrom, queryTo)) {
            for (int i = queryFrom; i < queryTo; i++) {
                result[resultFrom + i - queryFrom] = bound(sorted, from, to, queries[i], upper);
            }
            return;
        }
        int previous = from;
        int previousAdvance = Integer.MAX_VALUE;
        for (int i = queryFrom; i < queryTo; i++) {
            if (i == queryFrom) {
                previous = bound(sorted, from, to, queries[i], upper);
            } else {
                int direction = Long.compare(queries[i - 1], queries[i]);
                if (direction < 0) {
                    int start = previous;
                    previous = previousAdvance <= LOCAL_GALLOP_DISTANCE
                            ? gallopBound(sorted, previous, to, queries[i], upper)
                            : bound(sorted, previous, to, queries[i], upper);
                    previousAdvance = previous - start;
                } else if (direction > 0) {
                    previous = bound(sorted, from, previous, queries[i], upper);
                    previousAdvance = Integer.MAX_VALUE;
                }
            }
            result[resultFrom + i - queryFrom] = previous;
        }
    }

    /** Dispatch hint only: every answer still goes through an exact ordered-bound kernel. */
    private static boolean coherentQueryPrefix(long[] queries, int from, int to) {
        int direction = 0;
        int sample = Math.min(8, to - from);
        for (int i = 1; i < sample; i++) {
            int next = Integer.signum(Long.compare(queries[from + i - 1], queries[from + i]));
            if (next != 0) {
                if (direction != 0 && direction != next) { return false; }
                direction = next;
            }
        }
        return true;
    }

    /** Trusted sorted range; exponential bracketing reuses the existing binary kernel. */
    static int gallopBound(long[] sorted, int from, int to, long value, boolean upper) {
        if (from == to) { return to; }
        int comparison = Long.compare(sorted[from], value);
        if (comparison > 0 || (!upper && comparison == 0)) { return from; }
        int low = from + 1;
        // Long offsets prevent overflow at the signed-int array-index boundary.
        for (long offset = 1; (long) from + offset < to; offset = (offset << 1) + 1) {
            int probe = (int) ((long) from + offset);
            comparison = Long.compare(sorted[probe], value);
            if (comparison > 0 || (!upper && comparison == 0)) {
                return bound(sorted, low, probe, value, upper);
            }
            low = probe + 1;
        }
        return bound(sorted, low, to, value, upper);
    }

    private static int bound(long[] sorted, int from, int to, long value, boolean upper) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            int comparison = Long.compare(sorted[mid], value);
            if (comparison < 0 || (upper && comparison == 0)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int lowerBound(float[] sorted, int from, int to, float value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, false);
    }

    public static int upperBound(float[] sorted, int from, int to, float value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, true);
    }

    /** Rejects invalid ranges, aliases and descending inputs before writing output. */
    public static void bounds(float[] sorted, int from, int to, float[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        output(sorted, queries, result, resultFrom, queryTo - queryFrom);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Float.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        if (!coherentQueryPrefix(queries, queryFrom, queryTo)) {
            for (int i = queryFrom; i < queryTo; i++) {
                result[resultFrom + i - queryFrom] = bound(sorted, from, to, queries[i], upper);
            }
            return;
        }
        int previous = from;
        int previousAdvance = Integer.MAX_VALUE;
        for (int i = queryFrom; i < queryTo; i++) {
            if (i == queryFrom) {
                previous = bound(sorted, from, to, queries[i], upper);
            } else {
                int direction = Float.compare(queries[i - 1], queries[i]);
                if (direction < 0) {
                    int start = previous;
                    previous = previousAdvance <= LOCAL_GALLOP_DISTANCE
                            ? gallopBound(sorted, previous, to, queries[i], upper)
                            : bound(sorted, previous, to, queries[i], upper);
                    previousAdvance = previous - start;
                } else if (direction > 0) {
                    previous = bound(sorted, from, previous, queries[i], upper);
                    previousAdvance = Integer.MAX_VALUE;
                }
            }
            result[resultFrom + i - queryFrom] = previous;
        }
    }

    /** Dispatch hint only: every answer still goes through an exact ordered-bound kernel. */
    private static boolean coherentQueryPrefix(float[] queries, int from, int to) {
        int direction = 0;
        int sample = Math.min(8, to - from);
        for (int i = 1; i < sample; i++) {
            int next = Integer.signum(Float.compare(queries[from + i - 1], queries[from + i]));
            if (next != 0) {
                if (direction != 0 && direction != next) { return false; }
                direction = next;
            }
        }
        return true;
    }

    /** Trusted sorted range; exponential bracketing reuses the existing binary kernel. */
    static int gallopBound(float[] sorted, int from, int to, float value, boolean upper) {
        if (from == to) { return to; }
        int comparison = Float.compare(sorted[from], value);
        if (comparison > 0 || (!upper && comparison == 0)) { return from; }
        int low = from + 1;
        // Long offsets prevent overflow at the signed-int array-index boundary.
        for (long offset = 1; (long) from + offset < to; offset = (offset << 1) + 1) {
            int probe = (int) ((long) from + offset);
            comparison = Float.compare(sorted[probe], value);
            if (comparison > 0 || (!upper && comparison == 0)) {
                return bound(sorted, low, probe, value, upper);
            }
            low = probe + 1;
        }
        return bound(sorted, low, to, value, upper);
    }

    private static int bound(float[] sorted, int from, int to, float value, boolean upper) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            int comparison = Float.compare(sorted[mid], value);
            if (comparison < 0 || (upper && comparison == 0)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int lowerBound(double[] sorted, int from, int to, double value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, false);
    }

    public static int upperBound(double[] sorted, int from, int to, double value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, true);
    }

    /** Rejects invalid ranges, aliases and descending inputs before writing output. */
    public static void bounds(double[] sorted, int from, int to, double[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        output(sorted, queries, result, resultFrom, queryTo - queryFrom);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Double.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        if (!coherentQueryPrefix(queries, queryFrom, queryTo)) {
            for (int i = queryFrom; i < queryTo; i++) {
                result[resultFrom + i - queryFrom] = bound(sorted, from, to, queries[i], upper);
            }
            return;
        }
        int previous = from;
        int previousAdvance = Integer.MAX_VALUE;
        for (int i = queryFrom; i < queryTo; i++) {
            if (i == queryFrom) {
                previous = bound(sorted, from, to, queries[i], upper);
            } else {
                int direction = Double.compare(queries[i - 1], queries[i]);
                if (direction < 0) {
                    int start = previous;
                    previous = previousAdvance <= LOCAL_GALLOP_DISTANCE
                            ? gallopBound(sorted, previous, to, queries[i], upper)
                            : bound(sorted, previous, to, queries[i], upper);
                    previousAdvance = previous - start;
                } else if (direction > 0) {
                    previous = bound(sorted, from, previous, queries[i], upper);
                    previousAdvance = Integer.MAX_VALUE;
                }
            }
            result[resultFrom + i - queryFrom] = previous;
        }
    }

    /** Dispatch hint only: every answer still goes through an exact ordered-bound kernel. */
    private static boolean coherentQueryPrefix(double[] queries, int from, int to) {
        int direction = 0;
        int sample = Math.min(8, to - from);
        for (int i = 1; i < sample; i++) {
            int next = Integer.signum(Double.compare(queries[from + i - 1], queries[from + i]));
            if (next != 0) {
                if (direction != 0 && direction != next) { return false; }
                direction = next;
            }
        }
        return true;
    }

    /** Trusted sorted range; exponential bracketing reuses the existing binary kernel. */
    static int gallopBound(double[] sorted, int from, int to, double value, boolean upper) {
        if (from == to) { return to; }
        int comparison = Double.compare(sorted[from], value);
        if (comparison > 0 || (!upper && comparison == 0)) { return from; }
        int low = from + 1;
        // Long offsets prevent overflow at the signed-int array-index boundary.
        for (long offset = 1; (long) from + offset < to; offset = (offset << 1) + 1) {
            int probe = (int) ((long) from + offset);
            comparison = Double.compare(sorted[probe], value);
            if (comparison > 0 || (!upper && comparison == 0)) {
                return bound(sorted, low, probe, value, upper);
            }
            low = probe + 1;
        }
        return bound(sorted, low, to, value, upper);
    }

    private static int bound(double[] sorted, int from, int to, double value, boolean upper) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            int comparison = Double.compare(sorted[mid], value);
            if (comparison < 0 || (upper && comparison == 0)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int lowerBound(boolean[] sorted, int from, int to, boolean value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, false);
    }

    public static int upperBound(boolean[] sorted, int from, int to, boolean value) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        return bound(sorted, from, to, value, true);
    }

    /** Rejects invalid ranges, aliases and descending inputs before writing output. */
    public static void bounds(boolean[] sorted, int from, int to, boolean[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(queries, "queries");
        Objects.checkFromToIndex(from, to, sorted.length);
        Objects.checkFromToIndex(queryFrom, queryTo, queries.length);
        output(sorted, queries, result, resultFrom, queryTo - queryFrom);
        for (int i = from + (from < to ? 1 : 0); i < to; i++) {
            if (Boolean.compare(sorted[i - 1], sorted[i]) > 0) {
                throw new IllegalArgumentException("input is not sorted");
            }
        }
        if (!coherentQueryPrefix(queries, queryFrom, queryTo)) {
            for (int i = queryFrom; i < queryTo; i++) {
                result[resultFrom + i - queryFrom] = bound(sorted, from, to, queries[i], upper);
            }
            return;
        }
        int previous = from;
        int previousAdvance = Integer.MAX_VALUE;
        for (int i = queryFrom; i < queryTo; i++) {
            if (i == queryFrom) {
                previous = bound(sorted, from, to, queries[i], upper);
            } else {
                int direction = Boolean.compare(queries[i - 1], queries[i]);
                if (direction < 0) {
                    int start = previous;
                    previous = previousAdvance <= LOCAL_GALLOP_DISTANCE
                            ? gallopBound(sorted, previous, to, queries[i], upper)
                            : bound(sorted, previous, to, queries[i], upper);
                    previousAdvance = previous - start;
                } else if (direction > 0) {
                    previous = bound(sorted, from, previous, queries[i], upper);
                    previousAdvance = Integer.MAX_VALUE;
                }
            }
            result[resultFrom + i - queryFrom] = previous;
        }
    }

    /** Dispatch hint only: every answer still goes through an exact ordered-bound kernel. */
    private static boolean coherentQueryPrefix(boolean[] queries, int from, int to) {
        int direction = 0;
        int sample = Math.min(8, to - from);
        for (int i = 1; i < sample; i++) {
            int next = Integer.signum(Boolean.compare(queries[from + i - 1], queries[from + i]));
            if (next != 0) {
                if (direction != 0 && direction != next) { return false; }
                direction = next;
            }
        }
        return true;
    }

    /** Trusted sorted range; exponential bracketing reuses the existing binary kernel. */
    static int gallopBound(boolean[] sorted, int from, int to, boolean value, boolean upper) {
        if (from == to) { return to; }
        int comparison = Boolean.compare(sorted[from], value);
        if (comparison > 0 || (!upper && comparison == 0)) { return from; }
        int low = from + 1;
        // Long offsets prevent overflow at the signed-int array-index boundary.
        for (long offset = 1; (long) from + offset < to; offset = (offset << 1) + 1) {
            int probe = (int) ((long) from + offset);
            comparison = Boolean.compare(sorted[probe], value);
            if (comparison > 0 || (!upper && comparison == 0)) {
                return bound(sorted, low, probe, value, upper);
            }
            low = probe + 1;
        }
        return bound(sorted, low, to, value, upper);
    }

    private static int bound(boolean[] sorted, int from, int to, boolean value, boolean upper) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            int comparison = Boolean.compare(sorted[mid], value);
            if (comparison < 0 || (upper && comparison == 0)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
}
