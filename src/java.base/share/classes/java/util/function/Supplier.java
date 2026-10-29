/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.function;

/**
 * Represents a supplier of results.
 *
 * <p>There is no requirement that a new or distinct result be returned each
 * time the supplier is invoked.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #get()}.
 *
 * @param <T> the type of results supplied by this supplier
 *
 * @since 1.8
 */
@FunctionalInterface
public interface Supplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get();

<<<<<<< HEAD
    /**
     * Gets a result with a borrowed monitor scoped to the call.
     * The previous binding is restored on exit and the monitor is not completed.
     *
     * @param monitor the borrowed monitor
     * @return the supplied result
     * @throws NullPointerException if {@code monitor} is {@code null}
     * @throws java.util.concurrent.CancellationException if cancellation is
     *         observed before or after this operation
     */
    @SuppressWarnings("try")
||||||| merged common ancestors
    /** Execute with an explicit borrowed monitor, retaining the original SAM. */
=======
    /**
     * Obtains a result under an explicit borrowed monitor without completing it.
     * The original functional method remains the sole abstract method.
     * Cancellation is checked before and after normal completion.
     *
     * @param monitor the borrowed progress monitor
     * @return the supplied result
     * @throws NullPointerException if monitor is null
     * @throws java.util.concurrent.CancellationException if cancellation is detected
     */
    @SuppressWarnings("try")
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    default T get(java.util.concurrent.ProgressMonitor monitor) {
        java.util.Objects.requireNonNull(monitor, "monitor");
        final java.util.concurrent.ProgressMonitors.Scope scope =
                java.util.concurrent.ProgressMonitors.bind(monitor);
        try (scope) {
            monitor.checkCancelled();
            T result = get();
            monitor.checkCancelled();
            return result;
        }
    }

}
