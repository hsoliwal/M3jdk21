// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Reference-model gate of recipe/planner-family-20261107 in the test phase: the seeded corpus of
 * {@link M3IndexPrecomputePlannerDump} runs through {@link M3IndexPrecomputePlanner}, {@link M3FrozenLongTupleIndex},
 * {@link M3NativePrecomputeOps} (Java path), {@link M3JavaIndexPrecomputeAccelerator} and {@link M3PrecomputeHashes};
 * every recommendation, refusal, lookup and hash must equal the brute-force / HashMap / MessageDigest model of the
 * dump (it aborts on the first drift).
 */
//FLLDCIM: TFG - JUnit wrapper from the module template M3IndexPrecomputeConsumerProfilesReferenceTest; recipe m3/ports/precompute/recipe/planner-family-20261107
final class M3IndexPrecomputePlannerReferenceTest {
    private static final int CASES = 600;
    private static final String TUPLE_DOMAIN = "M3_FROZEN_LONG_TUPLE_V1:";

    @Test
    void everyRecommendationLookupAndHashEqualsReferenceModel() throws Exception {
        long[] counts = M3IndexPrecomputePlannerDump.run(M3IndexPrecomputePlannerDump.class.getPackageName(), "M3",
                TUPLE_DOMAIN, CASES, M3IndexPrecomputePlannerDump.discard());
        assertEquals(CASES, counts[0]);
        assertTrue(counts[1] > CASES, "reference comparisons");
        assertTrue(counts[2] > 0, "refusals");
    }
}
