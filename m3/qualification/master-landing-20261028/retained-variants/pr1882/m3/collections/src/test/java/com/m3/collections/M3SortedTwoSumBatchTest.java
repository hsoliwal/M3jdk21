// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/** JUnit owner for LeetCode #167 Java / optional existing JNI ABI. */
final class M3SortedTwoSumBatchTest {
    @Test void exactJavaNativeAndOverflowOracle() {
        M3SortedTwoSumBatchContract.contracts();
    }

    @Test void originalImmutableIndexReceivesFlatPhysicalPairs() {
        M3LongRankIndex index = M3LongRankIndex.copyOf(new long[]{7, -5, 0, 7, 9});
        assertArrayEquals(new int[]{0, 4, 1, 3, -1, -1},
                index.pairSumIndices(new long[]{4, 7, 100}));
        int[] output = new int[9];
        java.util.Arrays.fill(output, 777);
        index.pairSumIndicesInto(new long[]{4, 7, 100}, 0, 3, output, 2);
        assertArrayEquals(new int[]{777, 777, 0, 4, 1, 3, -1, -1, 777}, output);
        assertEquals(5, index.size());
        assertArrayEquals(new long[]{-5, 0, 7, 7, 9}, index.toArray());
    }
}
