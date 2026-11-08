// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.util.Objects;

/**
 * Immutable, lossless, block-wide adaptive-width packing of signed `int` values.
 *
 * <p>The signed minimum is the block base; unsigned deltas are represented with
 * the smallest width covering the entire range (0..32 bits). Bits use LSB-first
 * little-endian order. Caller arrays are never retained, and every original
 * signed value is recoverable exactly. Unlike neural weight quantization this
 * deliberately does not permit numerical approximation.
 */
public final class AdaptivePackedIntArray {
    private final int length;
    private final int base;
    private final int width;
    private final byte[] packed;

    private AdaptivePackedIntArray(int length, int base, int width, byte[] packed) {
        this.length = length;
        this.base = base;
        this.width = width;
        this.packed = packed;
    }

    /** Snapshot a signed-int array without retaining or mutating the source. */
    public static AdaptivePackedIntArray of(int[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) return new AdaptivePackedIntArray(0, 0, 0, new byte[0]);
        int min = source[0];
        int max = source[0];
        for (int value : source) {
            if (value < min) min = value;
            if (value > max) max = value;
        }
        long range = (long) max - min;
        int bits = range == 0 ? 0 : Long.SIZE - Long.numberOfLeadingZeros(range);
        byte[] data = new byte[checkedByteCount(source.length, bits)];
        for (int i = 0; i < source.length; i++) {
            writeUnsigned(data, (long) i * bits, bits, (long) source[i] - min);
        }
        return new AdaptivePackedIntArray(source.length, min, bits, data);
    }

    /** Import a bitplane, rejecting malformed geometry, padding and signed overflow. */
    public static AdaptivePackedIntArray fromEncoded(int length, int base, int width, byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        int byteCount = checkedByteCount(length, width);
        if (bytes.length != byteCount) throw new IllegalArgumentException("packed byte count mismatch");
        byte[] copy = bytes.clone();
        long totalBits = (long) length * width;
        if ((totalBits & 7L) != 0 && copy.length != 0) {
            int tailBits = (int) (totalBits & 7);
            if (((copy[copy.length - 1] & 0xff) >>> tailBits) != 0) {
                throw new IllegalArgumentException("nonzero packed padding bits");
            }
        }
        for (int i = 0; i < length; i++) {
            long value = (long) base + readUnsigned(copy, (long) i * width, width);
            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
                throw new IllegalArgumentException("decoded value is outside signed int range");
            }
        }
        return new AdaptivePackedIntArray(length, base, width, copy);
    }

    public int length() { return length; }
    public int base() { return base; }
    public int bitsPerValue() { return width; }

    /** Payload size in bytes, excluding object and array headers. */
    public int payloadBytes() { return packed.length; }

    /** O(1) indexed reconstruction, without an intermediate int array. */
    public int get(int index) {
        Objects.checkIndex(index, length);
        return (int) ((long) base + readUnsigned(packed, (long) index * width, width));
    }

    public int[] toIntArray() {
        int[] values = new int[length];
        for (int i = 0; i < values.length; i++) values[i] = get(i);
        return values;
    }

    /** Detached encoded bytes. */
    public byte[] toByteArray() { return packed.clone(); }

    private static int checkedByteCount(int length, int width) {
        if (length < 0 || width < 0 || width > Integer.SIZE) {
            throw new IllegalArgumentException("invalid length/bit width");
        }
        long count = (((long) length * width) + 7L) >>> 3;
        if (count > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("packed payload exceeds Java array capacity");
        }
        return (int) count;
    }

    private static void writeUnsigned(byte[] dst, long bitOffset, int width, long value) {
        int remaining = width;
        int byteIndex = (int) (bitOffset >>> 3);
        int shift = (int) (bitOffset & 7);
        while (remaining != 0) {
            int take = Math.min(remaining, 8 - shift);
            int mask = (1 << take) - 1;
            dst[byteIndex++] |= (byte) ((value & mask) << shift);
            value >>>= take;
            remaining -= take;
            shift = 0;
        }
    }

    private static long readUnsigned(byte[] src, long bitOffset, int width) {
        long result = 0;
        int remaining = width;
        int byteIndex = (int) (bitOffset >>> 3);
        int shift = (int) (bitOffset & 7);
        int outShift = 0;
        while (remaining != 0) {
            int take = Math.min(remaining, 8 - shift);
            int mask = (1 << take) - 1;
            result |= (long) (((src[byteIndex++] & 0xff) >>> shift) & mask) << outShift;
            remaining -= take;
            outShift += take;
            shift = 0;
        }
        return result;
    }
}
