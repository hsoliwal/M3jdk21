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
    /** Sentinel indicating that the total amount of work is unknown. */
    long UNKNOWN_WORK = -1L;

    /** A monitor using the default no-op progress and cancellation updates. */
    ProgressMonitor NONE = new ProgressMonitor() { };

    /**
     * Reports the beginning of a task. The default implementation does nothing.
     *
     * @param task the task description
     * @param totalWork the expected work units, or {@link #UNKNOWN_WORK}
     */
    default void begin(String task, long totalWork) { }

    /**
     * Reports additional completed work. The default implementation does nothing.
     *
     * @param units the number of additional work units
     */
    default void worked(long units) { }

    /**
     * Reports a progress message. The default implementation does nothing.
     *
     * @param text the progress message
     */
    default void message(String text) { }

    /**
     * Tests the monitor's cancellation state.
     *
     * @return false by default; implementations may report cancellation
     */
    default boolean isCancelled() { return false; }

    /** Requests cancellation. The default implementation does nothing. */
    default void cancel() { }

    /** Reports completion. The default implementation does nothing. */
    default void done() { }

    /**
     * Checks monitor cancellation and the current thread's interrupt status.
     *
     * @throws CancellationException if cancellation or interruption is detected
     */
    default void checkCancelled() {
        if (isCancelled() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Operation cancelled");
        }
    }

    /**
     * Allocates parent ticks to a child; completes the child independently.
     *
     * @param name the child description, or null for no description
     * @param parentTicks the nonnegative work units allocated in this monitor
     * @param totalWork the positive child work units, or {@link #UNKNOWN_WORK}
     * @return the child monitor, or {@link #NONE} if this monitor is {@code NONE}
     * @throws IllegalArgumentException if the work range is invalid
     */
    default ProgressMonitor child(String name, long parentTicks, long totalWork) {
        return ProgressMonitors.child(this, name, parentTicks, totalWork);
    }
}
