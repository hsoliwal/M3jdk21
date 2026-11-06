// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of M3CooccurrencePpmiSparseOracle (one JVM, one main call per mode): 1,500 seeded corpora dense-count/sparse parity and digest; sparse degree-one allocation witness; the oracle prints its figures. */
class M3CooccurrencePpmiSparseOracleTest {
    @Test void seededSparseOracle() throws Throwable {
        M3CooccurrencePpmiSparseOracle.main(new String[0]);
    }
    @Test void sparseAllocationWitness() throws Throwable {
        M3CooccurrencePpmiSparseOracle.main(new String[]{"--allocation"});
    }
}
