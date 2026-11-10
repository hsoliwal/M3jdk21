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

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * IVirtualExecutor - JDK 21 Virtual Threads made simple.
 *
 * <h2>Why Virtual Threads?</h2>
 * <pre>
 * Platform Threads:  ~2MB stack each, OS-scheduled, expensive
 * Virtual Threads:   ~1KB each, JVM-scheduled, cheap (millions possible)
 *
 * Virtual threads are ideal for:
 *   - I/O-bound tasks (file, network, database)
 *   - High-concurrency scenarios
 *   - Blocking code that looks synchronous
 * </pre>
 *
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  VIRTUAL EXECUTOR PATTERN                                               │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  IVirtualExecutor                                                       │
 * │    │                                                                    │
 * │    ├── run(Runnable)              - Fire and forget                     │
 * │    ├── submit(Callable)           - Get Future                          │
 * │    ├── supplyAsync(Supplier)      - CompletableFuture                   │
 * │    ├── runAsync(Runnable)         - CompletableFuture&lt;Void&gt;           │
 * │    ├── parallel(tasks)            - Run all in parallel                 │
 * │    ├── parallelMap(items, fn)     - Map items in parallel               │
 * │    └── withTimeout(duration)      - Scoped timeout                      │
 * │                                                                         │
 * │  JDK 21 APIs Used:                                                      │
 * │    Thread.ofVirtual()             - Virtual thread factory              │
 * │    Executors.newVirtualThreadPerTaskExecutor()                          │
 * │    StructuredTaskScope            - Structured concurrency              │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Simple async execution
 * IVirtualExecutor.run(() -> processFile(file));
 *
 * // Get result
 * CompletableFuture<String> future = IVirtualExecutor.supplyAsync(() -> fetchData(url));
 *
 * // Parallel processing
 * List<Result> results = IVirtualExecutor.parallelMap(urls, url -> fetch(url));
 *
 * // With timeout
 * IVirtualExecutor.withTimeout(Duration.ofSeconds(5))
 *     .supplyAsync(() -> slowOperation());
 *
 * // Structured concurrency (all-or-nothing)
 * record UserData(User user, List<Order> orders, List<Review> reviews) {}
 *
 * UserData data = IVirtualExecutor.structured(scope -> {
 *     var userFuture = scope.fork(() -> fetchUser(id));
 *     var ordersFuture = scope.fork(() -> fetchOrders(id));
 *     var reviewsFuture = scope.fork(() -> fetchReviews(id));
 *     scope.join();
 *     return new UserData(userFuture.get(), ordersFuture.get(), reviewsFuture.get());
 * });
 * }</pre>
 *
 * @since JDK 21 / ContextOS 1.0
 */
