// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Per-atom JUnit alongside the independent deterministic TreeMap oracle. */
final class M3LongCoordinateIndexTest {
    @Test
    void twoBinaryBoundariesHandleDuplicatesRefusalAndSignedExtremes() {
        M3LongCoordinateIndex index = new M3LongCoordinateIndex(
                new long[] {Long.MIN_VALUE, -1, 0, 12, Long.MAX_VALUE},
                new long[] {7, -2, 0, 18, 9});
        assertEquals(0, index.lowerBound(Long.MIN_VALUE));
        assertEquals(1, index.upperBound(Long.MIN_VALUE));
        assertEquals(5, index.upperBound(Long.MAX_VALUE));
        assertEquals(2, index.lowerBound(0));
        assertArrayEquals(new int[] {1, 4}, index.ordinalsInRange(-1, 13));
        assertThrows(IllegalArgumentException.class,
                () -> new M3LongCoordinateIndex(new long[] {4, 4}, new long[] {1, 2}));
    }

    @Test
    void rangeCountAndZeroStoredValueAreNotConflatedWithAbsence() {
        M3LongCoordinateIndex index = new M3LongCoordinateIndex(
                new long[] {-2, 0, 9}, new long[] {3, 0, 7});
        assertTrue(index.contains(0));
        assertFalse(index.contains(2));
        assertEquals(0, index.valueOrDefault(0, 123));
        assertEquals(123, index.valueOrDefault(2, 123));
        assertEquals(2, index.countInRange(-2, 9));
        assertEquals(0, index.countInRange(9, 9));
        assertThrows(IllegalArgumentException.class, () -> index.countInRange(6, -6));
    }

    @Test
    void prefixRangeSumUsesCheckedLongArithmetic() {
        M3LongCoordinateIndex index = new M3LongCoordinateIndex(
                new long[] {-5, 0, 7}, new long[] {5, -10, 3});
        assertEquals(-2, index.sumAll());
        assertEquals(-7, index.sumInRange(0, 9));
        assertEquals(-10, index.sumByOrdinal(1, 2));
        assertEquals(0, index.sumInRange(10, 10));
        assertThrows(ArithmeticException.class,
                () -> new M3LongCoordinateIndex(new long[] {1, 2},
                        new long[] {Long.MAX_VALUE, 1}));
        M3LongCoordinateIndex oppositePrefixes = new M3LongCoordinateIndex(
                new long[] {1, 2, 3},
                new long[] {Long.MAX_VALUE, -Long.MAX_VALUE, -2});
        assertThrows(ArithmeticException.class,
                () -> oppositePrefixes.sumInRange(2, 4));
    }

    @Test
    void primitiveInputAndOutputArraysAreDetached() {
        long[] keys = {2, 4, 6};
        long[] vals = {20, 40, 60};
        M3LongCoordinateIndex index = new M3LongCoordinateIndex(keys, vals);
        keys[0] = 3;
        vals[0] = -8;
        assertEquals(2, index.keyAt(0));
        assertEquals(20, index.valueAt(0));
        long[] export = index.keysToArray();
        export[0] = 500;
        assertEquals(2, index.keyAt(0));
        assertArrayEquals(new long[] {20, 40, 60}, index.valuesToArray());
    }

    @Test
    void fromSparseIndexesOnlyNonzeroLongLocationsAndRetainsSnapshot() {
        M3SparseLongIntArray sparse = new M3SparseLongIntArray(Long.MAX_VALUE);
        long a = (long) Integer.MAX_VALUE + 1L;
        long b = Long.MAX_VALUE - 1;
        sparse.set(b, 3);
        sparse.set(0, 4);
        sparse.set(a, -2);
        sparse.set(a + 1, 0);
        M3LongCoordinateIndex index = M3LongCoordinateIndex.fromSparse(sparse);
        assertArrayEquals(new long[] {0, a, b}, index.keysToArray());
        assertArrayEquals(new long[] {4, -2, 3}, index.valuesToArray());
        assertEquals(5, index.sumAll());
        sparse.clear();
        assertEquals(5, index.sumAll());
    }

