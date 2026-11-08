/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.compiler.signals;

import org.junit.jupiter.api.Test;

/** Original M3JDK 21 Java21 javac module JUnit receiver for method-level facts. */
final class MethodSignalsJUnitTest {
    @Test
    void attributedMethodSignalsAreDeterministicAndSourceSealed() throws Exception {
        MethodSignalsSelfTest.main(new String[0]);
    }
}
