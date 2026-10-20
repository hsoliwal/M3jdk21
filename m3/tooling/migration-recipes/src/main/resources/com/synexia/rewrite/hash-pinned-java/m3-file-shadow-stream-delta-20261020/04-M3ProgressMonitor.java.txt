// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal monitored-work contract for file/stream operations.
 *
 * <p>The interface deliberately uses default methods so low-level APIs can accept one small
 * abstraction while callers subdivide work without importing a UI/job framework. Child monitors
 * scale their completed units into a fixed parent budget and never report that budget twice.</p>
 */
public interface M3ProgressMonitor {
    M3ProgressMonitor NONE = new M3ProgressMonitor() {
        @Override public boolean isCanceled() { return false; }
        @Override public void worked(long units) { }
    };

    boolean isCanceled();

    void worked(long units);

    default void checkCanceled() {
        if (isCanceled()) throw new CancellationException("M3 operation canceled");
    }

    /**
     * Returns a child that maps {@code childUnits} completed units onto {@code parentUnits} of this
     * monitor. Calls are concurrency-safe and progress is monotonic.
     */
    default M3ProgressMonitor child(long parentUnits, long childUnits) {
        if (parentUnits < 0L) throw new IllegalArgumentException("negative parentUnits");
        if (childUnits <= 0L) throw new IllegalArgumentException("childUnits must be positive");
        M3ProgressMonitor parent = this;
        AtomicLong completed = new AtomicLong();
        AtomicLong reported = new AtomicLong();

        return new M3ProgressMonitor() {
            @Override public boolean isCanceled() {
                return parent.isCanceled();
            }

            @Override public void worked(long units) {
                if (units < 0L) throw new IllegalArgumentException("negative worked units");
                if (units == 0L || parentUnits == 0L) {
                    checkCanceled();
                    return;
                }
                checkCanceled();

                long next = completed.updateAndGet(current -> saturatingAdd(current, units, childUnits));
                long scaled = scale(next, parentUnits, childUnits);
                for (;;) {
                    long previous = reported.get();
                    if (scaled <= previous) break;
                    if (reported.compareAndSet(previous, scaled)) {
                        parent.worked(scaled - previous);
                        break;
                    }
                }
                checkCanceled();
            }

            @Override public void done() {
                for (;;) {
                    long previous = reported.get();
                    if (previous >= parentUnits) return;
                    if (reported.compareAndSet(previous, parentUnits)) {
                        parent.worked(parentUnits - previous);
                        return;
                    }
                }
            }
        };
    }

    /** Completes any remaining work allocated to this monitor. Ordinary monitors need no action. */
    default void done() { }

    default M3ProgressMonitor andThen(M3ProgressMonitor other) {
        M3ProgressMonitor left = this;
        M3ProgressMonitor right = Objects.requireNonNull(other, "other");
        return new M3ProgressMonitor() {
            @Override public boolean isCanceled() {
                return left.isCanceled() || right.isCanceled();
            }

            @Override public void worked(long units) {
                left.worked(units);
                right.worked(units);
            }

            @Override public void done() {
                left.done();
                right.done();
            }
        };
    }

    static M3ProgressMonitor none() {
        return NONE;
    }

    private static long saturatingAdd(long current, long amount, long ceiling) {
        if (current >= ceiling || amount >= ceiling - current) return ceiling;
        return current + amount;
    }

    private static long scale(long completed, long parentUnits, long childUnits) {
        if (completed >= childUnits) return parentUnits;
        if (completed == 0L || parentUnits == 0L) return 0L;
        if (completed <= Long.MAX_VALUE / parentUnits) {
            return Math.multiplyExact(completed, parentUnits) / childUnits;
        }
        return BigInteger.valueOf(completed)
                .multiply(BigInteger.valueOf(parentUnits))
                .divide(BigInteger.valueOf(childUnits))
                .longValueExact();
    }
}
