// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Original long-index and JNI batch atom parity, LeetCode #658. */
final class M3LongRankClosestWindowTest {
    @Test
    void primitiveDistanceOracleAndAtomicWindows() {
        M3LongRankClosestWindowContract.run(false);
    }

    @Test
    void immutableProductIndexUsesOriginalOwner() {
        long[] source = {Long.MAX_VALUE, 9L, Long.MIN_VALUE, -2L, 0L, Long.MIN_VALUE};
        M3LongRankIndex index = M3LongRankIndex.copyOf(source);
        source[0] = 42L; // snapshots own the sorted array; no caller aliasing.
        long[] requests = {0L, Long.MAX_VALUE, Long.MIN_VALUE};
        assertArrayEquals(new int[] {2, 4, 0}, index.closestWindowStarts(requests, 2));
        int[] output = new int[8];
        Arrays.fill(output, 12345);
        index.closestWindowStartsInto(requests, 0, requests.length, 2, output, 3);
        assertArrayEquals(new int[] {12345, 12345, 12345, 2, 4, 0, 12345, 12345}, output);
        assertEquals(6, index.size());
        assertEquals(Long.MIN_VALUE, index.select(0));
        assertThrows(IllegalArgumentException.class, () ->
                index.closestWindowStartsInto(requests, 0, requests.length, 7, output, 3));
        assertArrayEquals(new int[] {12345, 12345, 12345, 2, 4, 0, 12345, 12345}, output);
    }
}
