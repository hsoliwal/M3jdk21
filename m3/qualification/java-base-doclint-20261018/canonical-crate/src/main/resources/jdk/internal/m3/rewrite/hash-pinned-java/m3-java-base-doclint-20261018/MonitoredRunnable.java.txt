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
    /**
     * Performs the replaceable task body with the selected progress monitor.
     *
     * @param monitor the progress monitor supplied to the task body
     */
    void runMonitored(ProgressMonitor monitor);

    /** Template method: inherit monitor, check cancellation, dispatch body. */
    @Override
    default void run() {
        ProgressMonitor monitor = ProgressMonitors.current();
        monitor.checkCancelled();
        runMonitored(monitor);
        monitor.checkCancelled();
    }
}