@ISpec(
    value = "JDK 21 Virtual Threads - lightweight concurrency for I/O-bound tasks",
    constraints = {
        "Virtual threads: ~1KB each (vs ~2MB for platform threads)",
        "JVM-scheduled, ideal for I/O-bound tasks",
        "Blocking code that looks synchronous",
        "Millions of concurrent tasks possible"
    },
    invariants = {
        "run() starts task on virtual thread",
        "supplyAsync() returns CompletableFuture",
        "parallelMap() maintains result order"
    },
    examples = {
        "IVirtualExecutor.run(() -> processFile(file))",
        "IVirtualExecutor.parallelMap(urls, url -> fetch(url))",
        "IVirtualExecutor.withTimeout(Duration.ofSeconds(5)).supplyAsync(...)"
    },
    model = Model.ModelType.OPUS,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/concurrency/IVirtualExecutor.java}
 * blob {@code f844d424cd319a2b844fd16cdd67a3260e1d00af} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IVirtualExecutor {

    // --- SIMPLE EXECUTION ---

    /**
     * Run task on virtual thread (fire and forget).
     *
     * @param task the task
     */
    static void run(Runnable task) {
        Thread.ofVirtual().start(task);
    }

    /**
     * Run task with name on virtual thread.
     *
     * @param name thread name
     * @param task the task
     */
    static void run(String name, Runnable task) {
        Thread.ofVirtual().name(name).start(task);
    }

    /**
     * Submit callable on virtual thread.
     *
     * @param task the callable
     * @param <T> result type
     * @return future with result
     */
    static <T> Future<T> submit(Callable<T> task) {
        return virtualExecutor().submit(task);
    }

    // --- COMPLETABLE FUTURE STYLE ---

    /**
     * Supply value asynchronously on virtual thread.
     *
     * @param supplier the supplier
     * @param <T> result type
     * @return completable future
     */
    static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, virtualExecutor());
    }

    /**
     * Run action asynchronously on virtual thread.
     *
     * @param action the action
     * @return completable future
     */
    static CompletableFuture<Void> runAsync(Runnable action) {
        return CompletableFuture.runAsync(action, virtualExecutor());
    }

    // --- PARALLEL PROCESSING ---

    /**
     * Run all tasks in parallel on virtual threads.
     *
     * @param tasks the tasks
     * @param <T> result type
     * @return list of results in same order
     */
    static <T> List<T> parallel(Collection<Callable<T>> tasks) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return executor.invokeAll(tasks).stream()
                    .map(IVirtualExecutor::getUnchecked)
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel execution interrupted", e);
        }
    }

    /**
     * Map items in parallel on virtual threads.
     *
     * @param items the items to process
     * @param mapper the mapping function
     * @param <T> input type
     * @param <R> result type
     * @return list of results in same order
     */
    static <T, R> List<R> parallelMap(Collection<T> items, Function<T, R> mapper) {
        List<Callable<R>> tasks = items.stream()
                .<Callable<R>>map(item -> () -> mapper.apply(item))
                .toList();
        return parallel(tasks);
    }

    /**
     * Map stream items in parallel on virtual threads.
     *
     * @param items the items stream
     * @param mapper the mapping function
     * @param <T> input type
     * @param <R> result type
     * @return stream of results
     */
    static <T, R> Stream<R> parallelStream(Stream<T> items, Function<T, R> mapper) {
        List<T> list = items.toList();
        return parallelMap(list, mapper).stream();
    }

    // --- TIMEOUT SUPPORT ---

    /**
     * Create executor with timeout.
     *
     * @param timeout the timeout duration
     * @return scoped executor
     */
    static ScopedExecutor withTimeout(Duration timeout) {
        return new ScopedExecutor(timeout);
    }

    /** Scoped executor with timeout. */
    class ScopedExecutor {
        private final Duration timeout;

        ScopedExecutor(Duration timeout) {
            this.timeout = timeout;
        }

        /**
         * Supply value with timeout.
         *
         * @param supplier the supplier
         * @param <T> result type
         * @return completable future
         */
        public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
            return IVirtualExecutor.supplyAsync(supplier)
                    .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        /**
         * Run action with timeout.
         *
         * @param action the action
         * @return completable future
         */
        public CompletableFuture<Void> runAsync(Runnable action) {
            return IVirtualExecutor.runAsync(action)
                    .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        /**
         * Run all tasks with timeout.
         *
         * @param tasks the tasks
         * @param <T> result type
         * @return list of results
         * @throws TimeoutException if timeout exceeded
         */
        public <T> List<T> parallel(Collection<Callable<T>> tasks) throws TimeoutException {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                return executor.invokeAll(tasks, timeout.toMillis(), TimeUnit.MILLISECONDS)
                        .stream()
                        .map(IVirtualExecutor::getUnchecked)
                        .toList();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Parallel execution interrupted", e);
            }
        }
    }

    // --- STRUCTURED CONCURRENCY (Preview in JDK 21) ---

    /**
     * Scope for structured concurrency.
     * Wraps StructuredTaskScope for simpler API.
     */
    interface IScope extends AutoCloseable {
        /**
         * Fork a subtask.
         *
         * @param task the task
         * @param <T> result type
         * @return subtask handle
         */
        <T> Subtask<T> fork(Callable<T> task);

        /**
         * Wait for all subtasks.
         *
         * @throws InterruptedException if interrupted
         */
        void join() throws InterruptedException;

        /**
         * Wait for all subtasks with timeout.
         *
         * @param timeout the timeout
         * @throws InterruptedException if interrupted
         * @throws TimeoutException if timeout exceeded
         */
        void joinUntil(Duration timeout) throws InterruptedException, TimeoutException;

        @Override
        void close();
    }

    /**
     * Subtask handle.
     *
     * @param <T> result type
     */
    interface Subtask<T> {
        /**
         * Get result (blocks if not complete).
         *
         * @return the result
         */
        T get();

        /**
         * Check if completed successfully.
         *
         * @return true if successful
         */
        boolean isSuccess();

        /**
         * Get exception if failed.
         *
         * @return the exception or null
         */
        Throwable exception();
    }

    // --- UTILITIES ---

    /**
     * Get shared virtual thread executor.
     *
     * @return virtual thread executor
     */
    static ExecutorService virtualExecutor() {
        return VirtualExecutorHolder.INSTANCE;
    }

    /** Get result from future, converting checked exceptions. */
    private static <T> T getUnchecked(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Task failed", e.getCause());
        } catch (CancellationException e) {
            throw new RuntimeException("Task cancelled", e);
        }
    }

    /** Holder for shared virtual executor (lazy initialization). */
    class VirtualExecutorHolder {
        static final ExecutorService INSTANCE = Executors.newVirtualThreadPerTaskExecutor();

        static {
            // Shutdown hook for clean exit
            Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
                INSTANCE.shutdown();
                try {
                    INSTANCE.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
    }
}
