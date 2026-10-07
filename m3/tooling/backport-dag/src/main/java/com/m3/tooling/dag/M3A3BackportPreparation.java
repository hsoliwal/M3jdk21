// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.m3.a3.A3Apply;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Packet-front preparation gate between exact file-delta inventory and source-changing backport recipes.
 *
 * <p>Java source/test files are converged through the existing A3 FILE recipe laboratory before a
 * backport recipe crate may run. Non-Java files are classified explicitly and remain owned by the
 * source-sealed native/text recipe lane; they are never passed through a Java parser.</p>
 */
public final class M3A3BackportPreparation {
    public enum Lane {
        JAVA_A3_FIXED_POINT,
        NON_JAVA_SOURCE_SEALED
    }

    public record Row(
            String path,
            Lane lane,
            String beforeSha,
            String preparedSha,
            boolean changed,
            boolean fixedPoint) {
        public Row {
            path = path(path);
            lane = Objects.requireNonNull(lane, "lane");
            beforeSha = sha(beforeSha, "beforeSha");
            preparedSha = sha(preparedSha, "preparedSha");
            if (lane == Lane.JAVA_A3_FIXED_POINT && !fixedPoint) {
                throw new IllegalArgumentException("Java A3 row must be fixed point");
            }
            if (lane == Lane.NON_JAVA_SOURCE_SEALED && (changed || fixedPoint)) {
                throw new IllegalArgumentException("non-Java lane records classification only");
            }
        }
    }

    public record Receipt(List<Row> rows) {
        public Receipt {
            rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("empty preparation receipt");
            }
            List<String> ordered = rows.stream().map(Row::path).sorted().toList();
            if (!ordered.equals(rows.stream().map(Row::path).toList())) {
                throw new IllegalArgumentException("preparation rows must be path sorted");
            }
            if (ordered.stream().distinct().count() != ordered.size()) {
                throw new IllegalArgumentException("duplicate preparation path");
            }
        }

        public int javaFiles() {
            return (int) rows.stream().filter(row -> row.lane() == Lane.JAVA_A3_FIXED_POINT).count();
        }

        public int nonJavaFiles() {
            return rows.size() - javaFiles();
        }
    }

    private M3A3BackportPreparation() {}

    public static Receipt prepare(
            Path repositoryRoot,
            Path outputRoot,
            List<String> targetPaths) throws IOException {
        Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot").toAbsolutePath().normalize();
        Path out = Objects.requireNonNull(outputRoot, "outputRoot").toAbsolutePath().normalize();
        List<String> targets = targetPaths.stream()
                .map(M3A3BackportPreparation::path)
                .distinct()
                .sorted()
                .toList();
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("targetPaths");
        }

        ArrayList<String> java = new ArrayList<>();
        for (String target : targets) {
            Path file = root.resolve(target).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                throw new IOException("missing or escaped backport target: " + target);
            }
            if (javaSource(target)) {
                java.add(target);
            }
        }

        Map<String, A3Apply.Receipt> javaReceipts = new HashMap<>();
        if (!java.isEmpty()) {
            for (A3Apply.Receipt receipt : A3Apply.run(root, out.resolve("a3"), java)) {
                javaReceipts.put(receipt.path(), receipt);
            }
            if (javaReceipts.size() != java.size()) {
                throw new IllegalStateException("A3 receipt coverage mismatch");
            }
        }

        ArrayList<Row> rows = new ArrayList<>(targets.size());
        for (String target : targets) {
            Path file = root.resolve(target).normalize();
            String beforeSha = sha256(Files.readAllBytes(file));
            if (javaSource(target)) {
                A3Apply.Receipt receipt = Objects.requireNonNull(
                        javaReceipts.get(target), "missing A3 receipt " + target);
                if (!receipt.beforeSha().equals(beforeSha) || !receipt.fixedPoint()) {
                    throw new IllegalStateException("A3 receipt drift: " + target);
                }
                rows.add(new Row(
                        target,
                        Lane.JAVA_A3_FIXED_POINT,
                        receipt.beforeSha(),
                        receipt.afterSha(),
                        receipt.changed(),
                        true));
            } else {
                rows.add(new Row(
                        target,
                        Lane.NON_JAVA_SOURCE_SEALED,
                        beforeSha,
                        beforeSha,
                        false,
                        false));
            }
        }
        rows.sort(Comparator.comparing(Row::path));
        Receipt receipt = new Receipt(rows);
        write(out, receipt);
        return receipt;
    }

    private static boolean javaSource(String path) {
        return (path.startsWith("src/") || path.startsWith("test/"))
                && path.endsWith(".java");
    }

    private static void write(Path out, Receipt receipt) throws IOException {
        Files.createDirectories(out);
        StringBuilder tsv = new StringBuilder(
                "path\tlane\tbeforeSha\tpreparedSha\tchanged\tfixedPoint\n");
        for (Row row : receipt.rows()) {
            tsv.append(row.path()).append('\t')
                    .append(row.lane()).append('\t')
                    .append(row.beforeSha()).append('\t')
                    .append(row.preparedSha()).append('\t')
                    .append(row.changed()).append('\t')
                    .append(row.fixedPoint()).append('\n');
        }
        Files.writeString(out.resolve("backport-preparation.tsv"), tsv, StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String path(String value) {
        String checked = Objects.toString(value, "").strip().replace('\\', '/');
        if (checked.isEmpty()
                || checked.startsWith("/")
                || checked.contains("../")
                || checked.equals("..")
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException("invalid target path");
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = Objects.toString(value, "");
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
