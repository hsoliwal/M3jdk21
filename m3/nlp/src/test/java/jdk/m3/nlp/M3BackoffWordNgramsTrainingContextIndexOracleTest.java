// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of M3BackoffWordNgramsTrainingContextIndexOracle (one JVM, one main call per mode): reverse array trie of the training context index: behavior, allocation and layout witnesses; the oracle prints its figures. */
class M3BackoffWordNgramsTrainingContextIndexOracleTest {
    @Test void trainingContextBehavior() throws Throwable {
        M3BackoffWordNgramsTrainingContextIndexOracle.main(new String[0]);
    }
    @Test void trainingContextAllocationWitness() throws Throwable {
        M3BackoffWordNgramsTrainingContextIndexOracle.main(new String[]{"--allocation"});
    }
    @Test void trainingContextLayoutWitness() throws Throwable {
        M3BackoffWordNgramsTrainingContextIndexOracle.main(new String[]{"--layout"});
    }
}
