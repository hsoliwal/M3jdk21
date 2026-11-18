/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.viewport;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Toolkit-neutral latest-request viewport scheduler. The worker must only read
 * immutable/model-owned CPU data; the UI executor is the sole owner of toolkit
 * objects and native resources. Neither executor is created or owned here.
 *
 * <p>At most one CPU drain and one UI publication are queued per scheduler;
 * rapid requests replace queued work, and obsolete computations are never
 * published. An in-flight computation should poll its cancellation token.
 * Snapshot ownership transfers to the consumer: producers must not modify a
 * submitted snapshot after returning it. No SWT/Swing type is referenced.</p>
 *
 * <p>The caller must ensure that {@code ui.execute} schedules on the toolkit's
 * event thread and never performs the callback on a CPU worker. SWT adapters
 * should use the existing Synchronizer (MODEL priority); Swing adapters use
 * EventQueue.invokeLater.</p>
 *
 * @param <R> immutable CPU request type
 * @param <S> immutable CPU result type
 */
public final class M3ViewportScheduler<R, S> implements AutoCloseable {

    @FunctionalInterface
    public interface Computer<R, S> {
        S compute(R request, Cancellation cancellation) throws Exception;
    }

    @FunctionalInterface
    public interface Cancellation {
        boolean cancelled();

        default void check() {
            if (cancelled()) throw new CancellationException("stale viewport request");
        }
    }

    /** Always schedules work on the toolkit's UI/main event thread. */
    @FunctionalInterface
    public interface UiExecutor {
        void execute(Runnable command);
    }

    /** Both callbacks run on the UI thread, never on a worker. */
    public interface Receiver<S> {
        void present(long generation, S snapshot);
        default void failed(long generation, Exception exception) {
            throw new IllegalStateException("viewport computation failed", exception);
        }
    }

    private record Job<R>(long generation, R request) {}
    private record Result<S>(long generation, S snapshot, Exception failure) {}

    private final Executor worker;
    private final UiExecutor ui;
    private final Computer<R, S> computer;
    private final Receiver<S> receiver;
    private final AtomicLong generation = new AtomicLong();
    private final AtomicReference<Job<R>> requested = new AtomicReference<>();
    private final AtomicReference<Result<S>> published = new AtomicReference<>();
    private final AtomicBoolean workerQueued = new AtomicBoolean();
    private final AtomicBoolean uiQueued = new AtomicBoolean();
    private volatile boolean closed;

    public M3ViewportScheduler(Executor worker, UiExecutor ui,
            Computer<R, S> computer, Receiver<S> receiver) {
        this.worker = Objects.requireNonNull(worker, "worker");
        this.ui = Objects.requireNonNull(ui, "ui");
        this.computer = Objects.requireNonNull(computer, "computer");
        this.receiver = Objects.requireNonNull(receiver, "receiver");
    }

    /**
     * Enqueues a CPU-only request, replacing any older queued request.
     * Call on the toolkit's event thread with a detached immutable model input.
     * Returns the monotonically increasing request generation.
     */
    public long request(R request) {
        Objects.requireNonNull(request, "request");
        if (closed) throw new IllegalStateException("viewport scheduler closed");
        long next = generation.incrementAndGet();
        Job<R> job = new Job<>(next, request);
        requested.accumulateAndGet(job,
                (previous, incoming) -> previous == null ||
                        previous.generation() < incoming.generation() ? incoming : previous);
        queueWorker();
        return next;
    }

    public long latestGeneration() {
        return generation.get();
    }

    private void queueWorker() {
        if (closed || !workerQueued.compareAndSet(false, true)) return;
        try {
            worker.execute(this::drain);
        } catch (RuntimeException failure) {
            workerQueued.set(false);
            throw failure;
        }
    }

    private void drain() {
        try {
            Job<R> job;
            while (!closed && (job = requested.getAndSet(null)) != null) {
                long version = job.generation();
                Cancellation cancellation = () -> closed ||
                        version != generation.get() || Thread.currentThread().isInterrupted();
                if (cancellation.cancelled()) continue;
                try {
                    S snapshot = Objects.requireNonNull(
                            computer.compute(job.request(), cancellation), "snapshot");
                    if (!cancellation.cancelled()) publish(new Result<>(version, snapshot, null));
                } catch (CancellationException ignored) {
                    // A more recent request superseded this computation.
                } catch (Exception failure) {
                    if (!cancellation.cancelled()) publish(new Result<>(version, null, failure));
                }
            }
        } finally {
            workerQueued.set(false);
            if (!closed && requested.get() != null) queueWorker();
        }
    }

    private void publish(Result<S> result) {
        published.accumulateAndGet(result,
                (previous, incoming) -> previous == null ||
                        previous.generation() < incoming.generation() ? incoming : previous);
        queueUi();
    }

    private void queueUi() {
        if (closed || !uiQueued.compareAndSet(false, true)) return;
        try {
            ui.execute(this::deliver);
        } catch (RuntimeException failure) {
            uiQueued.set(false);
            published.set(null);
            throw failure;
        }
    }

    private void deliver() {
        try {
            Result<S> result = published.getAndSet(null);
            if (closed || result == null || result.generation() != generation.get()) return;
            if (result.failure() == null) receiver.present(result.generation(), result.snapshot());
            else receiver.failed(result.generation(), result.failure());
        } finally {
            uiQueued.set(false);
            if (!closed && published.get() != null) queueUi();
        }
    }

    /** Prevents further work and prevents queued UI callbacks from presenting. */
    @Override
    public void close() {
        closed = true;
        generation.incrementAndGet();
        requested.set(null);
        published.set(null);
    }
}
