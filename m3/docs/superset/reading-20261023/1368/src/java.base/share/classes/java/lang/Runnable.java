/*
 * Copyright (c) 1994, 2022, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * Represents an operation that does not return a result.
 *
 * <p> This is a {@linkplain java.util.function functional interface}
 * whose functional method is {@link #run()}.
 *
 * @author  Arthur van Hoff
 * @see     java.util.concurrent.Callable
 * @since   1.0
 */
@FunctionalInterface
public interface Runnable {
    /**
     * Runs this operation.
     */
    void run();

    /**
     * The ambient, never-null M3 progress monitor for this execution thread.
     * A pool must bind it explicitly when dispatching work asynchronously.
     *
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * @return the current thread's bound monitor, or the process fallback
=======
     * @return the current lexical monitor, or the process fallback monitor
>>>>>>> 82eb7fc30da9d3cf5556a11fb4a2b4ec0f09f385
     */
    default java.util.concurrent.ProgressMonitor progressMonitor() {
        return java.util.concurrent.ProgressMonitors.current();
    }

    /**
     * Run under a borrowed monitor without completing its parent.
     * Existing run() remains the sole abstract method.
     *
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * @param monitor the non-null monitor to bind for this execution
=======
     * @param monitor the non-null monitor borrowed for this invocation
     * @throws NullPointerException if {@code monitor} is null
>>>>>>> 82eb7fc30da9d3cf5556a11fb4a2b4ec0f09f385
     */
    default void run(java.util.concurrent.ProgressMonitor monitor) {
        java.util.concurrent.ProgressMonitors.run(this, monitor);
    }

}
