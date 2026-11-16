/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 */

/*
 * @test
 * @summary Monitored scheduled defaults preserve progress scope and future behavior
 * @run main/othervm MonitoredScheduledDefaults
 */

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MonitoredScheduledDefaults {
    private static final class Monitor implements ProgressMonitor {
        final AtomicInteger completions = new AtomicInteger();
        volatile boolean cancelled;

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void done() {
            completions.incrementAndGet();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void await(CountDownLatch latch) throws Exception {
        check(latch.await(5, TimeUnit.SECONDS), "timed out waiting for task");
    }

    private static void expectNpe(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected NullPointerException");
        } catch (NullPointerException expected) {
            // Required null contract.
        }
    }

    public static void main(String[] args) throws Exception {
        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(2);
        Monitor monitor = new Monitor();
        ProgressMonitor original = ProgressMonitors.current();
        try {
            AtomicInteger oneShot = new AtomicInteger();
            ScheduledFuture<?> runnable = pool.scheduleMonitored(() -> {
                check(ProgressMonitors.current() == monitor, "runnable monitor");
                oneShot.incrementAndGet();
            }, 0, TimeUnit.MILLISECONDS, monitor);
            check(runnable.get(5, TimeUnit.SECONDS) == null, "runnable result");
            check(oneShot.get() == 1, "runnable count");

            ScheduledFuture<Integer> callable = pool.scheduleMonitored(() -> {
                check(ProgressMonitors.current() == monitor, "callable monitor");
                return 41;
            }, 0, TimeUnit.MILLISECONDS, monitor);
            check(callable.get(5, TimeUnit.SECONDS) == 41, "callable result");

            CountDownLatch rates = new CountDownLatch(3);
            ScheduledFuture<?> rate = pool.scheduleAtFixedRateMonitored(() -> {
                check(ProgressMonitors.current() == monitor, "rate monitor");
                rates.countDown();
            }, 0, 1, TimeUnit.MILLISECONDS, monitor);
            await(rates);
            check(rate.cancel(false), "rate cancellation");

            CountDownLatch delays = new CountDownLatch(3);
            ScheduledFuture<?> delay = pool.scheduleWithFixedDelayMonitored(() -> {
                check(ProgressMonitors.current() == monitor, "delay monitor");
                delays.countDown();
            }, 0, 1, TimeUnit.MILLISECONDS, monitor);
            await(delays);
            check(delay.cancel(false), "delay cancellation");

            monitor.cancelled = true;
            AtomicInteger skipped = new AtomicInteger();
            ScheduledFuture<?> cancelled = pool.scheduleMonitored(
                    () -> { skipped.incrementAndGet(); },
                    0, TimeUnit.MILLISECONDS, monitor);
            try {
                cancelled.get(5, TimeUnit.SECONDS);
                throw new AssertionError("expected cooperative cancellation");
            } catch (ExecutionException expected) {
                check(expected.getCause() instanceof CancellationException,
                        "cooperative cancellation cause");
            }
            check(skipped.get() == 0, "cancelled task ran");
            check(!cancelled.isCancelled(), "monitor cancellation changed future state");

            expectNpe(() -> pool.scheduleMonitored(
                    (Runnable) null, 0, TimeUnit.MILLISECONDS, monitor));
            expectNpe(() -> pool.scheduleMonitored(
                    () -> { }, 0, TimeUnit.MILLISECONDS, null));
            expectNpe(() -> pool.scheduleMonitored(
                    () -> 1, 0, TimeUnit.MILLISECONDS, null));
            expectNpe(() -> pool.scheduleAtFixedRateMonitored(
                    () -> { }, 0, 1, TimeUnit.MILLISECONDS, null));
            expectNpe(() -> pool.scheduleWithFixedDelayMonitored(
                    () -> { }, 0, 1, TimeUnit.MILLISECONDS, null));
            expectNpe(() -> pool.scheduleMonitored(
                    () -> { }, 0, null, monitor));

            check(monitor.completions.get() == 0, "borrowed monitor completed");
            check(ProgressMonitors.current() == original, "caller scope changed");
        } finally {
            pool.shutdownNow();
            check(pool.awaitTermination(5, TimeUnit.SECONDS), "pool shutdown");
        }
    }
}
