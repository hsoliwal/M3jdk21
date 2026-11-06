// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 wrapper of the relocated oracle main {@link M3IntRingBufferContract}, so the module's test phase runs it
 * (26 capacities, 10,000 seeded operations each against java.util.ArrayDeque, crate m3-mat-ring-20261030). Direct run:
 * {@code java -cp target/classes;target/test-classes jdk.m3.arrays.M3IntRingBufferContract}.
 */
//FLLDCIM: TFG - wrapper from the module template M3CsrIndexContractTest; recipe m3/arrays/recipe/int-ring-buffer-20261106
final class M3IntRingBufferContractTest {
    @Test
    void bothEndRingOperationsMatchArrayDequeAtEveryCapacity() throws Exception {
        M3IntRingBufferContract.main(new String[0]);
    }
}
