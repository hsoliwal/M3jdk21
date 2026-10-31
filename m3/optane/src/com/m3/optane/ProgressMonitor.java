// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.optane;

import java.util.concurrent.CancellationException;

/**
 * Dependency-free adapter of Synexia MIndexProgressMonitor.
 * This is an operation boundary, not a new global progress registry.
 * Callers can bridge an existing COP/M3 monitor; null selects NONE.
 */
public interface ProgressMonitor {
    ProgressMonitor NONE = new ProgressMonitor() {};
    default void begin(String task, long totalWork) {}
    default void worked(long work) {}
    default void done() {}
    default boolean isCancelled() { return false; }
    default void checkCancelled() {
        if (isCancelled()) throw new CancellationException("MIndex operation cancelled");
    }
    static ProgressMonitor none() { return NONE; }
}
