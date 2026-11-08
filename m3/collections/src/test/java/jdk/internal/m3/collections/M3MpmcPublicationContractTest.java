/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import org.junit.jupiter.api.Test;

final class M3MpmcPublicationContractTest {
    private static final VarHandle HEAD, TAIL, VALUES, SEQUENCES;
    private static final VarHandle CELL = MethodHandles.arrayElementVarHandle(long[].class);
    static {
        try {
            var lookup = MethodHandles.privateLookupIn(M3PackedLongMpmcQueue.class, MethodHandles.lookup());
            HEAD = lookup.findVarHandle(M3PackedLongMpmcQueue.class, "head", long.class);
            TAIL = lookup.findVarHandle(M3PackedLongMpmcQueue.class, "tail", long.class);
            VALUES = lookup.findVarHandle(M3PackedLongMpmcQueue.class, "values", long[].class);
            SEQUENCES = lookup.findVarHandle(M3PackedLongMpmcQueue.class, "sequences", long[].class);
        } catch (ReflectiveOperationException error) { throw new ExceptionInInitializerError(error); }
    }

    @Test void reservedUnpublishedHeadCanHideACompletedLaterOffer() {
        var queue = new M3PackedLongMpmcQueue(4);
        // Exact pause point of offer: tail CAS succeeded, value/sequence not published yet.
        assertTrue(TAIL.compareAndSet(queue, 0L, 1L));
        assertTrue(queue.offer(22));
        assertEquals(2, queue.sizeApprox());
        assertFalse(queue.tryPoll((long value) -> fail()));
        assertThrows(NoSuchElementException.class, queue::peek);
        assertThrows(NoSuchElementException.class, queue::poll);
        long[] target = {99, 99};
        assertEquals(0, queue.drainTo(target, 0, 2));
        assertArrayEquals(new long[] {99, 99}, target);
        CELL.setRelease((long[]) VALUES.get(queue), 0, 11L);
        CELL.setRelease((long[]) SEQUENCES.get(queue), 0, 1L);
        assertEquals(2, queue.drainTo(target, 0, 2));
        assertArrayEquals(new long[] {11, 22}, target);
    }

    @Test void stalledConsumerCanDelayReuseAndThrowingCallbackStillRecyclesSlot() {
        var queue = new M3PackedLongMpmcQueue(2);
        assertEquals(2, queue.offerBatch(new long[] {11, 22}, 0, 2));
        // Exact pause point after a consumer reserves slot 0 but before releasing sequence 2.
        assertTrue(HEAD.compareAndSet(queue, 0L, 1L));
        assertEquals(22, queue.poll());
        assertFalse(queue.offer(33));
        CELL.setRelease((long[]) SEQUENCES.get(queue), 0, 2L);
        assertTrue(queue.offer(33));
        assertThrows(IllegalStateException.class, () -> queue.tryPoll((long value) -> { throw new IllegalStateException(); }));
        assertTrue(queue.offer(Long.MIN_VALUE)); assertEquals(Long.MIN_VALUE, queue.poll());
        assertTrue(queue.offer(Long.MAX_VALUE)); assertEquals(Long.MAX_VALUE, queue.poll());
    }

    @Test void sequenceArithmeticSurvivesSignedLongWrap() {
        var queue = new M3PackedLongMpmcQueue(4); long start = Long.MAX_VALUE - 1;
        HEAD.set(queue, start); TAIL.set(queue, start);
        long[] sequence = (long[]) SEQUENCES.get(queue);
        for (int i = 0; i < 4; i++) CELL.set(sequence, (int) (start + i) & 3, start + i);
        for (int pass = 0; pass < 50; pass++) {
            long[] input = {pass, -pass, Long.MIN_VALUE, Long.MAX_VALUE};
            assertEquals(4, queue.offerBatch(input, 0, 4)); assertFalse(queue.offer(99));
            long[] output = new long[4]; assertEquals(4, queue.drainTo(output, 0, 4));
            assertArrayEquals(input, output); assertEquals(0, queue.sizeApprox());
        }
    }

    @Test void concurrentProducersConsumersHaveNoLostOrDuplicateValues() throws Exception {
        int producers = 4, each = 12000, total = producers * each;
        var queue = new M3PackedLongMpmcQueue(128);
        var seen = new AtomicIntegerArray(total); var removed = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(8); var start = new CountDownLatch(1);
        var jobs = new ArrayList<Future<?>>(); long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        try {
            for (int producer = 0; producer < producers; producer++) {
                int id = producer;
                jobs.add(pool.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    for (int n = 0; n < each; n++) while (!queue.offer((long) id * each + n)) checkDeadline(deadline);
                    return null;
                }));
            }
            for (int consumer = 0; consumer < 4; consumer++) jobs.add(pool.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS)); long[] value = new long[1];
                while (removed.get() < total) {
                    if (queue.tryPoll(value, 0)) {
                        int v = Math.toIntExact(value[0]); assertTrue(v >= 0 && v < total);
                        assertEquals(0, seen.getAndIncrement(v)); removed.incrementAndGet();
                    } else checkDeadline(deadline);
                }
                return null;
            }));
            start.countDown(); for (var job : jobs) job.get(25, TimeUnit.SECONDS);
            assertEquals(total, removed.get());
            for (int i = 0; i < total; i++) assertEquals(1, seen.get(i));
        } finally {
            start.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
    private static void checkDeadline(long deadline) {
        if (Thread.currentThread().isInterrupted() || System.nanoTime() >= deadline) fail("actor deadline");
        Thread.onSpinWait();
    }
}
