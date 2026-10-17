// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Maven/Surefire entry point for the source-sealed rank/select differential oracle. */
class M3LongRankIndexTest {
    @Test
    void randomizedBoundsAndOptionalBatchParity() {
        M3LongRankIndexSelfTest.main(new String[0]);
    }

    @Test
    void freezesLiveFilteredInputWithoutMutatingSource() {
        M3LongArrayList source = M3LongArrayList.copyOf(new long[]{5, 2, 2, 3, 4});
        M3LongFilteredView view = new M3LongFilteredView(source, value -> value % 2 == 0);
        M3LongRankIndex snapshot = M3LongRankIndex.snapshot(view);
        assertArrayEquals(new long[]{2, 2, 4}, snapshot.toArray());
        assertEquals(2, snapshot.count(2));
        assertFalse(snapshot.isDistinct());
        source.add(6);
        assertArrayEquals(new long[]{2, 2, 4}, snapshot.toArray());
        assertArrayEquals(new long[]{2, 2, 4, 6}, view.toArray());
        assertEquals(3, M3LongRankIndex.snapshotDistinct(view).size());
    }

    @Test
    void rejectsInvalidRangesAndDoesNotExposeRetainedArray() {
        M3LongRankIndex snapshot = M3LongRankIndex.copyOf(
                new long[]{Long.MAX_VALUE, 0, Long.MIN_VALUE, 0});
        assertThrows(IllegalArgumentException.class, () -> snapshot.rangeCount(5, 4));
        assertThrows(IllegalArgumentException.class, () -> snapshot.rangeCountInclusive(5, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.select(4));
        assertThrows(IllegalStateException.class, () -> snapshot.denseRankDescending(3));
        long[] copy = snapshot.toArray();
        copy[0] = 100;
        assertEquals(Long.MIN_VALUE, snapshot.select(0));
        assertArrayEquals(snapshot.toArray(), snapshot.longStream().toArray());
        assertTrue(snapshot.contains(Long.MAX_VALUE));
        assertEquals(2, snapshot.count(0));
    }
}
