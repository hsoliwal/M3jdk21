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
     * Performs the replaceable task body with the selected progress monitor.
     *
     * @param monitor the progress monitor supplied to the task body
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
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
