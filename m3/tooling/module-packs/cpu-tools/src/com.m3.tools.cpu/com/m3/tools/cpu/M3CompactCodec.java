// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

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
}
