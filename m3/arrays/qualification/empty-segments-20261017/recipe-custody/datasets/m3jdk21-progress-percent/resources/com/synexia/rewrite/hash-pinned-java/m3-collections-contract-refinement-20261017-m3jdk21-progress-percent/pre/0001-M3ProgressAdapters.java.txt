// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;

/** Compatibility adapters between legacy Progress APIs and the shared long-work monitor SPI. */
public final class M3ProgressAdapters {
    private M3ProgressAdapters() {}

    public static M3Progress asLegacy(M3ProgressMonitor monitor) {
        Objects.requireNonNull(monitor, "monitor");
        return monitor instanceof M3Progress progress ? progress : new LegacyAdapter(monitor);
    }

    private static int legacyTotal(long value) {
        if (value <= 0L) return 1;
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static int legacyWorked(long value) {
        if (value <= 0L) return 0;
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static final class LegacyAdapter extends M3Progress {
        private final M3ProgressMonitor delegate;

        private LegacyAdapter(M3ProgressMonitor delegate) {
            super(1);
            this.delegate = delegate;
        }

        @Override public void beginTask(String name, int totalWork) {
            delegate.beginTask(name, totalWork);
        }
        @Override public void beginTask(String name, long totalWork) {
            delegate.beginTask(name, totalWork);
        }
        @Override public void subTask(String name) { delegate.subTask(name); }
        @Override public void worked(int work) { delegate.worked(work); }
        @Override public void worked(long work) { delegate.worked(work); }
        @Override public void setProgress(long worked, long totalWork) {
            delegate.setProgress(worked, totalWork);
        }
        @Override public void setTaskName(String taskName) { delegate.setTaskName(taskName); }
        @Override public M3Progress split(int ticks) { return asLegacy(delegate.split(ticks)); }
        @Override public M3Progress split(long ticks) { return asLegacy(delegate.split(ticks)); }
        @Override public void done() { delegate.done(); }
        @Override public void cancel() { delegate.setCanceled(true); }
        @Override public void setCanceled(boolean canceled) { delegate.setCanceled(canceled); }
        @Override public boolean isCanceled() { return delegate.isCanceled(); }
        @Override public boolean isDone() { return delegate.state().done(); }
        @Override public int getTotalWork() { return legacyTotal(delegate.state().totalWork()); }
        @Override public int getWorkedSoFar() { return legacyWorked(delegate.state().worked()); }
        @Override public String getTaskName() { return delegate.state().taskName(); }
        @Override public String getSubTaskName() { return delegate.state().subTaskName(); }
        @Override public int getPercentComplete() {
            M3ProgressState state = delegate.state();
            if (state.totalWork() <= 0L) return 0;
            return (int) Math.min(100L, state.worked() * 100L / state.totalWork());
        }
        @Override public Snapshot snapshot() {
            M3ProgressState state = delegate.state();
            return new Snapshot(
                    state.taskName(),
                    state.subTaskName(),
                    legacyWorked(state.worked()),
                    legacyTotal(state.totalWork()),
                    state.canceled(),
                    state.done(),
                    state.generation());
        }
        @Override public M3ProgressState state() { return delegate.state(); }
    }
}
