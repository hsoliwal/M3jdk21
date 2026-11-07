// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Reference-model gate of recipe/consumer-profiles-20261107 in the test phase: the seeded corpus of
 * {@link M3IndexPrecomputeConsumerProfilesDump} runs through {@link M3IndexPrecomputeConsumerProfiles},
 * {@link M3IndexPrecomputeCapability} and {@link M3IndexPrecomputeWorkload}; every profile, refusal and blended weight
 * must equal the independent long-arithmetic model of the dump (it aborts on the first drift).
 */
//FLLDCIM: TFG - JUnit wrapper from the module template M3IntRingBufferReferenceTest (m3/arrays); recipe m3/ports/precompute/recipe/consumer-profiles-20261107
final class M3IndexPrecomputeConsumerProfilesReferenceTest {
    private static final int CASES = 1_000;

    @Test
    void everyProfileRefusalAndBlendEqualsReferenceModel() throws Exception {
        long[] counts = M3IndexPrecomputeConsumerProfilesDump.run(M3IndexPrecomputeConsumerProfiles.class,
                M3IndexPrecomputeCapability.class, M3IndexPrecomputeWorkload.class, CASES,
                M3IndexPrecomputeConsumerProfilesDump.discard());
        assertEquals(CASES, counts[0]);
        assertTrue(counts[1] > CASES, "reference comparisons");
        assertTrue(counts[2] >= 8, "refusals");
    }
}
