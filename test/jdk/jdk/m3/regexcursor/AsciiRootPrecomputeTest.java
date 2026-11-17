// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import org.junit.jupiter.api.Test;

/** JUnit/Maven entry point; native JNI remains an explicit standalone gate. */
class AsciiRootPrecomputeTest {
    @Test
    void exhaustiveUtf16AndSeededMaximalMunchParity() throws Exception {
        AsciiRootPrecomputeProof.verify(40_000);
    }
}
