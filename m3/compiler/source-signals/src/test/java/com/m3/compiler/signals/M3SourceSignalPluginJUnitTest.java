// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.compiler.signals;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry point invoking real JavaCompiler task fixtures (not mocks). */
class M3SourceSignalPluginJUnitTest {
    @Test
    void sourceTimeSignalsCompileAndRetainDeterministicMetadata() throws Exception {
        M3SourceSignalPluginSelfTest.main(new String[0]);
    }
}
