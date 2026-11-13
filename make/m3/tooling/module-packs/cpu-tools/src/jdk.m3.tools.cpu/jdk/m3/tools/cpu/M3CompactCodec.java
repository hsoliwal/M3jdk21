// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

/** Primitive byte run-length codec with strict canonical encoding and output budgets.
 * Not LZ, zstd, archive-format interoperability, or arbitrary binary compression.
 */
public final class M3CompactCodec {
    private M3CompactCodec() { }

    /** Pairs (unsigned count 1..255, literal byte); maximal runs are split at 255. */
    public static byte[] rle(byte[] source) {
        Objects.requireNonNull(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < source.length;) {
            int count = 1;
            while (count < 255 && i + count < source.length && source[i] == source[i + count]) count++;
            out.write(count); out.write(source[i]); i += count;
        }
        return out.toByteArray();
    }

    public static byte[] unrle(byte[] encoded, int maxLength) {
        Objects.requireNonNull(encoded);
        if (maxLength < 0 || (encoded.length & 1) != 0)
            throw new IllegalArgumentException("RLE input geometry");
        long total = 0;
        for (int i = 0; i < encoded.length; i += 2) {
            int count = encoded[i] & 0xff;
            if (count == 0) throw new IllegalArgumentException("zero RLE count");
            total += count;
            if (total > maxLength) throw new IllegalArgumentException("RLE output budget");
            // Consecutive identical runs are only canonical if prior run was full.
            if (i >= 2 && encoded[i + 1] == encoded[i - 1] && (encoded[i - 2] & 255) != 255)
                throw new IllegalArgumentException("noncanonical RLE split");
        }
        byte[] out = new byte[(int) total];
        int at = 0;
        for (int i = 0; i < encoded.length; i += 2) {
            int count = encoded[i] & 255;
            for (int j = 0; j < count; j++) out[at++] = encoded[i + 1];
        }
        return out;
    }

    /**
     * Canonical ZigZag/base-128 encoding of signed 64-bit values, including MIN_VALUE.
     * At most one million values and ten million output bytes. The output is exact
     * sized after a complete size pass; budget failure never returns a partial value.
     */
    public static byte[] signedVarints(long[] values, int maxBytes) {
        codecValues(values);
        if (maxBytes < 0 || maxBytes > 10_000_000) throw new IllegalArgumentException("byte budget");
        int size = 0;
        for (long value : values) {
            long bits = (value << 1) ^ (value >> 63);
            do { size++; bits >>>= 7; } while (bits != 0);
            if (size > maxBytes) throw new IllegalArgumentException("varint output budget");
        }
        byte[] result = new byte[size];
        int at = 0;
        for (long value : values) {
            long bits = (value << 1) ^ (value >> 63);
            do {
                int part = (int) (bits & 127); bits >>>= 7;
                result[at++] = (byte) (part | (bits == 0 ? 0 : 128));
            } while (bits != 0);
        }
        return result;
    }

    /**
     * Strict inverse: rejects truncated, overlong, nonminimal and >64-bit encodings.
     * The full input and value cap are checked before allocating the result array.
     * No external framing is implied; the input is a sequence of complete varints.
     */
    public static long[] readSignedVarints(byte[] encoded, int maxValues) {
        Objects.requireNonNull(encoded, "encoded");
        if (maxValues < 0 || maxValues > 1_000_000 || encoded.length > 10_000_000)
            throw new IllegalArgumentException("varint input budget");
        int count = decodeVarints(encoded, null, maxValues);
        long[] result = new long[count];
        decodeVarints(encoded, result, count);
        return result;
    }

    private static int decodeVarints(byte[] encoded, long[] output, int limit) {
        int at = 0, count = 0;
        while (at < encoded.length) {
            long bits = 0;
            int groups = 0, shift = 0;
            for (;;) {
                if (at == encoded.length) throw new IllegalArgumentException("truncated varint");
                int part = encoded[at++] & 255;
                if (groups == 9 && (part & 254) != 0)
                    throw new IllegalArgumentException("varint exceeds 64 bits");
                bits |= (long) (part & 127) << shift;
                groups++;
                if ((part & 128) == 0) {
                    if (groups > 1 && (part & 127) == 0)
                        throw new IllegalArgumentException("nonminimal varint");
                    break;
                }
                shift += 7;
            }
            if (count == limit) throw new IllegalArgumentException("varint value budget");
            if (output != null) output[count] = (bits >>> 1) ^ -(bits & 1L);
            count++;
        }
        return count;
    }

    /** Reversible consecutive XOR transform; no ordering or signed subtraction assumption. */
    public static long[] xorDelta(long[] values) {
        codecValues(values);
        long[] result = new long[values.length];
        long previous = 0;
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i] ^ previous; previous = values[i];
        }
        return result;
    }

    /** Inverse XOR transform, preserving every input bit pattern. */
    public static long[] undoXorDelta(long[] values) {
        codecValues(values);
        long[] result = new long[values.length];
        long previous = 0;
        for (int i = 0; i < values.length; i++) result[i] = previous ^= values[i];
        return result;
    }

    private static void codecValues(long[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length > 1_000_000) throw new IllegalArgumentException("codec value budget");
    }
}
