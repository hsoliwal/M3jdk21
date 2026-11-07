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

import java.util.Arrays;
import java.util.Objects;

/**
 * Stable, signed integer predicate index selection. Java is the behavioral oracle; JNI is an
 * explicitly opt-in optional batch backend and does not access Java object addresses.
 *
 * <p>The native backend is considered only for 4096+ entries to avoid tiny JNI calls. That
 * threshold is a conservative policy, not a measured speedup claim.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatIntFilterKernel.java}
 * blob {@code 40f49f3b55abdad8364ee9af36131cb9d8195db6} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatIntFilterKernel {
    private static final int JNI_MIN_ITEMS = 4096;
    private static final boolean NATIVE_AVAILABLE = loadNative();

    private M3MatIntFilterKernel() { }

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
        if (!NATIVE_AVAILABLE) throw new IllegalStateException("m3_matfilter JNI library unavailable");
        int[] positions = new int[values.length];
        int used = selectGreaterOrEqualNative(values, threshold, positions);
        if (used < 0 || used > positions.length) throw new IllegalStateException("invalid native selection count");
        return Arrays.copyOf(positions, used);
    }

    public static boolean nativeAvailable() { return NATIVE_AVAILABLE; }

    private static boolean loadNative() {
        if (!Boolean.getBoolean("m3.matcollections.jni")) return false;
        try {
            System.loadLibrary("m3_matfilter");
            return true;
        } catch (UnsatisfiedLinkError | SecurityException unavailable) {
            return false;
        }
    }

    private static native int selectGreaterOrEqualNative(int[] values, int threshold, int[] result);
}
