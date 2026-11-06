// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Fault injection for the existing three-lane interval map, not a new map implementation. */
final class M3LongLongRangeMapAtomicGrowthTest {
    @Test
    void failureOfEachArrayCopyPreservesAllThreeOriginalLanes() {
        for (int failAt = 1; failAt <= 3; failAt++) {
            M3LongLongRangeMap map = new M3LongLongRangeMap(1);
            map.put(2, 7, 41);
            int oldCapacity = map.capacity();
            long oldPayload = map.payloadBytes();
            AtomicInteger calls = new AtomicInteger();
            int failedCopy = failAt;
            M3LongLongRangeMap.BackingCopier injected = (source, capacity) -> {
                if (calls.incrementAndGet() == failedCopy) {
                    throw new OutOfMemoryError("synthetic array " + failedCopy);
                }
                return Arrays.copyOf(source, capacity);
            };
            assertThrows(OutOfMemoryError.class, () -> map.ensure(2, injected));
            assertEquals(failAt, calls.get());
            assertEquals(oldCapacity, map.capacity());
            assertEquals(oldPayload, map.payloadBytes());
            assertEquals(1, map.rangeCount());
            long[] entry = new long[3];
            assertTrue(map.copyEntryContaining(3, entry, 0));
            assertArrayEquals(new long[] {2, 7, 41}, entry);
            assertEquals(41, map.getOrDefault(3, -1));
            assertFalse(map.containsPoint(7));
            map.put(12, 18, 53);
            assertEquals(2, map.rangeCount());
            assertEquals(53, map.getOrDefault(15, -1));
        }
    }

    @Test
    void malformedNullAndShortCopiesFailBeforePublishingAnyLane() {
        M3LongLongRangeMap map = new M3LongLongRangeMap(1);
        map.put(-10, -3, 67);
        assertThrows(NullPointerException.class, () -> map.ensure(10, (source, size) -> null));
        assertThrows(IllegalStateException.class, () -> map.ensure(10,
                (source, size) -> new long[size - 1]));
        assertEquals(1, map.capacity());
        assertEquals(1, map.rangeCount());
        assertEquals(67, map.getOrDefault(-8, -1));
    }

    @Test
    void growthThatIsNotNeededNeverInvokesInjectedCopier() {
        M3LongLongRangeMap map = new M3LongLongRangeMap(8);
        map.put(1, 2, 3);
        map.ensure(8, (source, capacity) -> {
            throw new AssertionError("copy invoked for already sufficient capacity");
        });
        assertEquals(8, map.capacity());
        assertEquals(3, map.getOrDefault(1, -1));
    }

    @Test
    void deterministicOverwriteRemoveAndCoalescingMatchPointwiseOracle() {
        M3LongLongRangeMap map = new M3LongLongRangeMap(1);
        long[] values = new long[257];
        boolean[] present = new boolean[257];
        for (int operation = 0; operation < 1_024; operation++) {
            int from = (operation * 101 + 17) % 256;
            int to = Math.min(257, from + 1 + operation % 13);
            if (operation % 5 == 0) {
                map.remove(from, to);
                for (int p = from; p < to; p++) present[p] = false;
            } else {
                long value = operation - 300L;
                map.put(from, to, value);
                for (int p = from; p < to; p++) {
                    present[p] = true;
                    values[p] = value;
                }
            }
            for (int p = 0; p < present.length; p++) {
                assertEquals(present[p], map.containsPoint(p));
                assertEquals(present[p] ? values[p] : -999L,
                        map.getOrDefault(p, -999L), "operation " + operation + " point " + p);
            }
        }
    }
}
