/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class M3LongLazyPriorityFrontierTest {
    @Test
    void constructionAndBatchOfferDoNotComputeExactScores() {
        var calls = new AtomicInteger();
        var queue = new M3LongLazyPriorityFrontier(id -> {
            calls.incrementAndGet();
            return id;
        });
        queue.offerAll(new long[] {1, 3, 2}, new long[] {1, 3, 2});
        assertEquals(0, calls.get());
        assertEquals(3, queue.peekPriority());
        assertEquals(3, queue.peekId());
        assertEquals(1, calls.get());
        assertEquals(3, queue.pollId());
        assertEquals(1, queue.evaluations());
        assertEquals(2, queue.size());
    }

    @Test
    void optimisticDecoysAreRefinedUntilAnExactWinnerIsCertified() {
        var calls = new AtomicInteger();
        var queue = new M3LongLazyPriorityFrontier(id -> {
            calls.incrementAndGet();
            return id == 3 ? 10 : id == 1 ? 9 : 80;
        });
        queue.offerAll(new long[] {3, 1, 2}, new long[] {100, 90, 80});
        assertEquals(2, queue.peekId());
        assertEquals(80, queue.peekPriority());
        assertEquals(3, calls.get());
        assertEquals(3, queue.evaluations());
        assertEquals(2, queue.pollId());
        assertEquals(3, queue.pollId());
        assertEquals(1, queue.pollId());
        assertTrue(queue.isEmpty());
    }

    @Test
    void randomizedDifferentialOrderingSupportsDuplicatesAndSignedExtrema() {
        var random = new Random(0x5a1ced23L);
        var queue = new M3LongLazyPriorityFrontier(id -> id * 17L - 500L);
        record Entry(long id, long score, int serial) {}
        List<Entry> reference = new ArrayList<>();
        long[] ids = new long[4096];
        long[] bounds = new long[ids.length];
        for (int i = 0; i < ids.length; i++) {
            long id = random.nextInt(100) - 50L;
            long score = id * 17L - 500L;
            ids[i] = id;
            bounds[i] = score + random.nextInt(3000);
            reference.add(new Entry(id, score, i));
        }
        queue.offerAll(ids, bounds);
        reference.sort(Comparator.comparingLong(Entry::score).reversed()
                .thenComparingLong(Entry::id).thenComparingInt(Entry::serial));
        for (Entry entry : reference) {
            assertEquals(entry.score(), queue.peekPriority());
            assertEquals(entry.id(), queue.pollId());
        }
        assertTrue(queue.isEmpty());
        assertThrows(NoSuchElementException.class, queue::peekId);

        var extremes = new M3LongLazyPriorityFrontier(id -> id);
        extremes.offerAll(new long[] {Long.MIN_VALUE, Long.MAX_VALUE, 0},
                new long[] {Long.MIN_VALUE, Long.MAX_VALUE, 0});
        assertEquals(Long.MAX_VALUE, extremes.pollId());
        assertEquals(0, extremes.pollId());
        assertEquals(Long.MIN_VALUE, extremes.pollId());
    }

    @Test
    void failedScoreOrInvalidUpperBoundCannotConsumeAnEntry() {
        var calls = new AtomicInteger();
        var queue = new M3LongLazyPriorityFrontier(id -> {
            if (calls.getAndIncrement() == 0) throw new IllegalStateException("transient");
            return 12;
        });
        queue.offer(99, 20);
        assertThrows(IllegalStateException.class, queue::peekId);
        assertEquals(1, queue.size());
        assertEquals(0, queue.evaluations());
        assertEquals(99, queue.pollId());
        assertTrue(queue.isEmpty());

        var badBound = new M3LongLazyPriorityFrontier(id -> 21);
        badBound.offer(1, 20);
        assertThrows(IllegalArgumentException.class, badBound::pollId);
        assertEquals(1, badBound.size());
        assertEquals(0, badBound.evaluations());
    }

    @Test
    void scoreCallbacksCannotReenterMutationOrTopQueries() {
        var reference = new M3LongLazyPriorityFrontier[1];
        reference[0] = new M3LongLazyPriorityFrontier(id -> {
            reference[0].offer(17, 18);
            return 3;
        });
        reference[0].offer(1, 10);
        assertThrows(IllegalStateException.class, reference[0]::peekPriority);
        assertEquals(1, reference[0].size());
        assertEquals(0, reference[0].evaluations());
    }

    @Test
    void validationAndClearingLeaveQueueReusable() {
        var queue = new M3LongLazyPriorityFrontier(0, id -> id);
        assertEquals(25L, queue.payloadBytes());
        assertThrows(IllegalArgumentException.class,
                () -> queue.offerAll(new long[] {1}, new long[] {1, 2}));
        assertThrows(NoSuchElementException.class, queue::pollId);
        queue.offer(5, 5);
        queue.clear();
        assertEquals(0, queue.size());
        assertEquals(0, queue.evaluations());
        queue.offer(6, 6);
        assertEquals(6, queue.pollId());
    }
}
