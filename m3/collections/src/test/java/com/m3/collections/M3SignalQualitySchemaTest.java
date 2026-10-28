/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Actual owning JUnit adapter to the deterministic source oracle. */
final class M3SignalQualitySchemaTest {
    @Test void sourceOwnedQualityContract() {
        M3SignalQualitySchemaSelfTest.main(new String[0]);
    }
}
