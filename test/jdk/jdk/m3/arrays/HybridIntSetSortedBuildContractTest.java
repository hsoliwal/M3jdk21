// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated oracle main {@link M3HybridIntSetSortedBuildContract}, so the module's test phase runs it
 * (refusals, fixed ARRAY/RUNS/BITMAP lanes and seeded pairs against TreeSet, crate m3-mat-hybrid-sorted-unique-20261030). Direct run: {@code java -cp target/classes;target/test-classes jdk.m3.arrays.HybridIntSetSortedBuildContract}.
 */
//FLLDCIM: TFG - wrapper from the module template M3CsrIndexContractTest; recipe m3/arrays/recipe/hybrid-int-set-20261106
final class HybridIntSetSortedBuildContractTest {
    @Test
    void sortedUniqueBuilderMatchesTreeSetForContentAndAlgebra() throws Exception {
        HybridIntSetSortedBuildContract.main(new String[0]);
    }
}
