// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Reference-model gate of recipe/hybrid-int-set-20261106 in the test phase: the seeded corpus of
 * {@link M3HybridIntSetDump} runs through {@link M3HybridIntSet}, and every toArray, size, contains probe and
 * union/intersect/xor/minus result must equal java.util.TreeSet (the dump aborts on the first drift).
 */
//FLLDCIM: TFG - JUnit wrapper from the module template M3CsrIndexContractTest; recipe m3/arrays/recipe/hybrid-int-set-20261106
final class M3HybridIntSetReferenceTest {
    private static final int CASES = 400;

    @Test
    void everyResultEqualsTreeSetReference() throws Exception {
        long[] counts = M3HybridIntSetDump.run(M3HybridIntSet.class, CASES, M3HybridIntSetDump.discard());
        assertEquals(CASES, counts[0]);
        assertTrue(counts[1] > 0, "reference comparisons");
        assertTrue(counts[2] > 0, "refusals");
    }
}