    @Test
    void nativeRequiredFailsClosedOrMatchesJavaOnLongValues() {
        M3LongCoordinateIndex index = new M3LongCoordinateIndex(
                new long[] {Long.MIN_VALUE, -8, 3, 3_000_000_000L, Long.MAX_VALUE},
                new long[] {1, 1, 1, 1, 1});
        String path = System.getProperty("m3.arrays.native.path");
        if (!M3ArrayNative.isLoaded() && (path == null || path.isBlank())) {
            assertThrows(UnsatisfiedLinkError.class,
                    () -> index.lowerBoundRequiredNative(3));
        } else {
            assertEquals(index.lowerBound(3), index.lowerBoundRequiredNative(3));
            assertEquals(index.upperBound(Long.MAX_VALUE),
                    index.upperBoundRequiredNative(Long.MAX_VALUE));
        }
    }

    @Test
    void javaBatchBoundsPreserveOrderExtremesAndQueryIdentity() {
        M3LongCoordinateIndex index = new M3LongCoordinateIndex(
                new long[] {Long.MIN_VALUE, -4, 0, 100, Long.MAX_VALUE},
                new long[] {1, 2, 3, 4, 5});
        long[] queries = {Long.MIN_VALUE, -4, -4, -3, 0, 100,
                Long.MAX_VALUE, Long.MAX_VALUE, 99};
        long[] saved = queries.clone();
        assertArrayEquals(new int[] {0, 1, 1, 2, 2, 3, 4, 4, 3},
                index.lowerBounds(queries));
        assertArrayEquals(new int[] {1, 2, 2, 2, 3, 4, 5, 5, 3},
                index.upperBounds(queries));
        assertArrayEquals(saved, queries);
        assertArrayEquals(new int[0], index.lowerBounds(new long[0]));
        assertArrayEquals(new int[0], index.upperBounds(new long[0]));
        assertThrows(NullPointerException.class, () -> index.lowerBounds(null));
        assertThrows(NullPointerException.class, () -> index.upperBounds(null));
    }

    @Test
    void requiredNativeBatchMatchesJavaAndRefusesSilentFallback() {
        M3LongCoordinateIndex index = new M3LongCoordinateIndex(
                new long[] {Long.MIN_VALUE, -3, 0, Long.MAX_VALUE},
                new long[] {1, 2, 3, 4});
        long[] queries = {Long.MIN_VALUE, -3, 1, Long.MAX_VALUE};
        assertThrows(NullPointerException.class,
                () -> index.lowerBoundsRequiredNative(null));
        assertThrows(NullPointerException.class,
                () -> index.upperBoundsRequiredNative(null));
        String path = System.getProperty("m3.arrays.native.path");
        if (!M3ArrayNative.isLoaded() && (path == null || path.isBlank())) {
            assertThrows(UnsatisfiedLinkError.class,
                    () -> index.lowerBoundsRequiredNative(queries));
            assertThrows(UnsatisfiedLinkError.class,
                    () -> index.upperBoundsRequiredNative(new long[0]));
        } else {
            assertArrayEquals(index.lowerBounds(queries), index.lowerBoundsRequiredNative(queries));
            assertArrayEquals(index.upperBounds(queries), index.upperBoundsRequiredNative(queries));
            assertArrayEquals(new int[0], index.lowerBoundsRequiredNative(new long[0]));
            assertArrayEquals(new int[0], index.upperBoundsRequiredNative(new long[0]));
        }
    }

    @Test
    void largeQueryBatchCrossesBothNativeBoundaries() {
        int size = 4097;
        long[] keys = new long[size];
        long[] values = new long[size];
        for (int i = 0; i < size; i++) keys[i] = (long) i * 3 - 6012;
        M3LongCoordinateIndex index = new M3LongCoordinateIndex(keys, values);
        long[] queries = new long[8193];
        for (int i = 0; i < queries.length; i++) {
            queries[i] = switch (i % 7) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> keys[0];
                case 3 -> keys[size - 1];
                default -> (long) i - 4000;
            };
        }
        int[] lower = index.lowerBounds(queries);
        int[] upper = index.upperBounds(queries);
        for (int i = 0; i < queries.length; i++) {
            assertEquals(index.lowerBound(queries[i]), lower[i]);
            assertEquals(index.upperBound(queries[i]), upper[i]);
        }
        String path = System.getProperty("m3.arrays.native.path");
        if (M3ArrayNative.isLoaded() || (path != null && !path.isBlank())) {
            assertArrayEquals(lower, index.lowerBoundsRequiredNative(queries));
            assertArrayEquals(upper, index.upperBoundsRequiredNative(queries));
        }
    }

    @Test
    void seededJavaAndJniDifferentialOracle() {
        M3LongCoordinateIndexSelfTest.run();
    }
}
