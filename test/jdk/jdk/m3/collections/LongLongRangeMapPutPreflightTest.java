// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Regression for failure-atomic interval overwrite with exact capacity preflight. */
final class LongLongRangeMapPutPreflightTest {
    private static String rows(LongLongRangeMap map) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < map.rangeCount(); i++) {
            result.append(map.startAt(i)).append(':').append(map.endAt(i))
                    .append(':').append(map.valueAt(i)).append(';');
        }
        return result.toString();
    }

    @Test
    void eachBackingCopyFailureOccursBeforeAnyOverwrittenIntervalIsChanged() {
        for (int failureAt = 1; failureAt <= 3; failureAt++) {
            LongLongRangeMap map = new LongLongRangeMap(1);
            map.put(0, 10, 41);
            AtomicInteger copies = new AtomicInteger();
            int failingLane = failureAt;
            long originalBytes = map.payloadBytes();
            OutOfMemoryError failure = assertThrows(OutOfMemoryError.class,
                    () -> map.put(3, 7, 53, (source, capacity) -> {
                        if (copies.incrementAndGet() == failingLane) {
                            throw new OutOfMemoryError("lane " + failingLane);
                        }
                        return Arrays.copyOf(source, capacity);
                    }));
            assertEquals("lane " + failureAt, failure.getMessage());
            assertEquals(failureAt, copies.get());
            assertEquals("0:10:41;", rows(map));
            assertEquals(1, map.capacity());
            assertEquals(originalBytes, map.payloadBytes());
            map.put(3, 7, 53);
            assertEquals("0:3:41;3:7:53;7:10:41;", rows(map));
        }
    }

    @Test
    void exactPreflightAvoidsSpuriousGrowthDuringOverwrite() {
        LongLongRangeMap one = new LongLongRangeMap(1);
        one.put(0, 10, 7);
        one.put(0, 10, 9, (source, capacity) -> {
            throw new AssertionError("full overwrite must reuse current capacity");
        });
        assertEquals(1, one.capacity());
        assertEquals("0:10:9;", rows(one));

        LongLongRangeMap many = new LongLongRangeMap(3);
        many.put(0, 10, 11);
        many.put(20, 30, 12);
        many.put(40, 50, 13);
        many.put(5, 45, 14, (source, capacity) -> {
            throw new AssertionError("two surviving fragments fit three rows");
        });
        assertEquals(3, many.capacity());
        assertEquals("0:5:11;5:45:14;45:50:13;", rows(many));
    }

    @Test
    void disjointInsertionFailureDoesNotChangeExistingRows() {
        LongLongRangeMap map = new LongLongRangeMap(1);
        map.put(0, 10, 7);
        assertThrows(OutOfMemoryError.class,
                () -> map.put(20, 30, 8, (source, capacity) -> {
                    throw new OutOfMemoryError("disjoint insert");
                }));
        assertEquals("0:10:7;", rows(map));
        assertFalse(map.containsPoint(20));
        assertTrue(map.containsPoint(5));
    }

    @Test
    void malformedCopiedLaneRefusesBeforeMutation() {
        LongLongRangeMap map = new LongLongRangeMap(1);
        map.put(-10, 10, 21);
        assertThrows(IllegalStateException.class,
                () -> map.put(-3, 5, 22, (source, capacity) -> new long[capacity - 1]));
        assertThrows(NullPointerException.class,
                () -> map.put(-3, 5, 22, (source, capacity) -> null));
        assertEquals("-10:10:21;", rows(map));
        long[] entry = new long[3];
        assertTrue(map.copyEntryContaining(0, entry, 0));
        assertArrayEquals(new long[] {-10, 10, 21}, entry);
    }

    @Test
    void deterministicMutationsMatchPointwiseIntervalOracle() {
        LongLongRangeMap map = new LongLongRangeMap(1);
        boolean[] present = new boolean[257];
        long[] values = new long[257];
        Random random = new Random(0x1357BEEFL);
        for (int step = 0; step < 4_096; step++) {
            int first = random.nextInt(257);
            int end = Math.min(257, first + 1 + random.nextInt(25));
            if (end == first) continue;
            if (step % 7 == 0) {
                map.remove(first, end);
                for (int i = first; i < end; i++) present[i] = false;
            } else {
                long newValue = step - 500L;
                map.put(first, end, newValue);
                for (int i = first; i < end; i++) {
                    present[i] = true;
                    values[i] = newValue;
                }
            }
            for (int i = 0; i < 257; i++) {
                assertEquals(present[i] ? values[i] : -777L,
                        map.getOrDefault(i, -777L), "step " + step + ", point " + i);
            }
        }
    }

    @Test
    void extremeSignedLongEndpointsStillFollowHalfOpenBounds() {
        LongLongRangeMap map = new LongLongRangeMap(1);
        map.put(Long.MIN_VALUE, Long.MIN_VALUE + 4, 7);
        map.put(Long.MAX_VALUE - 4, Long.MAX_VALUE, 9);
        map.put(Long.MIN_VALUE + 1, Long.MIN_VALUE + 3, 11);
        map.put(Long.MAX_VALUE - 3, Long.MAX_VALUE - 2, 13);
        assertEquals(7, map.getOrDefault(Long.MIN_VALUE, -1));
        assertEquals(11, map.getOrDefault(Long.MIN_VALUE + 2, -1));
        assertEquals(13, map.getOrDefault(Long.MAX_VALUE - 3, -1));
        assertEquals(9, map.getOrDefault(Long.MAX_VALUE - 1, -1));
        assertFalse(map.containsPoint(Long.MAX_VALUE));
    }
}
