/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util.concurrent;

/**
 * A checked-value template task using the currently bound progress monitor.
 * The single abstract hook is callMonitored; inherited Callable.call() is
 * implemented as its default template method.
 * @param <V> computed result type
 */
@FunctionalInterface
public interface MonitoredCallable<V> extends Callable<V> {
    /**
     * Replaceable checked body, with no ownership over monitor completion.
     *
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * @param monitor the borrowed progress monitor for the task
     * @return the computed result
     * @throws Exception if the task cannot compute a result
=======
     * @param monitor the monitor supplied to the execution body
     * @return the computed result
     * @throws Exception if the body cannot compute a result
>>>>>>> 82eb7fc30da9d3cf5556a11fb4a2b4ec0f09f385
     */
    V callMonitored(ProgressMonitor monitor) throws Exception;

    /** Template method: inherit monitor, check cancellation, dispatch body. */
    @Override
    default V call() throws Exception {
        ProgressMonitor monitor = ProgressMonitors.current();
        monitor.checkCancelled();
        V value = callMonitored(monitor);
        monitor.checkCancelled();
        return value;
    }
}
