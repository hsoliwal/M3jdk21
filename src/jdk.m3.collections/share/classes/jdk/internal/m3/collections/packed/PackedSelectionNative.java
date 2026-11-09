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
 * Explicit JNI peers for primitive selection, compaction and set algebra. Host loads library. Same range/alias/value contracts as Java; no guarantee of equal exception messages, allocation or latency. JNI may copy arrays. No automatic native dispatch or callbacks.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedSelectionNative.java}
 * blob {@code 6ab335521a218d5a946010a2ac33d4b4b8f9af7a} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedSelectionNative {
    private PackedSelectionNative() { }

    /** Checked native heapify; loading is explicit and host-owned. */
    public static void heapify(boolean[] values, int from, int to, boolean minimum) {
        heapifyBoolean0(values, from, to, minimum);
    }
    private static native void heapifyBoolean0(boolean[] values, int from, int to, boolean minimum);

    /** Checked native ascending heapsort. */
    public static void sort(boolean[] values, int from, int to) { sortBoolean0(values, from, to); }
    private static native void sortBoolean0(boolean[] values, int from, int to);

    /** Checked native Top-K; decisions may be null to admit all. */
    public static int topK(boolean[] source, int from, int to, long[] decisions,
            boolean[] output, int outputFrom, int limit, boolean largest) {
        return topKBoolean0(source, from, to, decisions, output, outputFrom, limit, largest);
    }
    private static native int topKBoolean0(boolean[] source, int from, int to, long[] decisions,
            boolean[] output, int outputFrom, int limit, boolean largest);

    /** Checked native stable compaction; decisions must be nonnull. */
    public static int compact(boolean[] source, int from, int to, long[] decisions,
            boolean[] output, int outputFrom) {
        return compactBoolean0(source, from, to, decisions, output, outputFrom);
    }
    private static native int compactBoolean0(boolean[] source, int from, int to, long[] decisions,
            boolean[] output, int outputFrom);

    /** Checked native set algebra. Operation codes are defined by PackedPrimitiveSetAlgebra. */
    public static int combine(boolean[] left, int leftFrom, int leftTo,
            boolean[] right, int rightFrom, int rightTo, boolean[] output, int outputFrom, int operation) {
        return combineBoolean0(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }
    private static native int combineBoolean0(boolean[] left, int leftFrom, int leftTo,
            boolean[] right, int rightFrom, int rightTo, boolean[] output, int outputFrom, int operation);

    /** Checked native heapify; loading is explicit and host-owned. */
    public static void heapify(byte[] values, int from, int to, boolean minimum) {
        heapifyByte0(values, from, to, minimum);
    }
    private static native void heapifyByte0(byte[] values, int from, int to, boolean minimum);

    /** Checked native ascending heapsort. */
    public static void sort(byte[] values, int from, int to) { sortByte0(values, from, to); }
    private static native void sortByte0(byte[] values, int from, int to);

    /** Checked native Top-K; decisions may be null to admit all. */
    public static int topK(byte[] source, int from, int to, long[] decisions,
            byte[] output, int outputFrom, int limit, boolean largest) {
        return topKByte0(source, from, to, decisions, output, outputFrom, limit, largest);
    }
    private static native int topKByte0(byte[] source, int from, int to, long[] decisions,
            byte[] output, int outputFrom, int limit, boolean largest);

    /** Checked native stable compaction; decisions must be nonnull. */
    public static int compact(byte[] source, int from, int to, long[] decisions,
            byte[] output, int outputFrom) {
        return compactByte0(source, from, to, decisions, output, outputFrom);
    }
    private static native int compactByte0(byte[] source, int from, int to, long[] decisions,
            byte[] output, int outputFrom);

    /** Checked native set algebra. Operation codes are defined by PackedPrimitiveSetAlgebra. */
    public static int combine(byte[] left, int leftFrom, int leftTo,
            byte[] right, int rightFrom, int rightTo, byte[] output, int outputFrom, int operation) {
        return combineByte0(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }
    private static native int combineByte0(byte[] left, int leftFrom, int leftTo,
            byte[] right, int rightFrom, int rightTo, byte[] output, int outputFrom, int operation);

    /** Checked native heapify; loading is explicit and host-owned. */
    public static void heapify(short[] values, int from, int to, boolean minimum) {
        heapifyShort0(values, from, to, minimum);
    }
    private static native void heapifyShort0(short[] values, int from, int to, boolean minimum);

    /** Checked native ascending heapsort. */
    public static void sort(short[] values, int from, int to) { sortShort0(values, from, to); }
    private static native void sortShort0(short[] values, int from, int to);

    /** Checked native Top-K; decisions may be null to admit all. */
    public static int topK(short[] source, int from, int to, long[] decisions,
            short[] output, int outputFrom, int limit, boolean largest) {
        return topKShort0(source, from, to, decisions, output, outputFrom, limit, largest);
    }
    private static native int topKShort0(short[] source, int from, int to, long[] decisions,
            short[] output, int outputFrom, int limit, boolean largest);

    /** Checked native stable compaction; decisions must be nonnull. */
    public static int compact(short[] source, int from, int to, long[] decisions,
            short[] output, int outputFrom) {
        return compactShort0(source, from, to, decisions, output, outputFrom);
    }
    private static native int compactShort0(short[] source, int from, int to, long[] decisions,
            short[] output, int outputFrom);

    /** Checked native set algebra. Operation codes are defined by PackedPrimitiveSetAlgebra. */
    public static int combine(short[] left, int leftFrom, int leftTo,
            short[] right, int rightFrom, int rightTo, short[] output, int outputFrom, int operation) {
        return combineShort0(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }
    private static native int combineShort0(short[] left, int leftFrom, int leftTo,
            short[] right, int rightFrom, int rightTo, short[] output, int outputFrom, int operation);

    /** Checked native heapify; loading is explicit and host-owned. */
    public static void heapify(char[] values, int from, int to, boolean minimum) {
        heapifyChar0(values, from, to, minimum);
    }
    private static native void heapifyChar0(char[] values, int from, int to, boolean minimum);

    /** Checked native ascending heapsort. */
    public static void sort(char[] values, int from, int to) { sortChar0(values, from, to); }
    private static native void sortChar0(char[] values, int from, int to);

    /** Checked native Top-K; decisions may be null to admit all. */
    public static int topK(char[] source, int from, int to, long[] decisions,
            char[] output, int outputFrom, int limit, boolean largest) {
        return topKChar0(source, from, to, decisions, output, outputFrom, limit, largest);
    }
    private static native int topKChar0(char[] source, int from, int to, long[] decisions,
            char[] output, int outputFrom, int limit, boolean largest);

    /** Checked native stable compaction; decisions must be nonnull. */
    public static int compact(char[] source, int from, int to, long[] decisions,
            char[] output, int outputFrom) {
        return compactChar0(source, from, to, decisions, output, outputFrom);
    }
    private static native int compactChar0(char[] source, int from, int to, long[] decisions,
            char[] output, int outputFrom);

    /** Checked native set algebra. Operation codes are defined by PackedPrimitiveSetAlgebra. */
    public static int combine(char[] left, int leftFrom, int leftTo,
            char[] right, int rightFrom, int rightTo, char[] output, int outputFrom, int operation) {
        return combineChar0(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }
    private static native int combineChar0(char[] left, int leftFrom, int leftTo,
            char[] right, int rightFrom, int rightTo, char[] output, int outputFrom, int operation);

    /** Checked native heapify; loading is explicit and host-owned. */
    public static void heapify(int[] values, int from, int to, boolean minimum) {
        heapifyInt0(values, from, to, minimum);
    }
    private static native void heapifyInt0(int[] values, int from, int to, boolean minimum);

    /** Checked native ascending heapsort. */
    public static void sort(int[] values, int from, int to) { sortInt0(values, from, to); }
    private static native void sortInt0(int[] values, int from, int to);

    /** Checked native Top-K; decisions may be null to admit all. */
    public static int topK(int[] source, int from, int to, long[] decisions,
            int[] output, int outputFrom, int limit, boolean largest) {
        return topKInt0(source, from, to, decisions, output, outputFrom, limit, largest);
    }
    private static native int topKInt0(int[] source, int from, int to, long[] decisions,
            int[] output, int outputFrom, int limit, boolean largest);

    /** Checked native stable compaction; decisions must be nonnull. */
    public static int compact(int[] source, int from, int to, long[] decisions,
            int[] output, int outputFrom) {
        return compactInt0(source, from, to, decisions, output, outputFrom);
    }
    private static native int compactInt0(int[] source, int from, int to, long[] decisions,
            int[] output, int outputFrom);

    /** Checked native set algebra. Operation codes are defined by PackedPrimitiveSetAlgebra. */
    public static int combine(int[] left, int leftFrom, int leftTo,
            int[] right, int rightFrom, int rightTo, int[] output, int outputFrom, int operation) {
        return combineInt0(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }
    private static native int combineInt0(int[] left, int leftFrom, int leftTo,
            int[] right, int rightFrom, int rightTo, int[] output, int outputFrom, int operation);

    /** Checked native heapify; loading is explicit and host-owned. */
    public static void heapify(long[] values, int from, int to, boolean minimum) {
        heapifyLong0(values, from, to, minimum);
    }
    private static native void heapifyLong0(long[] values, int from, int to, boolean minimum);

    /** Checked native ascending heapsort. */
    public static void sort(long[] values, int from, int to) { sortLong0(values, from, to); }
    private static native void sortLong0(long[] values, int from, int to);

    /** Checked native Top-K; decisions may be null to admit all. */
    public static int topK(long[] source, int from, int to, long[] decisions,
            long[] output, int outputFrom, int limit, boolean largest) {
        return topKLong0(source, from, to, decisions, output, outputFrom, limit, largest);
    }
    private static native int topKLong0(long[] source, int from, int to, long[] decisions,
            long[] output, int outputFrom, int limit, boolean largest);

    /** Checked native stable compaction; decisions must be nonnull. */
    public static int compact(long[] source, int from, int to, long[] decisions,
            long[] output, int outputFrom) {
        return compactLong0(source, from, to, decisions, output, outputFrom);
    }
    private static native int compactLong0(long[] source, int from, int to, long[] decisions,
            long[] output, int outputFrom);

    /** Checked native set algebra. Operation codes are defined by PackedPrimitiveSetAlgebra. */
    public static int combine(long[] left, int leftFrom, int leftTo,
            long[] right, int rightFrom, int rightTo, long[] output, int outputFrom, int operation) {
        return combineLong0(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }
    private static native int combineLong0(long[] left, int leftFrom, int leftTo,
            long[] right, int rightFrom, int rightTo, long[] output, int outputFrom, int operation);

    /** Checked native heapify; loading is explicit and host-owned. */
    public static void heapify(float[] values, int from, int to, boolean minimum) {
        heapifyFloat0(values, from, to, minimum);
    }
    private static native void heapifyFloat0(float[] values, int from, int to, boolean minimum);

    /** Checked native ascending heapsort. */
    public static void sort(float[] values, int from, int to) { sortFloat0(values, from, to); }
    private static native void sortFloat0(float[] values, int from, int to);

    /** Checked native Top-K; decisions may be null to admit all. */
    public static int topK(float[] source, int from, int to, long[] decisions,
            float[] output, int outputFrom, int limit, boolean largest) {
        return topKFloat0(source, from, to, decisions, output, outputFrom, limit, largest);
    }
    private static native int topKFloat0(float[] source, int from, int to, long[] decisions,
            float[] output, int outputFrom, int limit, boolean largest);

    /** Checked native stable compaction; decisions must be nonnull. */
    public static int compact(float[] source, int from, int to, long[] decisions,
            float[] output, int outputFrom) {
        return compactFloat0(source, from, to, decisions, output, outputFrom);
    }
    private static native int compactFloat0(float[] source, int from, int to, long[] decisions,
            float[] output, int outputFrom);

    /** Checked native set algebra. Operation codes are defined by PackedPrimitiveSetAlgebra. */
    public static int combine(float[] left, int leftFrom, int leftTo,
            float[] right, int rightFrom, int rightTo, float[] output, int outputFrom, int operation) {
        return combineFloat0(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }
    private static native int combineFloat0(float[] left, int leftFrom, int leftTo,
            float[] right, int rightFrom, int rightTo, float[] output, int outputFrom, int operation);

    /** Checked native heapify; loading is explicit and host-owned. */
    public static void heapify(double[] values, int from, int to, boolean minimum) {
        heapifyDouble0(values, from, to, minimum);
    }
    private static native void heapifyDouble0(double[] values, int from, int to, boolean minimum);

    /** Checked native ascending heapsort. */
    public static void sort(double[] values, int from, int to) { sortDouble0(values, from, to); }
    private static native void sortDouble0(double[] values, int from, int to);

    /** Checked native Top-K; decisions may be null to admit all. */
    public static int topK(double[] source, int from, int to, long[] decisions,
            double[] output, int outputFrom, int limit, boolean largest) {
        return topKDouble0(source, from, to, decisions, output, outputFrom, limit, largest);
    }
    private static native int topKDouble0(double[] source, int from, int to, long[] decisions,
            double[] output, int outputFrom, int limit, boolean largest);

    /** Checked native stable compaction; decisions must be nonnull. */
    public static int compact(double[] source, int from, int to, long[] decisions,
            double[] output, int outputFrom) {
        return compactDouble0(source, from, to, decisions, output, outputFrom);
    }
    private static native int compactDouble0(double[] source, int from, int to, long[] decisions,
            double[] output, int outputFrom);

    /** Checked native set algebra. Operation codes are defined by PackedPrimitiveSetAlgebra. */
    public static int combine(double[] left, int leftFrom, int leftTo,
            double[] right, int rightFrom, int rightTo, double[] output, int outputFrom, int operation) {
        return combineDouble0(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }
    private static native int combineDouble0(double[] left, int leftFrom, int leftTo,
            double[] right, int rightFrom, int rightTo, double[] output, int outputFrom, int operation);
}
