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
<<<<<<< HEAD
    /** Indicates that the total amount of work is not known. */
||||||| merged common ancestors
=======
    /**
     * The sentinel indicating that the total amount of work is unknown.
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    long UNKNOWN_WORK = -1L;
<<<<<<< HEAD
    /**
     * A monitor with no progress state and no local cancellation state.
     * Its inherited {@link #checkCancelled()} still observes interruption
     * of the calling thread.
     */
||||||| merged common ancestors
=======
    /**
     * A monitor whose lifecycle, work, and cancellation methods use the no-op defaults.
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    ProgressMonitor NONE = new ProgressMonitor() { };

<<<<<<< HEAD
    /**
     * Announces a task and its estimated work. The default implementation
     * does nothing; implementations define their task lifecycle and validation.
     *
     * @param task the task description
     * @param totalWork the estimated work, or {@link #UNKNOWN_WORK}
     */
||||||| merged common ancestors
=======
    /**
     * Begins a task. The default implementation does nothing.
     *
     * @param task the task description
     * @param totalWork the total work, or {@link #UNKNOWN_WORK} if unknown
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    default void begin(String task, long totalWork) { }
<<<<<<< HEAD
    /**
     * Reports additional completed work. The default implementation does nothing.
     *
     * @param units the additional work units, with validation defined by the
     *              implementation
     */
||||||| merged common ancestors
=======
    /**
     * Reports completed work. The default implementation does nothing.
     *
     * @param units the amount of newly completed work
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    default void worked(long units) { }
<<<<<<< HEAD
    /**
     * Reports a progress message. The default implementation does nothing.
     *
     * @param text the progress message
     */
||||||| merged common ancestors
=======
    /**
     * Reports a message. The default implementation does nothing.
     *
     * @param text the message to report
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    default void message(String text) { }
<<<<<<< HEAD
    /**
     * Reports cooperative cancellation state. The default returns {@code false}.
     *
     * @return whether this monitor reports cancellation
     */
||||||| merged common ancestors
=======
    /**
     * Reports whether cancellation has been requested.
     *
     * @return false in the default implementation
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    default boolean isCancelled() { return false; }
<<<<<<< HEAD
    /** Requests cooperative cancellation. The default implementation does nothing. */
||||||| merged common ancestors
=======
    /**
     * Requests cancellation. The default implementation does nothing.
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    default void cancel() { }
<<<<<<< HEAD
    /**
     * Completes work owned by this monitor. The default implementation does
     * nothing. A task must not complete a monitor borrowed from its caller.
     */
||||||| merged common ancestors
=======
    /**
     * Completes this monitor. The default implementation does nothing.
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    default void done() { }

<<<<<<< HEAD
    /**
     * Checks this monitor and the calling thread for cancellation without
     * clearing the thread's interrupt status.
     *
     * @throws CancellationException if {@link #isCancelled()} is true or
     *         the calling thread is interrupted
     */
||||||| merged common ancestors
=======
    /**
     * Checks this monitor and the current thread for cancellation without clearing interruption.
     *
     * @throws CancellationException if this monitor is cancelled or the current thread is interrupted
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    default void checkCancelled() {
        if (isCancelled() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Operation cancelled");
        }
    }

<<<<<<< HEAD
    /**
     * Allocates parent ticks to a child that can be completed independently.
     * Delegates to {@link ProgressMonitors#child(ProgressMonitor, String, long, long)}.
     *
     * @param name the child description, or {@code null} for no description
     * @param parentTicks the nonnegative work budget credited to this monitor
     * @param totalWork the positive child work estimate, or {@link #UNKNOWN_WORK}
     * @return the child monitor, or {@link #NONE} when this monitor is {@code NONE}
     * @throws IllegalArgumentException if the work budget or estimate is invalid
     */
||||||| merged common ancestors
    /** Allocate parent ticks to a child; complete the child independently. */
=======
    /**
     * Allocates a weighted child without completing this parent monitor.
     *
     * @param name the child task name; null is treated as an empty name
     * @param parentTicks the nonnegative number of parent units reserved for the child
     * @param totalWork the positive total child work, or {@link #UNKNOWN_WORK}
     * @return the weighted child, or {@link #NONE} if this monitor is {@code NONE}
     * @throws IllegalArgumentException if parentTicks is negative, or totalWork is zero or below UNKNOWN_WORK
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    default ProgressMonitor child(String name, long parentTicks, long totalWork) {
        return ProgressMonitors.child(this, name, parentTicks, totalWork);
    }
    /**
     * Allocates parent ticks to an unnamed child with an estimated work total.
     *
     * @implSpec
     * The default implementation invokes {@link #child(String, long, long)}
     * with a {@code null} name and the supplied budget and estimate.
     *
     * @param parentTicks the nonnegative work budget credited to this monitor
     * @param totalWork the positive child work estimate, or {@link #UNKNOWN_WORK}
     * @return the child returned by the full-argument method
     * @throws IllegalArgumentException if the work budget or estimate is invalid
     */
    default ProgressMonitor child(long parentTicks, long totalWork) {
        return child(null, parentTicks, totalWork);
    }

    /**
     * Allocates parent ticks to a named child whose work total is unknown.
     *
     * @implSpec
     * The default implementation invokes {@link #child(String, long, long)}
     * with the supplied name and budget and {@link #UNKNOWN_WORK} as the total.
     *
     * @param name the child description, or {@code null} for no description
     * @param parentTicks the nonnegative work budget credited to this monitor
     * @return the child returned by the full-argument method
     * @throws IllegalArgumentException if the work budget is invalid
     */
    default ProgressMonitor child(String name, long parentTicks) {
        return child(name, parentTicks, UNKNOWN_WORK);
    }

    /**
     * Allocates parent ticks to an unnamed child whose work total is unknown.
     *
     * @implSpec
     * The default implementation invokes {@link #child(String, long, long)}
     * with a {@code null} name, the supplied budget and {@link #UNKNOWN_WORK}.
     *
     * @param parentTicks the nonnegative work budget credited to this monitor
     * @return the child returned by the full-argument method
     * @throws IllegalArgumentException if the work budget is invalid
     */
    default ProgressMonitor child(long parentTicks) {
        return child(null, parentTicks, UNKNOWN_WORK);
    }

}
