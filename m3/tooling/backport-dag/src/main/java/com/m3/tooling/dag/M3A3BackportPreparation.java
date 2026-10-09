// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.m3.a3.A3Apply;
import com.synexia.rewrite.M3RecipeMasteryPortableReceipt;
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
    private static final String PHASE_SCHEMA = "M3_A3_BACKPORT_PHASE_PROOF_V1";

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

    public enum ProofPhase {
        ATOMIZATION,
        PATTERN_IOP,
        DOCUMENTATION,
        FIXED_POINT
    }

    /** Content-addressed proof that the canonical A3 recipe covered one semantic sub-phase. */
    public record PhaseProof(
            ProofPhase phase,
            int javaFiles,
            boolean applicable,
            String root) {
        public PhaseProof {
            phase = Objects.requireNonNull(phase, "phase");
            if (javaFiles < 0) {
                throw new IllegalArgumentException("javaFiles");
            }
            if (applicable != (javaFiles > 0)) {
                throw new IllegalArgumentException("applicable");
            }
            root = sha(root, "root");
        }
    }

    public record Receipt(List<Row> rows, String masteryRoot) {
        public Receipt {
            rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
            masteryRoot = sha(masteryRoot, "masteryRoot");
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

        /**
         * One immutable proof row per semantic A3 phase.
         *
         * <p>The canonical Synexia A3 recipe remains the sole transformation owner. These rows
         * expose its atomization, pattern/IOP, documentation and fixed-point proof as small DAG
         * atoms without rerunning or reimplementing the recipe.</p>
         */
        public List<PhaseProof> phaseProofs() {
            int javaCount = javaFiles();
            boolean applicable = javaCount > 0;
            return java.util.Arrays.stream(ProofPhase.values())
                    .map(phase -> new PhaseProof(
                            phase,
                            javaCount,
                            applicable,
                            phaseRoot(phase, rows, masteryRoot)))
                    .toList();
        }

        public PhaseProof requirePhase(ProofPhase phase) {
            ProofPhase checked = Objects.requireNonNull(phase, "phase");
            return phaseProofs().stream()
                    .filter(proof -> proof.phase() == checked)
                    .findFirst()
                    .orElseThrow();
        }
    }

    private M3A3BackportPreparation() {}

    /**
     * Legacy entry point retained only to fail closed. A3 execution without portable mastery
     * evidence is forbidden.
     */
    @Deprecated(forRemoval = false)
    public static Receipt prepare(
            Path repositoryRoot,
            Path outputRoot,
            List<String> targetPaths) throws IOException {
        throw new IllegalArgumentException("verified Synexia mastery receipt required");
    }

    public static Receipt prepare(
            Path repositoryRoot,
            Path outputRoot,
            List<String> targetPaths,
            M3RecipeMasteryPortableReceipt.Verified mastery) throws IOException {
        M3RecipeMasteryPortableReceipt.Verified checkedMastery =
                Objects.requireNonNull(mastery, "mastery");
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
            for (A3Apply.Receipt receipt :
                    A3Apply.run(root, out.resolve("a3"), java, checkedMastery)) {
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
        Receipt receipt = new Receipt(rows, checkedMastery.root());
        write(out, receipt);
        writePhaseProofs(out, receipt.phaseProofs(), receipt.masteryRoot());
        return receipt;
    }

    private static boolean javaSource(String path) {
        return (path.startsWith("src/") || path.startsWith("test/"))
                && path.endsWith(".java");
    }

    private static void write(Path out, Receipt receipt) throws IOException {
        Files.createDirectories(out);
        StringBuilder tsv = new StringBuilder(
                "path\tlane\tbeforeSha\tpreparedSha\tchanged\tfixedPoint\tmasteryRoot\n");
        for (Row row : receipt.rows()) {
            tsv.append(row.path()).append('\t')
                    .append(row.lane()).append('\t')
                    .append(row.beforeSha()).append('\t')
                    .append(row.preparedSha()).append('\t')
                    .append(row.changed()).append('\t')
                    .append(row.fixedPoint()).append('\t')
                    .append(receipt.masteryRoot()).append('\n');
        }
        Files.writeString(out.resolve("backport-preparation.tsv"), tsv, StandardCharsets.UTF_8);
    }

    private static void writePhaseProofs(
            Path out,
            List<PhaseProof> proofs,
            String masteryRoot) throws IOException {
        String checkedMasteryRoot = sha(masteryRoot, "masteryRoot");
        StringBuilder tsv =
                new StringBuilder("phase\tjavaFiles\tapplicable\troot\tmasteryRoot\n");
        for (PhaseProof proof : proofs) {
            tsv.append(proof.phase())
                    .append('\t')
                    .append(proof.javaFiles())
                    .append('\t')
                    .append(proof.applicable())
                    .append('\t')
                    .append(proof.root())
                    .append('\t')
                    .append(checkedMasteryRoot)
                    .append('\n');
        }
        Files.writeString(
                out.resolve("backport-preparation-phases.tsv"),
                tsv,
                StandardCharsets.UTF_8);
    }

    private static String phaseRoot(
            ProofPhase phase,
            List<Row> rows,
            String masteryRoot) {
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");
            update(digest, PHASE_SCHEMA);
            update(digest, Objects.requireNonNull(phase, "phase").name());
            update(digest, sha(masteryRoot, "masteryRoot"));
            List<Row> javaRows = rows.stream()
                    .filter(row -> row.lane() == Lane.JAVA_A3_FIXED_POINT)
                    .toList();
            update(digest, Integer.toString(javaRows.size()));
            for (Row row : javaRows) {
                update(digest, row.path());
                update(digest, row.beforeSha());
                update(digest, row.preparedSha());
                update(digest, Boolean.toString(row.changed()));
                update(digest, Boolean.toString(row.fixedPoint()));
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void update(
            java.security.MessageDigest digest,
            String value) {
        byte[] bytes = Objects.requireNonNull(value, "value")
                .getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
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
