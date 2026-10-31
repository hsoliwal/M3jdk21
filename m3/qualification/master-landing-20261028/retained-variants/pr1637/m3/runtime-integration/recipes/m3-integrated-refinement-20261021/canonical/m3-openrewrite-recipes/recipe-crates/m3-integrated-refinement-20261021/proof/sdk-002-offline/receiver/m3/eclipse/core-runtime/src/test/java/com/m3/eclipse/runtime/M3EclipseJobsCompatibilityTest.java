// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3DefaultProgressMonitor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.Test;

/** Real Eclipse jobs are the oracle for cancellation and scheduling exclusion. */
final class M3EclipseJobsCompatibilityTest {
    private static final long TIMEOUT_MILLIS = 5_000L;

    @Test void eclipseCancellationExceptionMatchesNativeJob() throws Exception {
        Job original = new Job("native cancellation oracle") {
            @Override protected IStatus run(IProgressMonitor monitor) {
                throw new OperationCanceledException("native cancellation");
            }
        };
        original.schedule();
        join(original);
        assertSame(Status.CANCEL_STATUS, original.getResult());

        var owner = new M3DefaultProgressMonitor();
        Job candidate = M3EclipseJobScheduler.schedule("M3 cancellation", null,
                owner, 1, monitor -> {
                    throw new OperationCanceledException("Eclipse task cancellation");
                });
        join(candidate);
        assertEquals(original.getResult().getSeverity(), candidate.getResult().getSeverity(),
                "standard Eclipse OperationCanceledException must retain native CANCEL status");
        assertTrue(owner.isCanceled());
        assertTrue(owner.state().done());
    }

    @Test void synchronousEclipseCancellationIsNotAnError() {
        var owner = new M3DefaultProgressMonitor();
        IStatus result = M3EclipseJobs.runNow(owner, "synchronous cancellation", 1,
                monitor -> { throw new OperationCanceledException(); });
        assertSame(Status.CANCEL_STATUS, result,
                "runNow must recognize the Eclipse task cancellation signal");
        assertTrue(owner.isCanceled());
        assertTrue(owner.state().done());
    }

    @Test void actualJobManagerExcludesConflictingRules() throws Exception {
        ISchedulingRule rule = new IdentityRule();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var firstFinished = new AtomicBoolean();
        var starts = new AtomicInteger();
        Job first = blockingOriginal(rule, entered, release, firstFinished);
        first.schedule();
        assertTrue(entered.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        var owner = new M3DefaultProgressMonitor();
        Job conflicting = M3EclipseJobScheduler.schedule("conflicting M3 job", rule,
                owner, 1, monitor -> {
                    starts.incrementAndGet();
                    if (!firstFinished.get()) {
                        throw new AssertionError("conflicting native rule overlapped");
                    }
                    return Status.OK_STATUS;
                });
        Job unrelated = M3EclipseJobScheduler.schedule("unrelated M3 job",
                new IdentityRule(), new M3DefaultProgressMonitor(), 1,
                monitor -> Status.OK_STATUS);
        try {
            join(unrelated);
            assertSame(Status.OK_STATUS, unrelated.getResult());
            assertEquals(0, starts.get(), "conflicting job must wait for the original rule owner");
            assertEquals(0L, owner.state().generation(), "waiting job has no M3 lifecycle yet");
        } finally {
            release.countDown();
            join(first);
            join(conflicting);
        }
        assertSame(rule, conflicting.getRule());
        assertSame(Status.OK_STATUS, conflicting.getResult());
        assertEquals(1, starts.get());
        assertTrue(owner.state().done());
    }

    @Test void cancelingWaitingNativeJobDoesNotStartM3Owner() throws Exception {
        ISchedulingRule rule = new IdentityRule();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        Job first = blockingOriginal(rule, entered, release, new AtomicBoolean());
        first.schedule();
        assertTrue(entered.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        var owner = new M3DefaultProgressMonitor();
        Job waiting = M3EclipseJobScheduler.schedule("cancel before start", rule,
                owner, 1, monitor -> {
                    throw new AssertionError("waiting canceled job must not execute");
                });
        try {
            assertTrue(waiting.cancel());
            join(waiting);
            assertEquals(0L, owner.state().generation());
            assertFalse(owner.state().done());
            assertFalse(owner.isCanceled(), "a never-started native job does not call its M3 owner");
        } finally {
            release.countDown();
            join(first);
        }
    }

    private static Job blockingOriginal(ISchedulingRule rule, CountDownLatch entered,
            CountDownLatch release, AtomicBoolean finished) {
        Job job = new Job("native rule owner") {
            @Override protected IStatus run(IProgressMonitor monitor) {
                entered.countDown();
                try {
                    if (!release.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                        throw new AssertionError("native rule owner timed out");
                    }
                    finished.set(true);
                    return Status.OK_STATUS;
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                }
            }
        };
        job.setRule(rule);
        return job;
    }

    private static void join(Job job) throws InterruptedException {
        assertTrue(job.join(TIMEOUT_MILLIS, null), "job did not finish: " + job.getName());
    }

    private static final class IdentityRule implements ISchedulingRule {
        @Override public boolean contains(ISchedulingRule other) { return this == other; }
        @Override public boolean isConflicting(ISchedulingRule other) { return this == other; }
    }
}
