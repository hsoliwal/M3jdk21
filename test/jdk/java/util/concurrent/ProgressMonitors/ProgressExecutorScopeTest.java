/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Borrowed progress scopes cross ordinary ExecutorService and scheduled worker boundaries
 * @run main/othervm ProgressExecutorScopeTest
 * @run main/othervm -Xint ProgressExecutorScopeTest
 */

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ProgressJob;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressExecutorScopeTest {
    private static int assertions;

    private static final class Probe implements ProgressMonitor {
        final AtomicInteger checks = new AtomicInteger();
        final AtomicInteger completions = new AtomicInteger();
        final AtomicInteger cancellations = new AtomicInteger();

        @Override public void checkCancelled() {
            check(ProgressMonitors.current() == this, "monitor bound at check");
            checks.incrementAndGet();
            ProgressMonitor.super.checkCancelled();
        }
        @Override public void cancel() { cancellations.incrementAndGet(); }
        @Override public void done() { completions.incrementAndGet(); }
    }

    public static void main(String[] args) throws Exception {
        ProgressMonitor fallback = ProgressMonitors.current();
        capturedTasks(fallback);
        explicitScope(fallback);
        exceptionsAndQueuedCancellation(fallback);
        delayedAndRejected(fallback);
        check(ProgressMonitors.current() == fallback, "caller restored");
        System.out.println("PROGRESS_EXECUTOR_SCOPE_PASS assertions=" + assertions);
    }

    private static void capturedTasks(ProgressMonitor fallback) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<?> blocked = pool.submit(() -> {
                entered.countDown();
                release.await();
                return null;
            });
            check(entered.await(5, TimeUnit.SECONDS), "worker entered");
            Probe monitor = new Probe();
            Future<?> runnable;
            Future<Integer> callable;
            Future<String> job;
            final ProgressMonitors.Scope scope = ProgressMonitors.bind(monitor);
            try (scope) {
                runnable = pool.submit(ProgressMonitors.wrap((Runnable) () ->
                        check(ProgressMonitors.current() == monitor, "Runnable binding")));
                callable = pool.submit(ProgressMonitors.wrap((Callable<Integer>) () -> {
                    check(ProgressMonitors.current() == monitor, "Callable binding");
                    return 21;
                }));
                ProgressJob<String> template = new ProgressJob<>() {
                    @Override protected String run(ProgressMonitor owner) {
                        check(owner == monitor, "ProgressJob binding");
                        return "job";
                    }
                };
                job = pool.submit(ProgressMonitors.wrap(template));
            }
            check(ProgressMonitors.current() == fallback, "capture does not retain caller scope");
            release.countDown();
            blocked.get(5, TimeUnit.SECONDS);
            runnable.get(5, TimeUnit.SECONDS);
            check(callable.get(5, TimeUnit.SECONDS) == 21, "Callable result");
            check("job".equals(job.get(5, TimeUnit.SECONDS)), "ProgressJob result");
            check(pool.submit((Callable<ProgressMonitor>) ProgressMonitors::current)
                    .get(5, TimeUnit.SECONDS) == fallback, "worker scope restored");
            check(monitor.checks.get() == 8, "cancellation checks include ProgressJob template");
            check(monitor.completions.get() == 0, "borrowed monitor not done");
            check(monitor.cancellations.get() == 0, "borrowed monitor not cancelled");
        } finally {
            release.countDown();
            pool.shutdownNow();
            check(pool.awaitTermination(5, TimeUnit.SECONDS), "worker terminates");
        }
    }

    private static void explicitScope(ProgressMonitor fallback) throws Exception {
        Probe parent = new Probe();
        Probe child = new Probe();
        final ProgressMonitors.Scope scope = ProgressMonitors.bind(parent);
        try (scope) {
            ProgressMonitors.wrap((Runnable) () ->
                    check(ProgressMonitors.current() == child, "explicit Runnable"), child).run();
            String result = ProgressMonitors.wrap((Callable<String>) () -> {
                check(ProgressMonitors.current() == child, "explicit Callable");
                return "ok";
            }, child).call();
            check("ok".equals(result), "explicit result");
            check(ProgressMonitors.current() == parent, "nested scope restored");
            try {
                ProgressMonitors.wrap((Callable<?>) null);
                throw new AssertionError("null callable accepted");
            } catch (NullPointerException expected) {
                check("task".equals(expected.getMessage()), "task checked before capture");
            }
        }
        check(ProgressMonitors.current() == fallback, "outer scope restored");
        check(child.completions.get() == 0, "explicit child borrowed");
    }

    private static void exceptionsAndQueuedCancellation(ProgressMonitor fallback) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Probe owner = new Probe();
            IOException problem = new IOException("checked");
            Future<?> failed = pool.submit(ProgressMonitors.wrap((Callable<Object>) () -> {
                throw problem;
            }, owner));
            try {
                failed.get(5, TimeUnit.SECONDS);
                throw new AssertionError("checked exception missing");
            } catch (ExecutionException expected) {
                check(expected.getCause() == problem, "original exception identity");
            }
            check(pool.submit((Callable<ProgressMonitor>) ProgressMonitors::current)
                    .get(5, TimeUnit.SECONDS) == fallback, "failure restores scope");
            Future<?> blocker = pool.submit(() -> {
                blocked.countDown();
                release.await();
                return null;
            });
            check(blocked.await(5, TimeUnit.SECONDS), "blocker entered");
            AtomicBoolean invoked = new AtomicBoolean();
            Future<?> queued = pool.submit(ProgressMonitors.wrap(
                    (Runnable) () -> invoked.set(true), owner));
            check(queued.cancel(false), "queued Future cancelled");
            release.countDown();
            blocker.get(5, TimeUnit.SECONDS);
            try {
                queued.get();
                throw new AssertionError("cancelled Future completed");
            } catch (CancellationException expected) {
                check(!invoked.get(), "cancelled task not invoked");
            }
            check(owner.completions.get() == 0, "future never completes borrowed monitor");
            check(owner.cancellations.get() == 0, "future never cancels borrowed monitor");
        } finally {
            release.countDown();
            pool.shutdownNow();
            check(pool.awaitTermination(5, TimeUnit.SECONDS), "worker termination");
        }
    }

    private static void delayedAndRejected(ProgressMonitor fallback) throws Exception {
        ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
        Probe owner = new Probe();
        try {
            Callable<String> task;
            final ProgressMonitors.Scope scope = ProgressMonitors.bind(owner);
            try (scope) {
                task = ProgressMonitors.wrap((Callable<String>) () -> {
                    check(ProgressMonitors.current() == owner, "scheduled binding");
                    return "scheduled";
                });
            }
            check("scheduled".equals(pool.schedule(task, 0, TimeUnit.MILLISECONDS)
                    .get(5, TimeUnit.SECONDS)), "scheduled result");
            check(pool.submit((Callable<ProgressMonitor>) ProgressMonitors::current)
                    .get(5, TimeUnit.SECONDS) == fallback, "scheduled worker restored");
        } finally {
            pool.shutdownNow();
            check(pool.awaitTermination(5, TimeUnit.SECONDS), "scheduled termination");
        }
        ExecutorService rejected = Executors.newSingleThreadExecutor();
        rejected.shutdown();
        try {
            rejected.execute(ProgressMonitors.wrap((Runnable) () ->
                    { throw new AssertionError("rejected body ran"); }, owner));
            throw new AssertionError("rejection lost");
        } catch (RejectedExecutionException expected) {
            check(owner.completions.get() == 0 && owner.cancellations.get() == 0,
                    "rejection cannot mutate borrowed monitor");
        }
    }

    private static void check(boolean ok, String label) {
        assertions++;
        if (!ok) throw new AssertionError(label);
    }
}
