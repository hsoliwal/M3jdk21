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
=======
    /** Sentinel indicating that the total amount of work is unknown. */
    long UNKNOWN_WORK = -1L;

    /** A monitor using the default no-op progress and cancellation updates. */
    ProgressMonitor NONE = new ProgressMonitor() { };

    /**
     * Reports the beginning of a task. The default implementation does nothing.
     *
     * @param task the task description
>>>>>>> 82eb7fc30da9d3cf5556a11fb4a2b4ec0f09f385
     * @param totalWork the expected work units, or {@link #UNKNOWN_WORK}
     */
    default void begin(String task, long totalWork) { }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Reports additional completed work.
     * The default implementation does nothing.
     *
     * @param units the additional completed work units
=======
     * Reports additional completed work. The default implementation does nothing.
     *
     * @param units the number of additional work units
>>>>>>> 82eb7fc30da9d3cf5556a11fb4a2b4ec0f09f385
     */
    default void worked(long units) { }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Reports a progress message.
     * The default implementation does nothing.
     *
     * @param text the message interpreted by the implementation
=======
     * Reports a progress message. The default implementation does nothing.
     *
     * @param text the progress message
>>>>>>> 82eb7fc30da9d3cf5556a11fb4a2b4ec0f09f385
     */
    default void message(String text) { }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
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
     */
    default void done() { }

    /**
     * Checks this monitor and the current thread for cancellation without
     * clearing the thread's interrupted status.
     *
     * @throws CancellationException if this monitor reports cancellation or
     *         the current thread is interrupted
=======
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
>>>>>>> 82eb7fc30da9d3cf5556a11fb4a2b4ec0f09f385
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
     * Allocates parent ticks to a child; completes the child independently.
     *
     * @param name the child description, or null for no description
     * @param parentTicks the nonnegative work units allocated in this monitor
     * @param totalWork the positive child work units, or {@link #UNKNOWN_WORK}
     * @return the child monitor, or {@link #NONE} if this monitor is {@code NONE}
     * @throws IllegalArgumentException if the work range is invalid
>>>>>>> 82eb7fc30da9d3cf5556a11fb4a2b4ec0f09f385
     */
    default ProgressMonitor child(String name, long parentTicks, long totalWork) {
        return ProgressMonitors.child(this, name, parentTicks, totalWork);
    }
}
