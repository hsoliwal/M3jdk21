// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A3M = A3 mastery receipt gate.
 *
 * <p>A3M runs the bounded compiler/runtime/regex mastery laboratory once, binds its deterministic
 * result table to the exact atomizer, patternizer, documenter, convergence owner, fixture/compiler
 * owners and Maven dependency descriptors, and persists evidence under {@code m3/build}. Later A3
 * CLI absorption checks the receipt and source pins without rerunning the expensive laboratory for
 * every file.</p>
 */
final class A3M {
    static final String SCHEMA = "M3-A3-MASTERY/1";

    private static final List<String> PIN_PATHS =
            List.of(
                    "m3/tooling/a3/pom.xml",
                    "m3/tooling/a3/src/main/java/com/m3/a3/A3.java",
                    "m3/tooling/a3/src/main/java/com/m3/a3/A3Cases.java",
                    "m3/tooling/a3/src/main/java/com/m3/a3/A3Fs.java",
                    "m3/tooling/a3/src/main/java/com/m3/a3/A3Lab.java",
                    "m3/tooling/a3/src/main/java/com/m3/a3/A3M.java",
                    "m3/tooling/a3/src/main/java/com/m3/a3/A3MCodec.java",
                    "m3/tooling/a3/src/main/java/com/m3/a3/A3MemoryCompiler.java",
                    "m3/tooling/a3/src/main/java/com/m3/a3/A3RegexMatrix.java",
                    "m3/tooling/migration-recipes/pom.xml",
                    "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceCatalog.java",
                    "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/M3Java21ConvergenceRecipe.java",
                    "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/M3AtomizePureIntReturnRecipe.java",
                    "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/M3DocumentPureIntAtomRecipe.java",
                    "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/M3FileAtomCandidateTable.java",
                    "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/M3InventoryPureIntAtomCandidates.java",
                    "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/M3PatternizePureIntAtomRecipe.java",
                    "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/M3PureIntAtomEligibility.java",
                    "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-java21-convergence.yml");

    record Pin(String path, String sha256) {
        Pin {
            path = A3MCodec.path(path);
            sha256 = A3MCodec.sha(sha256, "sha256");
        }
    }

    record Receipt(
            String schema,
            String root,
            String pinRoot,
            String labTsvSha256,
            int fixtureCount,
            int scheduleCount,
            int resultCount,
            long applicationCount,
            long compileCount,
            int changedCount,
            int regexStableCount) {
        Receipt {
            schema = A3MCodec.text(schema, "schema");
            if (!SCHEMA.equals(schema)) {
                throw new IllegalArgumentException("schema");
            }
            pinRoot = A3MCodec.sha(pinRoot, "pinRoot");
            labTsvSha256 = A3MCodec.sha(labTsvSha256, "labTsvSha256");
            A3MCodec.requireCounts(
                    fixtureCount,
                    scheduleCount,
                    resultCount,
                    applicationCount,
                    compileCount,
                    changedCount,
                    regexStableCount);
            String expected =
                    A3MCodec.receiptRoot(
                            pinRoot,
                            labTsvSha256,
                            fixtureCount,
                            scheduleCount,
                            resultCount,
                            applicationCount,
                            compileCount,
                            changedCount,
                            regexStableCount);
            root = root == null || root.isBlank() ? expected : A3MCodec.sha(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("A3M receipt root mismatch");
            }
        }
    }

    private A3M() {}

    static Receipt write(Path root, Path out) throws Exception {
        Path checkedRoot = A3Fs.root(root);
        Path checkedOut = A3Fs.out(checkedRoot, out);
        A3Lab.write(checkedRoot, checkedOut.resolve("lab"));
        String labText =
                Files.readString(
                        checkedOut.resolve("lab/results.tsv"),
                        StandardCharsets.UTF_8);
        A3MCodec.LabSummary summary = A3MCodec.summarizeLab(labText);
        requireLiveDenominator(summary);
        List<Pin> pins = currentPins(checkedRoot);
        Receipt receipt =
                new Receipt(
                        SCHEMA,
                        "",
                        A3Fs.sha(renderPins(pins)),
                        A3Fs.sha(labText),
                        summary.fixtureCount(),
                        summary.scheduleCount(),
                        summary.resultCount(),
                        summary.applicationCount(),
                        summary.compileCount(),
                        summary.changedCount(),
                        summary.regexStableCount());

        A3Fs.write(checkedRoot, checkedOut.resolve("pins.tsv"), renderPins(pins));
        A3Fs.write(checkedRoot, checkedOut.resolve("receipt.tsv"), A3MCodec.render(receipt));
        return receipt;
    }

