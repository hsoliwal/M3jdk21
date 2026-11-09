/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Dependency-light bounded in-memory batch accumulator.
 *
 * <p>Items are flushed when {@code batchSize} is reached or when {@code maxDelay} elapses. Flush
 * callbacks are serialized even when producers and the scheduler request a flush concurrently.
 * The callback receives an immutable list.</p>
 *
 * <p>This class is deliberately <strong>not durable</strong>: it owns no checkpoint, retry, replay,
 * transaction, distributed lock or delivery guarantee. If a callback throws, that failure is
 * recorded and surfaced by later operations. BatchProcessor does not automatically retry callback
 * failures because retrying an arbitrary callback could duplicate side effects. Durable jobs must
 * remain with Spring Batch/Camel/Kafka/PostgreSQL or another explicit durable owner.</p>
 *
 * @param <T> item type
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/BatchProcessor.java}
 * blob {@code 292944db8ed44f1ccb81a37905e06956b64f935e} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.lazy batch 1 (eligible core contracts) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class BatchProcessor<T> implements AutoCloseable {

    private static final String THREAD_NAME = "BatchProcessor";
    private static final String ERR_CLOSED = "BatchProcessor is closed";
    private static final String ERR_INTERRUPTED = "Interrupted while adding to batch";
    private static final int DEFAULT_QUEUE_CAPACITY = 10_000;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final int batchSize;
    private final Duration maxDelay;
    private final Consumer<List<T>> processor;
    private final BlockingQueue<T> queue;
    private final ScheduledExecutorService scheduler;
    private final ReentrantLock flushLock = new ReentrantLock();
    private final AtomicReference<RuntimeException> failure = new AtomicReference<>();
    private volatile boolean closed;

    /**
     * Bounded in-memory batch configuration.
     *
     * @param batchSize maximum items passed to one callback
     * @param maxDelay maximum delay before a non-empty queue is flushed
     * @param queueCapacity maximum number of pending items
     */
    public record Config(int batchSize, Duration maxDelay, int queueCapacity) {
        public Config {
            if (batchSize <= 0) {
                throw new IllegalArgumentException("batchSize must be > 0");
            }
            Objects.requireNonNull(maxDelay, "maxDelay");
            if (maxDelay.isZero() || maxDelay.isNegative()) {
                throw new IllegalArgumentException("maxDelay must be > 0");
            }
            try {
                if (maxDelay.toNanos() <= 0) {
                    throw new IllegalArgumentException("maxDelay must be at least one nanosecond");
                }
            } catch (ArithmeticException overflow) {
                throw new IllegalArgumentException("maxDelay is too large", overflow);
            }
            if (queueCapacity < batchSize) {
                throw new IllegalArgumentException("queueCapacity must be >= batchSize");
            }
        }

        /** Creates a config with one-second delay and the default bounded capacity. */
        public static Config of(int batchSize) {
            return new Config(batchSize, Duration.ofSeconds(1), DEFAULT_QUEUE_CAPACITY);
        }

        /** Creates a config with the default bounded capacity. */
        public static Config of(int batchSize, Duration maxDelay) {
            return new Config(batchSize, maxDelay, DEFAULT_QUEUE_CAPACITY);
        }
    }

    private BatchProcessor(Config config, Consumer<List<T>> processor) {
        Objects.requireNonNull(config, "config");
        this.batchSize = config.batchSize();
        this.maxDelay = config.maxDelay();
        this.processor = Objects.requireNonNull(processor, "processor");
        this.queue = new LinkedBlockingQueue<>(config.queueCapacity());
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
        long delayNanos = maxDelay.toNanos();
        scheduler.scheduleAtFixedRate(this::scheduledFlush, delayNanos, delayNanos, TimeUnit.NANOSECONDS);
    }

    /** Creates a bounded batch processor with explicit configuration. */
    public static <T> BatchProcessor<T> create(Config config, Consumer<List<T>> processor) {
        return new BatchProcessor<>(config, processor);
    }

    /** Creates a bounded batch processor with default delay/capacity. */
    public static <T> BatchProcessor<T> create(int batchSize, Consumer<List<T>> processor) {
        return new BatchProcessor<>(Config.of(batchSize), processor);
    }

    /**
     * Adds one non-null item, blocking only when the bounded queue is full.
     *
     * @throws IllegalStateException if closed or if a prior callback failed
     */
    public void add(T item) {
        requireOpen();
        throwIfFailed();
        Objects.requireNonNull(item, "item");
        try {
            queue.put(item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ERR_INTERRUPTED, e);
        }
        if (queue.size() >= batchSize) {
            flush();
        }
    }

    /**
     * Flushes at most {@code batchSize} pending items through the serialized callback.
     *
     * <p>If the callback throws, the exception is recorded and rethrown. The callback owns that
     * batch at invocation time; this in-memory primitive does not guess whether replay is safe.</p>
     */
    public void flush() {
        throwIfFailed();
        flushLock.lock();
        try {
            throwIfFailed();
            List<T> batch = new ArrayList<>(batchSize);
            queue.drainTo(batch, batchSize);
            if (batch.isEmpty()) {
                return;
            }
            try {
                processor.accept(List.copyOf(batch));
            } catch (RuntimeException e) {
                failure.compareAndSet(null, e);
                throw e;
            }
        } finally {
            flushLock.unlock();
        }
    }

    /** Returns the current number of pending items. */
    public int pending() {
        return queue.size();
    }

    /** Returns whether this processor has been closed. */
    public boolean isClosed() {
        return closed;
    }

    private void scheduledFlush() {
        if (closed || queue.isEmpty() || failure.get() != null) {
            return;
        }
        try {
            flush();
        } catch (RuntimeException e) {
            failure.compareAndSet(null, e);
            scheduler.shutdown();
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException(ERR_CLOSED);
        }
    }

    private void throwIfFailed() {
        RuntimeException recorded = failure.get();
        if (recorded != null) {
            throw new IllegalStateException("BatchProcessor callback previously failed", recorded);
        }
    }

    /**
     * Stops timed flushing and drains all remaining batches when no callback failure was recorded.
     *
     * @throws IllegalStateException when a prior callback failed
     */
    @Override
    public void close() {
        if (closed) {
            throwIfFailed();
            return;
        }
        closed = true;
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while closing BatchProcessor", e);
        }
        throwIfFailed();
        while (!queue.isEmpty()) {
            flush();
        }
        throwIfFailed();
    }
}
