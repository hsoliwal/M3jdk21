/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.function.LongPredicate;
import org.junit.jupiter.api.Test;

/** API_UNION U3: LongPriorityQueue is one interface; the Synexia contract and the M3 members meet on it. */
class LongPriorityQueueUnionTest {

    private static long[] drain(LongPriorityQueue queue) {
        long[] out = new long[queue.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = queue.poll();
        }
        return out;
    }

    @Test
    void synexiaContractMembers() {
        LongPriorityQueue queue = LongPriorityQueue.of();
        assertTrue(queue.offer(5));
        assertTrue(queue.offerIf(3, v -> v > 0));
        assertFalse(queue.offerIf(-1, v -> v > 0));
        assertTrue(queue.add(9));
        assertEquals(3, queue.peek());
        assertEquals(queue.peek(), queue.firstLong());
        assertEquals(3, queue.replaceTop(7));
        assertArrayEquals(new long[] {5, 7, 9}, queue.toSortedArray());
        assertEquals(5, queue.poll());
        assertEquals(7, queue.removeFirstLong());
        assertTrue(queue.capacity() >= queue.size());
        assertEquals(9, queue.poll());
        assertThrows(NoSuchElementException.class, queue::peek);
        assertThrows(NoSuchElementException.class, queue::poll);
        assertThrows(NoSuchElementException.class, () -> queue.replaceTop(1));
        assertThrows(NullPointerException.class, () -> queue.offerIf(1, null));
    }

    @Test
    void filterInPlaceKeepsMatchesInHeapOrder() {
        LongPriorityQueue queue = LongPriorityQueue.of(new long[] {9, 4, 7, 2, 8, 1, 6, 3, 5, 0});
        assertEquals(5, queue.filterInPlace(v -> (v & 1) == 0));
        assertArrayEquals(new long[] {0, 2, 4, 6, 8}, drain(queue));
        assertEquals(0, LongPriorityQueue.of().filterInPlace(v -> false));
    }

    @Test
    void filterInPlaceExceptionKeepsThrowingValueAndSuffix() {
        LongPriorityQueue queue = LongPriorityQueue.of(new long[] {1, 2, 3, 4, 5, 6});
        IllegalStateException boom = new IllegalStateException("boom");
        LongPredicate predicate = v -> {
            if (v == 4) {
                throw boom;
            }
            return v != 2;
        };
        // Heap (array) order of a bottom-up heap of 1..6 is 1,2,3,4,5,6; 2 is rejected before 4 throws.
        assertSame(boom, assertThrows(IllegalStateException.class, () -> queue.filterInPlace(predicate)));
        assertArrayEquals(new long[] {1, 3, 4, 5, 6}, drain(queue));
    }

    @Test
    void removeDefaultTakesExactlyOneOccurrence() {
        LongPriorityQueue inner = LongPriorityQueue.of(new long[] {4, 1, 4, 2});
        LongPriorityQueue foreign = new Foreign(inner);
        assertTrue(foreign.remove(4));
        assertArrayEquals(new long[] {1, 2, 4}, foreign.toSortedArray());
        assertFalse(foreign.remove(9));
        assertEquals(3, foreign.size());
    }

    /** Another implementation that keeps the interface's remove(long) and toSortedArray() defaults. */
    private static final class Foreign implements LongPriorityQueue {
        private final LongPriorityQueue d;

        Foreign(LongPriorityQueue d) { this.d = d; }

        @Override public boolean offer(long v) { return d.offer(v); }
        @Override public boolean offerIf(long v, LongPredicate p) { return d.offerIf(v, p); }
        @Override public long peek() { return d.peek(); }
        @Override public long poll() { return d.poll(); }
        @Override public long replaceTop(long v) { return d.replaceTop(v); }
        @Override public int filterInPlace(LongPredicate p) { return d.filterInPlace(p); }
        @Override public int capacity() { return d.capacity(); }
        @Override public int size() { return d.size(); }
        @Override public boolean contains(long v) { return d.contains(v); }
        @Override public void clear() { d.clear(); }
        @Override public LongIterator iterator() { return d.iterator(); }
        @Override public long[] toArray() { return d.toArray(); }
    }
}