    static Receipt requireCurrent(Path root, Path receiptPath) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        Path receiptFile = A3Fs.source(checkedRoot, receiptPath);
        requireRegular(receiptFile, "receipt");

        Receipt receipt =
                A3MCodec.parse(
                        Files.readString(receiptFile, StandardCharsets.UTF_8));
        List<Pin> pins = currentPins(checkedRoot);
        if (!receipt.pinRoot().equals(A3Fs.sha(renderPins(pins)))) {
            throw new IllegalStateException("A3M source/dependency pins are stale");
        }

        Path evidenceRoot = receiptFile.getParent();
        requireEvidenceFile(
                checkedRoot,
                evidenceRoot.resolve("pins.tsv"),
                renderPins(pins),
                "pins");
        Path labFile = evidenceRoot.resolve("lab/results.tsv");
        requireRegular(labFile, "lab results");
        String labText = Files.readString(labFile, StandardCharsets.UTF_8);
        if (!receipt.labTsvSha256().equals(A3Fs.sha(labText))) {
            throw new IllegalStateException("A3M lab results drift");
        }
        A3MCodec.LabSummary summary = A3MCodec.summarizeLab(labText);
        requireLiveDenominator(summary);
        if (receipt.fixtureCount() != summary.fixtureCount()
                || receipt.scheduleCount() != summary.scheduleCount()
                || receipt.resultCount() != summary.resultCount()
                || receipt.applicationCount() != summary.applicationCount()
                || receipt.compileCount() != summary.compileCount()
                || receipt.changedCount() != summary.changedCount()
                || receipt.regexStableCount() != summary.regexStableCount()) {
            throw new IllegalStateException("A3M receipt/lab summary drift");
        }
        return receipt;
    }

    static List<String> pinPaths() {
        return PIN_PATHS;
    }

    static List<Pin> currentPins(Path root) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        ArrayList<Pin> pins = new ArrayList<>(PIN_PATHS.size());
        for (String relative : PIN_PATHS) {
            Path file = A3Fs.source(checkedRoot, Path.of(relative));
            requireRegular(file, "pin " + relative);
            pins.add(new Pin(relative, A3Fs.sha(Files.readAllBytes(file))));
        }
        return List.copyOf(pins);
    }

    static String renderPins(List<Pin> pins) {
        StringBuilder out = new StringBuilder("path\tsha256\n");
        for (Pin pin : Objects.requireNonNull(pins, "pins")) {
            out.append(A3Fs.cell(pin.path()))
                    .append('\t')
                    .append(pin.sha256())
                    .append('\n');
        }
        return out.toString();
    }

    private static void requireLiveDenominator(A3MCodec.LabSummary summary) {
        if (summary.fixtureCount() != A3Lab.fixtureCount()
                || summary.scheduleCount() != A3Lab.scheduleCount()
                || !summary.scheduleNames().equals(A3Lab.scheduleNames())
                || summary.resultCount()
                        != Math.multiplyExact(A3Lab.fixtureCount(), A3Lab.scheduleCount())) {
            throw new IllegalStateException("A3M lab denominator does not match live mastery");
        }
    }

    private static void requireEvidenceFile(
            Path root,
            Path file,
            String expected,
            String label) throws IOException {
        Path checked = A3Fs.source(root, file);
        requireRegular(checked, label);
        if (!Files.readString(checked, StandardCharsets.UTF_8).equals(expected)) {
            throw new IllegalStateException("A3M " + label + " evidence drift");
        }
    }

    private static void requireRegular(Path file, String label) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(file)) {
            throw new IOException("missing/non-regular A3M " + label + ": " + file);
        }
    }

}
