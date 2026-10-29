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
 * A task that returns a result and may throw an exception.
 * Implementors define a single method with no arguments called
 * {@code call}.
 *
 * <p>The {@code Callable} interface is similar to {@link
 * java.lang.Runnable}, in that both are designed for classes whose
 * instances are potentially executed by another thread.  A
 * {@code Runnable}, however, does not return a result and cannot
 * throw a checked exception.
 *
 * <p>The {@link Executors} class contains utility methods to
 * convert from other common forms to {@code Callable} classes.
 *
 * @see Executor
 * @since 1.5
 * @author Doug Lea
 * @param <V> the result type of method {@code call}
 */
@FunctionalInterface
public interface Callable<V> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    V call() throws Exception;

<<<<<<< HEAD
    /**
     * Computes this task's result with a borrowed monitor scoped to this call.
     * The no-argument {@link #call()} remains the sole abstract method.
     *
     * @param monitor the borrowed monitor to bind during this call
     * @return the computed result
     * @throws NullPointerException if {@code monitor} is {@code null}
     * @throws CancellationException if cancellation is observed before or
     *         after this call
     * @throws Exception if this task cannot compute a result
     */
||||||| 2f2beb2eff7
    /** A borrowed monitor is scoped for this call; call() remains abstract. */
=======
    /**
     * Calls this task under a borrowed monitor without completing it.
     * The original {@code call()} remains the sole abstract method.
     * Cancellation is checked before and after normal completion.
     *
     * @param monitor the borrowed progress monitor
     * @return the result of the task
     * @throws Exception if the task fails
     * @throws NullPointerException if monitor is null
     * @throws CancellationException if cancellation is detected
     */
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
    default V call(ProgressMonitor monitor) throws Exception {
        return ProgressMonitors.call(this, monitor);
    }

}
