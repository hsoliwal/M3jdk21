// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** CLI that joins the live JEP residue queue with an admitted Nebula transfer bundle. */
public final class M3JepResiduePlanMain {
    private M3JepResiduePlanMain() {}

    public static void main(String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "usage: M3JepResiduePlanMain <JEP_RESIDUE_QUEUE.tsv> <nebula-transfer-dir> <out-dir>");
        }
        Path queuePath = Path.of(args[0]).normalize();
        Path transferDir = Path.of(args[1]).normalize();
        Path out = Path.of(args[2]).normalize();

        M3JepResidueQueue.Queue queue =
                M3JepResidueQueue.parse(read(queuePath));
        M3NebulaTransferAdmission.Receipt transfer =
                M3NebulaTransferAdmission.read(transferDir);
        M3JepResidueDagPlanner.Plan plan =
                M3JepResidueDagPlanner.plan(queue, transfer);

        try {
            Files.createDirectories(out);
            Files.writeString(
                    out.resolve("nebula-transfer-admission.tsv"),
                    transfer.admissionTsv(),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    out.resolve("jep-recipe-dag-queue.tsv"),
                    plan.tsv(),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    out.resolve("jep-recipe-dag-queue.sha256"),
                    plan.root() + "
",
                    StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot write JEP residue DAG plan", failure);
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalArgumentException("cannot read residue queue: " + path, failure);
        }
    }
}
