// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

/** Command-line front end for guarded Synexia handoff resource import. */
public final class SynexiaHandoffImportCli {
    private SynexiaHandoffImportCli() {}

    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != 0) {
            throw new IllegalStateException(
                    "Synexia handoff import failed with status " + status);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        if (args.length != 4
                || !("--check".equals(args[0]) || "--apply".equals(args[0]))) {
            err.println(
                    "Usage: SynexiaHandoffImportCli --check|--apply "
                            + "EXPORT_ROOT M3JDK21_ROOT synexia-crate");
            return 2;
        }
        try {
            var receipt =
                    "--apply".equals(args[0])
                            ? SynexiaHandoffImport.apply(
                                    Path.of(args[1]), Path.of(args[2]), args[3])
                            : SynexiaHandoffImport.inspect(
                                    Path.of(args[1]), Path.of(args[2]), args[3]);
            out.println(
                    "SYNEXIA_HANDOFF_IMPORT_V1"
                            + "\tmode="
                            + args[0].substring(2)
                            + "\tcrate="
                            + receipt.crateName()
                            + "\tsourceRevision="
                            + receipt.sourceRevision()
                            + "\tpacketRoot="
                            + receipt.packetRoot()
                            + "\tresources="
                            + receipt.resourceFiles()
                            + "\tchanged="
                            + receipt.changedFiles());
            return 0;
        } catch (RuntimeException failure) {
            err.println(
                    failure.getClass().getSimpleName()
                            + ": "
                            + Objects.toString(failure.getMessage(), ""));
            return 1;
        }
    }
}
