// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.verbatim;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Deterministically generates M3HashPinnedJavaSnapshotRecipe crates from verbatim JDK21 diffs.
 *
 * <p>The generator does not modify product source. It reads an immutable baseline tree and one
 * reviewed candidate tree, finds added/modified Java files below JDK product roots, shards them,
 * and emits exact preimage/output hashes plus exact reviewed postimage templates. Deletions are
 * rejected because the snapshot recipe is reconstruction-only rather than a file deletion tool.</p>
 */
public final class M3HashPinnedCrateGenerator {
    public static final int DEFAULT_SHARD_SIZE = 256;
    private static final List<String> PRODUCT_ROOTS = List.of("src", "test");

    private M3HashPinnedCrateGenerator() {}

    public static Result generate(
            Path baselineRoot,
            Path reviewedRoot,
            Path outputResourceRoot,
            Path proposedApprovalTsv,
            String cratePrefix,
            int shardSize)
            throws IOException {
        Path baseline = directory(baselineRoot, "baselineRoot");
        Path reviewed = directory(reviewedRoot, "reviewedRoot");
        Path output = Objects.requireNonNull(outputResourceRoot, "outputResourceRoot")
                .toAbsolutePath()
                .normalize();
        String prefix = crateName(cratePrefix);
        if (shardSize < 1 || shardSize > DEFAULT_SHARD_SIZE) {
            throw new IllegalArgumentException("shardSize");
        }

        List<Delta> deltas = deltas(baseline, reviewed);
        Files.createDirectories(output);

        ArrayList<Crate> crates = new ArrayList<>();
        for (int from = 0, shard = 0; from < deltas.size(); from += shardSize, shard++) {
            int to = Math.min(deltas.size(), from + shardSize);
            String crateName = prefix + "-" + String.format("%04d", shard);
            Path crateDirectory = output.resolve(crateName);
            Files.createDirectories(crateDirectory);

            StringBuilder manifest = new StringBuilder();
            for (int index = from; index < to; index++) {
                Delta delta = deltas.get(index);
                String templateName = String.format("%04d.java.txt", index - from);
                String afterText = readUtf8(reviewed.resolve(delta.path()));
                String afterTextHash = sha256(afterText);
                String beforeTextHash = delta.beforeExists()
                        ? sha256(readUtf8(baseline.resolve(delta.path())))
                        : "ABSENT";
                Files.writeString(
                        crateDirectory.resolve(templateName),
                        afterText,
                        StandardCharsets.UTF_8);
                manifest.append(delta.path())
                        .append('\t')
                        .append(beforeTextHash)
                        .append('\t')
                        .append(afterTextHash)
                        .append('\t')
                        .append(templateName)
                        .append('\n');
            }
            Files.writeString(
                    crateDirectory.resolve("manifest.tsv"),
                    manifest,
                    StandardCharsets.UTF_8);
            crates.add(new Crate(crateName, from, to - from));
        }

        Path approval = Objects.requireNonNull(proposedApprovalTsv, "proposedApprovalTsv")
                .toAbsolutePath()
                .normalize();
        Path approvalParent = approval.getParent();
        if (approvalParent != null) Files.createDirectories(approvalParent);
        StringBuilder proposed = new StringBuilder(
                "path\tbefore_sha256\tafter_sha256\trecipe_id\n");
        for (int index = 0; index < deltas.size(); index++) {
            Delta delta = deltas.get(index);
            int shard = index / shardSize;
            String crateName = prefix + "-" + String.format("%04d", shard);
            proposed.append(delta.path())
                    .append('\t')
                    .append(delta.beforeSha256())
                    .append('\t')
                    .append(delta.afterSha256())
                    .append('\t')
                    .append("hash-pinned:")
                    .append(crateName)
                    .append('\n');
        }
        Files.writeString(approval, proposed, StandardCharsets.UTF_8);

        return new Result(List.copyOf(deltas), List.copyOf(crates), output, approval);
    }

