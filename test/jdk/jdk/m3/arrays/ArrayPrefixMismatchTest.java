// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * LC14 first-difference category: exact signed UTF-16 delta, offsets, aliases and JNI parity.
 * The historical scalar loop is the independent oracle, never the new implementation.
 */
final class ArrayPrefixMismatchTest {
    @Test
    void equalLongChunksAndMismatchesHaveExactLegacyDeltas() {
        loadRequestedNative();
        Random random = new Random(0x4d334c433134L);
        long cases = 0;
        for (int length : new int[] {
                0, 1, 2, 3, 7, 8, 15, 16, 31, 32, 63, 64, 127, 128, 255, 256,
                1023, 1024, 1025, 2048, 4096, 4097
        }) {
            for (int leftStart = 0; leftStart < 5; leftStart++) {
                for (int rightStart = 0; rightStart < 5; rightStart++) {
                    char[] left = randomUnits(random, leftStart + length + 1);
                    char[] right = randomUnits(random, rightStart + length + 1);
                    System.arraycopy(left, leftStart, right, rightStart, length);
                    verify(left, leftStart, right, rightStart, length);
                    cases++;
                    if (length > 0) {
                        for (int at : new int[] {0, length / 2, length - 1}) {
                            right[rightStart + at] ^= (char) 0x8001;
                            verify(left, leftStart, right, rightStart, length);
                            right[rightStart + at] ^= (char) 0x8001;
                            cases++;
                        }
                    }
                }
            }
        }
        for (int trial = 0; trial < 8192; trial++) {
            int length = random.nextInt(4098);
            int leftStart = random.nextInt(7);
            int rightStart = random.nextInt(7);
            char[] alias = randomUnits(random, 7 + length);
            verify(alias, leftStart, alias, rightStart, length);
            cases++;
        }
        assertTrue(cases > 9_000L, "the exhaustive boundary matrix must run");
    }

    @Test
    void nullRangeAndUnsignedCodeUnitsPreservePublicContract() {
        char[] units = {'\u0000', '\uffff', '\ud800', '\udfff'};
        assertEquals(-65535, ArrayNative.compareUtf16(units, 0, units, 1, 1));
        assertEquals(65535, ArrayNative.compareUtf16(units, 1, units, 0, 1));
        assertEquals(0, ArrayNative.compareUtf16(units, 4, units, 4, 0));
        assertEquals("left", assertThrows(NullPointerException.class,
                () -> ArrayNative.compareUtf16(null, -1, null, -1, -1)).getMessage());
        assertEquals("right", assertThrows(NullPointerException.class,
                () -> ArrayNative.compareUtf16(units, -1, null, -1, -1)).getMessage());
        for (int[] invalid : new int[][] {
                {-1, 0}, {0, -1}, {5, 0}, {3, 2}, {1, Integer.MAX_VALUE}
        }) {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> ArrayNative.compareUtf16(units, invalid[0], units, 0, invalid[1]));
            assertThrows(IndexOutOfBoundsException.class,
                    () -> ArrayNative.compareUtf16(units, 0, units, invalid[0], invalid[1]));
        }
    }

    private static void loadRequestedNative() {
        String configured = System.getProperty("m3.arrays.native.path");
        if (configured != null && !configured.isBlank()) {
            ArrayNative.load(Path.of(configured));
            assertTrue(ArrayNative.isLoaded());
        }
        if (Boolean.getBoolean("m3.arrays.native.required")) {
            assertTrue(ArrayNative.isLoaded(), "native-required must not silently fall back");
        }
    }

    private static char[] randomUnits(Random random, int length) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) result[i] = (char) random.nextInt(65536);
        return result;
    }

    private static int scalar(char[] left, int leftStart, char[] right, int rightStart, int count) {
        for (int i = 0; i < count; i++) {
            int difference = left[leftStart + i] - right[rightStart + i];
            if (difference != 0) return difference;
        }
        return 0;
    }

    private static void verify(char[] left, int leftStart, char[] right, int rightStart, int count) {
        char[] leftSnapshot = left.clone();
        char[] rightSnapshot = right.clone();
        int expected = scalar(left, leftStart, right, rightStart, count);
        assertEquals(expected, ArrayNative.compareUtf16(left, leftStart, right, rightStart, count));
        assertEquals(-expected, ArrayNative.compareUtf16(right, rightStart, left, leftStart, count));
        assertArrayEquals(leftSnapshot, left, "caller left array must not change");
        assertArrayEquals(rightSnapshot, right, "caller right array must not change");
    }
}
