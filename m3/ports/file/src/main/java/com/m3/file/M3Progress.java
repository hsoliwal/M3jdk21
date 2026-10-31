// Copyright 2026 Hitesh Soliwal and contributors
// Full donor dependency receiver; source custody in JSON_CLOSURE_PROVENANCE.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.concurrent.CancellationException;

/** Dependency-light progress/cancellation SPI shared below job and collection layers. */
public interface M3Progress {
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

    default M3Progress split(long parentWork) {
        return new M3ProgressChild(this, parentWork, state().generation());
    }
}
