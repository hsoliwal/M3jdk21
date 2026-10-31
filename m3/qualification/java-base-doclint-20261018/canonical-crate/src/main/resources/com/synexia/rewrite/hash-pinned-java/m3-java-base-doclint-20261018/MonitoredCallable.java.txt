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
     * Performs the replaceable task body with the selected progress monitor.
     *
     * @param monitor the progress monitor supplied to the task body
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
