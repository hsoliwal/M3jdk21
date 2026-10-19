/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit adapter for the source-sealed 64-bit index-path oracle. */
final class M3IndexPathMasksLongTest {
    @Test void longRowIdsPreserveSignedValuesAndMasks() {
        M3IndexPathMasksLongSelfTest.main(new String[0]);
    }
}
