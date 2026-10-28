// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3DefaultProgressMonitor;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.junit.jupiter.api.Test;

final class M3EclipseJobsTest {
    @Test
    void borrowedProgressAndCompletedStatus() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        try (var pool = Executors.newSingleThreadExecutor()) {
            var future = M3EclipseJobs.submit(pool, owner, "index", 10, monitor -> {
                monitor.beginTask("nested", 5);
                monitor.worked(3);
                monitor.done();
                return Status.OK_STATUS;
            });
            assertSame(Status.OK_STATUS, future.get());
        }
        assertEquals("index", owner.state().taskName());
        assertEquals(3L, owner.state().worked());
        assertTrue(owner.state().done());
        assertFalse(owner.isCanceled());
    }

    @Test
    void childProgressSharesOneParentWithoutCompletingIt() throws Exception {
        var parent = new M3DefaultProgressMonitor();
        parent.beginTask("reactor", 100);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = M3EclipseJobs.submitChild(pool, parent, 12, "compile", 4,
                    monitor -> { monitor.worked(4); return Status.OK_STATUS; });
            var second = M3EclipseJobs.submitChild(pool, parent, 18, "index", 2,
                    monitor -> { monitor.worked(2); return Status.OK_STATUS; });
            assertEquals(IStatus.OK, first.get().getSeverity());
            assertEquals(IStatus.OK, second.get().getSeverity());
        }
        assertEquals(30L, parent.state().worked());
        assertFalse(parent.state().done());
    }

    @Test
    void exceptionBecomesErrorStatusWithOriginalCause() {
        var owner = new M3DefaultProgressMonitor();
        var failure = new IllegalStateException("parser failed");
        IStatus status = M3EclipseJobs.runNow(owner, "parse", 1, m -> { throw failure; });
        assertEquals(IStatus.ERROR, status.getSeverity());
        assertSame(failure, status.getException());
        assertTrue(owner.state().done());
    }

    @Test
    void cooperativeCancellationProducesCancelStatus() {
        var owner = new M3DefaultProgressMonitor();
        owner.setCanceled(true);
        IStatus status = M3EclipseJobs.runNow(owner, "cancel", 1, m -> {
            throw new AssertionError("canceled task must not run");
        });
        assertEquals(IStatus.CANCEL, status.getSeverity());
        assertTrue(owner.state().done());
    }

    @Test
    void futureCancellationPropagatesToMonitorAndInterruptsWorker() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        var entered = new CountDownLatch(1);
        var finished = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var future = M3EclipseJobs.submit(pool, owner, "long-job", 20, m -> {
                entered.countDown();
                try {
                    new CountDownLatch(1).await();
                    return Status.OK_STATUS;
                } finally {
                    finished.countDown();
                }
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertTrue(future.cancel(true));
            assertThrows(CancellationException.class, future::get);
            assertTrue(finished.await(5, TimeUnit.SECONDS));
            assertTrue(owner.isCanceled());
        }
        assertTrue(owner.state().done());
    }

    @Test
    void executorRejectionLeavesProgressUnstarted() {
        var owner = new M3DefaultProgressMonitor();
        var pool = Executors.newSingleThreadExecutor();
        pool.shutdown();
        assertThrows(RejectedExecutionException.class,
                () -> M3EclipseJobs.submit(pool, owner, "rejected", 1, m -> Status.OK_STATUS));
        assertFalse(owner.state().done());
        assertEquals(0L, owner.state().generation());
    }

    @Test
    void invalidInputsFailBeforeScheduling() {
        var owner = new M3DefaultProgressMonitor();
        try (var pool = Executors.newSingleThreadExecutor()) {
            assertThrows(IllegalArgumentException.class,
                    () -> M3EclipseJobs.submit(pool, owner, "invalid", -2, m -> Status.OK_STATUS));
            assertThrows(IllegalArgumentException.class,
                    () -> M3EclipseJobs.submitChild(pool, owner, -1, "invalid", 1, m -> Status.OK_STATUS));
        }
        assertEquals(0L, owner.state().generation());
    }
}
