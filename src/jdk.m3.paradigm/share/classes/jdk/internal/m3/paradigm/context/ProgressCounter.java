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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe general-purpose monitor suitable as the process/global or a context-local monitor.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextProgressCounter.java}
 * blob {@code 79cd1a5330115d871927d305fe5f000048fee7e5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ProgressCounter implements ContextProgressMonitor {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public ProgressCounter() {
    }

    public record Snapshot(String task, long totalWork, long worked, String message,
                           boolean cancelled, boolean done) { }

    private final AtomicReference<String> task = new AtomicReference<>("");
    private final AtomicReference<String> message = new AtomicReference<>("");
    private final AtomicLong total = new AtomicLong(UNKNOWN);
    private final AtomicLong worked = new AtomicLong();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean done = new AtomicBoolean();

    @Override public void begin(String taskName, long totalWork) {
        if (totalWork < UNKNOWN) throw new IllegalArgumentException("totalWork must be >= -1");
        task.set(Objects.requireNonNullElse(taskName, ""));
        total.set(totalWork);
        worked.set(0);
        done.set(false);
    }

    @Override public void worked(long work) {
        if (work < 0) throw new IllegalArgumentException("work must be non-negative");
        worked.getAndAccumulate(work, (left,right) -> {
            long sum = left + right;
            return sum < 0 ? Long.MAX_VALUE : sum;
        });
    }

    @Override public void message(String value) { message.set(Objects.requireNonNullElse(value, "")); }
    @Override public boolean isCancelled() { return cancelled.get(); }
    public void cancel() { cancelled.set(true); }
    @Override public void done() { done.set(true); }

    @Override public ProgressSnapshot snapshot() {
        return new ProgressSnapshot(task.get(), total.get(), worked.get(), message.get(), done.get(), cancelled.get(), java.util.List.of());
    }

    public Snapshot counterSnapshot() {
        return new Snapshot(task.get(), total.get(), worked.get(), message.get(), cancelled.get(), done.get());
    }
}
