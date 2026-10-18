// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import org.junit.jupiter.api.Test;

/** Execute the stand-alone Java21 token automaton oracle in the owning Maven module. */
final class M3TokenRegexTest {
    @Test
    void tokenIdsKindsOffsetsAndRandomizedOracle() {
        M3TokenRegexSelfTest.main(new String[0]);
    }
}
