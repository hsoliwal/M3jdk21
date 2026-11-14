/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import java.util.List;

/**
 * Progress and cancellation contract inherited by ContextOS execution.
 *
 * <p>The API keeps the legacy {@code beginTask/subTask} aliases while the
 * canonical surface is {@code begin/message}. Implementations may be null
 * objects; callers never need a nullable monitor.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextProgressMonitor.java}
 * blob {@code b7687262d91678282065f3b511f8692713c35dce} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ContextProgressMonitor {
    long UNKNOWN_WORK = -1L;
    long UNKNOWN = UNKNOWN_WORK;

    ContextProgressMonitor NONE = new ContextProgressMonitor() { };

    default void begin(String task, long totalWork) { }

    default void beginTask(String task, long totalWork) {
        begin(task, totalWork);
    }

    default void subTask(String task) {
        message(task);
    }

    default void worked(long work) { }

    default void message(String message) { }

    default boolean isCancelled() {
        return false;
    }

    default void cancel() { }

    default void setCancelled(boolean cancelled) {
        if (cancelled) {
            cancel();
        }
    }

    default void done() { }

    default ProgressSnapshot snapshot() {
        return new ProgressSnapshot(
                "", UNKNOWN_WORK, 0L, "", false,
                isCancelled(), List.of());
    }

    default void checkCancelled() {
        if (isCancelled() || Thread.currentThread().isInterrupted()) {
            throw new CancelledException( // DONOR_BUG_FIX (2026-11-10, com.synexia #16982)
                    "Context operation cancelled");
        }
    }

    /**
     * Create a nested monitor. Implementations may return themselves when
     * hierarchy is irrelevant.
     */
    default ContextProgressMonitor child(
            String task,
            long totalWork) {
        return this == NONE
                || this == NullProgressMonitor.instance()
                ? this
                : ProgressChild.child(this, task, totalWork);
    }
}
