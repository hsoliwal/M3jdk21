// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of M3BackoffWordNgramsContextOracle (one JVM, one main call per mode): reverse suffix / depth / ordering parity of the frozen context index; suffix-probe allocation and layout witnesses; the oracle prints its figures. */
class M3BackoffWordNgramsContextOracleTest {
    @Test void contextParity() throws Throwable {
        M3BackoffWordNgramsContextOracle.main(new String[0]);
    }
    @Test void suffixProbeAllocationWitness() throws Throwable {
        M3BackoffWordNgramsContextOracle.main(new String[]{"--allocation"});
    }
    @Test void contextLayoutWitness() throws Throwable {
        M3BackoffWordNgramsContextOracle.main(new String[]{"--layout"});
    }
}
