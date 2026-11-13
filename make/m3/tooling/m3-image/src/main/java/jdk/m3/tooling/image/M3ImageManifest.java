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

/** Complete image manifest and fixed-point verifier. */
public final class M3ImageManifest {
    private static final Set<String> EXCLUDED = Set.of("image-manifest.tsv", "IMAGE_SHA256", "CACHE_COMPLETE");

    private M3ImageManifest() {
    }

    /** Seals an image and returns its root identity. */
    public static String seal(Path image) throws IOException {
        List<String> rows = M3Files.manifest(image, EXCLUDED);
        M3Files.writeUtf8(image.resolve("image-manifest.tsv"), String.join("", rows));
        String root = M3Hashing.hashRows(rows);
        M3Files.writeUtf8(image.resolve("IMAGE_SHA256"), root + "\n");
        return root;
    }

    /** Verifies every manifested file, rejects extra regular files, and returns the root identity. */
    public static String verify(Path image) throws IOException {
        Path root = image.toAbsolutePath().normalize();
        List<String> raw = Files.readAllLines(root.resolve("image-manifest.tsv"), StandardCharsets.UTF_8);
        List<String> expectedRows = raw.stream().filter(line -> !line.isEmpty()).map(line -> line + "\n").toList();
        for (String row : expectedRows) {
            String[] fields = row.substring(0, row.length() - 1).split("\t", -1);
            if (fields.length != 2 || !fields[0].matches("[0-9a-f]{64}")) {
                throw new IOException("Invalid image manifest row: " + row);
            }
            Path file = safeResolve(root, fields[1]);
            if (!Files.isRegularFile(file) || !M3Hashing.sha256(file).equals(fields[0])) {
                throw new IOException("Image file mismatch: " + fields[1]);
            }
        }
        List<String> actualRows = M3Files.manifest(root, EXCLUDED);
        if (!actualRows.equals(expectedRows)) {
            throw new IOException("Image manifest file set differs from current image");
        }
        String actualRoot = M3Hashing.hashRows(actualRows);
        String expectedRoot = Files.readString(root.resolve("IMAGE_SHA256"), StandardCharsets.UTF_8).trim();
        if (!actualRoot.equals(expectedRoot)) {
            throw new IOException("IMAGE_SHA256 mismatch: expected " + expectedRoot + " actual " + actualRoot);
        }
        return actualRoot;
    }

    private static Path safeResolve(Path root, String relative) throws IOException {
        if (relative.startsWith("/") || relative.contains("\\")) {
            throw new IOException("Unsafe image manifest path: " + relative);
        }
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("Image manifest escapes root: " + relative);
        }
        return resolved;
    }
}
