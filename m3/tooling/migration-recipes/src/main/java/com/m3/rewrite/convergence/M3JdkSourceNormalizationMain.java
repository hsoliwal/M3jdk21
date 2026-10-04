// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.convergence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Typed wrapper around the canonical FILE convergence engine.
 *
 * <p>The same OpenRewrite atomize -> patternize/IOP -> documentation -> fixed-point DAG is used for
 * the Java-21 baseline and for newer-JDK donor checkouts. Role and exact revision are persisted in
 * a separate identity receipt so baseline and donor evidence cannot be mixed accidentally.</p>
 */
public final class M3JdkSourceNormalizationMain {
    static final String IDENTITY = "SOURCE_CONVERGENCE.image.tsv";

    private M3JdkSourceNormalizationMain() {}

    public enum ImageRole {
        BASELINE,
        DONOR
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5) {
            throw new IllegalArgumentException(
                    "usage: <BASELINE|DONOR> <revision> <jdk-root> <output-dir> [threads]");
        }
        ImageRole role = ImageRole.valueOf(args[0].strip().toUpperCase(java.util.Locale.ROOT));
        String revision = token(args[1], "revision");
        Path root = Path.of(args[2]).toAbsolutePath().normalize();
        Path output = Path.of(args[3]).toAbsolutePath().normalize();
        int threads = args.length == 5
                ? positive(Integer.parseInt(args[4]), "threads")
                : Math.max(1, Runtime.getRuntime().availableProcessors());

        var summary = normalize(role, revision, root, output, threads);
        System.out.println(
                "M3JDK source normalization: role=" + role
                        + " revision=" + revision
                        + " files=" + summary.files()
                        + " changed=" + summary.changed()
                        + " holds=" + summary.holds()
                        + " root=" + summary.root());
    }

    public static M3Jdk21SourceConvergenceMain.RunSummary normalize(
            ImageRole role,
            String revision,
            Path repositoryRoot,
            Path outputDirectory,
            int threads)
            throws IOException, InterruptedException {
        Objects.requireNonNull(role, "role");
        String checkedRevision = token(revision, "revision");
        Path output = Objects.requireNonNull(outputDirectory, "outputDirectory")
                .toAbsolutePath()
                .normalize();

        var summary = M3Jdk21SourceConvergenceMain.convergeTree(
                repositoryRoot,
                output,
                threads);

        Files.createDirectories(output);
        String identity =
                "role\trevision\tsemanticRoot\tfiles\tchanged\tholds\n"
                        + role.name()
                        + "\t"
                        + tsv(checkedRevision)
                        + "\t"
                        + summary.root()
                        + "\t"
                        + summary.files()
                        + "\t"
                        + summary.changed()
                        + "\t"
                        + summary.holds()
                        + "\n";
        Files.writeString(output.resolve(IDENTITY), identity, StandardCharsets.UTF_8);
        return summary;
    }

    private static int positive(int value, String field) {
        if (value <= 0) throw new IllegalArgumentException(field);
        return value;
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String tsv(String value) {
        return token(value, "value");
    }
}
