/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Complete training-run manifest and integrity verifier. */
public final class M3TrainingManifest {
    private static final Set<String> EXCLUDED = Set.of("training-manifest.tsv", "TRAINING_SHA256");

    private M3TrainingManifest() {
    }

    /** Seals training outputs and returns their content identity. */
    public static String seal(Path training) throws IOException {
        List<String> rows = M3Files.manifest(training, EXCLUDED);
        M3Files.writeUtf8(training.resolve("training-manifest.tsv"), String.join("", rows));
        String root = M3Hashing.hashRows(rows);
        M3Files.writeUtf8(training.resolve("TRAINING_SHA256"), root + "\n");
        return root;
    }

    /** Verifies all training outputs and rejects unmanifested regular files. */
    public static String verify(Path training) throws IOException {
        Path root = training.toAbsolutePath().normalize();
        List<String> rows = Files.readAllLines(root.resolve("training-manifest.tsv"), StandardCharsets.UTF_8)
                .stream().filter(line -> !line.isEmpty()).map(line -> line + "\n").toList();
        for (String row : rows) {
            String[] fields = row.substring(0, row.length() - 1).split("\t", -1);
            if (fields.length != 2 || !fields[0].matches("[0-9a-f]{64}")) {
                throw new IOException("Invalid training manifest row: " + row);
            }
            Path file = safeResolve(root, fields[1]);
            if (!Files.isRegularFile(file) || !M3Hashing.sha256(file).equals(fields[0])) {
                throw new IOException("Training file mismatch: " + fields[1]);
            }
        }
        List<String> actual = M3Files.manifest(root, EXCLUDED);
        if (!actual.equals(rows)) {
            throw new IOException("Training manifest file set differs from current outputs");
        }
        String actualRoot = M3Hashing.hashRows(actual);
        String expectedRoot = Files.readString(root.resolve("TRAINING_SHA256"), StandardCharsets.UTF_8).trim();
        M3Hashing.requireSha256(expectedRoot, "TRAINING_SHA256");
        if (!actualRoot.equals(expectedRoot)) {
            throw new IOException("TRAINING_SHA256 mismatch: expected " + expectedRoot + " actual " + actualRoot);
        }
        return actualRoot;
    }

    private static Path safeResolve(Path root, String relative) throws IOException {
        if (relative.startsWith("/") || relative.contains("\\")) {
            throw new IOException("Unsafe training manifest path: " + relative);
        }
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("Training manifest escapes root: " + relative);
        }
        return resolved;
    }
}
