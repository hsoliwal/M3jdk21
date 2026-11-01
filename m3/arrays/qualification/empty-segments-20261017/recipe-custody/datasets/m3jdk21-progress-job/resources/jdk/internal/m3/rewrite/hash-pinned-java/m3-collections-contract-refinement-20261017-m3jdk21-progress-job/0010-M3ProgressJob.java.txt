/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary ProgressJob preserves result and exception identity through existing monitored Callable and FutureTask templates
 * @run main/othervm M3ProgressJob
 */

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.MonitoredCallable;
import java.util.concurrent.ProgressJob;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class M3ProgressJob {
    private static final AtomicInteger CHECKS = new AtomicInteger();

    private M3ProgressJob() { }

    private static void check(boolean condition, String message) {
        CHECKS.incrementAndGet();
        if (!condition) throw new AssertionError(message);
    }

    private static final class Monitor implements ProgressMonitor {
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicLong work = new AtomicLong();
        final AtomicInteger begins = new AtomicInteger();
        final AtomicInteger finishes = new AtomicInteger();
        final CancellationException marker = new CancellationException("monitor marker");

        @Override public boolean isCancelled() { return cancelled.get(); }
        @Override public void cancel() { cancelled.set(true); }
        @Override public void begin(String name, long total) { begins.incrementAndGet(); }
        @Override public void worked(long units) { work.addAndGet(units); }
        @Override public void done() { finishes.incrementAndGet(); }
        @Override public void checkCancelled() {
            if (isCancelled()) throw marker;
            ProgressMonitor.super.checkCancelled();
        }
    }

    private static <S> ProgressJob<S> constant(S value) {
        return new ProgressJob<>() {
            @Override protected S run(ProgressMonitor monitor) { return value; }
        };
    }

    private static void signatures() throws Exception {
        check(ProgressJob.class.getClassLoader() == null, "JDK-owned class");
        check(ProgressJob.class.getModule() == Object.class.getModule(), "java.base owner");
        check(Modifier.isAbstract(ProgressJob.class.getModifiers()), "abstract class");
        check(ProgressJob.class.getDeclaredFields().length == 0, "no task or monitor fields");
        check(ProgressJob.class.getInterfaces().length == 1
                && ProgressJob.class.getInterfaces()[0] == MonitoredCallable.class, "one existing template owner");
        check(Modifier.isProtected(ProgressJob.class.getDeclaredConstructor().getModifiers()), "protected constructor");
        var run = ProgressJob.class.getDeclaredMethod("run", ProgressMonitor.class);
        check(Modifier.isProtected(run.getModifiers()) && Modifier.isAbstract(run.getModifiers()), "protected abstract hook");
        check(run.getExceptionTypes().length == 1 && run.getExceptionTypes()[0] == Exception.class, "checked hook contract");
        for (var method : new java.lang.reflect.Method[] {
                ProgressJob.class.getDeclaredMethod("call"),
                ProgressJob.class.getDeclaredMethod("callMonitored", ProgressMonitor.class)}) {
            check(Modifier.isPublic(method.getModifiers()) && Modifier.isFinal(method.getModifiers()), "final public template " + method);
        }
        check(Callable.class.getMethod("call").getExceptionTypes()[0] == Exception.class, "Callable still permits checked exceptions");
        check(Runnable.class.getMethod("run").getReturnType() == void.class, "Runnable remains void");
    }

    private static void valuesAndScopes(Monitor global) throws Exception {
        Object status = new Object();
        Monitor outer = new Monitor();
        Monitor supplied = new Monitor();
        ProgressMonitors.Scope scope = ProgressMonitors.bind(outer);
        try {
            ProgressJob<Object> job = new ProgressJob<>() {
                @Override protected Object run(ProgressMonitor monitor) {
                    check(monitor == supplied, "explicit monitor identity");
                    check(ProgressMonitors.current() == supplied, "explicit lexical binding");
                    monitor.worked(3);
                    return status;
                }
            };
            check(job.call(supplied) == status, "result identity");
            check(ProgressMonitors.current() == outer, "outer scope restored after success");
            check(supplied.work.get() == 3, "body can report progress");
            check(supplied.begins.get() == 0 && supplied.finishes.get() == 0, "borrowed lifecycle retained");
            check(constant(null).call() == null, "null result preserved");
            ProgressJob<ProgressMonitor> direct = new ProgressJob<>() {
                @Override protected ProgressMonitor run(ProgressMonitor monitor) {
                    check(ProgressMonitors.current() == outer, "direct body does not bind");
                    return monitor;
                }
            };
            check(direct.call() == outer, "current scope inherited");
            supplied.cancel();
            check(direct.callMonitored(supplied) == supplied, "direct body does not check cancellation");
            check(direct.callMonitored(null) == null, "direct body forwards null unchanged");
        } finally {
            scope.close();
        }
        check(ProgressMonitors.current() == global, "global restored after scope");
        ProgressJob<ProgressMonitor> fallback = new ProgressJob<>() {
            @Override protected ProgressMonitor run(ProgressMonitor monitor) { return monitor; }
        };
        check(fallback.call() == global, "system provider fallback identity");
        Supplier<? extends ProgressMonitor> previous = ProgressMonitors.setSystem(() -> null);
        try {
            check(fallback.call() == ProgressMonitor.NONE, "null provider value uses NONE");
        } finally {
            ProgressMonitors.setSystem(previous);
        }
    }

    private static void exceptionsAndCancellation(Monitor global) throws Exception {
        Monitor monitor = new Monitor();
        IOException checked = new IOException("checked identity");
        ProgressJob<Object> failure = new ProgressJob<>() {
            @Override protected Object run(ProgressMonitor current) throws IOException {
                check(current == monitor, "exception path monitor");
                monitor.cancel();
                throw checked;
            }
        };
        try {
            failure.call(monitor);
            throw new AssertionError("expected checked exception");
        } catch (IOException actual) {
            check(actual == checked, "checked exception retained even if body cancels then throws");
        }
        check(ProgressMonitors.current() == global, "scope restored after checked exception");
        check(monitor.finishes.get() == 0, "failure does not finish borrowed monitor");
        RuntimeException runtime = new IllegalArgumentException("runtime identity");
        Error error = new AssertionError("error identity");
        for (Throwable expected : new Throwable[] { runtime, error }) {
            ProgressJob<Object> task = new ProgressJob<>() {
                @Override protected Object run(ProgressMonitor ignored) {
                    if (expected instanceof RuntimeException exception) throw exception;
                    throw (Error) expected;
                }
            };
            try {
                task.call(ProgressMonitor.NONE);
                throw new AssertionError("expected throwable");
            } catch (Throwable actual) {
                check(actual == expected, "unchecked throwable identity");
            }
        }
        AtomicInteger bodies = new AtomicInteger();
        ProgressJob<Object> success = new ProgressJob<>() {
            @Override protected Object run(ProgressMonitor ignored) { bodies.incrementAndGet(); return new Object(); }
        };
        try {
            success.call(monitor);
            throw new AssertionError("expected pre-cancellation");
        } catch (CancellationException actual) {
            check(actual == monitor.marker, "pre-cancellation exception identity");
        }
        check(bodies.get() == 0, "pre-cancellation skips body");
        Monitor after = new Monitor();
        ProgressJob<Object> cancels = new ProgressJob<>() {
            @Override protected Object run(ProgressMonitor current) {
                bodies.incrementAndGet(); current.cancel(); return new Object();
            }
        };
        try {
            cancels.call(after);
            throw new AssertionError("expected post-cancellation");
        } catch (CancellationException actual) {
            check(actual == after.marker, "post-cancellation replaces returned value by existing contract");
        }
        check(bodies.get() == 1, "post-cancellation follows one body execution");
        check(after.finishes.get() == 0, "cancellation does not finish borrowed monitor");
        check(ProgressMonitors.current() == global, "scope restored after cancellations");
        try {
            success.call(null);
            throw new AssertionError("expected null-monitor rejection");
        } catch (NullPointerException expected) {
            check(bodies.get() == 1, "null monitor rejected before body");
        }
        Thread.currentThread().interrupt();
        try {
            success.call(ProgressMonitor.NONE);
            throw new AssertionError("expected interruption cancellation");
        } catch (CancellationException expected) {
            check(Thread.currentThread().isInterrupted(), "interrupt status preserved");
            check(bodies.get() == 1, "interruption skips body");
        } finally {
            check(Thread.interrupted(), "test clears its own interruption");
        }
    }

    private static void expectedFutureFailure(Future<?> future, Throwable expected) throws Exception {
        try {
            future.get(15, TimeUnit.SECONDS);
            throw new AssertionError("expected exceptional completion");
        } catch (ExecutionException actual) {
            check(actual.getCause() == expected, "Future.get cause identity");
        }
        check(future.isDone(), "future completes exceptionally");
        check(!future.isCancelled(), "body-thrown cancellation is exceptional completion, not Future.cancel");
    }

    private static void executors(Monitor global) throws Exception {
        Executor executor = Runnable::run;
        Object result = new Object();
        Monitor monitor = new Monitor();
        ProgressJob<Object> job = constant(result);
        FutureTask<Object> future = new FutureTask<>(() -> job.call(monitor));
        executor.execute(future);
        check(future.get(15, TimeUnit.SECONDS) == result, "ordinary Executor/FutureTask result identity");
        IOException failure = new IOException("future checked identity");
        ProgressJob<Object> throwsChecked = new ProgressJob<>() {
            @Override protected Object run(ProgressMonitor current) throws IOException {
                check(current == monitor, "future exception monitor identity");
                throw failure;
            }
        };
        FutureTask<Object> failed = new FutureTask<>(() -> throwsChecked.call(monitor));
        executor.execute(failed);
        expectedFutureFailure(failed, failure);
        monitor.cancel();
        FutureTask<Object> preCancelled = new FutureTask<>(() -> job.call(monitor));
        executor.execute(preCancelled);
        expectedFutureFailure(preCancelled, monitor.marker);
        check(ProgressMonitors.current() == global, "direct Executor scope restored");

        FutureTask<Object> outside = new FutureTask<>(job);
        try {
            executor.execute(outside, monitor);
            throw new AssertionError("expected outer-wrapper cancellation");
        } catch (CancellationException actual) {
            check(actual == monitor.marker, "outer-wrapper counterexample observed");
            check(!outside.isDone(), "outer wrapper can skip FutureTask.run; bind inside future instead");
        } finally {
            outside.cancel(false);
        }

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Monitor supplied = new Monitor();
            ProgressJob<ProgressMonitor> sees = new ProgressJob<>() {
                @Override protected ProgressMonitor run(ProgressMonitor current) {
                    check(current == ProgressMonitors.current(), "pool lexical monitor");
                    return current;
                }
            };
            check(pool.submitMonitored(sees, supplied).get(15, TimeUnit.SECONDS) == supplied, "existing submitMonitored result");
            check(pool.submit((Callable<ProgressMonitor>) ProgressMonitors::current).get(15, TimeUnit.SECONDS) == global,
                    "reused worker restored after successful call");
            monitor.cancelled.set(false);
            expectedFutureFailure(pool.submitMonitored(throwsChecked, monitor), failure);
            monitor.cancel();
            expectedFutureFailure(pool.submitMonitored(job, monitor), monitor.marker);
            check(pool.submit((Callable<ProgressMonitor>) ProgressMonitors::current).get(15, TimeUnit.SECONDS) == global,
                    "reused worker restored after exceptional calls");
            ProgressMonitors.Scope scope = ProgressMonitors.bind(supplied);
            try {
                check(pool.submit(sees).get(15, TimeUnit.SECONDS) == global, "ordinary submit does not capture caller scope");
            } finally {
                scope.close();
            }
            check(supplied.finishes.get() == 0, "pool retains borrowed monitor ownership");
        } finally {
            pool.shutdownNow();
            check(pool.awaitTermination(15, TimeUnit.SECONDS), "single worker terminates");
        }
    }

    private static void concurrentBindings(Monitor global) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch entered = new CountDownLatch(2);
        ProgressJob<ProgressMonitor> shared = new ProgressJob<>() {
            @Override protected ProgressMonitor run(ProgressMonitor monitor) throws InterruptedException {
                entered.countDown();
                check(entered.await(15, TimeUnit.SECONDS), "both calls entered concurrently");
                check(ProgressMonitors.current() == monitor, "independent concurrent lexical binding");
                monitor.worked(1);
                return monitor;
            }
        };
        Monitor first = new Monitor();
        Monitor second = new Monitor();
        try {
            Future<ProgressMonitor> a = pool.submitMonitored(shared, first);
            Future<ProgressMonitor> b = pool.submitMonitored(shared, second);
            check(a.get(15, TimeUnit.SECONDS) == first, "first concurrent result identity");
            check(b.get(15, TimeUnit.SECONDS) == second, "second concurrent result identity");
            check(first.work.get() == 1 && second.work.get() == 1, "independent progress reports");
            check(first.finishes.get() == 0 && second.finishes.get() == 0, "concurrent calls borrow monitors");
            check(ProgressMonitors.current() == global, "caller scope unchanged by workers");
        } finally {
            pool.shutdownNow();
            check(pool.awaitTermination(15, TimeUnit.SECONDS), "concurrent workers terminate");
        }
    }

    public static void main(String[] args) throws Exception {
        Monitor global = new Monitor();
        Supplier<? extends ProgressMonitor> previous = ProgressMonitors.setSystem(() -> global);
        try {
            signatures();
            valuesAndScopes(global);
            exceptionsAndCancellation(global);
            executors(global);
            concurrentBindings(global);
            check(global.begins.get() == 0 && global.finishes.get() == 0, "global monitor remains borrowed");
        } finally {
            ProgressMonitors.setSystem(previous);
        }
        System.out.println("PASS ProgressJob checks=" + CHECKS.get()
                + "; new API=PENDING; Object universal compatibility=HOLD; STRING phase unchanged");
    }
}
