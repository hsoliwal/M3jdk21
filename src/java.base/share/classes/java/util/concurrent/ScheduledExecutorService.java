/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * An {@link ExecutorService} that can schedule commands to run after a given
 * delay, or to execute periodically.
 *
 * <p>The {@code schedule} methods create tasks with various delays
 * and return a task object that can be used to cancel or check
 * execution. The {@code scheduleAtFixedRate} and
 * {@code scheduleWithFixedDelay} methods create and execute tasks
 * that run periodically until cancelled.
 *
 * <p>Commands submitted using the {@link Executor#execute(Runnable)}
 * and {@link ExecutorService} {@code submit} methods are scheduled
 * with a requested delay of zero. Zero and negative delays (but not
 * periods) are also allowed in {@code schedule} methods, and are
 * treated as requests for immediate execution.
 *
 * <p>All {@code schedule} methods accept <em>relative</em> delays and
 * periods as arguments, not absolute times or dates. It is a simple
 * matter to transform an absolute time represented as a {@link
 * java.util.Date} to the required form. For example, to schedule at
 * a certain future {@code date}, you can use: {@code schedule(task,
 * date.getTime() - System.currentTimeMillis(),
 * TimeUnit.MILLISECONDS)}. Beware however that expiration of a
 * relative delay need not coincide with the current {@code Date} at
 * which the task is enabled due to network time synchronization
 * protocols, clock drift, or other factors.
 *
 * <p>The {@link Executors} class provides convenient factory methods for
 * the ScheduledExecutorService implementations provided in this package.
 *
 * <h2>Usage Example</h2>
 *
 * Here is a class with a method that sets up a ScheduledExecutorService
 * to beep every ten seconds for an hour:
 *
 * <pre> {@code
 * import static java.util.concurrent.TimeUnit.*;
 * class BeeperControl {
 *   private final ScheduledExecutorService scheduler =
 *     Executors.newScheduledThreadPool(1);
 *
 *   public void beepForAnHour() {
 *     Runnable beeper = () -> System.out.println("beep");
 *     ScheduledFuture<?> beeperHandle =
 *       scheduler.scheduleAtFixedRate(beeper, 10, 10, SECONDS);
 *     Runnable canceller = () -> beeperHandle.cancel(false);
 *     scheduler.schedule(canceller, 1, HOURS);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ScheduledExecutorService extends ExecutorService {

    /**
     * Submits a one-shot task that becomes enabled after the given delay.
     *
     * @param command the task to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @return a ScheduledFuture representing pending completion of
     *         the task and whose {@code get()} method will return
     *         {@code null} upon completion
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if command or unit is null
     */
    public ScheduledFuture<?> schedule(Runnable command,
                                       long delay, TimeUnit unit);

    /**
     * Submits a value-returning one-shot task that becomes enabled
     * after the given delay.
     *
     * @param callable the function to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @param <V> the type of the callable's result
     * @return a ScheduledFuture that can be used to extract result or cancel
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if callable or unit is null
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay, TimeUnit unit);

    /**
     * Submits a periodic action that becomes enabled first after the
     * given initial delay, and subsequently with the given period;
     * that is, executions will commence after
     * {@code initialDelay}, then {@code initialDelay + period}, then
     * {@code initialDelay + 2 * period}, and so on.
     *
     * <p>The sequence of task executions continues indefinitely until
     * one of the following exceptional completions occur:
     * <ul>
     * <li>The task is {@linkplain Future#cancel explicitly cancelled}
     * via the returned future.
     * <li>The executor terminates, also resulting in task cancellation.
     * <li>An execution of the task throws an exception.  In this case
     * calling {@link Future#get() get} on the returned future will throw
     * {@link ExecutionException}, holding the exception as its cause.
     * </ul>
     * Subsequent executions are suppressed.  Subsequent calls to
     * {@link Future#isDone isDone()} on the returned future will
     * return {@code true}.
     *
     * <p>If any execution of this task takes longer than its period, then
     * subsequent executions may start late, but will not concurrently
     * execute.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit the time unit of the initialDelay and period parameters
     * @return a ScheduledFuture representing pending completion of
     *         the series of repeated tasks.  The future's {@link
     *         Future#get() get()} method will never return normally,
     *         and will throw an exception upon task cancellation or
     *         abnormal termination of a task execution.
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if command or unit is null
     * @throws IllegalArgumentException if period less than or equal to zero
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit);

    /**
     * Submits a periodic action that becomes enabled first after the
     * given initial delay, and subsequently with the given delay
     * between the termination of one execution and the commencement of
     * the next.
     *
     * <p>The sequence of task executions continues indefinitely until
     * one of the following exceptional completions occur:
     * <ul>
     * <li>The task is {@linkplain Future#cancel explicitly cancelled}
     * via the returned future.
     * <li>The executor terminates, also resulting in task cancellation.
     * <li>An execution of the task throws an exception.  In this case
     * calling {@link Future#get() get} on the returned future will throw
     * {@link ExecutionException}, holding the exception as its cause.
     * </ul>
     * Subsequent executions are suppressed.  Subsequent calls to
     * {@link Future#isDone isDone()} on the returned future will
     * return {@code true}.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one
     * execution and the commencement of the next
     * @param unit the time unit of the initialDelay and delay parameters
     * @return a ScheduledFuture representing pending completion of
     *         the series of repeated tasks.  The future's {@link
     *         Future#get() get()} method will never return normally,
     *         and will throw an exception upon task cancellation or
     *         abnormal termination of a task execution.
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if command or unit is null
     * @throws IllegalArgumentException if delay less than or equal to zero
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit);

    /**
     * Schedules a one-shot action with a borrowed progress monitor bound
     * only while the action executes. The monitor is not completed by the
     * executor. Monitor cancellation is cooperative and remains distinct
     * from cancellation of the returned future.
     *
     * @implSpec
     * The default implementation delegates to
     * {@link #schedule(Runnable, long, TimeUnit)} with the runnable returned
     * by {@link ProgressMonitors#wrap(Runnable, ProgressMonitor)}.
     *
     * @param command the action to execute
     * @param delay the delay before execution
     * @param unit the unit of the delay
     * @param monitor the borrowed monitor
     * @return the future returned by the delegated schedule operation
     * @throws NullPointerException if command, unit, or monitor is null
     * @throws RejectedExecutionException if scheduling is rejected
     */
    default ScheduledFuture<?> scheduleMonitored(
            Runnable command, long delay, TimeUnit unit,
            ProgressMonitor monitor) {
        return schedule(ProgressMonitors.wrap(command, monitor), delay, unit);
    }

    /**
     * Schedules a value-returning task with a borrowed progress monitor
     * bound only while the task executes. Checked exceptions and the
     * returned future retain the delegate's behavior.
     *
     * @implSpec
     * The default implementation delegates to
     * {@link #schedule(Callable, long, TimeUnit)} with the callable returned
     * by {@link ProgressMonitors#wrap(Callable, ProgressMonitor)}.
     *
     * @param <V> the result type
     * @param callable the task to execute
     * @param delay the delay before execution
     * @param unit the unit of the delay
     * @param monitor the borrowed monitor
     * @return the future returned by the delegated schedule operation
     * @throws NullPointerException if callable, unit, or monitor is null
     * @throws RejectedExecutionException if scheduling is rejected
     */
    default <V> ScheduledFuture<V> scheduleMonitored(
            Callable<V> callable, long delay, TimeUnit unit,
            ProgressMonitor monitor) {
        return schedule(ProgressMonitors.wrap(callable, monitor), delay, unit);
    }

    /**
     * Schedules a fixed-rate action with a borrowed monitor bound separately
     * for each execution. Cancellation reported by the monitor terminates
     * an execution exceptionally; it does not call {@link Future#cancel}.
     *
     * @implSpec
     * The default implementation delegates to
     * {@link #scheduleAtFixedRate(Runnable, long, long, TimeUnit)} with the
     * runnable returned by
     * {@link ProgressMonitors#wrap(Runnable, ProgressMonitor)}.
     *
     * @param command the periodic action
     * @param initialDelay the delay before the first execution
     * @param period the period between execution start times
     * @param unit the unit of both delays
     * @param monitor the borrowed monitor
     * @return the future returned by the delegated schedule operation
     * @throws NullPointerException if command, unit, or monitor is null
     * @throws IllegalArgumentException if period is not positive
     * @throws RejectedExecutionException if scheduling is rejected
     */
    default ScheduledFuture<?> scheduleAtFixedRateMonitored(
            Runnable command, long initialDelay, long period, TimeUnit unit,
            ProgressMonitor monitor) {
        return scheduleAtFixedRate(ProgressMonitors.wrap(command, monitor),
                initialDelay, period, unit);
    }

    /**
     * Schedules a fixed-delay action with a borrowed monitor bound separately
     * for each execution. The executor retains the original fixed-delay
     * timing, exception, and future-cancellation contracts.
     *
     * @implSpec
     * The default implementation delegates to
     * {@link #scheduleWithFixedDelay(Runnable, long, long, TimeUnit)} with the
     * runnable returned by
     * {@link ProgressMonitors#wrap(Runnable, ProgressMonitor)}.
     *
     * @param command the periodic action
     * @param initialDelay the delay before the first execution
     * @param delay the delay between executions
     * @param unit the unit of both delays
     * @param monitor the borrowed monitor
     * @return the future returned by the delegated schedule operation
     * @throws NullPointerException if command, unit, or monitor is null
     * @throws IllegalArgumentException if delay is not positive
     * @throws RejectedExecutionException if scheduling is rejected
     */
    default ScheduledFuture<?> scheduleWithFixedDelayMonitored(
            Runnable command, long initialDelay, long delay, TimeUnit unit,
            ProgressMonitor monitor) {
        return scheduleWithFixedDelay(ProgressMonitors.wrap(command, monitor),
                initialDelay, delay, unit);
    }

}
