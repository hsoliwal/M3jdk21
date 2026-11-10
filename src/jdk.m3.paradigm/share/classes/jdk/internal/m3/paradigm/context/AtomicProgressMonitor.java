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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe hierarchical progress monitor with shared cancellation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextAtomicProgressMonitor.java}
 * blob {@code 5e5fc9d7fd795d4d6d2da2477a9770544b990622} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class AtomicProgressMonitor
        implements ContextProgressMonitor {

    private final AtomicBoolean cancellation;
    private final CopyOnWriteArrayList<AtomicProgressMonitor> children =
            new CopyOnWriteArrayList<>();
    private final AtomicReference<String> task =
            new AtomicReference<>("");
    private final AtomicReference<String> subTask =
            new AtomicReference<>("");
    private final AtomicReference<String> message =
            new AtomicReference<>("");
    private final AtomicLong totalWork =
            new AtomicLong(UNKNOWN_WORK);
    private final AtomicLong worked = new AtomicLong();
    private final AtomicBoolean done = new AtomicBoolean();

    public AtomicProgressMonitor() {
        this(new AtomicBoolean());
    }

    private AtomicProgressMonitor(
            AtomicBoolean cancellation) {
        this.cancellation = Objects.requireNonNull(
                cancellation, "cancellation");
    }

    @Override
    public void begin(String name, long total) {
        if (total < UNKNOWN_WORK) {
            throw new IllegalArgumentException(
                    "totalWork must be -1 or >= 0");
        }
        task.set(Objects.requireNonNullElse(name, ""));
        totalWork.set(total);
        worked.set(0L);
        subTask.set("");
        message.set("");
        done.set(false);
    }

    @Override
    public void subTask(String name) {
        subTask.set(Objects.requireNonNullElse(name, ""));
    }

    @Override
    public void worked(long units) {
        if (units < 0L) {
            throw new IllegalArgumentException(
                    "work must be >= 0");
        }
        if (units != 0L) {
            worked.getAndAccumulate(
                    units,
                    AtomicProgressMonitor::addSaturated);
        }
    }

    @Override
    public void message(String nextMessage) {
        message.set(Objects.requireNonNullElse(
                nextMessage, ""));
    }

    @Override
    public boolean isCancelled() {
        return cancellation.get();
    }

    @Override
    public void cancel() {
        cancellation.set(true);
    }

    @Override
    public void setCancelled(boolean value) {
        cancellation.set(value);
    }

    @Override
    public void done() {
        long total = totalWork.get();
        if (total >= 0L) {
            worked.accumulateAndGet(total, Math::max);
        }
        done.set(true);
    }

    @Override
    public ContextProgressMonitor child(
            String name,
            long total) {
        AtomicProgressMonitor child =
                new AtomicProgressMonitor(cancellation);
        child.begin(name, total);
        children.add(child);
        return child;
    }

    @Override
    public ProgressSnapshot snapshot() {
        List<ProgressSnapshot> childSnapshots =
                children.stream()
                        .map(AtomicProgressMonitor::snapshot)
                        .toList();
        String detail = message.get();
        if (detail.isEmpty()) {
            detail = subTask.get();
        }
        return new ProgressSnapshot(
                task.get(),
                totalWork.get(),
                worked.get(),
                detail,
                done.get(),
                cancellation.get(),
                childSnapshots);
    }

    public long completedWork() {
        return worked.get();
    }

    public long totalWork() {
        return totalWork.get();
    }

    public String task() {
        return task.get();
    }

    public String subTask() {
        return subTask.get();
    }

    public boolean isDone() {
        return done.get();
    }

    public double fraction() {
        return snapshot().fraction();
    }

    private static long addSaturated(long left, long right) {
        long result = left + right;
        return result < 0L || result < left
                ? Long.MAX_VALUE
                : result;
    }
}
