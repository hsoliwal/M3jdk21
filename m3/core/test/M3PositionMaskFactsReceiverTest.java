/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import com.m3.text.M3PositionMaskFacts;

/**
 * Source-bound receiver proof for the metadata-only fuzzy histogram handoff.
 */
public final class M3PositionMaskFactsReceiverTest {
    private static final String ROOT =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private M3PositionMaskFactsReceiverTest() {
    }

    public static void main(String[] args) {
        int checks = 0;
        M3PositionMaskFacts facts = M3PositionMaskFacts.fromPrecomputed(
                "ab4edcbd7eb475a3c9abb12d4a0fc14703a826ec",
                8192,
                128,
                4096,
                107076L,
                ROOT,
                64,
                128);
        check(facts.schemaVersion() == 1, "schema");
        checks++;
        check(facts.rowCount() == 8192 && facts.blockCount() == 128, "geometry");
        checks++;
        check(facts.entryCount() == 4096 && facts.primitivePayloadBytes() == 107076L, "payload metadata");
        checks++;
        check(facts.sourceRevision().equals("ab4edcbd7eb475a3c9abb12d4a0fc14703a826ec"), "source revision");
        checks++;
        check(facts.rootHash().equals(ROOT) && facts.metadataOnly(), "identity");
        checks++;
        check(!facts.autoNativeHistogramCandidate(0), "zero policy");
        checks++;
        check(facts.autoNativeHistogramCandidate(64), "cutoff policy");
        checks++;
        check(!facts.autoNativeHistogramCandidate(65), "large policy");
        checks++;
        expectIllegal(() -> M3PositionMaskFacts.fromPrecomputed(
                "", 1, 1, 1, 1L, ROOT, 64, 128), "empty revision");
        checks++;
        expectIllegal(() -> M3PositionMaskFacts.fromPrecomputed(
                "source", -1, 1, 1, 1L, ROOT, 64, 128), "negative geometry");
        checks++;
        expectIllegal(() -> M3PositionMaskFacts.fromPrecomputed(
                "source", 1, 1, 1, 1L, "not-a-digest", 64, 128), "digest");
        checks++;
        expectIllegal(() -> facts.autoNativeHistogramCandidate(-1), "negative selection");
        checks++;
        expectIllegal(() -> M3PositionMaskFacts.fromPrecomputed(
                "source", 1, 1, 1, 1L, ROOT, 0, 128), "zero native cutoff");
        checks++;
        expectIllegal(() -> M3PositionMaskFacts.fromPrecomputed(
                "source", 1, 1, 1, 1L, ROOT, 64, 0), "zero native slab");
        checks++;
        expectIllegal(() -> M3PositionMaskFacts.fromPrecomputed(
                "source", 1, 1, 1, 89L, ROOT, 64, 128), "payload geometry");
        checks++;
        expectIllegal(() -> M3PositionMaskFacts.fromPrecomputed(
                "source", 1, 0, 1, 86L, ROOT, 64, 128), "entry geometry");
        checks++;
        expectIllegal(() -> M3PositionMaskFacts.fromPrecomputed(
                "source", 1, 1, 1, 1L, ROOT.toUpperCase(), 64, 128), "uppercase digest");
        checks++;
        expectNull(() -> M3PositionMaskFacts.fromPrecomputed(
                null, 1, 1, 1, 1L, ROOT, 64, 128), "null revision");
        checks++;
        expectNull(() -> M3PositionMaskFacts.fromPrecomputed(
                "source", 1, 1, 1, 1L, null, 64, 128), "null digest");
        checks++;
        System.out.println("M3_POSITION_MASK_FACTS_RECEIVER_PASS checks=" + checks
                + " metadata_only=" + facts.metadataOnly());
    }

    private static void expectIllegal(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " accepted");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void expectNull(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " accepted");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
