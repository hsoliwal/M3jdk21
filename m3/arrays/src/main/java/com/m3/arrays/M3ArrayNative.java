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

    /** Maximum input elements and output bytes for each explicit packed-codec JNI batch. */
    public static final int MAX_CODEC_ITEMS = 4096;

    /**
     * Encodes signed 40-, 48- or 56-bit values as exact little-endian payloads.
     * Both input elements and encoded bytes must fit {@link #MAX_CODEC_ITEMS}.
     * Requires the matching native library; no Java fallback occurs. Inputs must remain
     * unchanged until the call returns. The returned byte array is independently owned.
     * @throws NullPointerException for a null input
     * @throws IllegalArgumentException for an unsupported width, value or batch geometry
     * @throws UnsatisfiedLinkError if the matching native library is unavailable
     * @throws OutOfMemoryError if result allocation fails
     */
    public static byte[] encodePackedRequired(long[] values, int bits) {
        Objects.requireNonNull(values, "values");
        checkCodecInput(values.length);
        int width = codecWidth(bits);
        if (values.length > MAX_CODEC_ITEMS / width) {
            throw new IllegalArgumentException("codec encoded byte bound");
        }
        requireAllocationLengthAndLibrary(0);
        byte[] result = nativeEncodePacked(values, bits);
        requireCodecResult(result);
        return result;
    }

    /**
     * Decodes complete signed little-endian packed elements through the required library.
     * Width, batch and ownership rules are identical to {@link #encodePackedRequired}.
     * A partial trailing element is rejected. No native pointer is retained.
     */
    public static long[] decodePackedRequired(byte[] bytes, int bits) {
        Objects.requireNonNull(bytes, "bytes");
        checkCodecInput(bytes.length);
        int width = codecWidth(bits);
        if (bytes.length % width != 0) {
            throw new IllegalArgumentException("partial packed element");
        }
        requireAllocationLengthAndLibrary(0);
        long[] result = nativeDecodePacked(bytes, bits);
        requireCodecResult(result);
        return result;
    }

    /**
     * Encodes all 64 bits of each long as canonical base-128 through the required library.
     * Negative carriers represent unsigned values at least 2^63 and occupy ten bytes.
     * Both element count and encoded bytes must fit {@link #MAX_CODEC_ITEMS}.
     * Inputs remain caller-owned and must stay unchanged during the call.
     */
    public static byte[] encodeUnsigned64Required(long[] values) {
        Objects.requireNonNull(values, "values");
        checkCodecInput(values.length);
        requireAllocationLengthAndLibrary(0);
        byte[] result = nativeEncodeUnsigned64(values);
        requireCodecResult(result);
        return result;
    }

    /**
     * Decodes a complete canonical base-128 sequence through the required library.
     * Returns fresh unsigned long bit carriers. Truncated, nonminimal, overflowing and
     * over-bound sequences throw IllegalArgumentException; null throws NullPointerException.
     */
    public static long[] decodeUnsigned64Required(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        checkCodecInput(bytes.length);
        requireAllocationLengthAndLibrary(0);
        long[] result = nativeDecodeUnsigned64(bytes);
        requireCodecResult(result);
        return result;
    }

    /**
     * ZigZag-maps the complete signed-long range before canonical base-128 encoding.
     * Uses the same required-library, batch, ownership and allocation rules as unsigned64.
     */
    public static byte[] encodeZigZag64Required(long[] values) {
        Objects.requireNonNull(values, "values");
        checkCodecInput(values.length);
        requireAllocationLengthAndLibrary(0);
        byte[] result = nativeEncodeZigZag64(values);
        requireCodecResult(result);
        return result;
    }

    /** Decodes a canonical bounded base-128 sequence and reverses ZigZag into a fresh array. */
    public static long[] decodeZigZag64Required(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        checkCodecInput(bytes.length);
        requireAllocationLengthAndLibrary(0);
        long[] result = nativeDecodeZigZag64(bytes);
        requireCodecResult(result);
        return result;
    }

    private static int codecWidth(int bits) {
        if (bits != 40 && bits != 48 && bits != 56) {
            throw new IllegalArgumentException("packed width must be 40, 48 or 56");
        }
        return bits / Byte.SIZE;
    }

    private static void checkCodecInput(int count) {
        if (count > MAX_CODEC_ITEMS) throw new IllegalArgumentException("codec array bound");
    }

    private static void requireCodecResult(Object result) {
        if (result == null) throw new OutOfMemoryError("packed codec result allocation failed");
    }

    private static native byte[] nativeEncodePacked(long[] values, int bits);
    private static native long[] nativeDecodePacked(byte[] bytes, int bits);
    private static native byte[] nativeEncodeUnsigned64(long[] values);
    private static native long[] nativeDecodeUnsigned64(byte[] bytes);
    private static native byte[] nativeEncodeZigZag64(long[] values);
    private static native long[] nativeDecodeZigZag64(byte[] bytes);
}
