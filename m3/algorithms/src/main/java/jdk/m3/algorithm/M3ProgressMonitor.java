// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.concurrent.CancellationException;

/** Generic M3 progress/cancellation contract for every long-running operation. */
//FLLDCIM: RELOCATE - donor blob 6d0b659af853 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public interface M3ProgressMonitor {
    int UNKNOWN = -1;

    default void beginTask(String name, int totalWork) {}
    default void subTask(String name) {}
    default void worked(int work) {}
    default void internalWorked(double work) {}
    default void setTaskName(String name) {}
    default boolean isCanceled() { return false; }
    default void setCanceled(boolean value) {}
    default void done() {}

    default void checkCanceled() {
        if (isCanceled()) {
            throw new CancellationException("M3 operation canceled");
        }
    }

    static M3ProgressMonitor none() {
        return None.INSTANCE;
    }

    enum None implements M3ProgressMonitor {
        INSTANCE
    }
}
