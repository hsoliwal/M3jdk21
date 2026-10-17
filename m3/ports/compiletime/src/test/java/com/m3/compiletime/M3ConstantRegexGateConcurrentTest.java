// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import org.junit.jupiter.api.Test;

/** Same standalone Java21 contract is run under Maven/JUnit. */
final class M3ConstantRegexGateConcurrentTest {
    @Test void preservesPrecomputedTruthOnConcurrentStaticGate() throws Exception {
        M3ConstantRegexGateConcurrentContract.main(new String[0]);
    }
}
