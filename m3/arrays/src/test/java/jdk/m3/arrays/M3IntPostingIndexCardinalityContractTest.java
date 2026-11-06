// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated count oracle main {@link M3IntPostingIndexCardinalityContract} in its normal
 * mode (exhaustive, random, guards, huge and concurrent countAny/countAll checks; the raw count kernels when a
 * library is loaded), run in the module's test phase.
 */
//FLLDCIM: TFG - wrapper from the module template M3CsrIndexContractTest (M3*Test -> oracle main); recipe m3/arrays/recipe/posting-index-20261106
final class M3IntPostingIndexCardinalityContractTest {
    @Test
    void unionAndIntersectionCountsMatchMaterializedSelections() throws Exception {
        M3IntPostingNativePath.bridge();
        M3IntPostingIndexCardinalityContract.main(new String[0]);
    }
}
