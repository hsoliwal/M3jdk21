/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only
 */
/*
 * @test
 * @summary M3 typed self-adaptation, monitor scopes and executor overloads
 * @run main M3AdapterProgress
 */
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

public class M3AdapterProgress {
    static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
    static final class Counting implements ProgressMonitor {
        final AtomicLong work = new AtomicLong();
        volatile boolean cancelled;
        @Override public void worked(long count) { work.addAndGet(count); }
        @Override public boolean isCancelled() { return cancelled; }
    }
    public static void main(String[] args) throws Exception {
        Object original = "m3";
        check(original.toAdapter(CharSequence.class) == original, "assignable");
        check(original.toAdapter(Integer.class) == null, "nonassignable");
        try {
            original.toAdapter(null);
            throw new AssertionError("null type accepted");
        } catch (NullPointerException expected) { }
        Counting monitor = new Counting();
        ProgressMonitor child = monitor.child("work", 10, 4);
        child.worked(2);
        check(monitor.work.get() == 5, "half work");
        child.worked(2);
        child.done();
        child.done();
        check(monitor.work.get() == 10, "child done exactly once");
        try (ProgressMonitors.Scope scope = ProgressMonitors.bind(monitor)) {
            check(ProgressMonitors.current() == monitor, "scoped");
            Supplier<Integer> supplier = () -> {
                check(ProgressMonitors.current() == child, "supplier scope");
                return 7;
            };
            check(supplier.get(child) == 7, "supplier result");
            Function<Integer,Integer> f = value -> value + 1;
            check(f.apply(7, child) == 8, "function");
            check(ProgressMonitors.current() == monitor, "scope restored");
        }
        check(ProgressMonitors.current() == ProgressMonitor.NONE, "root restored");
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> answer = pool.submitMonitored(
                    (Callable<Integer>) () -> {
                        check(ProgressMonitors.current() == monitor, "worker monitor");
                        return 42;
                    }, monitor);
            check(answer.get() == 42, "answer");
        } finally {
            pool.shutdownNow();
        }
        monitor.cancelled = true;
        try {
            ((Runnable) () -> { }).run(monitor);
            throw new AssertionError("cancel ignored");
        } catch (CancellationException expected) { }
    }
}
