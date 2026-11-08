// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.util.Objects;

/**
 * Immutable blockwise exact integer bitplanes: each block independently selects
 * its signed minimum and 0..32-bit width, like per-group weight quantization,
 * but without losing integer precision or ordinary Java array behavior.
 *
 * <p>Values are reconstructed directly from owned bitplanes. There are no
 * per-element objects, eager flattening or intermediate source-block copies.
 */
public final class AdaptivePackedIntBlocks {
    private final int length;
    private final int blockSize;
    private final AdaptivePackedIntArray[] blocks;
    private final long payloadBytes;

    private AdaptivePackedIntBlocks(int length, int blockSize, AdaptivePackedIntArray[] blocks) {
        this.length = length;
        this.blockSize = blockSize;
        this.blocks = blocks;
        long bytes = 0;
        for (AdaptivePackedIntArray block : blocks) bytes += block.payloadBytes();
        this.payloadBytes = bytes;
    }

    /** Build independently packed blocks without per-block int-array copies. */
    public static AdaptivePackedIntBlocks of(int[] source, int blockSize) {
        Objects.requireNonNull(source, "source");
        if (blockSize <= 0) throw new IllegalArgumentException("blockSize must be positive");
        int count = (int) (((long) source.length + blockSize - 1L) / blockSize);
        AdaptivePackedIntArray[] blocks = new AdaptivePackedIntArray[count];
        for (int i = 0; i < count; i++) {
            int from = (int) ((long) i * blockSize);
            int to = (int) Math.min((long) from + blockSize, source.length);
            blocks[i] = AdaptivePackedIntArray.ofRange(source, from, to);
        }
        return new AdaptivePackedIntBlocks(source.length, blockSize, blocks);
    }

    public int length() { return length; }
    public int blockSize() { return blockSize; }
    public int blockCount() { return blocks.length; }
    public long payloadBytes() { return payloadBytes; }

    /** Exact bit width selected for this block. */
    public int blockBits(int blockIndex) {
        Objects.checkIndex(blockIndex, blocks.length);
        return blocks[blockIndex].bitsPerValue();
    }

    /** Exact O(1) random access, with one block selection. */
    public int get(int index) {
        Objects.checkIndex(index, length);
        return blocks[index / blockSize].get(index % blockSize);
    }

    /** Return a detached ordinary int array with no intermediate block copies. */
    public int[] toIntArray() {
        int[] values = new int[length];
        int offset = 0;
        for (AdaptivePackedIntArray block : blocks) {
            for (int i = 0; i < block.length(); i++) values[offset + i] = block.get(i);
            offset += block.length();
        }
        return values;
    }
}
