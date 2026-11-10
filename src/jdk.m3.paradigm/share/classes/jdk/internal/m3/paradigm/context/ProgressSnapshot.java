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
/*
 * Decompiled with CFR 0.152.
 */
package jdk.internal.m3.paradigm.context;

import java.util.List;
import java.util.Objects;

/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextProgressSnapshot.java}
 * blob {@code 2cf63ce19744b273edd7d1fe1578ee29637c44dc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public class ProgressSnapshot {
    private final String task;
    private final long totalWork;
    private final long worked;
    private final String message;
    private final boolean done;
    private final boolean cancelled;
    private final List<ProgressSnapshot> children;

    public ProgressSnapshot(String task, long totalWork, long worked, String message, boolean done, boolean cancelled, List<ProgressSnapshot> children) {
        this.task = Objects.requireNonNullElse(task, "");
        this.totalWork = totalWork;
        this.worked = worked;
        this.message = Objects.requireNonNullElse(message, "");
        this.done = done;
        this.cancelled = cancelled;
        this.children = List.copyOf(Objects.requireNonNull(children, "children"));
        if (totalWork < -1L) {
            throw new IllegalArgumentException("totalWork");
        }
        if (worked < 0L) {
            throw new IllegalArgumentException("worked");
        }
    }

    public String task() {
        return this.task;
    }

    public long totalWork() {
        return this.totalWork;
    }

    public long worked() {
        return this.worked;
    }

    public String message() {
        return this.message;
    }

    public boolean done() {
        return this.done;
    }

    public boolean cancelled() {
        return this.cancelled;
    }

    public List<ProgressSnapshot> children() {
        return this.children;
    }

    public double fraction() {
        if (this.totalWork <= 0L) {
            return this.done ? 1.0 : 0.0;
        }
        return Math.min(1.0, (double)this.worked / (double)this.totalWork);
    }
}
