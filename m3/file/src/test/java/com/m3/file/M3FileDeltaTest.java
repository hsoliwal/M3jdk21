// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry point to the real-file and real-javac regression oracle. */
final class M3FileDeltaTest {
    @Test
    void fullSourceClassAndCliContract() throws Exception {
        M3FileDeltaSelfTest.main(new String[0]);
        M3FileDeltaContractSelfTest.main(new String[0]);
    }
}
