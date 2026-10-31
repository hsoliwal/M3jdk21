// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Product owner public-snapshot contract, separately JNI-qualified by the standalone oracle. */
final class M3LongRankKDiffTest {
    @Test
    void distinctSignedDifferencePairsWithNoOwnerMutation() {
        long[] source = {3, 1, 4, 1, 5};
        M3LongRankIndex rank = M3LongRankIndex.copyOf(source);
        source[0] = Long.MIN_VALUE;
        assertEquals(2L, rank.distinctDifferencePairCount(2));
        assertEquals(1L, rank.distinctDifferencePairCount(0));
        assertEquals(0L, rank.distinctDifferencePairCount(9));
        assertEquals(0L, M3LongRankIndex.copyOf(new long[0])
                .distinctDifferencePairCount(Long.MAX_VALUE));
        assertEquals(1L, M3LongRankIndex.copyOf(new long[] {
                Long.MIN_VALUE, -1, Long.MAX_VALUE})
                .distinctDifferencePairCount(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> rank.distinctDifferencePairCount(-1));
    }

    @Test
    void independentJavaAndNativeKDiffOracle() {
        M3LongRankKDiffContract.contracts(false);
    }
}
