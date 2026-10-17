// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Stable, signed integer predicate index selection. Java is the behavioral oracle; JNI is an
 * explicitly opt-in optional batch backend and does not access Java object addresses.
 *
 * <p>The native backend is considered only for 4096+ entries to avoid tiny JNI calls. That
 * threshold is a conservative policy, not a measured speedup claim.</p>
 */
public final class M3IntFilterKernel {
    private static final int JNI_MIN_ITEMS = 4096;
    private static final boolean NATIVE_AVAILABLE = loadNative();

    private M3IntFilterKernel() { }

    /**
     * Returns original indexes with {@code values[index] >= threshold}, in ascending index order.
     * All comparisons are signed int comparisons; duplicates and extreme values are preserved.
     */
    public static int[] indicesGreaterOrEqual(int[] values, int threshold) {
        Objects.requireNonNull(values, "values");
        if (NATIVE_AVAILABLE && values.length >= JNI_MIN_ITEMS) {
            return indicesGreaterOrEqualNative(values, threshold);
        }
        return indicesGreaterOrEqualJava(values, threshold);
    }

    /** Reference Java implementation, usable for differential testing and native opt-out. */
    public static int[] indicesGreaterOrEqualJava(int[] values, int threshold) {
        Objects.requireNonNull(values, "values");
        int accepted = 0;
        for (int value : values) if (value >= threshold) accepted++;
        int[] indexes = new int[accepted];
        int output = 0;
        for (int index = 0; index < values.length; index++) {
            if (values[index] >= threshold) indexes[output++] = index;
        }
        return indexes;
    }

    /** Executes actual native code or fails if the optional library was not loaded. */
    public static int[] indicesGreaterOrEqualNative(int[] values, int threshold) {
        Objects.requireNonNull(values, "values");
        if (!NATIVE_AVAILABLE) throw new IllegalStateException("m3_int_filter JNI library unavailable");
        int[] positions = new int[values.length];
        int used = selectGreaterOrEqualNative(values, threshold, positions);
        if (used < 0 || used > positions.length) throw new IllegalStateException("invalid native selection count");
        // Dense selection already fills the entire result: keep that buffer instead of
        // allocating and copying another full-length int[]. Sparse results remain trimmed.
        return used == positions.length ? positions : Arrays.copyOf(positions, used);
    }

    public static boolean nativeAvailable() { return NATIVE_AVAILABLE; }

    private static boolean loadNative() {
        if (!Boolean.getBoolean("m3.collections.intfilter.jni")) return false;
        try {
            System.loadLibrary("m3_int_filter");
            return true;
        } catch (UnsatisfiedLinkError | SecurityException unavailable) {
            return false;
        }
    }

    private static native int selectGreaterOrEqualNative(int[] values, int threshold, int[] result);
}
