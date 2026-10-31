// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Frozen empty-ledger, timestamp and lossless-encoding admission regressions. */
public final class M3RecipeMemoryAdmissionContract {
    private static final String RECIPE = "com.synexia.m3jdk.AstBuildReverse20261017";
    private static final String LEDGER = "recipes/memory/RECIPE_MEMORY.tsv";
    private static int checks;

    private M3RecipeMemoryAdmissionContract() { }

    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Checked action, String message) throws Exception {
        checks++;
        try { action.run(); }
        catch (IOException expected) { return; }
        throw new AssertionError(message);
    }

    private static final class Fixture implements AutoCloseable {
        final Path root = Files.createTempDirectory("m3-memory-admission-");
        final Path ledger = root.resolve(LEDGER);
        final Path patch = root.resolve("change.diff");
        final Path guard = root.resolve("Regression.java");
        final Path proof = root.resolve("proof.tsv");

        Fixture() throws IOException {
            Files.writeString(patch, "-before\n+after\n", StandardCharsets.UTF_8);
            Files.writeString(guard, "class Regression {}\n", StandardCharsets.UTF_8);
            refreshProof();
        }

        void refreshProof() throws IOException {
            Files.writeString(proof, "M3_RECIPE_PROOF_V1\t" + RECIPE + "\t"
                    + M3RecipeMemory.sha256(patch) + "\t" + M3RecipeMemory.sha256(guard)
                    + "\ncompile\tPASS\ntest\tPASS\nfixed_point\tPASS\n", StandardCharsets.UTF_8);
        }

        M3RecipeMemory.Entry row(Instant time, String text, int field) throws IOException {
            return new M3RecipeMemory.Entry(time, RECIPE,
                    field == 0 ? text : "trigger", field == 1 ? text : "observed failure",
                    M3RecipeMemory.correction(root, "change.diff"),
                    field == 2 ? text : "coding-agent", "Regression.java",
                    field == 3 ? text : "Retain exact correction evidence");
        }

        M3RecipeMemory.Entry row(Instant time) throws IOException { return row(time, "unused", -1); }

        void header() throws IOException {
            Files.createDirectories(ledger.getParent());
            Files.writeString(ledger, M3RecipeMemory.HEADER, StandardCharsets.UTF_8);
        }

        @Override public void close() throws IOException {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private static void headerOnly() throws Exception {
        try (Fixture f = new Fixture()) {
            f.header();
            check(M3RecipeMemory.read(f.ledger).isEmpty(), "exact header is an empty ledger");
            byte[] before = Files.readAllBytes(f.ledger);
            var entry = f.row(Instant.EPOCH);
            M3RecipeMemory.append(f.root, LEDGER, entry, f.proof);
            check(M3RecipeMemory.read(f.ledger).equals(List.of(entry)), "header accepts first epoch row");
            M3RecipeMemory.verifyAppendOnly(before, Files.readAllBytes(f.ledger));
            check(Files.readString(f.ledger).equals(M3RecipeMemory.HEADER + entry.line()),
                    "header has exactly one retained newline");
            Files.writeString(f.ledger, M3RecipeMemory.HEADER + "\n", StandardCharsets.UTF_8);
            rejects(() -> M3RecipeMemory.read(f.ledger), "additional blank row must refuse");
            Files.writeString(f.ledger, M3RecipeMemory.HEADER.stripTrailing(), StandardCharsets.UTF_8);
            rejects(() -> M3RecipeMemory.read(f.ledger), "header without final newline must refuse");
        }
    }

    private static void timestamp(boolean existingHeader) throws Exception {
        for (Instant invalid : List.of(Instant.EPOCH.minusNanos(1), Instant.MIN)) {
            try (Fixture f = new Fixture()) {
                if (existingHeader) f.header();
                byte[] before = existingHeader ? Files.readAllBytes(f.ledger) : null;
                rejects(() -> M3RecipeMemory.append(f.root, LEDGER, f.row(invalid), f.proof),
                        "pre-epoch row must refuse");
                if (existingHeader) {
                    check(Arrays.equals(before, Files.readAllBytes(f.ledger)),
                            "pre-epoch refusal must leave header byte-identical");
                } else {
                    check(!Files.exists(f.ledger), "pre-epoch refusal must not publish a ledger");
                    check(!Files.exists(f.ledger.getParent()),
                            "pre-epoch refusal must not create destination directories");
                }
                check(!Files.exists(f.ledger.resolveSibling("RECIPE_MEMORY.tsv.lock")),
                        "pre-epoch refusal must precede lock creation");
                var entry = f.row(Instant.EPOCH);
                M3RecipeMemory.append(f.root, LEDGER, entry, f.proof);
                check(M3RecipeMemory.read(f.ledger).equals(List.of(entry)),
                        "valid append recovers after refused first row");
            }
        }
    }

    private static void malformedUtf16() throws Exception {
        for (String malformed : List.of("\uD800", "\uDC00", "x\uD800y", "\uD800\uD800")) {
            for (int field = 0; field < 4; field++) {
                for (boolean existingHeader : List.of(false, true)) {
                    try (Fixture f = new Fixture()) {
                        if (existingHeader) f.header();
                        var entry = f.row(Instant.EPOCH, malformed, field);
                        byte[] before = existingHeader ? Files.readAllBytes(f.ledger) : null;
                        rejects(() -> M3RecipeMemory.append(f.root, LEDGER, entry, f.proof),
                                "malformed UTF-16 must refuse instead of replacing recorded text");
                        if (existingHeader) {
                            check(Arrays.equals(before, Files.readAllBytes(f.ledger)),
                                    "encoding refusal preserves exact previous bytes");
                        } else {
                            check(!Files.exists(f.ledger.getParent()),
                                    "encoding refusal precedes destination creation");
                        }
                        check(!Files.exists(f.ledger.resolveSibling("RECIPE_MEMORY.tsv.lock")),
                                "encoding refusal precedes lock creation");
                    }
                }
            }
        }
    }

    private static void validUnicodeAndEqualTimes() throws Exception {
        for (String text : List.of("ASCII", "\u00E9", "\u0938\u0941\u0927\u093E\u0930", "\uD83D\uDE80", "a\uD800\uDC00z")) {
            for (int field = 0; field < 4; field++) {
                try (Fixture f = new Fixture()) {
                    var first = f.row(Instant.EPOCH, text, field);
                    M3RecipeMemory.append(f.root, LEDGER, first, f.proof);
                    check(M3RecipeMemory.read(f.ledger).equals(List.of(first)),
                            "successful append retains exact supplied Unicode entry");
                    check(Files.readString(f.ledger).equals(M3RecipeMemory.HEADER + first.line()),
                            "persisted UTF-8 round-trip is exact");
                    byte[] before = Files.readAllBytes(f.ledger);
                    Files.writeString(f.patch, "-after\n+next\n", StandardCharsets.UTF_8);
                    f.refreshProof();
                    var second = f.row(Instant.EPOCH, text, field);
                    M3RecipeMemory.append(f.root, LEDGER, second, f.proof);
                    check(M3RecipeMemory.read(f.ledger).equals(List.of(first, second)),
                            "equal timestamp distinct correction remains valid");
                    M3RecipeMemory.verifyAppendOnly(before, Files.readAllBytes(f.ledger));
                    Files.writeString(f.patch, "-next\n+third\n", StandardCharsets.UTF_8);
                    f.refreshProof();
                    byte[] twoRows = Files.readAllBytes(f.ledger);
                    rejects(() -> M3RecipeMemory.append(f.root, LEDGER,
                            f.row(Instant.EPOCH.minusNanos(1)), f.proof), "backdated nonempty append refuses");
                    check(Arrays.equals(twoRows, Files.readAllBytes(f.ledger)),
                            "existing valid rows survive rejected append");
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        List<String> cases = args.length == 0
                ? List.of("header", "timestamp-absent", "timestamp-header", "utf16", "compatible")
                : List.of(args);
        int failures = 0;
        for (String test : cases) {
            try {
                switch (test) {
                    case "header" -> headerOnly();
                    case "timestamp-absent" -> timestamp(false);
                    case "timestamp-header" -> timestamp(true);
                    case "utf16" -> malformedUtf16();
                    case "compatible" -> validUnicodeAndEqualTimes();
                    default -> throw new IllegalArgumentException("unknown test: " + test);
                }
                System.out.println("M3_MEMORY_ADMISSION_CASE_PASS " + test);
            } catch (Exception | AssertionError failure) {
                failures++;
                System.err.println("M3_MEMORY_ADMISSION_CASE_FAIL " + test + " " + failure);
                failure.printStackTrace(System.err);
            }
        }
        System.out.println("M3_MEMORY_ADMISSION_RESULT checks=" + checks + " failures=" + failures);
        if (failures != 0) throw new AssertionError("memory admission cases failed: " + failures);
    }
}
