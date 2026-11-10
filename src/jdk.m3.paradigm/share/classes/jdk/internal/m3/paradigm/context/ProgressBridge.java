/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import jdk.m3.collections.Progress;
import java.util.List;
import java.util.Objects;

/**
 * Adapts the existing int-based Progress API to COP's native long-based monitor.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/runtime/ProgressBridge.java}
 * blob {@code 26038201f0c2666c87e0c0f08aab962eae772fc0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ProgressBridge implements ContextProgressMonitor {
    private final Progress progress;

    private ProgressBridge(Progress progress) { this.progress = progress; }

    public static ContextProgressMonitor context(Progress progress) {
        Objects.requireNonNull(progress, "progress");
        if (progress == Progress.NULL) return NullProgressMonitor.instance();
        return progress instanceof Legacy legacy ? legacy.monitor : new ProgressBridge(progress);
    }

    public static Progress legacy(ContextProgressMonitor monitor) {
        Objects.requireNonNull(monitor, "monitor");
        if (monitor == NullProgressMonitor.instance() || monitor == ContextProgressMonitor.NONE) return Progress.NULL;
        return monitor instanceof ProgressBridge bridge ? bridge.progress : new Legacy(monitor);
    }

    @Override public void begin(String task, long total) { progress.beginTask(task, legacyUnits(total)); }
    @Override public void worked(long units) {
        if (units < 0) throw new IllegalArgumentException("units must be non-negative");
        progress.worked(legacyUnits(units));
    }
    @Override public void message(String message) { progress.subTask(message); }
    @Override public boolean isCancelled() { return progress.isCanceled(); }
    @Override public void cancel() { progress.cancel(); }
    @Override public void done() { progress.done(); }
    @Override public ContextProgressMonitor child(String task, long total) {
        return context(progress.split(total < 0 ? 1 : legacyUnits(total)));
    }
    @Override public ProgressSnapshot snapshot() {
        Progress.Snapshot state = progress.snapshot();
        return new ProgressSnapshot(state.taskName(), state.total(), state.worked(),
                state.subTaskName(), state.done(), progress.isCanceled(), List.of());
    }

    /** Legacy counters saturate; native monitors retain their original long precision. */
    private static int legacyUnits(long value) {
        if (value < UNKNOWN_WORK) throw new IllegalArgumentException("invalid work");
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    private static final class Legacy extends Progress {
        private final ContextProgressMonitor monitor;
        Legacy(ContextProgressMonitor monitor) { super(1); this.monitor = monitor; }
        @Override public void beginTask(String name, int total) { monitor.begin(name, total); }
        @Override public void subTask(String name) { monitor.message(name); }
        @Override public void worked(int units) { monitor.worked(units); }
        @Override public Progress split(int ticks) {
            if (ticks < 0) throw new IllegalArgumentException("ticks must be non-negative");
            return legacy(monitor.child("", ticks));
        }
        @Override public void done() { monitor.done(); }
        @Override public void cancel() { monitor.cancel(); }
        @Override public boolean isCanceled() { return monitor.isCancelled(); }
        @Override public boolean isDone() { return monitor.snapshot().done(); }
        @Override public int getPercentComplete() { return (int) (monitor.snapshot().fraction() * 100); }
        @Override public int getTotalWork() { return legacyUnits(monitor.snapshot().totalWork()); }
        @Override public int getWorkedSoFar() { return legacyUnits(monitor.snapshot().worked()); }
        @Override public String getTaskName() { return monitor.snapshot().task(); }
        @Override public String getSubTaskName() { return monitor.snapshot().message(); }
        @Override public Snapshot snapshot() {
            ProgressSnapshot state = monitor.snapshot();
            return new Snapshot(state.task(), state.message(), legacyUnits(state.worked()),
                    legacyUnits(state.totalWork()), state.cancelled(), state.done(), 0L);
        }
    }
}
