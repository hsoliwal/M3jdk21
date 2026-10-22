// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Host-independent proof of the canonical FILE recipe DAG and scheduling evidence.
 *
 * <p>This suite runs unchanged on Windows, macOS and Linux. WSL uses the Linux host path.
 * It deliberately does not load SWT, JNI or a native widget library: such binaries need their
 * separate per-platform runtime and rendering qualification.</p>
 */
final class NebulaM3HostPortabilityTest {
    @TempDir
    Path temporary;

    @Test
    void canonicalDagRootIsTheExactUtf8LfByteHash() throws Exception {
        String tsv = NebulaM3RecipeDagManifest.tsv();
        byte[] bytes = tsv.getBytes(StandardCharsets.UTF_8);

        assertEquals(sha256(bytes), NebulaM3RecipeDagManifest.root());
        assertTrue(tsv.startsWith("ordinal\tid\tphase\trecipe\tscope\tauthority\tdependsOn\n"));
        assertTrue(tsv.endsWith("\n"));
        assertFalse(tsv.contains("\r"));
        assertTrue(NebulaM3RecipeDagManifest.nodes().stream()
                .allMatch(node ->
                        node.id().matches("[a-z][a-z0-9-]*")
                                && node.recipeClass().matches("[A-Za-z0-9_$.]+")
                                && "FILE".equals(node.scope())
                                && !node.id().contains("\\")
                                && !node.recipeClass().contains("\\")));
    }

    @Test
    void schedulerPlansHaveCanonicalLineEndingsAndNoExecutionAuthority() {
        Map<String, String> plans = Map.of(
                "camel", NebulaM3OrchestratorPlans.camelYaml(),
                "airflow", NebulaM3OrchestratorPlans.airflowPython(),
                "drools", NebulaM3OrchestratorPlans.droolsDrl());
        for (Map.Entry<String, String> plan : plans.entrySet()) {
            assertFalse(plan.getValue().contains("\r"), plan.getKey());
            assertTrue(plan.getValue().startsWith(
                    plan.getKey().equals("drools") ? "// generated from DAG " : "# generated from DAG "));
            assertTrue(plan.getValue().contains(NebulaM3RecipeDagManifest.root()));
        }
        assertFalse(NebulaM3RecipeDagManifest.orchestratorMutationAuthority());
        assertFalse(NebulaM3RecipeDagManifest.orchestratorPromotionAuthority());
        assertFalse(NebulaM3OrchestratorPlans.camelYaml().contains("rewrite:run"));
        assertFalse(NebulaM3OrchestratorPlans.airflowPython().contains("BashOperator"));
        assertFalse(NebulaM3OrchestratorPlans.droolsDrl().contains("ProcessBuilder"));
    }

    @Test
    void exportedBytesMatchInMemoryPlansEvenWithSpacesInHostPath() throws Exception {
        Path output = temporary.resolve("M3 proof with spaces").resolve("dag evidence");
        NebulaM3RecipeDagManifestCli.main(new String[] {output.toString()});

        assertArrayEquals(
                NebulaM3RecipeDagManifest.tsv().getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(output.resolve("recipe-dag.tsv")));
        assertArrayEquals(
                (NebulaM3RecipeDagManifest.root() + "\n").getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(output.resolve("recipe-dag.sha256")));
        assertArrayEquals(
                NebulaM3OrchestratorPlans.camelYaml().getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(output.resolve("camel-route.yaml")));
        assertArrayEquals(
                NebulaM3OrchestratorPlans.airflowPython().getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(output.resolve("airflow-dag.py")));
        assertArrayEquals(
                NebulaM3OrchestratorPlans.droolsDrl().getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(output.resolve("drools-agenda.drl")));
        assertEquals(
                NebulaM3OrchestratorPlans.root() + "\n",
                Files.readString(output.resolve("orchestrator-plans.sha256"), StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
