/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Product-owning executable Java21 JUnit endpoint, no test-framework replacement. */
final class SignalExecutionPressureTest {
    @Test void cancellationAndProgressPreserveExactQueryContracts() {
        SignalExecutionPressureSelfTest.main(new String[0]);
    }
}
