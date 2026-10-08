// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.m3.collections;

import java.util.function.Consumer;

/**
 * Thread-safe progress with cumulative weighted children and a no-op singleton.
 *
 * <p>Configure a task before starting workers. A root may be restarted; children
 * from an earlier generation cannot credit its new task. Splits allocate a
 * contribution, not a reservation: callers choose child weights within the
 * parent's budget. Listeners run on the updating thread, outside monitor locks.
 */
public class M3Progress implements M3ProgressMonitor {
    public static final M3Progress NULL = new NullProgress();
    private static final int PERCENT = 100;
    private int totalWork;
    private int workedSoFar;
    private String taskName = "";
    private String subTaskName = "";
    private boolean canceled;
    private boolean done;
    private long generation;
    private long startedAtMs = System.currentTimeMillis();
    private Consumer<M3Progress> listener;

    protected M3Progress(int totalWork) {
        this.totalWork = Math.max(1, totalWork);
    }

    public static M3Progress of(int totalWork) { return new M3Progress(totalWork); }

    public static M3Progress of(int totalWork, Consumer<M3Progress> listener) {
        M3Progress progress = new M3Progress(totalWork);
        progress.listener = listener;
        return progress;
    }

    /** Starts/restarts a root task; cancellation remains sticky. */
    public void beginTask(String name, int totalWork) {
        synchronized (this) {
            generation = Math.incrementExact(generation);
            startedAtMs = System.currentTimeMillis();
            this.totalWork = Math.max(1, totalWork);
            taskName = name == null ? "" : name;
            subTaskName = "";
            workedSoFar = 0;
            done = false;
        }
        notifyListener();
    }

    @Override
    public void beginTask(String name, long totalWork) {
        if (totalWork < UNKNOWN) throw new IllegalArgumentException("invalid totalWork");
        beginTask(name, totalWork <= 0L ? 1 : Math.toIntExact(totalWork));
    }

    public void subTask(String name) {
        synchronized (this) { subTaskName = name == null ? "" : name; }
        notifyListener();
    }

    public void worked(int work) {
        if (work < 0) throw new IllegalArgumentException("work must be non-negative");
        synchronized (this) {
            if (done) return;
            workedSoFar = (int) Math.min(totalWork, (long) workedSoFar + work);
        }
        onWorkChanged();
        notifyListener();
    }

    @Override
    public void worked(long work) {
        if (work < 0L || work > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("legacy work outside int range");
        }
        worked((int) work);
    }

    @Override
    public void setProgress(long worked, long totalWork) {
        if (worked < 0L || totalWork < UNKNOWN || worked > Integer.MAX_VALUE
                || totalWork > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("legacy progress outside int range");
        }
        synchronized (this) {
            this.totalWork = totalWork <= 0L ? 1 : (int) totalWork;
            this.workedSoFar = (int) Math.min(this.totalWork, worked);
            done = false;
        }
        onWorkChanged();
        notifyListener();
    }

    @Override
    public void setTaskName(String taskName) {
        synchronized (this) { this.taskName = taskName == null ? "" : taskName; }
        notifyListener();
    }

    public M3Progress split(int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("ticks must be non-negative");
        return new SubProgress(this, ticks, snapshot().generation());
    }

    @Override
    public M3Progress split(long ticks) {
        if (ticks < 0L || ticks > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("legacy split outside int range");
        }
        return split((int) ticks);
    }

    /** Credits the remainder once. Failure/cancellation should not call done(). */
    public void done() {
        synchronized (this) {
            if (done) return;
            done = true;
            workedSoFar = totalWork;
        }
        onWorkChanged();
        notifyListener();
    }

    public synchronized void cancel() { canceled = true; }
    @Override public synchronized boolean isCanceled() { return canceled; }
    @Override public synchronized void setCanceled(boolean canceled) { this.canceled = canceled; }
    public synchronized boolean isDone() { return done; }
    public synchronized int getPercentComplete() {
        return (int) ((long) workedSoFar * PERCENT / totalWork);
    }
    public synchronized int getTotalWork() { return totalWork; }
    public synchronized int getWorkedSoFar() { return workedSoFar; }
    public synchronized String getTaskName() { return taskName; }
    public synchronized String getSubTaskName() { return subTaskName; }

    /** Coherent state for observers; child cancellation also consults its parent. */
    public synchronized Snapshot snapshot() {
        return new Snapshot(taskName, subTaskName, workedSoFar, totalWork, canceled, done, generation);
    }

    public record Snapshot(String taskName, String subTaskName, int worked, int total,
                           boolean canceled, boolean done, long generation) { }

    @Override
    public synchronized M3ProgressState state() {
        return new M3ProgressState(
                taskName, subTaskName, workedSoFar, totalWork, canceled, done, generation, startedAtMs);
    }

    /** Called outside this monitor's lock after work changes. */
    protected void onWorkChanged() { }

    private void credit(int work, long expectedGeneration) {
        synchronized (this) {
            if (done || generation != expectedGeneration) return;
            workedSoFar = (int) Math.min(totalWork, (long) workedSoFar + work);
        }
        onWorkChanged();
        notifyListener();
    }

    private void notifyListener() {
        if (listener != null) listener.accept(this);
    }

    @Override
    public String toString() {
        Snapshot state = snapshot();
        return "Progress[" + state.worked() + "/" + state.total() + " "
                + (state.done() ? "DONE" : isCanceled() ? "CANCELED" : "RUNNING") + "]";
    }

    static final class SubProgress extends M3Progress {
        private final M3Progress parent;
        private final int parentTicks;
        private final long parentGeneration;
        private int forwarded;
        private boolean started;

        SubProgress(M3Progress parent, int parentTicks, long parentGeneration) {
            super(PERCENT);
            this.parent = parent;
            this.parentTicks = parentTicks;
            this.parentGeneration = parentGeneration;
        }

        /** A split is single-use; restarting it could credit work twice. */
        @Override
        public void beginTask(String name, int totalWork) {
            synchronized (this) {
                if (started || getWorkedSoFar() != 0 || isDone()) {
                    throw new IllegalStateException("child progress is single-use");
                }
                started = true;
            }
            super.beginTask(name, totalWork);
        }

        @Override
        protected void onWorkChanged() {
            int delta;
            synchronized (this) {
                Snapshot state = snapshot();
                int cumulative = (int) ((long) state.worked() * parentTicks / state.total());
                delta = cumulative - forwarded;
                forwarded = cumulative;
            }
            if (delta > 0) parent.credit(delta, parentGeneration);
        }

        @Override
        public boolean isCanceled() { return super.isCanceled() || parent.isCanceled(); }
        @Override
        public void cancel() { super.cancel(); parent.cancel(); }
    }

    static final class NullProgress extends M3Progress {
        NullProgress() { super(1); }
        @Override public void beginTask(String name, int totalWork) { }
        @Override public void beginTask(String name, long totalWork) { }
        @Override public void subTask(String name) { }
        @Override public void worked(int work) { }
        @Override public void worked(long work) { }
        @Override public void setProgress(long worked, long totalWork) { }
        @Override public void setTaskName(String taskName) { }
        @Override public M3Progress split(int ticks) { return this; }
        @Override public M3Progress split(long ticks) { return this; }
        @Override public void done() { }
        @Override public void cancel() { }
        @Override public void setCanceled(boolean canceled) { }
    }
}
