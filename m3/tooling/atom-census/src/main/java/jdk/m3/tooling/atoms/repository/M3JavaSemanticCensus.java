/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.repository;

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
import java.util.Set;
import jdk.m3.tooling.atoms.contract.M3AtomCoverageLedger;
import jdk.m3.tooling.atoms.contract.M3AtomKind;
import jdk.m3.tooling.atoms.contract.M3CoverageGate;

/**
 * Writes source-sealed Java/JNI-declaration atom records from one immutable committed Git tree.
 *
 * <p>Outputs are additive companions to the existing file-container census. They deliberately contain pending
 * per-atom contract, compiler, JIT, native and performance gates: neither javac parsing nor an LLM may admit them.
 * Other language families remain outside the present Java-only scope.</p>
 */
public final class M3JavaSemanticCensus {
    /** Schema identity, independent of file-container schema. */
    public static final String SCHEMA = "M3-JAVA-SEMANTIC-CENSUS/1";
    /** Mandatory source-parse disposition header. */
    public static final String PARSE_STATUS_HEADER = "source_path\tsource_sha256\tstatus\tatoms\tnative_declarations\tdetail";
    /** Mandatory receipt header. */
    public static final String RECEIPT_HEADER = "key\tvalue";
    /** Invariant contract only; not proof of each method's behavioral contract. */
    public static final String CONTRACT_REF = M3RepositoryFileCensus.CONTRACT_REF;

    private M3JavaSemanticCensus() {
    }

