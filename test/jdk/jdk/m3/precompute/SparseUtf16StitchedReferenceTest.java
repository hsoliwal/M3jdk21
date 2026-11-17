// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Reference-model gate of recipe/utf16-stitch-20261106 in the test phase: the seeded corpus of
 * {@link M3SparseUtf16StitchedDump} runs through the relocated Shift-And batch, the sparse UTF-16 plan (single and
 * batched regions) and the stitched scanner, and every first position must equal an independent brute-force UTF-16
 * scan (the dump aborts on the first drift). The oracle report is left to {@link M3SparseUtf16StitchedTest}.
 */
//FLLDCIM: TFG - JUnit wrapper from the module template M3LcsPlanProofTest (recipe utf16-atoms-20261106)
final class SparseUtf16StitchedReferenceTest {
    private static final int CASES = 3_000;

    @Test
    void everyFirstPositionEqualsBruteForceUtf16Scan() throws Exception {
        long[] counts = SparseUtf16StitchedDump.run(getClass().getPackageName(), null, CASES,
                SparseUtf16StitchedDump.discard());
        assertEquals(CASES, counts[0]);
        assertTrue(counts[1] > 0, "reference comparisons");
        assertTrue(counts[2] > 0, "refusals");
    }
}
