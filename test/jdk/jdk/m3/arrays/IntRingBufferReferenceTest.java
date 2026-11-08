// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Reference-model gate of recipe/int-ring-buffer-20261106 in the test phase: the seeded operation sequences of
 * {@link M3IntRingBufferDump} run through {@link M3IntRingBuffer}, and every result must equal an independent
 * brute-force model (a plain shifted int array bounded by the capacity; the dump aborts on the first drift).
 */
//FLLDCIM: TFG - JUnit wrapper from the module template M3CsrIndexContractTest; recipe m3/arrays/recipe/int-ring-buffer-20261106
final class IntRingBufferReferenceTest {
    private static final int CASES = 400;

    @Test
    void everyResultEqualsBruteForceModel() throws Exception {
        long[] counts = IntRingBufferDump.run(IntRingBuffer.class, CASES, IntRingBufferDump.discard());
        assertEquals(CASES, counts[0]);
        assertTrue(counts[1] > 0, "reference comparisons");
        assertTrue(counts[2] > 0, "refusals");
    }
}
