// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.runtime;

import com.m3.collections.M3ProgressMonitor;
import java.util.Objects;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Thin Eclipse {@link IProgressMonitor} entry point backed by an existing M3 progress owner.
 *
 * <p>Choose {@link #owned(M3ProgressMonitor)} only for an Eclipse operation that owns the
 * progress monitor's begin/done lifecycle. Choose {@link #borrowed(M3ProgressMonitor)} for a
 * nested Eclipse/JDT/PDE call whose outer M3 operation owns that lifecycle. A borrowed
 * monitor reports task names as subtasks and intentionally does not begin or complete its owner.
 * Neither mode creates another scheduler, task store, or progress payload.</p>
 *
 * <p>{@code internalWorked(double)} preserves fractional credit across calls. Invalid,
 * negative and non-finite progress is ignored, like other Eclipse monitor adapters. Work
 * is forwarded in integral chunks because the existing M3 monitor SPI is long-based.
 * Calls are serialized per facade; the M3 backing owner determines observer semantics.</p>
 */
public final class M3EclipseProgressMonitor implements IProgressMonitor {
    private final M3ProgressMonitor owner;
    private final boolean ownsLifecycle;
    private boolean begun;
    private boolean finished;
    private double fractionalWork;

    private M3EclipseProgressMonitor(M3ProgressMonitor owner, boolean ownsLifecycle) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.ownsLifecycle = ownsLifecycle;
    }

    public static M3EclipseProgressMonitor owned(M3ProgressMonitor owner) {
        return new M3EclipseProgressMonitor(owner, true);
    }

    public static M3EclipseProgressMonitor borrowed(M3ProgressMonitor owner) {
        return new M3EclipseProgressMonitor(owner, false);
    }

    /** Returns the M3 owner; no Eclipse or M3 state snapshot is copied. */
    public M3ProgressMonitor owner() { return owner; }

    @Override
    public synchronized void beginTask(String name, int totalWork) {
        if (!ownsLifecycle) {
            owner.subTask(name);
            return;
        }
        if (begun || finished) {
            throw new IllegalStateException("Eclipse monitor lifecycle already started or completed");
        }
        if (totalWork < UNKNOWN) {
            throw new IllegalArgumentException("invalid Eclipse totalWork");
        }
        owner.beginTask(name, totalWork);
        begun = true;
    }

    @Override
    public synchronized void done() {
        if (!ownsLifecycle || finished) return;
        owner.done();
        finished = true;
    }

    @Override
    public synchronized void internalWorked(double work) {
        if (finished || !(work > 0.0d) || !Double.isFinite(work)) return;
        double pending = fractionalWork + work;
        if (!Double.isFinite(pending) || pending >= Long.MAX_VALUE) {
            owner.worked(Long.MAX_VALUE);
            fractionalWork = 0.0d;
            return;
        }
        long integral = (long) pending;
        if (integral != 0L) owner.worked(integral);
        fractionalWork = pending - integral;
    }

    @Override
    public synchronized void worked(int work) {
        if (!finished && work > 0) owner.worked(work);
    }

    @Override
    public void subTask(String name) { owner.subTask(name); }

    @Override
    public void setTaskName(String name) {
        if (ownsLifecycle) owner.setTaskName(name);
        else owner.subTask(name);
    }

    @Override
    public boolean isCanceled() {
        return owner.isCanceled() || Thread.currentThread().isInterrupted();
    }

    @Override
    public void setCanceled(boolean value) { owner.setCanceled(value); }
}
