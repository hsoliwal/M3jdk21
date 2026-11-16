/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Monitored scheduling preserves protected task decoration, future identity, and worker scopes
 * @run main/othervm ScheduledProgressDecorateTask
 * @run main/othervm -Xint ScheduledProgressDecorateTask
 */

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduledProgressDecorateTask {
    private static final long WAIT_SECONDS = 10L;
    private static final AtomicInteger CHECKS = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        ProgressMonitors.resetSystem();
        ProgressMonitor worker = new ProgressMonitor() { };
        ProgressMonitor task = new ProgressMonitor() { };
        try (DecoratingScheduler scheduler = new DecoratingScheduler(worker)) {
            oneShotRunnable(scheduler, worker, task);
            oneShotCallable(scheduler, worker, task);
            periodic(scheduler, worker, task, true);
            periodic(scheduler, worker, task, false);
            check(ProgressMonitors.current() == ProgressMonitor.NONE,
                    "submission leaves caller scope unchanged");
            System.out.println("ScheduledProgressDecorateTask checks=" + CHECKS.get());
        } finally {
            ProgressMonitors.resetSystem();
        }
    }

    private static void oneShotRunnable(DecoratingScheduler scheduler,
            ProgressMonitor worker, ProgressMonitor task) throws Exception {
        int runnableBefore = scheduler.runnableDecorations;
        int callableBefore = scheduler.callableDecorations;
        Runnable command = () -> check(ProgressMonitors.current() == task,
                "decorated runnable executes in its borrowed scope");
        ScheduledFuture<?> future = scheduler.scheduleMonitored(
                command, 0L, TimeUnit.NANOSECONDS, task);
        checkDecoration(scheduler, future, runnableBefore, callableBefore, false, false);
        check(future.get(WAIT_SECONDS, TimeUnit.SECONDS) == null,
                "decorated runnable retains its null result");
        checkWorkerScope(scheduler, worker);
    }

    private static void oneShotCallable(DecoratingScheduler scheduler,
            ProgressMonitor worker, ProgressMonitor task) throws Exception {
        int runnableBefore = scheduler.runnableDecorations;
        int callableBefore = scheduler.callableDecorations;
        Object result = new Object();
        Callable<Object> command = () -> {
            check(ProgressMonitors.current() == task,
                    "decorated callable executes in its borrowed scope");
            return result;
        };
        ScheduledFuture<Object> future = scheduler.scheduleMonitored(
                command, 0L, TimeUnit.NANOSECONDS, task);
        checkDecoration(scheduler, future, runnableBefore, callableBefore, true, false);
        check(future.get(WAIT_SECONDS, TimeUnit.SECONDS) == result,
                "decorated callable retains result identity");
        checkWorkerScope(scheduler, worker);
    }

    private static void periodic(DecoratingScheduler scheduler,
            ProgressMonitor worker, ProgressMonitor task, boolean fixedRate) throws Exception {
        int runnableBefore = scheduler.runnableDecorations;
        int callableBefore = scheduler.callableDecorations;
        AtomicInteger executions = new AtomicInteger();
        CountDownLatch secondEntered = new CountDownLatch(1);
        CountDownLatch releaseSecond = new CountDownLatch(1);
        Runnable command = () -> {
            check(ProgressMonitors.current() == task,
                    "each decorated periodic execution has the borrowed scope");
            int execution = executions.incrementAndGet();
            check(execution <= 2, "cancelled periodic task is not rescheduled");
            if (execution == 2) {
                secondEntered.countDown();
                awaitUnchecked(releaseSecond);
            }
        };
        try {
            ScheduledFuture<?> future = fixedRate
                    ? scheduler.scheduleAtFixedRateMonitored(
                            command, 0L, 1L, TimeUnit.NANOSECONDS, task)
                    : scheduler.scheduleWithFixedDelayMonitored(
                            command, 0L, 1L, TimeUnit.NANOSECONDS, task);
            checkDecoration(scheduler, future, runnableBefore, callableBefore, false, true);
            await(secondEntered);
            check(!future.isDone(), "periodic decorated future remains live after rescheduling");
            check(future.cancel(false), "decorated future delegates running cancellation");
            check(future.isDone() && future.isCancelled(),
                    "decorated future exposes the scheduler task's cancelled state");
            try {
                future.get(WAIT_SECONDS, TimeUnit.SECONDS);
                throw new AssertionError("cancelled future returned normally");
            } catch (CancellationException expected) {
                check(true, "decorated future get preserves cancellation");
            }
            releaseSecond.countDown();
            // One worker makes this probe run after the cancelled invocation has
            // left its progress scope and completed the scheduler's run/reset path.
            checkWorkerScope(scheduler, worker);
            check(executions.get() == 2, "periodic cancellation suppresses the next invocation");
        } finally {
            releaseSecond.countDown();
        }
    }

    private static void checkDecoration(DecoratingScheduler scheduler,
            ScheduledFuture<?> future, int runnableBefore, int callableBefore,
            boolean callable, boolean periodic) {
        check(future == (callable ? scheduler.lastCallable : scheduler.lastRunnable),
                "monitored operation returns the exact protected-hook result");
        check(future instanceof DecoratedFuture<?>, "future is the distinct decoration");
        check(scheduler.runnableDecorations == runnableBefore + (callable ? 0 : 1)
                        && scheduler.callableDecorations == callableBefore + (callable ? 1 : 0),
                "exactly the original Runnable or Callable decoration hook is invoked");
        check(((RunnableScheduledFuture<?>) future).isPeriodic() == periodic,
                "decoration preserves the scheduler's periodic classification");
    }

    private static void checkWorkerScope(DecoratingScheduler scheduler,
            ProgressMonitor worker) throws Exception {
        Callable<ProgressMonitor> probe = ProgressMonitors::current;
        check(scheduler.schedule(probe, 0L, TimeUnit.NANOSECONDS)
                        .get(WAIT_SECONDS, TimeUnit.SECONDS) == worker,
                "unmonitored work sees the restored worker scope");
    }

    private static final class DecoratingScheduler extends ScheduledThreadPoolExecutor {
        int runnableDecorations;
        int callableDecorations;
        DecoratedFuture<?> lastRunnable;
        DecoratedFuture<?> lastCallable;

        DecoratingScheduler(ProgressMonitor worker) {
            super(1, workerFactory(worker));
        }

        @SuppressWarnings("try")
        private static ThreadFactory workerFactory(ProgressMonitor monitor) {
            return command -> new Thread(() -> {
                try (ProgressMonitors.Scope ignored = ProgressMonitors.bind(monitor)) {
                    command.run();
                }
            }, "scheduled-progress-decoration-worker");
        }

        @Override
        protected <V> RunnableScheduledFuture<V> decorateTask(
                Runnable command, RunnableScheduledFuture<V> task) {
            DecoratedFuture<V> decorated = new DecoratedFuture<>(task);
            runnableDecorations++;
            lastRunnable = decorated;
            return decorated;
        }

        @Override
        protected <V> RunnableScheduledFuture<V> decorateTask(
                Callable<V> command, RunnableScheduledFuture<V> task) {
            DecoratedFuture<V> decorated = new DecoratedFuture<>(task);
            callableDecorations++;
            lastCallable = decorated;
            return decorated;
        }

        @Override
        public void close() {
            shutdownNow();
            try {
                if (!awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)) {
                    throw new AssertionError("decorating scheduler did not terminate");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
        }
    }

    private static final class DecoratedFuture<V> implements RunnableScheduledFuture<V> {
        private final RunnableScheduledFuture<V> delegate;

        DecoratedFuture(RunnableScheduledFuture<V> delegate) {
            this.delegate = delegate;
        }

        @Override public void run() { delegate.run(); }
        @Override public boolean isPeriodic() { return delegate.isPeriodic(); }
        @Override public long getDelay(TimeUnit unit) { return delegate.getDelay(unit); }
        @Override public int compareTo(Delayed other) {
            return delegate.compareTo(other instanceof DecoratedFuture<?> decorated
                    ? decorated.delegate : other);
        }
        @Override public boolean cancel(boolean interrupt) { return delegate.cancel(interrupt); }
        @Override public boolean isCancelled() { return delegate.isCancelled(); }
        @Override public boolean isDone() { return delegate.isDone(); }
        @Override public V get() throws InterruptedException, ExecutionException {
            return delegate.get();
        }
        @Override public V get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.get(timeout, unit);
        }
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
            throw new AssertionError("periodic decoration latch timeout");
        }
    }

    private static void awaitUnchecked(CountDownLatch latch) {
        try {
            await(latch);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static void check(boolean condition, String message) {
        CHECKS.incrementAndGet();
        if (!condition) throw new AssertionError(message);
    }
}
