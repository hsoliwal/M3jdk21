// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.nullness;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry point: the three relocated donor proofs and the donor plugin test over the relocated plugin. */
class M3NullnessFactsJUnitTest {
    @Test
    void latticeProof() {
        M3NullnessProof.main(new String[0]);
    }

    @Test
    void javacBridgeProof() throws Exception {
        M3JavacBridgeProof.main(new String[0]);
    }

    @Test
    void javacGuardMatrixProof() throws Exception {
        M3JavacGuardMatrixProof.main(new String[0]);
    }

    @Test
    void pluginOnDonorFixture() throws Exception {
        M3NullnessFactsPluginSelfTest.main(new String[0]);
    }
}
