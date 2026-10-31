/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class M3ObjectProgressStandaloneProbe {
    private static void check(boolean result, String name) {
        if (!result) throw new AssertionError(name);
    }
    static final class Counter implements ProgressMonitor {
        final AtomicLong ticks = new AtomicLong();
        volatile boolean cancelled;
        public void worked(long n) { ticks.addAndGet(n); }
        public boolean isCancelled() { return cancelled; }
    }
    public static void main(String[] args) throws Exception {
        Counter root = new Counter();
        ProgressMonitor child = root.child("parse", 12L, 3L);
        child.worked(1L);
        check(root.ticks.get() == 4L, "weighted-third");
        child.worked(2L);
        child.done(); child.done();
        check(root.ticks.get() == 12L, "weighted-complete-once");
        Counter huge = new Counter();
        ProgressMonitor large = huge.child("huge", Long.MAX_VALUE, 3L);
        large.worked(1L);
        check(huge.ticks.get() == Long.MAX_VALUE / 3L, "overflow-safe-scale");
        large.worked(2L); large.done();
        check(huge.ticks.get() == Long.MAX_VALUE, "overflow-safe-completion");
        check(ProgressMonitors.current() == ProgressMonitor.NONE, "default");
        try (ProgressMonitors.Scope s = ProgressMonitors.bind(root)) {
            check(ProgressMonitors.current() == root, "active");
            try (ProgressMonitors.Scope inner = ProgressMonitors.bind(child)) {
                check(ProgressMonitors.current() == child, "nested");
            }
            check(ProgressMonitors.current() == root, "restored");
            ProgressMonitors.run(() -> check(ProgressMonitors.current() == root, "run"), root);
        }
        check(ProgressMonitors.current() == ProgressMonitor.NONE, "no scope leak");
        root.cancelled = true;
        try {
            ProgressMonitors.run(() -> { throw new AssertionError("executed after cancellation"); }, root);
            throw new AssertionError("cancellation was ignored");
        } catch (CancellationException expected) { }
        System.out.println("PASS M3 standalone progress subset");
    }
}
