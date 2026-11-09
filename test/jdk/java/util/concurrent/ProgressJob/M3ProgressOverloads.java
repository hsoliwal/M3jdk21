/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Monitored result submission and child overloads preserve existing executor and progress ownership
 * @build M3LegacyExecutor
 * @run main/othervm M3ProgressOverloads
 */

import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.MonitoredCallable;
import java.util.concurrent.MonitoredRunnable;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class M3ProgressOverloads {
    private static final AtomicInteger CHECKS = new AtomicInteger();
    private static final long TIMEOUT_SECONDS = 15;

    private M3ProgressOverloads() { }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private static void check(boolean condition, String message) {
        CHECKS.incrementAndGet();
        if (!condition) throw new AssertionError(message);
    }

    private static <T extends Throwable> T expect(Class<T> type, Action action) {
        try {
            action.run();
        } catch (Throwable actual) {
            check(type.isInstance(actual), "expected " + type.getName() + ", got " + actual);
            return type.cast(actual);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static <T> T get(Future<T> future) throws Exception {
        return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static void failedFuture(Future<?> future, Throwable expected) {
        ExecutionException failure = expect(ExecutionException.class, () -> get(future));
        check(failure.getCause() == expected, "future preserves exception identity");
        check(future.isDone(), "exceptional future is complete");
        check(!future.isCancelled(), "thrown cancellation is not Future.cancel");
    }

    private static class Monitor implements ProgressMonitor {
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicLong work = new AtomicLong();
        final AtomicInteger begins = new AtomicInteger();
        final AtomicInteger finishes = new AtomicInteger();
        final AtomicInteger messages = new AtomicInteger();
        final CancellationException cancellation = new CancellationException("monitor cancellation");
        volatile String lastMessage;

        @Override public void begin(String name, long units) { begins.incrementAndGet(); }
        @Override public void worked(long units) { work.addAndGet(units); }
        @Override public void done() { finishes.incrementAndGet(); }
        @Override public boolean isCancelled() { return cancelled.get(); }
        @Override public void cancel() { cancelled.set(true); }
        @Override public void message(String text) {
            lastMessage = text;
            messages.incrementAndGet();
        }
        @Override public void checkCancelled() {
            if (isCancelled()) throw cancellation;
            ProgressMonitor.super.checkCancelled();
        }
    }

    private static final class ChildOwner extends Monitor {
        final ProgressMonitor result = new Monitor();
        int calls;
        String name;
        long ticks;
        long estimate;
        RuntimeException failure;

        @Override
        public ProgressMonitor child(String childName, long parentTicks, long totalWork) {
            calls++;
            name = childName;
            ticks = parentTicks;
            estimate = totalWork;
            if (failure != null) throw failure;
            return result;
        }
    }

    private static final class DualTask implements Runnable, Callable<Object> {
        int runs;
        int calls;
        final Object value = new Object();

        @Override public void run() { runs++; }
        @Override public Object call() { calls++; return value; }
    }

    private static void signatures() throws Exception {
        check(ExecutorService.class.getMethod("submitMonitored", Runnable.class,
                Object.class, ProgressMonitor.class).isDefault(), "submission is an additive default");
        check(ProgressMonitor.class.getMethod("child", long.class, long.class).isDefault(),
                "weighted child convenience is a default");
        check(ProgressMonitor.class.getMethod("child", String.class, long.class).isDefault(),
                "named unknown child convenience is a default");
        check(ProgressMonitor.class.getMethod("child", long.class).isDefault(),
                "unknown child convenience is a default");
        for (Class<?> sam : new Class<?>[] { Runnable.class, Callable.class,
                MonitoredRunnable.class, MonitoredCallable.class }) {
            long abstractMethods = java.util.Arrays.stream(sam.getMethods())
                    .filter(method -> Modifier.isAbstract(method.getModifiers())).count();
            check(abstractMethods == 1, "one abstract method remains in " + sam.getName());
        }
    }

    private static void legacyDispatch(Monitor global) throws Exception {
        M3LegacyExecutor legacy = new M3LegacyExecutor();
        ExecutorService executor = legacy;
        Monitor outer = new Monitor();
        Monitor supplied = new Monitor();
        Object result = new Object();
        AtomicInteger bodies = new AtomicInteger();
        ProgressMonitors.Scope scope = ProgressMonitors.bind(outer);
        try {
            Runnable body = () -> {
                check(ProgressMonitors.current() == supplied, "explicit monitor bound inside task");
                supplied.worked(2);
                bodies.incrementAndGet();
            };
            Future<Object> future = executor.submitMonitored(body, result, supplied);
            check(future == legacy.lastFuture, "actual executor future identity retained");
            check(legacy.lastResult == result, "result forwarded unchanged to legacy submit");
            check(legacy.submissions == 1 && legacy.executions == 1, "one legacy submit and execution");
            check(get(future) == result, "successful future result identity");
            check(bodies.get() == 1 && supplied.work.get() == 2, "body executed once");
            check(ProgressMonitors.current() == outer, "outer binding restored after success");
            check(supplied.begins.get() == 0 && supplied.finishes.get() == 0,
                    "submission borrows monitor lifecycle");

            Future<Object> nullFuture = executor.submitMonitored(() -> { }, null, supplied);
            check(nullFuture == legacy.lastFuture && get(nullFuture) == null, "null result preserved");
            int before = legacy.submissions;
            Future<Object> oldCall = executor.submit((Runnable) () -> { }, null);
            check(get(oldCall) == null && legacy.submissions == before + 1,
                    "old submit(Runnable,null) retains result overload resolution");

            int priorSubmissions = legacy.submissions;
            Future<?> oldMonitored = executor.submitMonitored((Runnable) () -> { }, supplied);
            check(get(oldMonitored) == null, "old monitored Runnable returns null");
            check(legacy.submissions == priorSubmissions,
                    "old monitored Runnable retains submit(Runnable) dispatch");

            DualTask dual = new DualTask();
            check(get(executor.submitMonitored(dual, result, supplied)) == result,
                    "dual-interface task accepted as Runnable by new arity");
            check(dual.runs == 1 && dual.calls == 0, "new overload executes run, not call");
            check(dual.call(supplied) == dual.value && dual.calls == 1, "Callable default still works");
            dual.run(supplied);
            check(dual.runs == 2, "Runnable default still works on dual implementation");

            Runnable runnableSam = () -> bodies.incrementAndGet();
            Callable<Object> callableSam = () -> result;
            MonitoredRunnable monitoredSam = monitor -> {
                check(monitor == supplied, "monitor-aware lambda sees borrowed monitor");
                bodies.incrementAndGet();
            };
            MonitoredCallable<Object> monitoredCall = monitor -> {
                check(monitor == supplied, "monitor-aware Callable lambda sees monitor");
                return result;
            };
            check(get(executor.submitMonitored(runnableSam, result, supplied)) == result,
                    "ordinary Runnable SAM compiles and executes");
            check(get(executor.submitMonitored(callableSam, supplied)) == result,
                    "existing Callable overload remains usable");
            check(get(executor.submitMonitored(monitoredSam, result, supplied)) == result,
                    "monitored Runnable SAM remains usable");
            check(get(executor.submitMonitored(monitoredCall, supplied)) == result,
                    "monitored Callable SAM remains usable");
            check(ProgressMonitors.current() == outer, "all nested dispatches restore outer binding");
        } finally {
            scope.close();
            executor.shutdown();
        }
        check(ProgressMonitors.current() == global, "legacy executor leaves caller global binding");
    }

    private static void failuresAndValidation(Monitor global) throws Exception {
        M3LegacyExecutor legacy = new M3LegacyExecutor();
        ExecutorService executor = legacy;
        Monitor supplied = new Monitor();
        Monitor outer = new Monitor();
        AtomicInteger bodies = new AtomicInteger();
        Object result = new Object();
        ProgressMonitors.Scope scope = ProgressMonitors.bind(outer);
        try {
            for (Throwable failure : new Throwable[] {
                    new IllegalArgumentException("body failure"), new AssertionError("body error") }) {
                Future<Object> future = executor.submitMonitored(() -> {
                    check(ProgressMonitors.current() == supplied, "failure path monitor bound");
                    if (failure instanceof RuntimeException exception) throw exception;
                    throw (Error) failure;
                }, result, supplied);
                failedFuture(future, failure);
                check(ProgressMonitors.current() == outer, "scope restored after body throwable");
            }

            Monitor before = new Monitor();
            before.cancel();
            Future<Object> preCancelled = executor.submitMonitored(
                    () -> bodies.incrementAndGet(), result, before);
            failedFuture(preCancelled, before.cancellation);
            check(bodies.get() == 0, "pre-cancellation skips body without stranding future");
            check(ProgressMonitors.current() == outer, "scope restored after pre-cancellation");

            Monitor after = new Monitor();
            Future<Object> postCancelled = executor.submitMonitored(() -> {
                bodies.incrementAndGet();
                after.cancel();
            }, result, after);
            failedFuture(postCancelled, after.cancellation);
            check(bodies.get() == 1, "post-cancellation follows body execution");
            check(ProgressMonitors.current() == outer, "scope restored after post-cancellation");

            Monitor throwsThenCancels = new Monitor();
            RuntimeException bodyFailure = new IllegalStateException("body failure wins");
            failedFuture(executor.submitMonitored(() -> {
                throwsThenCancels.cancel();
                throw bodyFailure;
            }, result, throwsThenCancels), bodyFailure);

            int submissions = legacy.submissions;
            expect(NullPointerException.class,
                    () -> executor.submitMonitored((Runnable) null, result, supplied));
            expect(NullPointerException.class,
                    () -> executor.submitMonitored(() -> bodies.incrementAndGet(), result, null));
            expect(NullPointerException.class,
                    () -> executor.submitMonitored((Runnable) null, result, null));
            check(legacy.submissions == submissions, "null guards run before executor submission");
            check(bodies.get() == 1, "invalid arguments never invoke task");

            Thread.currentThread().interrupt();
            try {
                Future<Object> interrupted = executor.submitMonitored(
                        () -> bodies.incrementAndGet(), result, ProgressMonitor.NONE);
                ExecutionException failure = expect(ExecutionException.class, () -> get(interrupted));
                check(failure.getCause() instanceof CancellationException,
                        "NONE still observes thread interruption inside future");
                check(interrupted.isDone() && !interrupted.isCancelled(),
                        "interrupt cancellation is recorded as exceptional completion");
                check(Thread.currentThread().isInterrupted(), "interrupt bit retained");
                check(bodies.get() == 1, "interruption skips body");
            } finally {
                check(Thread.interrupted(), "fixture clears its own interruption");
            }
            check(ProgressMonitors.current() == outer, "scope restored after interruption");

            int executions = legacy.executions;
            executor.shutdown();
            RejectedExecutionException rejection = expect(RejectedExecutionException.class,
                    () -> executor.submitMonitored(() -> bodies.incrementAndGet(), result, supplied));
            check(rejection == legacy.rejection, "executor rejection identity retained");
            check(legacy.executions == executions && bodies.get() == 1, "rejection does not execute body");
            check(ProgressMonitors.current() == outer, "rejection does not bind monitor");
            for (Monitor monitor : new Monitor[] { supplied, before, after, throwsThenCancels }) {
                check(monitor.begins.get() == 0 && monitor.finishes.get() == 0,
                        "failure paths retain borrowed lifecycle");
            }
        } finally {
            scope.close();
            executor.shutdownNow();
        }
        check(ProgressMonitors.current() == global, "failure fixture restores global binding");
    }

    private static void realExecutor(Monitor global) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Monitor supplied = new Monitor();
        Object result = new Object();
        try {
            Future<Object> future = pool.submitMonitored(() -> {
                check(ProgressMonitors.current() == supplied, "worker receives explicit monitor");
                supplied.worked(1);
            }, result, supplied);
            check(get(future) == result, "real pool returns supplied result");
            check(!future.cancel(false), "completed future cannot be retroactively cancelled");
            check(!supplied.isCancelled(), "future cancellation does not cancel borrowed monitor");
            check(get(pool.submit((Callable<ProgressMonitor>) ProgressMonitors::current)) == global,
                    "reused worker binding restored after success");

            RuntimeException failure = new IllegalStateException("worker failure");
            failedFuture(pool.submitMonitored(() -> { throw failure; }, result, supplied), failure);
            Monitor cancelled = new Monitor();
            cancelled.cancel();
            AtomicInteger skipped = new AtomicInteger();
            failedFuture(pool.submitMonitored(() -> skipped.incrementAndGet(), result, cancelled),
                    cancelled.cancellation);
            check(skipped.get() == 0, "real pool pre-cancellation skips body and completes future");
            check(get(pool.submit((Callable<ProgressMonitor>) ProgressMonitors::current)) == global,
                    "reused worker restored after exceptional submissions");

            Future<Void> blocker = pool.submit(() -> {
                entered.countDown();
                if (!release.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new AssertionError("blocker release timeout");
                }
                return null;
            });
            check(entered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "worker is deterministically occupied");
            Monitor queuedMonitor = new Monitor();
            AtomicInteger queuedBodies = new AtomicInteger();
            Future<Object> queued = pool.submitMonitored(
                    () -> queuedBodies.incrementAndGet(), result, queuedMonitor);
            check(queued.cancel(false), "queued future can be cancelled by its owner");
            check(queued.isDone() && queued.isCancelled(), "Future.cancel owns cancellation bit");
            expect(CancellationException.class, () -> get(queued));
            release.countDown();
            get(blocker);
            check(get(pool.submit((Callable<ProgressMonitor>) ProgressMonitors::current)) == global,
                    "worker drained cancelled task without leaking monitor");
            check(queuedBodies.get() == 0, "queued cancellation prevents body");
            check(!queuedMonitor.isCancelled() && queuedMonitor.begins.get() == 0
                    && queuedMonitor.finishes.get() == 0, "queued cancellation does not own monitor lifecycle");
            check(supplied.begins.get() == 0 && supplied.finishes.get() == 0,
                    "real pool borrows monitor");

            pool.shutdown();
            expect(RejectedExecutionException.class,
                    () -> pool.submitMonitored(() -> { }, result, supplied));
        } finally {
            release.countDown();
            pool.shutdownNow();
            check(pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS), "real worker terminates");
        }
    }

    private static void childDispatch() {
        ChildOwner owner = new ChildOwner();
        ProgressMonitor monitor = owner;
        check(monitor.child(7, 11) == owner.result, "weighted overload retains virtual result identity");
        check(owner.calls == 1 && owner.name == null && owner.ticks == 7 && owner.estimate == 11,
                "weighted overload delegates exact unnamed arguments");
        check(monitor.child("named", 5) == owner.result, "named overload retains virtual result identity");
        check(owner.calls == 2 && "named".equals(owner.name) && owner.ticks == 5
                && owner.estimate == ProgressMonitor.UNKNOWN_WORK, "named overload delegates unknown estimate");
        check(monitor.child(3) == owner.result, "unknown overload retains virtual result identity");
        check(owner.calls == 3 && owner.name == null && owner.ticks == 3
                && owner.estimate == ProgressMonitor.UNKNOWN_WORK, "unknown overload delegates exact arguments");
        check(monitor.child(null, 4) == owner.result && owner.name == null,
                "named overload preserves null label");
        check(monitor.child(-9) == owner.result && owner.ticks == -9,
                "overload leaves validation to authoritative full-method override");
        RuntimeException marker = new IllegalArgumentException("owner validation");
        owner.failure = marker;
        check(expect(IllegalArgumentException.class, () -> monitor.child(1, 2)) == marker,
                "weighted overload preserves owner failure identity");
        check(expect(IllegalArgumentException.class, () -> monitor.child("x", 1)) == marker,
                "named overload preserves owner failure identity");
        check(expect(IllegalArgumentException.class, () -> monitor.child(1)) == marker,
                "unknown overload preserves owner failure identity");
        check(owner.calls == 8, "each convenience calls full virtual method exactly once");
    }

    private static void childSemantics() {
        Monitor parent = new Monitor();
        ProgressMonitor first = parent.child("first", 5);
        ProgressMonitor second = parent.child(7);
        check(first != second, "independently owned children are distinct");
        check(parent.messages.get() == 1 && "first".equals(parent.lastMessage),
                "only named child forwards a creation message");
        first.worked(100);
        second.worked(Long.MAX_VALUE);
        check(parent.work.get() == 0, "unknown work receives no credit before completion");
        first.done();
        check(parent.work.get() == 5, "first child credits only its allocation");
        check(!second.isCancelled(), "finishing first child does not cancel sibling");
        second.done();
        check(parent.work.get() == 12, "second child remains independently completable");
        first.done();
        second.done();
        check(parent.work.get() == 12, "repeated child completion does not double-credit");
        check(parent.begins.get() == 0 && parent.finishes.get() == 0,
                "child completion never begins or completes borrowed parent");

        Monitor weightedParent = new Monitor();
        ProgressMonitor weighted = weightedParent.child(6, 4);
        weighted.worked(1);
        check(weightedParent.work.get() == 1, "weighted child rounds by existing full-method contract");
        weighted.worked(1);
        check(weightedParent.work.get() == 3, "weighted child accumulates exact allocation");
        weighted.worked(Long.MAX_VALUE);
        check(weightedParent.work.get() == 6, "weighted child saturates at allocated budget");
        weighted.done();
        check(weightedParent.work.get() == 6 && weightedParent.finishes.get() == 0,
                "weighted completion neither over-credits nor completes parent");
        expect(IllegalArgumentException.class, () -> weighted.worked(-1));

        Monitor wideParent = new Monitor();
        ProgressMonitor wide = wideParent.child(Long.MAX_VALUE, Long.MAX_VALUE);
        wide.worked(Long.MAX_VALUE - 1);
        check(wideParent.work.get() == Long.MAX_VALUE - 1, "large weighted multiplication does not overflow");
        wide.done();
        check(wideParent.work.get() == Long.MAX_VALUE, "large child completes exactly");

        Monitor cancellationParent = new Monitor();
        ProgressMonitor local = cancellationParent.child(2);
        ProgressMonitor sibling = cancellationParent.child("sibling", 3);
        ProgressMonitor zero = cancellationParent.child(0, 4);
        local.cancel();
        check(local.isCancelled() && !cancellationParent.isCancelled() && !sibling.isCancelled(),
                "child cancellation remains local");
        cancellationParent.cancel();
        check(sibling.isCancelled() && zero.isCancelled(), "parent cancellation reaches each child");
        expect(CancellationException.class, zero::checkCancelled);
        zero.worked(100);
        zero.done();
        check(cancellationParent.work.get() == 0, "zero allocation still cancels without work credit");

        expect(IllegalArgumentException.class, () -> parent.child(-1));
        expect(IllegalArgumentException.class, () -> parent.child("negative", -1));
        expect(IllegalArgumentException.class, () -> parent.child(-1, 1));
        expect(IllegalArgumentException.class, () -> parent.child(1, 0));
        expect(IllegalArgumentException.class, () -> parent.child(1, -2));
        check(ProgressMonitor.NONE.child(1) == ProgressMonitor.NONE, "unknown NONE child retains singleton");
        check(ProgressMonitor.NONE.child("none", 1) == ProgressMonitor.NONE,
                "named NONE child retains singleton");
        check(ProgressMonitor.NONE.child(1, 2) == ProgressMonitor.NONE,
                "weighted NONE child retains singleton");
        expect(IllegalArgumentException.class, () -> ProgressMonitor.NONE.child(-1));
        expect(IllegalArgumentException.class, () -> ProgressMonitor.NONE.child("invalid", -1));
        expect(IllegalArgumentException.class, () -> ProgressMonitor.NONE.child(1, 0));
    }

    public static void main(String[] args) throws Exception {
        Monitor global = new Monitor();
        Supplier<? extends ProgressMonitor> previous = ProgressMonitors.setSystem(() -> global);
        try {
            signatures();
            legacyDispatch(global);
            failuresAndValidation(global);
            realExecutor(global);
            childDispatch();
            childSemantics();
            check(global.begins.get() == 0 && global.finishes.get() == 0,
                    "global monitor lifecycle remains borrowed");
        } finally {
            ProgressMonitors.setSystem(previous);
        }
        System.out.println("PASS progress overload contracts checks=" + CHECKS.get());
    }
}
