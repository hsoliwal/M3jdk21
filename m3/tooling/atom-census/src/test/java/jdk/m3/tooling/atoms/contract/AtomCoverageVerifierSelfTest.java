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
public final class AtomCoverageVerifierSelfTest {
    private static final String HASH_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    private int checks;

    private AtomCoverageVerifierSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        String contract = System.getProperty("m3.contract.dir");
        int checks = runAll(contract == null ? null : Path.of(contract));
        System.out.println("M3_ATOM_COVERAGE_SELF_TEST\tchecks=" + checks + "\tstatus=PASS");
    }

    /** Runs all dependency-free checks and optionally verifies the committed contract tables. */
    public static int runAll(Path contractDirectory) throws Exception {
        AtomCoverageVerifierSelfTest suite = new AtomCoverageVerifierSelfTest();
        suite.run(contractDirectory);
        return suite.checks;
    }

    private void run(Path contractDirectory) throws Exception {
        Path directory = Files.createTempDirectory("m3-atom-coverage-");
        try {
            validLedgerValidatesAndAdmits(directory);
            pendingLedgerValidatesButAdmissionRefuses(directory);
            optionalFailedGateIsPending(directory);
            optionalUnexecutedGateDoesNotBlock(directory);
            unknownPatternValidatesButAdmissionRefuses(directory);
            atomRowsMustBeSorted(directory);
            evidenceMustReferenceAnAtom(directory);
            pathTraversalIsRefused(directory);
            windowsAbsolutePathIsRefused(directory);
            windowsDriveRelativePathIsRefused(directory);
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
        String validate = verify(AtomCoverageVerifier.Mode.VALIDATE, fixture);
        check(validate.contains("atoms=2") && validate.contains("pending=0"), "valid summary");
        String admit = verify(AtomCoverageVerifier.Mode.ADMIT, fixture);
        check(admit.contains("admitted=2") && !admit.contains("FINDING"), "valid admission");
    }

    private void pendingLedgerValidatesButAdmissionRefuses(Path directory) throws Exception {
        Fixture fixture = writeFixture(directory.resolve("pending"), atomRows(false, false), evidenceRows(true, false));
        String validate = verify(AtomCoverageVerifier.Mode.VALIDATE, fixture);
        check(validate.contains("pending=1") && validate.contains("NOT_RUN"), "pending is explicit");
        expect(IllegalStateException.class,
                () -> verify(AtomCoverageVerifier.Mode.ADMIT, fixture), "pending admission refusal");
    }


    /** Optional FAIL is a recorded failure; it must not inflate the admitted count. */
    private void optionalFailedGateIsPending(Path directory) throws Exception {
        List<String> rows = new ArrayList<>(evidenceRows(false, false));
        rows.add(String.join("\t", "ATOM-A", CoverageGate.CONTRACT_CHANGE_AUTHORIZED.name(),
                GateStatus.FAIL.name(), "evidence/ATOM-A/optional-failure.txt", HASH_A,
                "explicit failed optional gate"));
        rows.subList(1, rows.size()).sort(Comparator.naturalOrder());
        Fixture fixture = writeFixture(directory.resolve("optional-failed"), atomRows(false, false), rows);
        String validate = verify(AtomCoverageVerifier.Mode.VALIDATE, fixture);
        check(validate.contains("admitted=1\tpending=1"), "optional FAIL reduces admitted count");
        check(validate.contains("FINDING\tATOM-A\tRECORDED_FAIL\tCONTRACT_CHANGE_AUTHORIZED"),
                "optional FAIL is reported");
        expect(IllegalStateException.class,
                () -> verify(AtomCoverageVerifier.Mode.ADMIT, fixture), "optional FAIL refuses admission");
    }

    /** An optional NOT_RUN alone must not pretend that a required proof is missing. */
    private void optionalUnexecutedGateDoesNotBlock(Path directory) throws Exception {
        List<String> rows = new ArrayList<>(evidenceRows(false, false));
        rows.add(String.join("\t", "ATOM-A", CoverageGate.CONTRACT_CHANGE_AUTHORIZED.name(),
                GateStatus.NOT_RUN.name(), "-", "-", "optional gate not executed"));
        rows.subList(1, rows.size()).sort(Comparator.naturalOrder());
        Fixture fixture = writeFixture(directory.resolve("optional-not-run"), atomRows(false, false), rows);
        String admit = verify(AtomCoverageVerifier.Mode.ADMIT, fixture);
        check(admit.contains("admitted=2\tpending=0") && !admit.contains("FINDING"),
                "optional NOT_RUN preserves complete required proof");
    }

    private void unknownPatternValidatesButAdmissionRefuses(Path directory) throws Exception {
        Fixture fixture = writeFixture(directory.resolve("unknown-pattern"), atomRows(false, true),
                evidenceRows(false, false));
        String validate = verify(AtomCoverageVerifier.Mode.VALIDATE, fixture);
        check(validate.contains("UNKNOWN_PATTERN"), "unknown pattern is reported");
        expect(IllegalStateException.class,
                () -> verify(AtomCoverageVerifier.Mode.ADMIT, fixture), "unknown pattern admission refusal");
    }

    private void atomRowsMustBeSorted(Path directory) throws Exception {
        Fixture fixture = writeFixture(directory.resolve("unsorted"), atomRows(true, false),
                evidenceRows(false, false));
        expect(IllegalArgumentException.class,
                () -> verify(AtomCoverageVerifier.Mode.VALIDATE, fixture), "atom sort refusal");
    }

    private void evidenceMustReferenceAnAtom(Path directory) throws Exception {
        List<String> evidence = new ArrayList<>(evidenceRows(false, false));
        evidence.set(1, evidence.get(1).replace("ATOM-A", "ATOM-0"));
        Fixture fixture = writeFixture(directory.resolve("orphan-evidence"), atomRows(false, false), evidence);
        expect(IllegalArgumentException.class,
                () -> verify(AtomCoverageVerifier.Mode.VALIDATE, fixture), "orphan evidence refusal");
    }

    private void pathTraversalIsRefused(Path directory) throws Exception {
        List<String> atoms = new ArrayList<>(atomRows(false, false));
        atoms.set(1, atoms.get(1).replace("src/A.java", "../src/A.java"));
        Fixture fixture = writeFixture(directory.resolve("path-traversal"), atoms, evidenceRows(false, false));
        expect(IllegalArgumentException.class,
                () -> verify(AtomCoverageVerifier.Mode.VALIDATE, fixture), "path traversal refusal");
    }


    private void windowsAbsolutePathIsRefused(Path directory) throws Exception {
        List<String> atoms = new ArrayList<>(atomRows(false, false));
        atoms.set(1, atoms.get(1).replace("src/A.java", "C:/src/A.java"));
        Fixture fixture = writeFixture(directory.resolve("absolute-path"), atoms, evidenceRows(false, false));
        expect(IllegalArgumentException.class,
                () -> verify(AtomCoverageVerifier.Mode.VALIDATE, fixture), "Windows absolute path refusal");
    }

    /** Drive-relative Windows paths are not repository-relative, even without a slash. */
    private void windowsDriveRelativePathIsRefused(Path directory) throws Exception {
        for (String path : List.of("C:src/A.java", "Z:")) {
            List<String> atoms = new ArrayList<>(atomRows(false, false));
            atoms.set(1, atoms.get(1).replace("src/A.java", path));
            Fixture fixture = writeFixture(directory.resolve("drive-relative-" + path.length()),
                    atoms, evidenceRows(false, false));
            expect(IllegalArgumentException.class,
                    () -> verify(AtomCoverageVerifier.Mode.VALIDATE, fixture),
                    "drive-relative repository source path refusal " + path);
        }
    }

    private void carriageReturnInputIsRefused(Path directory) throws Exception {
        Fixture fixture = writeFixture(directory.resolve("crlf"), atomRows(false, false), evidenceRows(false, false));
        String text = Files.readString(fixture.atoms(), StandardCharsets.UTF_8).replace("\n", "\r\n");
        Files.writeString(fixture.atoms(), text, StandardCharsets.UTF_8);
        expect(IllegalArgumentException.class,
                () -> verify(AtomCoverageVerifier.Mode.VALIDATE, fixture), "CRLF refusal");
    }

    private void passRequiresHashBoundEvidence(Path directory) throws Exception {
        List<String> evidence = new ArrayList<>(evidenceRows(false, false));
        evidence.set(1, evidence.get(1).replace("evidence/ATOM-A/CONTRACT_BOUND.txt\t" + HASH_B,
                "-\t-"));
        Fixture fixture = writeFixture(directory.resolve("missing-pass-evidence"), atomRows(false, false), evidence);
        expect(IllegalArgumentException.class,
                () -> verify(AtomCoverageVerifier.Mode.VALIDATE, fixture), "missing PASS evidence refusal");
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
                () -> verify(AtomCoverageVerifier.Mode.VALIDATE, fixture), "N/A rationale refusal");
    }

    private void uppercaseSourceDigestIsRefused(Path directory) throws Exception {
        List<String> atoms = new ArrayList<>(atomRows(false, false));
        atoms.set(1, atoms.get(1).replace(HASH_A, HASH_A.toUpperCase(java.util.Locale.ROOT)));
        Fixture fixture = writeFixture(directory.resolve("uppercase-hash"), atoms, evidenceRows(false, false));
        expect(IllegalArgumentException.class,
                () -> verify(AtomCoverageVerifier.Mode.VALIDATE, fixture), "uppercase digest refusal");
    }


    private void contractUnlockRequiresExplicitAuthorization(Path directory) throws Exception {
        List<String> atoms = new ArrayList<>(atomRows(false, false));
        atoms.set(2, atoms.get(2).substring(0, atoms.get(2).lastIndexOf('\t') + 1) + "false");
        Fixture blocked = writeFixture(directory.resolve("unlocked-without-authorization"), atoms,
                evidenceRows(false, false));
        String validate = verify(AtomCoverageVerifier.Mode.VALIDATE, blocked);
        check(validate.contains("MISSING_GATE\tCONTRACT_CHANGE_AUTHORIZED"),
                "unlocked contract missing authorization is reported");
        expect(IllegalStateException.class,
                () -> verify(AtomCoverageVerifier.Mode.ADMIT, blocked), "unlocked contract admission refusal");

        List<String> disclaimedEvidence = new ArrayList<>(evidenceRows(false, false));
        disclaimedEvidence.add(String.join("\t", "ATOM-B",
                CoverageGate.CONTRACT_CHANGE_AUTHORIZED.name(), GateStatus.NOT_APPLICABLE.name(),
                "evidence/ATOM-B/contract-change-review.md", HASH_B,
                "no explicit permission to change contract"));
        disclaimedEvidence.subList(1, disclaimedEvidence.size()).sort(Comparator.naturalOrder());
        Fixture disclaimed = writeFixture(directory.resolve("unlocked-not-applicable"), atoms, disclaimedEvidence);
        String disclaimedSummary = verify(AtomCoverageVerifier.Mode.VALIDATE, disclaimed);
        check(disclaimedSummary.contains("admitted=1\tpending=1"),
                "NOT_APPLICABLE cannot authorize an unlocked public contract");
        check(disclaimedSummary.contains("FINDING\tATOM-B\tNOT_APPLICABLE\tCONTRACT_CHANGE_AUTHORIZED"),
                "NOT_APPLICABLE authorization is a recorded blocker");
        expect(IllegalStateException.class,
                () -> verify(AtomCoverageVerifier.Mode.ADMIT, disclaimed),
                "unlocked NOT_APPLICABLE admission refusal");
        Fixture lockedOptional = writeFixture(directory.resolve("locked-optional-not-applicable"),
                atomRows(false, false), disclaimedEvidence);
        check(verify(AtomCoverageVerifier.Mode.ADMIT, lockedOptional).contains("admitted=2\tpending=0"),
                "non-required NOT_APPLICABLE remains nonblocking for a locked contract");

        List<String> authorizedEvidence = new ArrayList<>(evidenceRows(false, false));
        authorizedEvidence.add(String.join("\t", "ATOM-B", CoverageGate.CONTRACT_CHANGE_AUTHORIZED.name(),
                GateStatus.PASS.name(), "evidence/ATOM-B/contract-change.md", HASH_B,
                "explicit reviewed contract-change authorization"));
        authorizedEvidence.subList(1, authorizedEvidence.size()).sort(Comparator.naturalOrder());
        Fixture authorized = writeFixture(directory.resolve("unlocked-authorized"), atoms, authorizedEvidence);
        String admit = verify(AtomCoverageVerifier.Mode.ADMIT, authorized);
        check(admit.contains("pending=0"), "explicit contract-change authorization admits");
    }

    private void contractTablesMatchTheImplementation(Path contractDirectory) throws Exception {
        List<String> profiles = new ArrayList<>();
        profiles.add("kind\texecutable\tjava_atom\tnative_atom\trequired_gates");
        for (AtomKind kind : AtomKind.values()) {
            String gates = CoverageGate.requiredFor(kind).stream().map(Enum::name)
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
        check(String.join("\t", atomColumns).equals(AtomCoverageLedger.ATOMS_HEADER),
                "atom schema matches parser header");
        check(String.join("\t", evidenceColumns).equals(AtomCoverageLedger.EVIDENCE_HEADER),
                "evidence schema matches parser header");

        List<String> statuses = Files.readAllLines(contractDirectory.resolve("STATUS_CONTRACT.tsv"),
                StandardCharsets.UTF_8);
        List<String> statusNames = statuses.stream().skip(1).map(line -> line.split("\t", -1)[0]).toList();
        check(statuses.get(0).equals("status\tadmissible\tevidence_required\tnote_required\tmeaning")
                && statusNames.equals(java.util.Arrays.stream(GateStatus.values()).map(Enum::name).toList()),
                "status table covers the closed enum in order");
    }

    private static List<String> atomRows(boolean reverse, boolean unknownPattern) {
        String patternA = unknownPattern ? "UNKNOWN_PATTERN" : "DECLARATION_CONTRACT";
        String a = String.join("\t", "ATOM-A", "src/A.java", "DECLARATION_ONLY", "owner-a", "A",
                HASH_A, patternA, "docs/contracts/A.md", "true");
        String b = String.join("\t", "ATOM-B", "src/B.java", "JAVA_METHOD", "owner-b", "run()int",
                HASH_B, "ARITHMETIC_EXACT", "docs/contracts/B.md", "true");
        return reverse
                ? List.of(AtomCoverageLedger.ATOMS_HEADER, b, a)
                : List.of(AtomCoverageLedger.ATOMS_HEADER, a, b);
    }

    private static List<String> evidenceRows(boolean pending, boolean useNotApplicable) {
        List<String> rows = new ArrayList<>();
        rows.add(AtomCoverageLedger.EVIDENCE_HEADER);
        appendEvidence(rows, "ATOM-A", AtomKind.DECLARATION_ONLY, pending, useNotApplicable);
        appendEvidence(rows, "ATOM-B", AtomKind.JAVA_METHOD, false, useNotApplicable);
        rows.subList(1, rows.size()).sort(Comparator.naturalOrder());
        return rows;
    }

    private static void appendEvidence(List<String> rows, String atomId, AtomKind kind, boolean pending,
            boolean useNotApplicable) {
        EnumSet<CoverageGate> required = CoverageGate.requiredFor(kind);
        for (CoverageGate gate : required) {
            GateStatus status = pending && gate == CoverageGate.CORRECTNESS_TESTED
                    ? GateStatus.NOT_RUN : GateStatus.PASS;
            String note = status == GateStatus.NOT_RUN ? "fixture intentionally pending" : "verified fixture";
            if (useNotApplicable && atomId.equals("ATOM-A") && gate == CoverageGate.CORRECTNESS_TESTED) {
                status = GateStatus.NOT_APPLICABLE;
                note = "declaration has no executable behavior";
            }
            String evidenceRef = status == GateStatus.NOT_RUN ? "-" : "evidence/" + atomId + "/" + gate + ".txt";
            String evidenceHash = status == GateStatus.NOT_RUN ? "-" : HASH_B;
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

    private static String verify(AtomCoverageVerifier.Mode mode, Fixture fixture) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            AtomCoverageVerifier.verify(mode, fixture.atoms(), fixture.evidence(), output);
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
