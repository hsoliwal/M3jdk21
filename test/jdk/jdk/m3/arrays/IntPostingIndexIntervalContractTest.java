// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated native-required interval oracle main {@link M3IntPostingIndexIntervalContract}
 * (deterministic, skewed and randomized intersections through the JNI kernel). It needs the posting kernel inside
 * {@code libm3_arrays} ({@code m3.arrays.native.path}, native-linux profile) and is skipped without a library, as
 * the module's other native-required tests are.
 */
//FLLDCIM: TFG - wrapper from the module template (native-required tests skip without m3.arrays.native.path); recipe m3/arrays/recipe/posting-index-20261106
final class IntPostingIndexIntervalContractTest {
    @Test
    void nativeIntervalProofMatchesTheJavaIntersection() throws Exception {
        assumeTrue(IntPostingNativePath.bridge(), "posting JNI kernel not configured (m3.arrays.native.path)");
        IntPostingIndexIntervalContract.runAll();
    }
}
