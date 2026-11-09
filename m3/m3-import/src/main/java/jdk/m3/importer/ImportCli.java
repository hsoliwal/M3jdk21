// SPDX-License-Identifier: Apache-2.0
package jdk.m3.importer;

import java.nio.file.Files;
import java.nio.file.Path;

/** Command-line verifier/materializer for a Synexia delivery export. */
public final class ImportCli {
    private ImportCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: ImportCli <verify|verify-strict|verify-target|materialize|materialize-strict> <manifest.tsv> <m3-root> <m3jdk-root>");
        }
        String action = args[0];
        ImportManifest manifest =
                ImportManifest.parse(Files.readString(Path.of(args[1])));
        Path synexiaRoot = Path.of(args[2]);
        Path m3jdkRoot = Path.of(args[3]);
        switch (action) {
            case "verify" -> Importer.verify(synexiaRoot, m3jdkRoot, manifest);
            case "verify-strict" -> {
                GitCheckout.requireExactCleanHead(
                        synexiaRoot,
                        manifest.sourceRevision(),
                        manifest.entries().stream()
                                .map(ImportManifest.Entry::sourcePath)
                                .toList());
                Importer.verify(synexiaRoot, m3jdkRoot, manifest);
            }
            case "verify-target" -> Importer.verifyTargetSnapshot(m3jdkRoot, manifest);
            case "materialize" -> Importer.materialize(synexiaRoot, m3jdkRoot, manifest);
            case "materialize-strict" -> {
                GitCheckout.requireExactCleanHead(
                        synexiaRoot, manifest.sourceRevision());
                Importer.materialize(synexiaRoot, m3jdkRoot, manifest);
            }
            default -> throw new IllegalArgumentException("unknown action: " + action);
        }
        System.out.println(manifest.root());
    }
}
