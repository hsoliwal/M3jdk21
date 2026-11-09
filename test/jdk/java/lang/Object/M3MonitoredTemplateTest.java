/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only
 */
/*
 * @test
 * @summary M3 single-method template hooks, bound progress, cancellation and dual functional interfaces
 * @run main/othervm M3MonitoredTemplateTest
 */
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class M3MonitoredTemplateTest {
    static final class Counter implements ProgressMonitor {
        volatile boolean cancelled;
        final AtomicInteger units = new AtomicInteger();
        public void worked(long n) { units.addAndGet(Math.toIntExact(n)); }
        public boolean isCancelled() { return cancelled; }
    }

    /** Previously rejected by duplicate Callable/Runnable progressMonitor defaults. */
    static final class Both implements Runnable, Callable<Integer> {
        public void run() { }
        public Integer call() { return 7; }
    }

    static void check(boolean ok, String description) {
        if (!ok) throw new AssertionError(description);
    }

    public static void main(String[] args) throws Exception {
        Both both = new Both();
        check(both.call() == 7, "dual-interface compilation");
        both.run();

        Counter monitor = new Counter();
        MonitoredRunnable task = actual -> {
            check(actual == monitor, "runnable inherited monitor");
            actual.worked(1);
        };
        MonitoredCallable<Integer> compute = actual -> {
            check(actual == monitor, "callable inherited monitor");
            actual.worked(2);
            return 42;
        };
        ProgressMonitors.run(task, monitor);
        check(ProgressMonitors.call(compute, monitor) == 42, "callable result");
        check(monitor.units.get() == 3, "combined progress");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> future = executor.submit(() -> ProgressMonitors.call(compute, monitor));
            Future<?> follow = executor.submit(() -> ProgressMonitors.run(task, monitor));
            check(future.get() == 42, "async result");
            follow.get();
            check(monitor.units.get() == 6, "async progress");
        } finally {
            executor.shutdownNow();
        }

        ProgressMonitor zero = monitor.child("no parent work", 0L, 2L);
        monitor.cancelled = true;
        check(zero.isCancelled(), "zero-weight child inherits cancellation");
        try {
            ProgressMonitors.run(task, monitor);
            throw new AssertionError("run after cancellation");
        } catch (CancellationException expected) { }
        try {
            ProgressMonitors.call(compute, monitor);
            throw new AssertionError("call after cancellation");
        } catch (CancellationException expected) { }
        check(ProgressMonitors.current() == ProgressMonitor.NONE, "scope cleaned up");
    }
}
