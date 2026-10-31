// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Original M3JDK21 module contract for caller-owned signed-long range output. */
final class M3LongRankWindowTest {
    @Test
    void publicSnapshotWritesOnlyTheAdmittedPhysicalResultWindow() {
        M3LongRankIndex index = M3LongRankIndex.copyOf(
                new long[] {Long.MAX_VALUE, 0, Long.MIN_VALUE, 0});
        long[] queries = {99, Long.MIN_VALUE, 0, Long.MAX_VALUE, -1};
        int[] expected = index.equalRanges(Arrays.copyOfRange(queries, 1, 5));
        int[] output = new int[13];
        Arrays.fill(output, -777);
        index.equalRangesInto(queries, 1, 4, output, 2);
        assertArrayEquals(new int[] {0, 1, 1, 3, 3, 4, 1, 1}, expected);
        assertArrayEquals(expected, Arrays.copyOfRange(output, 2, 10));
        assertEquals(-777, output[0]);
        assertEquals(-777, output[1]);
        for (int i = 10; i < output.length; i++) assertEquals(-777, output[i]);
        assertThrows(IndexOutOfBoundsException.class,
                () -> index.equalRangesInto(queries, 0, 4, output, 6));
        assertThrows(IndexOutOfBoundsException.class,
                () -> index.equalRangesInto(queries, -1, 4, output, 0));
        assertThrows(NullPointerException.class,
                () -> index.equalRangesInto(null, 0, 1, output, 0));
        for (int i = 10; i < output.length; i++) assertEquals(-777, output[i]);
    }

    @Test
    void originalJavaOracleRetainsLeetCodeBoundaryAndStaleAbiCases() {
        M3LongRankWindowContract.main(new String[0]);
    }
}
