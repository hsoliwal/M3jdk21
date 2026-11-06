// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated semantics oracle main {@link M3IntPostingIndexConvergenceContract}
 * (equalSelection and the selection algebra against any/all over zero, sparse and 16,384-row dense fixtures and
 * 1,024 seeded rounds), run in the module's test phase.
 */
//FLLDCIM: TFG - wrapper from the module template M3CsrIndexContractTest (M3*Test -> oracle main); recipe m3/arrays/recipe/posting-index-20261106
final class M3IntPostingIndexConvergenceContractTest {
    @Test
    void anyAndAllAgreeWithTheSelectionAlgebraOverEqualSelections() {
        M3IntPostingNativePath.bridge();
        M3IntPostingIndexConvergenceContract.runAll();
    }
}
