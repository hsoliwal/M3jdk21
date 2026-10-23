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
     * Replaceable task body; implementations report progress through monitor.
     *
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * @param monitor the borrowed progress monitor for the task
=======
     * @param monitor the monitor supplied to the execution body
>>>>>>> 82eb7fc30da9d3cf5556a11fb4a2b4ec0f09f385
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
