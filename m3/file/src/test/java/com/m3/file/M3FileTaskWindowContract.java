// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Independent executable contracts for the actual production admission atom. */
public final class M3FileTaskWindowContract {
    private static int checks;
    private M3FileTaskWindowContract() { }
    public static void main(String[] args) throws Exception {
        geometry();
        orderedAndWrapped();
        failureBehindBlockedHead();
        exceptionIdentity();
        cancellation();
        nullResultsAndRootIdentity();
        interruptedCoordinator();
        closeStopsEveryTask();
        System.out.println("M3_FILE_TASK_WINDOW_PASS checks=" + checks);
    }

    private static void geometry() throws Exception {
        for (int n : new int[] {-1, 0, 257}) {
            expect(IllegalArgumentException.class, () -> new M3FileTaskWindow<>(n, "bad-"));
        }
        try (var window = new M3FileTaskWindow<Integer>(1, "geometry-")) {
            expect(IllegalStateException.class, window::row);
            expect(NullPointerException.class, () -> window.add(1, null));
            window.add(9, () -> 19);
            check(window.full(), "full at capacity");
            expect(IllegalStateException.class, () -> window.add(2, () -> 3));
            check(window.await(() -> { }) == 19 && window.row() == 9, "geometry value");
            window.remove();
            check(window.size() == 0, "empty after remove");
        }
    }

    private static void orderedAndWrapped() throws Exception {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        try (var window = new M3FileTaskWindow<Integer>(3, "order-")) {
            for (int batch = 0; batch < 100; batch++) {
                CountDownLatch ready = new CountDownLatch(3);
                CountDownLatch release = new CountDownLatch(1);
                for (int i = 0; i < 3; i++) {
                    int id = batch * 3 + i;
                    window.add(id, () -> {
                        checkWorker(Thread.currentThread().isVirtual(), "virtual worker");
                        peak.accumulateAndGet(active.incrementAndGet(), Math::max);
                        ready.countDown();
                        try { release.await(); return id * 7; }
                        finally { active.decrementAndGet(); }
                    });
                }
                await(ready, "three workers started");
                check(window.size() == 3, "bounded three tasks");
                release.countDown();
                for (int i = 0; i < 3; i++) {
                    int id = batch * 3 + i;
                    check(window.row() == id, "row encounter order");
                    check(window.await(() -> { }) == id * 7, "value encounter order");
                    window.remove();
                }
            }
        }
        check(peak.get() == 3 && active.get() == 0, "bounded concurrency and quiescence");
    }

    private static void failureBehindBlockedHead() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch allowFailure = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        IOException expected = new IOException("later worker failure");
        AtomicReference<Throwable> result = new AtomicReference<>();
        Thread coordinator = Thread.ofPlatform().start(() -> {
            try (var window = new M3FileTaskWindow<Integer>(2, "failure-")) {
                window.add(0, () -> {
                    firstStarted.countDown();
                    try { new CountDownLatch(1).await(); return 0; }
                    finally { stopped.countDown(); }
                });
                window.add(1, () -> { allowFailure.await(); throw expected; });
                allowFailure.countDown();
                window.await(() -> { });
                result.set(new AssertionError("failure not reported"));
            } catch (Throwable problem) { result.set(problem); }
            finally { finished.countDown(); }
        });
        try {
            await(firstStarted, "blocked head started");
            await(finished, "later failure interrupts blocked head without release");
            check(result.get() == expected, "original IOException identity");
            check(stopped.getCount() == 0, "head stopped before coordinator exit");
        } finally {
            coordinator.interrupt(); coordinator.join(3000);
        }
    }

    private static void exceptionIdentity() throws Exception {
        Throwable[] failures = {new IOException("io"), new IllegalArgumentException("runtime"),
                new AssertionError("error"), new Exception("checked")};
        for (Throwable expected : failures) {
            try (var window = new M3FileTaskWindow<Integer>(1, "identity-")) {
                window.add(0, () -> {
                    if (expected instanceof Error error) throw error;
                    throw (Exception) expected;
                });
                try { window.await(() -> { }); throw new AssertionError("missing failure"); }
                catch (Throwable actual) {
                    check(actual == expected || (expected.getClass() == Exception.class
                            && actual instanceof IOException && actual.getCause() == expected),
                            "worker exception identity or checked cause");
                }
            }
        }
    }

    private static void cancellation() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        InterruptedIOException expected = new InterruptedIOException("cancel probe");
        try (var window = new M3FileTaskWindow<Integer>(1, "cancel-")) {
            window.add(0, () -> { started.countDown(); try { new CountDownLatch(1).await(); return 0; }
                finally { stopped.countDown(); } });
            await(started, "cancellation worker starts");
            try { window.await(() -> { throw expected; }); throw new AssertionError("missing cancellation"); }
            catch (InterruptedIOException actual) { check(actual == expected, "cancellation identity"); }
        }
        check(stopped.getCount() == 0, "cancelled task quiescent after close");
    }

    private static void nullResultsAndRootIdentity() throws Exception {
        Path root = Path.of("original-provider-path");
        var window = new M3FileTaskWindow<String>(1, "root-");
        try (window) {
            window.add(root, () -> null);
            check(window.root() == root, "original Path reference retained");
            check(window.await(() -> { }) == null, "null callback result retained");
            window.remove();
        }
        window.close();
        check(window.size() == 0, "idempotent close");
        expect(IllegalStateException.class, () -> window.add(1, () -> "closed"));
    }

    private static void interruptedCoordinator() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Throwable> result = new AtomicReference<>();
        AtomicInteger interrupted = new AtomicInteger();
        Thread coordinator = Thread.ofPlatform().start(() -> {
            try (var window = new M3FileTaskWindow<Integer>(1, "interrupt-")) {
                window.add(0, () -> { started.countDown(); new CountDownLatch(1).await(); return 0; });
                window.await(() -> { });
            } catch (Throwable problem) {
                result.set(problem);
                if (Thread.currentThread().isInterrupted()) interrupted.incrementAndGet();
            }
        });
        await(started, "interrupt worker started");
        coordinator.interrupt(); coordinator.join(3000);
        check(!coordinator.isAlive(), "interrupted coordinator terminated");
        check(result.get() instanceof InterruptedIOException && interrupted.get() == 1,
                "interrupt exception and flag preserved");
    }

    private static void closeStopsEveryTask() throws Exception {
        CountDownLatch started = new CountDownLatch(8);
        CountDownLatch stopped = new CountDownLatch(8);
        try (var window = new M3FileTaskWindow<Integer>(8, "close-")) {
            for (int i = 0; i < 8; i++) window.add(i, () -> {
                started.countDown();
                try { new CountDownLatch(1).await(); return 0; }
                finally { stopped.countDown(); }
            });
            await(started, "all admitted tasks start");
        }
        check(stopped.getCount() == 0, "close cancels every admitted task");
    }

    private static void await(CountDownLatch gate, String message) throws InterruptedException {
        check(gate.await(3, TimeUnit.SECONDS), message);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void checkWorker(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void expect(Class<? extends Throwable> type, Checked action) throws Exception {
        try { action.run(); } catch (Throwable error) {
            check(type.isInstance(error), "expected " + type.getName() + ", got " + error); return;
        }
        throw new AssertionError("missing " + type.getName());
    }
}
