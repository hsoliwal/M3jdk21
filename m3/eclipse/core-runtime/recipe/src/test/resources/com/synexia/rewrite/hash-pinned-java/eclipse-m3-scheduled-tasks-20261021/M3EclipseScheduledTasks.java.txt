// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.runtime;

import com.m3.collections.M3ProgressMonitor;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.IStatus;

/**
 * Java 21 Runnable/Callable scheduling with one existing M3 progress owner.
 *
 * <p>Executors own their queues, scheduling policies and shutdown. M3 owns only the supplied
 * progress monitor. Each submission requires an exclusive, single-use owner: it is begun only
 * after the task starts and completed once on exit. Cancellation propagates to that owner only
 * when the original Future accepts cancellation. Rejection never begins progress.
 *
 * <p>Ordinary {@code Callable<V>} failures retain the standard Future.get/ExecutionException
 * contract; Eclipse status tasks retain {@link M3EclipseJobs#runNow} status semantics. This is
 * not an Eclipse JobManager replacement: scheduling rules, job families, priority, UI dispatch
 * and listener notification must still use {@link M3EclipseJobScheduler}. No user callback
 * or Eclipse lifecycle operation crosses JNI.
 */
public final class M3EclipseScheduledTasks {
    private M3EclipseScheduledTasks() {}

    /** Submit an ordinary Callable without introducing a second task-state owner. */
    public static <V> Future<V> submit(
            ExecutorService executor,
            M3ProgressMonitor owner,
            String taskName,
            long totalWork,
            Callable<V> callable) {
        Objects.requireNonNull(executor, "executor");
        checked(owner, taskName, totalWork, callable);
        FutureTask<V> future = new FutureTask<>(() -> invoke(owner, taskName, totalWork, callable)) {
            @Override
            public boolean cancel(boolean interrupt) {
                if (!super.cancel(interrupt)) return false;
                owner.setCanceled(true);
                return true;
            }
        };
        executor.execute(future);
        return future;
    }

    /** Submit Runnable using the same Future/cancellation and progress contract. */
    public static Future<Void> submit(
            ExecutorService executor,
            M3ProgressMonitor owner,
            String taskName,
            long totalWork,
            Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return submit(executor, owner, taskName, totalWork, Executors.callable(runnable, null));
    }

    /**
     * Schedule a Callable with standard ScheduledExecutorService delay semantics.
     * A negative delay is left to the executor, which treats it as immediate.
     */
    public static <V> ScheduledFuture<V> schedule(
            ScheduledExecutorService executor,
            M3ProgressMonitor owner,
            String taskName,
            long totalWork,
            Callable<V> callable,
            long delay,
            TimeUnit unit) {
        Objects.requireNonNull(executor, "executor");
        checked(owner, taskName, totalWork, callable);
        Objects.requireNonNull(unit, "unit");
        ScheduledFuture<V> original =
                executor.schedule(() -> invoke(owner, taskName, totalWork, callable), delay, unit);
        return new CancelAwareScheduledFuture<>(original, owner);
    }

    /** Schedule Runnable, preserving the ScheduledFuture null-result contract. */
    public static ScheduledFuture<Void> schedule(
            ScheduledExecutorService executor,
            M3ProgressMonitor owner,
            String taskName,
            long totalWork,
            Runnable runnable,
            long delay,
            TimeUnit unit) {
        Objects.requireNonNull(runnable, "runnable");
        return schedule(
                executor,
                owner,
                taskName,
                totalWork,
                Executors.callable(runnable, null),
                delay,
                unit);
    }

    /**
     * Schedule an Eclipse monitored task without changing IStatus error/cancel conversion.
     * This delegates completion semantics to the existing M3EclipseJobs owner.
     */
    public static ScheduledFuture<IStatus> scheduleStatus(
            ScheduledExecutorService executor,
            M3ProgressMonitor owner,
            String taskName,
            long totalWork,
            M3EclipseJobs.Task task,
            long delay,
            TimeUnit unit) {
        Objects.requireNonNull(executor, "executor");
        checked(owner, taskName, totalWork, task);
        Objects.requireNonNull(unit, "unit");
        ScheduledFuture<IStatus> original =
                executor.schedule(
                        () -> M3EclipseJobs.runNow(owner, taskName, totalWork, task),
                        delay,
                        unit);
        return new CancelAwareScheduledFuture<>(original, owner);
    }

    private static void checked(
            M3ProgressMonitor owner,
            String taskName,
            long totalWork,
            Object action) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(taskName, "taskName");
        Objects.requireNonNull(action, "action");
        if (totalWork < M3ProgressMonitor.UNKNOWN) {
            throw new IllegalArgumentException("totalWork below UNKNOWN");
        }
    }

    private static <V> V invoke(
            M3ProgressMonitor owner,
            String taskName,
            long totalWork,
            Callable<V> callable) throws Exception {
        owner.beginTask(taskName, totalWork);
        try {
            if (owner.isCanceled() || Thread.currentThread().isInterrupted()) {
                throw new CancellationException("M3 task canceled");
            }
            return callable.call();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            owner.setCanceled(true);
            throw interrupted;
        } catch (CancellationException canceled) {
            owner.setCanceled(true);
            throw canceled;
        } finally {
            owner.done();
        }
    }

    /**
     * One facade over the executor's original scheduled future; no separate completion state.
     * Cancellation changes M3 state only if delegate cancellation actually succeeds.
     */
    private static final class CancelAwareScheduledFuture<V> implements ScheduledFuture<V> {
        private final ScheduledFuture<V> delegate;
        private final M3ProgressMonitor owner;

        private CancelAwareScheduledFuture(
                ScheduledFuture<V> delegate,
                M3ProgressMonitor owner) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!delegate.cancel(mayInterruptIfRunning)) return false;
            owner.setCanceled(true);
            return true;
        }

        @Override public boolean isCancelled() { return delegate.isCancelled(); }
        @Override public boolean isDone() { return delegate.isDone(); }
        @Override public V get() throws java.lang.InterruptedException,
                java.util.concurrent.ExecutionException { return delegate.get(); }

        @Override public V get(long timeout, TimeUnit unit)
                throws java.lang.InterruptedException,
                java.util.concurrent.ExecutionException,
                java.util.concurrent.TimeoutException {
            return delegate.get(timeout, unit);
        }

        @Override public long getDelay(TimeUnit unit) { return delegate.getDelay(unit); }

        @Override public int compareTo(Delayed other) {
            if (other == this) return 0;
            if (other instanceof CancelAwareScheduledFuture<?> wrapped) {
                return delegate.compareTo(wrapped.delegate);
            }
            return delegate.compareTo(other);
        }
    }
}
