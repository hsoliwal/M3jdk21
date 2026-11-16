/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 */

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A stock-JDK-compatible scheduler whose old virtual methods and protected
 * decoration hooks must remain authoritative for new inherited defaults.
 * Compile this fixture against an unmodified Java 21 JDK, then reuse its bytes.
 * It deliberately mentions no M3JDK progress type or method.
 */
public final class M3LegacyDecoratingScheduler extends ScheduledThreadPoolExecutor {
    final AtomicInteger submissions = new AtomicInteger();
    final AtomicInteger runnableDecorations = new AtomicInteger();
    final AtomicInteger callableDecorations = new AtomicInteger();
    final AtomicInteger beforeHooks = new AtomicInteger();
    final AtomicInteger afterHooks = new AtomicInteger();
    final AtomicReference<Object> unexpectedWorkerContext = new AtomicReference<>();
    volatile String lastKind;
    volatile long lastInitialDelay;
    volatile long lastInterval;
    volatile TimeUnit lastUnit;
    volatile ScheduledFuture<?> lastFuture;
    volatile RunnableScheduledFuture<?> lastDecoration;
    volatile RuntimeException forcedFailure;
    volatile Supplier<?> workerContext;
    volatile Object expectedWorkerContext;

    public M3LegacyDecoratingScheduler(ThreadFactory factory) {
        super(1, factory);
    }

    private void submitted(String kind, long initialDelay, long interval, TimeUnit unit) {
        submissions.incrementAndGet();
        lastKind = kind;
        lastInitialDelay = initialDelay;
        lastInterval = interval;
        lastUnit = unit;
        if (forcedFailure != null) throw forcedFailure;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        submitted("runnable", delay, 0, unit);
        ScheduledFuture<?> future = super.schedule(command, delay, unit);
        lastFuture = future;
        return future;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> command, long delay, TimeUnit unit) {
        submitted("callable", delay, 0, unit);
        ScheduledFuture<V> future = super.schedule(command, delay, unit);
        lastFuture = future;
        return future;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
            Runnable command, long initialDelay, long period, TimeUnit unit) {
        submitted("rate", initialDelay, period, unit);
        ScheduledFuture<?> future = super.scheduleAtFixedRate(command, initialDelay, period, unit);
        lastFuture = future;
        return future;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable command, long initialDelay, long delay, TimeUnit unit) {
        submitted("delay", initialDelay, delay, unit);
        ScheduledFuture<?> future = super.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        lastFuture = future;
        return future;
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(
            Runnable runnable, RunnableScheduledFuture<V> task) {
        runnableDecorations.incrementAndGet();
        RunnableScheduledFuture<V> decorated = new DecoratedTask<>(task);
        lastDecoration = decorated;
        return decorated;
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(
            Callable<V> callable, RunnableScheduledFuture<V> task) {
        callableDecorations.incrementAndGet();
        RunnableScheduledFuture<V> decorated = new DecoratedTask<>(task);
        lastDecoration = decorated;
        return decorated;
    }

    private void inspectWorkerContext() {
        Supplier<?> supplier = workerContext;
        if (supplier == null) return;
        Object actual = supplier.get();
        if (actual != expectedWorkerContext) {
            unexpectedWorkerContext.compareAndSet(null, actual);
        }
    }

    @Override
    protected void beforeExecute(Thread thread, Runnable task) {
        inspectWorkerContext();
        beforeHooks.incrementAndGet();
        super.beforeExecute(thread, task);
    }

    @Override
    protected void afterExecute(Runnable task, Throwable failure) {
        super.afterExecute(task, failure);
        inspectWorkerContext();
        afterHooks.incrementAndGet();
    }

    static final class DecoratedTask<V> implements RunnableScheduledFuture<V> {
        private final RunnableScheduledFuture<V> delegate;

        DecoratedTask(RunnableScheduledFuture<V> delegate) { this.delegate = delegate; }
        @Override public boolean isPeriodic() { return delegate.isPeriodic(); }
        @Override public long getDelay(TimeUnit unit) { return delegate.getDelay(unit); }
        @Override public int compareTo(Delayed other) {
            return delegate.compareTo(other instanceof DecoratedTask<?> task ? task.delegate : other);
        }
        @Override public void run() { delegate.run(); }
        @Override public boolean cancel(boolean interrupt) { return delegate.cancel(interrupt); }
        @Override public boolean isCancelled() { return delegate.isCancelled(); }
        @Override public boolean isDone() { return delegate.isDone(); }
        @Override public V get() throws InterruptedException, ExecutionException { return delegate.get(); }
        @Override public V get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.get(timeout, unit);
        }
    }
}
