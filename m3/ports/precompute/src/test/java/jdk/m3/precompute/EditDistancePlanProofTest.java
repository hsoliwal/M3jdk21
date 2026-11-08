// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Gate 2 of the bit-parallel edit oracle relocation (census leaf L040): the donor proof main of
 * {@link M3EditDistancePlanOracle}, whose fast method is bound to {@link M3EditDistancePlan}, runs in the test
 * phase (Java mode, no JNI) and must reproduce the receipt of the donor crate's own Java run: 46,960 differentials
 * (11 directed, 14,641 exhaustive tiny pairs over NUL / a / U+D800, 30,000 seeded pairs plus 2,308 swapped
 * orientations), the 5 null/oversize refusals and the SHA-256 digest of the reference distances. The digest is
 * fixed by the independent DP reference, so an owner that disagreed with it would fail inside the main before the
 * line is printed.
 */
//FLLDCIM: RELOCATE - census L040 gate-2 wrapper, M3JDK-authored (recipe m3/ports/precompute/recipe/utf16-edit-oracle-20261107, module style M3Utf16GlobProofTest)
final class EditDistancePlanProofTest {
    private static final String PROOF_PREFIX =
            "M3_UTF16_BITPARALLEL_EDIT_PROOF_V1 differentials=46960 validationRefusals=5 sha256=";
    private static final String PROOF_DIGEST = "170f1ab0ca2853c825f91ba8081775ae6490f9e780f4d639ed71287db13446cf";
    private static final String PROOF_SUFFIX = " java=PASS jni=NOT_RUN donorCodeCopied=false promotion=false";
    private static final String JAVA_MODE = "--java";
    private static final String NO_LIBRARY = "NO_LIB";

    @Test
    void proofMainReproducesDonorReceipt() throws Exception {
        String line = ProofCapture.run(() -> EditDistancePlanOracle.main(new String[] {JAVA_MODE, NO_LIBRARY}));
        assertEquals(PROOF_PREFIX + PROOF_DIGEST + PROOF_SUFFIX, line);
    }
}
