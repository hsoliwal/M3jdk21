// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.runtime;

import com.m3.collections.M3ProgressMonitor;
import java.util.Objects;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Schedules an M3-owned progress operation through the original Eclipse JobManager.
 * The Eclipse jobs engine, not a competing M3 queue, controls scheduling rules,
 * priorities, cancellation, worker threads and lifecycle notifications.
 *
 * <p>The provided M3 owner is exclusively assigned to this job. An existing M3
 * parent may hand in a distinct split child, but must not restart its generation
 * before this job finishes. The Eclipse monitor and the borrowed M3 monitor both
 * receive progress; neither owns a second task or payload store.
 */
public final class M3EclipseJobScheduler {
    private M3EclipseJobScheduler() { }

    /**
     * Schedule work with Eclipse's original ISchedulingRule exclusion semantics.
     * Return the actual Job so callers can use join, cancel, families and listeners.
     */
    public static Job schedule(String name, ISchedulingRule rule,
            M3ProgressMonitor owner, long totalWork, M3EclipseJobs.Task task) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(task, "task");
        if (totalWork < M3ProgressMonitor.UNKNOWN) {
            throw new IllegalArgumentException("totalWork below UNKNOWN");
        }
        Job job = new Job(name) {
            @Override protected IStatus run(IProgressMonitor eclipse) {
                if (eclipse.isCanceled()) owner.setCanceled(true);
                return M3EclipseJobs.runNow(owner, name, totalWork, ignored -> {
                    IProgressMonitor both = new SharedProgress(eclipse,
                            M3EclipseProgressMonitor.borrowed(owner));
                    try {
                        return task.run(both);
                    } finally {
                        if (eclipse.isCanceled()) owner.setCanceled(true);
                    }
                });
            }
        };
        if (rule != null) job.setRule(rule);
        job.schedule();
        return job;
    }

    /**
     * Preserve both native Eclipse cancellation and the existing M3 progress owner.
     * User callbacks and predicates remain in Java, never passed through JNI.
     */
    private static final class SharedProgress implements IProgressMonitor {
        private final IProgressMonitor eclipse;
        private final IProgressMonitor m3;

        private SharedProgress(IProgressMonitor eclipse, IProgressMonitor m3) {
            this.eclipse = Objects.requireNonNull(eclipse, "eclipse");
            this.m3 = Objects.requireNonNull(m3, "m3");
        }

        @Override public void beginTask(String name, int totalWork) {
            eclipse.beginTask(name, totalWork);
            m3.beginTask(name, totalWork);
        }
        @Override public void done() {
            eclipse.done();
            m3.done();
        }
        @Override public void internalWorked(double work) {
            eclipse.internalWorked(work);
            m3.internalWorked(work);
        }
        @Override public void worked(int work) {
            eclipse.worked(work);
            m3.worked(work);
        }
        @Override public void setTaskName(String name) {
            eclipse.setTaskName(name);
            m3.setTaskName(name);
        }
        @Override public void subTask(String name) {
            eclipse.subTask(name);
            m3.subTask(name);
        }
        @Override public boolean isCanceled() {
            return eclipse.isCanceled() || m3.isCanceled()
                    || Thread.currentThread().isInterrupted();
        }
        @Override public void setCanceled(boolean canceled) {
            eclipse.setCanceled(canceled);
            m3.setCanceled(canceled);
        }
    }
}
