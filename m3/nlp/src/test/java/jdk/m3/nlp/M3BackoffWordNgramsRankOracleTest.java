// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of M3BackoffWordNgramsRankOracle (one JVM, one main call per mode): 2,000 seeded corpora / 1,071,280 checks and digest; exact long counts above 2^53; 6,000-candidate top-10 allocation witness; the oracle prints its figures. */
class M3BackoffWordNgramsRankOracleTest {
    @Test void seededRankOracle() throws Throwable {
        M3BackoffWordNgramsRankOracle.main(new String[0]);
    }
    @Test void exactLongCounts() throws Throwable {
        M3BackoffWordNgramsRankOracle.main(new String[]{"--long-counts"});
    }
    @Test void queryAllocationWitness() throws Throwable {
        M3BackoffWordNgramsRankOracle.main(new String[]{"--allocation"});
    }
}
