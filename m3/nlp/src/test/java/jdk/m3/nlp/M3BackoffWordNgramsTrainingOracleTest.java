// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of M3BackoffWordNgramsTrainingOracle (one JVM, one main call per mode): primitive training rows: behavior, AVL layout, overflow, boxing allocation and public allocation; the oracle prints its figures. */
class M3BackoffWordNgramsTrainingOracleTest {
    @Test void trainingBehavior() throws Throwable {
        M3BackoffWordNgramsTrainingOracle.main(new String[]{"--behavior"});
    }
    @Test void trainingLayout() throws Throwable {
        M3BackoffWordNgramsTrainingOracle.main(new String[]{"--layout"});
    }
    @Test void trainingOverflow() throws Throwable {
        M3BackoffWordNgramsTrainingOracle.main(new String[]{"--overflow"});
    }
    @Test void trainingAllocationWitness() throws Throwable {
        M3BackoffWordNgramsTrainingOracle.main(new String[]{"--allocation"});
    }
    @Test void publicAllocation() throws Throwable {
        M3BackoffWordNgramsTrainingOracle.main(new String[]{"--public-allocation"});
    }
}
