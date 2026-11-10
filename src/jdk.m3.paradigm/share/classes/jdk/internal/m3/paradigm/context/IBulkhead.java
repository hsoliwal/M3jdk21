/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * IBulkhead - Bulkhead pattern for resource isolation (Microservice).
 *
 * <p>Isolates resources to prevent cascade failures.
 * Inspired by Resilience4j but pure Java 21 with virtual threads.
 *
 * <p>Types:
 * <pre>
 *   SEMAPHORE  → Limits concurrent calls (no thread isolation)
 *   THREADPOOL → Isolated thread pool for operations
 * </pre>
 *
 * <p>From Grand Java Patterns Proof:
 * <pre>
 *   Bulkhead is compartmentalization.
 *   Like ship bulkheads - isolate flooding.
 *   Maps to: resource partitioning, isolation.
 * </pre>
 *
 * @since Resilience Pattern Internalization - Apache 2.0
 */
@ISpec(
    value = "Bulkhead - isolates resources to prevent cascade failures",
    constraints = {
        "Semaphore bulkhead: limits concurrent calls",
        "ThreadPool bulkhead: isolated execution context",
        "Configurable max concurrent and wait time"
    },
    invariants = {
        "execute() blocks until permit acquired or timeout",
        "Metrics track concurrent calls and rejections",
        "Resources released on completion"
    },
    examples = {
        "IBulkhead.semaphore(10)",
        "IBulkhead.threadPool(5, 10)",
        "bulkhead.execute(() -> heavyOperation())"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/resilience/IBulkhead.java}
 * blob {@code 2da1dd7bc812837c2013a864cb7278619be0b7de} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IBulkhead {

    /**
     * Execute operation within bulkhead.
     *
     * @param <T> result type
     * @param operation operation to execute
     * @return result
     * @throws BulkheadFullException if bulkhead at capacity
     */
    <T> T execute(Supplier<T> operation);

    /**
     * Execute runnable within bulkhead.
     *
     * @param operation operation to execute
     */
    default void execute(Runnable operation) {
        execute(() -> {
            operation.run();
            return null; // TODO: FM-36 — Void Callable contract
        });
    }

    /**
     * Execute with timeout.
     *
     * @param <T> result type
     * @param operation operation to execute
     * @param timeout max wait for permit
     * @return result
     * @throws BulkheadFullException if timeout waiting for permit
     */
    <T> T execute(Supplier<T> operation, Duration timeout);

    /**
     * Get current concurrent call count.
     *
     * @return concurrent calls in progress
     */
    int concurrentCalls();

    /**
     * Get available permits.
     *
     * @return permits available
     */
    int availablePermits();

    /**
     * Get max concurrent calls.
     *
     * @return max concurrent calls
     */
    int maxConcurrent();

    /**
     * Get metrics.
     *
     * @return bulkhead metrics
     */
    Metrics metrics();

    /** Bulkhead metrics. */
    record Metrics(
            int maxConcurrent,
            int availablePermits,
            int concurrentCalls,
            long totalCalls,
            long rejectedCalls
    ) {}

    // --- EXCEPTION ---

    /** Exception when bulkhead at capacity. */
    @SuppressWarnings("serial") // LINT_SUPPRESS (declared, paradigm COP-2 2026-11-10): source-retention
    class BulkheadFullException extends RuntimeException {
        public BulkheadFullException(String message) {
            super(message);
        }
    }

    // --- FACTORY METHODS ---

    /**
     * Create semaphore bulkhead.
     *
     * @param maxConcurrent max concurrent calls
     * @return semaphore bulkhead
     */
    static IBulkhead semaphore(int maxConcurrent) {
        return new SemaphoreBulkhead(maxConcurrent, Duration.ofSeconds(30));
    }

    /**
     * Create semaphore bulkhead with wait timeout.
     *
     * @param maxConcurrent max concurrent calls
     * @param maxWait max time to wait for permit
     * @return semaphore bulkhead
     */
    static IBulkhead semaphore(int maxConcurrent, Duration maxWait) {
        return new SemaphoreBulkhead(maxConcurrent, maxWait);
    }

    /**
     * Create thread pool bulkhead (virtual threads).
     *
     * @param coreSize thread pool core size
     * @param maxSize thread pool max size
     * @return thread pool bulkhead
     */
    static IBulkhead threadPool(int coreSize, int maxSize) {
        return new ThreadPoolBulkhead(coreSize, maxSize, Duration.ofSeconds(30));
    }

    /**
     * Create virtual thread bulkhead.
     *
     * @param maxConcurrent max concurrent virtual threads
     * @return virtual thread bulkhead
     */
    static IBulkhead virtualThreads(int maxConcurrent) {
        return new VirtualThreadBulkhead(maxConcurrent, Duration.ofSeconds(30));
    }

    // --- IMPLEMENTATIONS ---

    /** Semaphore-based bulkhead. */
    class SemaphoreBulkhead implements IBulkhead {
        private final Semaphore semaphore;
        private final int maxConcurrent;
        private final Duration maxWait;
        private final AtomicInteger totalCalls = new AtomicInteger(0);
        private final AtomicInteger rejectedCalls = new AtomicInteger(0);

        SemaphoreBulkhead(int maxConcurrent, Duration maxWait) {
            this.maxConcurrent = maxConcurrent;
            this.maxWait = maxWait;
            this.semaphore = new Semaphore(maxConcurrent);
        }

        @Override
        public <T> T execute(Supplier<T> operation) {
            return execute(operation, maxWait);
        }

        @Override
        public <T> T execute(Supplier<T> operation, Duration timeout) {
            totalCalls.incrementAndGet();
            try {
                if (!semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    rejectedCalls.incrementAndGet();
                    throw new BulkheadFullException("Bulkhead full: no permit available");
                }
                try {
                    return operation.get();
                } finally {
                    semaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                rejectedCalls.incrementAndGet();
                throw new BulkheadFullException("Interrupted waiting for permit");
            }
        }

        @Override
        public int concurrentCalls() {
            return maxConcurrent - semaphore.availablePermits();
        }

        @Override
        public int availablePermits() {
            return semaphore.availablePermits();
        }

        @Override
        public int maxConcurrent() {
            return maxConcurrent;
        }

        @Override
        public Metrics metrics() {
            return new Metrics(
                    maxConcurrent,
                    semaphore.availablePermits(),
                    concurrentCalls(),
                    totalCalls.get(),
                    rejectedCalls.get()
            );
        }
    }

    /** Thread pool based bulkhead. */
    class ThreadPoolBulkhead implements IBulkhead {
        private final ExecutorService executor;
        private final int maxConcurrent;
        private final Duration maxWait;
        private final AtomicInteger concurrent = new AtomicInteger(0);
        private final AtomicInteger totalCalls = new AtomicInteger(0);
        private final AtomicInteger rejectedCalls = new AtomicInteger(0);

        ThreadPoolBulkhead(int coreSize, int maxSize, Duration maxWait) {
            this.maxConcurrent = maxSize;
            this.maxWait = maxWait;
            this.executor = new ThreadPoolExecutor(
                    coreSize, maxSize,
                    60L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(maxSize),
                    new ThreadPoolExecutor.AbortPolicy()
            );
        }

        @Override
        public <T> T execute(Supplier<T> operation) {
            return execute(operation, maxWait);
        }

        @Override
        public <T> T execute(Supplier<T> operation, Duration timeout) {
            totalCalls.incrementAndGet();
            try {
                concurrent.incrementAndGet();
                Future<T> future = executor.submit(operation::get);
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException e) {
                rejectedCalls.incrementAndGet();
                throw new BulkheadFullException("Thread pool bulkhead full");
            } catch (TimeoutException e) {
                rejectedCalls.incrementAndGet();
                throw new BulkheadFullException("Execution timeout");
            } catch (Exception e) {
                throw new RuntimeException("Bulkhead execution failed", e);
            } finally {
                concurrent.decrementAndGet();
            }
        }

        @Override
        public int concurrentCalls() {
            return concurrent.get();
        }

        @Override
        public int availablePermits() {
            return maxConcurrent - concurrent.get();
        }

        @Override
        public int maxConcurrent() {
            return maxConcurrent;
        }

        @Override
        public Metrics metrics() {
            return new Metrics(
                    maxConcurrent,
                    availablePermits(),
                    concurrent.get(),
                    totalCalls.get(),
                    rejectedCalls.get()
            );
        }
    }

    /** Virtual thread bulkhead. */
    class VirtualThreadBulkhead implements IBulkhead {
        private final Semaphore semaphore;
        private final int maxConcurrent;
        private final Duration maxWait;
        private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        private final AtomicInteger totalCalls = new AtomicInteger(0);
        private final AtomicInteger rejectedCalls = new AtomicInteger(0);

        VirtualThreadBulkhead(int maxConcurrent, Duration maxWait) {
            this.maxConcurrent = maxConcurrent;
            this.maxWait = maxWait;
            this.semaphore = new Semaphore(maxConcurrent);
        }

        @Override
        public <T> T execute(Supplier<T> operation) {
            return execute(operation, maxWait);
        }

        @Override
        public <T> T execute(Supplier<T> operation, Duration timeout) {
            totalCalls.incrementAndGet();
            try {
                if (!semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    rejectedCalls.incrementAndGet();
                    throw new BulkheadFullException("Virtual thread bulkhead full");
                }
                try {
                    Future<T> future = executor.submit(operation::get);
                    return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                } finally {
                    semaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                rejectedCalls.incrementAndGet();
                throw new BulkheadFullException("Interrupted");
            } catch (Exception e) {
                throw new RuntimeException("Bulkhead execution failed", e);
            }
        }

        @Override
        public int concurrentCalls() {
            return maxConcurrent - semaphore.availablePermits();
        }

        @Override
        public int availablePermits() {
            return semaphore.availablePermits();
        }

        @Override
        public int maxConcurrent() {
            return maxConcurrent;
        }

        @Override
        public Metrics metrics() {
            return new Metrics(
                    maxConcurrent,
                    semaphore.availablePermits(),
                    concurrentCalls(),
                    totalCalls.get(),
                    rejectedCalls.get()
            );
        }
    }
}
