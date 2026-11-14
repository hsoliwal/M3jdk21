// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated CSR oracle main {@link M3CsrIndexContract}, so the module's test phase runs its
 * 7,329,532 assertions (direct owner-array construction against the staged Builder and an independent row oracle).
 * Direct run: {@code java -cp target/classes;target/test-classes jdk.m3.arrays.CsrIndexContract}.
 */
//FLLDCIM: TFG - wrapper from the module template M3SparseLongIntArrayTest -> M3SparseLongIntArraySelfTest.run(); recipe m3/arrays/recipe/csr-index-20261106
final class CsrIndexContractTest {
    @Test
    void directOwnerArrayConstructionMatchesStagedBuilderAndRowOracle() throws Exception {
        CsrIndexContract.main(new String[0]);
    }
}
