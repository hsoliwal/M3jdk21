// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Each algorithmic atom has an independently executable Java/JNI contract gate. */
final class IntLaneAlgorithmsTest {
    @Test
    void binarySearchBoundariesMatchSortedDuplicatesAndSignedExtremes() {
        int[] lane = {Integer.MIN_VALUE, -2, -2, 0, 0, 0, Integer.MAX_VALUE};
        assertEquals(1, IntLaneAlgorithms.lowerBound(lane, 0, 7, -2));
        assertEquals(3, IntLaneAlgorithms.upperBound(lane, 0, 7, -2));
        assertArrayEquals(new int[] {3, 6}, IntLaneAlgorithms.equalRange(lane, 0, 7, 0));
        assertArrayEquals(new int[] {3, 3}, IntLaneAlgorithms.equalRange(lane, 0, 7, -1));
        assertEquals(7, IntLaneAlgorithms.upperBound(lane, 0, 7, Integer.MAX_VALUE));
        assertEquals(4, IntLaneAlgorithms.lowerBound(lane, 4, 4, 0));
    }

    @Test
    void stableCompactionKeepsEncounterOrderAndOutsideSlices() {
        int[] lane = {99, 4, 0, 2, 0, 6, 88};
        int end = IntLaneAlgorithms.compactExcept(lane, 1, 6, 0);
        assertEquals(4, end);
        assertArrayEquals(new int[] {99, 4, 2, 6}, Arrays.copyOf(lane, end));
        assertEquals(88, lane[6]);
        assertEquals(1, IntLaneAlgorithms.compactExcept(new int[] {0, 0}, 1, 1, 0));
    }

    @Test
    void rotationSupportsSlicesNegativeDistanceAndExtremes() {
        int[] lane = {99, 1, 2, 3, 4, 88};
        IntLaneAlgorithms.rotateLeft(lane, 1, 5, -1);
        assertArrayEquals(new int[] {99, 4, 1, 2, 3, 88}, lane);
        IntLaneAlgorithms.rotateLeft(lane, 1, 5, Integer.MIN_VALUE);
        assertArrayEquals(new int[] {99, 4, 1, 2, 3, 88}, lane);
        assertThrows(IndexOutOfBoundsException.class,
                () -> IntLaneAlgorithms.rotateLeft(lane, 1, 7, 3));
    }

    @Test
    void differenceArrayRangeAddIsExactAndRejectsOverflow() {
        assertEquals(200, IntLaneAlgorithms.rangeAddMaximum(5,
                new int[] {0, 1, 2}, new int[] {1, 4, 3},
                new long[] {100, 100, 100}));
        assertEquals(-7, IntLaneAlgorithms.rangeAddMaximum(2,
                new int[] {0}, new int[] {1}, new long[] {-7}));
        assertEquals(0, IntLaneAlgorithms.rangeAddMaximum(0, new int[0], new int[0], new long[0]));
        assertThrows(ArithmeticException.class,
                () -> IntLaneAlgorithms.rangeAddMaximum(2,
                        new int[] {0}, new int[] {0}, new long[] {Long.MIN_VALUE}));
    }

    @Test
    void monotonicDequeKeepsEqualValuesAndWindowExtremes() {
        assertArrayEquals(new int[] {3, 3, 5, 5, 6, 7},
                IntLaneAlgorithms.slidingWindowMaximum(
                        new int[] {1, 3, -1, -3, 5, 3, 6, 7}, 3));
        assertArrayEquals(new int[] {7, 7, 7}, IntLaneAlgorithms.slidingWindowMaximum(
                new int[] {7, 7, 7, 7}, 2));
        assertThrows(IllegalArgumentException.class,
                () -> IntLaneAlgorithms.slidingWindowMaximum(new int[0], 0));
    }

    @Test
    void jniRequiredNeverFallsBackWhenUnavailable() {
        if (!ArrayNative.isLoaded()
                && System.getProperty("m3.arrays.native.path") == null) {
            assertThrows(UnsatisfiedLinkError.class,
                    () -> IntLaneAlgorithms.lowerBoundRequired(new int[] {1, 2}, 0, 2, 1));
        } else {
            int[] lane = {-2, -2, 3, 3, Integer.MAX_VALUE};
            assertEquals(2, IntLaneAlgorithms.lowerBoundRequired(lane, 0, lane.length, 3));
            assertEquals(4, IntLaneAlgorithms.upperBoundRequired(lane, 0, lane.length, 3));
        }
        assertThrows(IndexOutOfBoundsException.class,
                () -> IntLaneAlgorithms.upperBoundRequired(new int[0], 0, 1, 0));
    }

    @Test
    void seededNativeAndJavaDifferentialMaintainsAllAtoms() {
        IntLaneAlgorithmsSelfTest.run();
        assertTrue(true, "all deterministic independently computed oracles completed");
    }
}
