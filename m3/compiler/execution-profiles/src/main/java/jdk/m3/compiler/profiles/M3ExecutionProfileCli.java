/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.compiler.profiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Command-line validation and deterministic merge for M3 execution-profile bundles. */
public final class M3ExecutionProfileCli {
    private M3ExecutionProfileCli() {
    }

    /**
     * CLI entry point.
     *
     * @param args {@code validate <bundle>} or {@code merge <output> <input>...}
     * @throws IOException when a bundle cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: validate <bundle> | merge <output> <input>...");
        }
        switch (args[0]) {
            case "validate" -> {
                if (args.length != 2) {
                    throw new IllegalArgumentException("validate requires exactly one bundle directory");
                }
                print("VALIDATE", M3ExecutionProfileBundle.read(Path.of(args[1])));
            }
            case "merge" -> {
                if (args.length < 3) {
                    throw new IllegalArgumentException("merge requires an output and at least one input directory");
                }
                List<Path> inputs = new ArrayList<>();
                for (int index = 2; index < args.length; index++) {
                    inputs.add(Path.of(args[index]));
                }
                print("MERGE", M3ExecutionProfileMerger.mergeDirectories(Path.of(args[1]), inputs));
            }
            default -> throw new IllegalArgumentException("command must be validate or merge");
        }
    }

    private static void print(String mode, M3ExecutionProfileBundle bundle) {
        System.out.printf(Locale.ROOT,
                "M3_EXECUTION_PROFILE\tmode=%s\tbundle_id=%s\tobservations=%d\truntime_commit=%s"
                        + "\tworkload_sha256=%s\tcapture_target=%s\tdeployment_target=%s%n",
                mode,
                bundle.metadata().bundleId(),
                bundle.observations().size(),
                bundle.metadata().runtimeCommit(),
                bundle.metadata().workloadSha256(),
                bundle.metadata().captureTarget(),
                bundle.metadata().deploymentTarget());
    }
}
