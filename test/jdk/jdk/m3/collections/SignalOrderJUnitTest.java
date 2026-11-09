/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the same deterministic source-level oracle under the owning Maven/JUnit lifecycle. */
final class SignalOrderJUnitTest {
    @Test
    void stableFilteredSignalOrderPreservesExactJavaSemantics() {
        SignalOrderContract.main(new String[0]);
    }
}
