// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

final class M3LongLongRangeMapQueryTest {
    @Test
    void containingEntryUsesCallerBufferAndPreservesMissBuffer() {
        M3LongLongRangeMap ranges = sample();
        long[] output = {-9L, -8L, -7L, -6L, -5L};
        assertTrue(ranges.copyEntryContaining(25L, output, 1));
        assertArrayEquals(new long[] {-9L, 20L, 40L, 2L, -5L}, output);
        long[] miss = {11L, 12L, 13L, 14L};
        assertFalse(ranges.copyEntryContaining(10L, miss, 0));
        assertArrayEquals(new long[] {11L, 12L, 13L, 14L}, miss);
    }

    @Test
    void copiedEntryReflectsCoalescedCanonicalRange() {
        M3LongLongRangeMap ranges = sample();
        long[] output = new long[3];
        assertTrue(ranges.copyEntryContaining(35L, output, 0));
        assertArrayEquals(new long[] {20L, 40L, 2L}, output);
        assertEquals(2L, ranges.getOrDefault(35L, -1L));
    }

    @Test
    void spanMatchesMinimalEnclosingRangeAndEmptyThrows() {
        M3LongLongRangeMap ranges = sample();
        assertEquals(0L, ranges.spanStart());
        assertEquals(40L, ranges.spanEnd());
        M3LongLongRangeMap empty = new M3LongLongRangeMap();
        assertThrows(NoSuchElementException.class, empty::spanStart);
        assertThrows(NoSuchElementException.class, empty::spanEnd);
    }

    private static M3LongLongRangeMap sample() {
        M3LongLongRangeMap ranges = new M3LongLongRangeMap();
        ranges.put(0L, 10L, 1L);
        ranges.put(20L, 30L, 2L);
        ranges.put(30L, 40L, 2L);
        assertEquals(2, ranges.rangeCount());
        return ranges;
    }
}
