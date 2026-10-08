/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ConcurrentModificationException;
import java.util.Spliterator;
import org.junit.jupiter.api.Test;

/** Current-target integration oracle; historical superseded contracts stay frozen separately. */
final class M3ReceivingRecoveryTest {
    @Test void restoredSequenceAndCurrentListAreOneOwnerThroughTheFactory() {
        M3LongArrayList owner = M3Collections.longArrayList(0);
        M3LongList current = owner;
        M3LongSequence restored = current;
        assertSame(owner, restored);
        assertEquals(0, restored.capacity());
        assertTrue(restored.addAll(new long[] {1, 2, 3, 4}));
        current.add(2, 8);
        restored.insert(0, 9);
        assertTrue(restored.removeValue(3));
        assertArrayEquals(new long[] {9, 1, 2, 8, 4}, current.toArray());
        var split = restored.longSpliterator();
        current.ensureCapacity(4096);
        current.set(0, 19);
        assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED,
                split.characteristics());
        var observed = java.util.stream.LongStream.builder();
        split.forEachRemaining((long value) -> observed.add(value));
        assertArrayEquals(new long[] {19, 1, 2, 8, 4}, observed.build().toArray());
        assertEquals(34, restored.parallelLongStream().sum());
    }

    @Test void restoredFactoryKeepsCurrentSmallListSortInvalidation() {
        for (int size = 0; size <= 1; size++) {
            M3LongList list = M3Collections.longArrayList();
            if (size == 1) list.add(7);
            var cursor = list.iterator();
            list.sort();
            assertThrows(ConcurrentModificationException.class, cursor::hasNext);
        }
    }

    @Test void restoredDequeFactoryKeepsCurrentCallbackFailureAndNoOpSemantics() {
        M3LongDeque deque = M3Collections.longArrayDeque(8);
        for (long value = 1; value <= 5; value++) deque.addLast(value);
        var unchanged = deque.iterator();
        assertEquals(0, deque.filterInPlace(value -> true));
        assertTrue(unchanged.hasNext());
        RuntimeException failure = new IllegalStateException("predicate");
        assertSame(failure, assertThrows(RuntimeException.class, () -> deque.filterInPlace(value -> {
            if (value == 3) throw failure;
            return value == 2;
        })));
        assertArrayEquals(new long[] {2, 3, 4, 5}, deque.toArray());
        assertThrows(ConcurrentModificationException.class, unchanged::hasNext);
        assertEquals(2, deque.removeFirst());
        assertEquals(5, deque.removeLast());
        assertArrayEquals(new long[] {3, 4}, deque.parallelLongStream().toArray());
    }
}
