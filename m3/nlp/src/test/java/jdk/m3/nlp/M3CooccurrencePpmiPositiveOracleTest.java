// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of M3CooccurrencePpmiPositiveOracle (one JVM, one main call per mode): low-positive-density fixture and heap growth boundaries; the default mode adds the public allocation envelope; the oracle prints its figures. */
class M3CooccurrencePpmiPositiveOracleTest {
    @Test void positiveBehavior() throws Throwable {
        M3CooccurrencePpmiPositiveOracle.main(new String[]{"--behavior"});
    }
    @Test void positiveAllocationWitness() throws Throwable {
        M3CooccurrencePpmiPositiveOracle.main(new String[0]);
    }
}
