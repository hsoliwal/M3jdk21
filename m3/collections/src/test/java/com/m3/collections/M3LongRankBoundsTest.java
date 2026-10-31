// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** JUnit admission for the original M3 rank index and its fused JNI batch owner. */
final class M3LongRankBoundsTest {
    @Test
    void signedLeetCode34And35Differential() {
        M3LongRankBoundsContract.contracts();
    }

    @Test
    void originalIndexExposesDetachedPairLaneAndCallerReuse() {
        M3LongRankIndex snapshot = M3LongRankIndex.copyOf(
                new long[]{Long.MAX_VALUE, 2, 2, 2, -4, Long.MIN_VALUE});
        long[] queries = {Long.MIN_VALUE, -4, -3, 2, 3, Long.MAX_VALUE};
        int[] expected = {0, 1, 1, 2, 2, 2, 2, 5, 5, 5, 5, 6};
        assertArrayEquals(expected, snapshot.lowerUpperBounds(queries));
        int[] reusable = new int[expected.length];
        snapshot.fillLowerUpperBounds(queries, reusable);
        assertArrayEquals(expected, reusable);
        assertThrows(IllegalArgumentException.class, () ->
                snapshot.fillLowerUpperBounds(queries, new int[1]));
        assertThrows(NullPointerException.class, () -> snapshot.lowerUpperBounds(null));
        assertThrows(NullPointerException.class, () -> snapshot.fillLowerUpperBounds(queries, null));
        assertArrayEquals(new long[]{Long.MIN_VALUE, -4, 2, 2, 2, Long.MAX_VALUE},
                snapshot.toArray());
    }
}
