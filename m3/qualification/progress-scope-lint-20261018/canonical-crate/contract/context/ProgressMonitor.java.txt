/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util.concurrent;

/**
 * Cooperative progress and cancellation for a bounded unit of work.
 * A monitor belongs to its caller. A task must not finish a borrowed monitor.
 * Implementations must document whether concurrent updates are supported.
 * This opt-in M3JDK API does not alter legacy Executor execution semantics.
 */
public interface ProgressMonitor {
    /**
     * The sentinel indicating that the total amount of work is unknown.
     */
    long UNKNOWN_WORK = -1L;
    /**
     * A monitor whose lifecycle, work, and cancellation methods use the no-op defaults.
     */
    ProgressMonitor NONE = new ProgressMonitor() { };

    /**
     * Begins a task. The default implementation does nothing.
     *
     * @param task the task description
     * @param totalWork the total work, or {@link #UNKNOWN_WORK} if unknown
     */
    default void begin(String task, long totalWork) { }
    /**
     * Reports completed work. The default implementation does nothing.
     *
     * @param units the amount of newly completed work
     */
    default void worked(long units) { }
    /**
     * Reports a message. The default implementation does nothing.
     *
     * @param text the message to report
     */
    default void message(String text) { }
    /**
     * Reports whether cancellation has been requested.
     *
     * @return false in the default implementation
     */
    default boolean isCancelled() { return false; }
    /**
     * Requests cancellation. The default implementation does nothing.
     */
    default void cancel() { }
    /**
     * Completes this monitor. The default implementation does nothing.
     */
    default void done() { }

    /**
     * Checks this monitor and the current thread for cancellation without clearing interruption.
     *
     * @throws CancellationException if this monitor is cancelled or the current thread is interrupted
     */
    default void checkCancelled() {
        if (isCancelled() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Operation cancelled");
        }
    }

    /**
     * Allocates a weighted child without completing this parent monitor.
     *
     * @param name the child task name; null is treated as an empty name
     * @param parentTicks the nonnegative number of parent units reserved for the child
     * @param totalWork the positive total child work, or {@link #UNKNOWN_WORK}
     * @return the weighted child, or {@link #NONE} if this monitor is {@code NONE}
     * @throws IllegalArgumentException if parentTicks is negative, or totalWork is zero or below UNKNOWN_WORK
     */
    default ProgressMonitor child(String name, long parentTicks, long totalWork) {
        return ProgressMonitors.child(this, name, parentTicks, totalWork);
    }
}
