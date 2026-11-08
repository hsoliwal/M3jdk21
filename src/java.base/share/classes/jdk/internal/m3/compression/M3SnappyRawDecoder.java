// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.util.Objects;

/**
 * Bounded decoder for the Snappy raw (unframed) block format.
 * Does not parse Snappy framed streams or provide a Snappy encoder.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/main/java/com/synexia/algorithms/compression/M3SnappyRawDecoder.java blob 0abb7b5dd3c7; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3SnappyRawDecoder {
    private M3SnappyRawDecoder() {}

    /** Decode one exact raw block; refuse malformed input and oversized output. */
    public static byte[] decode(byte[] compressed, int maximumOutputBytes) {
        Objects.requireNonNull(compressed, "compressed");
        if (maximumOutputBytes < 0) {
            throw new IllegalArgumentException("negative output limit");
        }
        Cursor cursor = new Cursor(compressed);
        long advertised = readLength(cursor);
        if (advertised > maximumOutputBytes || advertised > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Snappy output exceeds bound");
        }
        byte[] output = new byte[(int) advertised];
        int written = 0;
        while (cursor.remaining() != 0) {
            int tag = cursor.take();
            int kind = tag & 3;
            if (kind == 0) {
                long length = literalLength(tag, cursor);
                if (length > output.length - written || length > cursor.remaining()) {
                    throw new IllegalArgumentException("truncated Snappy literal");
                }
                int count = (int) length;
                System.arraycopy(compressed, cursor.position, output, written, count);
                cursor.position += count;
                written += count;
            } else {
                int length;
                long distance;
                if (kind == 1) {
                    length = 4 + ((tag >>> 2) & 7);
                    distance = ((long) (tag & 0xe0) << 3) | cursor.take();
                } else if (kind == 2) {
                    length = 1 + (tag >>> 2);
                    distance = cursor.littleEndian(2);
                } else {
                    length = 1 + (tag >>> 2);
                    distance = cursor.littleEndian(4);
                }
                if (distance == 0 || distance > written || length > output.length - written) {
                    throw new IllegalArgumentException("invalid Snappy back-reference");
                }
                M3LzByteKernels.copyMatch(output, written, (int) distance, length);
                written += length;
            }
        }
        if (written != output.length) {
            throw new IllegalArgumentException("Snappy output size mismatch");
        }
        return output;
    }

    private static long readLength(Cursor cursor) {
        long value = 0;
        for (int shift = 0; shift <= 28; shift += 7) {
            int next = cursor.take();
            value |= (long) (next & 0x7f) << shift;
            if ((next & 0x80) == 0) {
                return value;
            }
        }
        throw new IllegalArgumentException("invalid Snappy length varint");
    }

    private static long literalLength(int tag, Cursor cursor) {
        int prefix = tag >>> 2;
        return prefix < 60 ? prefix + 1L : cursor.littleEndian(prefix - 59) + 1L;
    }

    private static final class Cursor {
        private final byte[] bytes;
        private int position;

        private Cursor(byte[] bytes) {
            this.bytes = bytes;
        }

        private int remaining() {
            return bytes.length - position;
        }

        private int take() {
            if (position == bytes.length) {
                throw new IllegalArgumentException("truncated Snappy block");
            }
            return bytes[position++] & 0xff;
        }

        private long littleEndian(int width) {
            long value = 0;
            for (int i = 0; i < width; i++) {
                value |= (long) take() << (8 * i);
            }
            return value;
        }
    }
}
