/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning JUnit entrypoint over the deterministic original-source oracle. */
final class M3SignalBooleanQueryTest {
    @Test void mixedTypeAndOrAgainstIndependentBruteForce() {
        M3SignalBooleanQuerySelfTest.main(new String[0]);
    }
}
