/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Execute the same deterministic oracle against the original owning JDK source. */
final class SignalDependencyEpochsTest {
    @Test void seededDependencyOracle() throws Exception {
        SignalDependencyEpochsSelfTest.main(new String[0]);
    }
}
