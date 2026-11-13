/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Maven/JUnit binding for the portable reachability-kernel suite. */
class M3ReachabilityKernelTest {
    @Test
    void portableKernel() {
        assertTrue(M3ReachabilityKernelSelfTest.runAll(null) >= 5);
    }
}
