/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the same deterministic source-level oracle under the owning Maven/JUnit lifecycle. */
final class M3SignalOrderJUnitTest {
    @Test
    void stableFilteredSignalOrderPreservesExactJavaSemantics() {
        M3SignalOrderContract.main(new String[0]);
    }
}
