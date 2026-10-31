/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ConcurrentModificationException;
import org.junit.jupiter.api.Test;

/** Public-contract oracle, frozen against the pre-change deque before recipe application. */
final class M3DequeSpanCopyTest {
    private static long value(int i) {
        return switch (i & 7) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3 -> -1L;
            default -> 0x9e3779b97f4a7c15L * i;
        };
    }

    private static M3LongArrayDeque ring(int capacity, int head, int size) {
        var deque = new M3LongArrayDeque(capacity - 1);
        assertEquals(capacity, deque.capacity());
        for (int i = 0; i < head; i++) { deque.addLast(-7L); deque.removeFirstLong(); }
        for (int i = 0; i < size; i++) deque.addLast(value(i));
        return deque;
    }

    @Test void exportsEveryHeadAndSizeWithoutMutationOrAliasing() {
        int layouts = 0;
        for (int capacity : new int[]{8, 16, 32, 64}) {
            for (int head = 0; head < capacity; head++) {
                for (int size = 0; size <= capacity; size++) {
                    var deque = ring(capacity, head, size);
                    var forward = deque.iterator();
                    var backward = deque.descendingIterator();
                    long[] expected = new long[size];
                    for (int i = 0; i < size; i++) expected[i] = value(i);
                    long[] first = deque.toArray();
                    long[] second = deque.toArray();
                    assertNotSame(first, second, "each export owns its array, including empty exports");
                    assertArrayEquals(expected, first);
                    assertArrayEquals(expected, second);
                    assertEquals(size, deque.size());
                    assertEquals(capacity, deque.capacity());
                    if (size != 0) first[0] ^= -1L;
                    assertArrayEquals(expected, deque.toArray());
                    for (int i = 0; i < size; i++) {
                        assertTrue(forward.hasNext()); assertEquals(expected[i], forward.nextLong());
                        assertTrue(backward.hasNext()); assertEquals(expected[size - 1 - i], backward.nextLong());
                    }
                    assertFalse(forward.hasNext()); assertFalse(backward.hasNext());
                    deque.addLast(19L);
                    assertArrayEquals(expected, second, "later deque mutation cannot change an export");
                    layouts++;
                }
            }
        }
        assertEquals(5560, layouts, "bounded exhaustive head/size spectrum");
    }

    @Test void growsAtEitherEndFromEveryHeadAndPreservesIteratorEpochs() {
        int layouts = 0;
        for (int capacity : new int[]{8, 16, 32, 64, 128, 256}) {
            for (int head = 0; head < capacity; head++) {
                for (boolean atFront : new boolean[]{false, true}) {
                    var deque = ring(capacity, head, capacity);
                    var forward = deque.iterator();
                    var backward = deque.descendingIterator();
                    long[] expected = new long[capacity + 1];
                    for (int i = 0; i < capacity; i++) expected[i + (atFront ? 1 : 0)] = value(i);
                    expected[atFront ? 0 : capacity] = 37L;
                    if (atFront) deque.addFirst(37L); else deque.addLast(37L);
                    assertEquals(capacity * 2, deque.capacity());
                    assertEquals(capacity + 1, deque.size());
                    assertArrayEquals(expected, deque.toArray());
                    assertThrows(ConcurrentModificationException.class, forward::hasNext);
                    assertThrows(ConcurrentModificationException.class, backward::nextLong);
                    for (int i = 0; i < expected.length; i++) assertEquals(expected[i], deque.get(i));
                    for (long item : expected) assertEquals(item, deque.removeFirstLong());
                    assertEquals(0, deque.size());
                    assertArrayEquals(new long[0], deque.toArray());
                    layouts++;
                }
            }
        }
        assertEquals(1008, layouts, "both growth ends at every bounded head");
    }
}
