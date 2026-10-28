// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/**
 * Sub-progress monitor for M3File streams and walks (shape of Eclipse {@code SubMonitor}; no Eclipse
 * code or dependency).
 *
 * <p>One abstract method, {@link #worked(long)}; everything else is a default. Streams report bytes as
 * units, call {@link #begin} with the expected total when known, and check {@link #isCanceled()} before
 * every transfer ({@link #checkCanceled()} throws {@link InterruptedIOException}). {@link #split(long)}
 * hands a child a share of this monitor's units; the child scales its own total onto that share.
 * Monitors are not thread-safe.</p>
 */
@FunctionalInterface
public interface M3FileProgress {
    /** Ignores all work and is never canceled. */
    M3FileProgress NONE = units -> { };

    /** Reports {@code units} of completed work. */
    void worked(long units);

    default boolean isCanceled() {
        return false;
    }

    /** Announces a task and its total units ({@code 0} when unknown). */
    default void begin(String task, long totalUnits) {
    }

    /** The task is complete; a split child reports its remaining share. */
    default void done() {
    }

    default void checkCanceled() throws InterruptedIOException {
        if (isCanceled()) throw new InterruptedIOException("canceled");
    }

    /** Child monitor owning {@code parentUnits} of this monitor's units. */
    default M3FileProgress split(long parentUnits) {
        if (parentUnits < 0) throw new IllegalArgumentException("negative units: " + parentUnits);
        return new M3FileSubProgress(this, parentUnits);
    }

    /** Monitor from a work sink and a cancellation probe. */
    static M3FileProgress of(LongConsumer worked, BooleanSupplier canceled) {
        Objects.requireNonNull(worked, "worked");
        Objects.requireNonNull(canceled, "canceled");
        return new M3FileProgress() {
            @Override
            public void worked(long units) {
                worked.accept(units);
            }

            @Override
            public boolean isCanceled() {
                return canceled.getAsBoolean();
            }
        };
    }
}
