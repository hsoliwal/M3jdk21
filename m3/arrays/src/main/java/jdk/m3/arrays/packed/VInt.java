// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.util.Objects;

/**
 * Sequential, canonical base-128 encoding of full-width 64-bit integer arrays.
 *
 * <p>Unsigned operations interpret each {@code long} as an unsigned bit carrier;
 * negative carriers therefore occupy ten bytes. ZigZag operations map signed
 * values {@code 0, -1, 1, -2} to unsigned values {@code 0, 1, 2, 3} first.
 * Each byte carries seven payload bits, least-significant group first, with
 * its high bit indicating continuation. A ten-byte value ends in {@code 0x01}.
 *
 * <p>Every operation returns a fresh array and leaves its input unchanged.
 * Callers must not mutate an input until the operation returns. No native
 * library or batch ceiling is required; Java array and available-memory limits
 * apply. This codec supplies sequential serialization, not random access.
 * Encoding computes the complete output size before allocation. Decoding
 * validates the complete format before allocating or populating the result.
 */
public final class VInt {
    private VInt() { }

    /**
     * Encodes every unsigned 64-bit carrier in array order.
     *
     * @param values unsigned values represented by their {@code long} bits
     * @return a fresh array of minimal base-128 encodings
     * @throws NullPointerException if {@code values} is null
     * @throws IllegalArgumentException if the encoded length exceeds
     *         {@link Integer#MAX_VALUE}
     */
    public static byte[] encodeUnsigned64(long[] values) {
        return encode(values, false);
    }

    /**
     * Decodes an entire sequence of canonical unsigned 64-bit values.
     *
     * @param bytes the complete encoded sequence
     * @return a fresh array of unsigned {@code long} bit carriers
     * @throws NullPointerException if {@code bytes} is null
     * @throws IllegalArgumentException for truncation, a nonminimal encoding,
     *         overflow beyond 64 bits or continuation beyond ten bytes
     */
    public static long[] decodeUnsigned64(byte[] bytes) {
        return decode(bytes, false);
    }

    /**
     * ZigZag-maps every signed value and emits its minimal base-128 encoding.
     *
     * @param values signed 64-bit values in array order
     * @return a fresh array of canonical ZigZag encodings
     * @throws NullPointerException if {@code values} is null
     * @throws IllegalArgumentException if the encoded length exceeds
     *         {@link Integer#MAX_VALUE}
     */
    public static byte[] encodeZigZag64(long[] values) {
        return encode(values, true);
    }

    /**
     * Decodes a complete canonical base-128 sequence and reverses ZigZag.
     *
     * @param bytes the complete encoded sequence
     * @return a fresh array of signed 64-bit values
     * @throws NullPointerException if {@code bytes} is null
     * @throws IllegalArgumentException for truncation, a nonminimal encoding,
     *         overflow beyond 64 bits or continuation beyond ten bytes
     */
    public static long[] decodeZigZag64(byte[] bytes) {
        return decode(bytes, true);
    }

    private static byte[] encode(long[] values, boolean zigZag) {
        Objects.requireNonNull(values, "values");
        long length = 0L;
        for (long value : values) {
            long bits = zigZag ? (value << 1) ^ (value >> 63) : value;
            do {
                length++;
                bits >>>= 7;
            } while (bits != 0L);
        }
        // At most ten bytes per input element: this long count cannot overflow.
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Encoded byte count exceeds Java array limit");
        }
        byte[] result = new byte[(int) length];
        int output = 0;
        for (long value : values) {
            long bits = zigZag ? (value << 1) ^ (value >> 63) : value;
            while ((bits & ~0x7fL) != 0L) {
                result[output++] = (byte) ((bits & 0x7fL) | 0x80L);
                bits >>>= 7;
            }
            result[output++] = (byte) bits;
        }
        return result;
    }

    private static long[] decode(byte[] bytes, boolean zigZag) {
        Objects.requireNonNull(bytes, "bytes");
        long[] result = new long[validatedValueCount(bytes)];
        long value = 0L;
        int shift = 0;
        int output = 0;
        for (byte octet : bytes) {
            int unsigned = octet & 0xff;
            // The validation pass limits shift to 63 and the tenth payload to 1.
            value |= (long) (unsigned & 0x7f) << shift;
            if ((unsigned & 0x80) == 0) {
                result[output++] = zigZag ? (value >>> 1) ^ -(value & 1L) : value;
                value = 0L;
                shift = 0;
            } else {
                shift += 7;
            }
        }
        return result;
    }

    /** Validates all bytes without producing any decoded output. */
    private static int validatedValueCount(byte[] bytes) {
        int values = 0;
        int width = 0;
        for (int index = 0; index < bytes.length; index++) {
            int unsigned = bytes[index] & 0xff;
            width++;
            if (width == 10 && unsigned != 1) {
                throw new IllegalArgumentException("Invalid tenth byte at offset " + index);
            }
            if ((unsigned & 0x80) == 0) {
                if (width > 1 && unsigned == 0) {
                    throw new IllegalArgumentException("Nonminimal integer at offset " + index);
                }
                values++;
                width = 0;
            }
        }
        if (width != 0) {
            throw new IllegalArgumentException("Truncated integer at end of input");
        }
        return values;
    }
}
