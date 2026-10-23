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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Replaceable checked body, with no ownership over monitor completion.
     *
     * @param monitor the borrowed progress monitor for the task
=======
     * Performs the replaceable task body with the selected progress monitor.
     *
     * @param monitor the progress monitor supplied to the task body
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     * @return the computed result
     * @throws Exception if the task cannot compute a result
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
