// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entry point: the three relocated donor proofs and the donor plugin test over the relocated plugin. */
class NullnessFactsJUnitTest {
    @Test
    void latticeProof() {
        NullnessProof.main(new String[0]);
    }

    @Test
    void javacBridgeProof() throws Exception {
        JavacBridgeProof.main(new String[0]);
    }

    @Test
    void javacGuardMatrixProof() throws Exception {
        JavacGuardMatrixProof.main(new String[0]);
    }

    @Test
    void pluginOnDonorFixture() throws Exception {
        NullnessFactsPluginSelfTest.main(new String[0]);
    }
}
