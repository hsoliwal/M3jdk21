// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The bounded admission/result-window atom used by the parallel file owner.
 * Only the coordinator mutates ring geometry. Workers publish a first observed
 * failure through an atomic reference. Tasks/futures are required lifetime owners;
 * row/root association uses parallel arrays, not a retained record per entry.
 * The fixed-size future lane uses an ArrayList to retain generic type safety
 * without unchecked array casts; its backing is a single array.
 * Successful results are consumed in encounter order. A worker failure may stop
 * publication earlier than the failing task's ordinal. Cancellation is cooperative.
 */
final class M3FileTaskWindow<R> implements AutoCloseable {
    private static final int MAX_PARALLELISM = 256;
    private static final long CHECK_MILLIS = 25L;
    private final ExecutorService executor;
    private final java.util.ArrayList<Future<R>> futures;
    private final int[] rows;
    private final Path[] roots;
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private int head;
    private int size;
    private boolean closed;

    @FunctionalInterface
    interface Check {
        void run() throws IOException;
    }

    M3FileTaskWindow(int capacity, String threadName) {
        if (capacity < 1 || capacity > MAX_PARALLELISM) {
            throw new IllegalArgumentException("parallelism must be 1..256");
        }
        Objects.requireNonNull(threadName, "threadName");
        futures = new java.util.ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) futures.add(null);
        rows = new int[capacity];
        roots = new Path[capacity];
        executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name(threadName, 0).factory());
    }

    int size() { return size; }
    boolean full() { return size == futures.size(); }
    int row() { requireHead(); return rows[head]; }
    Path root() { requireHead(); return roots[head]; }

    void add(int row, Callable<R> work) throws IOException {
        submit(row, null, work);
    }

    void add(Path root, Callable<R> work) throws IOException {
        submit(-1, Objects.requireNonNull(root, "root"), work);
    }

    private void submit(int row, Path root, Callable<R> work) throws IOException {
        Objects.requireNonNull(work, "work");
        requireOpen();
        checkFailure();
        if (full()) throw new IllegalStateException("task window is full");
        int tail = (head + size) % futures.size();
        Future<R> future = executor.submit(() -> {
            try {
                return work.call();
            } catch (Exception | Error error) {
                failure.compareAndSet(null, error);
                throw error;
            }
        });
        rows[tail] = row;
        roots[tail] = root;
        futures.set(tail, future);
        size++;
    }

    R await(Check check) throws IOException {
        Objects.requireNonNull(check, "check");
        requireHead();
        try {
            for (;;) {
                check.run();
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException("M3 parallel work interrupted");
                }
                checkFailure();
                try {
                    R value = futures.get(head).get(CHECK_MILLIS, TimeUnit.MILLISECONDS);
                    check.run();
                    checkFailure();
                    return value;
                } catch (TimeoutException pending) {
                    // An out-of-order failure is checked even when the head is blocked.
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            InterruptedIOException error = new InterruptedIOException("M3 parallel work interrupted");
            error.initCause(interrupted);
            throw error;
        } catch (CancellationException cancelled) {
            InterruptedIOException error = new InterruptedIOException("M3 parallel work cancelled");
            error.initCause(cancelled);
            throw error;
        } catch (ExecutionException completed) {
            rethrow(completed.getCause());
            throw new AssertionError("unreachable");
        }
    }

    void remove() {
        requireHead();
        if (!futures.get(head).isDone()) throw new IllegalStateException("head task is not complete");
        futures.set(head, null);
        roots[head] = null;
        rows[head] = 0;
        head = (head + 1) % futures.size();
        size--;
    }

    void checkFailure() throws IOException {
        Throwable problem = failure.get();
        if (problem != null) rethrow(problem);
    }

    private static void rethrow(Throwable problem) throws IOException {
        if (problem instanceof IOException error) throw error;
        if (problem instanceof RuntimeException error) throw error;
        if (problem instanceof Error error) throw error;
        throw new IOException("M3 parallel source work failed", problem);
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("task window is closed");
    }

    private void requireHead() {
        requireOpen();
        if (size == 0) throw new IllegalStateException("task window is empty");
    }

    /** Cancels every admitted task before waiting for cooperative worker termination. */
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (size != 0) {
            for (Future<?> future : futures) {
                if (future != null) future.cancel(true);
            }
            executor.shutdownNow();
        }
        try {
            executor.close();
        } finally {
            java.util.Collections.fill(futures, null);
            java.util.Arrays.fill(roots, null);
            java.util.Arrays.fill(rows, 0);
            failure.set(null);
            size = 0;
        }
    }
}
