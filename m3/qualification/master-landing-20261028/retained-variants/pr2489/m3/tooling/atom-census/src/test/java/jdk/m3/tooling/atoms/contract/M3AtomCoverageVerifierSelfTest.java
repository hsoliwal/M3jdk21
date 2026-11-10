/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** Dependency-free regression suite for the ledger parser and admission boundary. */
public final class M3AtomCoverageVerifierSelfTest {
    private static final String HASH_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    private int checks;

    private M3AtomCoverageVerifierSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        String contract = System.getProperty("m3.contract.dir");
        int checks = runAll(contract == null ? null : Path.of(contract));
        System.out.println("M3_ATOM_COVERAGE_SELF_TEST\tchecks=" + checks + "\tstatus=PASS");
    }

    /** Runs all dependency-free checks and optionally verifies the committed contract tables. */
    public static int runAll(Path contractDirectory) throws Exception {
        M3AtomCoverageVerifierSelfTest suite = new M3AtomCoverageVerifierSelfTest();
        suite.run(contractDirectory);
        return suite.checks;
    }

    private void run(Path contractDirectory) throws Exception {
        Path directory = Files.createTempDirectory("m3-atom-coverage-");
        try {
            validLedgerValidatesAndAdmits(directory);
            pendingLedgerValidatesButAdmissionRefuses(directory);
            unknownPatternValidatesButAdmissionRefuses(directory);
            atomRowsMustBeSorted(directory);
            evidenceMustReferenceAnAtom(directory);
            pathTraversalIsRefused(directory);
            windowsAbsolutePathIsRefused(directory);
            carriageReturnInputIsRefused(directory);
            passRequiresHashBoundEvidence(directory);
            notApplicableRequiresRationale(directory);
            uppercaseSourceDigestIsRefused(directory);
            contractUnlockRequiresExplicitAuthorization(directory);
            if (contractDirectory != null) {
                contractTablesMatchTheImplementation(contractDirectory);
            }
        } finally {
            deleteTree(directory);
        }
    }

    private void validLedgerValidatesAndAdmits(Path directory) throws Exception {
        Fixture fixture = writeFixture(directory.resolve("valid"), atomRows(false, false), evidenceRows(false, false));
        String validate = verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture);
        check(validate.contains("atoms=2") && validate.contains("pending=0"), "valid summary");
        String admit = verify(M3AtomCoverageVerifier.Mode.ADMIT, fixture);
        check(admit.contains("admitted=2") && !admit.contains("FINDING"), "valid admission");
    }

    private void pendingLedgerValidatesButAdmissionRefuses(Path directory) throws Exception {
        Fixture fixture = writeFixture(directory.resolve("pending"), atomRows(false, false), evidenceRows(true, false));
        String validate = verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture);
        check(validate.contains("pending=1") && validate.contains("NOT_RUN"), "pending is explicit");
        expect(IllegalStateException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.ADMIT, fixture), "pending admission refusal");
    }

    private void unknownPatternValidatesButAdmissionRefuses(Path directory) throws Exception {
        Fixture fixture = writeFixture(directory.resolve("unknown-pattern"), atomRows(false, true),
                evidenceRows(false, false));
        String validate = verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture);
        check(validate.contains("UNKNOWN_PATTERN"), "unknown pattern is reported");
        expect(IllegalStateException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.ADMIT, fixture), "unknown pattern admission refusal");
    }

    private void atomRowsMustBeSorted(Path directory) throws Exception {
        Fixture fixture = writeFixture(directory.resolve("unsorted"), atomRows(true, false),
                evidenceRows(false, false));
        expect(IllegalArgumentException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture), "atom sort refusal");
    }

    private void evidenceMustReferenceAnAtom(Path directory) throws Exception {
        List<String> evidence = new ArrayList<>(evidenceRows(false, false));
        evidence.set(1, evidence.get(1).replace("ATOM-A", "ATOM-0"));
        Fixture fixture = writeFixture(directory.resolve("orphan-evidence"), atomRows(false, false), evidence);
        expect(IllegalArgumentException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture), "orphan evidence refusal");
    }

    private void pathTraversalIsRefused(Path directory) throws Exception {
        List<String> atoms = new ArrayList<>(atomRows(false, false));
        atoms.set(1, atoms.get(1).replace("src/A.java", "../src/A.java"));
        Fixture fixture = writeFixture(directory.resolve("path-traversal"), atoms, evidenceRows(false, false));
        expect(IllegalArgumentException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture), "path traversal refusal");
    }


    private void windowsAbsolutePathIsRefused(Path directory) throws Exception {
        List<String> atoms = new ArrayList<>(atomRows(false, false));
        atoms.set(1, atoms.get(1).replace("src/A.java", "C:/src/A.java"));
        Fixture fixture = writeFixture(directory.resolve("absolute-path"), atoms, evidenceRows(false, false));
        expect(IllegalArgumentException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture), "Windows absolute path refusal");
    }

    private void carriageReturnInputIsRefused(Path directory) throws Exception {
        Fixture fixture = writeFixture(directory.resolve("crlf"), atomRows(false, false), evidenceRows(false, false));
        String text = Files.readString(fixture.atoms(), StandardCharsets.UTF_8).replace("\n", "\r\n");
        Files.writeString(fixture.atoms(), text, StandardCharsets.UTF_8);
        expect(IllegalArgumentException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture), "CRLF refusal");
    }

    private void passRequiresHashBoundEvidence(Path directory) throws Exception {
        List<String> evidence = new ArrayList<>(evidenceRows(false, false));
        evidence.set(1, evidence.get(1).replace("evidence/ATOM-A/CONTRACT_BOUND.txt\t" + HASH_B,
                "-\t-"));
        Fixture fixture = writeFixture(directory.resolve("missing-pass-evidence"), atomRows(false, false), evidence);
        expect(IllegalArgumentException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture), "missing PASS evidence refusal");
    }

    private void notApplicableRequiresRationale(Path directory) throws Exception {
        List<String> evidence = new ArrayList<>(evidenceRows(false, true));
        for (int index = 1; index < evidence.size(); index++) {
            if (evidence.get(index).contains("\tNOT_APPLICABLE\t")) {
                String[] cells = evidence.get(index).split("\\t", -1);
                cells[5] = "";
                evidence.set(index, String.join("\t", cells));
                break;
            }
        }
        Fixture fixture = writeFixture(directory.resolve("na-without-rationale"), atomRows(false, false), evidence);
        expect(IllegalArgumentException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture), "N/A rationale refusal");
    }

    private void uppercaseSourceDigestIsRefused(Path directory) throws Exception {
        List<String> atoms = new ArrayList<>(atomRows(false, false));
        atoms.set(1, atoms.get(1).replace(HASH_A, HASH_A.toUpperCase(java.util.Locale.ROOT)));
        Fixture fixture = writeFixture(directory.resolve("uppercase-hash"), atoms, evidenceRows(false, false));
        expect(IllegalArgumentException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.VALIDATE, fixture), "uppercase digest refusal");
    }


    private void contractUnlockRequiresExplicitAuthorization(Path directory) throws Exception {
        List<String> atoms = new ArrayList<>(atomRows(false, false));
        atoms.set(2, atoms.get(2).substring(0, atoms.get(2).lastIndexOf('\t') + 1) + "false");
        Fixture blocked = writeFixture(directory.resolve("unlocked-without-authorization"), atoms,
                evidenceRows(false, false));
        String validate = verify(M3AtomCoverageVerifier.Mode.VALIDATE, blocked);
        check(validate.contains("MISSING_GATE\tCONTRACT_CHANGE_AUTHORIZED"),
                "unlocked contract missing authorization is reported");
        expect(IllegalStateException.class,
                () -> verify(M3AtomCoverageVerifier.Mode.ADMIT, blocked), "unlocked contract admission refusal");

        List<String> authorizedEvidence = new ArrayList<>(evidenceRows(false, false));
        authorizedEvidence.add(String.join("\t", "ATOM-B", M3CoverageGate.CONTRACT_CHANGE_AUTHORIZED.name(),
                M3GateStatus.PASS.name(), "evidence/ATOM-B/contract-change.md", HASH_B,
                "explicit reviewed contract-change authorization"));
        authorizedEvidence.subList(1, authorizedEvidence.size()).sort(Comparator.naturalOrder());
        Fixture authorized = writeFixture(directory.resolve("unlocked-authorized"), atoms, authorizedEvidence);
        String admit = verify(M3AtomCoverageVerifier.Mode.ADMIT, authorized);
        check(admit.contains("pending=0"), "explicit contract-change authorization admits");
    }

    private void contractTablesMatchTheImplementation(Path contractDirectory) throws Exception {
        List<String> profiles = new ArrayList<>();
        profiles.add("kind\texecutable\tjava_atom\tnative_atom\trequired_gates");
        for (M3AtomKind kind : M3AtomKind.values()) {
            String gates = M3CoverageGate.requiredFor(kind).stream().map(Enum::name)
                    .collect(java.util.stream.Collectors.joining(","));
            profiles.add(String.join("\t", kind.name(), Boolean.toString(kind.executable()),
                    Boolean.toString(kind.javaAtom()), Boolean.toString(kind.nativeAtom()), gates));
        }
        check(profiles.equals(Files.readAllLines(contractDirectory.resolve("GATE_PROFILES.tsv"),
                StandardCharsets.UTF_8)), "gate profile table fixed point");

        List<String> schema = Files.readAllLines(contractDirectory.resolve("ATOM_COVERAGE_SCHEMA.tsv"),
                StandardCharsets.UTF_8);
        List<String> atomColumns = schema.stream().skip(1).filter(line -> line.startsWith("ATOMS.tsv\t"))
                .map(line -> line.split("\t", -1)[2]).toList();
        List<String> evidenceColumns = schema.stream().skip(1).filter(line -> line.startsWith("EVIDENCE.tsv\t"))
                .map(line -> line.split("\t", -1)[2]).toList();
        check(String.join("\t", atomColumns).equals(M3AtomCoverageLedger.ATOMS_HEADER),
                "atom schema matches parser header");
        check(String.join("\t", evidenceColumns).equals(M3AtomCoverageLedger.EVIDENCE_HEADER),
                "evidence schema matches parser header");

        List<String> statuses = Files.readAllLines(contractDirectory.resolve("STATUS_CONTRACT.tsv"),
                StandardCharsets.UTF_8);
        List<String> statusNames = statuses.stream().skip(1).map(line -> line.split("\t", -1)[0]).toList();
        check(statuses.get(0).equals("status\tadmissible\tevidence_required\tnote_required\tmeaning")
                && statusNames.equals(java.util.Arrays.stream(M3GateStatus.values()).map(Enum::name).toList()),
                "status table covers the closed enum in order");
    }

    private static List<String> atomRows(boolean reverse, boolean unknownPattern) {
        String patternA = unknownPattern ? "UNKNOWN_PATTERN" : "DECLARATION_CONTRACT";
        String a = String.join("\t", "ATOM-A", "src/A.java", "DECLARATION_ONLY", "owner-a", "A",
                HASH_A, patternA, "docs/contracts/A.md", "true");
        String b = String.join("\t", "ATOM-B", "src/B.java", "JAVA_METHOD", "owner-b", "run()int",
                HASH_B, "ARITHMETIC_EXACT", "docs/contracts/B.md", "true");
        return reverse
                ? List.of(M3AtomCoverageLedger.ATOMS_HEADER, b, a)
                : List.of(M3AtomCoverageLedger.ATOMS_HEADER, a, b);
    }

    private static List<String> evidenceRows(boolean pending, boolean useNotApplicable) {
        List<String> rows = new ArrayList<>();
        rows.add(M3AtomCoverageLedger.EVIDENCE_HEADER);
        appendEvidence(rows, "ATOM-A", M3AtomKind.DECLARATION_ONLY, pending, useNotApplicable);
        appendEvidence(rows, "ATOM-B", M3AtomKind.JAVA_METHOD, false, useNotApplicable);
        rows.subList(1, rows.size()).sort(Comparator.naturalOrder());
        return rows;
    }

    private static void appendEvidence(List<String> rows, String atomId, M3AtomKind kind, boolean pending,
            boolean useNotApplicable) {
        EnumSet<M3CoverageGate> required = M3CoverageGate.requiredFor(kind);
        for (M3CoverageGate gate : required) {
            M3GateStatus status = pending && gate == M3CoverageGate.CORRECTNESS_TESTED
                    ? M3GateStatus.NOT_RUN : M3GateStatus.PASS;
            String note = status == M3GateStatus.NOT_RUN ? "fixture intentionally pending" : "verified fixture";
            if (useNotApplicable && atomId.equals("ATOM-A") && gate == M3CoverageGate.CORRECTNESS_TESTED) {
                status = M3GateStatus.NOT_APPLICABLE;
                note = "declaration has no executable behavior";
            }
            String evidenceRef = status == M3GateStatus.NOT_RUN ? "-" : "evidence/" + atomId + "/" + gate + ".txt";
            String evidenceHash = status == M3GateStatus.NOT_RUN ? "-" : HASH_B;
            rows.add(String.join("\t", atomId, gate.name(), status.name(), evidenceRef, evidenceHash, note));
        }
    }

    private static Fixture writeFixture(Path directory, List<String> atoms, List<String> evidence) throws IOException {
        Files.createDirectories(directory);
        Path atomsPath = directory.resolve("ATOMS.tsv");
        Path evidencePath = directory.resolve("EVIDENCE.tsv");
        Files.write(atomsPath, atoms, StandardCharsets.UTF_8);
        Files.write(evidencePath, evidence, StandardCharsets.UTF_8);
        return new Fixture(atomsPath, evidencePath);
    }

    private static String verify(M3AtomCoverageVerifier.Mode mode, Fixture fixture) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            M3AtomCoverageVerifier.verify(mode, fixture.atoms(), fixture.evidence(), output);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
        checks++;
    }

    private void expect(Class<? extends Throwable> expected, CheckedAction action, String name) throws Exception {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                checks++;
                return;
            }
            throw new AssertionError(name + ": expected " + expected.getName() + ", got " + actual, actual);
        }
        throw new AssertionError(name + ": expected " + expected.getName());
    }

    private static void deleteTree(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private record Fixture(Path atoms, Path evidence) {
    }
}
