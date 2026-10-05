// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

/** Executes an already-imported Synexia handoff crate through the retained OpenRewrite receivers. */
public final class SynexiaHandoffMaterializeCli {
    private SynexiaHandoffMaterializeCli() {}

    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != 0) {
            throw new IllegalStateException(
                    "Synexia handoff materialization failed with status " + status);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        if (args.length != 3
                || !("--check".equals(args[0]) || "--apply".equals(args[0]))) {
            err.println(
                    "Usage: SynexiaHandoffMaterializeCli --check|--apply "
                            + "M3JDK21_ROOT synexia-crate");
            return 2;
        }
        try {
            var receipt =
                    "--apply".equals(args[0])
                            ? SynexiaHandoffMaterializer.apply(Path.of(args[1]), args[2])
                            : SynexiaHandoffMaterializer.inspect(Path.of(args[1]), args[2]);
            out.println(
                    "SYNEXIA_HANDOFF_MATERIALIZE_V1"
                            + "\tmode="
                            + args[0].substring(2)
                            + "\tcrate="
                            + receipt.crateName()
                            + "\tsourceRevision="
                            + receipt.sourceRevision()
                            + "\tpacketRoot="
                            + receipt.packetRoot()
                            + "\ttargets="
                            + receipt.targets()
                            + "\tchanged="
                            + receipt.changedTargets());
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
