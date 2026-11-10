/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Strict parser and immutable view of the atom and evidence ledgers. */
public final class M3AtomCoverageLedger {
    /** Exact header required for {@code ATOMS.tsv}. */
    public static final String ATOMS_HEADER = "atom_id\tsource_path\tkind\towner\tsignature\tsource_sha256"
            + "\tpattern_id\tcontract_ref\tpublic_contract_locked";
    /** Exact header required for {@code EVIDENCE.tsv}. */
    public static final String EVIDENCE_HEADER = "atom_id\tgate\tstatus\tevidence_ref\tevidence_sha256\tnote";

    private static final Pattern ATOM_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:/@+\\-]{0,255}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern SAFE_TOKEN = Pattern.compile("[^\\p{Cntrl}\\t]+");

    private final Map<String, M3AtomRecord> atoms;
    private final Map<String, Map<M3CoverageGate, M3EvidenceRecord>> evidence;
    private final int evidenceCount;
    private final String semanticSha256;

    private M3AtomCoverageLedger(Map<String, M3AtomRecord> atoms,
            Map<String, Map<M3CoverageGate, M3EvidenceRecord>> evidence,
            int evidenceCount,
            String semanticSha256) {
        this.atoms = Collections.unmodifiableMap(atoms);
        this.evidence = Collections.unmodifiableMap(evidence);
        this.evidenceCount = evidenceCount;
        this.semanticSha256 = semanticSha256;
    }

    /**
     * Reads and validates both ledgers without interpreting incomplete evidence as success.
     *
     * @param atomsPath path to {@code ATOMS.tsv}
     * @param evidencePath path to {@code EVIDENCE.tsv}
     * @return immutable validated ledger
     * @throws IOException when either file cannot be read as UTF-8
     * @throws IllegalArgumentException when a schema, ordering, identity or reference invariant fails
     */
    public static M3AtomCoverageLedger read(Path atomsPath, Path evidencePath) throws IOException {
        Objects.requireNonNull(atomsPath, "atomsPath");
        Objects.requireNonNull(evidencePath, "evidencePath");
        List<String> atomLines = readStrictLines(atomsPath, ATOMS_HEADER);
        List<String> evidenceLines = readStrictLines(evidencePath, EVIDENCE_HEADER);

        Map<String, M3AtomRecord> atoms = new TreeMap<>();
        String previousAtomId = null;
        for (int lineNumber = 2; lineNumber <= atomLines.size(); lineNumber++) {
            String line = atomLines.get(lineNumber - 1);
            String[] cells = cells(line, 9, atomsPath, lineNumber);
            String atomId = requireAtomId(cells[0], atomsPath, lineNumber);
            if (previousAtomId != null && previousAtomId.compareTo(atomId) >= 0) {
                throw invalid(atomsPath, lineNumber, "atom rows must be strictly sorted by atom_id");
            }
            previousAtomId = atomId;
            M3AtomRecord record = new M3AtomRecord(
                    atomId,
                    requireRelativePath(cells[1], atomsPath, lineNumber),
                    enumValue(M3AtomKind.class, cells[2], atomsPath, lineNumber),
                    requireToken("owner", cells[3], atomsPath, lineNumber),
                    requireToken("signature", cells[4], atomsPath, lineNumber),
                    requireSha("source_sha256", cells[5], atomsPath, lineNumber),
                    requireToken("pattern_id", cells[6], atomsPath, lineNumber),
                    requireToken("contract_ref", cells[7], atomsPath, lineNumber),
                    strictBoolean(cells[8], atomsPath, lineNumber));
            if (atoms.put(atomId, record) != null) {
                throw invalid(atomsPath, lineNumber, "duplicate atom_id " + atomId);
            }
        }
        if (atoms.isEmpty()) {
            throw invalid(atomsPath, 1, "at least one atom row is required");
        }

        Map<String, Map<M3CoverageGate, M3EvidenceRecord>> evidence = new TreeMap<>();
        String previousEvidenceKey = null;
        int evidenceCount = 0;
        for (int lineNumber = 2; lineNumber <= evidenceLines.size(); lineNumber++) {
            String line = evidenceLines.get(lineNumber - 1);
            String[] cells = cells(line, 6, evidencePath, lineNumber);
            String atomId = requireAtomId(cells[0], evidencePath, lineNumber);
            if (!atoms.containsKey(atomId)) {
                throw invalid(evidencePath, lineNumber, "evidence references unknown atom_id " + atomId);
            }
            M3CoverageGate gate = enumValue(M3CoverageGate.class, cells[1], evidencePath, lineNumber);
            M3GateStatus status = enumValue(M3GateStatus.class, cells[2], evidencePath, lineNumber);
            String evidenceKey = atomId + '\t' + gate.name();
            if (previousEvidenceKey != null && previousEvidenceKey.compareTo(evidenceKey) >= 0) {
                throw invalid(evidencePath, lineNumber,
                        "evidence rows must be strictly sorted by atom_id then gate");
            }
            previousEvidenceKey = evidenceKey;
            String evidenceRef = cells[3];
            String evidenceSha256 = cells[4];
            String note = cells[5];
            validateEvidencePayload(status, evidenceRef, evidenceSha256, note, evidencePath, lineNumber);
            M3EvidenceRecord record = new M3EvidenceRecord(atomId, gate, status, evidenceRef, evidenceSha256, note);
            Map<M3CoverageGate, M3EvidenceRecord> perAtom = evidence.computeIfAbsent(atomId,
                    ignored -> new EnumMap<>(M3CoverageGate.class));
            if (perAtom.put(gate, record) != null) {
                throw invalid(evidencePath, lineNumber, "duplicate evidence key " + evidenceKey);
            }
            evidenceCount++;
        }

        return new M3AtomCoverageLedger(atoms, freezeEvidence(evidence), evidenceCount,
                semanticDigest(atomLines, evidenceLines));
    }

    /**
     * Returns atoms in stable identifier order.
     *
     * @return immutable atom map
     */
    public Map<String, M3AtomRecord> atoms() {
        return atoms;
    }

    /**
     * Returns evidence in stable atom order; each inner map is keyed by gate.
     *
     * @return immutable nested evidence map
     */
    public Map<String, Map<M3CoverageGate, M3EvidenceRecord>> evidence() {
        return evidence;
    }

    /**
     * Returns the total evidence-row count.
     *
     * @return evidence-row count
     */
    public int evidenceCount() {
        return evidenceCount;
    }

    /**
     * Returns the SHA-256 over the exact normalized ledger rows and their boundary marker.
     *
     * @return lowercase semantic SHA-256
     */
    public String semanticSha256() {
        return semanticSha256;
    }

    /**
     * Returns deterministic, human-readable blockers that prevent strict admission.
     *
     * @return immutable ordered findings
     */
    public List<String> admissionFindings() {
        List<String> findings = new ArrayList<>();
        for (M3AtomRecord atom : atoms.values()) {
            if (isUnknownPattern(atom.patternId())) {
                findings.add(atom.atomId() + "\tUNKNOWN_PATTERN\t" + atom.patternId());
            }
            Map<M3CoverageGate, M3EvidenceRecord> perAtom = evidence.getOrDefault(atom.atomId(), Map.of());
            perAtom.values().stream().filter(record -> record.status() == M3GateStatus.FAIL)
                    .filter(record -> !M3CoverageGate.requiredFor(atom.kind()).contains(record.gate()))
                    .forEach(record -> findings.add(atom.atomId() + "\tRECORDED_FAIL\t" + record.gate()));
            java.util.EnumSet<M3CoverageGate> requiredGates = M3CoverageGate.requiredFor(atom.kind());
            if (!atom.publicContractLocked()) {
                requiredGates.add(M3CoverageGate.CONTRACT_CHANGE_AUTHORIZED);
            }
            for (M3CoverageGate required : requiredGates) {
                M3EvidenceRecord record = perAtom.get(required);
                if (record == null) {
                    findings.add(atom.atomId() + "\tMISSING_GATE\t" + required);
                } else if (!record.status().admissible()) {
                    findings.add(atom.atomId() + "\t" + record.status() + "\t" + required);
                }
            }
        }
        return List.copyOf(findings);
    }

    /**
     * Returns the number of atoms whose complete required gate profile is admissible.
     *
     * @return admitted atom count
     */
    public int admittedAtomCount() {
        int admitted = 0;
        for (M3AtomRecord atom : atoms.values()) {
            if (isUnknownPattern(atom.patternId())) {
                continue;
            }
            Map<M3CoverageGate, M3EvidenceRecord> perAtom = evidence.getOrDefault(atom.atomId(), Map.of());
            boolean complete = true;
            java.util.EnumSet<M3CoverageGate> requiredGates = M3CoverageGate.requiredFor(atom.kind());
            if (!atom.publicContractLocked()) {
                requiredGates.add(M3CoverageGate.CONTRACT_CHANGE_AUTHORIZED);
            }
            for (M3CoverageGate gate : requiredGates) {
                M3EvidenceRecord record = perAtom.get(gate);
                if (record == null || !record.status().admissible()) {
                    complete = false;
                    break;
                }
            }
            admitted += complete ? 1 : 0;
        }
        return admitted;
    }

    private static Map<String, Map<M3CoverageGate, M3EvidenceRecord>> freezeEvidence(
            Map<String, Map<M3CoverageGate, M3EvidenceRecord>> mutable) {
        Map<String, Map<M3CoverageGate, M3EvidenceRecord>> frozen = new TreeMap<>();
        mutable.forEach((atomId, rows) -> frozen.put(atomId, Collections.unmodifiableMap(new EnumMap<>(rows))));
        return frozen;
    }

    private static List<String> readStrictLines(Path path, String expectedHeader) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            throw invalid(path, 1, "UTF-8 byte-order marks are not permitted");
        }
        if (text.indexOf('\r') >= 0) {
            throw invalid(path, 1, "carriage returns are not permitted; use LF");
        }
        if (!text.endsWith("\n") || text.endsWith("\n\n")) {
            throw invalid(path, 1, "file must end with exactly one LF");
        }
        List<String> lines = List.of(text.substring(0, text.length() - 1).split("\n", -1));
        if (lines.isEmpty() || !expectedHeader.equals(lines.get(0))) {
            throw invalid(path, 1, "expected exact header: " + expectedHeader);
        }
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).isEmpty()) {
                throw invalid(path, index + 1, "blank lines are not permitted");
            }
        }
        return lines;
    }

    private static String[] cells(String line, int expected, Path path, int lineNumber) {
        String[] cells = line.split("\\t", -1);
        if (cells.length != expected) {
            throw invalid(path, lineNumber, "expected " + expected + " cells, found " + cells.length);
        }
        return cells;
    }

    private static String requireAtomId(String value, Path path, int lineNumber) {
        if (!ATOM_ID.matcher(value).matches()) {
            throw invalid(path, lineNumber, "invalid atom_id " + value);
        }
        return value;
    }

    private static String requireRelativePath(String value, Path path, int lineNumber) {
        requireToken("source_path", value, path, lineNumber);
        boolean windowsAbsolute = value.length() >= 3 && Character.isLetter(value.charAt(0))
                && value.charAt(1) == ':' && value.charAt(2) == '/';
        if (value.startsWith("/") || value.startsWith("\\") || value.contains("\\") || value.endsWith("/")
                || windowsAbsolute) {
            throw invalid(path, lineNumber, "source_path must be a normalized repository-relative path");
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw invalid(path, lineNumber, "source_path contains an empty or traversal segment");
            }
        }
        return value;
    }

    private static String requireToken(String name, String value, Path path, int lineNumber) {
        if (value.isBlank() || !SAFE_TOKEN.matcher(value).matches()) {
            throw invalid(path, lineNumber, name + " must be nonblank printable text without tabs");
        }
        return value;
    }

    private static String requireSha(String name, String value, Path path, int lineNumber) {
        if (!SHA256.matcher(value).matches()) {
            throw invalid(path, lineNumber, name + " must be lowercase SHA-256");
        }
        return value;
    }

    private static boolean strictBoolean(String value, Path path, int lineNumber) {
        if (value.equals("true")) {
            return true;
        }
        if (value.equals("false")) {
            return false;
        }
        throw invalid(path, lineNumber, "public_contract_locked must be true or false");
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, Path path, int lineNumber) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw invalid(path, lineNumber, "invalid " + type.getSimpleName() + " value " + value);
        }
    }

    private static void validateEvidencePayload(M3GateStatus status, String evidenceRef, String evidenceSha256,
            String note, Path path, int lineNumber) {
        boolean missingRef = evidenceRef.equals("-");
        boolean missingSha = evidenceSha256.equals("-");
        if (missingRef != missingSha) {
            throw invalid(path, lineNumber, "evidence_ref and evidence_sha256 must both be present or both be '-'");
        }
        if (!missingRef) {
            requireToken("evidence_ref", evidenceRef, path, lineNumber);
            requireSha("evidence_sha256", evidenceSha256, path, lineNumber);
        }
        if (status.admissible() || status == M3GateStatus.FAIL) {
            if (missingRef) {
                throw invalid(path, lineNumber, status + " requires hash-bound evidence");
            }
        }
        if (status == M3GateStatus.NOT_APPLICABLE && note.isBlank()) {
            throw invalid(path, lineNumber, "NOT_APPLICABLE requires a rationale in note");
        }
        if (status == M3GateStatus.BLOCKED && note.isBlank()) {
            throw invalid(path, lineNumber, "BLOCKED requires a rationale in note");
        }
        if (!note.isEmpty() && !SAFE_TOKEN.matcher(note).matches()) {
            throw invalid(path, lineNumber, "note contains a tab or control character");
        }
    }

    private static boolean isUnknownPattern(String patternId) {
        String normalized = patternId.toUpperCase(Locale.ROOT);
        return normalized.equals("UNKNOWN") || normalized.equals("UNKNOWN_PATTERN")
                || normalized.equals("UNCLASSIFIED") || normalized.equals("RESIDUE");
    }

    private static String semanticDigest(List<String> atomLines, List<String> evidenceLines) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "M3-ATOM-COVERAGE/1\n");
            atomLines.forEach(line -> update(digest, line + "\n"));
            update(digest, "--EVIDENCE--\n");
            evidenceLines.forEach(line -> update(digest, line + "\n"));
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the JDK", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
    }

    private static IllegalArgumentException invalid(Path path, int lineNumber, String message) {
        return new IllegalArgumentException(path + ":" + lineNumber + ": " + message);
    }
}
