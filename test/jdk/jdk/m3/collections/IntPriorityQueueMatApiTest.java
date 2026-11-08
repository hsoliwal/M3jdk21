/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * The Synexia mat-lane MatIntPriorityQueue API merged into IntPriorityQueue (peek, poll, primitivePayloadBytes; isEmpty
 * comes from IntCollection). Replays the Mat deep contract (heap order against {@code PriorityQueue<Integer>}) through
 * the merged names, with interleaved peeks, duplicates and the int extremes.
 */
final class IntPriorityQueueMatApiTest {
    @Test void matContractHeapOrderThroughMergedNames() {
        Random random = new Random(0x4D41_5451L);
        IntPriorityQueue queue = new IntPriorityQueue();
        PriorityQueue<Integer> oracle = new PriorityQueue<>();
        for (int i = 0; i < 100_000; i++) {
            int value = switch (i % 17) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2 -> 0;
                default -> random.nextInt();
            };
            queue.offer(value);
            oracle.offer(value);
            if ((i & 1023) == 0) {
                assertEquals(oracle.peek().intValue(), queue.peek());
            }
        }
        while (!oracle.isEmpty()) {
            assertEquals(oracle.peek().intValue(), queue.peek());
            assertEquals(oracle.poll().intValue(), queue.poll());
        }
        assertTrue(queue.isEmpty());
    }

    @Test void interleavedOffersAndPollsMatchOracle() {
        Random random = new Random(7L);
        IntPriorityQueue queue = new IntPriorityQueue(1);
        PriorityQueue<Integer> oracle = new PriorityQueue<>();
        for (int step = 0; step < 20_000; step++) {
            if (oracle.isEmpty() || random.nextInt(3) != 0) {
                int value = random.nextInt(64) - 32;
                queue.offer(value);
                oracle.offer(value);
            } else {
                assertEquals(oracle.poll().intValue(), queue.poll());
            }
            assertEquals(oracle.size(), queue.size());
            assertEquals(oracle.isEmpty(), queue.isEmpty());
        }
    }

    @Test void emptyAndPayloadContract() {
        IntPriorityQueue queue = new IntPriorityQueue(16);
        assertThrows(NoSuchElementException.class, queue::peek);
        assertThrows(NoSuchElementException.class, queue::poll);
        assertEquals(queue.payloadBytes(), queue.primitivePayloadBytes());
        assertEquals(16L * Integer.BYTES, queue.primitivePayloadBytes());
        queue.offer(3);
        queue.offer(1);
        assertEquals(1, queue.peek());
        assertEquals(2, queue.size());
        queue.clear();
        assertTrue(queue.isEmpty());
        assertThrows(NoSuchElementException.class, queue::peek);
    }
}
