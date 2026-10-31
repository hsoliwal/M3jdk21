// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.runtime;

import com.m3.collections.M3ProgressMonitor;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Runs M3-owned monitored operations on a caller-owned Java 21 executor.
 *
 * <p>The M3 progress monitor remains the only mutable progress owner. A borrowed
 * Eclipse facade forwards callbacks without resetting a parent lifecycle. The
 * Future controls asynchronous completion; IStatus describes completed work.
 * Cancellation of an unfinished Future sets M3 cancellation and optionally
 * interrupts the worker. A completed Future is not retroactively canceled.
 *
 * <p>This is a task executor, <em>not</em> a replacement for Eclipse JobManager:
 * callers requiring Job scheduling rules, job families, UI-thread dispatch or
 * Eclipse lifecycle notifications must continue to schedule through Job.
 * There is no JNI invocation of user tasks or progress callbacks.
 */
public final class M3EclipseJobs {
    private static final String PLUGIN_ID = "com.m3.eclipse.runtime";

    private M3EclipseJobs() { }

    @FunctionalInterface
    public interface Task {
        IStatus run(IProgressMonitor monitor) throws Exception;
    }

    /**
     * Submit work with an exclusive progress owner. The owner must not be reused
     * by unrelated concurrent tasks. No progress lifecycle starts on rejection.
     */
    public static Future<IStatus> submit(ExecutorService executor,
            M3ProgressMonitor owner, String taskName, long totalWork, Task task) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(taskName, "taskName");
        Objects.requireNonNull(task, "task");
        requireWork(totalWork);

        FutureTask<IStatus> future = new FutureTask<>(
                () -> runNow(owner, taskName, totalWork, task)) {
            @Override public boolean cancel(boolean mayInterruptIfRunning) {
                if (!super.cancel(mayInterruptIfRunning)) return false;
                owner.setCanceled(true);
                return true;
            }
        };
        executor.execute(future);
        return future;
    }

    /**
     * Split the caller-owned parent once; each child has an independent progress
     * cursor. The parent must remain in the same generation until children finish.
     * Cancellation of a child is intentionally propagated to its parent.
     */
    public static Future<IStatus> submitChild(ExecutorService executor,
            M3ProgressMonitor parent, long parentWork, String taskName,
            long childTotalWork, Task task) {
        Objects.requireNonNull(parent, "parent");
        if (parentWork < 0L) throw new IllegalArgumentException("negative parentWork");
        return submit(executor, parent.split(parentWork), taskName, childTotalWork, task);
    }

    /**
     * Execute synchronously, e.g. inside an original Eclipse Job.run callback.
     * Exceptions become ERROR status; interruption/cancellation becomes CANCEL.
     * JVM Errors propagate after progress is completed.
     */
    public static IStatus runNow(M3ProgressMonitor owner,
            String taskName, long totalWork, Task task) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(taskName, "taskName");
        Objects.requireNonNull(task, "task");
        requireWork(totalWork);
        owner.beginTask(taskName, totalWork);
        try {
            if (owner.isCanceled() || Thread.currentThread().isInterrupted()) {
                return Status.CANCEL_STATUS;
            }
            IStatus outcome = Objects.requireNonNull(
                    task.run(M3EclipseProgressMonitor.borrowed(owner)), "task status");
            return owner.isCanceled() || Thread.currentThread().isInterrupted()
                    ? Status.CANCEL_STATUS : outcome;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            owner.setCanceled(true);
            return Status.CANCEL_STATUS;
        } catch (CancellationException canceled) {
            owner.setCanceled(true);
            return Status.CANCEL_STATUS;
        } catch (Exception failure) {
            return new Status(IStatus.ERROR, PLUGIN_ID, "M3 task failed: " + taskName, failure);
        } finally {
            owner.done();
        }
    }

    private static void requireWork(long work) {
        if (work < M3ProgressMonitor.UNKNOWN) {
            throw new IllegalArgumentException("totalWork below UNKNOWN");
        }
    }
}
