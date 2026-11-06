// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated bit matrix oracle main {@link M3BitMatrixContract}: dimensional boundary cases,
 * 10 x 10 boundary shapes x 3 densities and 2,048 seeded shapes up to 128 x 128 against an independent
 * {@code boolean[][]} oracle (Java path; the JNI parity lane joins when {@link M3BitMatrixNativePath#bridge()} finds
 * the module library). The figures ({@code MAT_BIT_MATRIX_SCENARIOS}, {@code MAT_BIT_MATRIX_ASSERTIONS},
 * {@code MAT_BIT_MATRIX_BACKEND}, {@code MAT_BIT_MATRIX_RESULT}) are printed in the surefire output. Direct run:
 * {@code java -cp target/classes;target/test-classes jdk.m3.arrays.M3BitMatrixContract}.
 */
//FLLDCIM: TFG - wrapper from the module template M3CsrIndexContractTest (main of the relocated oracle); recipe m3/arrays/recipe/bitmatrix-20261106
final class M3BitMatrixContractTest {
    @Test
    void transpositionAndBoundariesMatchTheBooleanOracle() {
        M3BitMatrixNativePath.bridge();
        M3BitMatrixContract.main(new String[0]);
    }
}
