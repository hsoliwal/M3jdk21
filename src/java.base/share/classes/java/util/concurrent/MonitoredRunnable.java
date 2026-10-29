/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util.concurrent;

/**
 * A single-method task whose execution body receives the currently bound
 * progress monitor. The inherited Runnable.run() entry point remains usable
 * by existing executors, and completion of a borrowed monitor is not owned here.
 */
@FunctionalInterface
public interface MonitoredRunnable extends Runnable {
<<<<<<< HEAD
    /**
     * Runs the task body using the supplied borrowed progress monitor.
     *
     * @param monitor the monitor bound by the template run
     */
||||||| merged common ancestors
    /** Replaceable task body; implementations report progress through monitor. */
=======
    /**
     * Replaceable task body; implementations report progress through monitor.
     *
     * @param monitor the monitor supplied to the execution body
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
    void runMonitored(ProgressMonitor monitor);

    /**
     * Inherits the current monitor, checks cancellation, and dispatches the body.
     * Cancellation is checked again after the body returns.
     *
     * @throws CancellationException if cancellation is observed before or
     *         after the body
     */
    @Override
    default void run() {
        ProgressMonitor monitor = ProgressMonitors.current();
        monitor.checkCancelled();
        runMonitored(monitor);
        monitor.checkCancelled();
    }
}
