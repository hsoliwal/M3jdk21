// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of CooccurrencePpmiRankingOracle (one JVM, one main call per mode): 2,500 seeded corpora full-sort differential and digest; 6,000-candidate top-10 allocation envelope (Java path; --native-boundary belongs to the JNI leaf); the oracle prints its figures. */
class CooccurrencePpmiRankingOracleTest {
    @Test void seededRankingOracle() throws Throwable {
        CooccurrencePpmiRankingOracle.main(new String[0]);
    }
    @Test void rankingAllocationWitness() throws Throwable {
        CooccurrencePpmiRankingOracle.main(new String[]{"--allocation"});
    }
}
