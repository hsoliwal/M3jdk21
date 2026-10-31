// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: explicit equivalent public no-argument constructor.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe dependency-light progress monitor used by the process-wide system root. */
public final class M3DefaultProgressMonitor implements M3ProgressMonitor {
    private final AtomicReference<String> taskName = new AtomicReference<>("");
    private final AtomicReference<String> subTaskName = new AtomicReference<>("");
    private final AtomicLong worked = new AtomicLong();
    private final AtomicLong totalWork = new AtomicLong(UNKNOWN);
    private final AtomicLong startedAtMs = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean canceled = new AtomicBoolean();
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicLong generation = new AtomicLong();

    /** Creates an independent monitor with the default unknown total-work state. */
    public M3DefaultProgressMonitor() { }

    @Override
    public void beginTask(String taskName, long totalWork) {
        if (totalWork < UNKNOWN) throw new IllegalArgumentException("invalid totalWork");
        generation.incrementAndGet();
        this.taskName.set(Objects.requireNonNullElse(taskName, ""));
        subTaskName.set("");
        worked.set(0L);
        this.totalWork.set(totalWork);
        startedAtMs.set(System.currentTimeMillis());
        done.set(false);
    }

    @Override public void done() { done.set(true); }

    @Override
    public void worked(long work) {
        if (work < 0L) throw new IllegalArgumentException("work must be non-negative");
        if (work == 0L || done.get()) return;
        worked.updateAndGet(current -> saturatedAdd(current, work));
    }

    @Override
    public void setProgress(long worked, long totalWork) {
        if (worked < 0L || totalWork < UNKNOWN) {
            throw new IllegalArgumentException("invalid progress");
        }
        this.worked.set(worked);
        this.totalWork.set(totalWork);
    }

    @Override public void setTaskName(String taskName) {
        this.taskName.set(Objects.requireNonNullElse(taskName, ""));
    }
    @Override public void subTask(String subTaskName) {
        this.subTaskName.set(Objects.requireNonNullElse(subTaskName, ""));
    }
    @Override public boolean isCanceled() { return canceled.get(); }
    @Override public void setCanceled(boolean canceled) { this.canceled.set(canceled); }

    @Override
    public M3ProgressMonitor split(long parentWork) {
        return new M3ChildProgressMonitor(this, parentWork, generation.get());
    }

    @Override
    public M3ProgressState state() {
        return new M3ProgressState(
                taskName.get(),
                subTaskName.get(),
                worked.get(),
                totalWork.get(),
                canceled.get(),
                done.get(),
                generation.get(),
                startedAtMs.get());
    }

    private static long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }
}
