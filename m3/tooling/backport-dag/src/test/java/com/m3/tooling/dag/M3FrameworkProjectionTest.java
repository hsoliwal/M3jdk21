// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class M3FrameworkProjectionTest {

    @Test
    void camelProjectionContainsEveryCanonicalDagNodeInTopologicalOrder() {
        String yaml = resource("adapters/camel/m3-backport-route.yaml");
        int previous = -1;
        for (M3DagNode node : M3RecipeDag.canonical().topologicalOrder()) {
            String token = "direct:" + node.id();
            int at = yaml.indexOf(token);
            assertTrue(at > previous, token);
            assertEquals(at, yaml.lastIndexOf(token), token);
            previous = at;
        }
    }

    @Test
    void airflowProjectionContainsEveryCanonicalDagTaskInTopologicalOrder() {
        String python = resource("adapters/airflow/m3_backport_dag.py");
        int previous = -1;
        for (M3DagNode node : M3RecipeDag.canonical().topologicalOrder()) {
            String token = "task_id=\"" + node.id() + "\"";
            int at = python.indexOf(token);
            assertTrue(at > previous, token);
            assertEquals(at, python.lastIndexOf(token), token);
            previous = at;
        }
        assertTrue(python.contains("inventory >> compatibility >> dependency"));
        assertTrue(python.contains("delta >> baseline >> recipe"));
        assertTrue(python.contains("runtime >> promote"));
    }

    @Test
    void droolsProjectionCanOnlyBlockPromotionAndUsesCanonicalAdmissionFact() {
        String drl = resource("adapters/drools/m3-backport-admission.drl");
        assertTrue(drl.contains("import com.m3.tooling.dag.M3AdmissionFact;"));
        assertTrue(drl.contains("compatibilityProven == false"));
        assertTrue(drl.contains("recipeTestsPassed == false"));
        assertTrue(drl.contains("compilePassed == false || testsPassed == false"));
        assertTrue(drl.contains("scopeApproved == false"));
        assertTrue(drl.contains("benchmarkRequired == true, benchmarkPassed == false"));
        assertTrue(drl.contains("promotionRequested == true"));
    }

    @Test
    void runnableAdaptersArePinnedToTheCanonicalDagSemanticRoot() {
        String root = M3DagSemanticRoot.of(M3RecipeDag.canonical());
        String marker = "M3-DAG-ROOT: " + root;
        assertTrue(resource("adapters/camel/m3-backport-route.yaml").contains(marker));
        assertTrue(resource("adapters/airflow/m3_backport_dag.py").contains(marker));
        assertTrue(resource("adapters/drools/m3-backport-admission.drl").contains(marker));
    }

    private static String resource(String name) {
        try (var input =
                M3FrameworkProjectionTest.class.getResourceAsStream(
                        "/com/m3/tooling/dag/" + name)) {
            if (input == null) {
                throw new IllegalStateException("missing projection: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
