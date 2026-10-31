// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.compiler.standard;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry point invoking the real javac and changed-source self-tests. */
class M3JdkStandardPluginJUnitTest {
    @Test
    void verifiesAllFixtures() throws Exception {
        M3JdkStandardPluginSelfTest.main(new String[0]);
        M3JdkStandardCensusSelfTest.main(new String[0]);
        M3ChangedNameGateSelfTest.main(new String[0]);
    }
}
