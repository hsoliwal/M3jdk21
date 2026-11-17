// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SimilarityBatchTest {
    @Test
    void canonicalBatchRanksPrecomputedCandidatesDeterministically() {
        List<String> values = List.of("alpha beta", "alpha betb", "zzzzzzzz", "alpha beta gamma");
        TextSignals query = TextSignals.compile(values.getFirst());
        int[] lengths = new int[values.size()];
        long[] hashes = new long[values.size()];
        for (int index = 0; index < values.size(); index++) {
            TextSignals signals = TextSignals.compile(values.get(index));
            lengths[index] = signals.metrics().utf16Length();
            hashes[index] = signals.simHash64();
        }

        int[] scores =
                SimilarityBatchJava.INSTANCE.scores(
                        query.metrics().utf16Length(),
                        query.simHash64(),
                        lengths,
                        hashes,
                        SimilarityBatch.Limits.DEFAULT,
                        null);

        assertEquals(0, scores[0]);
        assertTrue(scores[1] < scores[2]);
        assertEquals(
                SimilarityBatchJava.score(
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
                        SimilarityBatchJava.INSTANCE.scores(
                                1,
                                2L,
                                new int[] {1},
                                new long[0],
                                SimilarityBatch.Limits.DEFAULT,
                                null));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SimilarityBatchJava.INSTANCE.scores(
                                1,
                                2L,
                                new int[] {-1},
                                new long[] {3L},
                                SimilarityBatch.Limits.DEFAULT,
                                null));
    }
}
