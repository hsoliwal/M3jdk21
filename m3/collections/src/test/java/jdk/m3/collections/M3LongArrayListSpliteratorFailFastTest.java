/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ConcurrentModificationException;
import java.util.Spliterator;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

/**
 * Merge-loss case 06 restore: {@link LongArrayList#longSpliterator()} is fail-fast like the list's own
 * iterators (the count is captured when the spliterator is created), restoring the behaviour the 2026-10-07
 * IndexSpliterator had before merge e6ff978d1145 dropped it.
 */
final class M3LongArrayListSpliteratorFailFastTest {
    private static final int SIZE = 64;

    private static LongArrayList filled() {
        LongArrayList list = new LongArrayList();
        for (long value = 0; value < SIZE; value++) {
            list.add(value * 3);
        }
        return list;
    }

    @Test
    void unmodifiedTraversalVisitsEveryElementInOrder() {
        LongArrayList list = filled();
        long[] seen = new long[SIZE];
        int[] at = new int[1];
        list.longSpliterator().forEachRemaining((long value) -> seen[at[0]++] = value);
        assertArrayEquals(LongStream.range(0, SIZE).map(v -> v * 3).toArray(), seen);
    }

    @Test
    void structuralChangeAfterCreationFailsOnAdvance() {
        LongArrayList list = filled();
        Spliterator.OfLong split = list.longSpliterator();
        list.add(7L);
        assertThrows(ConcurrentModificationException.class, () -> split.tryAdvance((long value) -> { }));
    }

    @Test
    void structuralChangeByTheActionFailsTheBulkTraversal() {
        LongArrayList list = filled();
        assertThrows(ConcurrentModificationException.class,
                () -> list.longSpliterator().forEachRemaining((long value) -> {
                    if (value == 0L) {
                        list.add(-1L);
                    }
                }));
    }

    @Test
    void splitChildSharesTheCapturedCount() {
        LongArrayList list = filled();
        Spliterator.OfLong parent = list.longSpliterator();
        Spliterator.OfLong child = parent.trySplit();
        assertNotNull(child);
        list.add(11L);
        assertThrows(ConcurrentModificationException.class, () -> child.tryAdvance((long value) -> { }));
        assertThrows(ConcurrentModificationException.class, () -> parent.tryAdvance((long value) -> { }));
    }

    @Test
    void nonStructuralReadsKeepWorking() {
        LongArrayList list = filled();
        Spliterator.OfLong split = list.longSpliterator();
        assertTrue(split.tryAdvance((long value) -> { }));
        assertTrue(split.estimateSize() == SIZE - 1);
    }
}
