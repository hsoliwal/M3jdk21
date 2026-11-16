/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 */

/*
 * @test
 * @summary Monitored defaults dispatch through existing scheduling and decoration hooks
 * @run main/othervm MonitoredScheduledCompatibility
 */

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MonitoredScheduledCompatibility {
    private static final class Pool extends ScheduledThreadPoolExecutor {
        final AtomicInteger runSchedules = new AtomicInteger();
        final AtomicInteger callSchedules = new AtomicInteger();
        final AtomicInteger rateSchedules = new AtomicInteger();
        final AtomicInteger delaySchedules = new AtomicInteger();
        final AtomicInteger decoratedRuns = new AtomicInteger();
        final AtomicInteger decoratedCalls = new AtomicInteger();

        Pool() {
            super(2);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay,
                                           TimeUnit unit) {
            runSchedules.incrementAndGet();
            return super.schedule(command, delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay,
                                                TimeUnit unit) {
            callSchedules.incrementAndGet();
            return super.schedule(callable, delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                long initialDelay, long period, TimeUnit unit) {
            rateSchedules.incrementAndGet();
            return super.scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                long initialDelay, long delay, TimeUnit unit) {
            delaySchedules.incrementAndGet();
            return super.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }

        @Override
        protected <V> RunnableScheduledFuture<V> decorateTask(
                Runnable command, RunnableScheduledFuture<V> task) {
            decoratedRuns.incrementAndGet();
            return super.decorateTask(command, task);
        }

        @Override
        protected <V> RunnableScheduledFuture<V> decorateTask(
                Callable<V> command, RunnableScheduledFuture<V> task) {
            decoratedCalls.incrementAndGet();
            return super.decorateTask(command, task);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        Pool pool = new Pool();
        ProgressMonitor monitor = new ProgressMonitor() { };
        try {
            ScheduledFuture<?> run = pool.scheduleMonitored(() -> {
                check(ProgressMonitors.current() == monitor, "runnable scope");
            }, 0, TimeUnit.MILLISECONDS, monitor);
            check(run.get(5, TimeUnit.SECONDS) == null, "runnable result");

            ScheduledFuture<String> call = pool.scheduleMonitored(() -> {
                check(ProgressMonitors.current() == monitor, "callable scope");
                return "ok";
            }, 0, TimeUnit.MILLISECONDS, monitor);
            check("ok".equals(call.get(5, TimeUnit.SECONDS)), "callable result");

            CountDownLatch rateRuns = new CountDownLatch(1);
            ScheduledFuture<?> rate = pool.scheduleAtFixedRateMonitored(
                    rateRuns::countDown, 0, 10, TimeUnit.MILLISECONDS, monitor);
            check(rateRuns.await(5, TimeUnit.SECONDS), "rate timeout");
            rate.cancel(false);

            CountDownLatch delayRuns = new CountDownLatch(1);
            ScheduledFuture<?> delay = pool.scheduleWithFixedDelayMonitored(
                    delayRuns::countDown, 0, 10, TimeUnit.MILLISECONDS, monitor);
            check(delayRuns.await(5, TimeUnit.SECONDS), "delay timeout");
            delay.cancel(false);

            check(pool.schedule(() -> { }, 0, TimeUnit.MILLISECONDS)
                    .get(5, TimeUnit.SECONDS) == null, "legacy schedule");
            check(pool.runSchedules.get() == 2, "runnable dispatch");
            check(pool.callSchedules.get() == 1, "callable dispatch");
            check(pool.rateSchedules.get() == 1, "rate dispatch");
            check(pool.delaySchedules.get() == 1, "delay dispatch");
            check(pool.decoratedRuns.get() == 4, "runnable decorateTask");
            check(pool.decoratedCalls.get() == 1, "callable decorateTask");
        } finally {
            pool.shutdownNow();
            check(pool.awaitTermination(5, TimeUnit.SECONDS), "pool shutdown");
        }
    }
}
