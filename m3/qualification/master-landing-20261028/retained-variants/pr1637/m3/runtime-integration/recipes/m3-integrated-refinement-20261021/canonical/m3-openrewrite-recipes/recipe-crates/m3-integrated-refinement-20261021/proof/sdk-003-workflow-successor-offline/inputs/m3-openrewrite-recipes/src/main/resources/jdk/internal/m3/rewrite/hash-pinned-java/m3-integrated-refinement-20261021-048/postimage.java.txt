/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only
 */

/*
 * @test
 * @summary Borrowed progress scopes preserve real scheduled futures and periodic execution contracts
 * @run main/othervm M3ScheduledProgress candidate
 * @run main/othervm -Xint M3ScheduledProgress candidate
 */

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ProgressJob;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The baseline mode exercises the existing explicit scope composition. Candidate
 * mode exercises the additive defaults against the same externally observable
 * contracts. Neither mode substitutes a scheduler, future, clock or worker.
 */
public class M3ScheduledProgress {
    private static final AtomicInteger CHECKS = new AtomicInteger();
    private static final long WAIT_SECONDS = 10L;
    private static boolean baseline;

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("baseline") || args[0].equals("candidate"))) {
            throw new IllegalArgumentException("baseline or candidate required");
        }
        baseline = args[0].equals("baseline");
        ProgressMonitors.resetSystem();
        try {
            apiShape();
            oneShotAndScopes();
            exceptionalCompletion();
            cancellationBeforeExecution();
            cancellationWhileRunning(true);
            cancellationWhileRunning(false);
            childAndStatusIdentity();
            periodic(false, false);
            periodic(true, false);
            periodic(false, true);
            periodic(true, true);
            refusalAndShutdown();
            check(ProgressMonitors.current() == ProgressMonitor.NONE, "caller scope restored");
            System.out.println("M3ScheduledProgress semantic checks=" + CHECKS.get());
        } finally {
            ProgressMonitors.resetSystem();
        }
    }

    private static void apiShape() {
        int original = 0;
        int added = 0;
        for (Method method : ScheduledExecutorService.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) continue;
            if (method.getName().endsWith("Monitored")) {
                if (!method.isDefault()) throw new AssertionError("new method must be default");
                added++;
            } else {
                if (!Modifier.isAbstract(method.getModifiers())) {
                    throw new AssertionError("original declaration changed");
                }
                original++;
            }
        }
        check(original == 4 && added == (baseline ? 0 : 4), "four original scheduler descriptors");
    }

    @SuppressWarnings("try")
    private static void oneShotAndScopes() throws Exception {
        Probe worker = new Probe();
        Probe caller = new Probe();
        Probe task = new Probe();
        Probe nested = new Probe();
        try (RecordingScheduler scheduler = new RecordingScheduler(1, worker);
             ProgressMonitors.Scope ignored = ProgressMonitors.bind(caller)) {
            ScheduledFuture<?> runnable = schedule(scheduler, () -> {
                check(ProgressMonitors.current() == task, "explicit monitor reaches runnable");
                try (ProgressMonitors.Scope inner = ProgressMonitors.bind(nested)) {
                    check(ProgressMonitors.current() == nested, "nested scope enters");
                }
                check(ProgressMonitors.current() == task, "nested scope restores");
                task.worked(7);
            }, -37, TimeUnit.NANOSECONDS, task);
            scheduler.recorded(runnable, "runnable", -37, 0, TimeUnit.NANOSECONDS);
            check(ProgressMonitors.current() == caller, "submission does not bind caller");
            check(get(runnable) == null, "runnable result remains null");
            check(task.units.get() == 7, "task progress retained");
            check(get(scheduler.submit(ProgressMonitors::current)) == worker,
                    "reused worker scope restored after success");
            Object result = new Object();
            ScheduledFuture<Object> callable = schedule(scheduler, () -> {
                check(ProgressMonitors.current() == task, "explicit monitor reaches callable");
                return result;
            }, 0, TimeUnit.MICROSECONDS, task);
            scheduler.recorded(callable, "callable", 0, 0, TimeUnit.MICROSECONDS);
            check(get(callable) == result, "callable result identity");
            check(get(schedule(scheduler, (Callable<Object>) () -> null,
                    0, TimeUnit.SECONDS, task)) == null, "null callable result");
            check(get(scheduler.submit(ProgressMonitors::current)) == worker,
                    "reused worker scope restored after callable");
            lifecycleUntouched(task);
            lifecycleUntouched(caller);
            lifecycleUntouched(worker);
        }
    }

    private static void exceptionalCompletion() throws Exception {
        Probe worker = new Probe();
        Probe task = new Probe();
        try (RecordingScheduler scheduler = new RecordingScheduler(1, worker)) {
            IOException checked = new IOException("original checked cause");
            Future<Object> failed = schedule(scheduler, (Callable<Object>) () -> { throw checked; },
                    0, TimeUnit.SECONDS, task);
            cause(failed, checked);
            AssertionError fatal = new AssertionError("original Error");
            cause(schedule(scheduler, (Runnable) () -> { throw fatal; },
                    0, TimeUnit.SECONDS, task), fatal);
            CancellationException explicit = new CancellationException("body cancellation");
            Future<?> bodyCancel = schedule(scheduler, (Runnable) () -> { throw explicit; },
                    0, TimeUnit.SECONDS, task);
            cause(bodyCancel, explicit);
            check(!bodyCancel.isCancelled(), "body cancellation is exceptional completion");
            check(get(scheduler.submit(ProgressMonitors::current)) == worker,
                    "worker scope restored after exceptions");
            task.cancelled.set(true);
            AtomicInteger bodies = new AtomicInteger();
            Future<?> pre = schedule(scheduler, (Runnable) bodies::incrementAndGet,
                    0, TimeUnit.SECONDS, task);
            cancellationCause(pre);
            check(bodies.get() == 0, "pre-cancel skips body");
            check(!pre.isCancelled(), "monitor cancellation does not cancel future state");
            task.cancelled.set(false);
            Future<?> post = schedule(scheduler, (Runnable) () -> task.cancelled.set(true),
                    0, TimeUnit.SECONDS, task);
            cancellationCause(post);
            check(!post.isCancelled(), "post-body cancellation remains exceptional");
            check(get(scheduler.submit(ProgressMonitors::current)) == worker,
                    "worker scope restored after cancellation");
            lifecycleUntouched(task);
        }
    }

    private static void cancellationBeforeExecution() throws Exception {
        Probe monitor = new Probe();
        AtomicInteger bodies = new AtomicInteger();
        try (RecordingScheduler scheduler = new RecordingScheduler(1, new Probe())) {
            Future<?> future = schedule(scheduler, (Runnable) bodies::incrementAndGet,
                    1, TimeUnit.DAYS, monitor);
            check(future.cancel(false), "pending future cancellation succeeds");
            expect(CancellationException.class, () -> get(future));
            check(future.isDone() && future.isCancelled(), "pending future terminal state");
            check(bodies.get() == 0 && monitor.checks.get() == 0,
                    "pending cancellation does not execute or observe borrowed monitor");
            check(!monitor.cancelled.get(), "pending future cancel does not cancel monitor");
            lifecycleUntouched(monitor);
        }
    }

    private static void cancellationWhileRunning(boolean interrupt) throws Exception {
        Probe monitor = new Probe();
        Probe worker = new Probe();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch leftBody = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        try (RecordingScheduler scheduler = new RecordingScheduler(1, worker)) {
            Future<?> future = schedule(scheduler, (Runnable) () -> {
                check(ProgressMonitors.current() == monitor, "running task scope");
                entered.countDown();
                try {
                    if (!release.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                        throw new AssertionError("worker release timeout");
                    }
                } catch (InterruptedException cancelled) {
                    interrupted.set(true);
                    Thread.currentThread().interrupt();
                } finally {
                    leftBody.countDown();
                }
            }, 0, TimeUnit.SECONDS, monitor);
            await(entered);
            check(future.cancel(interrupt), "running future cancellation succeeds");
            release.countDown();
            await(leftBody);
            expect(CancellationException.class, () -> get(future));
            check(interrupted.get() == interrupt, "requested interrupt policy retained");
            check(!monitor.cancelled.get(), "running future cancel does not cancel monitor");
            check(get(scheduler.submit(ProgressMonitors::current)) == worker,
                    "scope restores after running cancellation");
            lifecycleUntouched(monitor);
        } finally {
            release.countDown();
        }
    }

    private static void childAndStatusIdentity() throws Exception {
        Probe parent = new Probe();
        ProgressMonitor child = parent.child("bounded child", 12, 4);
        Object status = new Object();
        ProgressJob<Object> job = new ProgressJob<>() {
            @Override protected Object run(ProgressMonitor monitor) {
                check(monitor == child, "ProgressJob receives caller child");
                monitor.worked(2);
                return status;
            }
        };
        try (RecordingScheduler scheduler = new RecordingScheduler(1, new Probe())) {
            check(get(schedule(scheduler, job, 0, TimeUnit.SECONDS, child)) == status,
                    "ProgressJob status identity");
            check(parent.units.get() == 6, "task does not complete borrowed child budget");
            lifecycleUntouched(parent);
            child.done();
            child.done();
            check(parent.units.get() == 12, "caller completes child once");
            lifecycleUntouched(parent);
            ProgressMonitor cancelledChild = parent.child("cancel child", 3, 3);
            cancelledChild.cancel();
            cancellationCause(schedule(scheduler, (Runnable) () -> {
                throw new AssertionError("canceled child body");
            }, 0, TimeUnit.SECONDS, cancelledChild));
            check(!parent.cancelled.get(), "child cancellation remains local");
            check(parent.units.get() == 12, "canceled borrowed child not completed");
        }
    }

    private static void periodic(boolean fixedRate, boolean cooperative) throws Exception {
        Probe monitor = new Probe();
        Probe worker = new Probe();
        AtomicInteger runs = new AtomicInteger();
        AtomicInteger active = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        RuntimeException stop = new RuntimeException("periodic terminal cause");
        try (RecordingScheduler scheduler = new RecordingScheduler(2, worker)) {
            Runnable body = () -> {
                check(active.incrementAndGet() == 1, "periodic executions do not overlap");
                int count = runs.incrementAndGet();
                try {
                    check(ProgressMonitors.current() == monitor, "each periodic invocation is bound");
                    monitor.worked(1);
                    if (count == 1) {
                        entered.countDown();
                        awaitUnchecked(release);
                    }
                    if (count == 3) {
                        if (cooperative) monitor.cancelled.set(true);
                        else throw stop;
                    }
                } finally {
                    active.decrementAndGet();
                }
            };
            ScheduledFuture<?> future = periodic(scheduler, body, -11, 1,
                    TimeUnit.NANOSECONDS, monitor, fixedRate);
            scheduler.recorded(future, fixedRate ? "rate" : "delay",
                    -11, 1, TimeUnit.NANOSECONDS);
            await(entered);
            check(!future.isDone(), "successful periodic execution is not terminal");
            check(get(scheduler.submit(() -> active.get())) == 1,
                    "other worker progresses while first periodic invocation waits");
            release.countDown();
            if (cooperative) cancellationCause(future);
            else cause(future, stop);
            check(future.isDone() && !future.isCancelled(), "periodic failure state retained");
            get(scheduler.submit(() -> { }));
            check(runs.get() == 3 && active.get() == 0, "periodic successor executions suppressed");
            check(monitor.units.get() == 3, "periodic progress remains cumulative");
            lifecycleUntouched(monitor);
            check(get(scheduler.submit(ProgressMonitors::current)) == worker,
                    "periodic worker scope restored");
        } finally {
            release.countDown();
        }
    }

    private static void refusalAndShutdown() throws Exception {
        Probe monitor = new Probe();
        Runnable task = () -> { throw new AssertionError("refused task body"); };
        try (RecordingScheduler scheduler = new RecordingScheduler(1, new Probe())) {
            expect(NullPointerException.class,
                    () -> schedule(scheduler, (Runnable) null, 0, TimeUnit.SECONDS, monitor));
            expect(NullPointerException.class,
                    () -> schedule(scheduler, (Callable<Object>) null, 0, TimeUnit.SECONDS, monitor));
            expect(NullPointerException.class,
                    () -> schedule(scheduler, task, 0, TimeUnit.SECONDS, null));
            expect(NullPointerException.class,
                    () -> schedule(scheduler, task, 0, null, monitor));
            for (boolean rate : new boolean[] {false, true}) {
                expect(IllegalArgumentException.class,
                        () -> periodic(scheduler, task, 0, 0, TimeUnit.SECONDS, monitor, rate));
                expect(IllegalArgumentException.class,
                        () -> periodic(scheduler, task, 0, -1, TimeUnit.SECONDS, monitor, rate));
                expect(NullPointerException.class,
                        () -> periodic(scheduler, null, 0, 1, TimeUnit.SECONDS, monitor, rate));
                expect(NullPointerException.class,
                        () -> periodic(scheduler, task, 0, 1, TimeUnit.SECONDS, null, rate));
            }
            scheduler.shutdown();
            expect(RejectedExecutionException.class,
                    () -> schedule(scheduler, task, 0, TimeUnit.SECONDS, monitor));
            expect(RejectedExecutionException.class,
                    () -> schedule(scheduler, (Callable<Object>) () -> null, 0, TimeUnit.SECONDS, monitor));
            expect(RejectedExecutionException.class,
                    () -> periodic(scheduler, task, 0, 1, TimeUnit.SECONDS, monitor, true));
            expect(RejectedExecutionException.class,
                    () -> periodic(scheduler, task, 0, 1, TimeUnit.SECONDS, monitor, false));
            check(monitor.checks.get() == 0, "rejection and invalid inputs do not observe monitor");
            lifecycleUntouched(monitor);
        }
    }

    private static ScheduledFuture<?> schedule(ScheduledExecutorService scheduler,
            Runnable task, long delay, TimeUnit unit, ProgressMonitor monitor) {
        if (!baseline) return scheduler.scheduleMonitored(task, delay, unit, monitor);
        Objects.requireNonNull(task);
        Objects.requireNonNull(monitor);
        return scheduler.schedule((Runnable) () -> ProgressMonitors.run(task, monitor), delay, unit);
    }

    private static <V> ScheduledFuture<V> schedule(ScheduledExecutorService scheduler,
            Callable<V> task, long delay, TimeUnit unit, ProgressMonitor monitor) {
        if (!baseline) return scheduler.scheduleMonitored(task, delay, unit, monitor);
        Objects.requireNonNull(task);
        Objects.requireNonNull(monitor);
        return scheduler.schedule((Callable<V>) () -> ProgressMonitors.call(task, monitor), delay, unit);
    }

    private static ScheduledFuture<?> periodic(ScheduledExecutorService scheduler,
            Runnable task, long initial, long period, TimeUnit unit,
            ProgressMonitor monitor, boolean fixedRate) {
        if (!baseline) {
            return fixedRate
                    ? scheduler.scheduleAtFixedRateMonitored(task, initial, period, unit, monitor)
                    : scheduler.scheduleWithFixedDelayMonitored(task, initial, period, unit, monitor);
        }
        Objects.requireNonNull(task);
        Objects.requireNonNull(monitor);
        Runnable scoped = () -> ProgressMonitors.run(task, monitor);
        return fixedRate ? scheduler.scheduleAtFixedRate(scoped, initial, period, unit)
                : scheduler.scheduleWithFixedDelay(scoped, initial, period, unit);
    }

    private static final class Probe implements ProgressMonitor {
        final AtomicInteger begins = new AtomicInteger();
        final AtomicInteger dones = new AtomicInteger();
        final AtomicInteger cancels = new AtomicInteger();
        final AtomicInteger checks = new AtomicInteger();
        final AtomicLong units = new AtomicLong();
        final AtomicBoolean cancelled = new AtomicBoolean();
        @Override public void begin(String name, long total) { begins.incrementAndGet(); }
        @Override public void worked(long value) { units.addAndGet(value); }
        @Override public void done() { dones.incrementAndGet(); }
        @Override public void cancel() { cancels.incrementAndGet(); cancelled.set(true); }
        @Override public boolean isCancelled() { checks.incrementAndGet(); return cancelled.get(); }
    }

    private static final class RecordingScheduler extends ScheduledThreadPoolExecutor {
        private volatile ScheduledFuture<?> last;
        private String kind;
        private long first;
        private long second;
        private TimeUnit unit;

        RecordingScheduler(int threads, ProgressMonitor worker) {
            super(threads, workerFactory(worker));
            setRemoveOnCancelPolicy(true);
            setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        }

        @SuppressWarnings("try")
        private static ThreadFactory workerFactory(ProgressMonitor monitor) {
            return command -> new Thread(() -> {
                try (ProgressMonitors.Scope ignored = ProgressMonitors.bind(monitor)) {
                    command.run();
                }
            }, "monitored-scheduler-contract-worker");
        }

        private <V> ScheduledFuture<V> retain(ScheduledFuture<V> future,
                String operation, long a, long b, TimeUnit timeUnit) {
            kind = operation;
            first = a;
            second = b;
            unit = timeUnit;
            last = future;
            return future;
        }

        @Override public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return retain(super.schedule(command, delay, unit), "runnable", delay, 0, unit);
        }
        @Override public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
            return retain(super.schedule(task, delay, unit), "callable", delay, 0, unit);
        }
        @Override public ScheduledFuture<?> scheduleAtFixedRate(
                Runnable command, long initial, long period, TimeUnit unit) {
            return retain(super.scheduleAtFixedRate(command, initial, period, unit),
                    "rate", initial, period, unit);
        }
        @Override public ScheduledFuture<?> scheduleWithFixedDelay(
                Runnable command, long initial, long delay, TimeUnit unit) {
            return retain(super.scheduleWithFixedDelay(command, initial, delay, unit),
                    "delay", initial, delay, unit);
        }

        void recorded(ScheduledFuture<?> returned, String operation,
                long a, long b, TimeUnit expectedUnit) {
            check(returned == last, "actual scheduler future identity retained");
            check(kind.equals(operation) && first == a && second == b && unit == expectedUnit,
                    "actual scheduler operation and timing parameters retained");
        }

        @Override public void close() {
            shutdownNow();
            try {
                if (!awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)) {
                    throw new AssertionError("owned test scheduler did not terminate");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
        }
    }

    private static void lifecycleUntouched(Probe monitor) {
        check(monitor.begins.get() == 0 && monitor.dones.get() == 0 && monitor.cancels.get() == 0,
                "borrowed monitor lifecycle untouched");
    }
    private static <V> V get(Future<V> future) throws Exception {
        return future.get(WAIT_SECONDS, TimeUnit.SECONDS);
    }
    private static void cause(Future<?> future, Throwable expected) throws Exception {
        ExecutionException failure = expect(ExecutionException.class, () -> get(future));
        check(failure.getCause() == expected, "original exception identity retained");
    }
    private static void cancellationCause(Future<?> future) throws Exception {
        ExecutionException failure = expect(ExecutionException.class, () -> get(future));
        check(failure.getCause() instanceof CancellationException, "cooperative cancellation cause");
    }
    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(WAIT_SECONDS, TimeUnit.SECONDS)) throw new AssertionError("latch timeout");
    }
    private static void awaitUnchecked(CountDownLatch latch) {
        try { await(latch); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
    private static void check(boolean condition, String message) {
        CHECKS.incrementAndGet();
        if (!condition) throw new AssertionError(message);
    }
    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private static <T extends Throwable> T expect(Class<T> type, CheckedAction action) throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getName() + ", got " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("missing " + type.getName());
    }
}
