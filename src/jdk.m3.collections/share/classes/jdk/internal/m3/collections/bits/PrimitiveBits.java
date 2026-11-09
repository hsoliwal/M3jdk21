/* *****************************************************************************
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
package jdk.internal.m3.collections.bits;

import java.util.Objects;

/**
 * Bit-preserving adapters and checked packed spans without boxing.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveBits.java}
 * blob {@code 4950f70b98e683ada22fbfcad720e5157169a849} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveBits {
    private PrimitiveBits() {}
    public static int unsigned(byte value) { return Byte.toUnsignedInt(value); }
    public static int unsigned(short value) { return Short.toUnsignedInt(value); }
    public static int bits(float value) { return Float.floatToRawIntBits(value); }
    public static float floatValue(int bits) { return Float.intBitsToFloat(bits); }
    public static long bits(double value) { return Double.doubleToRawLongBits(value); }
    public static double doubleValue(long bits) { return Double.longBitsToDouble(bits); }
    public static int charBits(char value) { return value; }
    public static char charValue(int bits) { return (char) bits; }

    /** Low-bit mask for 1..64 bits, including the full-width raw-bit case. */
    public static long mask(int width) {
        if (width < 1 || width > Long.SIZE) {
            throw new IllegalArgumentException("width must be 1..64");
        }
        return width == Long.SIZE ? -1L : (1L << width) - 1L;
    }

    /** Rejects values outside the declared range; unsigned64 accepts all bit patterns. */
    public static void checkValue(long value, int width, boolean signed) {
        long mask = mask(width);
        if (width == Long.SIZE) return;
        if (signed) {
            long sign = 1L << (width - 1);
            if (value < -sign || value >= sign) {
                throw new IllegalArgumentException("value exceeds signed packed width");
            }
        } else if ((value & ~mask) != 0L) {
            throw new IllegalArgumentException("value exceeds unsigned packed width");
        }
    }

    /** Interprets canonical low width bits as a two's-complement signed value. */
    public static long signExtend(long raw, int width) {
        long mask = mask(width);
        if ((raw & ~mask) != 0L) {
            throw new IllegalArgumentException("noncanonical signed bits");
        }
        int shift = Long.SIZE - width;
        return (raw << shift) >> shift;
    }

    /** Exact word count using long address arithmetic; zero values require no words. */
    public static int packedWordCount(int count, int width) {
        mask(width);
        if (count < 0) throw new IllegalArgumentException("count < 0");
        long words = ((long) count * width + 63L) >>> 6;
        if (words > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("packed array too large");
        }
        return (int) words;
    }

    /**
     * Reads an LSB-first packed field. Adapted from the existing BOP BitPacking owner;
     * the bit offset is long even though each backing array has int coordinates.
     */
    public static long readPacked(long[] words, long bitOffset, int width) {
        checkSpan(words, bitOffset, width);
        int word = (int) (bitOffset >>> 6);
        int shift = (int) (bitOffset & 63L);
        long raw = words[word] >>> shift;
        if (shift != 0 && width > Long.SIZE - shift) {
            raw |= words[word + 1] << (Long.SIZE - shift);
        }
        return raw & mask(width);
    }

    /** Writes canonical raw bits; validates the complete span/value before either word changes. */
    public static void writePacked(long[] words, long bitOffset, int width, long raw) {
        checkSpan(words, bitOffset, width);
        long mask = mask(width);
        if ((raw & ~mask) != 0L) {
            throw new IllegalArgumentException("value exceeds packed span");
        }
        int word = (int) (bitOffset >>> 6);
        int shift = (int) (bitOffset & 63L);
        words[word] = (words[word] & ~(mask << shift)) | (raw << shift);
        if (shift != 0 && width > Long.SIZE - shift) {
            int highWidth = width - (Long.SIZE - shift);
            long highMask = mask(highWidth);
            words[word + 1] = (words[word + 1] & ~highMask)
                    | (raw >>> (Long.SIZE - shift));
        }
    }

    private static void checkSpan(long[] words, long bitOffset, int width) {
        Objects.requireNonNull(words, "words");
        mask(width);
        if (bitOffset < 0 || bitOffset > (long) words.length * Long.SIZE - width) {
            throw new IndexOutOfBoundsException("packed span outside words");
        }
    }

    /** Unsigned 32-bit value represented exactly in a nonnegative long. */
    public static long unsigned(int value) { return Integer.toUnsignedLong(value); }

    /**
     * Encode IEEE binary16 with round-to-nearest, ties-to-even.
     * Every NaN maps to the positive quiet encoding {@code 0x7e00}.
     */
    public static short toFloat16(float value) {
        if (Float.isNaN(value)) return (short) 0x7e00;
        return Float.floatToFloat16(value);
    }

    /** Decode binary16; every NaN becomes positive quiet binary32 {@code 0x7fc00000}. */
    public static float fromFloat16(short bits) {
        if ((bits & 0x7fff) > 0x7c00) return Float.intBitsToFloat(0x7fc00000);
        return Float.float16ToFloat(bits);
    }

    /**
     * Encode bfloat16 with round-to-nearest, ties-to-even.
     * Every NaN maps to the positive quiet encoding {@code 0x7fc0}.
     */
    public static short toBFloat16(float value) {
        int bits = Float.floatToRawIntBits(value);
        if ((bits & 0x7fff_ffff) > 0x7f80_0000) return (short) 0x7fc0;
        int rounded = bits + 0x7fff + ((bits >>> 16) & 1);
        return (short) (rounded >>> 16);
    }

    /** Decode bfloat16; every NaN becomes positive quiet binary32 {@code 0x7fc00000}. */
    public static float fromBFloat16(short bits) {
        if ((bits & 0x7fff) > 0x7f80) return Float.intBitsToFloat(0x7fc00000);
        return Float.intBitsToFloat((bits & 0xffff) << 16);
    }
}
