/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/index/TopKSelector.java (blob 05579759973c) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import jdk.m3.collections.M3ProgressMonitor;

/**
 * Read-only selection over already scored, reader-local candidates.
 *
 * <p>Returns input positions ordered by descending finite score, ascending signed
 * document ID, then ascending input position. Duplicate IDs are allowed. Inputs
 * must not be changed concurrently. Implementations must be thread-safe and must
 * neither retain nor mutate the arrays. No string lookup or score recomputation
 * belongs at this boundary.
 */
public interface TopKSelector {
    /**
     * Selects min(limit, input length) positions without changing either input.
     *
     * @throws NullPointerException if an input is null
     * @throws IllegalArgumentException if lengths differ, limit is not positive,
     *         or a score is not finite
     */
    int[] select(int[] documentIds, float[] scores, int limit);

    /**
     * Monitored compatibility overload. Existing third-party implementations receive
     * boundary checks; built-in Java/JNI selectors additionally check within input loops.
     * The monitor is caller-owned and must not be shared by simultaneous selections.
     * No partial result is returned on cancellation. A null monitor is rejected.
     */
    default int[] select(int[] documentIds, float[] scores, int limit, M3ProgressMonitor monitor) {
        return TopKInputs.monitored(monitor, () -> select(documentIds, scores, limit));
    }

    /** Returns the allocation-bounded Java implementation; no native library is required. */
    static TopKSelector javaSelector() {
        return JavaTopKSelector.INSTANCE;
    }
}
