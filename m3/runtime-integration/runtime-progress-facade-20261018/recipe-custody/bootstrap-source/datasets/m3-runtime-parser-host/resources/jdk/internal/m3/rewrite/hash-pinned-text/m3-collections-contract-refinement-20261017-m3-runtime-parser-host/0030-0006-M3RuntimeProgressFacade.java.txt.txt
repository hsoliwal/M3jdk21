/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Runtime progress delegates preserve scope, child, provider and executor ownership
 * @run main/othervm/timeout=120 M3RuntimeProgressFacade
 */
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class M3RuntimeProgressFacade {
    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final AtomicInteger CHECKS = new AtomicInteger();

    private M3RuntimeProgressFacade() { }

    private static class Counting implements ProgressMonitor {
        final AtomicLong work = new AtomicLong();
        final AtomicInteger begins = new AtomicInteger();
        final AtomicInteger completions = new AtomicInteger();
        final AtomicBoolean cancelled = new AtomicBoolean();
        final List<String> messages = new CopyOnWriteArrayList<>();
        @Override public void begin(String task, long total) { begins.incrementAndGet(); }
        @Override public void worked(long units) { work.addAndGet(units); }
        @Override public void message(String text) { messages.add(text); }
        @Override public boolean isCancelled() { return cancelled.get(); }
        @Override public void cancel() { cancelled.set(true); }
        @Override public void done() { completions.incrementAndGet(); }
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }

    private static void check(boolean truth, String reason) {
        CHECKS.incrementAndGet();
        if (!truth) throw new AssertionError(reason);
    }

    private static Throwable failure(Class<? extends Throwable> type, Action action) {
        try {
            action.run();
        } catch (Throwable actual) {
            check(type.isInstance(actual), "unexpected failure: " + actual);
            return actual;
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static void sameFailure(Throwable expected, Action action) {
        check(failure(expected.getClass(), action) == expected, "exception identity");
    }

    @SuppressWarnings("try")
    private static void lexicalAndGlobalSelection() throws Exception {
        Counting global = new Counting();
        AtomicInteger providerCalls = new AtomicInteger();
        ProgressMonitors.setSystem(() -> { providerCalls.incrementAndGet(); return global; });
        check(RUNTIME.progressMonitor() == global, "global monitor identity");
        check(RUNTIME.progressMonitor() == global, "global monitor repeated identity");
        check(providerCalls.get() == 2, "provider evaluated per unbound query");
        Counting outer = new Counting();
        Counting inner = new Counting();
        try (ProgressMonitors.Scope scope = ProgressMonitors.bind(outer)) {
            check(RUNTIME.progressMonitor() == outer, "lexical monitor takes precedence");
            try (ProgressMonitors.Scope nested = ProgressMonitors.bind(inner)) {
                check(RUNTIME.progressMonitor() == inner, "nested lexical monitor");
            }
            check(RUNTIME.progressMonitor() == outer, "outer binding restored");
            try (ProgressMonitors.Scope none = ProgressMonitors.bind(ProgressMonitor.NONE)) {
                check(RUNTIME.progressMonitor() == ProgressMonitor.NONE, "bound NONE is authoritative");
            }
            CompletableFuture<ProgressMonitor> observed = new CompletableFuture<>();
            Thread thread = new Thread(() -> observed.complete(RUNTIME.progressMonitor()),
                    "m3-runtime-unbound-observer");
            thread.start();
            check(observed.get(10, TimeUnit.SECONDS) == global, "lexical binding is not inherited");
            thread.join(10_000);
            check(!thread.isAlive(), "observer terminated");
        }
        check(providerCalls.get() == 3, "only the unbound thread evaluated the provider");
        check(RUNTIME.progressMonitor() == global, "global restored after scopes");
        check(outer.completions.get() == 0 && inner.completions.get() == 0
                && global.completions.get() == 0, "borrowed monitors not completed");
        check(outer.begins.get() == 0 && inner.begins.get() == 0 && global.begins.get() == 0,
                "selection does not begin work");
        ProgressMonitors.setSystem(() -> null);
        check(RUNTIME.progressMonitor() == ProgressMonitor.NONE, "null fallback normalizes to NONE");
        ProgressMonitors.resetSystem();
        check(RUNTIME.progressMonitor() == ProgressMonitor.NONE, "reset fallback");
    }

    @SuppressWarnings("try")
    private static void childLifecycle() {
        Counting parent = new Counting();
        Counting replacement = new Counting();
        ProgressMonitor child;
        String title = new String("three steps");
        try (ProgressMonitors.Scope scope = ProgressMonitors.bind(parent)) {
            child = RUNTIME.subProgressMonitor(title, 12, 3);
            check(RUNTIME.progressMonitor() == parent, "child creation does not replace binding");
        }
        ProgressMonitors.setSystem(() -> replacement);
        check(parent.messages.size() == 1 && parent.messages.getFirst() == title,
                "task description reaches selected parent unchanged");
        child.worked(1);
        check(parent.work.get() == 4, "weighted child first third");
        child.worked(1);
        check(parent.work.get() == 8, "weighted child second third");
        child.done();
        child.done();
        child.worked(100);
        check(parent.work.get() == 12, "remaining budget credited once and capped");
        check(parent.completions.get() == 0 && parent.begins.get() == 0,
                "borrowed parent lifecycle retained");
        check(replacement.work.get() == 0, "provider change does not rebind existing child");

        try (ProgressMonitors.Scope scope = ProgressMonitors.bind(parent)) {
            ProgressMonitor unknown = RUNTIME.subProgressMonitor(null, 5, ProgressMonitor.UNKNOWN_WORK);
            unknown.worked(Long.MAX_VALUE);
            check(parent.work.get() == 12, "unknown work waits for completion");
            unknown.done();
            unknown.done();
            check(parent.work.get() == 17, "unknown work completion budget");
            ProgressMonitor zero = RUNTIME.subProgressMonitor("", 0, 1);
            zero.worked(1);
            zero.done();
            check(parent.work.get() == 17, "zero budget");
            check(parent.messages.size() == 1, "null and empty descriptions have no message callback");
        }
        Counting large = new Counting();
        try (ProgressMonitors.Scope scope = ProgressMonitors.bind(large)) {
            ProgressMonitor huge = RUNTIME.subProgressMonitor(null, Long.MAX_VALUE, 3);
            huge.worked(1);
            check(large.work.get() == Long.MAX_VALUE / 3, "overflow-safe weighting");
            huge.worked(Long.MAX_VALUE);
            huge.done();
            check(large.work.get() == Long.MAX_VALUE, "overshoot saturation without overflow");
        }
        ProgressMonitors.resetSystem();
        check(RUNTIME.subProgressMonitor("none", 10, 3) == ProgressMonitor.NONE, "NONE child identity");
    }

    @SuppressWarnings("try")
    private static void rangeAndProviderFailures() {
        AtomicInteger calls = new AtomicInteger();
        Counting parent = new Counting();
        ProgressMonitors.setSystem(() -> { calls.incrementAndGet(); return parent; });
        long[][] invalid = {{-1, 1}, {Long.MIN_VALUE, 1}, {0, 0}, {1, -2}, {1, Long.MIN_VALUE}};
        for (long[] pair : invalid) {
            int before = calls.get();
            Throwable direct = failure(IllegalArgumentException.class,
                    () -> ProgressMonitors.child(parent, "bad", pair[0], pair[1]));
            Throwable facade = failure(IllegalArgumentException.class,
                    () -> RUNTIME.subProgressMonitor("bad", pair[0], pair[1]));
            check(direct.getMessage().equals(facade.getMessage()), "factory range failure preserved");
            check(calls.get() == before + 1, "invalid child still resolves current monitor once");
        }
        check(parent.messages.isEmpty() && parent.work.get() == 0, "range rejection has no parent callbacks");
        ProgressMonitors.resetSystem();
        failure(IllegalArgumentException.class, () -> RUNTIME.subProgressMonitor(null, -1, 1));
        failure(IllegalArgumentException.class, () -> RUNTIME.subProgressMonitor(null, 1, 0));

        RuntimeException providerFailure = new RuntimeException("provider failure");
        ProgressMonitors.setSystem(() -> { throw providerFailure; });
        check(Runtime.getRuntime() == RUNTIME && RUNTIME.availableProcessors() > 0,
                "legacy Runtime calls do not evaluate a progress provider");
        sameFailure(providerFailure, () -> RUNTIME.progressMonitor());
        sameFailure(providerFailure, () -> RUNTIME.subProgressMonitor("bad", -1, 0));
        try (ProgressMonitors.Scope scope = ProgressMonitors.bind(parent)) {
            check(RUNTIME.progressMonitor() == parent, "scope bypasses failed process provider");
            RUNTIME.subProgressMonitor(null, 0, 1).done();
        }
        RuntimeException callbackFailure = new RuntimeException("message failure");
        Counting throwingParent = new Counting() {
            @Override public void message(String text) { throw callbackFailure; }
        };
        ProgressMonitors.setSystem(() -> throwingParent);
        sameFailure(callbackFailure, () -> RUNTIME.subProgressMonitor("message", 1, 1));
        check(throwingParent.work.get() == 0 && throwingParent.completions.get() == 0,
                "message failure does not credit or finish parent");
        ProgressMonitors.resetSystem();
    }

    @SuppressWarnings("try")
    private static void cancellation() {
        Counting parent = new Counting();
        try (ProgressMonitors.Scope scope = ProgressMonitors.bind(parent)) {
            ProgressMonitor child = RUNTIME.subProgressMonitor(null, 0, 1);
            child.cancel();
            check(child.isCancelled() && !parent.isCancelled(), "local child cancellation");
            failure(CancellationException.class, child::checkCancelled);
            ProgressMonitor inherited = RUNTIME.subProgressMonitor(null, 0, 1);
            parent.cancel();
            check(inherited.isCancelled(), "parent cancellation observed by child");
            failure(CancellationException.class, inherited::checkCancelled);
            check(parent.completions.get() == 0, "cancellation does not finish borrowed parent");
        }
        ProgressMonitors.resetSystem();
        try {
            Thread.currentThread().interrupt();
            failure(CancellationException.class, () -> RUNTIME.progressMonitor().checkCancelled());
            check(Thread.currentThread().isInterrupted(), "cancellation check preserves interrupt status");
        } finally {
            Thread.interrupted();
        }
    }

    @SuppressWarnings("try")
    private static void executorPropagation() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Counting parent = new Counting();
        Counting fallback = new Counting();
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<ProgressMonitor> selected = new CompletableFuture<>();
        try {
            try (ProgressMonitors.Scope scope = ProgressMonitors.bind(parent)) {
                pool.executeWithProgress(() -> {
                    try {
                        if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("release timeout");
                        selected.complete(RUNTIME.progressMonitor());
                    } catch (Throwable failed) {
                        selected.completeExceptionally(failed);
                    }
                });
            }
            ProgressMonitors.setSystem(() -> fallback);
            release.countDown();
            check(selected.get(10, TimeUnit.SECONDS) == parent, "executor captures monitor at submission");
            check(pool.submit((Callable<ProgressMonitor>) RUNTIME::progressMonitor).get(10, TimeUnit.SECONDS)
                    == fallback, "plain submission observes worker fallback after restoration");
            ProgressMonitor child;
            try (ProgressMonitors.Scope scope = ProgressMonitors.bind(parent)) {
                child = RUNTIME.subProgressMonitor("worker", 12, 3);
            }
            Future<Integer> answer = pool.submitMonitored((Callable<Integer>) () -> {
                check(RUNTIME.progressMonitor() == child, "explicit child bound in existing executor overload");
                RUNTIME.progressMonitor().worked(1);
                return 42;
            }, child);
            check(answer.get(10, TimeUnit.SECONDS) == 42 && parent.work.get() == 4,
                    "monitored result and weighted child report");
            check(parent.completions.get() == 0, "executor borrows parent lifecycle");
            child.done();
            check(parent.work.get() == 12 && parent.completions.get() == 0, "caller completes child only");
            Exception bodyFailure = new Exception("checked task failure");
            Future<Integer> failed = pool.submitMonitored((Callable<Integer>) () -> { throw bodyFailure; }, parent);
            Throwable wrapped = failure(ExecutionException.class, () -> failed.get(10, TimeUnit.SECONDS));
            check(wrapped.getCause() == bodyFailure, "existing Future checked-failure cause identity");
            check(pool.submit((Callable<ProgressMonitor>) RUNTIME::progressMonitor).get(10, TimeUnit.SECONDS)
                    == fallback, "worker scope restored after task failure");
            check(parent.completions.get() == 0, "failed executor task leaves borrowed completion to owner");
        } finally {
            release.countDown();
            pool.shutdownNow();
            check(pool.awaitTermination(10, TimeUnit.SECONDS), "executor terminated");
            ProgressMonitors.resetSystem();
        }
    }

    @SuppressWarnings("try")
    private static void concurrentChildAccounting() throws Exception {
        Counting parent = new Counting();
        ProgressMonitor child;
        try (ProgressMonitors.Scope scope = ProgressMonitors.bind(parent)) {
            child = RUNTIME.subProgressMonitor(null, 30_000, 3_000);
        }
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> work = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                work.add(pool.submit(() -> {
                    for (int step = 0; step < 750; step++) child.worked(1);
                }));
            }
            for (Future<?> completed : work) completed.get(10, TimeUnit.SECONDS);
            child.done();
            child.done();
            check(parent.work.get() == 30_000, "concurrent updates credit exactly the bounded budget");
            check(parent.completions.get() == 0, "concurrent child never completes parent");
            failure(IllegalArgumentException.class, () -> child.worked(-1));
            check(parent.work.get() == 30_000, "negative work after completion retains factory refusal");
        } finally {
            pool.shutdownNow();
            check(pool.awaitTermination(10, TimeUnit.SECONDS), "concurrent pool terminated");
        }
    }

    public static void main(String[] args) throws Exception {
        Supplier<? extends ProgressMonitor> original = ProgressMonitors.setSystem(() -> ProgressMonitor.NONE);
        try {
            lexicalAndGlobalSelection();
            childLifecycle();
            rangeAndProviderFailures();
            cancellation();
            executorPropagation();
            concurrentChildAccounting();
        } finally {
            ProgressMonitors.setSystem(original);
        }
        System.out.println("M3_RUNTIME_PROGRESS_FACADE_PASS checks=" + CHECKS.get());
    }
}
