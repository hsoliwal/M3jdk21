// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of BackoffWordNgramsTrainingContextIndexOracle (one JVM, one main call per mode): reverse array trie of the training context index: behavior, allocation and layout witnesses; the oracle prints its figures. */
class BackoffWordNgramsTrainingContextIndexOracleTest {
    @Test void trainingContextBehavior() throws Throwable {
        BackoffWordNgramsTrainingContextIndexOracle.main(new String[0]);
    }
    @Test void trainingContextAllocationWitness() throws Throwable {
        BackoffWordNgramsTrainingContextIndexOracle.main(new String[]{"--allocation"});
    }
    @Test void trainingContextLayoutWitness() throws Throwable {
        BackoffWordNgramsTrainingContextIndexOracle.main(new String[]{"--layout"});
    }
}
