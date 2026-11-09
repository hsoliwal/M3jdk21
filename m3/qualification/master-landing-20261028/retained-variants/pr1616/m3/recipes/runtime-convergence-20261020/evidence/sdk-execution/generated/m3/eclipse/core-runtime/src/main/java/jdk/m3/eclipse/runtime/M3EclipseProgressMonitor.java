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
 * Calls are serialized per facade; the M3 backing owner determines observer semantics.
 * Synchronous owner callbacks may reenter this facade. An active begin cannot begin
 * again, and an active completion suppresses recursive completion and further work.
 * Lifecycle callback failures propagate unchanged and leave that operation retryable.
 * Fractional accounting is published before delivering work to the owner. On a
 * delivery failure, the previous fraction is restored only if no reentrant fractional
 * update occurred; nested updates and external owner effects cannot be rolled back.</p>
 */
public final class M3EclipseProgressMonitor implements IProgressMonitor {
    private final M3ProgressMonitor owner;
    private final boolean ownsLifecycle;
    private boolean begun;
    private boolean finished;
    private boolean beginning;
    private boolean finishing;
    private double fractionalWork;
    private long fractionalRevision;

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
        if (begun || finished || beginning || finishing) {
            throw new IllegalStateException("Eclipse monitor lifecycle already started or completed");
        }
        if (totalWork < UNKNOWN) {
            throw new IllegalArgumentException("invalid Eclipse totalWork");
        }
        beginning = true;
        try {
            owner.beginTask(name, totalWork);
            begun = true;
        } finally {
            beginning = false;
        }
    }

    @Override
    public synchronized void done() {
        if (!ownsLifecycle || finished || finishing) return;
        finishing = true;
        try {
            owner.done();
            finished = true;
        } finally {
            finishing = false;
        }
    }

    @Override
    public synchronized void internalWorked(double work) {
        if (finished || finishing || !(work > 0.0d) || !Double.isFinite(work)) return;
        double pending = fractionalWork + work;
        if (!Double.isFinite(pending) || pending >= Long.MAX_VALUE) {
            forwardWork(Long.MAX_VALUE, 0.0d);
            return;
        }
        long integral = (long) pending;
        forwardWork(integral, pending - integral);
    }

    private void forwardWork(long integral, double remainder) {
        double previous = fractionalWork;
        long previousRevision = fractionalRevision;
        long revision = ++fractionalRevision;
        fractionalWork = remainder;
        try {
            if (integral != 0L) owner.worked(integral);
        } catch (RuntimeException | Error failure) {
            if (fractionalRevision == revision) {
                fractionalWork = previous;
                fractionalRevision = previousRevision;
            }
            throw failure;
        }
    }

    @Override
    public synchronized void worked(int work) {
        if (!finished && !finishing && work > 0) owner.worked(work);
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
