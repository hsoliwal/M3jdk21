// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3MasteryTest {

    @TempDir
    Path root;

    @Test
    void verifiesPinnedReceiptInsideRepository() throws Exception {
        String tsv = receipt();
        String expected = cell(tsv, "root");
        Path receipt = root.resolve("m3/build/a3/mastery.tsv");
        Files.createDirectories(receipt.getParent());
        Files.writeString(receipt, tsv, StandardCharsets.UTF_8);

        var verified =
                A3Mastery.verify(
                        root,
                        Path.of("m3/build/a3/mastery.tsv"),
                        expected);

        assertEquals(expected, verified.root());
        assertEquals("EXHAUSTIVE", verified.scheduleCoverage());
        assertEquals(3, verified.challengeDonorLedgerRows());
    }

    @Test
    void refusesRootDriftMissingReceiptAndPathEscape() throws Exception {
        String tsv = receipt();
        Path receipt = root.resolve("m3/build/a3/mastery.tsv");
        Files.createDirectories(receipt.getParent());
        Files.writeString(receipt, tsv, StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Mastery.verify(
                                root,
                                Path.of("m3/build/a3/mastery.tsv"),
                                "0".repeat(64)));
        assertThrows(
                java.io.IOException.class,
                () ->
                        A3Mastery.verify(
                                root,
                                Path.of("m3/build/a3/missing.tsv"),
                                cell(tsv, "root")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Mastery.verify(
                                root,
                                Path.of("../escape.tsv"),
                                cell(tsv, "root")));
    }

    private static String receipt() {
        LinkedHashMap<String, String> v = new LinkedHashMap<>();
        v.put("schema", "M3_RECIPE_MASTERY_FANIN_V6");
        v.put("root", "");
        v.put("crateRoot", "1".repeat(64));
        v.put("masteryPlanRoot", "2".repeat(64));
        v.put("masteryCompletionRoot", "3".repeat(64));
        v.put("legacyExecutionRoot", "4".repeat(64));
        v.put("fullWrapperExecutionRoot", "5".repeat(64));
        v.put("schedulePlanRoot", "6".repeat(64));
        v.put("scheduleCoverage", "EXHAUSTIVE");
        v.put("counterexampleRegistryRoot", "7".repeat(64));
        v.put("counterexampleReplayRoot", "8".repeat(64));
        v.put("counterexampleCampaignRoot", "9".repeat(64));
        v.put("challengeDonorReviewRoot", "a".repeat(64));
        v.put("challengeDonorLedgerRoot", "b".repeat(64));
        v.put("challengeDonorLedgerRows", "3");
        v.put("counterexampleFixtureCount", "1");
        v.put("counterexampleRunCount", "1");
        v.put("regexMatrixRoot", "c".repeat(64));
        v.put("regexOracleRoot", "d".repeat(64));
        v.put("regexCaseCount", "10000");
        v.put("jniRequired", "false");
        v.put("nativeParityRoot", "-");
        v.put("nativeParityPairCount", "0");
        v.put("complete", "true");
        v.put("sourceMutationAuthority", "false");
        v.put("semanticAuthority", "false");
        v.put("donorSourceCopyAuthority", "false");
        v.put("replacementAuthority", "false");
        v.put("mergeAuthority", "false");
        v.put("promotionAuthority", "false");
        v.put("root", root(v));

        StringBuilder out = new StringBuilder("key\tvalue\n");
        v.forEach((key, value) -> out.append(key).append('\t').append(value).append('\n'));
        return out.toString();
    }

    private static String root(Map<String, String> v) {
        return digest(
                "M3_RECIPE_MASTERY_FANIN_V6",
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
                "",
                v.get("nativeParityPairCount"),
                "complete=true",
                "sourceMutationAuthority=false",
                "semanticAuthority=false",
                "donorSourceCopyAuthority=false",
                "replacementAuthority=false",
                "mergeAuthority=false",
                "promotionAuthority=false");
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
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String cell(String tsv, String key) {
        return tsv.lines()
                .filter(line -> line.startsWith(key + "\t"))
                .map(line -> line.substring(key.length() + 1))
                .findFirst()
                .orElseThrow();
    }
}
