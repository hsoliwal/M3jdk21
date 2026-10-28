// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;

/** Original rank-index API plus native/Java boundary oracle. */
final class M3LongRankEqualRangesTest {
    @Test void nativeOrJavaBatchEqualRange() {
        M3LongRankEqualRangesContract.contracts(false);
    }

    @Test void snapshotPublicEqualRanges() {
        M3LongRankIndex index = M3LongRankIndex.copyOf(new long[] {
                Long.MAX_VALUE, 2L, 2L, Long.MIN_VALUE, 1L, 2L});
        assertArrayEquals(new int[] {1, 1, 2, 5, 0, 1, 5, 6},
                index.equalRanges(new long[] {0L, 2L, Long.MIN_VALUE, Long.MAX_VALUE}));
    }
}
