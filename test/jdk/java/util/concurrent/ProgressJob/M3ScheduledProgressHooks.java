/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 */

/*
 * @test
 * @summary Monitored scheduling preserves legacy virtual entry points and protected task decoration
 * @build M3LegacyDecoratingScheduler
 * @run main/othervm M3ScheduledProgressHooks
 * @run main/othervm -Xint M3ScheduledProgressHooks
 */

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class M3ScheduledProgressHooks {
    private static final long TIMEOUT_SECONDS = 10;
    private static final AtomicInteger CHECKS = new AtomicInteger();

    @FunctionalInterface
    private interface Action { void run() throws Exception; }

    private static final class Monitor implements ProgressMonitor {
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicInteger checks = new AtomicInteger();
        final AtomicInteger begins = new AtomicInteger();
        final AtomicInteger dones = new AtomicInteger();
        final AtomicInteger cancels = new AtomicInteger();
        final CancellationException cancellation = new CancellationException("borrowed monitor");

        @Override public void begin(String name, long work) { begins.incrementAndGet(); }
        @Override public void done() { dones.incrementAndGet(); }
        @Override public boolean isCancelled() { return cancelled.get(); }
        @Override public void cancel() { cancels.incrementAndGet(); cancelled.set(true); }
        @Override public void checkCancelled() {
            checks.incrementAndGet();
            if (isCancelled() || Thread.currentThread().isInterrupted()) throw cancellation;
        }
    }

    private static void check(boolean condition, String message) {
        CHECKS.incrementAndGet();
        if (!condition) throw new AssertionError(message);
    }

    private static <X extends Throwable> X expect(Class<X> type, Action action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type + ", got " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("expected " + type);
    }

    private static <V> V get(Future<V> future) throws Exception {
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static void failed(Future<?> future, Throwable cause) {
        ExecutionException failure = expect(ExecutionException.class, () -> get(future));
        check(failure.getCause() == cause, "scheduler retains exact task failure");
        check(future.isDone() && !future.isCancelled(), "task exception is not Future.cancel");
    }

    private static void borrowed(Monitor monitor, int ownedCancels) {
        check(monitor.begins.get() == 0 && monitor.dones.get() == 0,
                "scheduler does not begin or finish borrowed monitor");
        check(monitor.cancels.get() == ownedCancels,
                "only explicit owner/body cancellation reaches monitor.cancel");
    }

    private static void returned(M3LegacyDecoratingScheduler legacy, ScheduledFuture<?> future,
            String kind, long initialDelay, long interval, TimeUnit unit, boolean periodic) {
        check(legacy.lastKind.equals(kind), "correct old virtual scheduler overload: " + kind);
        check(legacy.lastInitialDelay == initialDelay && legacy.lastInterval == interval
                && legacy.lastUnit == unit, "timing arguments pass unchanged to old override");
        check(future == legacy.lastFuture && future == legacy.lastDecoration,
                "default returns exact future produced by old protected decoration hook");
        check(((RunnableScheduledFuture<?>) future).isPeriodic() == periodic,
                "decorated future retains scheduler periodicity");
    }

    private static void signatures() throws Exception {
        for (String name : new String[] { "scheduleAtFixedRateMonitored", "scheduleWithFixedDelayMonitored" }) {
            Method method = ScheduledExecutorService.class.getDeclaredMethod(name,
                    Runnable.class, long.class, long.class, TimeUnit.class, ProgressMonitor.class);
            check(method.isDefault() && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers()), "periodic extension is an inherited default");
        }
        for (Class<?> taskType : new Class<?>[] { Runnable.class, Callable.class }) {
            Method method = ScheduledExecutorService.class.getDeclaredMethod("scheduleMonitored",
                    taskType, long.class, TimeUnit.class, ProgressMonitor.class);
            check(method.isDefault() && method.getReturnType() == ScheduledFuture.class,
                    "one-shot default retains actual ScheduledFuture contract");
        }
    }

    private static void oneShot(M3LegacyDecoratingScheduler legacy, Monitor caller) throws Exception {
        ScheduledExecutorService api = legacy;
        Monitor supplied = new Monitor();
        int runnableBefore = legacy.runnableDecorations.get();
        int callableBefore = legacy.callableDecorations.get();
        ScheduledFuture<?> runnable = api.scheduleMonitored((Runnable) () -> {
            check(ProgressMonitors.current() == supplied, "runnable receives supplied monitor");
        }, -7, TimeUnit.MILLISECONDS, supplied);
        returned(legacy, runnable, "runnable", -7, 0, TimeUnit.MILLISECONDS, false);
        check(get(runnable) == null, "runnable future retains null result");
        check(legacy.runnableDecorations.get() == runnableBefore + 1
                && legacy.callableDecorations.get() == callableBefore, "runnable uses Runnable decoration hook");

        Object result = new Object();
        ScheduledFuture<Object> callable = api.scheduleMonitored((Callable<Object>) () -> {
            check(ProgressMonitors.current() == supplied, "callable receives supplied monitor");
            return result;
        }, -11, TimeUnit.MICROSECONDS, supplied);
        returned(legacy, callable, "callable", -11, 0, TimeUnit.MICROSECONDS, false);
        check(get(callable) == result, "callable future retains result identity");
        check(legacy.runnableDecorations.get() == runnableBefore + 1
                && legacy.callableDecorations.get() == callableBefore + 1,
                "callable uses Callable decoration hook");
        check(supplied.checks.get() == 4, "one-shot wrappers check cancellation before and after bodies");
        check(ProgressMonitors.current() == caller, "submissions never replace caller scope");
        borrowed(supplied, 0);

        Exception checked = new Exception("checked task failure");
        failed(api.scheduleMonitored((Callable<Object>) () -> { throw checked; },
                0, TimeUnit.NANOSECONDS, supplied), checked);
        RuntimeException unchecked = new IllegalStateException("runnable failure");
        failed(api.scheduleMonitored((Runnable) () -> { throw unchecked; },
                0, TimeUnit.NANOSECONDS, supplied), unchecked);
        borrowed(supplied, 0);
    }

    private static void queuedCancellation(M3LegacyDecoratingScheduler legacy) throws Exception {
        ScheduledExecutorService api = legacy;
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> blocker = legacy.schedule((Callable<Void>) () -> {
            entered.countDown();
            if (!release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) throw new AssertionError("release timeout");
            return null;
        }, 0, TimeUnit.NANOSECONDS);
        check(entered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "worker occupied before scheduling");
        Monitor cancelled = new Monitor();
        cancelled.cancel();
        Monitor untouched = new Monitor();
        AtomicInteger bodies = new AtomicInteger();
        try {
            ScheduledFuture<?> runnable = api.scheduleMonitored((Runnable) bodies::incrementAndGet,
                    0, TimeUnit.NANOSECONDS, cancelled);
            ScheduledFuture<Integer> callable = api.scheduleMonitored((Callable<Integer>) bodies::incrementAndGet,
                    0, TimeUnit.NANOSECONDS, cancelled);
            check(cancelled.checks.get() == 0 && !runnable.isDone() && !callable.isDone(),
                    "monitor is checked on execution, not at submission or through proactive cancellation");

            ScheduledFuture<?> cancelledFuture = api.scheduleMonitored((Runnable) bodies::incrementAndGet,
                    0, TimeUnit.NANOSECONDS, untouched);
            returned(legacy, cancelledFuture, "runnable", 0, 0, TimeUnit.NANOSECONDS, false);
            check(cancelledFuture.cancel(false) && cancelledFuture.isCancelled(),
                    "owner cancels actual decorated future while queued");
            expect(CancellationException.class, () -> get(cancelledFuture));
            release.countDown();
            get(blocker);
            failed(runnable, cancelled.cancellation);
            failed(callable, cancelled.cancellation);
            check(bodies.get() == 0 && cancelled.checks.get() == 2, "pre-cancellation skips both one-shot bodies");
            check(untouched.checks.get() == 0 && !untouched.isCancelled(),
                    "Future.cancel never invokes body or borrowed monitor cancellation");
            borrowed(cancelled, 1);
            borrowed(untouched, 0);
        } finally {
            release.countDown();
        }
    }

    private static void periodic(M3LegacyDecoratingScheduler legacy, boolean rate) throws Exception {
        ScheduledExecutorService api = legacy;
        Monitor supplied = new Monitor();
        AtomicInteger ticks = new AtomicInteger();
        Runnable body = () -> {
            check(ProgressMonitors.current() == supplied, "every periodic tick binds the same supplied monitor");
            if (ticks.incrementAndGet() == 3) supplied.cancel();
        };
        int runnableBefore = legacy.runnableDecorations.get();
        int callableBefore = legacy.callableDecorations.get();
        ScheduledFuture<?> future = rate
                ? api.scheduleAtFixedRateMonitored(body, -3, 1, TimeUnit.MILLISECONDS, supplied)
                : api.scheduleWithFixedDelayMonitored(body, -3, 1, TimeUnit.MILLISECONDS, supplied);
        returned(legacy, future, rate ? "rate" : "delay", -3, 1, TimeUnit.MILLISECONDS, true);
        failed(future, supplied.cancellation);
        check(ticks.get() == 3 && supplied.checks.get() == 6,
                "post-body cooperative cancellation terminates the periodic future after three ticks");
        check(legacy.runnableDecorations.get() == runnableBefore + 1
                && legacy.callableDecorations.get() == callableBefore,
                "periodic wrapper preserves Runnable decoration");
        borrowed(supplied, 1);

        Monitor untouched = new Monitor();
        ScheduledFuture<?> cancelledFuture = rate
                ? api.scheduleAtFixedRateMonitored(body, 1, 1, TimeUnit.DAYS, untouched)
                : api.scheduleWithFixedDelayMonitored(body, 1, 1, TimeUnit.DAYS, untouched);
        returned(legacy, cancelledFuture, rate ? "rate" : "delay", 1, 1, TimeUnit.DAYS, true);
        check(cancelledFuture.cancel(false) && cancelledFuture.isCancelled(), "periodic future cancellation stays scheduler-owned");
        expect(CancellationException.class, () -> get(cancelledFuture));
        check(untouched.checks.get() == 0 && !untouched.isCancelled(), "cancelled periodic future does not use monitor");
        borrowed(untouched, 0);
        check(ticks.get() == 3, "completed periodic future never executes another body");
    }

    private static void validation(M3LegacyDecoratingScheduler legacy) {
        ScheduledExecutorService api = legacy;
        Monitor supplied = new Monitor();
        Runnable body = () -> { throw new AssertionError("validation must not execute body"); };
        Callable<Object> callable = () -> { throw new AssertionError("validation must not execute callable"); };
        int submissions = legacy.submissions.get();
        expect(NullPointerException.class, () -> api.scheduleMonitored((Runnable) null, 0, TimeUnit.SECONDS, supplied));
        expect(NullPointerException.class, () -> api.scheduleMonitored((Callable<Object>) null, 0, TimeUnit.SECONDS, supplied));
        expect(NullPointerException.class, () -> api.scheduleMonitored(body, 0, TimeUnit.SECONDS, null));
        expect(NullPointerException.class, () -> api.scheduleMonitored(callable, 0, TimeUnit.SECONDS, null));
        expect(NullPointerException.class, () -> api.scheduleAtFixedRateMonitored(null, 0, 1, TimeUnit.SECONDS, supplied));
        expect(NullPointerException.class, () -> api.scheduleWithFixedDelayMonitored(null, 0, 1, TimeUnit.SECONDS, supplied));
        expect(NullPointerException.class, () -> api.scheduleAtFixedRateMonitored(body, 0, 1, TimeUnit.SECONDS, null));
        expect(NullPointerException.class, () -> api.scheduleWithFixedDelayMonitored(body, 0, 1, TimeUnit.SECONDS, null));
        check(legacy.submissions.get() == submissions, "null task/monitor guards precede old scheduler dispatch");

        expect(NullPointerException.class, () -> api.scheduleMonitored(body, -7, null, supplied));
        check(legacy.submissions.get() == ++submissions && legacy.lastInitialDelay == -7 && legacy.lastUnit == null,
                "runnable unit validation belongs to old scheduler");
        expect(NullPointerException.class, () -> api.scheduleMonitored(callable, -8, null, supplied));
        check(legacy.submissions.get() == ++submissions && legacy.lastInitialDelay == -8 && legacy.lastUnit == null,
                "callable unit validation belongs to old scheduler");
        expect(IllegalArgumentException.class, () -> api.scheduleAtFixedRateMonitored(body, -9, 0, TimeUnit.SECONDS, supplied));
        check(legacy.submissions.get() == ++submissions && legacy.lastInitialDelay == -9 && legacy.lastInterval == 0,
                "zero rate validation belongs to old scheduler");
        expect(IllegalArgumentException.class, () -> api.scheduleWithFixedDelayMonitored(body, -10, -2, TimeUnit.SECONDS, supplied));
        check(legacy.submissions.get() == ++submissions && legacy.lastInitialDelay == -10 && legacy.lastInterval == -2,
                "negative delay validation belongs to old scheduler");

        RejectedExecutionException marker = new RejectedExecutionException("legacy policy rejection");
        legacy.forcedFailure = marker;
        try {
            check(expect(RejectedExecutionException.class,
                    () -> api.scheduleMonitored(body, 0, TimeUnit.SECONDS, supplied)) == marker,
                    "runnable rejection identity preserved");
            check(expect(RejectedExecutionException.class,
                    () -> api.scheduleMonitored(callable, 0, TimeUnit.SECONDS, supplied)) == marker,
                    "callable rejection identity preserved");
            check(expect(RejectedExecutionException.class,
                    () -> api.scheduleAtFixedRateMonitored(body, 0, 1, TimeUnit.SECONDS, supplied)) == marker,
                    "rate rejection identity preserved");
            check(expect(RejectedExecutionException.class,
                    () -> api.scheduleWithFixedDelayMonitored(body, 0, 1, TimeUnit.SECONDS, supplied)) == marker,
                    "fixed-delay rejection identity preserved");
        } finally {
            legacy.forcedFailure = null;
        }
        check(supplied.checks.get() == 0, "invalid or rejected submission does not bind/check monitor");
        borrowed(supplied, 0);
    }

    public static void main(String[] args) throws Exception {
        signatures();
        Monitor worker = new Monitor();
        Monitor caller = new Monitor();
        M3LegacyDecoratingScheduler legacy = new M3LegacyDecoratingScheduler(task -> new Thread(() -> {
            ProgressMonitors.Scope workerScope = ProgressMonitors.bind(worker);
            try (workerScope) { task.run(); }
        }, "legacy-decorating-scheduler"));
        legacy.workerContext = ProgressMonitors::current;
        legacy.expectedWorkerContext = worker;
        ProgressMonitors.Scope callerScope = ProgressMonitors.bind(caller);
        try (callerScope) {
            oneShot(legacy, caller);
            queuedCancellation(legacy);
            periodic(legacy, true);
            periodic(legacy, false);
            validation(legacy);
            check(get(legacy.schedule((Callable<ProgressMonitor>) ProgressMonitors::current,
                    0, TimeUnit.NANOSECONDS)) == worker, "ordinary scheduled call retains previous worker lexical binding");
            check(legacy.unexpectedWorkerContext.get() == null,
                    "old beforeExecute/afterExecute hooks always observe restored worker binding");
            check(ProgressMonitors.current() == caller, "caller lexical binding survives every submission path");
        } finally {
            legacy.shutdownNow();
            check(legacy.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS), "scheduler terminates");
        }
        check(legacy.beforeHooks.get() == legacy.afterHooks.get() && legacy.beforeHooks.get() > 0,
                "old executor lifecycle hooks run for every processed task");
        check(legacy.unexpectedWorkerContext.get() == null, "final worker completion leaves no monitor leak");
        borrowed(worker, 0);
        borrowed(caller, 0);
        System.out.println("PASS scheduled legacy decoration hooks checks=" + CHECKS.get());
    }
}
