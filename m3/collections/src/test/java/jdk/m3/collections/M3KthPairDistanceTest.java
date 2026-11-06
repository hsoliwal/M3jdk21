// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor blob df65643e3e90 at d5ad5090d87e; recipe m3/collections/qualification/census2-c4-20261106/relocate.py v1
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
