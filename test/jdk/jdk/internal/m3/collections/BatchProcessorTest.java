/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0.
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BatchProcessorTest {

    @Test
    void rejectsUnboundedOrInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new BatchProcessor.Config(0, Duration.ofSeconds(1), 10));
        assertThrows(IllegalArgumentException.class, () -> new BatchProcessor.Config(2, Duration.ZERO, 10));
        assertThrows(IllegalArgumentException.class, () -> new BatchProcessor.Config(5, Duration.ofSeconds(1), 4));
    }

    @Test
    void flushesAtBatchSizeAndSuppliesImmutableBatch() {
        List<List<Integer>> batches = new ArrayList<>();
        BatchProcessor<Integer> processor = BatchProcessor.create(
                new BatchProcessor.Config(2, Duration.ofHours(1), 4),
                batch -> {
                    assertThrows(UnsupportedOperationException.class, () -> batch.add(99));
                    batches.add(batch);
                });

        processor.add(1);
        assertEquals(1, processor.pending());
        processor.add(2);

        assertEquals(List.of(List.of(1, 2)), batches);
        assertEquals(0, processor.pending());
        processor.close();
        assertTrue(processor.isClosed());
    }

    @Test
    void closeFlushesRemainingItemsExactlyOnce() {
        List<List<String>> batches = new ArrayList<>();
        BatchProcessor<String> processor = BatchProcessor.create(
                new BatchProcessor.Config(10, Duration.ofHours(1), 10),
                batches::add);

        processor.add("a");
        processor.add("b");
        processor.close();

        assertEquals(List.of(List.of("a", "b")), batches);
        assertEquals(0, processor.pending());
        assertThrows(IllegalStateException.class, () -> processor.add("late"));
    }

    @Test
    void timerFlushesANonEmptyQueue() throws Exception {
        CountDownLatch flushed = new CountDownLatch(1);
        List<List<String>> batches = new ArrayList<>();
        BatchProcessor<String> processor = BatchProcessor.create(
                new BatchProcessor.Config(10, Duration.ofMillis(25), 10),
                batch -> {
                    batches.add(batch);
                    flushed.countDown();
                });

        processor.add("timer");
        assertTrue(flushed.await(2, TimeUnit.SECONDS), "timed flush did not run");
        processor.close();

        assertEquals(List.of(List.of("timer")), batches);
        assertFalse(batches.isEmpty());
    }

    @Test
    void callbackFailureIsSurfacedAndPoisonsFurtherUse() {
        RuntimeException expected = new RuntimeException("sink failed");
        BatchProcessor<Integer> processor = BatchProcessor.create(
                new BatchProcessor.Config(1, Duration.ofHours(1), 2),
                ignored -> {
                    throw expected;
                });

        assertThrows(RuntimeException.class, () -> processor.add(1));
        IllegalStateException later = assertThrows(IllegalStateException.class, () -> processor.add(2));
        assertEquals(expected, later.getCause());
        IllegalStateException close = assertThrows(IllegalStateException.class, processor::close);
        assertEquals(expected, close.getCause());
    }
}
