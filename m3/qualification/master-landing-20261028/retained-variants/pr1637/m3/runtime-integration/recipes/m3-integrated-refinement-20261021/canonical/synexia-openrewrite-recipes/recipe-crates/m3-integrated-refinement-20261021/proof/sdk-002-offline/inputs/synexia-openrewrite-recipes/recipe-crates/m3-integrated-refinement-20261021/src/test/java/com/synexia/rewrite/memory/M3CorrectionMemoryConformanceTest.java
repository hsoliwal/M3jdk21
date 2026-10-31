// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.memory;

import org.junit.jupiter.api.Test;

/** Runs the retained independent byte/schema/storage fixture as a separate JUnit gate. */
final class M3CorrectionMemoryConformanceTest {
    @Test
    void retainedConformanceOracle() throws Exception {
        M3RecoveryMemoryTest.main(new String[0]);
    }
}
