// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.precompute;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entrypoint for the same native Java21 byte oracle. */
final class CpuToolPrecomputeTest {
    @Test void preparedDagImageAndOperatorsPreserveAllFrozenBehavior() {
        CpuToolPrecomputeSelfTest.runAll();
    }
}
