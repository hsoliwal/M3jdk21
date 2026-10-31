/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Command-line validation and fail-closed admission for M3 atom-coverage ledgers. */
public final class M3AtomCoverageVerifier {
    private M3AtomCoverageVerifier() {
    }

    /** Modes differ only in whether incomplete evidence is reported or refused. */
    public enum Mode {
        /** Validate ledger integrity and report incomplete evidence without admitting it. */
        VALIDATE,
        /** Validate ledger integrity and refuse any incomplete or unsuccessful required proof. */
        ADMIT
    }

    /**
     * CLI: {@code M3AtomCoverageVerifier <validate|admit> <ATOMS.tsv> <EVIDENCE.tsv>}.
     *
     * <p>{@code validate} verifies schema, ordering, identities and references while preserving explicit pending
     * states. {@code admit} additionally requires every atom's complete kind-specific gate profile.</p>
     *
     * @param args mode, atom ledger and evidence ledger
     * @throws IOException when an input cannot be read
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("usage: <validate|admit> <ATOMS.tsv> <EVIDENCE.tsv>");
        }
        Mode mode;
        try {
            mode = Mode.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("mode must be validate or admit", exception);
        }
        verify(mode, Path.of(args[1]), Path.of(args[2]), System.out);
    }

    /**
     * Validates the ledgers and emits one deterministic summary plus any admission findings.
     *
     * @param mode validation or strict admission mode
     * @param atoms atom-ledger path
     * @param evidence evidence-ledger path
     * @param output destination for the deterministic report
     * @return validated immutable ledger
     * @throws IOException when an input cannot be read
     * @throws IllegalStateException when strict admission finds any blocker
     */
    public static M3AtomCoverageLedger verify(Mode mode, Path atoms, Path evidence, PrintStream output)
            throws IOException {
        M3AtomCoverageLedger ledger = M3AtomCoverageLedger.read(atoms, evidence);
        List<String> findings = ledger.admissionFindings();
        output.printf(Locale.ROOT,
                "M3_ATOM_COVERAGE\tmode=%s\tatoms=%d\tevidence=%d\tadmitted=%d\tpending=%d\tsemantic_sha256=%s%n",
                mode, ledger.atoms().size(), ledger.evidenceCount(), ledger.admittedAtomCount(),
                ledger.atoms().size() - ledger.admittedAtomCount(), ledger.semanticSha256());
        findings.forEach(finding -> output.println("FINDING\t" + finding));
        if (mode == Mode.ADMIT && !findings.isEmpty()) {
            throw new IllegalStateException("atom coverage admission refused: " + findings.size() + " finding(s)");
        }
        return ledger;
    }
}
