// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.nio.file.Files;
import java.nio.file.Path;

/** Command-line verifier/materializer for a Synexia delivery export. */
public final class SynexiaImportCli {
    private SynexiaImportCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: SynexiaImportCli <verify|verify-target|materialize> <manifest.tsv> <synexia-root> <m3jdk-root>");
        }
        String action = args[0];
        SynexiaImportManifest manifest =
                SynexiaImportManifest.parse(Files.readString(Path.of(args[1])));
        Path synexiaRoot = Path.of(args[2]);
        Path m3jdkRoot = Path.of(args[3]);
        switch (action) {
            case "verify" -> SynexiaImporter.verify(synexiaRoot, m3jdkRoot, manifest);
            case "verify-target" -> SynexiaImporter.verifyTargetSnapshot(m3jdkRoot, manifest);
            case "materialize" -> SynexiaImporter.materialize(synexiaRoot, m3jdkRoot, manifest);
            default -> throw new IllegalArgumentException("unknown action: " + action);
        }
        System.out.println(manifest.root());
    }
}
