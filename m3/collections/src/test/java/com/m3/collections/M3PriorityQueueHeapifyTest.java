/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent JDK and primitive-array differential witnesses for Floyd heap construction. */
final class M3PriorityQueueHeapifyTest {
    @Test void bulkBuildMatchesIncrementalAndJdkAcrossSeededArrays() {
        Random random = new Random(0x215703L);
        for (int round = 0; round < 128; round++) {
            int length = round < 64 ? round : round * 11;
            int[] ints = new int[length];
            long[] longs = new long[length];
            PriorityQueue<Integer> jdkInts = new PriorityQueue<>();
            PriorityQueue<Long> jdkLongs = new PriorityQueue<>();
            M3IntPriorityQueue incrementalInts = new M3IntPriorityQueue();
            M3LongPriorityQueue incrementalLongs = new M3LongPriorityQueue();
            for (int i = 0; i < length; i++) {
                ints[i] = round % 7 == 0 ? 0 : i % 31 == 0 ? Integer.MIN_VALUE
                        : i % 37 == 0 ? Integer.MAX_VALUE : random.nextInt(201) - 100;
                longs[i] = round % 7 == 0 ? 0 : i % 31 == 0 ? Long.MIN_VALUE
                        : i % 37 == 0 ? Long.MAX_VALUE : random.nextLong() % 1009;
                jdkInts.offer(ints[i]);
                jdkLongs.offer(longs[i]);
                incrementalInts.offer(ints[i]);
                incrementalLongs.offer(longs[i]);
            }
            int[] immutableInts = ints.clone();
            long[] immutableLongs = longs.clone();
            M3IntPriorityQueue bulkInts = new M3IntPriorityQueue(ints);
            M3LongPriorityQueue bulkLongs = new M3LongPriorityQueue(longs);
            assertArrayEquals(immutableInts, ints);
            assertArrayEquals(immutableLongs, longs);
            assertEquals(length, bulkInts.size());
            assertEquals(length, bulkLongs.size());
            assertEquals(length, bulkInts.capacity());
            assertEquals(length, bulkLongs.capacity());
            assertArrayEquals(Arrays.stream(immutableInts).sorted().toArray(), bulkInts.toSortedArray());
            assertArrayEquals(Arrays.stream(immutableLongs).sorted().toArray(), bulkLongs.toSortedArray());
            assertArrayEquals(incrementalInts.toSortedArray(), bulkInts.toSortedArray());
            assertArrayEquals(incrementalLongs.toSortedArray(), bulkLongs.toSortedArray());
            for (int i = 0; i < length; i++) {
                int expectedInt = jdkInts.remove();
                long expectedLong = jdkLongs.remove();
                assertEquals(expectedInt, bulkInts.removeFirstInt());
                assertEquals(expectedLong, bulkLongs.removeFirstLong());
                assertEquals(expectedInt, incrementalInts.removeFirstInt());
                assertEquals(expectedLong, incrementalLongs.removeFirstLong());
                assertEquals(jdkInts.isEmpty(), bulkInts.isEmpty());
                assertEquals(jdkLongs.isEmpty(), bulkLongs.isEmpty());
                if (!jdkInts.isEmpty()) {
                    assertEquals(jdkInts.peek().intValue(), bulkInts.firstInt());
                }
                if (!jdkLongs.isEmpty()) {
                    assertEquals(jdkLongs.peek().longValue(), bulkLongs.firstLong());
                }
            }
            assertTrue(bulkInts.isEmpty());
            assertTrue(bulkLongs.isEmpty());
            assertThrows(NoSuchElementException.class, bulkInts::removeFirstInt);
            assertThrows(NoSuchElementException.class, bulkLongs::removeFirstLong);
        }
    }

    @Test void inputIsDetachedAndMutationRulesRemainIntact() {
        int[] ints = {9, -1, 3, -1, 0, Integer.MAX_VALUE};
        long[] longs = {9, -1, 3, -1, 0, Long.MIN_VALUE};
        M3IntPriorityQueue qi = new M3IntPriorityQueue(ints);
        M3LongPriorityQueue ql = new M3LongPriorityQueue(longs);
        ints[0] = -1000;
        longs[0] = -1000;
        assertEquals(-1, qi.firstInt());
        assertEquals(Long.MIN_VALUE, ql.firstLong());
        var ii = qi.iterator();
        var il = ql.iterator();
        assertTrue(qi.remove(3));
        assertFalse(qi.remove(777));
        assertTrue(ql.remove(3));
        assertFalse(ql.remove(777));
        assertThrows(ConcurrentModificationException.class, ii::hasNext);
        assertThrows(ConcurrentModificationException.class, il::hasNext);
        qi.offer(-4000);
        ql.offer(-4000);
        assertEquals(-4000, qi.removeFirstInt());
        assertEquals(Long.MIN_VALUE, ql.removeFirstLong());
        assertArrayEquals(new int[] {-1, -1, 0, 9, Integer.MAX_VALUE}, qi.toSortedArray());
        assertArrayEquals(new long[] {-4000, -1, -1, 0, 9}, ql.toSortedArray());
    }

    @Test void nullAndEmptyConstructionAreDefined() {
        assertThrows(NullPointerException.class, () -> new M3IntPriorityQueue((int[]) null));
        assertThrows(NullPointerException.class, () -> new M3LongPriorityQueue((long[]) null));
        var qi = new M3IntPriorityQueue(new int[0]);
        var ql = new M3LongPriorityQueue(new long[0]);
        assertEquals(0, qi.capacity());
        assertEquals(0, ql.capacity());
        qi.offer(1);
        ql.offer(1);
        assertEquals(11, qi.capacity());
        assertEquals(11, ql.capacity());
    }

    @Test void sharedGrowthRetainsExactOriginalCapacityFormula() {
        assertEquals(11, M3PriorityQueueGrowth.nextCapacity(0, 1));
        assertEquals(24, M3PriorityQueueGrowth.nextCapacity(11, 12));
        assertEquals(128, M3PriorityQueueGrowth.nextCapacity(63, 64));
        assertEquals(96, M3PriorityQueueGrowth.nextCapacity(64, 65));
        assertEquals(190, M3PriorityQueueGrowth.nextCapacity(127, 128));
        assertEquals(Integer.MAX_VALUE,
                M3PriorityQueueGrowth.nextCapacity(Integer.MAX_VALUE - 8, Integer.MAX_VALUE));
    }
}
