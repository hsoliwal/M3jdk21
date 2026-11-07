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
 * Immutable signed-long snapshot using constant, base+packed-delta, or plain blocks.
 * Extends the existing MatBlockIntVector representation to the full signed-long domain.
 * All retained metadata and values reside in primitive arrays.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/BlockPackedLongArray.java}
 * blob {@code 99c737e14e70a421b6e105f95e0cccdab39dd681} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3BlockPackedLongArray {
    public static final int DEFAULT_BLOCK_SIZE = 256;
    private final int size;
    private final int blockSize;
    private final long[] bases;
    private final byte[] widths;
    private final int[] offsets;
    private final long[] packed;
    private final long[] plain;

    private M3BlockPackedLongArray(int size, int blockSize, long[] bases,
            byte[] widths, int[] offsets, long[] packed, long[] plain) {
        this.size = size;
        this.blockSize = blockSize;
        this.bases = bases;
        this.widths = widths;
        this.offsets = offsets;
        this.packed = packed;
        this.plain = plain;
    }

    public static M3BlockPackedLongArray of(long[] source) {
        return of(source, DEFAULT_BLOCK_SIZE);
    }

    public static M3BlockPackedLongArray of(long[] source, int blockSize) {
        Objects.requireNonNull(source, "source");
        if (blockSize < 1) throw new IllegalArgumentException("blockSize must be positive");
        int blocks = source.length == 0 ? 0 : 1 + (source.length - 1) / blockSize;
        long[] bases = new long[blocks];
        byte[] widths = new byte[blocks];
        int[] offsets = new int[blocks];
        long wordCount = 0L;
        int plainCount = 0;
        for (int block = 0; block < blocks; block++) {
            int from = block * blockSize;
            int to = from + Math.min(blockSize, source.length - from);
            long minimum = source[from];
            long maximum = minimum;
            for (int i = from + 1; i < to; i++) {
                minimum = Math.min(minimum, source[i]);
                maximum = Math.max(maximum, source[i]);
            }
            bases[block] = minimum;
            // A negative subtraction result is the correct unsigned range >= 2^63.
            long range = maximum - minimum;
            int width = Long.SIZE - Long.numberOfLeadingZeros(range);
            if (width == 0) continue;
            int words = M3PrimitiveBits.packedWordCount(to - from, width);
            if (width < Long.SIZE && words < to - from) {
                widths[block] = (byte) width;
                offsets[block] = Math.toIntExact(wordCount);
                wordCount += words;
            } else {
                widths[block] = Long.SIZE;
                offsets[block] = plainCount;
                plainCount += to - from;
            }
        }
        if (wordCount > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("packed array too large");
        }
        long[] packed = new long[(int) wordCount];
        long[] plain = new long[plainCount];
        for (int block = 0; block < blocks; block++) {
            int from = block * blockSize;
            int count = Math.min(blockSize, source.length - from);
            int width = widths[block];
            if (width == Long.SIZE) {
                System.arraycopy(source, from, plain, offsets[block], count);
            } else if (width != 0) {
                long firstBit = (long) offsets[block] * Long.SIZE;
                for (int i = 0; i < count; i++) {
                    M3PrimitiveBits.writePacked(packed, firstBit + (long) i * width,
                            width, source[from + i] - bases[block]);
                }
            }
        }
        return new M3BlockPackedLongArray(
                source.length, blockSize, bases, widths, offsets, packed, plain);
    }

    public int size() { return size; }
    public int blockSize() { return blockSize; }
    public int blocks() { return widths.length; }
    public int blockBitWidth(int block) {
        return widths[Objects.checkIndex(block, widths.length)];
    }

    public long get(int index) {
        Objects.checkIndex(index, size);
        int block = index / blockSize;
        int width = widths[block];
        int within = index % blockSize;
        if (width == 0) return bases[block];
        if (width == Long.SIZE) return plain[offsets[block] + within];
        return bases[block] + M3PrimitiveBits.readPacked(packed,
                (long) offsets[block] * Long.SIZE + (long) within * width, width);
    }

    public long[] toArray() {
        long[] result = new long[size];
        for (int i = 0; i < size; i++) result[i] = get(i);
        return result;
    }

    /** Retained primitive payload including block metadata; excludes JVM object headers. */
    public long payloadBytes() {
        return (long) bases.length * Long.BYTES + widths.length
                + (long) offsets.length * Integer.BYTES
                + ((long) packed.length + plain.length) * Long.BYTES;
    }
}
