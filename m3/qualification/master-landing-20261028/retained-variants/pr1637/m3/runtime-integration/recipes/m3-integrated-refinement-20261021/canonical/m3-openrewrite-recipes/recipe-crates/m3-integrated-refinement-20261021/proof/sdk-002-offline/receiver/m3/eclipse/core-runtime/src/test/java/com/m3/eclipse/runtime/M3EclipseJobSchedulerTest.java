// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3DefaultProgressMonitor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.Test;

/** Differential behavior of original Job scheduling rule and both progress owners. */
final class M3EclipseJobSchedulerTest {
    @Test void originalSchedulingRuleAndBothProgressOwners() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        ISchedulingRule rule = new ISchedulingRule() {
            @Override public boolean contains(ISchedulingRule other) { return other == this; }
            @Override public boolean isConflicting(ISchedulingRule other) { return other == this; }
        };
        Job job = M3EclipseJobScheduler.schedule("compile", rule, owner, 10, monitor -> {
            monitor.beginTask("javac", 10);
            monitor.worked(3);
            monitor.internalWorked(0.5);
            monitor.internalWorked(0.5);
            monitor.subTask("source.java");
            monitor.done();
            return Status.OK_STATUS;
        });
        job.join();
        assertSame(rule, job.getRule());
        assertSame(Status.OK_STATUS, job.getResult());
        assertEquals(4L, owner.state().worked());
        assertTrue(owner.state().done());
        assertEquals("compile", owner.state().taskName());
        assertFalse(owner.isCanceled());
    }

    @Test void taskCancellationUpdatesBothOriginalJobAndM3() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        Job job = M3EclipseJobScheduler.schedule("scan", null, owner, 5, monitor -> {
            monitor.setCanceled(true);
            assertTrue(monitor.isCanceled());
            return Status.OK_STATUS;
        });
        job.join();
        assertEquals(IStatus.CANCEL, job.getResult().getSeverity());
        assertTrue(owner.isCanceled());
        assertTrue(owner.state().done());
    }

    @Test void externalJobCancellationReachesSharedMonitor() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        var entered = new CountDownLatch(1);
        var permit = new CountDownLatch(1);
        Job job = M3EclipseJobScheduler.schedule("cancel", null, owner, 2, monitor -> {
            entered.countDown();
            if (!permit.await(5, TimeUnit.SECONDS)) throw new AssertionError("timeout");
            if (!monitor.isCanceled()) throw new AssertionError("Eclipse cancel lost");
            return Status.OK_STATUS;
        });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        job.cancel();
        permit.countDown();
        job.join();
        assertEquals(IStatus.CANCEL, job.getResult().getSeverity());
        assertTrue(owner.isCanceled());
    }

    @Test void failedTaskRetainsOriginalException() throws Exception {
        var owner = new M3DefaultProgressMonitor();
        var problem = new IllegalStateException("bad AST");
        Job job = M3EclipseJobScheduler.schedule("compile", null, owner, 7, monitor -> {
            throw problem;
        });
        job.join();
        assertEquals(IStatus.ERROR, job.getResult().getSeverity());
        assertSame(problem, job.getResult().getException());
        assertTrue(owner.state().done());
    }

    @Test void invalidWorkIsRejectedBeforeNativeSchedule() {
        var owner = new M3DefaultProgressMonitor();
        assertThrows(IllegalArgumentException.class,
                () -> M3EclipseJobScheduler.schedule("bad", null, owner, -2,
                        monitor -> Status.OK_STATUS));
        assertEquals(0L, owner.state().generation());
    }
}
