/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning M3 JDK21 module JUnit regression gate, no provider adapters. */
final class M3ObjectRangeMapTest {
    @Test void referenceRangesAndFullWidthMasks() {
        M3ObjectRangeMapSelfTest.main(new String[0]);
    }
}
