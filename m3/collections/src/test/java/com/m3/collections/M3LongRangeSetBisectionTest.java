/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent bounded boolean-lane oracle; no LeetCode/donor solution code is copied. */
final class M3LongRangeSetBisectionTest {
    private static final int WIDTH = 256;
    private static final long BASE = -128;

    @Test
    void twentyThousandRandomEditsAgainstBooleanLane() {
        M3LongRangeSet actual = new M3LongRangeSet();
        boolean[] expected = new boolean[WIDTH];
        Random random = new Random(0x4D3352414E4745L);
        for (int operation = 0; operation < 20_000; operation++) {
            int left = random.nextInt(WIDTH + 1);
            int right = random.nextInt(WIDTH + 1);
            int from = Math.min(left, right);
            int to = Math.max(left, right);
            if (random.nextBoolean()) {
                actual.add(BASE + from, BASE + to);
                Arrays.fill(expected, from, to, true);
            } else {
                actual.remove(BASE + from, BASE + to);
                Arrays.fill(expected, from, to, false);
            }
            if (operation % 17 == 0) assertMatches(expected, actual);
        }
        assertMatches(expected, actual);
    }

    @Test
    void mergedOuterEndpointsAndSignedExtremes() {
        M3LongRangeSet actual = new M3LongRangeSet();
        actual.add(Long.MIN_VALUE, Long.MIN_VALUE + 1);
        actual.add(Long.MIN_VALUE + 2, Long.MIN_VALUE + 3);
        actual.add(Long.MIN_VALUE + 1, Long.MIN_VALUE + 2);
        assertArrayEquals(new long[] {Long.MIN_VALUE}, actual.startsToArray());
        assertArrayEquals(new long[] {Long.MIN_VALUE + 3}, actual.endsToArray());

        actual.add(Long.MAX_VALUE - 1, Long.MAX_VALUE);
        actual.add(Long.MIN_VALUE + 3, Long.MAX_VALUE - 1);
        assertArrayEquals(new long[] {Long.MIN_VALUE}, actual.startsToArray());
        assertArrayEquals(new long[] {Long.MAX_VALUE}, actual.endsToArray());
        assertEquals(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE),
                actual.cardinality());

        actual.remove(-1, 1);
        assertArrayEquals(new long[] {Long.MIN_VALUE, 1}, actual.startsToArray());
        assertArrayEquals(new long[] {-1, Long.MAX_VALUE}, actual.endsToArray());
        assertFalse(actual.contains(0));
        assertTrue(actual.contains(Long.MIN_VALUE));
        assertTrue(actual.contains(Long.MAX_VALUE - 1));

        actual.remove(Long.MIN_VALUE, Long.MAX_VALUE);
        assertTrue(actual.isEmpty());
        actual.verifyInvariants();
    }

    @Test
    void unchangedRangesKeepCallbackEpochAndRejectInvalidOrder() {
        M3LongRangeSet actual = new M3LongRangeSet();
        actual.add(2, 4);
        actual.add(10, 12);
        int originalCapacity = actual.capacity();
        actual.forEachRange((from, to) -> actual.add(from, to));
        actual.remove(4, 10); // touches neither half-open interval
        actual.add(11, 11);    // empty interval, also a no-op
        assertEquals(originalCapacity, actual.capacity());
        assertArrayEquals(new long[] {2, 10}, actual.startsToArray());
        assertArrayEquals(new long[] {4, 12}, actual.endsToArray());
        assertThrows(IllegalArgumentException.class, () -> actual.add(12, 4));
        assertThrows(IllegalArgumentException.class, () -> actual.remove(12, 4));
        actual.verifyInvariants();
    }

    private static void assertMatches(boolean[] expected, M3LongRangeSet actual) {
        long[] starts = new long[WIDTH], ends = new long[WIDTH];
        int count = 0, total = 0;
        boolean inside = false;
        for (int i = 0; i < expected.length; i++) {
            if (expected[i]) total++;
            if (expected[i] && !inside) {
                starts[count] = BASE + i;
                inside = true;
            } else if (!expected[i] && inside) {
                ends[count++] = BASE + i;
                inside = false;
            }
            assertEquals(expected[i], actual.contains(BASE + i),
                    "point " + i);
        }
        if (inside) ends[count++] = BASE + expected.length;
        assertEquals(count, actual.rangeCount());
        assertArrayEquals(Arrays.copyOf(starts, count), actual.startsToArray());
        assertArrayEquals(Arrays.copyOf(ends, count), actual.endsToArray());
        assertEquals(BigInteger.valueOf(total), actual.cardinality());
        actual.verifyInvariants();
    }
}
