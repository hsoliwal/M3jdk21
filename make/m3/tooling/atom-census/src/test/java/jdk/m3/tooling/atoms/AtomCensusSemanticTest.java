/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** Maven/JUnit binding for the deterministic dependency-free Java21 proof. */
class AtomCensusSemanticTest {
    @Test
    void singleParseSemanticAtomsAndLegacyProof() throws Exception {
        assertTrue(AtomCensusSemanticSelfTest.runAll() >= 151);
    }
}
