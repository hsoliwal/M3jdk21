// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NebulaM3RecipeDagManifestCliTest {
    @Test
    void writesStableDagRootAndSchedulerAuthorityReceipts() throws Exception {
        Path output = Files.createTempDirectory("nebula-m3-dag-");

        NebulaM3RecipeDagManifestCli.main(new String[] {output.toString()});

        String dag = Files.readString(output.resolve("recipe-dag.tsv"), StandardCharsets.UTF_8);
        String root = Files.readString(output.resolve("recipe-dag.sha256"), StandardCharsets.UTF_8);
        String orchestrators =
                Files.readString(output.resolve("orchestrators.tsv"), StandardCharsets.UTF_8);

        assertEquals(NebulaM3RecipeDagManifest.tsv(), dag);
        assertEquals(NebulaM3RecipeDagManifest.root() + "\n", root);
        assertTrue(orchestrators.contains("AIRFLOW\tfalse\tfalse"));
        assertTrue(orchestrators.contains("CAMEL\tfalse\tfalse"));
        assertTrue(orchestrators.contains("DROOLS\tfalse\tfalse"));
        assertTrue(orchestrators.contains("MAVEN_OPENREWRITE\tfalse\tfalse"));

        assertEquals(
                NebulaM3OrchestratorPlans.camelYaml(),
                Files.readString(output.resolve("camel-route.yaml"), StandardCharsets.UTF_8));
        assertEquals(
                NebulaM3OrchestratorPlans.airflowPython(),
                Files.readString(output.resolve("airflow-dag.py"), StandardCharsets.UTF_8));
        assertEquals(
                NebulaM3OrchestratorPlans.droolsDrl(),
                Files.readString(output.resolve("drools-agenda.drl"), StandardCharsets.UTF_8));
        assertEquals(
                NebulaM3OrchestratorPlans.root() + "\n",
                Files.readString(output.resolve("orchestrator-plans.sha256"), StandardCharsets.UTF_8));
    }
}
