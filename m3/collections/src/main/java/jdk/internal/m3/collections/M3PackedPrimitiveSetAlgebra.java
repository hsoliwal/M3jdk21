/*******************************************************************************
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
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Adaptive sorted set algebra on caller-owned primitive arrays; no per-entry objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedPrimitiveSetAlgebra.java}
 * blob {@code eff909e991d01810c604f3d8ad7553e6858e4847} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedPrimitiveSetAlgebra {
    private M3PackedPrimitiveSetAlgebra() { }
    public static final int UNION = 0;
    public static final int INTERSECTION = 1;
    public static final int DIFFERENCE = 2;
    public static final int SYMMETRIC_DIFFERENCE = 3;

    /**
     * Set algebra on ascending ranges, collapsing equal values and preferring the left representative.
     * Null/range/opcode/alias/order/capacity checks precede every output write. No input may change
     * concurrently. Float/double equality uses total comparison: NaNs equal; signed zeros distinct.
     */
    public static int combine(boolean[] left, int leftFrom, int leftTo,
            boolean[] right, int rightFrom, int rightTo, boolean[] output, int outputFrom, int operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        if (operation < UNION || operation > SYMMETRIC_DIFFERENCE) { throw new IllegalArgumentException("operation"); }
        if (output == left || output == right) { throw new IllegalArgumentException("unsafe alias"); }
        sorted(left, leftFrom, leftTo);
        sorted(right, rightFrom, rightTo);
        long count = merge(left, leftFrom, leftTo, right, rightFrom, rightTo, null, 0, operation);
        if (count > Integer.MAX_VALUE) { throw new IllegalArgumentException("result too large"); }
        Objects.checkFromIndexSize(outputFrom, (int) count, output.length);
        return (int) merge(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }

    private static void sorted(boolean[] values, int from, int to) {
        for (int i = from; i < to && i + 1 < to; i++) {
            if (Boolean.compare(values[i], values[i + 1]) > 0) { throw new IllegalArgumentException("unsorted input"); }
        }
    }

    private static long merge(boolean[] left, int i, int leftTo, boolean[] right, int j, int rightTo,
            boolean[] output, int outputFrom, int operation) {
        long count = 0;
        while (i < leftTo || j < rightTo) {
            if ((operation == INTERSECTION && (i == leftTo || j == rightTo))
                    || (operation == DIFFERENCE && i == leftTo)) { break; }
            boolean value;
            boolean emit;
            if (j == rightTo || (i < leftTo && Boolean.compare(left[i], right[j]) < 0)) {
                if (operation == INTERSECTION) {
                    i = M3PackedPrimitiveArrayOps.gallopBound(left, i + 1, leftTo, right[j], false);
                    continue;
                }
                value = left[i]; emit = operation != INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
            } else if (i == leftTo || Boolean.compare(right[j], left[i]) < 0) {
                if (operation == INTERSECTION || operation == DIFFERENCE) {
                    j = M3PackedPrimitiveArrayOps.gallopBound(right, j + 1, rightTo, left[i], false);
                    continue;
                }
                value = right[j]; emit = operation == UNION || operation == SYMMETRIC_DIFFERENCE;
                j = afterEqual(right, j, rightTo, value);
            } else {
                value = left[i]; emit = operation == UNION || operation == INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
                j = afterEqual(right, j, rightTo, value);
            }
            if (emit) {
                if (output != null) { output[outputFrom + (int) count] = value; }
                count++;
            }
        }
        return count;
    }

    private static int afterEqual(boolean[] values, int index, int to, boolean value) {
        int next = index + 1;
        if (next == to || Boolean.compare(values[next], value) != 0) { return next; }
        return M3PackedPrimitiveArrayOps.gallopBound(values, next, to, value, true);
    }

    /**
     * Set algebra on ascending ranges, collapsing equal values and preferring the left representative.
     * Null/range/opcode/alias/order/capacity checks precede every output write. No input may change
     * concurrently. Float/double equality uses total comparison: NaNs equal; signed zeros distinct.
     */
    public static int combine(byte[] left, int leftFrom, int leftTo,
            byte[] right, int rightFrom, int rightTo, byte[] output, int outputFrom, int operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        if (operation < UNION || operation > SYMMETRIC_DIFFERENCE) { throw new IllegalArgumentException("operation"); }
        if (output == left || output == right) { throw new IllegalArgumentException("unsafe alias"); }
        sorted(left, leftFrom, leftTo);
        sorted(right, rightFrom, rightTo);
        long count = merge(left, leftFrom, leftTo, right, rightFrom, rightTo, null, 0, operation);
        if (count > Integer.MAX_VALUE) { throw new IllegalArgumentException("result too large"); }
        Objects.checkFromIndexSize(outputFrom, (int) count, output.length);
        return (int) merge(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }

    private static void sorted(byte[] values, int from, int to) {
        for (int i = from; i < to && i + 1 < to; i++) {
            if (Byte.compare(values[i], values[i + 1]) > 0) { throw new IllegalArgumentException("unsorted input"); }
        }
    }

    private static long merge(byte[] left, int i, int leftTo, byte[] right, int j, int rightTo,
            byte[] output, int outputFrom, int operation) {
        long count = 0;
        while (i < leftTo || j < rightTo) {
            if ((operation == INTERSECTION && (i == leftTo || j == rightTo))
                    || (operation == DIFFERENCE && i == leftTo)) { break; }
            byte value;
            boolean emit;
            if (j == rightTo || (i < leftTo && Byte.compare(left[i], right[j]) < 0)) {
                if (operation == INTERSECTION) {
                    i = M3PackedPrimitiveArrayOps.gallopBound(left, i + 1, leftTo, right[j], false);
                    continue;
                }
                value = left[i]; emit = operation != INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
            } else if (i == leftTo || Byte.compare(right[j], left[i]) < 0) {
                if (operation == INTERSECTION || operation == DIFFERENCE) {
                    j = M3PackedPrimitiveArrayOps.gallopBound(right, j + 1, rightTo, left[i], false);
                    continue;
                }
                value = right[j]; emit = operation == UNION || operation == SYMMETRIC_DIFFERENCE;
                j = afterEqual(right, j, rightTo, value);
            } else {
                value = left[i]; emit = operation == UNION || operation == INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
                j = afterEqual(right, j, rightTo, value);
            }
            if (emit) {
                if (output != null) { output[outputFrom + (int) count] = value; }
                count++;
            }
        }
        return count;
    }

    private static int afterEqual(byte[] values, int index, int to, byte value) {
        int next = index + 1;
        if (next == to || Byte.compare(values[next], value) != 0) { return next; }
        return M3PackedPrimitiveArrayOps.gallopBound(values, next, to, value, true);
    }

    /**
     * Set algebra on ascending ranges, collapsing equal values and preferring the left representative.
     * Null/range/opcode/alias/order/capacity checks precede every output write. No input may change
     * concurrently. Float/double equality uses total comparison: NaNs equal; signed zeros distinct.
     */
    public static int combine(short[] left, int leftFrom, int leftTo,
            short[] right, int rightFrom, int rightTo, short[] output, int outputFrom, int operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        if (operation < UNION || operation > SYMMETRIC_DIFFERENCE) { throw new IllegalArgumentException("operation"); }
        if (output == left || output == right) { throw new IllegalArgumentException("unsafe alias"); }
        sorted(left, leftFrom, leftTo);
        sorted(right, rightFrom, rightTo);
        long count = merge(left, leftFrom, leftTo, right, rightFrom, rightTo, null, 0, operation);
        if (count > Integer.MAX_VALUE) { throw new IllegalArgumentException("result too large"); }
        Objects.checkFromIndexSize(outputFrom, (int) count, output.length);
        return (int) merge(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }

    private static void sorted(short[] values, int from, int to) {
        for (int i = from; i < to && i + 1 < to; i++) {
            if (Short.compare(values[i], values[i + 1]) > 0) { throw new IllegalArgumentException("unsorted input"); }
        }
    }

    private static long merge(short[] left, int i, int leftTo, short[] right, int j, int rightTo,
            short[] output, int outputFrom, int operation) {
        long count = 0;
        while (i < leftTo || j < rightTo) {
            if ((operation == INTERSECTION && (i == leftTo || j == rightTo))
                    || (operation == DIFFERENCE && i == leftTo)) { break; }
            short value;
            boolean emit;
            if (j == rightTo || (i < leftTo && Short.compare(left[i], right[j]) < 0)) {
                if (operation == INTERSECTION) {
                    i = M3PackedPrimitiveArrayOps.gallopBound(left, i + 1, leftTo, right[j], false);
                    continue;
                }
                value = left[i]; emit = operation != INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
            } else if (i == leftTo || Short.compare(right[j], left[i]) < 0) {
                if (operation == INTERSECTION || operation == DIFFERENCE) {
                    j = M3PackedPrimitiveArrayOps.gallopBound(right, j + 1, rightTo, left[i], false);
                    continue;
                }
                value = right[j]; emit = operation == UNION || operation == SYMMETRIC_DIFFERENCE;
                j = afterEqual(right, j, rightTo, value);
            } else {
                value = left[i]; emit = operation == UNION || operation == INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
                j = afterEqual(right, j, rightTo, value);
            }
            if (emit) {
                if (output != null) { output[outputFrom + (int) count] = value; }
                count++;
            }
        }
        return count;
    }

    private static int afterEqual(short[] values, int index, int to, short value) {
        int next = index + 1;
        if (next == to || Short.compare(values[next], value) != 0) { return next; }
        return M3PackedPrimitiveArrayOps.gallopBound(values, next, to, value, true);
    }

    /**
     * Set algebra on ascending ranges, collapsing equal values and preferring the left representative.
     * Null/range/opcode/alias/order/capacity checks precede every output write. No input may change
     * concurrently. Float/double equality uses total comparison: NaNs equal; signed zeros distinct.
     */
    public static int combine(char[] left, int leftFrom, int leftTo,
            char[] right, int rightFrom, int rightTo, char[] output, int outputFrom, int operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        if (operation < UNION || operation > SYMMETRIC_DIFFERENCE) { throw new IllegalArgumentException("operation"); }
        if (output == left || output == right) { throw new IllegalArgumentException("unsafe alias"); }
        sorted(left, leftFrom, leftTo);
        sorted(right, rightFrom, rightTo);
        long count = merge(left, leftFrom, leftTo, right, rightFrom, rightTo, null, 0, operation);
        if (count > Integer.MAX_VALUE) { throw new IllegalArgumentException("result too large"); }
        Objects.checkFromIndexSize(outputFrom, (int) count, output.length);
        return (int) merge(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }

    private static void sorted(char[] values, int from, int to) {
        for (int i = from; i < to && i + 1 < to; i++) {
            if (Character.compare(values[i], values[i + 1]) > 0) { throw new IllegalArgumentException("unsorted input"); }
        }
    }

    private static long merge(char[] left, int i, int leftTo, char[] right, int j, int rightTo,
            char[] output, int outputFrom, int operation) {
        long count = 0;
        while (i < leftTo || j < rightTo) {
            if ((operation == INTERSECTION && (i == leftTo || j == rightTo))
                    || (operation == DIFFERENCE && i == leftTo)) { break; }
            char value;
            boolean emit;
            if (j == rightTo || (i < leftTo && Character.compare(left[i], right[j]) < 0)) {
                if (operation == INTERSECTION) {
                    i = M3PackedPrimitiveArrayOps.gallopBound(left, i + 1, leftTo, right[j], false);
                    continue;
                }
                value = left[i]; emit = operation != INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
            } else if (i == leftTo || Character.compare(right[j], left[i]) < 0) {
                if (operation == INTERSECTION || operation == DIFFERENCE) {
                    j = M3PackedPrimitiveArrayOps.gallopBound(right, j + 1, rightTo, left[i], false);
                    continue;
                }
                value = right[j]; emit = operation == UNION || operation == SYMMETRIC_DIFFERENCE;
                j = afterEqual(right, j, rightTo, value);
            } else {
                value = left[i]; emit = operation == UNION || operation == INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
                j = afterEqual(right, j, rightTo, value);
            }
            if (emit) {
                if (output != null) { output[outputFrom + (int) count] = value; }
                count++;
            }
        }
        return count;
    }

    private static int afterEqual(char[] values, int index, int to, char value) {
        int next = index + 1;
        if (next == to || Character.compare(values[next], value) != 0) { return next; }
        return M3PackedPrimitiveArrayOps.gallopBound(values, next, to, value, true);
    }

    /**
     * Set algebra on ascending ranges, collapsing equal values and preferring the left representative.
     * Null/range/opcode/alias/order/capacity checks precede every output write. No input may change
     * concurrently. Float/double equality uses total comparison: NaNs equal; signed zeros distinct.
     */
    public static int combine(int[] left, int leftFrom, int leftTo,
            int[] right, int rightFrom, int rightTo, int[] output, int outputFrom, int operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        if (operation < UNION || operation > SYMMETRIC_DIFFERENCE) { throw new IllegalArgumentException("operation"); }
        if (output == left || output == right) { throw new IllegalArgumentException("unsafe alias"); }
        sorted(left, leftFrom, leftTo);
        sorted(right, rightFrom, rightTo);
        long count = merge(left, leftFrom, leftTo, right, rightFrom, rightTo, null, 0, operation);
        if (count > Integer.MAX_VALUE) { throw new IllegalArgumentException("result too large"); }
        Objects.checkFromIndexSize(outputFrom, (int) count, output.length);
        return (int) merge(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }

    private static void sorted(int[] values, int from, int to) {
        for (int i = from; i < to && i + 1 < to; i++) {
            if (Integer.compare(values[i], values[i + 1]) > 0) { throw new IllegalArgumentException("unsorted input"); }
        }
    }

    private static long merge(int[] left, int i, int leftTo, int[] right, int j, int rightTo,
            int[] output, int outputFrom, int operation) {
        long count = 0;
        while (i < leftTo || j < rightTo) {
            if ((operation == INTERSECTION && (i == leftTo || j == rightTo))
                    || (operation == DIFFERENCE && i == leftTo)) { break; }
            int value;
            boolean emit;
            if (j == rightTo || (i < leftTo && Integer.compare(left[i], right[j]) < 0)) {
                if (operation == INTERSECTION) {
                    i = M3PackedPrimitiveArrayOps.gallopBound(left, i + 1, leftTo, right[j], false);
                    continue;
                }
                value = left[i]; emit = operation != INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
            } else if (i == leftTo || Integer.compare(right[j], left[i]) < 0) {
                if (operation == INTERSECTION || operation == DIFFERENCE) {
                    j = M3PackedPrimitiveArrayOps.gallopBound(right, j + 1, rightTo, left[i], false);
                    continue;
                }
                value = right[j]; emit = operation == UNION || operation == SYMMETRIC_DIFFERENCE;
                j = afterEqual(right, j, rightTo, value);
            } else {
                value = left[i]; emit = operation == UNION || operation == INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
                j = afterEqual(right, j, rightTo, value);
            }
            if (emit) {
                if (output != null) { output[outputFrom + (int) count] = value; }
                count++;
            }
        }
        return count;
    }

    private static int afterEqual(int[] values, int index, int to, int value) {
        int next = index + 1;
        if (next == to || Integer.compare(values[next], value) != 0) { return next; }
        return M3PackedPrimitiveArrayOps.gallopBound(values, next, to, value, true);
    }

    /**
     * Set algebra on ascending ranges, collapsing equal values and preferring the left representative.
     * Null/range/opcode/alias/order/capacity checks precede every output write. No input may change
     * concurrently. Float/double equality uses total comparison: NaNs equal; signed zeros distinct.
     */
    public static int combine(long[] left, int leftFrom, int leftTo,
            long[] right, int rightFrom, int rightTo, long[] output, int outputFrom, int operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        if (operation < UNION || operation > SYMMETRIC_DIFFERENCE) { throw new IllegalArgumentException("operation"); }
        if (output == left || output == right) { throw new IllegalArgumentException("unsafe alias"); }
        sorted(left, leftFrom, leftTo);
        sorted(right, rightFrom, rightTo);
        long count = merge(left, leftFrom, leftTo, right, rightFrom, rightTo, null, 0, operation);
        if (count > Integer.MAX_VALUE) { throw new IllegalArgumentException("result too large"); }
        Objects.checkFromIndexSize(outputFrom, (int) count, output.length);
        return (int) merge(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }

    private static void sorted(long[] values, int from, int to) {
        for (int i = from; i < to && i + 1 < to; i++) {
            if (Long.compare(values[i], values[i + 1]) > 0) { throw new IllegalArgumentException("unsorted input"); }
        }
    }

    private static long merge(long[] left, int i, int leftTo, long[] right, int j, int rightTo,
            long[] output, int outputFrom, int operation) {
        long count = 0;
        while (i < leftTo || j < rightTo) {
            if ((operation == INTERSECTION && (i == leftTo || j == rightTo))
                    || (operation == DIFFERENCE && i == leftTo)) { break; }
            long value;
            boolean emit;
            if (j == rightTo || (i < leftTo && Long.compare(left[i], right[j]) < 0)) {
                if (operation == INTERSECTION) {
                    i = M3PackedPrimitiveArrayOps.gallopBound(left, i + 1, leftTo, right[j], false);
                    continue;
                }
                value = left[i]; emit = operation != INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
            } else if (i == leftTo || Long.compare(right[j], left[i]) < 0) {
                if (operation == INTERSECTION || operation == DIFFERENCE) {
                    j = M3PackedPrimitiveArrayOps.gallopBound(right, j + 1, rightTo, left[i], false);
                    continue;
                }
                value = right[j]; emit = operation == UNION || operation == SYMMETRIC_DIFFERENCE;
                j = afterEqual(right, j, rightTo, value);
            } else {
                value = left[i]; emit = operation == UNION || operation == INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
                j = afterEqual(right, j, rightTo, value);
            }
            if (emit) {
                if (output != null) { output[outputFrom + (int) count] = value; }
                count++;
            }
        }
        return count;
    }

    private static int afterEqual(long[] values, int index, int to, long value) {
        int next = index + 1;
        if (next == to || Long.compare(values[next], value) != 0) { return next; }
        return M3PackedPrimitiveArrayOps.gallopBound(values, next, to, value, true);
    }

    /**
     * Set algebra on ascending ranges, collapsing equal values and preferring the left representative.
     * Null/range/opcode/alias/order/capacity checks precede every output write. No input may change
     * concurrently. Float/double equality uses total comparison: NaNs equal; signed zeros distinct.
     */
    public static int combine(float[] left, int leftFrom, int leftTo,
            float[] right, int rightFrom, int rightTo, float[] output, int outputFrom, int operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        if (operation < UNION || operation > SYMMETRIC_DIFFERENCE) { throw new IllegalArgumentException("operation"); }
        if (output == left || output == right) { throw new IllegalArgumentException("unsafe alias"); }
        sorted(left, leftFrom, leftTo);
        sorted(right, rightFrom, rightTo);
        long count = merge(left, leftFrom, leftTo, right, rightFrom, rightTo, null, 0, operation);
        if (count > Integer.MAX_VALUE) { throw new IllegalArgumentException("result too large"); }
        Objects.checkFromIndexSize(outputFrom, (int) count, output.length);
        return (int) merge(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }

    private static void sorted(float[] values, int from, int to) {
        for (int i = from; i < to && i + 1 < to; i++) {
            if (Float.compare(values[i], values[i + 1]) > 0) { throw new IllegalArgumentException("unsorted input"); }
        }
    }

    private static long merge(float[] left, int i, int leftTo, float[] right, int j, int rightTo,
            float[] output, int outputFrom, int operation) {
        long count = 0;
        while (i < leftTo || j < rightTo) {
            if ((operation == INTERSECTION && (i == leftTo || j == rightTo))
                    || (operation == DIFFERENCE && i == leftTo)) { break; }
            float value;
            boolean emit;
            if (j == rightTo || (i < leftTo && Float.compare(left[i], right[j]) < 0)) {
                if (operation == INTERSECTION) {
                    i = M3PackedPrimitiveArrayOps.gallopBound(left, i + 1, leftTo, right[j], false);
                    continue;
                }
                value = left[i]; emit = operation != INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
            } else if (i == leftTo || Float.compare(right[j], left[i]) < 0) {
                if (operation == INTERSECTION || operation == DIFFERENCE) {
                    j = M3PackedPrimitiveArrayOps.gallopBound(right, j + 1, rightTo, left[i], false);
                    continue;
                }
                value = right[j]; emit = operation == UNION || operation == SYMMETRIC_DIFFERENCE;
                j = afterEqual(right, j, rightTo, value);
            } else {
                value = left[i]; emit = operation == UNION || operation == INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
                j = afterEqual(right, j, rightTo, value);
            }
            if (emit) {
                if (output != null) { output[outputFrom + (int) count] = value; }
                count++;
            }
        }
        return count;
    }

    private static int afterEqual(float[] values, int index, int to, float value) {
        int next = index + 1;
        if (next == to || Float.compare(values[next], value) != 0) { return next; }
        return M3PackedPrimitiveArrayOps.gallopBound(values, next, to, value, true);
    }

    /**
     * Set algebra on ascending ranges, collapsing equal values and preferring the left representative.
     * Null/range/opcode/alias/order/capacity checks precede every output write. No input may change
     * concurrently. Float/double equality uses total comparison: NaNs equal; signed zeros distinct.
     */
    public static int combine(double[] left, int leftFrom, int leftTo,
            double[] right, int rightFrom, int rightTo, double[] output, int outputFrom, int operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(output, "output");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        if (operation < UNION || operation > SYMMETRIC_DIFFERENCE) { throw new IllegalArgumentException("operation"); }
        if (output == left || output == right) { throw new IllegalArgumentException("unsafe alias"); }
        sorted(left, leftFrom, leftTo);
        sorted(right, rightFrom, rightTo);
        long count = merge(left, leftFrom, leftTo, right, rightFrom, rightTo, null, 0, operation);
        if (count > Integer.MAX_VALUE) { throw new IllegalArgumentException("result too large"); }
        Objects.checkFromIndexSize(outputFrom, (int) count, output.length);
        return (int) merge(left, leftFrom, leftTo, right, rightFrom, rightTo, output, outputFrom, operation);
    }

    private static void sorted(double[] values, int from, int to) {
        for (int i = from; i < to && i + 1 < to; i++) {
            if (Double.compare(values[i], values[i + 1]) > 0) { throw new IllegalArgumentException("unsorted input"); }
        }
    }

    private static long merge(double[] left, int i, int leftTo, double[] right, int j, int rightTo,
            double[] output, int outputFrom, int operation) {
        long count = 0;
        while (i < leftTo || j < rightTo) {
            if ((operation == INTERSECTION && (i == leftTo || j == rightTo))
                    || (operation == DIFFERENCE && i == leftTo)) { break; }
            double value;
            boolean emit;
            if (j == rightTo || (i < leftTo && Double.compare(left[i], right[j]) < 0)) {
                if (operation == INTERSECTION) {
                    i = M3PackedPrimitiveArrayOps.gallopBound(left, i + 1, leftTo, right[j], false);
                    continue;
                }
                value = left[i]; emit = operation != INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
            } else if (i == leftTo || Double.compare(right[j], left[i]) < 0) {
                if (operation == INTERSECTION || operation == DIFFERENCE) {
                    j = M3PackedPrimitiveArrayOps.gallopBound(right, j + 1, rightTo, left[i], false);
                    continue;
                }
                value = right[j]; emit = operation == UNION || operation == SYMMETRIC_DIFFERENCE;
                j = afterEqual(right, j, rightTo, value);
            } else {
                value = left[i]; emit = operation == UNION || operation == INTERSECTION;
                i = afterEqual(left, i, leftTo, value);
                j = afterEqual(right, j, rightTo, value);
            }
            if (emit) {
                if (output != null) { output[outputFrom + (int) count] = value; }
                count++;
            }
        }
        return count;
    }

    private static int afterEqual(double[] values, int index, int to, double value) {
        int next = index + 1;
        if (next == to || Double.compare(values[next], value) != 0) { return next; }
        return M3PackedPrimitiveArrayOps.gallopBound(values, next, to, value, true);
    }
}
