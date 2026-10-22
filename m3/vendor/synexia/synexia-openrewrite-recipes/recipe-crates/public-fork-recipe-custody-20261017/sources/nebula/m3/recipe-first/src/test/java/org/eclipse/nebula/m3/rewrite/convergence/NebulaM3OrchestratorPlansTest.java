// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class NebulaM3OrchestratorPlansTest {
    @Test
    void camelPlanContainsEveryRecipeAtomInCanonicalOrder() {
        String yaml = NebulaM3OrchestratorPlans.camelYaml();
        int previous = -1;

        for (NebulaM3RecipeDagManifest.Node node : NebulaM3RecipeDagManifest.nodes()) {
            int position = yaml.indexOf("constant: \"" + node.id() + "\"");
            assertTrue(position > previous, node.id());
            previous = position;
            assertTrue(yaml.contains("constant: \"" + node.recipeClass() + "\""));
        }
        assertTrue(yaml.contains("name: m3RecipeWave"));
        assertTrue(yaml.contains("name: m3DependsOn"));
        for (NebulaM3RecipeDagManifest.Node node : NebulaM3RecipeDagManifest.nodes()) {
            assertTrue(
                    yaml.contains(
                            "constant: \"" + String.join(",", node.dependsOn()) + "\""),
                    node.id());
        }
        assertTrue(yaml.contains("direct:nebula-m3-openrewrite-atom"));
        assertFalse(yaml.contains("rewrite:run"));
    }

    @Test
    void airflowPlanUsesDeclaredDagEdgesAndCarriesNoMutationOperator() {
        String python = NebulaM3OrchestratorPlans.airflowPython();
        var nodes = NebulaM3RecipeDagManifest.nodes();

        assertTrue(python.contains("dag_id=\"nebula_m3_file_convergence\""));
        assertTrue(python.contains("\"mutation_authority\": False"));
        assertTrue(python.contains("\"promotion_authority\": False"));
        assertEquals(nodes.size(), occurrences(python, " = EmptyOperator("));
        for (NebulaM3RecipeDagManifest.Node node : nodes) {
            for (String dependency : node.dependsOn()) {
                String edge =
                        dependency.replace('-', '_')
                                + " >> "
                                + node.id().replace('-', '_');
                assertTrue(python.contains(edge), edge);
            }
        }
        assertFalse(python.contains("BashOperator"));
        assertFalse(python.contains("rewrite:run"));
    }

    @Test
    void droolsPlanEncodesDeclaredDependencyReadinessWithoutExecutingRecipes() {
        String drl = NebulaM3OrchestratorPlans.droolsDrl();
        var nodes = NebulaM3RecipeDagManifest.nodes();

        assertEquals(nodes.size(), occurrences(drl, "rule \"m3-"));
        assertTrue(drl.contains("modify($current) { setReady(true) };"));
        for (NebulaM3RecipeDagManifest.Node node : nodes) {
            for (String dependency : node.dependsOn()) {
                assertTrue(
                        drl.contains(
                                "M3RecipeNode(id == \""
                                        + dependency
                                        + "\", ready == true)"),
                        node.id() + " <- " + dependency);
            }
        }
        assertFalse(drl.contains("Runtime.getRuntime"));
        assertFalse(drl.contains("ProcessBuilder"));
    }

    @Test
    void digestFailureIsFailClosedWithoutChangingThePublicSchedulerRoot() {
        ExceptionInInitializerError failure =
                org.junit.jupiter.api.Assertions.assertThrows(
                        ExceptionInInitializerError.class,
                        () -> NebulaM3OrchestratorPlans.digest(
                                "payload",
                                "M3-NOT-A-MESSAGE-DIGEST"));

        assertTrue(failure.getCause() instanceof java.security.NoSuchAlgorithmException);
        assertTrue(NebulaM3OrchestratorPlans.root().matches("[0-9a-f]{64}"));
    }

    @Test
    void schedulerPlanRootIsStableAndBoundToCanonicalDag() {
        assertTrue(NebulaM3OrchestratorPlans.root().matches("[0-9a-f]{64}"));
        assertEquals(NebulaM3OrchestratorPlans.root(), NebulaM3OrchestratorPlans.root());
        assertTrue(
                NebulaM3OrchestratorPlans.camelYaml()
                        .contains(NebulaM3RecipeDagManifest.root()));
        assertTrue(
                NebulaM3OrchestratorPlans.airflowPython()
                        .contains(NebulaM3RecipeDagManifest.root()));
        assertTrue(
                NebulaM3OrchestratorPlans.droolsDrl()
                        .contains(NebulaM3RecipeDagManifest.root()));
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
