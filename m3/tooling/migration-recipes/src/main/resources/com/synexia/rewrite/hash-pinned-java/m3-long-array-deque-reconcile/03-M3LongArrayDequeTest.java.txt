/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Finite primitive-deque contract comparisons for the union owner; this is not JDK Deque
 * replacement proof. Adapted from the Synexia {@code LongDeque} lineage differential test: the
 * union keeps the JDK-shaped ring geometry, zeroing on removal and fail-fast iteration, and keeps
 * the lineage names {@code first}/{@code last}/{@code removeFirst}/{@code removeLast} as aliases.
 */
final class M3LongArrayDequeTest {
    private static long[] values(Collection<Long> values) {
        return values.stream().mapToLong(Long::longValue).toArray();
    }

    private static long value(Random random, int step) {
        return switch (step % 13) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> random.nextInt(61) - 30L;
        };
    }

    @Test
    void primitiveDequeMatchesJdkAcrossWrapGrowthReuseAndStreams() {
        var actual = new M3LongArrayDeque(0);
        var expected = new ArrayDeque<Long>();
        Random random = new Random(0x4d334445515545L);
        for (int step = 0; step < 6_000; step++) {
            long v = value(random, step);
            switch (random.nextInt(9)) {
                case 0 -> { actual.addFirst(v); expected.addFirst(v); }
                case 1 -> { actual.addLast(v); expected.addLast(v); }
                case 2 -> {
                    if (!expected.isEmpty()) {
                        assertEquals(expected.removeFirst().longValue(), actual.removeFirst());
                    }
                }
                case 3 -> {
                    if (!expected.isEmpty()) {
                        assertEquals(expected.removeLast().longValue(), actual.removeLast());
                    }
                }
                case 4 -> assertEquals(expected.offer(v), actual.offer(v));
                case 5 -> {
                    int before = expected.size();
                    expected.removeIf(x -> (x & 3L) == 1L);
                    assertEquals(before - expected.size(), actual.filterInPlace(x -> (x & 3L) != 1L));
                }
                case 6 -> {
                    boolean accept = v < 0;
                    assertEquals(accept, actual.addLastIf(v, x -> x < 0));
                    if (accept) {
                        expected.addLast(v);
                    }
                }
                case 7 -> {
                    // Force repeated ring wrap and then a resize from a nonzero head.
                    for (int i = 0; i < 17; i++) { actual.addLast(v + i); expected.addLast(v + i); }
                    for (int i = 0; i < 11; i++) {
                        assertEquals(expected.removeFirst().longValue(), actual.removeFirst());
                    }
                }
                default -> {
                    if (expected.size() > 90) { expected.clear(); actual.clear(); }
                }
            }
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.contains(v), actual.contains(v));
            assertArrayEquals(values(expected), actual.toArray(), "step " + step);
            if (!expected.isEmpty()) {
                assertEquals(expected.getFirst().longValue(), actual.first());
                assertEquals(expected.getLast().longValue(), actual.last());
                assertEquals(expected.getFirst().longValue(), actual.peek());
                int index = random.nextInt(expected.size());
                assertEquals(values(expected)[index], actual.get(index));
            }
            if (step % 47 == 0) {
                assertArrayEquals(values(expected), actual.longStream().toArray());
                assertArrayEquals(values(expected), actual.parallelLongStream().toArray());
            }
        }
        actual.clear();
        // Primitive poll has no null sentinel: preserve its established throwing contract.
        assertThrows(NoSuchElementException.class, actual::poll);
        assertThrows(NoSuchElementException.class, actual::peek);
        assertThrows(NoSuchElementException.class, actual::first);
        assertThrows(NoSuchElementException.class, actual::last);
        assertThrows(IndexOutOfBoundsException.class, () -> actual.get(0));
        assertThrows(IllegalArgumentException.class, () -> new M3LongArrayDeque(-1));
    }

    @Test
    void callbackFailurePreservesThrowingValueAndUnreadSuffixAcrossRingWrap() {
        for (int failAt = 0; failAt < 16; failAt++) {
            var deque = new M3LongArrayDeque(8);
            // Nonzero head so the logical range wraps around the physical ring.
            for (int i = 0; i < 5; i++) { deque.addLast(-1); }
            for (int i = 0; i < 5; i++) { deque.removeFirstLong(); }
            for (long i = 0; i < 16; i++) { deque.addLast(i); }
            var seen = new ArrayList<Long>();
            var expected = new ArrayList<Long>();
            for (int i = 0; i < failAt; i++) if ((i & 1) == 0) expected.add((long) i);
            for (int i = failAt; i < 16; i++) expected.add((long) i);
            int point = failAt;
            var failure = new IllegalStateException("predicate");
            assertSame(failure, assertThrows(IllegalStateException.class, () -> deque.filterInPlace(v -> {
                seen.add(v);
                if (v == point) throw failure;
                return (v & 1) == 0;
            })));
            assertArrayEquals(java.util.stream.LongStream.rangeClosed(0, failAt).toArray(),
                    seen.stream().mapToLong(Long::longValue).toArray());
            assertArrayEquals(values(expected), deque.toArray());
            assertEquals(expected.size(), deque.size());
        }
        var deque = new M3LongArrayDeque();
        deque.addLast(1);
        deque.addLast(2);
        deque.addLast(3);
        assertThrows(NullPointerException.class, () -> deque.filterInPlace(null));
        assertArrayEquals(new long[]{1, 2, 3}, deque.toArray());
        var iterator = deque.iterator();
        assertEquals(0, deque.filterInPlace(v -> true));
        assertTrue(iterator.hasNext()); // no structural change, no fail-fast
        assertEquals(2, deque.filterInPlace(v -> v == 2));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertArrayEquals(new long[]{2}, deque.toArray());
        assertFalse(deque.addLastIf(4, v -> false));
        assertTrue(deque.addLastIf(4, v -> true));
        assertArrayEquals(new long[]{2, 4}, deque.toArray());
        assertThrows(NullPointerException.class, () -> deque.addLastIf(5, null));
    }

    @Test
    void queueAndIndexedViewsAreJdkShapedAliases() {
        var deque = new M3LongArrayDeque();
        assertTrue(deque.offer(5));
        assertTrue(deque.offerLast(6));
        assertTrue(deque.offerFirst(4));
        assertEquals(4, deque.peek());
        assertEquals(4, deque.first());
        assertEquals(4, deque.getFirst());
        assertEquals(6, deque.last());
        assertEquals(6, deque.getLast());
        assertEquals(4, deque.get(0));
        assertEquals(5, deque.get(1));
        assertEquals(6, deque.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> deque.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> deque.get(-1));
        var descending = deque.descendingIterator();
        assertEquals(6, descending.nextLong());
        assertEquals(4, deque.poll());
        assertThrows(ConcurrentModificationException.class, descending::hasNext);
        assertEquals(5, deque.removeFirst());
        assertEquals(6, deque.removeLast());
        assertEquals(0, deque.size());
        assertThrows(NoSuchElementException.class, deque::poll);
        assertThrows(NoSuchElementException.class, deque::removeFirst);
        assertThrows(NoSuchElementException.class, deque::removeLast);
        long[] batch = {1, 2, 3, 4};
        assertEquals(4, deque.offerBatch(batch, 0, 4));
        long[] out = new long[4];
        assertEquals(3, deque.drainTo(out, 0, 3));
        assertArrayEquals(new long[]{1, 2, 3, 0}, out);
        assertEquals(4, deque.pollReserved());
        assertFalse(deque.tryPoll(out, 0));
        assertEquals(8, deque.capacity());
    }
}
