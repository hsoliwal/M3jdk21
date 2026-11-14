/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Stock-JDK-compatible executor fixture. Compile this file with an unmodified
 * JDK and retain its class bytes to exercise inherited default-method dispatch.
 * This file deliberately mentions no M3JDK progress API.
 */
public final class M3LegacyExecutor extends AbstractExecutorService {
    int submissions;
    int executions;
    Future<?> lastFuture;
    Object lastResult;
    final RejectedExecutionException rejection =
            new RejectedExecutionException("legacy executor rejection");
    private boolean stopped;

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        submissions++;
        lastResult = result;
        FutureTask<T> future = new FutureTask<>(task, result);
        lastFuture = future;
        execute(future);
        return future;
    }

    @Override
    public void execute(Runnable command) {
        if (stopped) throw rejection;
        executions++;
        command.run();
    }

    @Override
    public void shutdown() { stopped = true; }

    @Override
    public List<Runnable> shutdownNow() {
        stopped = true;
        return List.of();
    }

    @Override
    public boolean isShutdown() { return stopped; }

    @Override
    public boolean isTerminated() { return stopped; }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return stopped;
    }
}
