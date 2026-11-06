// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Original Java zero-mask window contracts; the required-JNI IT reuses these oracles. */
final class M3ZeroMaskSuccessorTest {
    private static final int[] LENGTHS = {0, 1, 31, 32, 63, 64, 255, 256, 257};
    private static final int ROW_OFFSET = 3;
    private static final int STATE_OFFSET = 5;
    private static final int CAPACITY = 280;

    @Test
    void intWindowsJavaAreDetachedAndByteStateIndependent() {
        verifyInt(M3IndexMaskCompactor.Backend.JAVA);
    }

    @Test
    void longWindowsJavaPreserveSignedExtremesAndDuplicates() {
        verifyLong(M3IndexMaskCompactor.Backend.JAVA);
    }

    static void verifyInt(M3IndexMaskCompactor.Backend backend) {
        int[] values = new int[CAPACITY];
        byte[] states = new byte[CAPACITY];
        for (int i = 0; i < values.length; i++) {
            values[i] = i % 4 == 0 ? Integer.MIN_VALUE
                    : i % 4 == 1 ? Integer.MAX_VALUE : i % 4 == 2 ? -7 : -7;
            states[i] = (byte) (i * 13 + 1);
        }
        int[] original = values.clone();
        byte[] originalStates = states.clone();
        for (int length : LENGTHS) {
            int[] expected = Arrays.copyOfRange(original, ROW_OFFSET, ROW_OFFSET + length);
            int[] selected = M3IndexMaskCompactor.selectWindow(
                    values, ROW_OFFSET, states, STATE_OFFSET, length, 0, backend);
            assertArrayEquals(expected, selected);
            assertNotSame(values, selected);
            assertArrayEquals(original, values);
            assertArrayEquals(originalStates, states);
        }
        assertThrows(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(
                values, -1, states, STATE_OFFSET, 1, 0, backend));
        assertThrows(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(
                values, ROW_OFFSET, states, -1, 1, 0, backend));
    }

    static void verifyLong(M3IndexMaskCompactor.Backend backend) {
        long[] values = new long[CAPACITY];
        byte[] states = new byte[CAPACITY];
        for (int i = 0; i < values.length; i++) {
            values[i] = i % 4 == 0 ? Long.MIN_VALUE
                    : i % 4 == 1 ? Long.MAX_VALUE : i % 4 == 2 ? -7L : -7L;
            states[i] = (byte) (i * 13 + 1);
        }
        long[] original = values.clone();
        byte[] originalStates = states.clone();
        for (int length : LENGTHS) {
            long[] expected = Arrays.copyOfRange(original, ROW_OFFSET, ROW_OFFSET + length);
            long[] selected = M3IndexMaskCompactor.selectLongWindow(
                    values, ROW_OFFSET, states, STATE_OFFSET, length, 0, backend);
            assertArrayEquals(expected, selected);
            assertNotSame(values, selected);
            assertArrayEquals(original, values);
            assertArrayEquals(originalStates, states);
        }
        assertThrows(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectLongWindow(
                values, -1, states, STATE_OFFSET, 1, 0, backend));
        assertThrows(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectLongWindow(
                values, ROW_OFFSET, states, -1, 1, 0, backend));
    }
}
