// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

/** Writes the content-addressed Nebula M3 recipe DAG evidence for build/CI consumption. */
public final class NebulaM3RecipeDagManifestCli {
    private NebulaM3RecipeDagManifestCli() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: NebulaM3RecipeDagManifestCli <output-directory>");
        }
        Path output = Path.of(Objects.requireNonNull(args[0], "output")).toAbsolutePath().normalize();
        Files.createDirectories(output);

        Files.writeString(
                output.resolve("recipe-dag.tsv"),
                NebulaM3RecipeDagManifest.tsv(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("recipe-dag.sha256"),
                NebulaM3RecipeDagManifest.root() + "\n",
                StandardCharsets.UTF_8);

        StringBuilder orchestrators = new StringBuilder("orchestrator\tmutationAuthority\tpromotionAuthority\n");
        NebulaM3RecipeDagManifest.orchestrationTargets().stream()
                .sorted(Comparator.comparing(Enum::name))
                .forEach(
                        target ->
                                orchestrators
                                        .append(target.name())
                                        .append("\tfalse\tfalse\n"));
        Files.writeString(
                output.resolve("orchestrators.tsv"),
                orchestrators.toString(),
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
    }
}
