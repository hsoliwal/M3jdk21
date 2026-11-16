/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections.sketch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** JUnit entry to the register oracle and the cache transition witness of LongHyperLogLog. */
class HllEstimateCacheTest {
    @Test
    void exactRegisterAndEstimatorParityAcrossMutationAndMerging() {
        assertEquals("assertions=24651,operations=16248,estimates=6457,changed=5748,"
                + "unchangedAdds=7508,sha256="
                + "f1d2615f134a4170ee79c9cbc16d4b7db2dfaa6416536f81e86842026f19fb2b",
                HllEstimateCacheOracle.run());
    }

    @Test
    void cachedEstimateInvalidatesOnlyWhenNecessary() {
        assertEquals(13, HllEstimateCacheStructural.run());
    }
}
