// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Runs the independent sorted-lane linear oracle against the actual M3LaneSearch owner. */
final class M3LaneSearchCategoryProofTest {
    @Test
    void allFourBoundsMatchTheLinearOracle() {
        assertEquals(80_640L, M3LaneSearchCategoryProof.run());
    }
}