    /**
     * CLI: {@code M3JavaSemanticCensus <repository> <revision> <output-directory>}.
     *
     * @param args repository, commit-resolving revision and output directory
     * @throws IOException when source Git objects or deterministic output cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("usage: <repository> <revision> <output-directory>");
        }
        Result result = generate(Path.of(args[0]), args[1], Path.of(args[2]));
        System.out.printf(Locale.ROOT,
                "M3_JAVA_SEMANTIC_CENSUS\tcommit=%s\tjava_files=%d\tparsed=%d\tblocked=%d"
                        + "\tatoms=%d\tnative=%d\tstate=HOLD%n",
                result.commitId(), result.javaFiles(), result.parsedFiles(), result.blockedFiles(),
                result.atoms(), result.nativeDeclarations());
    }

    /**
     * Generates all source-parse dispositions, including rejected/undecodable Java files.
     *
     * <p>A parse error creates a status row but no invented behavioral atom. A missing Java source is never silently
     * treated as proof; {@code blockedFiles > 0} prevents Java-complete claims. This generator does not mutate the
     * input repository or any original {@code FILE_CONTAINER} output.</p>
     *
     * @param repository repository root or path inside repository
     * @param revision revision resolving to an immutable commit
     * @param destination output directory owned by the caller
     * @return immutable summary with exact output paths
     * @throws IOException when Git or output files cannot be processed
     */
    public static Result generate(Path repository, String revision, Path destination) throws IOException {
        M3GitTreeSnapshot snapshot = M3GitTreeSnapshot.capture(repository, revision);
        List<M3JavaSemanticExtractor.Atom> atoms = new ArrayList<>();
        List<String> dispositions = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        int parsed = 0;
        int blocked = 0;
        int natives = 0;
        int javaFiles = 0;
        for (M3GitTreeEntry entry : snapshot.entries()) {
            if (M3SourceFamily.classify(entry.path()) != M3SourceFamily.JAVA) {
                continue;
            }
            javaFiles++;
            if (!entry.objectType().equals("blob")) {
                blocked++;
                dispositions.add(statusRow(entry, "BLOCKED", 0, 0, "JAVA_GITLINK_NOT_A_SOURCE_BLOB"));
                continue;
            }
            M3JavaSemanticExtractor.ParseResult parsedSource = M3JavaSemanticExtractor.extract(
                    entry.path(), snapshot.readBlob(entry));
            if (!parsedSource.complete()) {
                blocked++;
                dispositions.add(statusRow(entry, "BLOCKED", 0, 0, parsedSource.status()));
                continue;
            }
            parsed++;
            int nativeCount = 0;
            for (M3JavaSemanticExtractor.Atom atom : parsedSource.atoms()) {
                if (!identities.add(atom.atomId())) {
                    throw new IllegalStateException("duplicate semantic atom identity " + atom.atomId());
                }
                if (atom.kind() == M3AtomKind.JAVA_NATIVE_DECLARATION) {
                    natives++;
                    nativeCount++;
                }
                atoms.add(atom);
            }
            dispositions.add(statusRow(entry, "PASS", parsedSource.atoms().size(), nativeCount,
                    "JAVAC_PARSE_ONLY_NO_JNI_ABI_CLAIM"));
        }
        atoms.sort(Comparator.comparing(M3JavaSemanticExtractor.Atom::atomId));
        dispositions.sort(Comparator.naturalOrder());
        List<String> atomRows = new ArrayList<>(atoms.size());
        List<String> evidenceRows = new ArrayList<>(atoms.size() * 15);
        for (M3JavaSemanticExtractor.Atom atom : atoms) {
            atomRows.add(String.join("\t", atom.atomId(), atom.sourcePath(), atom.kind().name(),
                    atom.owner(), atom.signature(), atom.sourceSha256(), "UNKNOWN_PATTERN", CONTRACT_REF, "true"));
            EnumSet<M3CoverageGate> required = M3CoverageGate.requiredFor(atom.kind());
            for (M3CoverageGate gate : required) {
                boolean verified = gate == M3CoverageGate.INVENTORIED || gate == M3CoverageGate.SOURCE_IDENTITY;
                evidenceRows.add(String.join("\t", atom.atomId(), gate.name(),
                        verified ? "PASS" : "NOT_RUN",
                        verified ? "git-commit:" + snapshot.commitId() + ":" + atom.sourcePath()
                                + "#utf16=" + atom.startOffset() + "-" + atom.endOffset() : "-",
                        verified ? atom.sourceSha256() : "-",
                        verified ? "javac parse-only AST and exact UTF-8 source slice"
                                : "not executed by the Java semantic census"));
            }
        }
        evidenceRows.sort(Comparator.naturalOrder());
        String atomsText = table(M3AtomCoverageLedger.ATOMS_HEADER, atomRows);
        String evidenceText = table(M3AtomCoverageLedger.EVIDENCE_HEADER, evidenceRows);
        String statusText = table(PARSE_STATUS_HEADER, dispositions);
        String receiptText = table(RECEIPT_HEADER, List.of(
                "schema\t" + SCHEMA,
                "source_commit\t" + snapshot.commitId(),
                "source_tree\t" + snapshot.treeId(),
                "java_files\t" + javaFiles,
                "parsed_files\t" + parsed,
                "blocked_files\t" + blocked,
                "semantic_atoms\t" + atoms.size(),
                "java_native_declarations\t" + natives,
                "atoms_sha256\t" + sha256(atomsText),
                "evidence_sha256\t" + sha256(evidenceText),
                "parse_status_sha256\t" + sha256(statusText),
                "java_complete\t" + (blocked == 0 && javaFiles > 0),
                "admission\tHOLD"));
        Path output = destination.toAbsolutePath().normalize();
        Files.createDirectories(output);
        Path atomsPath = output.resolve("JAVA_ATOMS.tsv");
        Path evidencePath = output.resolve("JAVA_EVIDENCE.tsv");
        Path statusPath = output.resolve("JAVA_PARSE_STATUS.tsv");
        Path receiptPath = output.resolve("JAVA_SEMANTIC_RECEIPT.tsv");
        writeAtomically(atomsPath, atomsText);
        writeAtomically(evidencePath, evidenceText);
        writeAtomically(statusPath, statusText);
        writeAtomically(receiptPath, receiptText);
        if (!atoms.isEmpty()) {
            M3AtomCoverageLedger checked = M3AtomCoverageLedger.read(atomsPath, evidencePath);
            if (checked.atoms().size() != atoms.size() || checked.evidenceCount() != evidenceRows.size()) {
                throw new IllegalStateException("Java semantic ledger strict readback mismatch");
            }
            if (checked.admittedAtomCount() != 0) {
                throw new IllegalStateException("parse-only Java semantic atoms cannot be admitted");
            }
        }
        return new Result(snapshot.commitId(), snapshot.treeId(), javaFiles, parsed, blocked, atoms.size(),
                natives, atomsPath, evidencePath, statusPath, receiptPath);
    }

    private static String statusRow(M3GitTreeEntry entry, String status, int atoms, int natives, String detail) {
        return String.join("\t", entry.path(), entry.sourceSha256(), status, Integer.toString(atoms),
                Integer.toString(natives), detail);
    }

    private static String table(String header, List<String> rows) {
        StringBuilder text = new StringBuilder(header.length() + 1 + rows.size() * 200);
        text.append(header).append('\n');
        for (String row : rows) {
            text.append(row).append('\n');
        }
        return text.toString();
    }

    private static void writeAtomically(Path path, String text) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), "." + path.getFileName(), ".tmp");
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

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 required by JDK", exception);
        }
    }

    /**
     * Deterministic Java-only supplement receipt.
     *
     * @param commitId exact committed source
     * @param treeId exact root tree
     * @param javaFiles counted committed Java files
     * @param parsedFiles successfully parsed files
     * @param blockedFiles refused files
     * @param atoms explicit source atoms
     * @param nativeDeclarations Java {@code native} declarations (not JNI export proof)
     * @param atomsPath semantic atom ledger
     * @param evidencePath explicit proof ledger
     * @param statusPath complete Java-file parse disposition ledger
     * @param receiptPath immutable summary path
     */
    public record Result(String commitId, String treeId, int javaFiles, int parsedFiles, int blockedFiles,
            int atoms, int nativeDeclarations, Path atomsPath, Path evidencePath, Path statusPath,
            Path receiptPath) {
    }
}
