// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Exact percentage and callback contracts over the complete valid long-work domain. */
public final class M3ProgressAdaptersPercentageSelfTest {
    private static final BigInteger HUNDRED = BigInteger.valueOf(100L);

    private M3ProgressAdaptersPercentageSelfTest() { }

    public static void main(String[] args) throws Exception {
        System.out.println("PROGRESS_PERCENT_CONTRACT_PASS checks=" + verify());
    }

    public static long verify() throws Exception {
        M3DefaultProgressMonitor completed = new M3DefaultProgressMonitor();
        completed.setProgress(Long.MAX_VALUE, Long.MAX_VALUE);
        int observed = M3ProgressAdapters.asLegacy(completed).getPercentComplete();
        System.out.println("PROGRESS_PERCENT_WITNESS worked=" + Long.MAX_VALUE
                + " total=" + Long.MAX_VALUE + " expected=100 actual=" + observed);
        equal(100, observed, "full long-range completion");

        Fixture fixture = new Fixture();
        long[] values = {0L, 1L, 2L, 3L, 7L, 99L, 100L, 101L, Integer.MAX_VALUE,
                (1L << 53) - 1L, 1L << 53, (1L << 53) + 1L,
                Long.MAX_VALUE / 100L - 1L, Long.MAX_VALUE / 100L,
                Long.MAX_VALUE / 100L + 1L, Long.MAX_VALUE / 2L,
                Long.MAX_VALUE - 1L, Long.MAX_VALUE};
        for (long worked : values) {
            fixture.check(worked, -1L);
            fixture.check(worked, 0L);
            for (long total : values) if (total > 0L) fixture.check(worked, total);
        }
        for (long total : values) {
            if (total <= 0L) continue;
            BigInteger denominator = BigInteger.valueOf(total);
            for (int percent = 0; percent <= 100; percent++) {
                long threshold = denominator.multiply(BigInteger.valueOf(percent))
                        .add(BigInteger.valueOf(99L)).divide(HUNDRED).longValueExact();
                if (threshold > 0L) fixture.check(threshold - 1L, total);
                fixture.check(threshold, total);
                if (threshold < Long.MAX_VALUE) fixture.check(threshold + 1L, total);
            }
        }

        SplittableRandom random = new SplittableRandom(0x3eecda17L);
        for (int i = 0; i < 20_000; i++) {
            long total = random.nextLong() & Long.MAX_VALUE;
            fixture.check(random.nextLong() & Long.MAX_VALUE, total);
            fixture.check(random.nextLong(Long.MAX_VALUE / 100L + 1L), total);
        }
        callbackFailuresAndIdentity();
        rejectedStates();

        // Each reader owns its observation tape; the adapter calculation has no shared state.
        try (ExecutorService workers = Executors.newFixedThreadPool(4)) {
            List<Callable<Long>> tasks = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                final int workerId = worker;
                tasks.add(() -> {
                    Fixture reader = new Fixture();
                    SplittableRandom samples = new SplittableRandom(0x17beefL + workerId);
                    for (int i = 0; i < 2_000; i++) {
                        reader.check(samples.nextLong() & Long.MAX_VALUE,
                                samples.nextLong() & Long.MAX_VALUE);
                    }
                    return reader.checks;
                });
            }
            for (Future<Long> result : workers.invokeAll(tasks)) fixture.checks += result.get();
        }
        return fixture.checks;
    }

    private static void callbackFailuresAndIdentity() {
        M3Progress original = M3Progress.of(100);
        if (M3ProgressAdapters.asLegacy(original) != original
                || M3ProgressAdapters.asLegacy(M3Progress.NULL) != M3Progress.NULL) {
            throw new AssertionError("existing legacy instances must retain identity");
        }
        expect(NullPointerException.class, () -> M3ProgressAdapters.asLegacy(null));

        StateMonitor monitor = new StateMonitor();
        M3Progress legacy = M3ProgressAdapters.asLegacy(monitor);
        IllegalStateException failure = new IllegalStateException("state callback sentinel");
        monitor.failure = failure;
        try {
            legacy.getPercentComplete();
            throw new AssertionError("state callback failure must propagate");
        } catch (IllegalStateException actual) {
            if (actual != failure) throw new AssertionError("state failure identity changed");
        }
        equal(1, monitor.calls, "failed state callback count");
        monitor.failure = null;
        monitor.next = null;
        monitor.calls = 0;
        expect(NullPointerException.class, legacy::getPercentComplete);
        equal(1, monitor.calls, "null state callback count");

        monitor.next = state(Long.MAX_VALUE, Long.MAX_VALUE);
        equal(Integer.MAX_VALUE, legacy.getWorkedSoFar(), "legacy worked saturation");
        equal(Integer.MAX_VALUE, legacy.getTotalWork(), "legacy total saturation");
        if (legacy.state() != monitor.next) throw new AssertionError("state identity changed");
        M3Progress.Snapshot snapshot = legacy.snapshot();
        equal(Integer.MAX_VALUE, snapshot.worked(), "legacy snapshot worked saturation");
        equal(Integer.MAX_VALUE, snapshot.total(), "legacy snapshot total saturation");
    }

    private static void rejectedStates() {
        expect(IllegalArgumentException.class, () -> state(-1L, 1L));
        expect(IllegalArgumentException.class, () -> state(0L, -2L));
        expect(IllegalArgumentException.class, () ->
                new M3ProgressState("", "", 0L, 1L, false, false, -1L, 0L));
        expect(IllegalArgumentException.class, () ->
                new M3ProgressState("", "", 0L, 1L, false, false, 0L, -1L));
    }

    private static M3ProgressState state(long worked, long total) {
        return new M3ProgressState("task", "detail", worked, total, false, false, 0L, 0L);
    }

    private static int oracle(long worked, long total) {
        return total <= 0L ? 0 : BigInteger.valueOf(worked).multiply(HUNDRED)
                .divide(BigInteger.valueOf(total)).min(HUNDRED).intValueExact();
    }

    private static void equal(long expected, long actual, String operation) {
        if (actual != expected) throw new AssertionError(operation + ": expected="
                + expected + " actual=" + actual);
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected failure", failure);
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static final class Fixture {
        private final StateMonitor monitor = new StateMonitor();
        private final M3Progress legacy = M3ProgressAdapters.asLegacy(monitor);
        private long checks;

        void check(long worked, long total) {
            monitor.next = state(worked, total);
            monitor.calls = 0;
            int expected = oracle(worked, total);
            int actual = legacy.getPercentComplete();
            equal(expected, actual, "percentage worked=" + worked + " total=" + total);
            equal(1, monitor.calls, "single state observation");
            if (actual < 0 || actual > 100) throw new AssertionError("percentage outside [0,100]");
            if (total > 0L && worked <= Long.MAX_VALUE / 100L) {
                equal(Math.min(100L, worked * 100L / total), actual,
                        "unchanged non-overflow legacy result");
            }
            checks++;
        }
    }

    private static final class StateMonitor implements M3ProgressMonitor {
        private M3ProgressState next = M3ProgressAdaptersPercentageSelfTest.state(0L, 1L);
        private int calls;
        private RuntimeException failure;

        @Override public M3ProgressState state() {
            calls++;
            if (failure != null) throw failure;
            return next;
        }
        @Override public void beginTask(String name, long total) { unexpected(); }
        @Override public void done() { unexpected(); }
        @Override public void worked(long amount) { unexpected(); }
        @Override public void setProgress(long worked, long total) { unexpected(); }
        @Override public void setTaskName(String name) { unexpected(); }
        @Override public void subTask(String name) { unexpected(); }
        @Override public boolean isCanceled() { throw new AssertionError("unexpected cancellation callback"); }
        @Override public void setCanceled(boolean canceled) { unexpected(); }
        private static void unexpected() { throw new AssertionError("percentage must not mutate its monitor"); }
    }
}
