/*
 * Copyright 2026 Hitesh Soliwal and contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.apache.queue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Oracle for the received Commons 4.5.0 circular array owner.
 * Nonpower-of-two capacities, wrap and eviction retain the original API contract.
 */
class CircularFifoQueueTwoSpanIndexTest {

    @Test
    void randomizedWrappedQueueIndicesMatchIndependentDeque() {
        final int[] capacities = {1, 2, 3, 4, 5, 7, 9, 16, 31, 64, 128};
        for (int capacity : capacities) {
            for (int seed = 0; seed < 12; seed++) {
                CircularFifoQueue<Integer> actual = new CircularFifoQueue<>(capacity);
                ArrayDeque<Integer> expected = new ArrayDeque<>();
                Random rnd = new Random(0x4d334649464fL ^ ((long) capacity << 20) ^ seed);
                for (int step = 0; step < 2_000; step++) {
                    int action = rnd.nextInt(7);
                    if (action < 4) {
                        int value = rnd.nextInt();
                        if (expected.size() == capacity) {
                            expected.removeFirst();
                        }
                        expected.addLast(value);
                        assertTrue(actual.add(value));
                    } else if (action == 4) {
                        if (expected.isEmpty()) {
                            assertThrows(NoSuchElementException.class, actual::remove);
                        } else {
                            assertEquals(expected.removeFirst(), actual.remove());
                        }
                    } else if (action == 5 && (step & 63) == 0) {
                        expected.clear();
                        actual.clear();
                    } else {
                        assertEquals(expected.peekFirst(), actual.peek());
                    }
                    assertEquals(expected.size(), actual.size());
                    List<Integer> order = new ArrayList<>(expected);
                    assertArrayEquals(order.toArray(), actual.toArray());
                    for (int index = 0; index < order.size(); index++) {
                        assertEquals(order.get(index), actual.get(index),
                                "capacity=" + capacity + " seed=" + seed + " step=" + step);
                    }
                    assertThrows(NoSuchElementException.class, () -> actual.get(-1));
                    assertThrows(NoSuchElementException.class, () -> actual.get(actual.size()));
                }
            }
        }
    }

    @Test
    void unwrappedCapacityAndOverflowBoundariesRemainIntact() {
        for (int capacity : new int[] {1, 2, 3, 7, 32, 128}) {
            CircularFifoQueue<Integer> actual = new CircularFifoQueue<>(capacity);
            for (int index = 0; index < capacity * 4; index++) {
                actual.add(index);
                int start = Math.max(0, index - capacity + 1);
                for (int j = 0; j < actual.size(); j++) {
                    assertEquals(Integer.valueOf(start + j), actual.get(j));
                }
            }
        }
    }
    /**
     * The public size() is virtual. A subclass can intentionally expose more
     * logical positions than the physical ring capacity. Keep the cold
     * multiple-wrap indexing rule while avoiding modulo in normal queues.
     */
    private static final class ReportedSizeQueue extends CircularFifoQueue<Integer> {
        private static final long serialVersionUID = 1L;
        private boolean reportLargeSize;

        private ReportedSizeQueue(int capacity) {
            super(capacity);
        }

        @Override
        public int size() {
            return reportLargeSize ? 10 : super.size();
        }
    }

    @Test
    void overridingSizePreservesMultipleWrapIndices() {
        ReportedSizeQueue actual = new ReportedSizeQueue(3);
        actual.add(10);
        actual.add(20);
        actual.add(30);
        actual.reportLargeSize = true;
        int[] order = {10, 20, 30};
        for (int index = 0; index < 10; index++) {
            assertEquals(Integer.valueOf(order[index % order.length]), actual.get(index));
        }
    }

}
