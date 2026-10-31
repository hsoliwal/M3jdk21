// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

/** Original M3JDK21 LC315 Java/JNI primitive index-merging contracts. */
final class M3LongCountSmallerRightTest {
    @Test
    void countSmallerRightIsStableAndDoesNotCopyOwnerPayload() {
        M3LongCountSmallerRightContract.main(new String[0]);
        assertArrayEquals(new int[] {2, 1, 1, 0},
                M3LongRankIndex.countSmallerAfterSelf(new long[] {5, 2, 6, 1}));
    }
}
