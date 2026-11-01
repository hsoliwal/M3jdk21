// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class M3RecipeMasteryPortableReceiptTest {
    @Test
    void verifiesStrictAuthorityFreeReceiptAgainstPinnedRoot() {
        String tsv = receipt(false);
        String root = cell(tsv, "root");

        M3RecipeMasteryPortableReceipt.Verified verified =
                M3RecipeMasteryPortableReceipt.verify(tsv, root);

        assertEquals(root, verified.root());
        assertEquals("EXHAUSTIVE", verified.scheduleCoverage());
        assertEquals(3, verified.challengeDonorLedgerRows());
        assertEquals(2, verified.counterexampleFixtureCount());
        assertEquals(4, verified.counterexampleRunCount());
        assertEquals(10_000, verified.regexCaseCount());
        assertFalse(verified.jniRequired());
        assertFalse(verified.sourceMutationAuthority());
        assertFalse(verified.donorSourceCopyAuthority());
        assertFalse(verified.replacementAuthority());
        assertFalse(verified.mergeAuthority());
        assertFalse(verified.promotionAuthority());
    }

    @Test
    void verifiesJniReceiptAndRejectsRootAuthoritySchemaAndParityDrift() {
        String tsv = receipt(true);
        String root = cell(tsv, "root");
        assertTrue(M3RecipeMasteryPortableReceipt.verify(tsv, root).jniRequired());

        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeMasteryPortableReceipt.verify(tsv, "0".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3RecipeMasteryPortableReceipt.verify(
                                tsv.replace("promotionAuthority\tfalse", "promotionAuthority\ttrue"),
                                root));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3RecipeMasteryPortableReceipt.verify(
                                tsv.replace("jniRequired\ttrue", "jniRequired\tTRUE"),
                                root));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3RecipeMasteryPortableReceipt.verify(
                                tsv.replace("nativeParityPairCount\t3", "nativeParityPairCount\t0"),
                                root));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3RecipeMasteryPortableReceipt.verify(
                                tsv.replaceFirst("key\\tvalue", "bad\\theader"),
                                root));
    }

    private static String receipt(boolean jni) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("schema", M3RecipeMasteryPortableReceipt.SCHEMA);
        values.put("root", "");
        values.put("crateRoot", "1".repeat(64));
        values.put("masteryPlanRoot", "2".repeat(64));
        values.put("masteryCompletionRoot", "3".repeat(64));
        values.put("legacyExecutionRoot", "4".repeat(64));
        values.put("fullWrapperExecutionRoot", "5".repeat(64));
        values.put("schedulePlanRoot", "6".repeat(64));
        values.put("scheduleCoverage", "EXHAUSTIVE");
        values.put("counterexampleRegistryRoot", "7".repeat(64));
        values.put("counterexampleReplayRoot", "8".repeat(64));
        values.put("counterexampleCampaignRoot", "9".repeat(64));
        values.put("challengeDonorReviewRoot", "a".repeat(64));
        values.put("challengeDonorLedgerRoot", "b".repeat(64));
        values.put("challengeDonorLedgerRows", "3");
        values.put("counterexampleFixtureCount", "2");
        values.put("counterexampleRunCount", "4");
        values.put("regexMatrixRoot", "c".repeat(64));
        values.put("regexOracleRoot", "d".repeat(64));
        values.put("regexCaseCount", "10000");
        values.put("jniRequired", Boolean.toString(jni));
        values.put("nativeParityRoot", jni ? "e".repeat(64) : "-");
        values.put("nativeParityPairCount", jni ? "3" : "0");
        values.put("complete", "true");
        values.put("sourceMutationAuthority", "false");
        values.put("semanticAuthority", "false");
        values.put("donorSourceCopyAuthority", "false");
        values.put("replacementAuthority", "false");
        values.put("mergeAuthority", "false");
        values.put("promotionAuthority", "false");
        values.put("root", root(values));
        StringBuilder out = new StringBuilder("key\tvalue\n");
        values.forEach((key, value) -> out.append(key).append('\t').append(value).append('\n'));
        return out.toString();
    }

    private static String root(Map<String, String> v) {
        String nativeRoot = "-".equals(v.get("nativeParityRoot")) ? "" : v.get("nativeParityRoot");
        return digest(
                M3RecipeMasteryPortableReceipt.SCHEMA,
                v.get("crateRoot"),
                v.get("masteryPlanRoot"),
                v.get("masteryCompletionRoot"),
                v.get("legacyExecutionRoot"),
                v.get("fullWrapperExecutionRoot"),
                v.get("schedulePlanRoot"),
                v.get("scheduleCoverage"),
                v.get("counterexampleRegistryRoot"),
                v.get("counterexampleReplayRoot"),
                v.get("counterexampleCampaignRoot"),
                v.get("challengeDonorReviewRoot"),
                v.get("challengeDonorLedgerRoot"),
                v.get("challengeDonorLedgerRows"),
                v.get("counterexampleFixtureCount"),
                v.get("counterexampleRunCount"),
                v.get("regexMatrixRoot"),
                v.get("regexOracleRoot"),
                v.get("regexCaseCount"),
                v.get("jniRequired"),
                nativeRoot,
                v.get("nativeParityPairCount"),
                "complete=true",
                "sourceMutationAuthority=false",
                "semanticAuthority=false",
                "donorSourceCopyAuthority=false",
                "replacementAuthority=false",
                "mergeAuthority=false",
                "promotionAuthority=false");
    }

    private static String cell(String tsv, String key) {
        return tsv.lines()
                .filter(line -> line.startsWith(key + "\t"))
                .map(line -> line.substring(key.length() + 1))
                .findFirst()
                .orElseThrow();
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
