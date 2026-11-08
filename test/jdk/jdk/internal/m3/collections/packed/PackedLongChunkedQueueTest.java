// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongIterator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class PackedLongChunkedQueueTest {
    @Test
    void defaultGrowthDoublesChunksWithoutChangingFifoOrder() {
        PackedLongChunkedQueue queue = new PackedLongChunkedQueue();
        assertEquals(0, queue.capacity());

        assertTrue(queue.offer(0));
        assertEquals(1, queue.capacity());
        assertTrue(queue.offer(1));
        assertEquals(3, queue.capacity());
        assertTrue(queue.offer(2));
        assertEquals(3, queue.capacity());
        assertTrue(queue.offer(3));
        assertEquals(7, queue.capacity());

        for (int value = 4; value < 300; value++) {
            assertTrue(queue.offer(value));
        }
        assertEquals(300, queue.size());
        assertEquals(383, queue.capacity());
        assertEquals(9, queue.allocatedBlockCount());
        assertEquals(0, queue.peek());
        assertEquals(299, queue.back());

        for (int expected = 0; expected < 200; expected++) {
            assertEquals(expected, queue.poll());
        }
        assertEquals(100, queue.size());
        assertEquals(256, queue.capacity());
        assertArrayEquals(
                java.util.stream.LongStream.range(200, 300).toArray(),
                queue.toArray());

        queue.clear();
        assertTrue(queue.isEmpty());
        assertEquals(1, queue.allocatedBlockCount());
        assertEquals(128, queue.capacity());

        queue.shrinkToFit();
        assertEquals(0, queue.allocatedBlockCount());
        assertEquals(0, queue.capacity());
        assertEquals(0L, queue.payloadBytes());
    }

    @Test
    void customGrowthAndBulkPathsRemainPrimitiveAndOrdered() {
        PackedLongChunkedQueue queue = new PackedLongChunkedQueue(4, 16);
        long[] source = java.util.stream.LongStream.range(0, 30).toArray();
        assertEquals(source.length, queue.offerBatch(source, 0, source.length));
        assertEquals(30, queue.size());
        assertEquals(44, queue.capacity());
        assertEquals(4, queue.allocatedBlockCount());

        long[] first = new long[17];
        assertEquals(17, queue.drainTo(first, 0, first.length));
        assertArrayEquals(java.util.stream.LongStream.range(0, 17).toArray(), first);
        assertEquals(13, queue.size());
        assertEquals(32, queue.capacity());
        assertEquals(2, queue.allocatedBlockCount());

        long[] one = new long[1];
        assertTrue(queue.tryPoll(one, 0));
        assertEquals(17L, one[0]);
        assertEquals(18L, queue.peek());
        assertTrue(queue.contains(29L));
        assertFalse(queue.contains(-1L));
    }

    @Test
    void iteratorIsFailFastAndAllLongBitPatternsArePayload() {
        PackedLongChunkedQueue queue = new PackedLongChunkedQueue(2, 8);
        assertTrue(queue.offer(Long.MIN_VALUE));
        assertTrue(queue.offer(0L));
        assertTrue(queue.offer(Long.MAX_VALUE));

        LongIterator iterator = queue.iterator();
        assertEquals(Long.MIN_VALUE, iterator.nextLong());
        assertTrue(queue.offer(7L));
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);

        assertEquals(Long.MIN_VALUE, queue.poll());
        assertEquals(0L, queue.poll());
        assertEquals(Long.MAX_VALUE, queue.poll());
        assertEquals(7L, queue.poll());
        assertThrows(NoSuchElementException.class, queue::poll);
        assertThrows(NoSuchElementException.class, queue::peek);
        assertThrows(NoSuchElementException.class, queue::back);
    }

    @Test
    void randomizedDifferentialAgainstDeque() {
        PackedLongChunkedQueue queue = new PackedLongChunkedQueue();
        ArrayDeque<Long> oracle = new ArrayDeque<>();
        Random random = new Random(0x41425345494c514cL);

        for (int step = 0; step < 20_000; step++) {
            int operation = random.nextInt(8);
            switch (operation) {
                case 0, 1, 2 -> {
                    long value = random.nextLong();
                    assertTrue(queue.offer(value));
                    oracle.addLast(value);
                }
                case 3 -> {
                    if (oracle.isEmpty()) {
                        assertThrows(NoSuchElementException.class, queue::poll);
                    } else {
                        assertEquals(oracle.removeFirst().longValue(), queue.poll());
                    }
                }
                case 4 -> {
                    if (oracle.isEmpty()) {
                        assertThrows(NoSuchElementException.class, queue::peek);
                    } else {
                        assertEquals(oracle.getFirst().longValue(), queue.peek());
                        assertEquals(oracle.getLast().longValue(), queue.back());
                    }
                }
                case 5 -> {
                    long[] batch = new long[random.nextInt(12)];
                    for (int i = 0; i < batch.length; i++) {
                        batch[i] = random.nextLong();
                    }
                    assertEquals(batch.length, queue.offerBatch(batch, 0, batch.length));
                    for (long value : batch) {
                        oracle.addLast(value);
                    }
                }
                case 6 -> {
                    int requested = random.nextInt(12);
                    long[] drained = new long[requested];
                    int actual = queue.drainTo(drained, 0, requested);
                    assertEquals(Math.min(requested, oracle.size()), actual);
                    for (int i = 0; i < actual; i++) {
                        assertEquals(oracle.removeFirst().longValue(), drained[i]);
                    }
                }
                case 7 -> {
                    if (random.nextInt(64) == 0) {
                        queue.clear();
                        oracle.clear();
                        if (random.nextBoolean()) {
                            queue.shrinkToFit();
                        }
                    }
                }
                default -> throw new AssertionError();
            }

            assertEquals(oracle.size(), queue.size());
            long[] snapshot = queue.toArray();
            assertEquals(oracle.size(), snapshot.length);
            int at = 0;
            for (long value : oracle) {
                assertEquals(value, snapshot[at++]);
            }
        }
    }

    @Test
    void constructorAndRangeContractsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> new PackedLongChunkedQueue(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new PackedLongChunkedQueue(8, 4));

        PackedLongChunkedQueue queue = new PackedLongChunkedQueue(4);
        assertThrows(NullPointerException.class, () -> queue.offerBatch(null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> queue.offerBatch(new long[1], 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> queue.drainTo(new long[1], 1, 1));
    }
}
