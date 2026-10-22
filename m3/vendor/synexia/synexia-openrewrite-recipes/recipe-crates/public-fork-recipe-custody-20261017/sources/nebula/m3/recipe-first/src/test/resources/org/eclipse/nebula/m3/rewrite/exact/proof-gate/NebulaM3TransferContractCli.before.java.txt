// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.nebula.m3.NebulaM3FastSearchReviewPolicy;

/** Writes the portable Nebula M3 recipe-transfer evidence bundle. */
public final class NebulaM3TransferContractCli {
    private NebulaM3TransferContractCli() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "usage: NebulaM3TransferContractCli <output-directory>");
        }
        Path output =
                Path.of(Objects.requireNonNull(args[0], "output"))
                        .toAbsolutePath()
                        .normalize();
        Files.createDirectories(output);

        Files.writeString(
                output.resolve("transfer-metadata.tsv"),
                NebulaM3TransferContract.metadataTsv(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("transfer-dag.tsv"),
                NebulaM3RecipeDagManifest.tsv(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("transfer-targets.tsv"),
                NebulaM3TransferContract.targetsTsv(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("ui-behavior-donors.tsv"),
                NebulaM3UiBehaviorDonorCatalog.tsv(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("challenge-donors.tsv"),
                NebulaM3FastSearchReviewPolicy.renderTsv(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("orchestrators.tsv"),
                NebulaM3TransferContract.orchestratorsTsv(),
                StandardCharsets.UTF_8);

        Files.writeString(
                output.resolve("camel-route.yaml"),
                NebulaM3OrchestratorPlans.camelYaml(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("airflow-dag.py"),
                NebulaM3OrchestratorPlans.airflowPython(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("drools-agenda.drl"),
                NebulaM3OrchestratorPlans.droolsDrl(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("orchestrator-plans.sha256"),
                NebulaM3OrchestratorPlans.root() + "\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("transfer.sha256"),
                NebulaM3TransferContract.root() + "\n",
                StandardCharsets.UTF_8);
    }
}
