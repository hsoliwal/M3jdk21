/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util.concurrent;

/**
 * A Callable-oriented task with a protected, result-returning progress hook.
 * Subclasses implement {@link #run(ProgressMonitor)}. The final {@link #call()}
 * method uses the existing {@link MonitoredCallable} template to obtain the
 * current monitor and check cancellation before and after the body.
 *
 * <p>The result type belongs to the caller and may be an application's status
 * type. A successful call returns the body's value unchanged, including
 * {@code null}. A checked exception from the body is propagated unchanged;
 * this class does not convert exceptions into status values. Cancellation
 * observed by the template throws {@link CancellationException}, even when
 * the body has returned a value representing cancellation.
 *
 * <p>For an explicitly supplied monitor, the inherited
 * {@link Callable#call(ProgressMonitor) call(monitor)} method binds that borrowed
 * monitor during the call and restores the previous binding on exit. Neither
 * template completes the borrowed monitor. Binding is local to the executing
 * thread and is not automatically captured when a task is submitted elsewhere.
 *
 * <p>An ordinary {@link Executor} can execute the task through a
 * {@link FutureTask} whose Callable binds the monitor:
 * <pre>{@code
 * FutureTask<S> future = new FutureTask<>(() -> job.call(monitor));
 * executor.execute(future);
 * S result = future.get();
 * }</pre>
 * Binding inside the FutureTask lets it record a cancellation exception from
 * the call as exceptional completion. The Future uses its existing
 * {@link Future#get() get()} exception and cancellation contracts.
 *
 * <p>This class has no task state, scheduler, or monitor field. Subclasses and
 * monitor implementations determine whether their state supports concurrent
 * calls. It is a Callable base: its value-returning progress hook is
 * incompatible with the void progress overload in {@link Runnable}, so a
 * subclass cannot also implement Runnable.
 *
 * @param <S> the result type, including an application-defined status type
 */
public abstract class ProgressJob<S> implements MonitoredCallable<S> {
    /** Creates a task without execution or monitor state. */
    protected ProgressJob() { }

    /**
     * Computes this task's result using a borrowed progress monitor.
     * The monitor is never {@code null} when this method is invoked through
     * {@link #call()}. A direct {@link #callMonitored(ProgressMonitor)} call
     * forwards its argument unchanged. The body does not own completion of a
     * borrowed monitor.
     *
     * @param monitor the monitor supplied to this invocation
     * @return the computed result, which may be {@code null}
     * @throws Exception if the body cannot compute its result
     */
    protected abstract S run(ProgressMonitor monitor) throws Exception;

    /**
     * Dispatches directly to {@link #run(ProgressMonitor)} with the supplied
     * argument. This body entry point does not bind a monitor or check
     * cancellation. Use {@link #call()} or the inherited
     * {@link Callable#call(ProgressMonitor) call(monitor)} for template execution.
     *
     * @param monitor the argument passed unchanged to the body
     * @return the body's result, unchanged
     * @throws Exception if the body cannot compute its result
     */
    @Override
    public final S callMonitored(ProgressMonitor monitor) throws Exception {
        return run(monitor);
    }

    /**
     * Executes the existing {@link MonitoredCallable#call()} template using
     * {@link ProgressMonitors#current()} and returns the body's result.
     *
     * @return the body's result, unchanged when cancellation is not observed
     * @throws CancellationException if cancellation is observed before or
     *         after the body
     * @throws Exception if the body cannot compute its result
     */
    @Override
    public final S call() throws Exception {
        return MonitoredCallable.super.call();
    }
}
