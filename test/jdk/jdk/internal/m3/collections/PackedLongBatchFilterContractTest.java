// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import jdk.m3.collections.LongSortedSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.packed.PackedLongBatchSortedSet;
import jdk.internal.m3.collections.packed.PackedLongSortedSet;

class PackedLongBatchFilterContractTest {
    @Test
    void mixedLanesAreFlushedBeforeExactlyOneSortedCallbackPerValue() {
        PackedLongBatchSortedSet set = new PackedLongBatchSortedSet(0, 3);
        set.addAll(new long[] {6, 2, 4, 5, 1});
        assertEquals(2, set.pendingSize());
        List<Long> seen = new ArrayList<>();
        LongSortedSet contract = set;
        assertEquals(2, contract.filterInPlace(value -> {
            assertEquals(0, set.pendingSize());
            seen.add(value);
            return value % 2 == 0;
        }));
        assertEquals(List.of(1L, 2L, 4L, 5L, 6L), seen);
        assertArrayEquals(new long[] {2, 4, 6}, set.toArray());
        assertEquals(2, set.first());
        assertEquals(6, set.last());
        assertEquals(4, set.floor(5, -1));
        assertEquals(6, set.ceiling(5, -1));
        assertEquals(2, set.lower(4, -1));
        assertEquals(6, set.higher(4, -1));
    }

    @Test
    void nullPredicateFailsBeforePendingLaneChanges() {
        PackedLongBatchSortedSet set = new PackedLongBatchSortedSet(0, 4);
        set.addAll(new long[] {3, 1});
        NullPointerException failure = assertThrows(NullPointerException.class,
                () -> set.filterInPlace(null));
        assertEquals("predicate", failure.getMessage());
        assertEquals(2, set.pendingSize());
        assertTrue(set.contains(3));
        assertTrue(set.contains(1));
        assertArrayEquals(new long[] {1, 3}, set.toArray());
    }

    @Test
    void emptyKeepAllRejectAllAndRepeatedCallsHaveExactRemovalCounts() {
        PackedLongBatchSortedSet set = new PackedLongBatchSortedSet(0, 8);
        assertEquals(0, set.filterInPlace(value -> { throw new AssertionError("empty"); }));
        set.addAll(new long[] {Long.MAX_VALUE, Long.MIN_VALUE, 0, Long.MAX_VALUE});
        assertEquals(0, set.filterInPlace(value -> true));
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, set.toArray());
        assertEquals(3, set.filterInPlace(value -> false));
        assertEquals(0, set.filterInPlace(value -> false));
        assertTrue(set.isEmpty());
        assertEquals(0, set.pendingSize());
        assertThrows(java.util.NoSuchElementException.class, set::first);
        assertTrue(set.add(7));
        assertFalse(set.add(7));
        assertTrue(set.remove(7));
    }

    @Test
    void everyThrowPositionPreservesThrowingValueAndUntestedSuffix() {
        for (int stop = 0; stop < 8; stop++) {
            PackedLongBatchSortedSet set = new PackedLongBatchSortedSet(0, 5);
            set.addAll(new long[] {7, 3, 1, 5, 0, 6, 2, 4});
            final int failurePosition = stop;
            RuntimeException failure = new IllegalStateException("predicate failure");
            List<Long> seen = new ArrayList<>();
            assertSame(failure, assertThrows(RuntimeException.class,
                    () -> set.filterInPlace(value -> {
                        seen.add(value);
                        if (value == failurePosition) {
                            throw failure;
                        }
                        return value % 2 == 0;
                    })));
            List<Long> expected = new ArrayList<>();
            for (long value = 0; value < 8; value++) {
                if (value >= stop || value % 2 == 0) {
                    expected.add(value);
                }
            }
            assertArrayEquals(expected.stream().mapToLong(Long::longValue).toArray(), set.toArray());
            assertEquals(stop + 1, seen.size());
            assertEquals(0, set.pendingSize());
            assertEquals(expected.size(), set.size());
            for (long value : expected) {
                assertTrue(set.contains(value));
            }
            assertTrue(set.add(9));
            assertEquals(0, set.filterInPlace(value -> true));
            assertEquals(9, set.last());
        }
    }

    @Test
    void errorAlsoPreservesUnreadSuffixAndExceptionIdentity() {
        PackedLongBatchSortedSet set = new PackedLongBatchSortedSet(0, 8);
        set.addAll(new long[] {4, 3, 2, 1});
        AssertionError failure = new AssertionError("callback error");
        assertSame(failure, assertThrows(AssertionError.class, () -> set.filterInPlace(value -> {
            if (value == 3) {
                throw failure;
            }
            return false;
        })));
        assertArrayEquals(new long[] {3, 4}, set.toArray());
    }

    @Test
    void insertionFilterIsNotReevaluatedAndRetainedStateRemainsUsable() {
        int[] insertionCalls = {0};
        PackedLongBatchSortedSet set = new PackedLongBatchSortedSet(2, 4, value -> {
            insertionCalls[0]++;
            return value >= 0;
        });
        set.addAll(new long[] {-1, 3, 1, 2});
        assertEquals(4, insertionCalls[0]);
        assertEquals(1, set.filterInPlace(value -> value != 2));
        assertEquals(4, insertionCalls[0]);
        assertTrue(set.add(2));
        assertFalse(set.add(-2));
        assertArrayEquals(new long[] {1, 2, 3}, set.toArray());
        set.clear();
        assertEquals(0, set.filterInPlace(value -> true));
        set.trimToSize();
        assertTrue(set.add(4));
        assertEquals(0, set.filterInPlace(value -> true));
        assertArrayEquals(new long[] {4}, set.toArray());
    }

    @Test
    void randomizedTracesMatchTreeSetAndEstablishedSortedSetOwner() {
        Random random = new Random(0x5E7F17E);
        for (int batchSize : new int[] {1, 2, 3, 16, 65}) {
            PackedLongBatchSortedSet actual = new PackedLongBatchSortedSet(0, batchSize);
            PackedLongSortedSet established = new PackedLongSortedSet();
            TreeSet<Long> expected = new TreeSet<>();
            for (int step = 0; step < 2000; step++) {
                long value = random.nextInt(201) - 100;
                switch (random.nextInt(6)) {
                    case 0, 1, 2 -> {
                        boolean added = expected.add(value);
                        assertEquals(added, actual.add(value));
                        assertEquals(added, established.add(value));
                    }
                    case 3 -> {
                        boolean removed = expected.remove(value);
                        assertEquals(removed, actual.remove(value));
                        assertEquals(removed, established.remove(value));
                    }
                    case 4 -> {
                        int oldSize = expected.size();
                        expected.removeIf(item -> item < value);
                        assertEquals(oldSize - expected.size(), actual.filterInPlace(item -> item >= value));
                        assertEquals(oldSize - expected.size(), established.filterInPlace(item -> item >= value));
                        assertEquals(0, actual.pendingSize());
                    }
                    default -> {
                        assertEquals(expected.contains(value), actual.contains(value));
                        assertEquals(expected.contains(value), established.contains(value));
                    }
                }
                // Observe periodically so random traces also exercise mixed lanes.
                if (step % 23 == 0) {
                    assertArrayEquals(expected.stream().mapToLong(Long::longValue).toArray(), actual.toArray());
                    assertArrayEquals(established.toArray(), actual.toArray());
                }
            }
            assertArrayEquals(expected.stream().mapToLong(Long::longValue).toArray(), actual.toArray());
        }
    }
}
