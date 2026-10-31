// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.nio.file.Files;
import java.nio.file.Path;

/** Command-line verifier/materializer for a Synexia delivery export. */
public final class M3ImportCli {
    private M3ImportCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: M3ImportCli <verify|verify-strict|verify-target|materialize|materialize-strict> <manifest.tsv> <m3-root> <m3jdk-root>");
        }
        String action = args[0];
        M3ImportManifest manifest =
                M3ImportManifest.parse(Files.readString(Path.of(args[1])));
        Path synexiaRoot = Path.of(args[2]);
        Path m3jdkRoot = Path.of(args[3]);
        switch (action) {
            case "verify" -> M3Importer.verify(synexiaRoot, m3jdkRoot, manifest);
            case "verify-strict" -> {
                M3GitCheckout.requireExactCleanHead(
                        synexiaRoot,
                        manifest.sourceRevision(),
                        manifest.entries().stream()
                                .map(M3ImportManifest.Entry::sourcePath)
                                .toList());
                M3Importer.verify(synexiaRoot, m3jdkRoot, manifest);
            }
            case "verify-target" -> M3Importer.verifyTargetSnapshot(m3jdkRoot, manifest);
            case "materialize" -> M3Importer.materialize(synexiaRoot, m3jdkRoot, manifest);
            case "materialize-strict" -> {
                M3GitCheckout.requireExactCleanHead(
                        synexiaRoot, manifest.sourceRevision());
                M3Importer.materialize(synexiaRoot, m3jdkRoot, manifest);
            }
            default -> throw new IllegalArgumentException("unknown action: " + action);
        }
        System.out.println(manifest.root());
    }
}
