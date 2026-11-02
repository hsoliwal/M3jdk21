/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/** Maven/Surefire binding to the dependency-free source-bound join test. */
class M3JniExportJoinTest {
    @Test
    void sourceHeaderNativeExportJoinAndRefusals() throws Exception {
        assertEquals(28, M3JniExportJoinSelfTest.runAll());
    }
}
