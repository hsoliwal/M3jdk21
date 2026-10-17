// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * An asynchronous TERMINAL operation, not a promise that arbitrary escaped async callbacks are safe.
 * requestCancellation only requests cooperative stopping. Completion is published after the terminal
 * operation drains its workers. No Future.cancel shortcut, interruption, automatic retry or rollback.
 * Coordinator and worker pools are borrowed and must stay live until completion.
 */
public final class M3Execution<R> {
    private final CompletableFuture<R> result = new CompletableFuture<>();
    private final AtomicBoolean cancellation = new AtomicBoolean();
    private boolean finished;
    private M3Execution() { }
    static <R> M3Execution<R> submit(Executor coordinator, Function<BooleanSupplier, R> work) {
        Objects.requireNonNull(coordinator); Objects.requireNonNull(work);
        M3Execution<R> execution = new M3Execution<>();
        // Rejected submission propagates synchronously; no unusable handle is returned.
        coordinator.execute(() -> {
            R value = null; Throwable failure = null;
            try {
                if (execution.cancellation.get()) throw new CancellationException("execution cancelled before start");
                value = work.apply(execution.cancellation::get);
            } catch (Throwable error) { failure = error; }
            execution.finish(value, failure);
        });
        return execution;
    }
    /** True only for the first accepted request before terminal completion was committed. */
    public synchronized boolean requestCancellation() {
        return !finished && cancellation.compareAndSet(false, true);
    }
    /** Read-only stage: completing/cancelling its toCompletableFuture copy cannot alter this execution. */
    public CompletionStage<R> completion() { return result.minimalCompletionStage(); }
    public boolean cancellationRequested() { return cancellation.get(); }
    private void finish(R value, Throwable failure) {
        synchronized (this) {
            finished = true;
            if (failure == null && cancellation.get()) failure = new CancellationException("execution cancelled");
        }
        if (failure == null) result.complete(value); else result.completeExceptionally(failure);
    }
}
