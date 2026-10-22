// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NebulaM3ProofReceiptCliTest {
    private static final String NEBULA = "b".repeat(40);
    private static final String SYNEXIA = "c".repeat(40);
    private static final String DAG = "a".repeat(64);

    @TempDir Path temp;

    @Test
    void allPassedEvidenceEmitsBoundManualReviewReceipt() throws Exception {
        Path evidence = temp.resolve("evidence.tsv");
        Path output = temp.resolve("qualified/transfer-qualification.tsv");
        Files.writeString(evidence, passedEvidence());

        NebulaM3ProofReceiptCli.main(
                new String[] {
                    NEBULA, SYNEXIA, DAG, evidence.toString(), output.toString()
                });

        String receipt = Files.readString(output);
        assertEquals(NebulaM3ProofGate.Stage.values().length + 1, receipt.lines().count());
        assertTrue(receipt.contains(
                "NEBULA_M3_PILOT_PROOF_V1\t" + NEBULA + "\t" + SYNEXIA + "\t" + DAG));
        assertTrue(receipt.lines().skip(1).allMatch(
                line -> line.contains("\tPASSED\t")
                        && line.contains("\tMANUAL_REVIEW_REQUIRED\tfalse\tfalse\t")));
        assertFalse(receipt.contains("\tHOLD\t"));
    }

    @Test
    void incompleteEvidenceCannotEmitFinalReceipt() throws Exception {
        Path evidence = temp.resolve("held.tsv");
        Path output = temp.resolve("held-output.tsv");
        String held = passedEvidence().replaceFirst(
                "PATTERN_IOP\tPASSED\t[0-9a-f]{64}",
                "PATTERN_IOP\tHOLD\t-");
        Files.writeString(evidence, held);

        assertThrows(
                IllegalStateException.class,
                () ->
                        NebulaM3ProofReceiptCli.main(
                                new String[] {
                                    NEBULA,
                                    SYNEXIA,
                                    DAG,
                                    evidence.toString(),
                                    output.toString()
                                }));
        assertFalse(Files.exists(output));
    }

    @Test
    void reorderedMissingAndMalformedEvidenceFailClosed() throws Exception {
        Path evidence = temp.resolve("bad.tsv");
        var rows = passedEvidence().lines().toList();

        String reordered =
                String.join(
                                "\n",
                                rows.get(0),
                                rows.get(2),
                                rows.get(1),
                                String.join("\n", rows.subList(3, rows.size())))
                        + "\n";
        Files.writeString(evidence, reordered);
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaM3ProofReceiptCli.readEvidence(evidence));

        Files.writeString(evidence, String.join("\n", rows.subList(0, rows.size() - 1)) + "\n");
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaM3ProofReceiptCli.readEvidence(evidence));

        Files.writeString(
                evidence,
                passedEvidence().replaceFirst("[0-9a-f]{64}", "D".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaM3ProofReceiptCli.readEvidence(evidence));
    }

    private static String passedEvidence() {
        StringBuilder out = new StringBuilder(NebulaM3ProofReceiptCli.EVIDENCE_HEADER).append('\n');
        int ordinal = 1;
        for (var stage : NebulaM3ProofGate.Stage.values()) {
            out.append(stage)
                    .append("\tPASSED\t")
                    .append("%064x".formatted(ordinal++))
                    .append('\n');
        }
        return out.toString();
    }
}
