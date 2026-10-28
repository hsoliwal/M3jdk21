// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import org.junit.jupiter.api.Test;

/** The owning JUnit entry point for the standalone Java21 oracle. */
final class M3FileShadowBoundaryTest {
    @Test
    void boundedReadsAndExactProgress() throws Exception {
        M3FileShadowBoundaryContract.run();
    }
}
