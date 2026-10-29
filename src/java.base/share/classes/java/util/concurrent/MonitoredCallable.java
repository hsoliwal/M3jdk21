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
<<<<<<< HEAD
    /**
     * Computes a result using the supplied borrowed progress monitor.
     * This body does not own completion of the monitor.
     *
     * @param monitor the monitor bound by the template call
     * @return the computed result
     * @throws Exception if the body cannot compute a result
     */
||||||| 2f2beb2eff7
    /** Replaceable checked body, with no ownership over monitor completion. */
=======
    /**
     * Performs the replaceable task body with the selected progress monitor.
     *
     * @param monitor the progress monitor supplied to the task body
     * @return the computed result
     * @throws Exception if the task cannot compute a result
     */
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
    V callMonitored(ProgressMonitor monitor) throws Exception;

    /**
     * Inherits the current monitor, checks cancellation, and dispatches the body.
     * Cancellation is checked again after the body returns.
     *
     * @return the result computed by {@link #callMonitored(ProgressMonitor)}
     * @throws CancellationException if cancellation is observed before or
     *         after the body
     * @throws Exception if the body cannot compute a result
     */
    @Override
    default V call() throws Exception {
        ProgressMonitor monitor = ProgressMonitors.current();
        monitor.checkCancelled();
        V value = callMonitored(monitor);
        monitor.checkCancelled();
        return value;
    }
}
