// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Public snapshot adapter contract backed by original M3 sorted long[] owners. */
final class M3MergedSelectTest {
    @Test
    void signedSortedOccurrenceSelection() {
        M3MergedSelectContract.main(new String[0]);
        M3LongRankIndex left = M3LongRankIndex.copyOf(
                new long[] {Long.MAX_VALUE, 0, Long.MIN_VALUE, 0});
        M3LongRankIndex right = M3LongRankIndex.copyOf(
                new long[] {-1, 0, Long.MIN_VALUE});
        long[] expected = {Long.MIN_VALUE, Long.MIN_VALUE, -1, 0, 0, 0, Long.MAX_VALUE};
        for (int k = 0; k < expected.length; k++) {
            assertEquals(expected[k], left.selectMerged(right, k));
        }
        long[] ranks = {6, 0, 3, 3, 2, 5};
        assertArrayEquals(new long[] {Long.MAX_VALUE, Long.MIN_VALUE, 0, 0, -1, 0},
                left.selectMergedBatch(right, ranks));
        long[] output = {97, 97, 97, 97, 97, 97, 97};
        left.selectMergedInto(right, ranks, 1, 3, output, 2);
        assertArrayEquals(new long[] {97, 97, Long.MIN_VALUE, 0, 0, 97, 97}, output);
        assertThrows(IndexOutOfBoundsException.class,
                () -> left.selectMerged(right, (long) left.size() + right.size()));
        assertThrows(IndexOutOfBoundsException.class,
                () -> left.selectMerged(right, -1));
        long[] bad = {0, -1};
        assertThrows(IndexOutOfBoundsException.class,
                () -> left.selectMergedInto(right, bad, 0, 2, output, 0));
        assertArrayEquals(new long[] {97, 97, Long.MIN_VALUE, 0, 0, 97, 97}, output);
    }
}
