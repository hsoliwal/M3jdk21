// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3DefaultProgressMonitor;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** JDK Future/ScheduledFuture differential contracts with the existing M3 owner. */
@Timeout(30)
final class M3EclipseScheduledTasksTest {
    @Test
    void scheduledCallableReturnsValueAndCompletesOneOwner() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            var scheduled = M3EclipseScheduledTasks.schedule(
                    executor, owner, "index", 4, () -> {
                        owner.worked(3);
                        return 42;
                    }, 0, TimeUnit.MILLISECONDS);
            assertEquals(Integer.valueOf(42), scheduled.get(5, TimeUnit.SECONDS));
            assertEquals(0, scheduled.compareTo(scheduled));
            assertTrue(scheduled.isDone());
            assertFalse(scheduled.isCancelled());
            assertFalse(scheduled.cancel(true));
        }
        assertEquals("index", owner.state().taskName());
        assertEquals(3L, owner.state().worked());
        assertTrue(owner.state().done());
        assertFalse(owner.isCanceled());
    }

    @Test
    void runnableResultIsNullAndNegativeDelayUsesJdkImmediateSemantics() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            var scheduled = M3EclipseScheduledTasks.schedule(
                    executor, owner, "scan", 1, (Runnable) calls::incrementAndGet,
                    -1, TimeUnit.NANOSECONDS);
            assertNull(scheduled.get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, calls.get());
        assertTrue(owner.state().done());
    }

    @Test
    void scheduledStatusRetainsEclipseErrorAndCancelSemantics() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        var problem = new IllegalStateException("invalid AST");
        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            var result = M3EclipseScheduledTasks.scheduleStatus(
                    executor, owner, "build", 3, m -> { throw problem; },
                    0, TimeUnit.MILLISECONDS);
            IStatus status = result.get(5, TimeUnit.SECONDS);
            assertEquals(IStatus.ERROR, status.getSeverity());
            assertSame(problem, status.getException());
        }
        assertTrue(owner.state().done());

        var canceled = new M3DefaultProgressMonitor();
        canceled.setCanceled(true);
        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            var result = M3EclipseScheduledTasks.scheduleStatus(
                    executor, canceled, "cancelled", 1,
                    m -> { throw new AssertionError("callback must not run"); },
                    0, TimeUnit.MILLISECONDS);
            assertEquals(IStatus.CANCEL, result.get(5, TimeUnit.SECONDS).getSeverity());
        }
    }

    @Test
    void cancelBeforeStartLeavesProgressUnbegun() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.execute(() -> {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var scheduled = M3EclipseScheduledTasks.schedule(
                    executor, owner, "not-started", 1, () -> 7,
                    0, TimeUnit.MILLISECONDS);
            assertTrue(scheduled.cancel(false));
            assertThrows(CancellationException.class, scheduled::get);
            assertTrue(owner.isCanceled());
            assertEquals(0L, owner.state().generation());
            assertFalse(owner.state().done());
            release.countDown();
        } finally {
            release.countDown();
        }
    }

    @Test
    void cancelRunningTaskInterruptsWorkerAndCompletesOwner() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        var entered = new CountDownLatch(1);
        var finished = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            var scheduled = M3EclipseScheduledTasks.schedule(
                    executor, owner, "wait", 1, () -> {
                        entered.countDown();
                        try {
                            new CountDownLatch(1).await();
                            return 1;
                        } finally {
                            finished.countDown();
                        }
                    }, 0, TimeUnit.MILLISECONDS);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertTrue(scheduled.cancel(true));
            assertThrows(CancellationException.class, scheduled::get);
            assertTrue(finished.await(5, TimeUnit.SECONDS));
            assertTrue(owner.isCanceled());
        }
        assertTrue(owner.state().done());
    }

    @Test
    void standardCallableFailureRetainsFutureExecutionException() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        var failure = new IllegalStateException("source missing");
        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = M3EclipseScheduledTasks.submit(
                    executor, owner, "read", 2, () -> { throw failure; });
            ExecutionException thrown =
                    assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            assertSame(failure, thrown.getCause());
        }
        assertTrue(owner.state().done());
    }

    @Test
    void ordinaryRunnableAndCallableUseTheCallerExecutor() throws Exception {
        var first = new M3DefaultProgressMonitor();
        var second = new M3DefaultProgressMonitor();
        var counter = new java.util.concurrent.atomic.AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = M3EclipseScheduledTasks.submit(
                    pool, first, "runnable", 1, (Runnable) counter::incrementAndGet);
            var b = M3EclipseScheduledTasks.submit(
                    pool, second, "callable", 1, () -> "complete");
            assertNull(a.get(5, TimeUnit.SECONDS));
            assertEquals("complete", b.get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, counter.get());
        assertTrue(first.state().done());
        assertTrue(second.state().done());
    }

    @Test
    void rejectionAndInvalidArgumentsDoNotBeginProgress() {
        var owner = new M3DefaultProgressMonitor();
        var pool = Executors.newSingleThreadScheduledExecutor();
        pool.shutdown();
        assertThrows(RejectedExecutionException.class, () ->
                M3EclipseScheduledTasks.schedule(
                        pool, owner, "rejected", 1, () -> 1,
                        0, TimeUnit.MILLISECONDS));
        assertEquals(0L, owner.state().generation());

        try (var active = Executors.newSingleThreadScheduledExecutor()) {
            assertThrows(IllegalArgumentException.class, () ->
                    M3EclipseScheduledTasks.schedule(
                            active, owner, "invalid", -2, () -> 1,
                            0, TimeUnit.MILLISECONDS));
            assertThrows(NullPointerException.class, () ->
                    M3EclipseScheduledTasks.schedule(
                            active, owner, "invalid", 1, () -> 1,
                            0, null));
        }
        assertEquals(0L, owner.state().generation());
    }

    @Test
    void exceptionAndCooperativeCancellationKeepDistinctCompletionContracts() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        try (var pool = Executors.newSingleThreadExecutor()) {
            var future = M3EclipseScheduledTasks.submit(
                    pool, owner, "cancel", 1,
                    () -> { throw new CancellationException("stop"); });
            ExecutionException thrown =
                    assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            assertTrue(thrown.getCause() instanceof CancellationException);
        }
        assertTrue(owner.isCanceled());
        assertTrue(owner.state().done());

        var statusOwner = new M3DefaultProgressMonitor();
        try (var pool = Executors.newSingleThreadScheduledExecutor()) {
            var future = M3EclipseScheduledTasks.scheduleStatus(
                    pool, statusOwner, "status", 1, m -> Status.OK_STATUS,
                    0, TimeUnit.MILLISECONDS);
            assertSame(Status.OK_STATUS, future.get(5, TimeUnit.SECONDS));
        }
        assertTrue(statusOwner.state().done());
    }
}
