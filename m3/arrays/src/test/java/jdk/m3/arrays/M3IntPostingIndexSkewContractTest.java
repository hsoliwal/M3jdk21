// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated skew suite main {@link M3IntPostingIndexSkewContract} (1,034 scenarios: the
 * 65,536-row dense/sparse/odd/even fixtures with 16:1 skew and 1,024 seeded rounds), run in the module's test phase.
 */
//FLLDCIM: TFG - wrapper from the module template M3CsrIndexContractTest (M3*Test -> oracle main); recipe m3/arrays/recipe/posting-index-20261106
final class M3IntPostingIndexSkewContractTest {
    @Test
    void skewedPostingsIntersectThroughMergeAndLowerBoundLanes() {
        M3IntPostingNativePath.bridge();
        M3IntPostingIndexSkewContract.runAll();
    }
}
