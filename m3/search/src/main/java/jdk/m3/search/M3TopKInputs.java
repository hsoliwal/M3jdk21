/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/index/TopKInputs.java (blob dc60990a93ca) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import jdk.m3.collections.M3ProgressMonitor;
import java.util.Objects;
import java.util.function.Supplier;

/** Shared validation for Java and native selection boundaries. */
final class M3TopKInputs {
    private M3TopKInputs() {
    }

    static int validate(int[] documentIds, float[] scores, int limit) {
        return validate(documentIds, scores, limit, null);
    }

    static int validate(int[] documentIds, float[] scores, int limit, M3ProgressMonitor monitor) {
        if (monitor != null) monitor.subTask("Validate top-k inputs");
        Objects.requireNonNull(documentIds, "documentIds");
        Objects.requireNonNull(scores, "scores");
        if (documentIds.length != scores.length) {
            throw new IllegalArgumentException("documentIds and scores must have equal length");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be >= 1");
        }
        for (int i = 0; i < scores.length; i++) {
            checkpoint(monitor, i);
            if (!Float.isFinite(scores[i])) {
                throw new IllegalArgumentException("scores must be finite");
            }
        }
        if (monitor != null) monitor.checkCanceled();
        return Math.min(limit, scores.length);
    }

    static void checkpoint(M3ProgressMonitor monitor, int position) {
        if (monitor != null && (position & 4095) == 0) monitor.checkCanceled();
    }

    /** One terminal lifecycle; cleanup failures cannot hide the primary exception. */
    static int[] monitored(M3ProgressMonitor monitor, Supplier<int[]> action) {
        Objects.requireNonNull(monitor, "monitor");
        monitor.checkCanceled();
        monitor.beginTask("M3 top-k selection", 1L);
        Throwable failure = null;
        try {
            monitor.checkCanceled();
            int[] result = action.get();
            monitor.checkCanceled();
            monitor.worked(1L);
            monitor.checkCanceled();
            return result;
        } catch (RuntimeException | Error error) {
            failure = error;
            throw error;
        } finally {
            try {
                monitor.done();
            } catch (RuntimeException | Error cleanup) {
                if (failure == null) throw cleanup;
                if (failure != cleanup) failure.addSuppressed(cleanup);
            }
        }
    }
}
