/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit bridge for hash-set growth and primitive membership invariants. */
public final class IntHashSetGrowthTest {
    @Test
    void growthAdmissionAndStableProbeMembership() {
        IntHashSetGrowthSelfTest.main(new String[0]);
    }
}
