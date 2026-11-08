// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.mat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Regression for valid large block sizes and independent array-observation parity. */
final class MatBlockIntVectorBoundaryTest {
    @Test
    void twoValuesAcceptTheLargestPositiveBlockSize() {
        int[] values = {1, 2};
        MatBlockIntVector vector = MatBlockIntVector.of(values, Integer.MAX_VALUE);
        assertArrayEquals(values, vector.toArray());
        assertEquals(1, vector.blocks());
        assertEquals(Integer.MAX_VALUE, vector.blockSize());
    }

    @Test
    void fourValuesCannotSilentlyLoseTheirOnlyBlock() {
        int[] values = {Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE};
        MatBlockIntVector vector = MatBlockIntVector.of(values, Integer.MAX_VALUE);
        assertEquals(1, vector.blocks());
        assertArrayEquals(values, vector.toArray());
    }

    @Test
    void largeAndOrdinaryBlockSizesPreserveEveryInputValue() {
        Random random = new Random(0x4d33424c4f434bL);
        int[] sizes = {1, 2, 3, 7, 8, 17, 63, 64, 65, 255, 256, 257,
                1 << 30, Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        for (int length : new int[] {0, 1, 2, 3, 4, 7, 31, 63, 64, 65, 127, 255, 256, 257, 529}) {
            for (int shape = 0; shape < 4; shape++) {
                int[] values = new int[length];
                for (int index = 0; index < length; index++) {
                    values[index] = switch (shape) {
                        case 0 -> -7;
                        case 1 -> random.nextInt(8) - 4;
                        case 2 -> random.nextInt();
                        default -> (index & 1) == 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                    };
                }
                for (int size : sizes) {
                    MatBlockIntVector vector = MatBlockIntVector.of(values, size);
                    assertEquals(length, vector.size());
                    assertEquals(size, vector.blockSize());
                    assertEquals(((long) length + size - 1L) / size, vector.blocks());
                    assertArrayEquals(values, vector.toArray(), "length=" + length + ", blockSize=" + size);
                    for (int index = 0; index < length; index++) assertEquals(values[index], vector.get(index));
                    assertEquals(vector.blocks(), vector.constantBlocks() + vector.packedBlocks() + vector.plainBlocks());
                    assertTrue(vector.primitivePayloadBytes() >= 0);
                }
            }
        }
    }

    @Test
    void constantPackedAndPlainCapabilitiesRemainAvailable() {
        int[] values = new int[3 * MatBlockIntVector.DEFAULT_BLOCK_SIZE];
        Arrays.fill(values, 0, 256, 9);
        for (int index = 256; index < 512; index++) values[index] = (index & 7) - 4;
        for (int index = 512; index < values.length; index++) {
            values[index] = (index & 1) == 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        MatBlockIntVector vector = MatBlockIntVector.of(values);
        assertEquals(1, vector.constantBlocks());
        assertEquals(1, vector.packedBlocks());
        assertEquals(1, vector.plainBlocks());
        assertArrayEquals(values, vector.toArray());
    }

    @Test
    void callerAndReturnedArraysDoNotAliasCanonicalValues() {
        int[] input = {3, 4, 5};
        MatBlockIntVector vector = MatBlockIntVector.of(input, Integer.MAX_VALUE);
        input[0] = 99;
        int[] exported = vector.toArray();
        exported[1] = 88;
        assertArrayEquals(new int[] {3, 4, 5}, vector.toArray());
    }

    @Test
    void emptyInputAcceptsEveryPositiveBoundarySize() {
        for (int size : new int[] {1, 256, Integer.MAX_VALUE}) {
            MatBlockIntVector vector = MatBlockIntVector.of(new int[0], size);
            assertEquals(0, vector.size());
            assertEquals(0, vector.blocks());
            assertEquals(0, vector.primitivePayloadBytes());
            assertArrayEquals(new int[0], vector.toArray());
        }
    }

    @Test
    void nullAndBlockValidationKeepTheirOrderAndMessages() {
        assertEquals("values", assertThrows(NullPointerException.class,
                () -> MatBlockIntVector.of(null, 0)).getMessage());
        for (int size : new int[] {0, -1, Integer.MIN_VALUE}) {
            assertEquals("blockSize", assertThrows(IllegalArgumentException.class,
                    () -> MatBlockIntVector.of(new int[] {1}, size)).getMessage());
        }
    }

    @Test
    void indexBoundsRemainChecked() {
        MatBlockIntVector vector = MatBlockIntVector.of(new int[] {7, 8}, Integer.MAX_VALUE);
        assertThrows(IndexOutOfBoundsException.class, () -> vector.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> vector.get(2));
        assertThrows(IndexOutOfBoundsException.class,
                () -> MatBlockIntVector.of(new int[0], 1).get(0));
    }
}
