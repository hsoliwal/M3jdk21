// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent bisection oracles for monotone-cursor/galloping batch search. */
final class LongCoordinateGallopingTest {
    private static boolean nativeLoaded() {
        String configured = System.getProperty("m3.arrays.native.path");
        return ArrayNative.isLoaded() || (configured != null && !configured.isBlank());
    }

    private static void check(LongCoordinateIndex index, long[] queries) {
        long[] saved = queries.clone();
        int[] lower = index.lowerBounds(queries);
        int[] upper = index.upperBounds(queries);
        assertEquals(queries.length, lower.length);
        for (int at = 0; at < queries.length; at++) {
            assertEquals(index.lowerBound(queries[at]), lower[at]);
            assertEquals(index.upperBound(queries[at]), upper[at]);
        }
        assertArrayEquals(saved, queries);
        if (nativeLoaded()) {
            assertArrayEquals(lower, index.lowerBoundsRequiredNative(queries));
            assertArrayEquals(upper, index.upperBoundsRequiredNative(queries));
        }
    }

    @Test
    void ascendingQueriesReuseOrdinalsAcrossOneHundredTwentyEightItemWindows() {
        long[] keys = new long[8193];
        for (int i = 0; i < keys.length; i++) keys[i] = (long) i * 7 - 28_000;
        LongCoordinateIndex index = new LongCoordinateIndex(keys, new long[keys.length]);
        long[] queries = new long[16391];
        queries[0] = Long.MIN_VALUE;
        for (int i = 1; i < queries.length - 1; i++) queries[i] = -32_000L + i * 4L;
        queries[queries.length - 1] = Long.MAX_VALUE;
        check(index, queries);
    }

    @Test
    void descendingAndSawtoothQueriesResetHintsWithoutChangingResults() {
        long[] keys = new long[4097];
        for (int i = 0; i < keys.length; i++) keys[i] = i * 3L - 8_000;
        LongCoordinateIndex index = new LongCoordinateIndex(keys, new long[keys.length]);
        long[] queries = new long[12_803];
        for (int i = 0; i < queries.length; i++) {
            queries[i] = switch ((i / 263) % 4) {
                case 0 -> 20_000L - (i % 263) * 41L;
                case 1 -> -30_000L + (i % 263) * 17L;
                case 2 -> Long.MIN_VALUE;
                default -> Long.MAX_VALUE;
            };
        }
        check(index, queries);
    }

    @Test
    void equalQueriesDoNotMoveHintAndEmptyLaneRemainsAddressable() {
        LongCoordinateIndex empty = new LongCoordinateIndex(new long[0], new long[0]);
        long[] q = new long[10_001];
        Arrays.fill(q, Long.MIN_VALUE);
        check(empty, q);
        LongCoordinateIndex singleton = new LongCoordinateIndex(
                new long[] {Long.MAX_VALUE}, new long[] {0});
        check(singleton, q);
        Arrays.fill(q, Long.MAX_VALUE);
        check(singleton, q);
        assertArrayEquals(new int[0], empty.lowerBounds(new long[0]));
        assertArrayEquals(new int[0], singleton.upperBounds(new long[0]));
        assertThrows(NullPointerException.class, () -> singleton.lowerBounds(null));
    }

    @Test
    void extremeSignedLongQueriesAndRepeatedBoundaries() {
        LongCoordinateIndex index = new LongCoordinateIndex(
                new long[] {Long.MIN_VALUE, Long.MIN_VALUE + 1, -1, 0, 1,
                        Long.MAX_VALUE - 1, Long.MAX_VALUE}, new long[7]);
        check(index, new long[] {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE + 1,
                -2, -1, 0, 0, 1, 2, Long.MAX_VALUE - 1,
                Long.MAX_VALUE, Long.MAX_VALUE});
        check(index, new long[] {Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE,
                Long.MIN_VALUE + 1, -1, -1, 0, Long.MIN_VALUE, Long.MAX_VALUE});
    }

    @Test
    void seededIndependentScalarOracleCoversEveryQueryOrderingClass() {
        Random random = new Random(0x4d33474c50484f50L);
        for (int trial = 0; trial < 240; trial++) {
            int length = random.nextInt(300);
            long[] keys = new long[length];
            for (int i = 0; i < length; i++) keys[i] = i * 17L - 3_000L;
            LongCoordinateIndex index = new LongCoordinateIndex(keys, new long[length]);
            long[] queries = new long[random.nextInt(513)];
            for (int i = 0; i < queries.length; i++) {
                queries[i] = switch (trial % 6) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> -5_000L + i * 3L;
                    case 3 -> 5_000L - i * 3L;
                    case 4 -> (i % 2 == 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
                    default -> random.nextInt(10_000) - 5_000L;
                };
            }
            check(index, queries);
        }
    }
}
