// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Real Java/JNI parity against independent loops, separate from fake-JNI transfer counts. */
final class M3IndexMaskCompactorSpanTest {
    @Test void javaSelectionKeepsIndependentWindowAndValueContracts() {
        verify(M3IndexMaskCompactor.Backend.JAVA);
        invalidInputs(M3IndexMaskCompactor.Backend.JAVA);
        invalidInputs(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
    }

    @Test void nativeSelectionKeepsIndependentWindowAndValueContracts() {
        if (Boolean.getBoolean("m3.collections.all.native.required")) {
            assertTrue(M3IndexMaskCompactor.nativeAvailable(), "required owning JNI library");
        }
        assumeTrue(M3IndexMaskCompactor.nativeAvailable(), "optional JNI is absent in the Java lane");
        verify(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
    }

    private static void verify(M3IndexMaskCompactor.Backend backend) {
        int[] lengths = {0, 1, 127, 128, 255, 256, 257, 511, 512, 513, 600, 4096, 4097};
        int[] masks = {0, 1, 0x80, 0xa5, 0xff};
        Random random = new Random(0x4d33434f4d504143L);
        for (int length : lengths) {
            for (int rowOffset : new int[] {0, 17}) {
                for (int stateOffset : new int[] {0, 23}) {
                    int[] rows = new int[rowOffset + length + 11];
                    long[] longRows = new long[rows.length];
                    byte[] states = new byte[stateOffset + length + 13];
                    fillRows(rows, longRows);
                    for (int layout = 0; layout < 4; layout++) {
                        random.nextBytes(states);
                        for (int i = 0; i < length; i++) {
                            states[stateOffset + i] = switch (layout) {
                                case 0 -> 0;
                                case 1 -> (byte)0xff;
                                case 2 -> (byte)(i % 257 == 0 ? 0x80 : 0);
                                default -> (byte)i;
                            };
                        }
                        for (int mask : masks) {
                            compare(rows, longRows, rowOffset, states, stateOffset, length, mask, backend);
                        }
                    }
                }
            }
        }
        int[] rows = new int[628];
        long[] longRows = new long[rows.length];
        fillRows(rows, longRows);
        byte[] states = new byte[636];
        for (int position = 0; position < 600; position++) {
            Arrays.fill(states, (byte)0);
            states[23 + position] = (byte)0x80;
            compare(rows, longRows, 17, states, 23, 600, 0x80, backend);
        }
    }

    private static void fillRows(int[] rows, long[] longRows) {
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i % 5 == 0 ? Integer.MIN_VALUE
                    : i % 7 == 0 ? Integer.MAX_VALUE : i % 13 - 6;
            longRows[i] = i % 5 == 0 ? Long.MIN_VALUE
                    : i % 7 == 0 ? Long.MAX_VALUE : (long)i * 0x100000001L - 1_000_000_000_000L;
        }
    }

    private static void compare(int[] rows, long[] longRows, int rowOffset, byte[] states,
            int stateOffset, int length, int mask, M3IndexMaskCompactor.Backend backend) {
        int[] originalRows = rows.clone();
        long[] originalLongs = longRows.clone();
        byte[] originalStates = states.clone();
        int[] expectedBuffer = new int[length];
        long[] expectedLongBuffer = new long[length];
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (((states[stateOffset + i] & 0xff) & mask) == mask) {
                expectedBuffer[count] = rows[rowOffset + i];
                expectedLongBuffer[count++] = longRows[rowOffset + i];
            }
        }
        int[] expected = Arrays.copyOf(expectedBuffer, count);
        long[] expectedLong = Arrays.copyOf(expectedLongBuffer, count);
        int[] actual = M3IndexMaskCompactor.selectWindow(
                rows, rowOffset, states, stateOffset, length, mask, backend);
        long[] actualLong = M3IndexMaskCompactor.selectLongWindow(
                longRows, rowOffset, states, stateOffset, length, mask, backend);
        assertArrayEquals(expected, actual);
        assertArrayEquals(expectedLong, actualLong);
        assertNotSame(rows, actual);
        assertNotSame(longRows, actualLong);
        byte[] aligned = Arrays.copyOfRange(states, stateOffset, stateOffset + length);
        int[] wholeRows = Arrays.copyOfRange(rows, rowOffset, rowOffset + length);
        assertArrayEquals(expected, M3IndexMaskCompactor.select(wholeRows, aligned, mask, backend));
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            assertArrayEquals(expected,
                    M3IndexMaskCompactor.selectNativeRange(rows, rowOffset, length, aligned, mask));
        }
        if (count != 0) {
            actual[0] ^= 1;
            actualLong[0] ^= 1L;
        }
        assertArrayEquals(originalRows, rows);
        assertArrayEquals(originalLongs, longRows);
        assertArrayEquals(originalStates, states);
    }

    private static void invalidInputs(M3IndexMaskCompactor.Backend backend) {
        int[] rows = {1, 2};
        long[] longs = {1, 2};
        byte[] states = {0, (byte)0x80};
        assertThrows(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectWindow(null, 0, states, 0, 0, 0, backend));
        assertThrows(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectLongWindow(longs, 0, null, 0, 0, 0, backend));
        assertThrows(IndexOutOfBoundsException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, -1, states, 0, 0, 0, backend));
        assertThrows(IndexOutOfBoundsException.class,
                () -> M3IndexMaskCompactor.selectLongWindow(longs, 0, states, 1, 2, 0, backend));
        assertThrows(IndexOutOfBoundsException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, 1, states, 0, Integer.MAX_VALUE, 0, backend));
        assertThrows(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, 0, states, 0, 2, -1, backend));
        assertThrows(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.selectLongWindow(longs, 0, states, 0, 2, 256, backend));
        assertThrows(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.select(rows, new byte[1], 0, backend));
    }
}
