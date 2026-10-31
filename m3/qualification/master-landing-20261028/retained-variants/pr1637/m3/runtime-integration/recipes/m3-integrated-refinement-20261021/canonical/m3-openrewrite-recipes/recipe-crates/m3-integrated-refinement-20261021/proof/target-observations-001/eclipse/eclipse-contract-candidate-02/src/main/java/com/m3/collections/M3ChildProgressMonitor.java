// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.math.BigInteger;
import java.util.Objects;

/** Weighted single-use child for the dependency-light common progress SPI. */
final class M3ChildProgressMonitor implements M3ProgressMonitor {
    private final M3ProgressMonitor parent;
    private final long parentWork;
    private final long expectedParentGeneration;
    private String taskName = "";
    private String subTaskName = "";
    private long worked;
    private long totalWork = UNKNOWN;
    private long forwarded;
    private long startedAtMs = System.currentTimeMillis();
    private boolean canceled;
    private boolean done;
    private boolean started;
    private long generation;

    M3ChildProgressMonitor(
            M3ProgressMonitor parent,
            long parentWork,
            long expectedParentGeneration) {
        this.parent = Objects.requireNonNull(parent, "parent");
        if (parentWork < 0L) throw new IllegalArgumentException("parentWork must be non-negative");
        this.parentWork = parentWork;
        this.expectedParentGeneration = expectedParentGeneration;
    }

    @Override
    public void beginTask(String taskName, long totalWork) {
        synchronized (this) {
            if (started || worked != 0L || done) {
                throw new IllegalStateException("child progress is single-use");
            }
            if (totalWork < UNKNOWN) throw new IllegalArgumentException("invalid totalWork");
            started = true;
            generation = Math.incrementExact(generation);
            this.taskName = Objects.requireNonNullElse(taskName, "");
            subTaskName = "";
            worked = 0L;
            this.totalWork = totalWork;
            startedAtMs = System.currentTimeMillis();
        }
        parent.subTask(this.taskName);
    }

    @Override
    public void done() {
        long delta;
        synchronized (this) {
            if (done) return;
            done = true;
            if (totalWork > 0L) worked = totalWork;
            delta = advance(parentWork);
        }
        creditParent(delta);
    }

    @Override
    public void worked(long work) {
        if (work < 0L) throw new IllegalArgumentException("work must be non-negative");
        if (work == 0L) return;
        long delta;
        synchronized (this) {
            if (done) return;
            worked = boundedAdd(worked, work, totalWork);
            delta = advance(targetParentWork());
        }
        creditParent(delta);
    }

    @Override
    public void setProgress(long worked, long totalWork) {
        if (worked < 0L || totalWork < UNKNOWN) {
            throw new IllegalArgumentException("invalid progress");
        }
        long delta;
        synchronized (this) {
            if (done) return;
            this.totalWork = totalWork;
            this.worked = totalWork > 0L ? Math.min(worked, totalWork) : worked;
            delta = advance(targetParentWork());
        }
        creditParent(delta);
    }

    @Override public void setTaskName(String taskName) {
        String value = Objects.requireNonNullElse(taskName, "");
        synchronized (this) { this.taskName = value; }
        parent.subTask(value);
    }
    @Override public void subTask(String subTaskName) {
        String value = Objects.requireNonNullElse(subTaskName, "");
        synchronized (this) { this.subTaskName = value; }
        parent.subTask(value);
    }
    @Override public boolean isCanceled() {
        synchronized (this) { if (canceled) return true; }
        return parent.isCanceled();
    }
    @Override public void setCanceled(boolean canceled) {
        synchronized (this) { this.canceled = canceled; }
        if (canceled) parent.setCanceled(true);
    }
    @Override public M3ProgressMonitor split(long parentWork) {
        return new M3ChildProgressMonitor(this, parentWork, state().generation());
    }
    @Override public synchronized M3ProgressState state() {
        return new M3ProgressState(
                taskName, subTaskName, worked, totalWork, isCanceled(), done, generation, startedAtMs);
    }

    private void creditParent(long delta) {
        if (delta <= 0L || parent.state().generation() != expectedParentGeneration) return;
        if (!parent.state().done()) parent.worked(delta);
    }
    private long targetParentWork() {
        if (done) return parentWork;
        if (totalWork <= 0L || worked <= 0L || parentWork == 0L) return 0L;
        if (worked >= totalWork) return parentWork;
        return scale(worked, parentWork, totalWork);
    }
    private long advance(long target) {
        long bounded = Math.max(forwarded, Math.min(parentWork, target));
        long delta = bounded - forwarded;
        forwarded = bounded;
        return delta;
    }
    private static long scale(long value, long weight, long total) {
        try {
            return Math.multiplyExact(value, weight) / total;
        } catch (ArithmeticException overflow) {
            return BigInteger.valueOf(value).multiply(BigInteger.valueOf(weight))
                    .divide(BigInteger.valueOf(total)).longValueExact();
        }
    }
    private static long boundedAdd(long current, long increment, long total) {
        long added;
        try {
            added = Math.addExact(current, increment);
        } catch (ArithmeticException overflow) {
            return total > 0L ? total : Long.MAX_VALUE;
        }
        return total > 0L ? Math.min(total, added) : added;
    }
}
