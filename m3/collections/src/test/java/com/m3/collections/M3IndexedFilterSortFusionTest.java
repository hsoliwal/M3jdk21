/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit receiver for the repeatable, seeded Java21 differential oracle. */
final class M3IndexedFilterSortFusionTest {
    @Test void fusedMaskAndIndexOrder() {
        M3IndexedFilterSortFusionSelfTest.main(new String[0]);
    }
}
