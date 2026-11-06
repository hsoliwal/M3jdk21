// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Gate 2 of the UTF-16 atom relocation: the donor proof main of {@link M3Utf16GlobOracle} runs in the test phase
 * (Java mode, no JNI) and must reproduce the figures of the donor crate's PROOF_20261105.md: 73,310 full-match
 * comparisons (49 directed, 41,261 exhaustive tiny text/pattern pairs, 32,000 seeded pairs), the 4 null/oversize
 * refusals, input immutability and the SHA-256 result root shared by the crate's Java, JNI and interpreted-JNI runs.
 */
final class M3Utf16GlobProofTest {
    private static final String PROOF_PREFIX = "M3_UTF16_GLOB_V1 cases=73310 sha256=";
    private static final String PROOF_DIGEST = "5b3800a076a582f982dbe79f0146a5a4d1cae12f4e2f578e80b058120331763b";
    private static final String PROOF_SUFFIX =
            " java=PASS jni=NOT_RUN nullOrOversizeRefused=4 inputImmutable=true chromeCalls=0 promotion=false";
    private static final String JAVA_MODE = "--java";
    private static final String NO_LIBRARY = "NO_LIB";

    @Test
    void proofMainReproducesCrateDigest() throws Exception {
        String line = M3ProofCapture.run(() -> M3Utf16GlobOracle.main(new String[] {JAVA_MODE, NO_LIBRARY}));
        assertEquals(PROOF_PREFIX + PROOF_DIGEST + PROOF_SUFFIX, line);
    }
}
