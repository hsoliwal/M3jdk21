// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.nio.file.Files;
import java.nio.file.Path;

/** CLI for full Synexia delivery planning/staging without vendor-tree writes. */
public final class SynexiaImportPlanCli {
    private SynexiaImportPlanCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("missing Synexia import-plan action");
        }
        switch (args[0]) {
            case "plan" -> plan(args);
            case "stage" -> stage(args, false);
            case "stage-strict" -> stage(args, true);
            default -> throw new IllegalArgumentException(
                    "unknown Synexia import-plan action: " + args[0]);
        }
    }

    private static void plan(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: SynexiaImportPlanCli plan <manifest.tsv> <m3jdk-root> <out>");
        }
        SynexiaImportManifest manifest =
                SynexiaImportManifest.parse(Files.readString(Path.of(args[1])));
        Path root = Path.of(args[2]);
        SynexiaImportPlan.Plan plan = SynexiaImportPlan.plan(root, manifest);
        SynexiaImportPlan.writePlan(root, Path.of(args[3]), plan);
        System.out.println(plan.root());
    }

    private static void stage(String[] args, boolean strict) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "usage: SynexiaImportPlanCli "
                            + (strict ? "stage-strict" : "stage")
                            + " <manifest.tsv> <synexia-root> <m3jdk-root> <out>");
        }
        SynexiaImportManifest manifest =
                SynexiaImportManifest.parse(Files.readString(Path.of(args[1])));
        Path synexiaRoot = Path.of(args[2]);
        if (strict) {
            SynexiaGitCheckout.requireExactCleanHead(
                    synexiaRoot,
                    manifest.sourceRevision(),
                    manifest.entries().stream()
                            .map(SynexiaImportManifest.Entry::sourcePath)
                            .toList());
        }
        SynexiaImportPlan.Plan plan =
                SynexiaImportPlan.stage(
                        synexiaRoot,
                        Path.of(args[3]),
                        manifest,
                        Path.of(args[4]));
        System.out.println(plan.root());
    }
}
