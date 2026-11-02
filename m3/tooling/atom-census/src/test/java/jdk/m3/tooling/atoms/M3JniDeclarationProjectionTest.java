/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** JUnit binding for actual Java21 compiler and JNI source proof. */
final class M3JniDeclarationProjectionTest {
    @Test
    void strictOneParseJniDeclarationAndJavacHeaderParity() throws Exception {
        assertTrue(M3JniDeclarationProjectionSelfTest.runAll() >= 53);
    }
}
