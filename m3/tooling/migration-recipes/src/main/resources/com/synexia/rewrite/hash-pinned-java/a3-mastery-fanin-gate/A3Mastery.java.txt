// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Strict portable import of the Synexia M3 recipe-mastery V6 fan-in receipt. */
public final class A3Mastery {

    public static final String SCHEMA = "M3_RECIPE_MASTERY_FANIN_V6";
    public static final int REGEX_CASES = 10_000;

    public record Receipt(
            String crateRoot,
            String masteryPlanRoot,
            String masteryCompletionRoot,
            String legacyExecutionRoot,
            String fullWrapperExecutionRoot,
            String schedulePlanRoot,
            String scheduleCoverage,
            String counterexampleRegistryRoot,
            String counterexampleReplayRoot,
            String counterexampleCampaignRoot,
            String challengeDonorReviewRoot,
            String challengeDonorLedgerRoot,
            int challengeDonorLedgerRows,
            int counterexampleFixtureCount,
            int counterexampleRunCount,
            String regexMatrixRoot,
            String regexOracleRoot,
            int regexCaseCount,
            boolean jniRequired,
            String nativeParityRoot,
            int nativeParityPairCount,
            boolean complete,
            String root) {

        public Receipt {
            crateRoot = sha(crateRoot, "crateRoot");
            masteryPlanRoot = sha(masteryPlanRoot, "masteryPlanRoot");
            masteryCompletionRoot = sha(masteryCompletionRoot, "masteryCompletionRoot");
            legacyExecutionRoot = sha(legacyExecutionRoot, "legacyExecutionRoot");
            fullWrapperExecutionRoot = sha(fullWrapperExecutionRoot, "fullWrapperExecutionRoot");
            schedulePlanRoot = sha(schedulePlanRoot, "schedulePlanRoot");
            scheduleCoverage = token(scheduleCoverage, "scheduleCoverage");
            counterexampleRegistryRoot =
                    sha(counterexampleRegistryRoot, "counterexampleRegistryRoot");
            counterexampleReplayRoot =
                    sha(counterexampleReplayRoot, "counterexampleReplayRoot");
            counterexampleCampaignRoot =
                    sha(counterexampleCampaignRoot, "counterexampleCampaignRoot");
            challengeDonorReviewRoot =
                    sha(challengeDonorReviewRoot, "challengeDonorReviewRoot");
            challengeDonorLedgerRoot =
                    sha(challengeDonorLedgerRoot, "challengeDonorLedgerRoot");
            regexMatrixRoot = sha(regexMatrixRoot, "regexMatrixRoot");
            regexOracleRoot = sha(regexOracleRoot, "regexOracleRoot");

            if (!"EXHAUSTIVE".equals(scheduleCoverage)) {
                throw new IllegalArgumentException("scheduleCoverage");
            }
            if (challengeDonorLedgerRows < 3) {
                throw new IllegalArgumentException("challengeDonorLedgerRows");
            }
            if (counterexampleFixtureCount < 1
                    || counterexampleRunCount < counterexampleFixtureCount) {
                throw new IllegalArgumentException("counterexample counts");
            }
            if (regexCaseCount != REGEX_CASES) {
                throw new IllegalArgumentException("regexCaseCount");
            }
            nativeParityRoot = Objects.toString(nativeParityRoot, "").strip();
            if (jniRequired) {
                nativeParityRoot = sha(nativeParityRoot, "nativeParityRoot");
                if (nativeParityPairCount < 1) {
                    throw new IllegalArgumentException("nativeParityPairCount");
                }
            } else if (!nativeParityRoot.isEmpty() || nativeParityPairCount != 0) {
                throw new IllegalArgumentException("unexpected native parity evidence");
            }
            if (!complete) {
                throw new IllegalArgumentException("incomplete mastery fan-in");
            }

            String expected =
                    digest(
                            SCHEMA,
                            crateRoot,
                            masteryPlanRoot,
                            masteryCompletionRoot,
                            legacyExecutionRoot,
                            fullWrapperExecutionRoot,
                            schedulePlanRoot,
                            scheduleCoverage,
                            counterexampleRegistryRoot,
                            counterexampleReplayRoot,
                            counterexampleCampaignRoot,
                            challengeDonorReviewRoot,
                            challengeDonorLedgerRoot,
                            Integer.toString(challengeDonorLedgerRows),
                            Integer.toString(counterexampleFixtureCount),
                            Integer.toString(counterexampleRunCount),
                            regexMatrixRoot,
                            regexOracleRoot,
                            Integer.toString(regexCaseCount),
                            Boolean.toString(jniRequired),
                            nativeParityRoot,
                            Integer.toString(nativeParityPairCount),
                            "complete=true",
                            "sourceMutationAuthority=false",
                            "semanticAuthority=false",
                            "donorSourceCopyAuthority=false",
                            "replacementAuthority=false",
                            "mergeAuthority=false",
                            "promotionAuthority=false");
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("mastery fan-in root mismatch");
            }
        }

        public boolean sourceMutationAuthority() {
            return false;
        }

