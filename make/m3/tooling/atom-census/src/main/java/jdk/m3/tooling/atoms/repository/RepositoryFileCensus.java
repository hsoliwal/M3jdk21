/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.repository;

import jdk.m3.tooling.atoms.contract.AtomCoverageLedger;
import jdk.m3.tooling.atoms.contract.AtomKind;
import jdk.m3.tooling.atoms.contract.CoverageGate;
import jdk.m3.tooling.atoms.contract.GateStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Emits repository-complete file-container atoms from one exact committed Git tree. */
public final class RepositoryFileCensus {
    /** Exact schema version of the repository file census. */
    public static final String SCHEMA = "M3-REPOSITORY-FILE-CENSUS/1";
    /** Deterministic tracked-entry manifest header. */
    public static final String FILES_HEADER = "source_path\tmode\tobject_type\tobject_id\tsource_size"
            + "\tsource_sha256\tsource_family\towner\tatom_id";
    /** Deterministic receipt header. */
    public static final String RECEIPT_HEADER = "key\tvalue";
    /** Normative repository-complete contract document. */
    public static final String CONTRACT_PATH = "m3/docs/M3JDK21_MACHINE_MAINTAINED_MECHANICAL_SUPERSET.md";
    /** Normative contract section bound to generated file-container atoms. */
    public static final String CONTRACT_REF = CONTRACT_PATH + "#repository-complete-invariant";

    private RepositoryFileCensus() {
    }

    /**
     * CLI: {@code M3RepositoryFileCensus <repository> <revision> <output-directory>}.
     *
     * @param args repository path, commit-resolving revision and output directory
     * @throws IOException when Git objects or outputs cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("usage: <repository> <revision> <output-directory>");
        }
        Result result = generate(Path.of(args[0]), args[1], Path.of(args[2]));
        System.out.printf(Locale.ROOT,
                "M3_REPOSITORY_FILE_CENSUS\tcommit=%s\ttree=%s\tfiles=%d\tevidence=%d\tsemantic_sha256=%s%n",
                result.commitId(), result.treeId(), result.fileCount(), result.evidenceCount(),
                result.semanticSha256());
    }

    /**
     * Generates exact source, atom, evidence and receipt tables for {@code revision}.
     *
     * <p>The input is the committed Git tree, not the working tree. The first leaf proves inventory, source identity,
     * family-pattern binding and the global repository-complete contract when that contract is present. Every other
     * required file-container gate is emitted as explicit {@code NOT_RUN}; no absent proof becomes success.</p>
     *
     * @param repository repository root or a path inside it
     * @param revision Git revision resolving to a commit
     * @param outputDirectory destination for deterministic census files
     * @return generated result identity
     * @throws IOException when Git objects or outputs cannot be read or written
     */
    public static Result generate(Path repository, String revision, Path outputDirectory) throws IOException {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        GitTreeSnapshot snapshot = GitTreeSnapshot.capture(repository, revision);
        GitTreeEntry contract = snapshot.entries().stream()
                .filter(entry -> entry.path().equals(CONTRACT_PATH))
                .findFirst()
                .orElse(null);
        Map<String, String> identityOverrides = FileAtomIdentities.read(snapshot);

        List<FileRow> files = new ArrayList<>(snapshot.entries().size());
        List<AtomRow> atoms = new ArrayList<>(snapshot.entries().size());
        List<EvidenceRow> evidence = new ArrayList<>(snapshot.entries().size() * 11);
        Set<String> atomIds = new HashSet<>();
        for (GitTreeEntry entry : snapshot.entries()) {
            SourceFamily family = SourceFamily.classify(entry.path());
            String owner = owner(entry.path());
            String atomId = identityOverrides.getOrDefault(entry.path(), derivedAtomId(entry.path()));
            if (!atomIds.add(atomId)) {
                throw new IllegalStateException("file atom identity collision for " + entry.path());
            }
            String patternId = family.patternId();
            files.add(new FileRow(entry, family, owner, atomId));
            atoms.add(new AtomRow(atomId, entry.path(), owner,
                    "git-tree-entry:" + entry.mode() + ":" + entry.objectType() + ":" + entry.objectId(),
                    entry.sourceSha256(), patternId));
            evidence.addAll(evidence(snapshot, entry, contract, atomId, patternId));
        }

        files.sort(Comparator.comparing(row -> row.entry().path()));
        atoms.sort(Comparator.comparing(AtomRow::atomId));
        evidence.sort(Comparator.comparing(EvidenceRow::atomId).thenComparing(row -> row.gate().name()));

        String filesText = table(FILES_HEADER, files.stream().map(FileRow::row).toList());
        String atomsText = table(AtomCoverageLedger.ATOMS_HEADER, atoms.stream().map(AtomRow::row).toList());
        String evidenceText = table(AtomCoverageLedger.EVIDENCE_HEADER,
                evidence.stream().map(EvidenceRow::row).toList());
        String semanticSha256 = semanticDigest(filesText, atomsText, evidenceText);
        long blobCount = snapshot.entries().stream().filter(entry -> entry.objectType().equals("blob")).count();
        long gitlinkCount = snapshot.entries().size() - blobCount;
        String receiptText = receipt(snapshot, files.size(), evidence.size(), blobCount, gitlinkCount,
                identityOverrides.size(), filesText, atomsText, evidenceText, semanticSha256);

        Path output = outputDirectory.toAbsolutePath().normalize();
        Files.createDirectories(output);
        Path filesPath = output.resolve("FILES.tsv");
        Path atomsPath = output.resolve("ATOMS.tsv");
        Path evidencePath = output.resolve("EVIDENCE.tsv");
        Path receiptPath = output.resolve("REPOSITORY_CENSUS_RECEIPT.tsv");
        writeAtomically(filesPath, filesText);
        writeAtomically(atomsPath, atomsText);
        writeAtomically(evidencePath, evidenceText);
        writeAtomically(receiptPath, receiptText);

        AtomCoverageLedger parsed = AtomCoverageLedger.read(atomsPath, evidencePath);
        if (parsed.atoms().size() != files.size() || parsed.evidenceCount() != evidence.size()) {
            throw new IllegalStateException("generated ledger count mismatch after strict readback");
        }
        return new Result(snapshot.commitId(), snapshot.treeId(), files.size(), evidence.size(), semanticSha256,
                filesPath, atomsPath, evidencePath, receiptPath);
    }

