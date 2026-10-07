// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dependency-free verifier for the portable M3 recipe-mastery V6 receipt.
 *
 * <p>This class intentionally does not recreate the Synexia mastery engine. It validates the
 * exported wire schema, recomputes the exact V6 content-addressed root, requires the caller's
 * expected root, and refuses every authority-bearing variant. Cross-repository consumers such as
 * M3JDK21 and Nebula can therefore verify mastery evidence without importing the full mastery graph.
 */
public final class M3RecipeMasteryPortableReceipt {
    public static final String SCHEMA = "M3_RECIPE_MASTERY_FANIN_V6";

    private static final List<String> KEYS =
            List.of(
                    "schema",
                    "root",
                    "crateRoot",
                    "masteryPlanRoot",
                    "masteryCompletionRoot",
                    "legacyExecutionRoot",
                    "fullWrapperExecutionRoot",
                    "schedulePlanRoot",
                    "scheduleCoverage",
                    "counterexampleRegistryRoot",
                    "counterexampleReplayRoot",
                    "counterexampleCampaignRoot",
                    "challengeDonorReviewRoot",
                    "challengeDonorLedgerRoot",
                    "challengeDonorLedgerRows",
                    "counterexampleFixtureCount",
                    "counterexampleRunCount",
                    "regexMatrixRoot",
                    "regexOracleRoot",
                    "regexCaseCount",
                    "jniRequired",
                    "nativeParityRoot",
                    "nativeParityPairCount",
                    "complete",
                    "sourceMutationAuthority",
                    "semanticAuthority",
                    "donorSourceCopyAuthority",
                    "replacementAuthority",
                    "mergeAuthority",
                    "promotionAuthority");

    public record Verified(
            String root,
            String crateRoot,
            String masteryPlanRoot,
            String scheduleCoverage,
            int challengeDonorLedgerRows,
            int counterexampleFixtureCount,
            int counterexampleRunCount,
            int regexCaseCount,
            boolean jniRequired,
            int nativeParityPairCount) {
        public Verified {
            requireSha(root, "root");
            requireSha(crateRoot, "crateRoot");
            requireSha(masteryPlanRoot, "masteryPlanRoot");
            requireToken(scheduleCoverage, "scheduleCoverage");
            if (challengeDonorLedgerRows < 3
                    || counterexampleFixtureCount < 1
                    || counterexampleRunCount < counterexampleFixtureCount
                    || regexCaseCount < 1
                    || nativeParityPairCount < 0) {
                throw new IllegalArgumentException("invalid portable mastery counts");
            }
            if (!jniRequired && nativeParityPairCount != 0) {
                throw new IllegalArgumentException("unexpected native parity count");
            }
        }

        public boolean sourceMutationAuthority() {
            return false;
        }

        public boolean donorSourceCopyAuthority() {
            return false;
        }

        public boolean replacementAuthority() {
            return false;
        }

        public boolean mergeAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    private M3RecipeMasteryPortableReceipt() {
        throw new AssertionError("No instances");
    }

    public static Verified verify(String tsv, String expectedRoot) {
        Map<String, String> values = parse(tsv);
        String expected = requireSha(expectedRoot, "expectedRoot");
        String actual = requireSha(values.get("root"), "root");
        String recomputed = root(values);
        if (!actual.equals(recomputed) || !actual.equals(expected)) {
            throw new IllegalArgumentException("portable mastery root mismatch");
        }

        boolean jniRequired = strictBoolean(values.get("jniRequired"), "jniRequired");
        int nativePairs = integer(values.get("nativeParityPairCount"), "nativeParityPairCount");
        String nativeRoot = values.get("nativeParityRoot");
        if (jniRequired) {
            requireSha(nativeRoot, "nativeParityRoot");
            if (nativePairs < 1) {
                throw new IllegalArgumentException("missing native parity evidence");
            }
        } else if (!"-".equals(nativeRoot) || nativePairs != 0) {
            throw new IllegalArgumentException("unexpected native parity evidence");
        }

        return new Verified(
                actual,
                requireSha(values.get("crateRoot"), "crateRoot"),
                requireSha(values.get("masteryPlanRoot"), "masteryPlanRoot"),
                requireToken(values.get("scheduleCoverage"), "scheduleCoverage"),
                integer(values.get("challengeDonorLedgerRows"), "challengeDonorLedgerRows"),
                integer(values.get("counterexampleFixtureCount"), "counterexampleFixtureCount"),
                integer(values.get("counterexampleRunCount"), "counterexampleRunCount"),
                integer(values.get("regexCaseCount"), "regexCaseCount"),
                jniRequired,
                nativePairs);
    }

    private static Map<String, String> parse(String tsv) {
        String[] lines = Objects.requireNonNull(tsv, "tsv").split("\\R", -1);
        if (lines.length < 2 || !"key\tvalue".equals(lines[0])) {
            throw new IllegalArgumentException("portable mastery header");
        }

        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index];
            if (line.isEmpty()) continue;
            String[] cells = line.split("\\t", -1);
            if (cells.length != 2
                    || cells[0].isEmpty()
                    || values.putIfAbsent(cells[0], cells[1]) != null) {
                throw new IllegalArgumentException("portable mastery row");
            }
        }
        if (!values.keySet().equals(new LinkedHashSet<>(KEYS))
                || !SCHEMA.equals(values.get("schema"))
                || !"true".equals(values.get("complete"))
                || !authorityFalse(values, "sourceMutationAuthority")
                || !authorityFalse(values, "semanticAuthority")
                || !authorityFalse(values, "donorSourceCopyAuthority")
                || !authorityFalse(values, "replacementAuthority")
                || !authorityFalse(values, "mergeAuthority")
                || !authorityFalse(values, "promotionAuthority")) {
            throw new IllegalArgumentException("portable mastery schema/authority");
        }

