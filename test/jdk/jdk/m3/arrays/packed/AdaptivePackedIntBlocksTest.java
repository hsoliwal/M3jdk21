// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AdaptivePackedIntBlocksTest {
    @Test
    void varyingBlockWidthPreservesExactValues() {
        int[] values = new int[513];
        Random r = new Random(39);
        for (int i = 0; i < values.length; i++) {
            if (i < 256) values[i] = i & 3;
            else if (i < 512) values[i] = r.nextInt();
            else values[i] = Integer.MIN_VALUE;
        }
        for (int blockSize : new int[] {1, 3, 7, 64, 256, 1024}) {
            AdaptivePackedIntBlocks packed = AdaptivePackedIntBlocks.of(values, blockSize);
            assertArrayEquals(values, packed.toIntArray());
            assertEquals(values.length, packed.length());
            assertEquals((values.length + (long) blockSize - 1L) / blockSize, packed.blockCount());
            for (int i = 0; i < values.length; i++) assertEquals(values[i], packed.get(i));
            assertThrows(IndexOutOfBoundsException.class, () -> packed.get(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> packed.get(values.length));
        }
        AdaptivePackedIntBlocks perBlock = AdaptivePackedIntBlocks.of(values, 256);
        assertEquals(2, perBlock.blockBits(0));
        assertEquals(32, perBlock.blockBits(1));
        assertEquals(0, perBlock.blockBits(2));
        assertTrue(perBlock.payloadBytes() < AdaptivePackedIntArray.of(values).payloadBytes());
        values[0] = 99999;
        assertEquals(0, perBlock.get(0));
    }

    @Test
    void emptyAndMalformedInputs() {
        assertEquals(0, AdaptivePackedIntBlocks.of(new int[0], 1).blockCount());
        assertThrows(IllegalArgumentException.class, () -> AdaptivePackedIntBlocks.of(new int[0], 0));
        assertThrows(NullPointerException.class, () -> AdaptivePackedIntBlocks.of(null, 4));
        assertThrows(IndexOutOfBoundsException.class,
                () -> AdaptivePackedIntArray.ofRange(new int[] {1, 2}, 1, 3));
        assertArrayEquals(new int[] {2},
                AdaptivePackedIntArray.ofRange(new int[] {1, 2}, 1, 2).toIntArray());
    }
}
