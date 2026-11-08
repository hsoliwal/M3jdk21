// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Two-pointer algebra over sorted primitive int multiset lanes.
 *
 * <p>Inputs must be nondecreasing, may contain duplicates or signed extremes,
 * and remain caller-owned. The output is an exact-length detached primitive
 * array in nondecreasing order, with no per-element boxed entries. Multiplicity
 * is max(left,right) for union, min for intersection, max(left-right,0) for
 * subtraction, and abs(left-right) for symmetric difference. The two-pass
 * count/output scan costs O(left.length + right.length) time and O(result)
 * auxiliary storage, including for an empty result. Concurrent mutation of
 * input lanes is unsupported.
 *
 * <p>The native-required entrypoint borrows no Java array pointer. It uses
 * 1,024-int JNI region copies and a common C11 compare/emit decision kernel.
 * Algorithm donors: LeetCode 350 and HackerRank Missing Numbers; these are
 * problem references, not copied implementation bodies.
 */
public final class SortedIntMultisets {
    // ABI-stable C mode values: do not derive from an enum's declaration order.
    public enum Operation {
        UNION_MAX(0), INTERSECTION_MIN(1), SUBTRACT(2), SYMMETRIC_DIFFERENCE(3);
        private final int nativeCode;
        Operation(int nativeCode) { this.nativeCode = nativeCode; }
    }
    private static final int MAX_RESULT = Integer.MAX_VALUE - 8;

    private SortedIntMultisets() { }

    public static int[] union(int[] left, int[] right) {
        return apply(left, right, Operation.UNION_MAX);
    }

    public static int[] intersection(int[] left, int[] right) {
        return apply(left, right, Operation.INTERSECTION_MIN);
    }

    public static int[] subtract(int[] left, int[] right) {
        return apply(left, right, Operation.SUBTRACT);
    }

    public static int[] symmetricDifference(int[] left, int[] right) {
        return apply(left, right, Operation.SYMMETRIC_DIFFERENCE);
    }

    /** Full Java implementation. No intermediate maps, sets or Entry objects. */
    public static int[] apply(int[] left, int[] right, Operation operation) {
        validate(left, right, operation);
        int count = scan(left, right, operation, null);
        int[] result = new int[count];
        int second = scan(left, right, operation, result);
        if (second != count) {
            throw new java.util.ConcurrentModificationException("sorted input lanes changed during merge");
        }
        return result;
    }

    /** A required-native entrypoint with no silent Java fallback. */
    public static int[] applyRequiredNative(int[] left, int[] right, Operation operation) {
        validate(left, right, operation);
        if (!ArrayNative.isLoaded()) {
            String path = System.getProperty("m3.arrays.native.path");
            if (path == null || path.isBlank()) {
                throw new UnsatisfiedLinkError("M3 array JNI library is not loaded");
            }
            ArrayNative.load(Path.of(path));
        }
        int[] result = nativeMerge(left, right, operation.nativeCode);
        if (result == null) throw new OutOfMemoryError("native sorted multiset result unavailable");
        return result;
    }

    private static native int[] nativeMerge(int[] left, int[] right, int operation);

    private static void validate(int[] left, int[] right, Operation op) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(op, "operation");
        if (!sorted(left) || !sorted(right)) {
            throw new IllegalArgumentException("both multiset lanes must be nondecreasing");
        }
    }

    private static boolean sorted(int[] values) {
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] > values[i]) return false;
        }
        return true;
    }

    private static int scan(int[] left, int[] right, Operation operation, int[] result) {
        int li = 0, ri = 0, count = 0;
        while (li < left.length || ri < right.length) {
            boolean emit;
            int value;
            if (li == left.length) {
                value = right[ri++];
                emit = operation == Operation.UNION_MAX
                        || operation == Operation.SYMMETRIC_DIFFERENCE;
            } else if (ri == right.length) {
                value = left[li++];
                emit = operation != Operation.INTERSECTION_MIN;
            } else if (left[li] < right[ri]) {
                value = left[li++];
                emit = operation != Operation.INTERSECTION_MIN;
            } else if (left[li] > right[ri]) {
                value = right[ri++];
                emit = operation == Operation.UNION_MAX
                        || operation == Operation.SYMMETRIC_DIFFERENCE;
            } else {
                value = left[li++];
                ri++;
                emit = operation == Operation.UNION_MAX
                        || operation == Operation.INTERSECTION_MIN;
            }
            if (emit) {
                if (count >= MAX_RESULT) throw new OutOfMemoryError("multiset result exceeds Java array limit");
                if (result != null) result[count] = value;
                count++;
            }
        }
        return count;
    }
}
