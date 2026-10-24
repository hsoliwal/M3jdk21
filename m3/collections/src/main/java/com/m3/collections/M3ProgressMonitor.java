/*******************************************************************************
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
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
package com.m3.collections;

import java.io.InputStream;
import java.io.OutputStream;

import java.util.concurrent.CancellationException;

/** Dependency-light progress/cancellation SPI shared below job and collection layers. */
public interface M3ProgressMonitor {
    long UNKNOWN = -1L;

    void beginTask(String taskName, long totalWork);
    void done();
    void worked(long work);
    void setProgress(long worked, long totalWork);
    void setTaskName(String taskName);
    void subTask(String subTaskName);
    boolean isCanceled();
    void setCanceled(boolean canceled);
    M3ProgressState state();

    default void checkCanceled() {
        if (isCanceled()) {
            throw new CancellationException("Task canceled: " + state().taskName());
        }
    }

    default M3ProgressMonitor split(long parentWork) {
        return new M3ChildProgressMonitor(this, parentWork, state().generation());
    }

    /**
     * Consume the original InputStream once; borrowed shadow outputs observe only
     * bytes the primary has consumed. The split child reports completed byte work.
     */
    default InputStream shadowInput(InputStream source, long parentWork,
            long expectedBytes, OutputStream... shadows) {
        if (expectedBytes < UNKNOWN) throw new IllegalArgumentException("negative stream work");
        M3ProgressMonitor child = split(parentWork);
        child.beginTask("Shadow input", expectedBytes);
        return M3ShadowStreams.input(source, child, shadows);
    }

    /** Mirror successful primary output writes to borrowed observers without rereading. */
    default OutputStream shadowOutput(OutputStream output, long parentWork,
            long expectedBytes, OutputStream... shadows) {
        if (expectedBytes < UNKNOWN) throw new IllegalArgumentException("negative stream work");
        M3ProgressMonitor child = split(parentWork);
        child.beginTask("Shadow output", expectedBytes);
        return M3ShadowStreams.output(output, child, shadows);
    }
}
