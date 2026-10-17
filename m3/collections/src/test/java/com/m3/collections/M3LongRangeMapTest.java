/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** The finite primitive interval-map differential oracle as an owning-module JUnit gate. */
final class M3LongRangeMapTest {
    @Test void sparseSortedIntervalsAndMasks() {
        M3LongRangeMapSelfTest.main(new String[0]);
    }
}
