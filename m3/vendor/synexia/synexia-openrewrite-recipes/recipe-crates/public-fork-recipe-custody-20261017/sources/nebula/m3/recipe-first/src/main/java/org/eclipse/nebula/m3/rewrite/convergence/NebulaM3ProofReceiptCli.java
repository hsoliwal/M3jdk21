// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Emits a bound proof receipt from verifier-supplied stage fingerprints.
 *
 * <p>This CLI does not run tests or authenticate external logs. It only validates the canonical
 * evidence schema, binds exact source/DAG identities and refuses to emit a complete receipt unless
 * every proof stage is PASSED. A complete receipt still requires manual review and carries no
 * mutation or promotion authority.</p>
 */
public final class NebulaM3ProofReceiptCli {
    static final String EVIDENCE_HEADER = "stage\tstate\tproof_sha256";

    private NebulaM3ProofReceiptCli() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "usage: NebulaM3ProofReceiptCli "
                            + "<nebula-commit> <synexia-commit> <dag-root> "
                            + "<evidence-tsv> <output-tsv>");
        }

        Path evidence = Path.of(args[3]).toAbsolutePath().normalize();
        Path output = Path.of(args[4]).toAbsolutePath().normalize();
        var receipt =
                new NebulaM3ProofGate.Receipt(
                        args[0], args[1], args[2], readEvidence(evidence));
        if (!receipt.complete()) {
            throw new IllegalStateException("final proof receipt is incomplete");
        }
        if (!"MANUAL_REVIEW_REQUIRED".equals(receipt.disposition())
                || receipt.mutationAuthority()
                || receipt.promotionAuthority()) {
            throw new IllegalStateException("proof receipt authority drift");
        }

        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(output, receipt.toTsv(), StandardCharsets.UTF_8);
        System.out.println("NEBULA_M3_PROOF_RECEIPT_ROOT=" + receipt.root());
    }

    static List<NebulaM3ProofGate.Evidence> readEvidence(Path evidence) throws IOException {
        List<String> lines = Files.readAllLines(evidence, StandardCharsets.UTF_8);
        if (lines.size() != NebulaM3ProofGate.Stage.values().length + 1
                || !EVIDENCE_HEADER.equals(lines.getFirst())) {
            throw new IllegalArgumentException("invalid proof evidence row count/header");
        }

        var result = new ArrayList<NebulaM3ProofGate.Evidence>();
        for (int index = 0; index < NebulaM3ProofGate.Stage.values().length; index++) {
            String[] cells = lines.get(index + 1).split("\t", -1);
            if (cells.length != 3) {
                throw new IllegalArgumentException("invalid proof evidence row");
            }
            NebulaM3ProofGate.Stage expected = NebulaM3ProofGate.Stage.values()[index];
            NebulaM3ProofGate.Stage stage;
            NebulaM3ProofGate.State state;
            try {
                stage = NebulaM3ProofGate.Stage.valueOf(cells[0]);
                state = NebulaM3ProofGate.State.valueOf(cells[1]);
            } catch (IllegalArgumentException invalidEnum) {
                throw new IllegalArgumentException("invalid proof evidence enum", invalidEnum);
            }
            if (stage != expected) {
                throw new IllegalArgumentException("proof evidence stages must follow canonical order");
            }
            result.add(new NebulaM3ProofGate.Evidence(stage, state, cells[2]));
        }
        return List.copyOf(result);
    }
}
