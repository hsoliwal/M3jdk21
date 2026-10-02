// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.verbatim;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Byte-exact JDK21 product-tree comparator used before hash-pinned OpenRewrite mutation.
 *
 * <p>The immutable baseline is compared against the candidate for every regular file below
 * {@code src/}, {@code test/}, {@code make/} and {@code doc/}. A changed path is admitted only when
 * its exact before/after SHA-256 pair is present in the approval manifest and names the owning
 * recipe. This is the repository-wide preimage gate for file-local recipe authoring.</p>
 */
public final class M3Jdk21VerbatimCompare {
    private static final List<String> PRODUCT_ROOTS = List.of("src", "test", "make", "doc");
    private static final String ABSENT = "ABSENT";

    private M3Jdk21VerbatimCompare() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println(
                    "usage: M3Jdk21VerbatimCompare "
                            + "<baseline-root> <candidate-root> <approved-tsv> <output-tsv>");
            System.exit(64);
        }

        Summary summary = compare(
                Path.of(args[0]),
                Path.of(args[1]),
                Path.of(args[2]),
                Path.of(args[3]));
        System.out.println(summary);
        if (summary.unapproved() != 0) {
            System.exit(2);
        }
    }

    public static Summary compare(
            Path baselineRoot,
            Path candidateRoot,
            Path approvedTsv,
            Path outputTsv)
            throws IOException {
        Path baseline = directory(baselineRoot, "baselineRoot");
        Path candidate = directory(candidateRoot, "candidateRoot");
        Map<String, Approval> approvals = approvals(approvedTsv);
        Map<String, State> before = inventory(baseline);
        Map<String, State> after = inventory(candidate);

        TreeSet<String> paths = new TreeSet<>();
        paths.addAll(before.keySet());
        paths.addAll(after.keySet());

        Path output = Objects.requireNonNull(outputTsv, "outputTsv").toAbsolutePath().normalize();
        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);

        int same = 0;
        int modified = 0;
        int added = 0;
        int deleted = 0;
        int approvedChanges = 0;
        int unapproved = 0;

        try (BufferedWriter writer = Files.newBufferedWriter(
                output,
                StandardCharsets.UTF_8)) {
            writer.write(
                    "path\tstatus\tbefore_sha256\tafter_sha256\t"
                            + "before_bytes\tafter_bytes\trecipe_id\tapproved\n");

            for (String path : paths) {
                State left = before.get(path);
                State right = after.get(path);
                String leftHash = left == null ? ABSENT : left.sha256();
                String rightHash = right == null ? ABSENT : right.sha256();
                long leftBytes = left == null ? 0L : left.bytes();
                long rightBytes = right == null ? 0L : right.bytes();

                Status status;
                if (left == null) {
                    status = Status.ADDED;
                    added++;
                } else if (right == null) {
                    status = Status.DELETED;
                    deleted++;
                } else if (!leftHash.equals(rightHash)) {
                    status = Status.MODIFIED;
                    modified++;
                } else {
                    status = Status.SAME;
                    same++;
                }

                String recipeId = "";
                boolean approved = status == Status.SAME;
                if (status != Status.SAME) {
                    Approval approval = approvals.get(path);
                    if (approval != null
                            && approval.beforeSha256().equals(leftHash)
                            && approval.afterSha256().equals(rightHash)) {
                        approved = true;
                        recipeId = approval.recipeId();
                        approvedChanges++;
                    } else {
                        unapproved++;
                    }
                }

                writer.write(path);
                writer.write('\t');
                writer.write(status.name());
                writer.write('\t');
                writer.write(leftHash);
                writer.write('\t');
                writer.write(rightHash);
                writer.write('\t');
                writer.write(Long.toString(leftBytes));
                writer.write('\t');
                writer.write(Long.toString(rightBytes));
                writer.write('\t');
                writer.write(recipeId);
                writer.write('\t');
                writer.write(approved ? "YES" : "NO");
                writer.write('\n');
            }
        }

        return new Summary(
                paths.size(),
                same,
                modified,
                added,
                deleted,
                approvedChanges,
                unapproved,
                output);
    }

    private static Map<String, State> inventory(Path root) throws IOException {
        TreeMap<String, State> result = new TreeMap<>();
        for (String productRoot : PRODUCT_ROOTS) {
            Path subtree = root.resolve(productRoot);
            if (!Files.isDirectory(subtree)) continue;
            try (var stream = Files.walk(subtree)) {
                for (Path file : stream.filter(Files::isRegularFile).toList()) {
                    String relative = normalized(root.relativize(file));
                    State prior = result.put(relative, state(file));
                    if (prior != null) {
                        throw new IllegalStateException("duplicate product path: " + relative);
                    }
                }
            }
        }
        return Map.copyOf(result);
    }

    private static State state(Path file) throws IOException {
        return new State(sha256(file), Files.size(file));
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest = digest();
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
            for (int read; (read = input.read(buffer)) != -1; ) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Map<String, Approval> approvals(Path file) throws IOException {
        Path checked = Objects.requireNonNull(file, "approvedTsv").toAbsolutePath().normalize();
        if (!Files.isRegularFile(checked)) {
            throw new IllegalArgumentException("approved TSV does not exist: " + checked);
        }

        List<String> lines = Files.readAllLines(checked, StandardCharsets.UTF_8);
        if (lines.isEmpty()
                || !lines.getFirst().equals(
                        "path\tbefore_sha256\tafter_sha256\trecipe_id")) {
            throw new IllegalArgumentException("invalid approved-product-delta.tsv header");
        }

        TreeMap<String, Approval> result = new TreeMap<>();
        for (int line = 1; line < lines.size(); line++) {
            String value = lines.get(line);
            if (value.isBlank()) continue;
            String[] cells = value.split("\t", -1);
            if (cells.length != 4
                    || !productPath(cells[0])
                    || !hashOrAbsent(cells[1])
                    || !hashOrAbsent(cells[2])
                    || cells[3].isBlank()) {
                throw new IllegalArgumentException(
                        "invalid approved-product-delta row at line " + (line + 1));
            }
            Approval prior = result.put(
                    cells[0],
                    new Approval(cells[1], cells[2], cells[3]));
            if (prior != null) {
                throw new IllegalArgumentException("duplicate approval path: " + cells[0]);
            }
        }
        return Map.copyOf(result);
    }

    private static boolean productPath(String value) {
        if (value == null
                || value.indexOf('\\') >= 0
                || value.startsWith("/")
                || value.contains("/../")
                || value.contains("/./")
                || value.endsWith("/..")
                || value.endsWith("/.")) {
            return false;
        }
        return PRODUCT_ROOTS.stream().anyMatch(root -> value.startsWith(root + "/"));
    }

    private static boolean hashOrAbsent(String value) {
        return ABSENT.equals(value) || value.matches("[0-9a-f]{64}");
    }

    private static Path directory(Path value, String field) {
        Path checked = Objects.requireNonNull(value, field).toAbsolutePath().normalize();
        if (!Files.isDirectory(checked)) {
            throw new IllegalArgumentException(field + " is not a directory: " + checked);
        }
        return checked;
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public enum Status {
        SAME,
        MODIFIED,
        ADDED,
        DELETED
    }

    private record State(String sha256, long bytes) {
        private State {
            if (!sha256.matches("[0-9a-f]{64}") || bytes < 0) {
                throw new IllegalArgumentException("invalid file state");
            }
        }
    }

    public record Approval(
            String beforeSha256,
            String afterSha256,
            String recipeId) {
        public Approval {
            if (!hashOrAbsent(beforeSha256)
                    || !hashOrAbsent(afterSha256)
                    || Objects.requireNonNull(recipeId, "recipeId").isBlank()) {
                throw new IllegalArgumentException("invalid approval");
            }
        }
    }

    public record Summary(
            int total,
            int same,
            int modified,
            int added,
            int deleted,
            int approvedChanges,
            int unapproved,
            Path output) {
        public Summary {
            if (total < 0
                    || same < 0
                    || modified < 0
                    || added < 0
                    || deleted < 0
                    || approvedChanges < 0
                    || unapproved < 0
                    || total != same + modified + added + deleted) {
                throw new IllegalArgumentException("invalid comparison summary");
            }
            output = Objects.requireNonNull(output, "output").toAbsolutePath().normalize();
        }

        public boolean clean() {
            return unapproved == 0;
        }

        @Override
        public String toString() {
            return "M3 JDK21 verbatim comparison: total="
                    + total
                    + " same="
                    + same
                    + " modified="
                    + modified
                    + " added="
                    + added
                    + " deleted="
                    + deleted
                    + " approvedChanges="
                    + approvedChanges
                    + " unapproved="
                    + unapproved;
        }
    }
}
