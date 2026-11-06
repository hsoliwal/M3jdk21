// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of M3ScopedNgramsOracle: 384 seeded corpora, 183,900 checks; the oracle prints its figures and digest. */
class M3ScopedNgramsOracleTest {
    @Test void seededTopKOracle() throws Exception {
        M3ScopedNgramsOracle.main(new String[]{"verify"});
    }
}