        for (String key :
                List.of(
                        "crateRoot",
                        "masteryPlanRoot",
                        "masteryCompletionRoot",
                        "legacyExecutionRoot",
                        "fullWrapperExecutionRoot",
                        "schedulePlanRoot",
                        "counterexampleRegistryRoot",
                        "counterexampleReplayRoot",
                        "counterexampleCampaignRoot",
                        "challengeDonorReviewRoot",
                        "challengeDonorLedgerRoot",
                        "regexMatrixRoot",
                        "regexOracleRoot")) {
            requireSha(values.get(key), key);
        }
        requireToken(values.get("scheduleCoverage"), "scheduleCoverage");
        strictBoolean(values.get("jniRequired"), "jniRequired");
        integer(values.get("challengeDonorLedgerRows"), "challengeDonorLedgerRows");
        integer(values.get("counterexampleFixtureCount"), "counterexampleFixtureCount");
        integer(values.get("counterexampleRunCount"), "counterexampleRunCount");
        integer(values.get("regexCaseCount"), "regexCaseCount");
        integer(values.get("nativeParityPairCount"), "nativeParityPairCount");
        return Map.copyOf(values);
    }

    private static String root(Map<String, String> values) {
        String nativeRoot =
                "-".equals(values.get("nativeParityRoot")) ? "" : values.get("nativeParityRoot");
        return digest(
                SCHEMA,
                values.get("crateRoot"),
                values.get("masteryPlanRoot"),
                values.get("masteryCompletionRoot"),
                values.get("legacyExecutionRoot"),
                values.get("fullWrapperExecutionRoot"),
                values.get("schedulePlanRoot"),
                values.get("scheduleCoverage"),
                values.get("counterexampleRegistryRoot"),
                values.get("counterexampleReplayRoot"),
                values.get("counterexampleCampaignRoot"),
                values.get("challengeDonorReviewRoot"),
                values.get("challengeDonorLedgerRoot"),
                values.get("challengeDonorLedgerRows"),
                values.get("counterexampleFixtureCount"),
                values.get("counterexampleRunCount"),
                values.get("regexMatrixRoot"),
                values.get("regexOracleRoot"),
                values.get("regexCaseCount"),
                values.get("jniRequired"),
                nativeRoot,
                values.get("nativeParityPairCount"),
                "complete=true",
                "sourceMutationAuthority=false",
                "semanticAuthority=false",
                "donorSourceCopyAuthority=false",
                "replacementAuthority=false",
                "mergeAuthority=false",
                "promotionAuthority=false");
    }

    private static boolean authorityFalse(Map<String, String> values, String key) {
        return "false".equals(values.get(key));
    }

    private static boolean strictBoolean(String value, String field) {
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException(field);
        }
        return Boolean.parseBoolean(value);
    }

    private static int integer(String value, String field) {
        try {
            return Integer.parseInt(Objects.requireNonNull(value, field));
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(field, failure);
        }
    }

    private static String requireToken(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (!checked.matches("[A-Z][A-Z0-9_]{0,63}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String requireSha(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes =
                        Objects.requireNonNull(value, "digest value")
                                .getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
