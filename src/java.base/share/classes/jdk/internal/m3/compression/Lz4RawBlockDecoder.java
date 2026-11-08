// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.util.Objects;

/**
 * Bounds-checked decoder for a single LZ4 raw block, not an LZ4 frame.
 * The uncompressed size is provided out-of-band, as required by the raw format.
 * All output is private to the call and no third-party runtime dependency is retained.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/main/java/com/synexia/algorithms/compression/M3Lz4RawBlockDecoder.java blob d4c311817219; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class Lz4RawBlockDecoder {
    private static final int LITERAL_BITS = 4;
    private static final int NIBBLE_MAX = 15;
    private static final int MIN_MATCH = 4;

    private Lz4RawBlockDecoder() {}

    /**
     * Decode one raw LZ4 block with an explicit expected size and allocation cap.
     * Rejects malformed streams and trailing bytes. Zero-length output requires the
     * canonical one-byte zero token.
     */
    public static byte[] decode(byte[] compressed, int expectedBytes, int maximumOutputBytes) {
        Objects.requireNonNull(compressed, "compressed");
        if (expectedBytes < 0 || maximumOutputBytes < 0 || expectedBytes > maximumOutputBytes) {
            throw new IllegalArgumentException("invalid LZ4 output bound");
        }
        if (expectedBytes == 0) {
            if (compressed.length != 1 || compressed[0] != 0) {
                throw new IllegalArgumentException("invalid empty LZ4 raw block");
            }
            return new byte[0];
        }
        byte[] output = new byte[expectedBytes];
        Cursor in = new Cursor(compressed);
        int written = 0;
        while (in.remaining() > 0) {
            int token = in.take();
            int literals = in.expandedLength(token >>> LITERAL_BITS, output.length - written);
            if (literals > in.remaining()) {
                throw new IllegalArgumentException("truncated LZ4 literal");
            }
            System.arraycopy(compressed, in.position, output, written, literals);
            in.position += literals;
            written += literals;
            if (in.remaining() == 0) {
                if (written != output.length) {
                    throw new IllegalArgumentException("LZ4 decoded size mismatch");
                }
                return output;
            }
            int offset = in.take() | (in.take() << 8);
            if (offset == 0 || offset > written) {
                throw new IllegalArgumentException("invalid LZ4 match distance");
            }
            int matchLimit = output.length - written;
            if (matchLimit < MIN_MATCH) {
                throw new IllegalArgumentException("LZ4 match exceeds output");
            }
            int matchLength = in.expandedLength(token & NIBBLE_MAX, matchLimit - MIN_MATCH)
                    + MIN_MATCH;
            LzByteKernels.copyMatch(output, written, offset, matchLength);
            written += matchLength;
            if (in.remaining() == 0) {
                throw new IllegalArgumentException("LZ4 block lacks final literal sequence");
            }
        }
        throw new IllegalArgumentException("truncated LZ4 token");
    }

    private static final class Cursor {
        private final byte[] bytes;
        private int position;

        Cursor(byte[] bytes) {
            this.bytes = bytes;
        }

        int remaining() {
            return bytes.length - position;
        }

        int take() {
            if (remaining() == 0) {
                throw new IllegalArgumentException("truncated LZ4 block");
            }
            return bytes[position++] & 0xff;
        }

        int expandedLength(int initial, int maximum) {
            long length = initial;
            if (initial == NIBBLE_MAX) {
                int extension;
                do {
                    extension = take();
                    length += extension;
                    if (length > maximum) {
                        throw new IllegalArgumentException("LZ4 sequence exceeds output");
                    }
                } while (extension == 255);
            }
            if (length > maximum) {
                throw new IllegalArgumentException("LZ4 sequence exceeds output");
            }
            return (int) length;
        }
    }
}
