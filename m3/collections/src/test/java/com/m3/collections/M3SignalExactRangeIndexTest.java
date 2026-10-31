/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Original owning JUnit executes the same deterministic Java21 differential. */
final class M3SignalExactRangeIndexTest {
    @Test void exactIndexedRangesAndFreshness() {
        M3SignalExactRangeIndexSelfTest.main(new String[0]);
    }
}
