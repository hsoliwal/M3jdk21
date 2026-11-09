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

/**
 * Explicit JNI companions. Loading remains the host's responsibility; this class has no loader,
 * fallback, global native pointer, retained entry, worker thread, or automatic backend selection.
 * Contracts match the checked Java operators. JNI may bulk-copy arrays per call.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedArrayNative.java}
 * blob {@code c7ff504678225567b29235a74a301e0195a0f364} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedArrayNative {
    private PackedArrayNative() { }

    public static void bounds(byte[] sorted, int from, int to, byte[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        boundsByte0(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, upper);
    }

    private static native void boundsByte0(byte[] sorted, int from, int to, byte[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper);

    public static void bounds(short[] sorted, int from, int to, short[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        boundsShort0(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, upper);
    }

    private static native void boundsShort0(short[] sorted, int from, int to, short[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper);

    public static void bounds(char[] sorted, int from, int to, char[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        boundsChar0(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, upper);
    }

    private static native void boundsChar0(char[] sorted, int from, int to, char[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper);

    public static void bounds(int[] sorted, int from, int to, int[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        boundsInt0(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, upper);
    }

    private static native void boundsInt0(int[] sorted, int from, int to, int[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper);

    public static void bounds(long[] sorted, int from, int to, long[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        boundsLong0(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, upper);
    }

    private static native void boundsLong0(long[] sorted, int from, int to, long[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper);

    public static void bounds(float[] sorted, int from, int to, float[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        boundsFloat0(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, upper);
    }

    private static native void boundsFloat0(float[] sorted, int from, int to, float[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper);

    public static void bounds(double[] sorted, int from, int to, double[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        boundsDouble0(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, upper);
    }

    private static native void boundsDouble0(double[] sorted, int from, int to, double[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper);

    public static void bounds(boolean[] sorted, int from, int to, boolean[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper) {
        boundsBoolean0(sorted, from, to, queries, queryFrom, queryTo, result, resultFrom, upper);
    }

    private static native void boundsBoolean0(boolean[] sorted, int from, int to, boolean[] queries,
            int queryFrom, int queryTo, int[] result, int resultFrom, boolean upper);

    public static long equalRangePacked(
            byte[] sorted, int from, int to, byte value) {
        return equalRangeByte0(sorted, from, to, value);
    }

    private static native long equalRangeByte0(
            byte[] sorted, int from, int to, byte value);

    public static int equalRangesPacked(byte[] sorted, int from, int to, byte[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        return equalRangesByte0(
                sorted, from, to, queries, queryFrom, queryTo, result, resultFrom);
    }

    private static native int equalRangesByte0(byte[] sorted, int from, int to, byte[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom);

    public static long equalRangePacked(
            short[] sorted, int from, int to, short value) {
        return equalRangeShort0(sorted, from, to, value);
    }

    private static native long equalRangeShort0(
            short[] sorted, int from, int to, short value);

    public static int equalRangesPacked(short[] sorted, int from, int to, short[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        return equalRangesShort0(
                sorted, from, to, queries, queryFrom, queryTo, result, resultFrom);
    }

    private static native int equalRangesShort0(short[] sorted, int from, int to, short[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom);

    public static long equalRangePacked(
            char[] sorted, int from, int to, char value) {
        return equalRangeChar0(sorted, from, to, value);
    }

    private static native long equalRangeChar0(
            char[] sorted, int from, int to, char value);

    public static int equalRangesPacked(char[] sorted, int from, int to, char[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        return equalRangesChar0(
                sorted, from, to, queries, queryFrom, queryTo, result, resultFrom);
    }

    private static native int equalRangesChar0(char[] sorted, int from, int to, char[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom);

    public static long equalRangePacked(
            int[] sorted, int from, int to, int value) {
        return equalRangeInt0(sorted, from, to, value);
    }

    private static native long equalRangeInt0(
            int[] sorted, int from, int to, int value);

    public static int equalRangesPacked(int[] sorted, int from, int to, int[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        return equalRangesInt0(
                sorted, from, to, queries, queryFrom, queryTo, result, resultFrom);
    }

    private static native int equalRangesInt0(int[] sorted, int from, int to, int[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom);

    public static long equalRangePacked(
            long[] sorted, int from, int to, long value) {
        return equalRangeLong0(sorted, from, to, value);
    }

    private static native long equalRangeLong0(
            long[] sorted, int from, int to, long value);

    public static int equalRangesPacked(long[] sorted, int from, int to, long[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        return equalRangesLong0(
                sorted, from, to, queries, queryFrom, queryTo, result, resultFrom);
    }

    private static native int equalRangesLong0(long[] sorted, int from, int to, long[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom);

    public static long equalRangePacked(
            float[] sorted, int from, int to, float value) {
        return equalRangeFloat0(sorted, from, to, value);
    }

    private static native long equalRangeFloat0(
            float[] sorted, int from, int to, float value);

    public static int equalRangesPacked(float[] sorted, int from, int to, float[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        return equalRangesFloat0(
                sorted, from, to, queries, queryFrom, queryTo, result, resultFrom);
    }

    private static native int equalRangesFloat0(float[] sorted, int from, int to, float[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom);

    public static long equalRangePacked(
            double[] sorted, int from, int to, double value) {
        return equalRangeDouble0(sorted, from, to, value);
    }

    private static native long equalRangeDouble0(
            double[] sorted, int from, int to, double value);

    public static int equalRangesPacked(double[] sorted, int from, int to, double[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        return equalRangesDouble0(
                sorted, from, to, queries, queryFrom, queryTo, result, resultFrom);
    }

    private static native int equalRangesDouble0(double[] sorted, int from, int to, double[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom);

    public static long equalRangePacked(
            boolean[] sorted, int from, int to, boolean value) {
        return equalRangeBoolean0(sorted, from, to, value);
    }

    private static native long equalRangeBoolean0(
            boolean[] sorted, int from, int to, boolean value);

    public static int equalRangesPacked(boolean[] sorted, int from, int to, boolean[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom) {
        return equalRangesBoolean0(
                sorted, from, to, queries, queryFrom, queryTo, result, resultFrom);
    }

    private static native int equalRangesBoolean0(boolean[] sorted, int from, int to, boolean[] queries,
            int queryFrom, int queryTo, long[] result, int resultFrom);

    // Exact lookup is a projection of the canonical bound/equal-range facts.

    public static int exactIndex(byte[] sorted, int from, int to, byte value) {
        long range = equalRangePacked(sorted, from, to, value);
        int lower = PackedPrimitiveArrayOps.equalRangeLower(range);
        return lower < PackedPrimitiveArrayOps.equalRangeUpper(range) ? lower : -1;
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
        int lower = PackedPrimitiveArrayOps.equalRangeLower(range);
        return lower < PackedPrimitiveArrayOps.equalRangeUpper(range) ? lower : -1;
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
        int lower = PackedPrimitiveArrayOps.equalRangeLower(range);
        return lower < PackedPrimitiveArrayOps.equalRangeUpper(range) ? lower : -1;
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
        int lower = PackedPrimitiveArrayOps.equalRangeLower(range);
        return lower < PackedPrimitiveArrayOps.equalRangeUpper(range) ? lower : -1;
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
        int lower = PackedPrimitiveArrayOps.equalRangeLower(range);
        return lower < PackedPrimitiveArrayOps.equalRangeUpper(range) ? lower : -1;
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
        int lower = PackedPrimitiveArrayOps.equalRangeLower(range);
        return lower < PackedPrimitiveArrayOps.equalRangeUpper(range) ? lower : -1;
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
        int lower = PackedPrimitiveArrayOps.equalRangeLower(range);
        return lower < PackedPrimitiveArrayOps.equalRangeUpper(range) ? lower : -1;
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
        int lower = PackedPrimitiveArrayOps.equalRangeLower(range);
        return lower < PackedPrimitiveArrayOps.equalRangeUpper(range) ? lower : -1;
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

    public static native int setOperation(long[] left, int leftFrom, int leftTo,
            long[] right, int rightFrom, int rightTo, long[] output, int outputFrom, int operation);

    public static native void heapify(long[] heap, int size, boolean minHeap);
}
