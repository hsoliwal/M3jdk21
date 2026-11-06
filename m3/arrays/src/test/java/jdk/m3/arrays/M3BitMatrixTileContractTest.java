// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated native-required tile oracle main {@link M3BitMatrixTileContract} (dense and sparse
 * 64 x 64 tile transposes, raw JNI refusals, 8-thread concurrent native transposes against the Java result). It needs
 * the bit matrix kernel inside {@code libm3_arrays} ({@code m3.arrays.native.path}, native-linux profile) and is
 * skipped without a library, as the module's other native-required tests are; with a configured library that does not
 * load, the oracle fails instead of skipping.
 */
//FLLDCIM: TFG - wrapper from the module template (native-required tests skip without m3.arrays.native.path); recipe m3/arrays/recipe/bitmatrix-20261106
final class M3BitMatrixTileContractTest {
    @Test
    void nativeTileTransposeMatchesTheJavaTranspose() throws Exception {
        assumeTrue(M3BitMatrixNativePath.bridge(), "bit matrix JNI kernel not configured (m3.arrays.native.path)");
        M3BitMatrixTileContract.runAll();
    }
}
