// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.concurrent.CancellationException;
import jdk.m3.collections.ProgressMonitor;
import jdk.m3.collections.ProgressState;

/**
 * Progress/cancellation contract for non-constant-time pool operations: the immutable-pool donor's surface
 * ({@code begin(String,long)}, {@code worked(long)}, {@code cancelled()}, {@code done()}, {@code checkCancelled()},
 * every method default, {@code none()}) as an adapter over the owner {@link ProgressMonitor} of this module.
 * {@code begin} delegates to {@code beginTask} and {@code cancelled} to {@code isCanceled}, so an owner-native
 * monitor drives pool operations unchanged; the owner's abstract methods carry no-op defaults, so a monitor that
 * overrides only the donor methods (the pool oracles' proxies) still compiles and still receives every pool call.
 */
//FLLDCIM: RELOCATE - donor blob 6328187b7bf3 at commit 3385d79a2968 (repository, path, URL and digests: the SUPERSEDED row of make/m3/collections/recipe/immutable-pools-20261106/RELOCATION_MAP.tsv); recipe make/m3/collections/recipe/immutable-pools-20261106/relocate.py v2
//FLLDCIM: UNIFY - adapter over jdk.m3.collections.ProgressMonitor (long units on both sides; begin -> beginTask, cancelled -> isCanceled); recipe make/m3/qualification/progress-monitor-unify-20261106/unify.py v1
public interface PoolProgressMonitor extends ProgressMonitor {
    /** Unknown total work: the owner's value, redeclared under the donor-visible name. */
    long UNKNOWN = -1L;

    default void begin(String task, long totalWork) {
        beginTask(task, totalWork);
    }

    default boolean cancelled() {
        return isCanceled();
    }

    default void checkCancelled() {
        if (cancelled()) throw new CancellationException("immutable-pool operation cancelled");
    }

    @Override default void beginTask(String taskName, long totalWork) { }
    @Override default void worked(long work) { }
    @Override default void done() { }
    @Override default void setProgress(long worked, long totalWork) { }
    @Override default void setTaskName(String taskName) { }
    @Override default void subTask(String subTaskName) { }
    @Override default boolean isCanceled() { return false; }
    @Override default void setCanceled(boolean canceled) { }

    @Override
    default ProgressState state() {
        return new ProgressState("", "", 0L, UNKNOWN, isCanceled(), false, 0L, 0L);
    }

    static PoolProgressMonitor none() {
        return None.INSTANCE;
    }

    enum None implements PoolProgressMonitor {
        INSTANCE
    }
}
