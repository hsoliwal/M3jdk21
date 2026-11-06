// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Gate 2 of the UTF-16 atom relocation: the donor proof main of {@link M3LcsPlanOracle} runs in the test phase (Java
 * mode, no JNI) and must reproduce the figures of the donor crate's PROOF_20261105.md: 47,614 admitted cases (14
 * directed, 1,600 exhaustive three-symbol pairs, 46,000 seeded pairs; the 4 refusals are counted separately) and the
 * SHA-256 answer digest shared by the crate's Java, JNI and interpreted-JNI runs.
 */
final class M3LcsPlanProofTest {
    private static final String PROOF_PREFIX = "M3_UTF16_LCS_BITVECTOR_V1 cases=47614 answerSha256=";
    private static final String PROOF_DIGEST = "b12c30396266431206d782c55599803d5e05386d748536064302dbb746767fc1";
    private static final String PROOF_SUFFIX = " java=PASS jni=NOT_RUN sourcePromotion=false chromeCalls=0";
    private static final String JAVA_MODE = "--java";
    private static final String NO_LIBRARY = "NO_LIB";

    @Test
    void proofMainReproducesCrateDigest() throws Exception {
        String line = M3ProofCapture.run(() -> M3LcsPlanOracle.main(new String[] {JAVA_MODE, NO_LIBRARY}));
        assertEquals(PROOF_PREFIX + PROOF_DIGEST + PROOF_SUFFIX, line);
    }
}
