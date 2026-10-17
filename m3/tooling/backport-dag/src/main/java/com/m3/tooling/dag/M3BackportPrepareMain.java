// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.m3.a3.A3Mastery;
import com.synexia.rewrite.M3RecipeMasteryPortableReceipt;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * CLI boundary for the existing A3 backport-preparation gate.
 *
 * <p>The path file is evidence only: one repository-relative target per line, with blank and
 * comment lines ignored. Java targets execute A3 to fixed point; non-Java targets are classified
 * into the existing source-sealed lane. This command never writes canonical source files.</p>
 */
public final class M3BackportPrepareMain {
    private M3BackportPrepareMain() {}

    public static void main(String[] args) throws Exception {
        ReceiptSummary summary = run(args);
        System.out.println(
                "M3_A3_BACKPORT_PREPARATION_OK targets="
                        + summary.targets()
                        + " java="
                        + summary.javaFiles()
                        + " nonJava="
                        + summary.nonJavaFiles()
                        + " changedJava="
                        + summary.changedJava()
                        + " fixedPoint=true sourcePromotionAuthority=false");
    }

    static ReceiptSummary run(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "usage: M3BackportPrepareMain <repository-root> <output-root> <paths-file>"
                            + " <mastery-receipt> <mastery-root-sha256>");
        }
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        // The same Synexia canonical verifier and path guards used by the A3 CLI.
        // Never synthesize a Verified record from command-line options.
        M3RecipeMasteryPortableReceipt.Verified mastery =
                A3Mastery.verify(root, Path.of(args[3]), args[4]);
        return run(root, Path.of(args[1]), Path.of(args[2]), mastery);
    }

    static ReceiptSummary run(
            Path root, Path output, Path pathsFile,
            M3RecipeMasteryPortableReceipt.Verified mastery) throws Exception {
        Objects.requireNonNull(mastery, "mastery");
        root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        output = Objects.requireNonNull(output, "output").toAbsolutePath().normalize();
        pathsFile = Objects.requireNonNull(pathsFile, "pathsFile").toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("repository root not found: " + root);
        }
        if (!Files.isRegularFile(pathsFile)) {
            throw new IllegalArgumentException("paths file not found: " + pathsFile);
        }

        List<String> targets =
                Files.readAllLines(pathsFile).stream()
                        .map(String::strip)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList();
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("paths file contains no targets: " + pathsFile);
        }

        M3A3BackportPreparation.Receipt receipt =
                M3A3BackportPreparation.prepare(root, output, targets, mastery);
        long changedJava =
                receipt.rows().stream()
                        .filter(
                                row ->
                                        row.lane()
                                                        == M3A3BackportPreparation.Lane
                                                                .JAVA_A3_FIXED_POINT
                                                && row.changed())
                        .count();
        return new ReceiptSummary(
                receipt.rows().size(),
                receipt.javaFiles(),
                receipt.nonJavaFiles(),
                Math.toIntExact(changedJava));
    }

    record ReceiptSummary(int targets, int javaFiles, int nonJavaFiles, int changedJava) {
        ReceiptSummary {
            if (targets <= 0
                    || javaFiles < 0
                    || nonJavaFiles < 0
                    || changedJava < 0
                    || javaFiles + nonJavaFiles != targets
                    || changedJava > javaFiles) {
                throw new IllegalArgumentException("invalid preparation summary");
            }
        }
    }
}
