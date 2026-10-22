// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Emits the Java-grade qualification bound to verifier-supplied proof-stage fingerprints. */
public final class NebulaM3JavaGradeQualificationCli {
    private NebulaM3JavaGradeQualificationCli() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "usage: NebulaM3JavaGradeQualificationCli "
                            + "<nebula-commit> <synexia-commit> <dag-root> "
                            + "<evidence-tsv> <output-tsv>");
        }
        var proof = new NebulaM3ProofGate.Receipt(
                args[0], args[1], args[2],
                NebulaM3ProofReceiptCli.readEvidence(Path.of(args[3]).toAbsolutePath().normalize()));
        var receipt = NebulaM3JavaGradeQualification.fromProof(proof);
        if (!receipt.qualified()
                || !"MANUAL_TRANSFER_REVIEW_REQUIRED".equals(receipt.disposition())
                || receipt.mutationAuthority()
                || receipt.promotionAuthority()) {
            throw new IllegalStateException("Java-grade qualification is incomplete");
        }
        Path output = Path.of(args[4]).toAbsolutePath().normalize();
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.writeString(output, receipt.toTsv(), StandardCharsets.UTF_8);
        System.out.println("NEBULA_M3_JAVA_GRADE_ROOT=" + receipt.root());
    }
}
