// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.nio.file.Path;
import java.util.Objects;
import jdk.m3.arrays.M3ArrayNative;

/**
 * Explicit JNI transport of the exact LSB-first adaptive signed-int bitplane.
 * The portable immutable owner is {@link AdaptivePackedIntArray}; native calls
 * are bounded to 4096 elements and 4096 bytes, and never retain array pointers.
 */
public final class AdaptivePackedIntNative {
    public static final int MAX_ITEMS = 4096;
    public static final int MAX_BYTES = 4096;

    private AdaptivePackedIntNative() {}

    /** Encode using caller-selected signed base and unsigned delta width. */
    public static byte[] encodeRequired(int[] values, int base, int width) {
        Objects.requireNonNull(values, "values");
        int count = checkedBytes(values.length, width);
        if (count > MAX_BYTES || values.length > MAX_ITEMS) {
            throw new IllegalArgumentException("native bitplane batch bound");
        }
        requireLibrary();
        return nativeEncode(values, base, width);
    }

    /** Decode using explicitly supplied plane metadata into an owned int array. */
    public static int[] decodeRequired(byte[] bytes, int length, int base, int width) {
        Objects.requireNonNull(bytes, "bytes");
        int expected = checkedBytes(length, width);
        if (bytes.length != expected || bytes.length > MAX_BYTES || length > MAX_ITEMS) {
            throw new IllegalArgumentException("native bitplane geometry");
        }
        requireLibrary();
        return nativeDecode(bytes, length, base, width);
    }

    private static int checkedBytes(int length, int width) {
        if (length < 0 || width < 0 || width > 32) {
            throw new IllegalArgumentException("invalid length/bit width");
        }
        long bytes = (((long) length * width) + 7L) >>> 3;
        if (bytes > Integer.MAX_VALUE) throw new IllegalArgumentException("bitplane too large");
        return (int) bytes;
    }

    private static void requireLibrary() {
        if (M3ArrayNative.isLoaded()) return;
        String path = System.getProperty("m3.arrays.native.path");
        if (path == null || path.isBlank()) {
            throw new UnsatisfiedLinkError("M3 arrays JNI must be loaded explicitly");
        }
        M3ArrayNative.load(Path.of(path));
    }

    private static native byte[] nativeEncode(int[] values, int base, int width);
    private static native int[] nativeDecode(byte[] bytes, int length, int base, int width);
}
