// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;

/** Dependency-light immutable state shared by all Synexia progress monitor adapters. */
public record M3ProgressState(
        String taskName,
        String subTaskName,
        long worked,
        long totalWork,
        boolean canceled,
        boolean done,
        long generation,
        long startedAtMs) {
    public M3ProgressState {
        taskName = Objects.requireNonNullElse(taskName, "");
        subTaskName = Objects.requireNonNullElse(subTaskName, "");
        if (worked < 0L) throw new IllegalArgumentException("worked must be non-negative");
        if (totalWork < M3ProgressMonitor.UNKNOWN) {
            throw new IllegalArgumentException("invalid totalWork");
        }
        if (generation < 0L || startedAtMs < 0L) {
            throw new IllegalArgumentException("negative generation or start time");
        }
    }
}
