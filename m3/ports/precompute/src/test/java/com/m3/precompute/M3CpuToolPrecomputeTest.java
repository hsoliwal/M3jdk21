// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entrypoint for the same native Java21 byte oracle. */
final class M3CpuToolPrecomputeTest {
    @Test void preparedDagImageAndOperatorsPreserveAllFrozenBehavior() {
        M3CpuToolPrecomputeSelfTest.runAll();
    }
}