    private static List<EvidenceRow> evidence(GitTreeSnapshot snapshot, GitTreeEntry entry,
            GitTreeEntry contract, String atomId, String patternId) {
        EnumSet<CoverageGate> required = CoverageGate.requiredFor(AtomKind.FILE_CONTAINER);
        List<EvidenceRow> rows = new ArrayList<>(required.size());
        required.stream().sorted(Comparator.comparing(Enum::name)).forEach(gate -> {
            switch (gate) {
                case INVENTORIED -> rows.add(pass(atomId, gate,
                        "git-tree:" + snapshot.treeId() + ":" + entry.objectId() + ":" + entry.path(),
                        sha256("M3-GIT-TREE-ENTRY/1\n" + entry.mode() + "\t" + entry.objectType() + "\t"
                                + entry.objectId() + "\t" + entry.path() + "\n"),
                        "exact committed Git tree entry"));
                case SOURCE_IDENTITY -> rows.add(pass(atomId, gate,
                        "git-object:" + snapshot.objectFormat() + ":" + entry.objectId(),
                        entry.sourceSha256(), "SHA-256 of exact committed source bytes"));
                case PATTERN_BOUND -> rows.add(pass(atomId, gate, "m3-pattern:" + patternId,
                        sha256("M3-PATTERN-BINDING/1\n" + patternId + "\n"),
                        "deterministic source-family file-container pattern"));
                case CONTRACT_BOUND -> {
                    if (contract == null) {
                        rows.add(notRun(atomId, gate, "normative repository-complete contract absent at revision"));
                    } else {
                        rows.add(pass(atomId, gate,
                                "git-object:" + snapshot.objectFormat() + ":" + contract.objectId() + ":"
                                        + CONTRACT_PATH,
                                contract.sourceSha256(), "repository-complete invariant contract"));
                    }
                }
                default -> rows.add(notRun(atomId, gate, "not executed by the repository file census leaf"));
            }
        });
        return rows;
    }

    private static EvidenceRow pass(String atomId, CoverageGate gate, String evidenceRef, String evidenceSha256,
            String note) {
        return new EvidenceRow(atomId, gate, GateStatus.PASS, evidenceRef, evidenceSha256, note);
    }

    private static EvidenceRow notRun(String atomId, CoverageGate gate, String note) {
        return new EvidenceRow(atomId, gate, GateStatus.NOT_RUN, "-", "-", note);
    }

