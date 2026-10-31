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
}
