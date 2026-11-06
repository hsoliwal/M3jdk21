// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated oracle main {@link M3HybridIntSetRunPackedMetadataContract}, so the module's test phase runs it
 * (packed RUNS count lane and exact run lane read by reflection, crate m3-mat-hybrid-run-binary-20261030). Direct run: {@code java -cp target/classes;target/test-classes jdk.m3.arrays.M3HybridIntSetRunPackedMetadataContract}.
 */
//FLLDCIM: TFG - wrapper from the module template M3CsrIndexContractTest; recipe m3/arrays/recipe/hybrid-int-set-20261106
final class M3HybridIntSetRunPackedMetadataContractTest {
    @Test
    void packedRunCountLaneAndExactRunLane() throws Exception {
        M3HybridIntSetRunPackedMetadataContract.main(new String[0]);
    }
}
