// SPDX-License-Identifier: Apache-2.0
package jdk.m3.importer;

import java.nio.file.Files;
import java.nio.file.Path;

/** CLI for full Synexia delivery planning/staging without vendor-tree writes. */
public final class ImportPlanCli {
    private ImportPlanCli() {}

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
                    "usage: ImportPlanCli plan <manifest.tsv> <m3jdk-root> <out>");
        }
        ImportManifest manifest =
                ImportManifest.parse(Files.readString(Path.of(args[1])));
        Path root = Path.of(args[2]);
        ImportPlan.Plan plan = ImportPlan.plan(root, manifest);
        ImportPlan.writePlan(root, Path.of(args[3]), plan);
        System.out.println(plan.root());
    }

    private static void stage(String[] args, boolean strict) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "usage: ImportPlanCli "
                            + (strict ? "stage-strict" : "stage")
                            + " <manifest.tsv> <m3-root> <m3jdk-root> <out>");
        }
        ImportManifest manifest =
                ImportManifest.parse(Files.readString(Path.of(args[1])));
        Path synexiaRoot = Path.of(args[2]);
        if (strict) {
            GitCheckout.requireExactCleanHead(
                    synexiaRoot,
                    manifest.sourceRevision(),
                    manifest.entries().stream()
                            .map(ImportManifest.Entry::sourcePath)
                            .toList());
        }
        ImportPlan.Plan plan =
                ImportPlan.stage(
                        synexiaRoot,
                        Path.of(args[3]),
                        manifest,
                        Path.of(args[4]));
        System.out.println(plan.root());
    }
}
