// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.compiler.standard;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry point invoking the real javac fixture self-test (not mocks). */
class M3JdkStandardPluginJUnitTest {
    @Test
    void verifiesFixtures() throws Exception {
        M3JdkStandardPluginSelfTest.main(new String[0]);
    }
}
