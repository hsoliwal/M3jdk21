/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import java.util.Objects;

/**
 * Bulk algebra for already key-aligned, nonnegative int count lanes.
 * Output is newly allocated and inputs are never mutated. Alignment is a caller
 * contract: numeric row ordinals from different dictionaries are not comparable.
 * Java is the default. Native execution is explicit, not an automatic speedup.
 * Inputs must not change during either call. No Java callbacks cross JNI.
 */
public final class M3CountAlgebra {
    public enum Operation { SUM, MAX, MIN, DIFFERENCE }
    private M3CountAlgebra() { }

    private static void validate(int[] left, int[] right, Operation operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(operation, "operation");
        if (left.length != right.length) throw new IllegalArgumentException("unaligned lengths");
    }

    public static int[] combine(int[] left, int[] right, Operation operation) {
        validate(left, right, operation);
        int[] result = new int[left.length];
        for (int i = 0; i < left.length; i++) {
            int a = left[i], b = right[i];
            if (a < 0 || b < 0) throw new IllegalArgumentException("negative count");
            result[i] = switch (operation) {
                case SUM -> {
                    if (b > Integer.MAX_VALUE - a) throw new IllegalArgumentException("count overflow");
                    yield a + b;
                }
                case MAX -> Math.max(a, b);
                case MIN -> Math.min(a, b);
                case DIFFERENCE -> Math.max(0, a - b);
            };
        }
        return result;
    }

    /** Explicitly loads the optional platform library; linkage failures propagate. */
    public static void loadNativeLibrary() { System.loadLibrary("m3_collection_counts"); }

    /** Requires successful explicit native loading; never silently falls back. */
    public static int[] combineNative(int[] left, int[] right, Operation operation) {
        validate(left, right, operation);
        return combine0(left, right, operation.ordinal());
    }

    private static native int[] combine0(int[] left, int[] right, int operation);
}
