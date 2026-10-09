// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.synexia.rewrite.M3RecipeMasteryPortableReceipt;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CLI for authority-free A3 preparation of exact backport target paths.
 *
 * <p>The portable Synexia V6 mastery receipt is verified before any Java candidate is produced.
 * Preparation writes only beneath the explicit output root; the OpenJDK source tree is unchanged.</p>
 */
public final class M3A3BackportPreparationMain {
    private M3A3BackportPreparationMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "usage: M3A3BackportPreparationMain "
                            + "<repository-root> <output-root> <paths-file> "
                            + "<mastery-receipt.tsv> <mastery-root-file>");
        }

        Path repository = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        Path pathsFile = Path.of(args[2]).toAbsolutePath().normalize();
        Path masteryReceipt = Path.of(args[3]).toAbsolutePath().normalize();
        Path masteryRootFile = Path.of(args[4]).toAbsolutePath().normalize();

        requireFile(pathsFile, "paths");
        requireFile(masteryReceipt, "mastery receipt");
        requireFile(masteryRootFile, "mastery root");

        List<String> paths =
                Files.readAllLines(pathsFile).stream()
                        .map(String::strip)
                        .filter(value -> !value.isEmpty() && !value.startsWith("#"))
                        .toList();
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("paths file contains no targets");
        }

        String tsv = Files.readString(masteryReceipt);
        String expectedRoot = masteryRoot(Files.readString(masteryRootFile));
        M3RecipeMasteryPortableReceipt.Verified mastery =
                M3RecipeMasteryPortableReceipt.verify(tsv, expectedRoot);

        M3A3BackportPreparation.Receipt receipt =
                M3A3BackportPreparation.prepare(repository, output, paths, mastery);

        System.out.println(
                "M3_A3_BACKPORT_PREPARATION_OK"
                        + " targets="
                        + receipt.rows().size()
                        + " java="
                        + receipt.javaFiles()
                        + " nonJava="
                        + receipt.nonJavaFiles()
                        + " masteryRoot="
                        + mastery.root());
    }

    static String masteryRoot(String text) {
        String checked = text == null ? "" : text.strip();
        if (!checked.matches("ROOT  [0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid mastery root file");
        }
        return checked.substring("ROOT  ".length());
    }

    private static void requireFile(Path path, String role) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(role + " file not found: " + path);
        }
    }
}
