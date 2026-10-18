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
import java.util.List;
import java.util.Objects;

/** Strict cross-repository admission for Synexia M3 recipe-mastery V6 evidence. */
public final class A3Gate {
    private static final String SCHEMA = "M3_RECIPE_MASTERY_FANIN_V6";
    private static final int MAX_RECEIPT_BYTES = 64 * 1024;
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
            String root) {

        public Receipt {
            crateRoot = sha(crateRoot, "crateRoot");
            masteryPlanRoot = sha(masteryPlanRoot, "masteryPlanRoot");
            masteryCompletionRoot = sha(masteryCompletionRoot, "masteryCompletionRoot");
            legacyExecutionRoot = sha(legacyExecutionRoot, "legacyExecutionRoot");
            fullWrapperExecutionRoot = sha(fullWrapperExecutionRoot, "fullWrapperExecutionRoot");
            schedulePlanRoot = sha(schedulePlanRoot, "schedulePlanRoot");
            scheduleCoverage = coverage(scheduleCoverage);
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
            if (challengeDonorLedgerRows < 3) {
                throw new IllegalArgumentException("challengeDonorLedgerRows");
            }
            if (counterexampleFixtureCount < 1
                    || counterexampleRunCount < counterexampleFixtureCount) {
                throw new IllegalArgumentException("counterexample counts");
            }
            regexMatrixRoot = sha(regexMatrixRoot, "regexMatrixRoot");
            regexOracleRoot = sha(regexOracleRoot, "regexOracleRoot");
            if (regexCaseCount != 10_000) {
                throw new IllegalArgumentException("regexCaseCount");
            }
            nativeParityRoot = Objects.toString(nativeParityRoot, "").strip();
            if (jniRequired) {
                nativeParityRoot = sha(nativeParityRoot, "nativeParityRoot");
                if (nativeParityPairCount < 1) {
                    throw new IllegalArgumentException("nativeParityPairCount");
                }
            } else if (!nativeParityRoot.isEmpty() || nativeParityPairCount != 0) {
                throw new IllegalArgumentException("unexpected JNI parity evidence");
            }
            String expected =
                    root(
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
                            challengeDonorLedgerRows,
                            counterexampleFixtureCount,
                            counterexampleRunCount,
                            regexMatrixRoot,
                            regexOracleRoot,
                            regexCaseCount,
                            jniRequired,
                            nativeParityRoot,
                            nativeParityPairCount);
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("V6 mastery root mismatch");
            }
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

        public boolean sourceMutationAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    public record Run(Receipt mastery, List<A3Apply.Receipt> candidates) {
        public Run {
            mastery = Objects.requireNonNull(mastery, "mastery");
            candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("empty mastered candidate set");
            }
        }
    }

    private A3Gate() {}

    public static Receipt admit(Path receiptFile, String expectedRoot) throws IOException {
        Path file =
                Objects.requireNonNull(receiptFile, "receiptFile")
                        .toAbsolutePath()
                        .normalize();
        if (Files.isSymbolicLink(file)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.size(file) > MAX_RECEIPT_BYTES) {
            throw new IOException("invalid A3 V6 mastery receipt file");
        }
        Receipt receipt = parse(Files.readString(file, StandardCharsets.UTF_8));
        String pinned = sha(expectedRoot, "expectedRoot");
        if (!receipt.root().equals(pinned)) {
            throw new IllegalStateException("A3 V6 mastery pinned root mismatch");
        }
        return receipt;
    }

    public static Run apply(
            Path root,
            Path out,
            List<String> sources,
            Path receiptFile,
            String expectedRoot)
            throws IOException {
        Receipt mastery = admit(receiptFile, expectedRoot);
        Path checkedRoot = A3Fs.root(root);
        Path checkedOut = A3Fs.out(checkedRoot, out);
        List<A3Apply.Receipt> candidates =
                A3Apply.run(checkedRoot, checkedOut, sources);
        A3Fs.write(
                checkedRoot,
                checkedOut.resolve("mastery-v6.tsv"),
                mastery.toTsv());
        return new Run(mastery, candidates);
    }

    public static Receipt parse(String tsv) {
        String[] lines = Objects.requireNonNull(tsv, "tsv").split("\\R", -1);
        if (lines.length < 2 || !"key\tvalue".equals(lines[0])) {
            throw new IllegalArgumentException("A3 V6 mastery TSV header");
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index];
            if (line.isEmpty()) continue;
            String[] cells = line.split("\\t", -1);
            if (cells.length != 2
                    || values.putIfAbsent(cells[0], cells[1]) != null) {
                throw new IllegalArgumentException("A3 V6 mastery TSV row");
            }
        }
        if (!List.copyOf(values.keySet()).equals(KEYS)
                || !SCHEMA.equals(values.get("schema"))
                || !"true".equals(values.get("complete"))) {
            throw new IllegalArgumentException("A3 V6 mastery TSV schema");
        }
        for (String authority :
                List.of(
                        "sourceMutationAuthority",
                        "semanticAuthority",
                        "donorSourceCopyAuthority",
                        "replacementAuthority",
                        "mergeAuthority",
                        "promotionAuthority")) {
            if (!"false".equals(values.get(authority))) {
                throw new IllegalArgumentException("A3 V6 mastery authority: " + authority);
            }
        }
        boolean jni = bool(values.get("jniRequired"), "jniRequired");
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
                values.get("root"));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            usage();
            throw new IllegalArgumentException("missing A3Gate command");
        }
        switch (args[0]) {
            case "verify" -> {
                if (args.length != 3) {
                    throw new IllegalArgumentException(
                            "A3Gate verify <receipt.tsv> <expected-root>");
                }
                Receipt receipt = admit(Path.of(args[1]), args[2]);
                System.out.println("masteryRoot=" + receipt.root());
            }
            case "apply" -> {
                if (args.length < 6) {
                    throw new IllegalArgumentException(
                            "A3Gate apply <jdk-root> <out> <receipt.tsv> "
                                    + "<expected-root> <source.java> [source.java ...]");
                }
                Run run =
                        apply(
                                Path.of(args[1]),
                                Path.of(args[2]),
                                List.of(args).subList(5, args.length),
                                Path.of(args[3]),
                                args[4]);
                System.out.println("masteryRoot=" + run.mastery().root());
                System.out.println("candidateCount=" + run.candidates().size());
            }
            default -> {
                usage();
                throw new IllegalArgumentException(
                        "unknown A3Gate command: " + args[0]);
            }
        }
    }

    private static void usage() {
        System.err.println(
                "A3Gate: verify <receipt.tsv> <expected-root> | "
                        + "apply <jdk-root> <out> <receipt.tsv> <expected-root> "
                        + "<source.java> [source.java ...]");
    }

    private static String root(
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
            int nativeParityPairCount) {
        return digest(
                "M3_RECIPE_MASTERY_FANIN_V6",
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
    }

    private static String coverage(String value) {
        String checked = text(value, "scheduleCoverage");
        if (!checked.equals("EXHAUSTIVE") && !checked.equals("ORDERED_PAIRWISE")) {
            throw new IllegalArgumentException("scheduleCoverage");
        }
        return checked;
    }

    private static boolean bool(String value, String field) {
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

    private static String sha(String value, String field) {
        String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
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

    private static void row(StringBuilder out, String key, String value) {
        if (key.indexOf('\t') >= 0
                || value.indexOf('\t') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("A3 V6 mastery TSV cell");
        }
        out.append(key).append('\t').append(value).append('\n');
    }
}
