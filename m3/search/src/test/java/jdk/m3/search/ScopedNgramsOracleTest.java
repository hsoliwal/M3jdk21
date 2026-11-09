// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import org.junit.jupiter.api.Test;

/** JUnit 5 wrapper of ScopedNgramsOracle: 384 seeded corpora, 183,900 checks; the oracle prints its figures and digest. */
class ScopedNgramsOracleTest {
    @Test void seededTopKOracle() throws Exception {
        ScopedNgramsOracle.main(new String[]{"verify"});
    }
}
