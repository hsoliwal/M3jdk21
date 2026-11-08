// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.util.Objects;

/**
 * Opt-in LZ4 raw-block encoder with bounded scratch and canonical M3 match atoms.
 * Maintains the LZ4 last-match/last-literals restrictions for block interoperability.
 * Not an LZ4 frame encoder. Caller owns one instance per worker.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/main/java/com/synexia/algorithms/compression/M3Lz4RawBlockEncoder.java blob 2cc10f467893; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3Lz4RawBlockEncoder {
    private static final int KEY_BYTES = Integer.BYTES;
    private static final int MAX_DISTANCE = 0xffff;
    private static final int LAST_LITERALS = 5;
    private static final int LAST_MATCH_START_GUARD = 12;
    private static final int MAX_NIBBLE = 15;
    private final M3LzMatchCandidateIndex candidates;
    private final M3CompressionByteSink workspace;

    public M3Lz4RawBlockEncoder(int tableBits) {
        candidates = new M3LzMatchCandidateIndex(tableBits);
        workspace = new M3CompressionByteSink(0, 0);
    }

    /** Emit one raw LZ4 block, never exceeding the supplied wire-output cap. */
    public byte[] encode(byte[] input, int maximumCompressedBytes) {
        Objects.requireNonNull(input, "input");
        M3CompressionByteSink out = workspace;
        out.reset(maximumCompressedBytes, input.length);
        candidates.begin(input);
        try {
            int anchor = 0;
            int position = 0;
            while (position <= input.length - LAST_MATCH_START_GUARD) {
                int match = candidates.findAndRecord(position, MAX_DISTANCE);
                if (match < 0) {
                    position++;
                    continue;
                }
                int matched = M3LzByteKernels.commonPrefix(input, match, position,
                        input.length - position - LAST_LITERALS);
                if (matched < KEY_BYTES) {
                    position++;
                    continue;
                }
                int literals = position - anchor;
                int matchExtra = matched - KEY_BYTES;
                out.append((Math.min(literals, MAX_NIBBLE) << 4)
                        | Math.min(matchExtra, MAX_NIBBLE));
                extension(out, literals);
                out.append(input, anchor, literals);
                out.appendLE16(position - match);
                extension(out, matchExtra);
                position += matched;
                anchor = position;
            }
            int remaining = input.length - anchor;
            out.append(Math.min(remaining, MAX_NIBBLE) << 4);
            extension(out, remaining);
            out.append(input, anchor, remaining);
            return out.finish();
        } finally {
            candidates.clear();
        }
    }

    private static void extension(M3CompressionByteSink output, int length) {
        if (length < MAX_NIBBLE) {
            return;
        }
        int remaining = length - MAX_NIBBLE;
        while (remaining >= 255) {
            output.append(255);
            remaining -= 255;
        }
        output.append(remaining);
    }
}
