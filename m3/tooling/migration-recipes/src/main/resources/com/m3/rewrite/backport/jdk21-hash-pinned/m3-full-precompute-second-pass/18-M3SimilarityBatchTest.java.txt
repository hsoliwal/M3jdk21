// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class M3SimilarityBatchTest {
    @Test
    void canonicalBatchRanksPrecomputedCandidatesDeterministically() {
        List<String> values = List.of("alpha beta", "alpha betb", "zzzzzzzz", "alpha beta gamma");
        M3TextSignals query = M3TextSignals.compile(values.getFirst());
        int[] lengths = new int[values.size()];
        long[] hashes = new long[values.size()];
        for (int index = 0; index < values.size(); index++) {
            M3TextSignals signals = M3TextSignals.compile(values.get(index));
            lengths[index] = signals.metrics().utf16Length();
            hashes[index] = signals.simHash64();
        }

        int[] scores =
                M3SimilarityBatchJava.INSTANCE.scores(
                        query.metrics().utf16Length(),
                        query.simHash64(),
                        lengths,
                        hashes,
                        M3SimilarityBatch.Limits.DEFAULT,
                        null);

        assertEquals(0, scores[0]);
        assertTrue(scores[1] < scores[2]);
        assertEquals(
                M3SimilarityBatchJava.score(
                        query.metrics().utf16Length(),
                        query.simHash64(),
                        lengths[3],
                        hashes[3]),
                scores[3]);
    }

    @Test
    void invalidGeometryAndNegativeLengthsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3SimilarityBatchJava.INSTANCE.scores(
                                1,
                                2L,
                                new int[] {1},
                                new long[0],
                                M3SimilarityBatch.Limits.DEFAULT,
                                null));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3SimilarityBatchJava.INSTANCE.scores(
                                1,
                                2L,
                                new int[] {-1},
                                new long[] {3L},
                                M3SimilarityBatch.Limits.DEFAULT,
                                null));
    }
}
