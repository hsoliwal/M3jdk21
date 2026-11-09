// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of CooccurrencePpmiPositiveOracle (one JVM, one main call per mode): low-positive-density fixture and heap growth boundaries; the default mode adds the public allocation envelope; the oracle prints its figures. */
class CooccurrencePpmiPositiveOracleTest {
    @Test void positiveBehavior() throws Throwable {
        CooccurrencePpmiPositiveOracle.main(new String[]{"--behavior"});
    }
    @Test void positiveAllocationWitness() throws Throwable {
        CooccurrencePpmiPositiveOracle.main(new String[0]);
    }
}
