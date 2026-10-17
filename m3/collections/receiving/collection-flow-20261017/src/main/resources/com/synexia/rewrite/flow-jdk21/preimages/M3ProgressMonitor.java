// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

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
}
