/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 */

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Compiled once with an unmodified stock JDK; its bytes are never recompiled for the receiver. */
public final class LegacyScheduledClient {
    private static int checks;

    public static void main(String[] args) throws Exception {
        try (FrozenScheduler scheduler = new FrozenScheduler()) {
            boolean added = args.length == 1 && args[0].equals("new-defaults");
            Object value = new Object();
            int[] seen = {0};
            Runnable runnable = () -> seen[0]++;
            Callable<Object> callable = () -> value;
            ScheduledFuture<?> run;
            ScheduledFuture<?> call;
            ScheduledFuture<?> rate;
            ScheduledFuture<?> delay;
            RuntimeException stop = new RuntimeException("periodic stop");
            Runnable terminal = () -> { throw stop; };
            if (added) {
                Class<?> monitorClass = Class.forName("java.util.concurrent.ProgressMonitor");
                Object monitor = monitorClass.getField("NONE").get(null);
                Class<ScheduledExecutorService> api = ScheduledExecutorService.class;
                Method runMethod = api.getMethod("scheduleMonitored", Runnable.class,
                        long.class, TimeUnit.class, monitorClass);
                Method callMethod = api.getMethod("scheduleMonitored", Callable.class,
                        long.class, TimeUnit.class, monitorClass);
                Method rateMethod = api.getMethod("scheduleAtFixedRateMonitored", Runnable.class,
                        long.class, long.class, TimeUnit.class, monitorClass);
                Method delayMethod = api.getMethod("scheduleWithFixedDelayMonitored", Runnable.class,
                        long.class, long.class, TimeUnit.class, monitorClass);
                run = (ScheduledFuture<?>) runMethod.invoke(scheduler, runnable, -1L, TimeUnit.SECONDS, monitor);
                call = (ScheduledFuture<?>) callMethod.invoke(scheduler, callable, 0L, TimeUnit.SECONDS, monitor);
                rate = (ScheduledFuture<?>) rateMethod.invoke(scheduler, terminal,
                        0L, 1L, TimeUnit.NANOSECONDS, monitor);
                delay = (ScheduledFuture<?>) delayMethod.invoke(scheduler, terminal,
                        0L, 1L, TimeUnit.NANOSECONDS, monitor);
            } else {
                run = scheduler.schedule(runnable, -1, TimeUnit.SECONDS);
                call = scheduler.schedule(callable, 0, TimeUnit.SECONDS);
                rate = scheduler.scheduleAtFixedRate(terminal, 0, 1, TimeUnit.NANOSECONDS);
                delay = scheduler.scheduleWithFixedDelay(terminal, 0, 1, TimeUnit.NANOSECONDS);
            }
            check(run.get(10, TimeUnit.SECONDS) == null && seen[0] == 1);
            check(call.get(10, TimeUnit.SECONDS) == value);
            cause(rate, stop);
            cause(delay, stop);
            check(scheduler.runnableCalls == 1 && scheduler.callableCalls == 1
                    && scheduler.rateCalls == 1 && scheduler.delayCalls == 1);
            check(java.util.Arrays.stream(FrozenScheduler.class.getDeclaredMethods())
                    .noneMatch(method -> method.getName().endsWith("Monitored")));
            System.out.println("LegacyScheduledClient checks=" + checks);
        }
    }

    private static void cause(ScheduledFuture<?> future, Throwable expected) throws Exception {
        try {
            future.get(10, TimeUnit.SECONDS);
            throw new AssertionError("periodic task completed normally");
        } catch (ExecutionException failure) {
            check(failure.getCause() == expected);
        }
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("legacy scheduler contract");
    }

    public static final class FrozenScheduler extends AbstractExecutorService
            implements ScheduledExecutorService {
        private final ScheduledExecutorService delegate = Executors.newSingleThreadScheduledExecutor();
        int runnableCalls;
        int callableCalls;
        int rateCalls;
        int delayCalls;
        @Override public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            runnableCalls++;
            return delegate.schedule(command, delay, unit);
        }
        @Override public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
            callableCalls++;
            return delegate.schedule(task, delay, unit);
        }
        @Override public ScheduledFuture<?> scheduleAtFixedRate(
                Runnable command, long initial, long period, TimeUnit unit) {
            rateCalls++;
            return delegate.scheduleAtFixedRate(command, initial, period, unit);
        }
        @Override public ScheduledFuture<?> scheduleWithFixedDelay(
                Runnable command, long initial, long delay, TimeUnit unit) {
            delayCalls++;
            return delegate.scheduleWithFixedDelay(command, initial, delay, unit);
        }
        @Override public void shutdown() { delegate.shutdown(); }
        @Override public List<Runnable> shutdownNow() { return delegate.shutdownNow(); }
        @Override public boolean isShutdown() { return delegate.isShutdown(); }
        @Override public boolean isTerminated() { return delegate.isTerminated(); }
        @Override public boolean awaitTermination(long time, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(time, unit);
        }
        @Override public void execute(Runnable command) { delegate.execute(command); }
    }
}
