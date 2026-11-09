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
package jdk.internal.m3.collections;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
/**
 * Thread and concurrency utilities.
 *
 * <p>
 * Provides:
 * <ul>
 * <li>Safe sleep operations</li>
 * <li>Virtual thread execution (Java 21)</li>
 * <li>Timeout operations</li>
 * <li>Thread naming and info</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * import static jdk.internal.m3.collections.ThreadOps.*;
 *
 * // Safe sleep
 * if (!sleepMs(100)) return; // interrupted
 *
 * // Virtual thread
 * runVirtual(() -> heavyTask());
 *
 * // With timeout
 * String result = withTimeout(() -> slowOperation(), Duration.ofSeconds(5), "timeout");
 * }</pre>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/ThreadOps.java}
 * blob {@code 51e0d8450e7f3d8786ff7c28e77bf3b8b5932d8e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ThreadOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ThreadOps.class);


    private ThreadOps() {}

    // --- SAFE SLEEP ---

    /** Sleep for duration, return false if interrupted. */
    public static boolean sleep(Duration duration) {
        try {
            Thread.sleep(duration);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Sleep for milliseconds, return false if interrupted. */
    public static boolean sleepMs(long millis) {
        return sleep(Duration.ofMillis(millis));
    }

    /** Sleep for seconds, return false if interrupted. */
    public static boolean sleepSec(long seconds) {
        return sleep(Duration.ofSeconds(seconds));
    }

    /** Sleep for nanoseconds, return false if interrupted. */
    public static boolean sleepNanos(long nanos) {
        try {
            Thread.sleep(Duration.ofNanos(nanos));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Sleep quietly (ignore interrupts). */
    public static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            log.debug("Interrupted", ignored);
        }
    }

    // --- VIRTUAL THREADS (Java 21) ---

    /** Run task on virtual thread. */
    public static Thread runVirtual(Runnable task) {
        return Thread.ofVirtual().start(task);
    }

    /** Run task on virtual thread with name. */
    public static Thread runVirtual(String name, Runnable task) {
        return Thread.ofVirtual().name(name).start(task);
    }

    /** Run task on platform thread. */
    public static Thread runPlatform(Runnable task) {
        return Thread.ofPlatform().start(task);
    }

    /** Submit task to virtual thread executor, get future. */
    public static <T> Future<T> submitVirtual(Callable<T> task) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return executor.submit(task);
        }
    }

    /** Execute all tasks on virtual threads, return results. */
    public static <T> List<T> executeAllVirtual(List<Callable<T>> tasks) throws InterruptedException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return executor.invokeAll(tasks).stream()
                    .map(f -> {
                        try {
                            return f.get();
                        } catch (Exception e) {
                            return null; // TODO: FM-36 — API contract (lambda, Future.get error sentinel)
                        }
                    })
                    .toList();
        }
    }

    // --- TIMEOUT OPERATIONS ---

    /** Execute with timeout, return default if timeout exceeded. */
    public static <T> T withTimeout(Supplier<T> supplier, Duration timeout, T defaultValue) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<T> future = executor.submit(supplier::get);
            try {
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                return defaultValue;
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    /** Execute with timeout, throw on timeout. */
    public static <T> T withTimeoutOrThrow(Callable<T> callable, Duration timeout) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<T> future = executor.submit(callable);
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** Run with timeout, return success status. */
    public static boolean runWithTimeout(Runnable task, Duration timeout) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> future = executor.submit(task);
            try {
                future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                return true;
            } catch (TimeoutException e) {
                future.cancel(true);
                return false;
            } catch (Exception e) {
                return false;
            }
        }
    }

    // --- THREAD INFO ---

    /** Get current thread name. */
    public static String currentName() {
        return Thread.currentThread().getName();
    }

    /** Get current thread ID. */
    public static long currentId() {
        return Thread.currentThread().threadId();
    }

    /** Check if current thread is virtual. */
    public static boolean isVirtual() {
        return Thread.currentThread().isVirtual();
    }

    /** Check if current thread is interrupted. */
    public static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    /** Interrupt current thread. */
    public static void interrupt() {
        Thread.currentThread().interrupt();
    }

    // --- THREAD NAMING ---

    /** Set current thread name. */
    public static void setName(String name) {
        Thread.currentThread().setName(name);
    }

    /** Execute with temporary thread name. */
    public static void withName(String name, Runnable task) {
        String original = currentName();
        try {
            setName(name);
            task.run();
        } finally {
            setName(original);
        }
    }

    /** Execute with temporary thread name, return result. */
    public static <T> T withName(String name, Supplier<T> supplier) {
        String original = currentName();
        try {
            setName(name);
            return supplier.get();
        } finally {
            setName(original);
        }
    }

    // --- UTILITY METHODS ---

    /** Yield current thread. */
    public static void yield() {
        Thread.yield();
    }

    /** Join thread with timeout. */
    public static boolean join(Thread thread, Duration timeout) {
        try {
            thread.join(timeout);
            return !thread.isAlive();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Join thread indefinitely. */
    public static boolean join(Thread thread) {
        try {
            thread.join();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Get available processors. */
    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /** Create thread factory with name prefix. */
    public static ThreadFactory namedFactory(String prefix) {
        return new ThreadFactory() {

            private int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                return Thread.ofPlatform().name(prefix + "-" + (count++)).unstarted(r);
            }
        };
    }

    /** Create virtual thread factory with name prefix. */
    public static ThreadFactory virtualFactory(String prefix) {
        return Thread.ofVirtual().name(prefix, 0).factory();
    }

    /**
     * Command-line interface for ThreadOps utilities.
     *
     * <p>
     * Provides interactive access to thread and concurrency operations including
     * sleep, virtual threads, and timeout operations.
     *
     * <h4>Usage</h4>
     *
     * <pre>{@code
     * java jdk.internal.m3.collections.ThreadOps list
     * java jdk.internal.m3.collections.ThreadOps sleep 1000
     * java jdk.internal.m3.collections.ThreadOps virtual
     * java jdk.internal.m3.collections.ThreadOps info
     * java jdk.internal.m3.collections.ThreadOps timeout 2000
     * }</pre>
     *
     * @param args
     *            command-line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1); // FM-40: acceptable in CLI main()
        }

        try {
            String command = args[0].toLowerCase();
            switch (command) {
                case "list" -> printOperations();
                case "sleep" -> {
                    if (args.length < 2) {
                        log.error("sleep requires duration in milliseconds");
                        System.exit(1);
                    }
                    long millis = Long.parseLong(args[1]);
                    log.info("Sleeping for {}ms...", millis);
                    boolean success = sleepMs(millis);
                    log.info(success ? "Sleep completed" : "Sleep interrupted");
                }
                case "virtual" -> {
                    log.info("Starting 5 virtual threads...");
                    CountDownLatch latch = new CountDownLatch(5);
                    java.util.stream.IntStream.rangeClosed(1, 5).forEach(taskId ->
                            runVirtual("worker-" + taskId, () -> {
                                log.info("Virtual thread {} running on: {}", taskId, currentName());
                                sleepMs(100);
                                log.info("Virtual thread {} completed", taskId);
                                latch.countDown();
                            }));
                    latch.await();
                    log.info("All virtual threads completed");
                }
                case "info" -> {
                    log.info("Thread Information:");
                    log.info("  Current thread name: {}", currentName());
                    log.info("  Current thread ID: {}", currentId());
                    log.info("  Is virtual: {}", isVirtual());
                    log.info("  Available processors: {}", availableProcessors());
                }
                case "timeout" -> {
                    if (args.length < 2) {
                        log.error("timeout requires duration in milliseconds");
                        System.exit(1);
                    }
                    long timeout = Long.parseLong(args[1]);
                    log.info("Running task with {}ms timeout...", timeout);
                    String result = withTimeout(
                            () -> {
                                sleepMs(timeout * 2);
                                return "Completed";
                            },
                            Duration.ofMillis(timeout),
                            "Timeout!");
                    log.info("Result: {}", result);
                }
                case "processors" -> {
                    int count = availableProcessors();
                    log.info("Available processors: {}", count);
                }
                default -> {
                    log.error("Unknown command: {}", command);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            log.error("Error executing command", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        log.info("Usage: java jdk.internal.m3.collections.ThreadOps <command> [args...]");

        log.info("Commands:");
        log.info("  list                - List all available operations");
        log.info("  sleep <millis>      - Sleep for specified milliseconds");
        log.info("  virtual             - Demonstrate virtual threads");
        log.info("  info                - Show current thread information");
        log.info("  timeout <millis>    - Demonstrate timeout operation");
        log.info("  processors          - Show available processors");
    }

    private static void printOperations() {
        log.info("ThreadOps - Thread and concurrency utilities");

        log.info("Safe Sleep:");
        log.info("  sleep(Duration)     - Sleep, return false if interrupted");
        log.info("  sleepMs(long)       - Sleep milliseconds");
        log.info("  sleepSec(long)      - Sleep seconds");
        log.info("  sleepNanos(long)    - Sleep nanoseconds");
        log.info("  sleepQuietly(Duration) - Sleep, ignore interrupts");

        log.info("Virtual Threads (Java 21):");
        log.info("  runVirtual(Runnable) - Run task on virtual thread");
        log.info("  runVirtual(name, Runnable) - Named virtual thread");
        log.info("  runPlatform(Runnable) - Run on platform thread");
        log.info("  submitVirtual(Callable) - Submit task, get future");
        log.info("  executeAllVirtual(tasks) - Execute all tasks");

        log.info("Timeout Operations:");
        log.info("  withTimeout(Supplier, Duration, default) - Execute with timeout");
        log.info("  withTimeoutOrThrow(Callable, Duration) - Throw on timeout");
        log.info("  runWithTimeout(Runnable, Duration) - Run with timeout");

        log.info("Thread Info:");
        log.info("  currentName()       - Get current thread name");
        log.info("  currentId()         - Get current thread ID");
        log.info("  isVirtual()         - Check if virtual thread");
        log.info("  isInterrupted()     - Check if interrupted");
        log.info("  interrupt()         - Interrupt current thread");

        log.info("Thread Naming:");
        log.info("  setName(String)     - Set current thread name");
        log.info("  withName(name, Runnable) - Execute with temp name");
        log.info("  withName(name, Supplier) - Execute with temp name");

        log.info("Utility Methods:");
        log.info("  yield()             - Yield current thread");
        log.info("  join(Thread)        - Join thread indefinitely");
        log.info("  join(Thread, Duration) - Join with timeout");
        log.info("  availableProcessors() - Get processor count");
        log.info("  namedFactory(prefix) - Create named thread factory");
        log.info("  virtualFactory(prefix) - Create virtual thread factory");
    }
}