    private static String owner(String path) {
        String[] segments = path.split("/", -1);
        if (segments[0].equals("src") && segments.length > 1) {
            return "module:" + segments[1];
        }
        if (segments[0].equals("test") && segments.length > 1) {
            return "test:" + segments[1];
        }
        if (segments[0].equals("make")) {
            return "build:make";
        }
        if (segments[0].equals("m3") && segments.length > 1) {
            return "m3:" + segments[1];
        }
        if (segments[0].equals(".github")) {
            return "automation:.github";
        }
        return segments.length == 1 ? "repository:root" : "repository:" + segments[0];
    }

    private static String derivedAtomId(String path) {
        return "m3:file:" + sha256("M3-FILE-ID/1\n" + path + "\n");
    }

    private static String table(String header, List<String> rows) {
        StringBuilder builder = new StringBuilder(header.length() + rows.size() * 160);
        builder.append(header).append('\n');
        rows.forEach(row -> builder.append(row).append('\n'));
        return builder.toString();
    }

    private static String receipt(GitTreeSnapshot snapshot, int fileCount, int evidenceCount, long blobCount,
            long gitlinkCount, int identityOverrideCount, String filesText, String atomsText, String evidenceText,
            String semanticSha256) {
        List<String> rows = List.of(
                "schema\t" + SCHEMA,
                "commit\t" + snapshot.commitId(),
                "tree\t" + snapshot.treeId(),
                "object_format\t" + snapshot.objectFormat(),
                "file_count\t" + fileCount,
                "blob_count\t" + blobCount,
                "gitlink_count\t" + gitlinkCount,
                "identity_override_count\t" + identityOverrideCount,
                "atom_count\t" + fileCount,
                "evidence_count\t" + evidenceCount,
                "files_sha256\t" + sha256(filesText),
                "atoms_sha256\t" + sha256(atomsText),
                "evidence_sha256\t" + sha256(evidenceText),
                "semantic_sha256\t" + semanticSha256);
        return table(RECEIPT_HEADER, rows);
    }

    private static String semanticDigest(String filesText, String atomsText, String evidenceText) {
        return sha256(SCHEMA + "\n--FILES--\n" + filesText + "--ATOMS--\n" + atomsText
                + "--EVIDENCE--\n" + evidenceText);
    }

    private static void writeAtomically(Path path, String text) throws IOException {
        Path directory = path.getParent();
        Path temporary = Files.createTempFile(directory, "." + path.getFileName() + ".", ".tmp");
        boolean moved = false;
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static String sha256(String value) {
        return HexFormat.of().formatHex(sha256Digest().digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the JDK", exception);
        }
    }

    /**
     * Immutable receipt returned by one successful generation and strict readback.
     *
     * @param commitId exact source commit identity
     * @param treeId exact source root-tree identity
     * @param fileCount generated file-container atom count
     * @param evidenceCount generated evidence-row count
     * @param semanticSha256 semantic digest of all three generated tables
     * @param filesPath generated tracked-entry manifest
     * @param atomsPath generated atom ledger
     * @param evidencePath generated evidence ledger
     * @param receiptPath generated census receipt
     */
    public record Result(
            String commitId,
            String treeId,
            int fileCount,
            int evidenceCount,
            String semanticSha256,
            Path filesPath,
            Path atomsPath,
            Path evidencePath,
            Path receiptPath) {
    }

    private record FileRow(GitTreeEntry entry, SourceFamily family, String owner, String atomId) {
        String row() {
            return String.join("\t", entry.path(), entry.mode(), entry.objectType(), entry.objectId(),
                    Long.toString(entry.sourceSize()), entry.sourceSha256(), family.name(), owner, atomId);
        }
    }

    private record AtomRow(String atomId, String sourcePath, String owner, String signature, String sourceSha256,
            String patternId) {
        String row() {
            return String.join("\t", atomId, sourcePath, AtomKind.FILE_CONTAINER.name(), owner, signature,
                    sourceSha256, patternId, CONTRACT_REF, Boolean.TRUE.toString());
        }
    }

    private record EvidenceRow(String atomId, CoverageGate gate, GateStatus status, String evidenceRef,
            String evidenceSha256, String note) {
        String row() {
            return String.join("\t", atomId, gate.name(), status.name(), evidenceRef, evidenceSha256, note);
        }
    }
}
