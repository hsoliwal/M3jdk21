// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Original collection owner, direct native/Java exact-result differential. */
final class M3LongRankBatchIntersectionValuesTest {
    @Test
    void sortedSetAndMultisetNativeDifferential() {
        M3LongRankBatchIntersectionValuesContract.main(new String[0]);
    }

    @Test
    void rankSnapshotsProduceDetachedPrimitiveIntersection() {
        M3LongRankIndex left = M3LongRankIndex.copyOf(
                new long[]{Long.MAX_VALUE, 2, 2, Long.MIN_VALUE, 2, 3, 3});
        M3LongRankIndex right = M3LongRankIndex.copyOf(
                new long[]{2, 3, 2, Long.MIN_VALUE, Long.MIN_VALUE, 8});
        long[] multiset = left.intersectionValues(right, false);
        assertArrayEquals(new long[]{Long.MIN_VALUE, 2, 2, 3}, multiset);
        assertArrayEquals(new long[]{Long.MIN_VALUE, 2, 3},
                left.intersectionValues(right, true));
        assertEquals(4L, left.intersectionCount(right));
        multiset[0] = 999;
        assertArrayEquals(new long[]{
                Long.MIN_VALUE, 2, 2, 2, 3, 3, Long.MAX_VALUE}, left.toArray());
        assertArrayEquals(new long[]{
                Long.MIN_VALUE, Long.MIN_VALUE, 2, 2, 3, 8}, right.toArray());
        assertThrows(NullPointerException.class, () -> left.intersectionValues(null, false));
    }
}
