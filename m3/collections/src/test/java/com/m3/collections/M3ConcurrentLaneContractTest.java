/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class M3ConcurrentLaneContractTest {
    @Test void atomicValuesWrapAndZeroWritesKeepPublicationLocations() {
        M3ConcurrentLongLane28 lane = M3Collections.concurrentLongLane();
        assertEquals(0, lane.getAcquire(M3Address28.MAX_SLOT));
        assertEquals(0, lane.allocatedBlockCount());
        lane.setRelease(0, 0);
        assertEquals(1, lane.allocatedBlockCount());
        assertTrue(lane.compareAndSet(4096, 0, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, lane.getAndAdd(4096, 1));
        assertEquals(Long.MIN_VALUE, lane.getAcquire(4096));
        assertFalse(lane.compareAndSet(4096, 0, 1));
        assertEquals(Long.MIN_VALUE, lane.getAndSet(4096, 0));
        assertTrue(lane.compareAndSet(4096, 0, 0));
        assertEquals(2, lane.allocatedBlockCount());
        assertThrows(IndexOutOfBoundsException.class, () -> lane.getAndSet(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> lane.prepare(2, 1));
    }

    @Test void racingPublicationAndAtomicAddsLoseNoUpdates() throws Exception {
        M3ConcurrentLongLane28 lane = new M3ConcurrentLongLane28();
        M3ConcurrentIntLane28 ints = new M3ConcurrentIntLane28();
        AtomicInteger winners = new AtomicInteger();
        race(8, actor -> {
            if (lane.compareAndSet(4095, 0, actor + 1)) winners.incrementAndGet();
            for (int i = 0; i < 15000; i++) {
                lane.getAndAdd(4096, 1);
                ints.getAndAdd(1 << 20, 1);
            }
            lane.setRelease((1 << 20) + actor, actor + 1);
        });
        assertEquals(1, winners.get());
        assertEquals(120000, lane.getAcquire(4096));
        assertEquals(120000, ints.getAcquire(1 << 20));
        assertEquals(3, lane.allocatedBlockCount());
        assertEquals(1, ints.allocatedBlockCount());
        for (int actor = 0; actor < 8; actor++) assertEquals(actor + 1, lane.getAcquire((1 << 20) + actor));
    }

    @Test void parallelStreamsSplitFixedRangesWithoutLosingOrDuplicatingSlots() {
        M3ConcurrentLongLane28 lane = new M3ConcurrentLongLane28();
        M3ConcurrentIntLane28 ints = new M3ConcurrentIntLane28();
        int n = 25001;
        lane.prepare(0, n); ints.prepare(0, n);
        long[] expected = new long[n]; int[] intExpected = new int[n];
        for (int i = 0; i < n; i++) {
            lane.setRelease(i, i); ints.setRelease(i, i);
            expected[i] = i; intExpected[i] = i;
        }
        assertArrayEquals(expected, lane.parallelStream(0, n).toArray());
        assertArrayEquals(intExpected, ints.parallelStream(0, n).toArray());
        assertEquals((long) n * (n - 1) / 2, lane.parallelStream(0, n).sum());
        Spliterator.OfLong tail = lane.spliterator(3, n);
        Spliterator.OfLong head = tail.trySplit();
        assertEquals(n - 3, head.estimateSize() + tail.estimateSize());
        assertTrue(tail.hasCharacteristics(Spliterator.CONCURRENT | Spliterator.SUBSIZED));
        assertFalse(tail.hasCharacteristics(Spliterator.IMMUTABLE | Spliterator.SORTED));
        assertEquals(0, lane.stream(M3Address28.MAX_SLOTS, M3Address28.MAX_SLOTS).count());
        assertThrows(NullPointerException.class, () -> tail.tryAdvance((java.util.function.LongConsumer) null));
        long before = tail.estimateSize();
        assertThrows(IllegalStateException.class, () -> tail.tryAdvance((long value) -> { throw new IllegalStateException(); }));
        assertEquals(before - 1, tail.estimateSize());
    }

    @Test void intAtomicBoundaryAndSparsePreparation() {
        M3ConcurrentIntLane28 lane = M3Collections.concurrentIntLane();
        lane.prepare(M3Address28.MAX_SLOT, M3Address28.MAX_SLOTS);
        assertEquals(1, lane.allocatedBlockCount());
        assertEquals(4096L * Integer.BYTES, lane.payloadBytes());
        assertTrue(lane.compareAndSet(M3Address28.MAX_SLOT, 0, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, lane.getAndAdd(M3Address28.MAX_SLOT, 1));
        assertEquals(Integer.MIN_VALUE, lane.getAndSet(M3Address28.MAX_SLOT, 7));
        assertEquals(7, lane.getAcquire(M3Address28.MAX_SLOT));
        assertFalse(lane.compareAndSet(M3Address28.MAX_SLOT, 8, 9));
    }

    @Test void concurrentCollectionStreamsPreserveCapturedSnapshotsAndIndexOrder() {
        M3ConcurrentLongMap map = new M3ConcurrentLongMap();
        M3ConcurrentLongSet set = new M3ConcurrentLongSet();
        M3ConcurrentBitSet bits = new M3ConcurrentBitSet(10003);
        for (int i = 2000; i >= 0; i--) { map.put(i, 2L * i); set.add(i); bits.set(i * 3); }
        var keys = map.keyStream(); var values = map.valueStream(); var members = set.parallelLongStream();
        map.clear(); set.clear();
        assertArrayEquals(java.util.stream.LongStream.rangeClosed(0, 2000).toArray(), keys.parallel().toArray());
        assertArrayEquals(java.util.stream.LongStream.rangeClosed(0, 2000).map(i -> i * 2).toArray(), values.parallel().toArray());
        assertArrayEquals(java.util.stream.LongStream.rangeClosed(0, 2000).toArray(), members.toArray());
        int[] expected = java.util.stream.IntStream.rangeClosed(0, 2000).map(i -> i * 3).toArray();
        assertArrayEquals(expected, bits.setBitStream().toArray());
        assertArrayEquals(expected, bits.parallelSetBitStream().toArray());
        assertEquals(0, new M3ConcurrentBitSet(0).parallelSetBitStream().count());
        bits.set(10002);
        assertEquals(10002, bits.parallelSetBitStream().max().orElseThrow());
        assertFalse(bits.setBitStream().spliterator().hasCharacteristics(Spliterator.SIZED));
    }

    private static void race(int actors, java.util.function.IntConsumer body) throws Exception {
        var pool = Executors.newFixedThreadPool(actors);
        CountDownLatch ready = new CountDownLatch(actors);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> jobs = new ArrayList<>();
        try {
            for (int actor = 0; actor < actors; actor++) {
                int id = actor;
                jobs.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                    body.accept(id); return null;
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            for (Future<?> job : jobs) job.get(30, TimeUnit.SECONDS);
        } finally {
            start.countDown(); pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
