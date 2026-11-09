// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated oracle main {@link M3HybridIntSetRunSearchContract}, so the module's test phase runs it
 * (RUNS membership by binary search, multi-chunk offsets and seeded run encodings against TreeSet, crate m3-mat-hybrid-run-binary-20261030). Direct run: {@code java -cp target/classes;target/test-classes jdk.m3.arrays.HybridIntSetRunSearchContract}.
 */
//FLLDCIM: TFG - wrapper from the module template M3CsrIndexContractTest; recipe m3/arrays/recipe/hybrid-int-set-20261106
final class HybridIntSetRunSearchContractTest {
    @Test
    void runMembershipBinarySearchMatchesTreeSet() throws Exception {
        HybridIntSetRunSearchContract.main(new String[0]);
    }
}
