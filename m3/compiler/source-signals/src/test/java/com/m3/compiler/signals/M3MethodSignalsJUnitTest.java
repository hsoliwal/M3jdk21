/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.compiler.signals;

import org.junit.jupiter.api.Test;

/** Original M3JDK21 Java21 javac module JUnit receiver for method-level facts. */
final class M3MethodSignalsJUnitTest {
    @Test
    void attributedMethodSignalsAreDeterministicAndSourceSealed() throws Exception {
        M3MethodSignalsSelfTest.main(new String[0]);
    }
}
