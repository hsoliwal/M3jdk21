/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.range;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** API_UNION U6: the Synexia primitives LongRangeSet members on the M3 owner. */
class LongRangeSetUnionTest {

    private static LongRangeSet of(long... bounds) {
        LongRangeSet set = new LongRangeSet();
        for (int i = 0; i < bounds.length; i += 2) {
            set.add(bounds[i], bounds[i + 1]);
        }
        return set;
    }

    @Test
    void coversAndCopyRangeContaining() {
        LongRangeSet set = of(10, 20, 30, 40);
        assertTrue(set.covers(12, 18));
        assertTrue(set.covers(10, 20));
        assertFalse(set.covers(15, 25));
        assertFalse(set.covers(20, 30));
        assertTrue(set.covers(25, 25), "an empty query is covered (M3 empty-interval convention)");
        assertThrows(IllegalArgumentException.class, () -> set.covers(5, 4));
        long[] out = {-1, -1, -1, -1};
        assertTrue(set.copyRangeContaining(35, out, 1));
        assertArrayEquals(new long[] {-1, 30, 40, -1}, out);
        assertFalse(set.copyRangeContaining(20, out, 0));
        assertFalse(set.copyRangeContaining(9, out, 0));
        assertArrayEquals(new long[] {-1, 30, 40, -1}, out, "a miss leaves the buffer unchanged");
        assertThrows(IndexOutOfBoundsException.class, () -> set.copyRangeContaining(35, out, 3));
        assertThrows(NullPointerException.class, () -> set.copyRangeContaining(35, null, 0));
    }

    @Test
    void spansIndexesAndArrays() {
        LongRangeSet set = of(30, 40, 10, 20);
        assertEquals(10, set.spanStart());
        assertEquals(40, set.spanEnd());
        assertEquals(30, set.startAt(1));
        assertEquals(20, set.endAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> set.startAt(2));
        assertThrows(IndexOutOfBoundsException.class, () -> set.endAt(-1));
        assertArrayEquals(set.startsToArray(), set.startsArray());
        assertArrayEquals(set.endsToArray(), set.endsArray());
        List<Long> seen = new ArrayList<>();
        set.forEach((s, e) -> { seen.add(s); seen.add(e); });
        assertEquals(List.of(10L, 20L, 30L, 40L), seen);
        LongRangeSet empty = new LongRangeSet();
        assertThrows(NoSuchElementException.class, empty::spanStart);
        assertThrows(NoSuchElementException.class, empty::spanEnd);
    }

    @Test
    void addAllMergesAndAcceptsItself() {
        LongRangeSet set = of(10, 20);
        set.addAll(of(15, 25, 40, 50));
        assertArrayEquals(new long[] {10, 40}, set.startsArray());
        assertArrayEquals(new long[] {25, 50}, set.endsArray());
        set.addAll(set);
        assertEquals(2, set.rangeCount());
        set.verifyInvariants();
        assertThrows(NullPointerException.class, () -> set.addAll(null));
    }

    @Test
    void payloadBytesCountsBothLanes() {
        LongRangeSet set = new LongRangeSet(4);
        assertEquals(4L * Long.BYTES * 2L, set.payloadBytes());
        LongRangeSet grown = of(1, 2, 3, 4, 5, 6);
        assertEquals((long) grown.capacity() * Long.BYTES * 2L, grown.payloadBytes());
    }
}
