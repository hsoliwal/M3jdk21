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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Replaceable task body; implementations report progress through monitor.
     *
     * @param monitor the borrowed progress monitor for the task
=======
     * Performs the replaceable task body with the selected progress monitor.
     *
     * @param monitor the progress monitor supplied to the task body
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
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
