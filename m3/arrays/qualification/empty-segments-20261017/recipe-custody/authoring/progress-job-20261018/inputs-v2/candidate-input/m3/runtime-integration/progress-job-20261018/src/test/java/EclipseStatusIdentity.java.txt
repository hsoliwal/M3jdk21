/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ProgressJob;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

/** Executes against checksum-pinned Eclipse jars, not substitute status types. */
public final class EclipseStatusIdentity {
    private static int checks;
    private EclipseStatusIdentity() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        check(IStatus.class.getProtectionDomain().getCodeSource() != null, "external real Eclipse class source");
        check(Status.class.getClassLoader() != null, "Eclipse is outside java.base");
        IOException cause = new IOException("status cause");
        IStatus warning = new Status(IStatus.WARNING, "m3.progress.job.fixture", 17, "warning", null);
        IStatus error = new Status(IStatus.ERROR, "m3.progress.job.fixture", 19, "error", cause);
        IStatus multi = new MultiStatus("m3.progress.job.fixture", 23, new IStatus[] { warning, error }, "aggregate", cause);
        Executor executor = Runnable::run;
        for (IStatus value : new IStatus[] { Status.OK_STATUS, Status.CANCEL_STATUS, warning, error, multi }) {
            ProgressJob<IStatus> job = new ProgressJob<>() {
                @Override protected IStatus run(ProgressMonitor monitor) {
                    check(monitor == ProgressMonitors.current(), "real status lexical monitor");
                    return value;
                }
            };
            check(job.call(ProgressMonitor.NONE) == value, "real status direct identity");
            FutureTask<IStatus> future = new FutureTask<>(() -> job.call(ProgressMonitor.NONE));
            executor.execute(future);
            check(future.get(15, TimeUnit.SECONDS) == value, "real status FutureTask identity");
            check(!future.isCancelled(), "returned CANCEL status does not call Future.cancel");
        }
        check(error.getException() == cause, "status retains original cause");
        check(error.getCode() == 19 && error.getSeverity() == IStatus.ERROR, "status code and severity");
        check(multi.isMultiStatus() && multi.getChildren()[0] == warning && multi.getChildren()[1] == error,
                "multi-status child identities");
        ProgressJob<IStatus> throwsChecked = new ProgressJob<>() {
            @Override protected IStatus run(ProgressMonitor monitor) throws IOException { throw cause; }
        };
        try {
            throwsChecked.call(ProgressMonitor.NONE);
            throw new AssertionError("expected checked exception");
        } catch (IOException actual) {
            check(actual == cause, "checked exception does not become a Status");
        }
        FutureTask<IStatus> failed = new FutureTask<>(() -> throwsChecked.call(ProgressMonitor.NONE));
        executor.execute(failed);
        try {
            failed.get(15, TimeUnit.SECONDS);
            throw new AssertionError("expected future failure");
        } catch (ExecutionException actual) {
            check(actual.getCause() == cause, "real IStatus job keeps Future.get cause identity");
        }
        AtomicBoolean cancelled = new AtomicBoolean(true);
        AtomicInteger bodies = new AtomicInteger();
        CancellationException marker = new CancellationException("Eclipse result pre-cancel marker");
        ProgressMonitor monitor = new ProgressMonitor() {
            @Override public boolean isCancelled() { return cancelled.get(); }
            @Override public void cancel() { cancelled.set(true); }
            @Override public void checkCancelled() { if (isCancelled()) throw marker; }
        };
        ProgressJob<IStatus> cancels = new ProgressJob<>() {
            @Override protected IStatus run(ProgressMonitor current) {
                bodies.incrementAndGet(); current.cancel(); return Status.CANCEL_STATUS;
            }
        };
        for (boolean before : new boolean[] { true, false }) {
            cancelled.set(before);
            FutureTask<IStatus> future = new FutureTask<>(() -> cancels.call(monitor));
            executor.execute(future);
            try {
                future.get(15, TimeUnit.SECONDS);
                throw new AssertionError("expected monitor cancellation");
            } catch (ExecutionException actual) {
                check(actual.getCause() == marker, "monitor cancellation retains existing template exception");
            }
            check(future.isDone() && !future.isCancelled(), "monitor cancellation completes future exceptionally");
        }
        check(bodies.get() == 1, "pre-cancel skips body; post-cancel rejects returned CANCEL status");
        System.out.println("PASS real Eclipse IStatus/Status/MultiStatus checks=" + checks
                + "; status return preserved; cancellation follows existing MonitoredCallable contract");
    }
}
