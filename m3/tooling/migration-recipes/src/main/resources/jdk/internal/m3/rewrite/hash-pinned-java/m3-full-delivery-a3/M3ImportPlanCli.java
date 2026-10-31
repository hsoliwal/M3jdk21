// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.nio.file.Files;
import java.nio.file.Path;

/** CLI for full Synexia delivery planning/staging without vendor-tree writes. */
public final class M3ImportPlanCli {
    private M3ImportPlanCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("missing Synexia import-plan action");
        }
        switch (args[0]) {
            case "plan" -> plan(args);
            case "stage" -> stage(args);
            default -> throw new IllegalArgumentException(
                    "unknown Synexia import-plan action: " + args[0]);
        }
    }

    private static void plan(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: M3ImportPlanCli plan <manifest.tsv> <m3jdk-root> <out>");
        }
        M3ImportManifest manifest =
                M3ImportManifest.parse(Files.readString(Path.of(args[1])));
        Path root = Path.of(args[2]);
        M3ImportPlan.Plan plan = M3ImportPlan.plan(root, manifest);
        M3ImportPlan.writePlan(root, Path.of(args[3]), plan);
        System.out.println(plan.root());
    }

    private static void stage(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "usage: M3ImportPlanCli stage <manifest.tsv> <m3-root> "
                            + "<m3jdk-root> <out>");
        }
        M3ImportManifest manifest =
                M3ImportManifest.parse(Files.readString(Path.of(args[1])));
        M3ImportPlan.Plan plan =
                M3ImportPlan.stage(
                        Path.of(args[2]),
                        Path.of(args[3]),
                        manifest,
                        Path.of(args[4]));
        System.out.println(plan.root());
    }
}