        public boolean semanticAuthority() {
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

        public String toTsv() {
            StringBuilder out = new StringBuilder("key\tvalue\n");
            row(out, "schema", SCHEMA);
            row(out, "root", root);
            row(out, "crateRoot", crateRoot);
            row(out, "masteryPlanRoot", masteryPlanRoot);
            row(out, "masteryCompletionRoot", masteryCompletionRoot);
            row(out, "legacyExecutionRoot", legacyExecutionRoot);
            row(out, "fullWrapperExecutionRoot", fullWrapperExecutionRoot);
            row(out, "schedulePlanRoot", schedulePlanRoot);
            row(out, "scheduleCoverage", scheduleCoverage);
            row(out, "counterexampleRegistryRoot", counterexampleRegistryRoot);
            row(out, "counterexampleReplayRoot", counterexampleReplayRoot);
            row(out, "counterexampleCampaignRoot", counterexampleCampaignRoot);
            row(out, "challengeDonorReviewRoot", challengeDonorReviewRoot);
            row(out, "challengeDonorLedgerRoot", challengeDonorLedgerRoot);
            row(out, "challengeDonorLedgerRows", Integer.toString(challengeDonorLedgerRows));
            row(out, "counterexampleFixtureCount", Integer.toString(counterexampleFixtureCount));
            row(out, "counterexampleRunCount", Integer.toString(counterexampleRunCount));
            row(out, "regexMatrixRoot", regexMatrixRoot);
            row(out, "regexOracleRoot", regexOracleRoot);
            row(out, "regexCaseCount", Integer.toString(regexCaseCount));
            row(out, "jniRequired", Boolean.toString(jniRequired));
            row(out, "nativeParityRoot", nativeParityRoot.isEmpty() ? "-" : nativeParityRoot);
            row(out, "nativeParityPairCount", Integer.toString(nativeParityPairCount));
            row(out, "complete", "true");
            row(out, "sourceMutationAuthority", "false");
            row(out, "semanticAuthority", "false");
            row(out, "donorSourceCopyAuthority", "false");
            row(out, "replacementAuthority", "false");
            row(out, "mergeAuthority", "false");
            row(out, "promotionAuthority", "false");
            return out.toString();
        }
    }

    private A3Mastery() {}

    public static Receipt read(Path repositoryRoot, Path receiptPath, String expectedRoot)
            throws IOException {
        Path root = A3Fs.root(repositoryRoot);
        Path requested = Objects.requireNonNull(receiptPath, "receiptPath");
        Path file =
                (requested.isAbsolute() ? requested : root.resolve(requested))
                        .toAbsolutePath()
                        .normalize();
        if (Files.isSymbolicLink(file)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("A3 mastery receipt must be a regular non-symlink file");
        }
        Receipt receipt = parse(Files.readString(file, StandardCharsets.UTF_8));
        String pinned = sha(expectedRoot, "expectedRoot");
        if (!receipt.root().equals(pinned)) {
            throw new IllegalStateException("A3 mastery receipt root mismatch");
        }
        return receipt;
    }

    public static Receipt parse(String tsv) {
        String[] lines = Objects.requireNonNull(tsv, "tsv").split("\\R", -1);
        if (lines.length < 2 || !"key\tvalue".equals(lines[0])) {
            throw new IllegalArgumentException("V6 mastery TSV header");
        }

        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isEmpty()) continue;
            String[] cells = lines[index].split("\\t", -1);
            if (cells.length != 2 || values.putIfAbsent(cells[0], cells[1]) != null) {
                throw new IllegalArgumentException("V6 mastery TSV row");
            }
        }

        List<String> keys =
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
        if (!values.keySet().equals(new LinkedHashSet<>(keys))
                || !SCHEMA.equals(values.get("schema"))
                || !"true".equals(values.get("complete"))
                || !authorityFalse(values, "sourceMutationAuthority")
                || !authorityFalse(values, "semanticAuthority")
                || !authorityFalse(values, "donorSourceCopyAuthority")
                || !authorityFalse(values, "replacementAuthority")
                || !authorityFalse(values, "mergeAuthority")
                || !authorityFalse(values, "promotionAuthority")) {
            throw new IllegalArgumentException("V6 mastery TSV schema");
        }

        boolean jni = strictBoolean(values.get("jniRequired"), "jniRequired");
        String nativeRoot =
                "-".equals(values.get("nativeParityRoot"))
                        ? ""
                        : values.get("nativeParityRoot");
        return new Receipt(
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
                integer(values.get("challengeDonorLedgerRows"), "challengeDonorLedgerRows"),
                integer(values.get("counterexampleFixtureCount"), "counterexampleFixtureCount"),
                integer(values.get("counterexampleRunCount"), "counterexampleRunCount"),
                values.get("regexMatrixRoot"),
                values.get("regexOracleRoot"),
                integer(values.get("regexCaseCount"), "regexCaseCount"),
                jni,
                nativeRoot,
                integer(values.get("nativeParityPairCount"), "nativeParityPairCount"),
                true,
                values.get("root"));
    }

    public static void writeBinding(Path repositoryRoot, Path out, Receipt receipt)
            throws IOException {
        Receipt checked = Objects.requireNonNull(receipt, "receipt");
        String body =
                "schema\troot\n"
                        + SCHEMA
                        + "\t"
                        + checked.root()
                        + "\n";
        A3Fs.write(repositoryRoot, out.resolve("mastery.tsv"), body);
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

    private static String token(String value, String field) {
        String checked = Objects.toString(value, "").strip().toUpperCase(Locale.ROOT);
        if (!checked.matches("[A-Z][A-Z0-9_]{0,63}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static void row(StringBuilder out, String key, String value) {
        String checkedKey = Objects.requireNonNull(key, "key");
        String checkedValue = Objects.requireNonNull(value, checkedKey);
        if (checkedKey.indexOf('\t') >= 0
                || checkedValue.indexOf('\t') >= 0
                || checkedValue.indexOf('\n') >= 0
                || checkedValue.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("V6 mastery TSV cell");
        }
        out.append(checkedKey).append('\t').append(checkedValue).append('\n');
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
