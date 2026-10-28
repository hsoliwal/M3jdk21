/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the exact first-party projection corpus through the current Maven/JUnit owner. */
final class M3LongProjectionOrderConvergenceTest {
    @Test
    void stableDirectionConversionPreservesPhysicalOccurrences() throws Exception {
        M3ProjectionRefinementProbe.main(new String[] {"java"});
    }
}
