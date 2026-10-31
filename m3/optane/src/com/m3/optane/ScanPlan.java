// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.optane;

import java.io.IOException;
import java.util.Objects;

/**
 * Constant-space, long-addressed batching over an EXISTING payload owner.
 * Does not map, allocate, copy, intern, cache or publish corpus bytes.
 * Calls are serial in logical address order; the caller owns cross-file scheduling.
 */
public record ScanPlan(int windowBytes, int workers, int simultaneousCopies,
                       long softTemporaryBudgetBytes) {
    public static final int MAX_WINDOW_BYTES = 64 * 1024 * 1024;

    public ScanPlan {
        if (windowBytes <= 0 || windowBytes > MAX_WINDOW_BYTES || workers <= 0
                || simultaneousCopies <= 0 || softTemporaryBudgetBytes <= 0
                || windowBytes > softTemporaryBudgetBytes / workers / simultaneousCopies)
            throw new IllegalArgumentException("window exceeds soft worker/copy budget");
    }

    /** Upper bound on declared window payloads, not total process/JVM memory. */
    public long declaredTemporaryBytes() {
        return (long) windowBytes * workers * simultaneousCopies;
    }

    public long windowsFor(long length) {
        if (length < 0) throw new IllegalArgumentException("negative length");
        return length / windowBytes + (length % windowBytes == 0 ? 0 : 1);
    }

    @FunctionalInterface
    public interface RangeConsumer {
        /** Borrow through the existing owner; retain no temporary shadow after this call. */
        void accept(long offset, int length) throws IOException;
    }

    /**
     * Emits every byte range exactly once, including the final short window.
     * Cancellation is checked between callbacks. Exceptions propagate unchanged;
     * progress cleanup exceptions are suppressed onto an existing failure.
     * Consumer callbacks may have side effects: this is not a transaction/replay engine.
     */
    public long forEach(long offset, long length, RangeConsumer consumer,
                        ProgressMonitor progress) throws IOException {
        Objects.requireNonNull(consumer, "consumer");
        if (offset < 0 || length < 0 || length > Long.MAX_VALUE - offset)
            throw new IllegalArgumentException("negative or overflowing range");
        ProgressMonitor monitor = progress == null ? ProgressMonitor.NONE : progress;
        monitor.checkCancelled();
        monitor.begin("M3 Optane bounded scan", length);
        Throwable failure = null;
        try {
            long completed = 0;
            while (completed < length) {
                monitor.checkCancelled();
                int count = (int) Math.min((long) windowBytes, length - completed);
                consumer.accept(offset + completed, count);
                completed += count;
                monitor.worked(count);
            }
            return completed;
        } catch (IOException | RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            try { monitor.done(); }
            catch (RuntimeException | Error cleanup) {
                if (failure == null) throw cleanup;
                if (cleanup != failure) failure.addSuppressed(cleanup);
            }
        }
    }
}
