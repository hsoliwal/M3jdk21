// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * Primitive approximate candidate ranking over precomputed lengths and SimHash values.
 *
 * <p>Lower scores rank closer. Scores never prove equality or an edit-distance threshold.</p>
 */
public interface M3SimilarityBatch {
    record Limits(int maxRows, long maxTransferBytes, long maxWork) {
        public static final Limits DEFAULT =
                new Limits(1_000_000, 64L << 20, 64_000_000L);

        public Limits {
            if (maxRows < 1 || maxTransferBytes < 0 || maxWork < 0) {
                throw new IllegalArgumentException("invalid similarity limits");
            }
        }
    }

    String name();

    boolean accelerated();

    int[] scores(
            int queryUtf16Length,
            long querySimHash,
            int[] candidateUtf16Lengths,
            long[] candidateSimHashes,
            Limits limits,
            M3Progress monitor);
}
