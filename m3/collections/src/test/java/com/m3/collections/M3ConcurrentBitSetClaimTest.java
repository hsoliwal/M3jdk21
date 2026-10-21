// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry point for the standalone Java 21 differential/concurrency oracle. */
class M3ConcurrentBitSetClaimTest {
    @Test
    void atomicClaimAndTailWordContracts() throws Exception {
        M3ConcurrentBitSetClaimSelfTest.runAll();
    }
}
