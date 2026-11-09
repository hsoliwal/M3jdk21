// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated galloping oracle main {@link M3IntPostingIndexContract} (38,360 scenarios:
 * lower-bound windows, 1,024 public rounds against BitSet oracles, 16 concurrent immutable queries, and the native
 * cases when a library is loaded), so the module's test phase runs it and prints MAT_GALLOP_DIGEST.
 * Direct run: {@code java -cp target/classes;target/test-classes jdk.m3.arrays.IntPostingIndexContract}.
 */
//FLLDCIM: TFG - wrapper from the module template M3CsrIndexContractTest (M3*Test -> oracle main); recipe m3/arrays/recipe/posting-index-20261106
final class IntPostingIndexContractTest {
    @Test
    void gallopingLowerBoundAndPublicQueriesMatchIndependentOracles() throws Exception {
        IntPostingNativePath.bridge();
        IntPostingIndexContract.runAll();
    }
}
