/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Ordinary Maven/JUnit discovery; native-required execution is separately explicit. */
class M3ViewportReceivingTest {
    @Test void residentWindowBounds() { M3ViewportPlanProof.main(new String[0]); }
    @Test void rowGeometryAndProjection() { M3ViewportTreeProof.main(new String[0]); }
}
