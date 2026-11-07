/*******************************************************************************
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
package jdk.internal.m3.collections;

import java.util.Objects;

/**
 * Size-sensitive immutable bitmap codec with one canonical dense-or-sparse spelling.
 *
 * <p>The declared finite universe is significant. Dense words use existing MAT
 * big-endian lanes. Sparse positions use canonical unsigned LEB128 gaps. The
 * smaller encoding is chosen deterministically, with dense winning ties.
 * This is an opt-in codec for the existing {@link M3MatPool}, not a new interner.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatBitsAdaptiveCodec.java}
 * blob {@code 45fc409b6ec0d27db9159dee127232651387bae4} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatBitsAdaptiveCodec implements M3MatCodec<M3MatBits> {
    private static final int MODE_DENSE = 0;
    private static final int MODE_SPARSE = 1;
    private static final int DEFAULT_MAX_UNIVERSE_BITS = 1 << 24;

    public static final M3MatBitsAdaptiveCodec INSTANCE = new M3MatBitsAdaptiveCodec(
            DEFAULT_MAX_UNIVERSE_BITS);

    private final int maxUniverseBits;

    private M3MatBitsAdaptiveCodec(int maxUniverseBits) {
        if (maxUniverseBits < 0) throw new IllegalArgumentException("negative universe limit");
        this.maxUniverseBits = maxUniverseBits;
    }

    /** Explicit per-call resource policy; the type identity and wire format stay fixed. */
    public static M3MatBitsAdaptiveCodec bounded(int maxUniverseBits) {
        return new M3MatBitsAdaptiveCodec(maxUniverseBits);
    }

    public int maxUniverseBits() { return maxUniverseBits; }

    @Override public String typeId() { return "synex:bits-adaptive/1"; }
    @Override public Class<M3MatBits> javaType() { return M3MatBits.class; }

    @Override
    public void encode(M3MatBits value, M3MatSink sink) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(sink, "sink");
        requireUniverse(value.size());
        long denseSize = denseSize(value.size());
        boolean sparse = sparseWins(value, denseSize);
        sink.i32(value.size());
        if (sparse) {
            sink.u8(MODE_SPARSE);
            sink.i32(value.cardinality());
            long previous = -1;
            for (int bit = value.nextSetBit(0); bit >= 0; bit = value.nextSetBit(bit + 1)) {
                writeVarint(bit - previous - 1, sink);
                previous = bit;
            }
        } else {
            sink.u8(MODE_DENSE);
            for (int word = 0; word < value.wordCount(); word++) sink.i64(value.word(word));
        }
    }

    @Override
    public M3MatBits decode(M3MatSource source) {
        Objects.requireNonNull(source, "source");
        int universe = source.i32();
        requireUniverse(universe);
        int mode = source.u8();
        if (mode == MODE_DENSE) {
            int wordCount = M3MatBits.wordsFor(universe);
            if (8L * wordCount > source.remaining()) {
                throw new IllegalArgumentException("truncated dense bitmap");
            }
            long[] words = new long[wordCount];
            for (int i = 0; i < words.length; i++) words[i] = source.i64();
            int remainder = universe & 63;
            if (remainder != 0 && wordCount != 0
                    && (words[wordCount - 1] >>> remainder) != 0) {
                throw new IllegalArgumentException("noncanonical dense tail bits");
            }
            M3MatBits bitmap = M3MatBits.ofWords(universe, words);
            if (sparseWins(bitmap, denseSize(universe))) {
                throw new IllegalArgumentException("nonminimal dense representation");
            }
            return bitmap;
        }
        if (mode == MODE_SPARSE) {
            int count = source.i32();
            if (count < 0 || count > universe || count > source.remaining()) {
                throw new IllegalArgumentException("sparse cardinality or length");
            }
            M3MatBits.Builder bits = M3MatBits.builder(universe);
            long previous = -1;
            long length = 9L;
            for (int index = 0; index < count; index++) {
                long gap = readVarint(source);
                long bit = previous + 1L + gap;
                if (bit >= universe) throw new IllegalArgumentException("sparse bit outside universe");
                bits.set((int) bit);
                length += varintLength(gap);
                previous = bit;
            }
            if (length >= denseSize(universe)) {
                throw new IllegalArgumentException("nonminimal sparse representation");
            }
            return bits.build();
        }
        throw new IllegalArgumentException("unknown bitmap representation mode");
    }

    private void requireUniverse(int size) {
        if (size < 0 || size > maxUniverseBits) {
            throw new IllegalArgumentException("bitmap universe exceeds codec resource limit");
        }
    }

    private static long denseSize(int universe) {
        return 5L + 8L * M3MatBits.wordsFor(universe);
    }

    /** Fail fast on high cardinality; exact gap lengths only matter below the dense cost. */
    private static boolean sparseWins(M3MatBits bits, long denseBytes) {
        long count = bits.cardinality();
        if (9L + count >= denseBytes) return false;
        long bytes = 9L;
        long previous = -1;
        for (int bit = bits.nextSetBit(0); bit >= 0; bit = bits.nextSetBit(bit + 1)) {
            bytes += varintLength(bit - previous - 1);
            if (bytes >= denseBytes) return false;
            previous = bit;
        }
        return true;
    }

    private static int varintLength(long value) {
        int bytes = 1;
        while ((value >>>= 7) != 0) bytes++;
        return bytes;
    }

    private static void writeVarint(long value, M3MatSink sink) {
        while ((value & ~0x7fL) != 0) {
            sink.u8((int) (value & 0x7fL) | 0x80);
            value >>>= 7;
        }
        sink.u8((int) value);
    }

    /** Rejects overlong forms and values exceeding the signed int bitmap universe. */
    private static long readVarint(M3MatSource source) {
        long value = 0;
        for (int index = 0; index < 5; index++) {
            int next = source.u8();
            value |= (long) (next & 0x7f) << (index * 7);
            if (value > Integer.MAX_VALUE) throw new IllegalArgumentException("oversized sparse gap");
            if ((next & 0x80) == 0) {
                if (index != 0 && (next & 0x7f) == 0) {
                    throw new IllegalArgumentException("nonminimal sparse varint");
                }
                return value;
            }
        }
        throw new IllegalArgumentException("overlong sparse varint");
    }
}
