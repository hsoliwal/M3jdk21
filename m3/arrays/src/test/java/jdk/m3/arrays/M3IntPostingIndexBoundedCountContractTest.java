// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated bounded count oracle main {@link M3IntPostingIndexBoundedCountContract} in its
 * normal mode (exhaustive, random, guards and concurrent countAnyUpTo/countAllUpTo checks; raw kernel validation
 * when a library is loaded), run in the module's test phase.
 */
//FLLDCIM: TFG - wrapper from the module template M3CsrIndexContractTest (M3*Test -> oracle main); recipe m3/arrays/recipe/posting-index-20261106
final class M3IntPostingIndexBoundedCountContractTest {
    @Test
    void limitBoundedCountsStopAtTheDistinctMatchLimit() throws Exception {
        M3IntPostingNativePath.bridge();
        M3IntPostingIndexBoundedCountContract.main(new String[0]);
    }
}
