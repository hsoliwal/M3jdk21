// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compression;

import java.util.Objects;

/**
 * Opt-in Snappy raw (unframed) block encoder backed by the canonical M3 four-byte
 * match-candidate index. Single-thread owned, reusable scratch, standard raw bytes.
 * No frame/chunk/checksum headers are emitted and no codec is auto-replaced.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/main/java/com/synexia/algorithms/compression/M3SnappyRawEncoder.java blob 33c96e1331be; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3SnappyRawEncoder {
    private static final int KEY_BYTES = Integer.BYTES;
    private static final int MAX_COPY_DISTANCE = 0xffff;
    private static final int MAX_COPY_LENGTH = 64;
    private final M3LzMatchCandidateIndex candidates;
    private final M3CompressionByteSink workspace;

    public M3SnappyRawEncoder(int tableBits) {
        candidates = new M3LzMatchCandidateIndex(tableBits);
        workspace = new M3CompressionByteSink(0, 0);
    }

    /**
     * Compress exactly one raw block. Output cap applies to the wire bytes, including
     * the uncompressed-size varint. The instance must not be shared concurrently.
     */
    public byte[] encode(byte[] input, int maximumCompressedBytes) {
        Objects.requireNonNull(input, "input");
        M3CompressionByteSink out = workspace;
        out.reset(maximumCompressedBytes, input.length);
        candidates.begin(input);
        try {
            writeUnsignedLength(out, input.length);
            int anchor = 0;
            int position = 0;
            while (position <= input.length - KEY_BYTES) {
                int match = candidates.findAndRecord(position, MAX_COPY_DISTANCE);
                if (match < 0) {
                    position++;
                    continue;
                }
                int matched = M3LzByteKernels.commonPrefix(input, match, position,
                        input.length - position);
                if (matched < KEY_BYTES) {
                    position++;
                    continue;
                }
                literal(out, input, anchor, position - anchor);
                copy(out, position - match, matched);
                position += matched;
                anchor = position;
            }
            literal(out, input, anchor, input.length - anchor);
            return out.finish();
        } finally {
            candidates.clear();
        }
    }

    private static void writeUnsignedLength(M3CompressionByteSink out, int length) {
        int remaining = length;
        while ((remaining & ~0x7f) != 0) {
            out.append((remaining & 0x7f) | 0x80);
            remaining >>>= 7;
        }
        out.append(remaining);
    }

    private static void literal(M3CompressionByteSink out, byte[] data, int offset, int length) {
        if (length == 0) {
            return;
        }
        int encoded = length - 1;
        if (length <= 60) {
            out.append(encoded << 2);
        } else {
            int width = encoded <= 0xff ? 1 : encoded <= 0xffff ? 2
                    : encoded <= 0xffffff ? 3 : 4;
            out.append((59 + width) << 2);
            for (int i = 0; i < width; i++) {
                out.append(encoded >>> (8 * i));
            }
        }
        out.append(data, offset, length);
    }

    private static void copy(M3CompressionByteSink out, int distance, int length) {
        if (distance < 1 || distance > MAX_COPY_DISTANCE || length < KEY_BYTES) {
            throw new IllegalArgumentException("invalid Snappy LZ copy");
        }
        int remaining = length;
        while (remaining > 0) {
            int count = Math.min(MAX_COPY_LENGTH, remaining);
            out.append(((count - 1) << 2) | 2); // COPY_2, little-endian offset.
            out.appendLE16(distance);
            remaining -= count;
        }
    }
}
