// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-18: require actual JNI when requested and qualify lane/offset/refusal boundaries.
package com.m3.arrays;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;

class M3ArrayNativeTest {
    @Test
    void javaFallbackHasExactCompareAndMaterializationSemantics() {
        char[] left = "abcdef".toCharArray();
        char[] right = "abcxef".toCharArray();

        assertEquals(0, M3ArrayNative.compareUtf16(left, 0, left, 0, left.length));
        assertTrue(M3ArrayNative.compareUtf16(left, 0, right, 0, left.length) < 0);
        assertArrayEquals(
                "abcdef".toCharArray(),
                M3ArrayNative.concatenateUtf16("ab".toCharArray(), "cd".toCharArray(), "ef".toCharArray()));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> M3ArrayNative.compareUtf16(left, 2, right, 2, 10));
        assertThrows(
                NullPointerException.class,
                () -> M3ArrayNative.concatenateUtf16("a".toCharArray(), null));
    }

    @Test
    void optionalNativeProviderMatchesJavaAcrossChunkAndUnicodeBoundaries() {
        if (!loadOptionalNative()) return;
        Random random = new Random(0x5eed);
        for (int length : new int[] {0, 1, 1023, 1024, 1025, 4097}) {
            char[] left = new char[length];
            char[] right = new char[length];
            for (int index = 0; index < length; index++) {
                left[index] = (char) random.nextInt(65536);
                right[index] = left[index];
            }
            assertEquals(0, M3ArrayNative.compareUtf16(left, 0, right, 0, length));
            if (length > 0) {
                int changed = length / 2;
                right[changed] = (char) ((right[changed] + 1) & 0xffff);
                int expected = left[changed] - right[changed];
                assertEquals(expected, M3ArrayNative.compareUtf16(left, 0, right, 0, length));
            }
        }

        char[] emoji = "\ud83d\ude00".toCharArray();
        assertArrayEquals(
                "A\ud83d\ude00B".toCharArray(),
                M3ArrayNative.concatenateUtf16(new char[] {'A', emoji[0]}, new char[] {emoji[1], 'B'}));
    }

    @Test
    void comparisonRetainsExactResultsAcrossLanesOffsetsAndSeededInputs() {
        loadOptionalNative();
        Random random = new Random(0x4d335554L);
        int[] lengths = {
            0, 1, 2, 3, 4, 5, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65,
            1023, 1024, 1025, 4097
        };
        for (int length : lengths) {
            for (int leftStart = 0; leftStart < 8; leftStart++) {
                for (int rightStart = 0; rightStart < 8; rightStart++) {
                    char[] left = randomUnits(random, leftStart + length);
                    char[] right = randomUnits(random, rightStart + length);
                    System.arraycopy(left, leftStart, right, rightStart, length);
                    assertComparison(left, leftStart, right, rightStart, length);
                    if (length <= 33) {
                        for (int changed = 0; changed < length; changed++) {
                            assertMismatch(left, leftStart, right, rightStart, length, changed);
                        }
                    } else {
                        int[] positions = {
                            0, 1, 2, 3, 4, 7, 8, 15, 16, 31, 32, 63, 64,
                            1022, 1023, 1024, length / 2, length - 1
                        };
                        for (int changed : positions) {
                            if (changed < length) {
                                assertMismatch(left, leftStart, right, rightStart, length, changed);
                            }
                        }
                    }
                }
            }
        }
        for (int trial = 0; trial < 4096; trial++) {
            int length = random.nextInt(4098);
            int leftStart = random.nextInt(8);
            int rightStart = random.nextInt(8);
            char[] left = randomUnits(random, leftStart + length);
            char[] right = randomUnits(random, rightStart + length);
            System.arraycopy(left, leftStart, right, rightStart, length);
            if (length != 0 && (trial & 1) != 0) {
                int changed = random.nextInt(length);
                right[rightStart + changed] ^= 0x8021;
            }
            assertComparison(left, leftStart, right, rightStart, length);
        }
        char[] overlap = randomUnits(random, 72);
        for (int leftStart = 0; leftStart < 8; leftStart++) {
            for (int rightStart = 0; rightStart < 8; rightStart++) {
                assertComparison(overlap, leftStart, overlap, rightStart, 64);
            }
        }
    }

    @Test
    void publicRangeNullAndEmptyValidationStaysExact() {
        loadOptionalNative();
        char[] units = {'\u0000', '\uffff', '\ud800', '\udfff'};
        assertEquals(0, M3ArrayNative.compareUtf16(units, units.length, units, units.length, 0));
        assertEquals(-65535, M3ArrayNative.compareUtf16(units, 0, units, 1, 1));
        assertEquals(65535, M3ArrayNative.compareUtf16(units, 1, units, 0, 1));
        assertEquals("left", assertThrows(NullPointerException.class,
                () -> M3ArrayNative.compareUtf16(null, -1, null, -1, -1)).getMessage());
        assertEquals("right", assertThrows(NullPointerException.class,
                () -> M3ArrayNative.compareUtf16(units, -1, null, -1, -1)).getMessage());
        for (int[] range : new int[][] {{-1, 0}, {0, -1}, {5, 0}, {3, 2}, {1, Integer.MAX_VALUE}}) {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> M3ArrayNative.compareUtf16(units, range[0], units, 0, range[1]));
            assertThrows(IndexOutOfBoundsException.class,
                    () -> M3ArrayNative.compareUtf16(units, 0, units, range[0], range[1]));
        }
        assertArrayEquals(new char[0], M3ArrayNative.concatenateUtf16());
        assertArrayEquals(units, M3ArrayNative.concatenateUtf16(new char[0], units, new char[0]));
    }

    @Test
    void actualJniDirectValidationKeepsItsOwnRefusalContract() throws ReflectiveOperationException {
        if (!loadOptionalNative()) return;
        Method compare = M3ArrayNative.class.getDeclaredMethod(
                "nativeCompareUtf16", char[].class, int.class, char[].class, int.class, int.class);
        compare.setAccessible(true);
        char[] units = {'a', 'b', 'c', 'd'};
        assertEquals(0, compare.invoke(null, units, 4, units, 4, 0));
        for (Object[] arguments : new Object[][] {
                {null, 0, units, 0, 0},
                {units, 0, null, 0, 0},
                {null, -1, null, -1, -1}
        }) {
            InvocationTargetException error = assertThrows(
                    InvocationTargetException.class, () -> compare.invoke(null, arguments));
            assertInstanceOf(IllegalArgumentException.class, error.getCause());
            assertEquals("null UTF-16 array", error.getCause().getMessage());
        }
        for (int[] range : new int[][] {{-1, 0}, {0, -1}, {5, 0}, {3, 2}, {1, Integer.MAX_VALUE}}) {
            for (Object[] arguments : new Object[][] {
                    {units, range[0], units, 0, range[1]},
                    {units, 0, units, range[0], range[1]}
            }) {
                InvocationTargetException error = assertThrows(
                        InvocationTargetException.class, () -> compare.invoke(null, arguments));
                assertInstanceOf(IllegalArgumentException.class, error.getCause());
                assertEquals("UTF-16 compare range", error.getCause().getMessage());
            }
        }
        Method concatenate = M3ArrayNative.class.getDeclaredMethod(
                "nativeConcatenateUtf16", char[][].class);
        concatenate.setAccessible(true);
        InvocationTargetException nullSegments = assertThrows(
                InvocationTargetException.class, () -> concatenate.invoke(null, (Object) null));
        assertInstanceOf(IllegalArgumentException.class, nullSegments.getCause());
        assertEquals("null UTF-16 segment array", nullSegments.getCause().getMessage());
        InvocationTargetException nullSegment = assertThrows(
                InvocationTargetException.class,
                () -> concatenate.invoke(null, (Object) new char[][] {units, null}));
        assertInstanceOf(IllegalArgumentException.class, nullSegment.getCause());
        assertEquals("null UTF-16 segment", nullSegment.getCause().getMessage());
    }

    private static boolean loadOptionalNative() {
        String path = System.getProperty("m3.arrays.native.path");
        if (path == null || path.isBlank()) {
            assertFalse(Boolean.getBoolean("m3.arrays.native.required"),
                    "Actual JNI qualification requires m3.arrays.native.path");
            return false;
        }
        M3ArrayNative.load(Path.of(path));
        assertTrue(M3ArrayNative.isLoaded(), "Requested JNI provider must actually load");
        return true;
    }

    private static char[] randomUnits(Random random, int length) {
        char[] units = new char[length];
        for (int index = 0; index < length; index++) units[index] = (char) random.nextInt(65536);
        return units;
    }

    private static int scalarCompare(char[] left, int leftStart, char[] right, int rightStart, int count) {
        for (int index = 0; index < count; index++) {
            int difference = left[leftStart + index] - right[rightStart + index];
            if (difference != 0) return difference;
        }
        return 0;
    }

    private static void assertComparison(
            char[] left, int leftStart, char[] right, int rightStart, int count) {
        char[] savedLeft = left.clone();
        char[] savedRight = right.clone();
        int expected = scalarCompare(left, leftStart, right, rightStart, count);
        assertEquals(expected, M3ArrayNative.compareUtf16(left, leftStart, right, rightStart, count),
                "count=" + count + ", leftStart=" + leftStart + ", rightStart=" + rightStart);
        assertEquals(-expected, M3ArrayNative.compareUtf16(right, rightStart, left, leftStart, count));
        assertArrayEquals(savedLeft, left, "left input unchanged");
        assertArrayEquals(savedRight, right, "right input unchanged");
    }

    private static void assertMismatch(
            char[] left, int leftStart, char[] right, int rightStart, int count, int changed) {
        char savedLeft = left[leftStart + changed];
        char savedRight = right[rightStart + changed];
        left[leftStart + changed] = 0;
        right[rightStart + changed] = 0xffff;
        assertComparison(left, leftStart, right, rightStart, count);
        left[leftStart + changed] = 0x00ff;
        right[rightStart + changed] = 0x0100;
        assertComparison(left, leftStart, right, rightStart, count);
        left[leftStart + changed] = savedLeft;
        right[rightStart + changed] = savedRight;
    }

}
