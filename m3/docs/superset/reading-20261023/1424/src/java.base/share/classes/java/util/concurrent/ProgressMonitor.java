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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /** Sentinel for an unknown total number of work units. */
    long UNKNOWN_WORK = -1L;
    /** No-op monitor that reports no monitor-specific cancellation. */
    ProgressMonitor NONE = new ProgressMonitor() { };

    /**
     * Reports the beginning of a task.
     * The default implementation does nothing.
     *
     * @param task the task label interpreted by the implementation
     * @param totalWork the expected work units, or {@link #UNKNOWN_WORK}
     */
    default void begin(String task, long totalWork) { }

    /**
     * Reports additional completed work.
     * The default implementation does nothing.
     *
     * @param units the additional completed work units
     */
    default void worked(long units) { }

    /**
     * Reports a progress message.
     * The default implementation does nothing.
     *
     * @param text the message interpreted by the implementation
     */
    default void message(String text) { }

    /**
     * Reports whether this monitor has been cancelled.
     * The default implementation returns {@code false}.
     *
     * @return {@code true} if this monitor reports cancellation
     */
    default boolean isCancelled() { return false; }

    /**
     * Requests cancellation through this monitor.
     * The default implementation does nothing.
     */
    default void cancel() { }

    /**
     * Reports completion of work owned by the caller.
     * The default implementation does nothing.
=======
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
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    default void done() { }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Checks this monitor and the current thread for cancellation without
     * clearing the thread's interrupted status.
     *
     * @throws CancellationException if this monitor reports cancellation or
     *         the current thread is interrupted
=======
     * Checks this monitor and the current thread for cancellation without clearing interruption.
     *
     * @throws CancellationException if this monitor is cancelled or the current thread is interrupted
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    default void checkCancelled() {
        if (isCancelled() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Operation cancelled");
        }
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Allocate parent ticks to a child; complete the child independently.
     *
     * @param name the child task label; {@code null} is treated as empty
     * @param parentTicks the non-negative work units allocated in this monitor
     * @param totalWork the positive child work total, or {@link #UNKNOWN_WORK}
     * @return a child monitor, or {@link #NONE} when this monitor is {@code NONE}
     * @throws IllegalArgumentException if {@code parentTicks} is negative, or
     *         {@code totalWork} is neither positive nor {@code UNKNOWN_WORK}
=======
     * Allocates a weighted child without completing this parent monitor.
     *
     * @param name the child task name; null is treated as an empty name
     * @param parentTicks the nonnegative number of parent units reserved for the child
     * @param totalWork the positive total child work, or {@link #UNKNOWN_WORK}
     * @return the weighted child, or {@link #NONE} if this monitor is {@code NONE}
     * @throws IllegalArgumentException if parentTicks is negative, or totalWork is zero or below UNKNOWN_WORK
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    default ProgressMonitor child(String name, long parentTicks, long totalWork) {
        return ProgressMonitors.child(this, name, parentTicks, totalWork);
    }
}
