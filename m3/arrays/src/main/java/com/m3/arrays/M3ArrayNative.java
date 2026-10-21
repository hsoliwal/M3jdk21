// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Optional JNI materialization/compare boundary for ordinary Java UTF-16 arrays.
 *
 * <p>No native pointer is retained after a call. Absence of the library falls back to exact Java
 * behavior; native acceleration never owns array identity or correctness.</p>
 */
public final class M3ArrayNative {
    private static volatile boolean loaded;

    private M3ArrayNative() {}

    public static synchronized void load(Path library) {
        if (loaded) return;
        System.load(Objects.requireNonNull(library, "library").toAbsolutePath().normalize().toString());
        loaded = true;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static int compareUtf16(
            char[] left, int leftStart, char[] right, int rightStart, int count) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.checkFromIndexSize(leftStart, count, left.length);
        Objects.checkFromIndexSize(rightStart, count, right.length);
        if (loaded) return nativeCompareUtf16(left, leftStart, right, rightStart, count);
        for (int index = 0; index < count; index++) {
            int difference = left[leftStart + index] - right[rightStart + index];
            if (difference != 0) return difference;
        }
        return 0;
    }

    public static char[] concatenateUtf16(char[]... segments) {
        Objects.requireNonNull(segments, "segments");
        long total = 0;
        for (char[] segment : segments) {
            total = Math.addExact(total, Objects.requireNonNull(segment, "segment").length);
        }
        if (total > Integer.MAX_VALUE) throw new OutOfMemoryError("required UTF-16 array size too large");
        if (loaded) return nativeConcatenateUtf16(segments);
        char[] result = new char[(int) total];
        int offset = 0;
        for (char[] segment : segments) {
            System.arraycopy(segment, 0, result, offset, segment.length);
            offset += segment.length;
        }
        return result;
    }

    private static native int nativeCompareUtf16(
            char[] left, int leftStart, char[] right, int rightStart, int count);

    private static native char[] nativeConcatenateUtf16(char[][] segments);

    // SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
    /**
     * Creates detached, zero-initialized Object backing storage through the loaded JNI owner.
     * The result's component type is Object. This required-native boundary never substitutes
     * Java allocation and retains no native pointer or global reference.
     */
    public static Object[] allocateObjectsRequired(int length) {
        requireAllocationLengthAndLibrary(length);
        Object[] result = nativeAllocateObjects(length);
        if (result == null) throw new OutOfMemoryError("native Object[] allocation failed");
        return result;
    }

    /** Creates detached, zero-initialized byte backing storage through the loaded JNI owner. */
    public static byte[] allocateBytesRequired(int length) {
        requireAllocationLengthAndLibrary(length);
        byte[] result = nativeAllocateBytes(length);
        if (result == null) throw new OutOfMemoryError("native byte[] allocation failed");
        return result;
    }

    /** Creates detached, zero-initialized int backing storage through the loaded JNI owner. */
    public static int[] allocateIntsRequired(int length) {
        requireAllocationLengthAndLibrary(length);
        int[] result = nativeAllocateInts(length);
        if (result == null) throw new OutOfMemoryError("native int[] allocation failed");
        return result;
    }

    /** Creates detached, zero-initialized long backing storage through the loaded JNI owner. */
    public static long[] allocateLongsRequired(int length) {
        requireAllocationLengthAndLibrary(length);
        long[] result = nativeAllocateLongs(length);
        if (result == null) throw new OutOfMemoryError("native long[] allocation failed");
        return result;
    }

    private static void requireAllocationLengthAndLibrary(int length) {
        if (length < 0) throw new NegativeArraySizeException(Integer.toString(length));
        if (!loaded) {
            synchronized (M3ArrayNative.class) {
                if (!loaded) {
                    String configured = System.getProperty("m3.arrays.native.path");
                    if (configured == null || configured.isBlank()) {
                        throw new UnsatisfiedLinkError("M3 array JNI library is not loaded");
                    }
                    load(Path.of(configured));
                }
            }
        }
    }

    private static native Object[] nativeAllocateObjects(int length);

    private static native byte[] nativeAllocateBytes(int length);

    private static native int[] nativeAllocateInts(int length);

    private static native long[] nativeAllocateLongs(int length);
}