    private static List<Delta> deltas(Path baseline, Path reviewed) throws IOException {
        TreeSet<String> paths = new TreeSet<>();
        paths.addAll(javaPaths(baseline));
        paths.addAll(javaPaths(reviewed));

        ArrayList<Delta> result = new ArrayList<>();
        for (String path : paths) {
            Path before = baseline.resolve(path);
            Path after = reviewed.resolve(path);
            boolean beforeExists = Files.isRegularFile(before);
            boolean afterExists = Files.isRegularFile(after);
            if (!afterExists && beforeExists) {
                throw new IllegalArgumentException(
                        "hash-pinned snapshot crate cannot delete source: " + path);
            }
            if (!afterExists) continue;

            String beforeHash = beforeExists ? sha256Bytes(before) : "ABSENT";
            String afterHash = sha256Bytes(after);
            if (!beforeHash.equals(afterHash)) {
                result.add(new Delta(path, beforeHash, afterHash, beforeExists));
            }
        }
        return List.copyOf(result);
    }

    private static List<String> javaPaths(Path root) throws IOException {
        TreeSet<String> paths = new TreeSet<>();
        for (String productRoot : PRODUCT_ROOTS) {
            Path subtree = root.resolve(productRoot);
            if (!Files.isDirectory(subtree)) continue;
            try (var stream = Files.walk(subtree)) {
                for (Path file : stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .toList()) {
                    paths.add(normalized(root.relativize(file)));
                }
            }
        }
        return List.copyOf(paths);
    }

    private static String readUtf8(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (java.nio.charset.CharacterCodingException invalid) {
            throw new IOException("Java source is not strict UTF-8: " + file, invalid);
        }
    }

    private static String sha256(String text) {
        return HexFormat.of().formatHex(
                digest().digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String sha256Bytes(Path file) throws IOException {
        return HexFormat.of().formatHex(digest().digest(Files.readAllBytes(file)));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static Path directory(Path value, String field) {
        Path checked = Objects.requireNonNull(value, field).toAbsolutePath().normalize();
        if (!Files.isDirectory(checked)) {
            throw new IllegalArgumentException(field + " is not a directory: " + checked);
        }
        return checked;
    }

    private static String crateName(String value) {
        String checked = Objects.requireNonNull(value, "cratePrefix").strip();
        if (!checked.matches("[a-z0-9][a-z0-9-]{0,70}")) {
            throw new IllegalArgumentException("cratePrefix");
        }
        return checked;
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    public record Delta(
            String path,
            String beforeSha256,
            String afterSha256,
            boolean beforeExists) {
        public Delta {
            path = Objects.requireNonNull(path, "path");
            beforeSha256 = hashOrAbsent(beforeSha256, "beforeSha256");
            afterSha256 = hash(afterSha256, "afterSha256");
        }
    }

    public record Crate(String crateName, int offset, int size) {
        public Crate {
            crateName = M3HashPinnedCrateGenerator.crateName(crateName);
            if (offset < 0 || size < 1 || size > DEFAULT_SHARD_SIZE) {
                throw new IllegalArgumentException("crate range");
            }
        }
    }

    public record Result(
            List<Delta> deltas,
            List<Crate> crates,
            Path outputResourceRoot,
            Path proposedApprovalTsv) {
        public Result {
            deltas = List.copyOf(Objects.requireNonNull(deltas, "deltas"));
            crates = List.copyOf(Objects.requireNonNull(crates, "crates"));
            outputResourceRoot =
                    Objects.requireNonNull(outputResourceRoot, "outputResourceRoot")
                            .toAbsolutePath()
                            .normalize();
            proposedApprovalTsv =
                    Objects.requireNonNull(proposedApprovalTsv, "proposedApprovalTsv")
                            .toAbsolutePath()
                            .normalize();
        }
    }

    private static String hashOrAbsent(String value, String field) {
        return "ABSENT".equals(value) ? value : hash(value, field);
    }

    private static String hash(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
