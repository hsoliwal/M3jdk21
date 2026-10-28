// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3KthPairDistanceTest {
    @Test void javaAndOptionalJniKthPairDistance() {
        M3KthPairDistanceContract.main(new String[0]);
    }

    @Test void originalImmutableIndexPublicRoute() {
        M3LongRankIndex index = M3LongRankIndex.copyOf(new long[] {1, 3, 1});
        if (index.kthPairDistance(1) != 0 || index.kthPairDistance(3) != 2) {
            throw new AssertionError("public immutable index pair distance");
        }
        long[] queries = {1, 2, 3};
        long[] actual = index.kthPairDistances(queries);
        if (!java.util.Arrays.equals(actual, new long[] {0, 2, 2})) {
            throw new AssertionError("index vector pair distance");
        }
    }
}
