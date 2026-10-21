// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;
import java.util.concurrent.CancellationException;

/** A byte-work progress contract shared by M3JDK21 input and output streams. */
@FunctionalInterface
public interface M3StreamProgress {
    /** Nonnegative units completed by this caller, including deliberate replay work. */
    void worked(long units);

    /** Parent cancellation propagates through a subprogress monitor. */
    default boolean cancelled() { return false; }

    default void checkCancelled() {
        if (cancelled()) throw new CancellationException("M3 stream cancelled");
    }

    /**
     * Allocate at most {@code budget} units of progress to one nested operation.
     * The child never over-reports to its parent, even if a parser retries.
     */
    default M3StreamProgress subProgress(long budget) {
        if (budget < 0) throw new IllegalArgumentException("negative progress budget");
        M3StreamProgress parent = this;
        return new M3StreamProgress() {
            private long remaining = budget;
            @Override public synchronized void worked(long units) {
                if (units < 0) throw new IllegalArgumentException("negative progress");
                checkCancelled();
                long granted = Math.min(units, remaining);
                if (granted != 0) {
                    parent.worked(granted);
                    remaining -= granted;
                }
            }
            @Override public boolean cancelled() { return parent.cancelled(); }
        };
    }

    static M3StreamProgress none() { return units -> { }; }

    static M3StreamProgress require(M3StreamProgress monitor) {
        return Objects.requireNonNull(monitor, "progress monitor");
    }
}
