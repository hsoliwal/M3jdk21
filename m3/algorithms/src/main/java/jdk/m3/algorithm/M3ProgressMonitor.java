// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.concurrent.CancellationException;

import jdk.m3.collections.M3ProgressState;

/**
 * Generic M3 progress/cancellation contract for every long-running operation: the donor's int-unit surface
 * ({@code int UNKNOWN}, {@code beginTask(String,int)}, {@code worked(int)}, every method default, {@code none()})
 * as an adapter over the owner {@link jdk.m3.collections.M3ProgressMonitor}. The int methods widen into the
 * owner's long methods; the owner's abstract methods carry no-op defaults, so a monitor that overrides only the
 * int methods (the donor oracles) compiles and still receives every algorithm call. There is no long-to-int
 * default: a monitor meant for owner-side callers overrides the long methods.
 */
//FLLDCIM: RELOCATE - donor blob 6d0b659af853 at commit 3385d79a2968 (repository, path, URL and digests: the SUPERSEDED row of m3/algorithms/recipe/families-20261106/RELOCATION_MAP.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
//FLLDCIM: UNIFY - adapter over jdk.m3.collections.M3ProgressMonitor (int work units widen to long); recipe m3/qualification/progress-monitor-unify-20261106/unify.py v1
public interface M3ProgressMonitor extends jdk.m3.collections.M3ProgressMonitor {
    /** Unknown total work in the donor's int unit; the same value as the owner's {@code long UNKNOWN}. */
    int UNKNOWN = -1;

    default void beginTask(String name, int totalWork) {
        beginTask(name, (long) totalWork);
    }

    default void worked(int work) {
        worked((long) work);
    }

    default void internalWorked(double work) {}

    @Override default void beginTask(String name, long totalWork) {}
    @Override default void subTask(String name) {}
    @Override default void worked(long work) {}
    @Override default void setProgress(long worked, long totalWork) {}
    @Override default void setTaskName(String name) {}
    @Override default boolean isCanceled() { return false; }
    @Override default void setCanceled(boolean value) {}
    @Override default void done() {}

    @Override
    default M3ProgressState state() {
        return new M3ProgressState("", "", 0L, UNKNOWN, isCanceled(), false, 0L, 0L);
    }

    @Override
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
