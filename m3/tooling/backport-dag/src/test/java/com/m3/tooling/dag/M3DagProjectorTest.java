// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3EditScope;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class M3DagProjectorTest {
    @Test
    void allProjectionsBindTheSameCanonicalSemanticRoot() {
        M3RecipeDag dag = sampleDag();

        Map<M3DagProjectionFormat, M3DagProjection> projections =
                M3DagProjector.all(dag, "jdk-sample");

        assertEquals(3, projections.size());
        String root = M3DagSemanticRoot.of(dag);
        assertTrue(root.matches("[0-9a-f]{64}"));
        for (M3DagProjection projection : projections.values()) {
            assertEquals(root, projection.dagRoot());
            assertTrue(projection.content().contains(root));
            assertFalse(projection.content().isBlank());
        }
    }

    @Test
    void airflowProjectionPreservesExactDirectEdges() {
        M3RecipeDag dag = sampleDag();
        String source =
                M3DagProjector.project(
                                dag,
                                "jdk-sample",
                                M3DagProjectionFormat.AIRFLOW_PYTHON)
                        .content();

        assertTrue(source.contains("n_file_delta >> n_a3_preparation"));
        assertTrue(source.contains("n_a3_preparation >> n_packet_java"));
        assertTrue(source.contains("n_a3_preparation >> n_packet_text"));
        assertTrue(source.contains("n_packet_java >> n_packet_join"));
        assertTrue(source.contains("n_packet_text >> n_packet_join"));
        assertTrue(source.contains("n_packet_join >> n_recipe_junit"));
        assertTrue(source.contains("n_runtime >> n_promote"));
        assertFalse(source.contains("n_packet_java >> n_packet_text"));
        assertFalse(source.contains("n_packet_text >> n_packet_java"));
        assertTrue(source.contains("'serial_promotion': True"));
        assertTrue(source.contains("'scope_promotion_approved': True"));
        assertTrue(source.contains("'scope': 'PACKAGE'"));
    }

    @Test
    void camelProjectionUsesConservativeParallelLayerBarriers() {
        M3RecipeDag dag = sampleDag();
        String source =
                M3DagProjector.project(
                                dag,
                                "jdk-sample",
                                M3DagProjectionFormat.CAMEL_JAVA)
                        .content();

        int javaNode = source.indexOf(".to(\"direct:m3-node-packet-java\")");
        int textNode = source.indexOf(".to(\"direct:m3-node-packet-text\")");
        int joinNode = source.indexOf(".to(\"direct:m3-node-packet-join\")");
        int promoteNode = source.indexOf(".to(\"direct:m3-node-promote\")");

        assertTrue(javaNode >= 0);
        assertTrue(textNode >= 0);
        assertTrue(joinNode > javaNode);
        assertTrue(joinNode > textNode);
        assertTrue(promoteNode > joinNode);
        assertTrue(source.contains(".multicast().parallelProcessing().stopOnException()"));
        assertTrue(source.contains(".setHeader(\"M3SerialPromotion\").constant(true)"));
        assertTrue(source.contains(".setHeader(\"M3ScopePromotionApproved\").constant(true)"));
        assertTrue(source.contains(".to(\"direct:m3-dispatch\")"));
    }

    @Test
    void droolsJoinRuleRequiresEveryDirectDependency() {
        M3RecipeDag dag = sampleDag();
        String source =
                M3DagProjector.project(
                                dag,
                                "jdk-sample",
                                M3DagProjectionFormat.DROOLS_DRL)
                        .content();

        int joinRule = source.indexOf("rule \"ready-packet-join\"");
        int junitRule = source.indexOf("rule \"ready-recipe-junit\"");
        assertTrue(joinRule >= 0);
        assertTrue(junitRule > joinRule);

        String join = source.substring(joinRule, junitRule);
        assertTrue(join.contains("id == \"packet-java\", status == \"DONE\""));
        assertTrue(join.contains("id == \"packet-text\", status == \"DONE\""));
        assertFalse(join.contains("id == \"runtime\", status == \"DONE\""));

        int promote = source.indexOf("rule \"ready-promote\"");
        String promoteRule = source.substring(promote);
        assertTrue(promoteRule.contains("id == \"runtime\", status == \"DONE\""));
    }

    @Test
    void semanticRootChangesWhenWorkOrDependencySemanticsChange() {
        M3RecipeDag first = sampleDag();
        M3RecipeDag changedWork =
                M3PacketDagComposer.compose(
                        new M3BackportPacket(
                                "jdk-sample",
                                List.of(
                                        atom("java", M3EditScope.FILE, false, "recipe.Java.v2", List.of()),
                                        atom("text", M3EditScope.FILE, false, "recipe.Text", List.of()),
                                        atom(
                                                "join",
                                                M3EditScope.PACKAGE,
                                                true,
                                                "recipe.Join",
                                                List.of("java", "text")))));

        assertNotEquals(
                M3DagSemanticRoot.of(first),
                M3DagSemanticRoot.of(changedWork));
        assertEquals(
                M3DagSemanticRoot.of(first),
                M3DagSemanticRoot.of(sampleDag()));
    }

    @Test
    void workReferencesAreEscapedWithoutChangingDagIdentity() {
        M3RecipeDag dag =
                M3PacketDagComposer.compose(
                        new M3BackportPacket(
                                "jdk-escape",
                                List.of(
                                        atom(
                                                "java",
                                                M3EditScope.FILE,
                                                false,
                                                "recipe.\"Quoted\"\\Path",
                                                List.of()))));

        String camel =
                M3DagProjector.project(
                                dag,
                                "jdk-escape",
                                M3DagProjectionFormat.CAMEL_JAVA)
                        .content();
        String airflow =
                M3DagProjector.project(
                                dag,
                                "jdk-escape",
                                M3DagProjectionFormat.AIRFLOW_PYTHON)
                        .content();
        String drools =
                M3DagProjector.project(
                                dag,
                                "jdk-escape",
                                M3DagProjectionFormat.DROOLS_DRL)
                        .content();

        assertTrue(camel.contains("recipe.\\\"Quoted\\\"\\\\Path"));
        assertTrue(airflow.contains("recipe.\"Quoted\"\\\\Path"));
        assertTrue(drools.contains("workRef=recipe.\"Quoted\"\\Path"));
        assertEquals(
                M3DagSemanticRoot.of(dag),
                M3DagProjector.project(
                                dag,
                                "jdk-escape",
                                M3DagProjectionFormat.CAMEL_JAVA)
                        .dagRoot());
    }


    @Test
    void invalidProjectionInputsFailClosed() {
        M3RecipeDag dag = sampleDag();

        assertThrows(
                IllegalArgumentException.class,
                () -> M3DagProjector.project(
                        dag, "bad id!", M3DagProjectionFormat.AIRFLOW_PYTHON));
        assertThrows(
                NullPointerException.class,
                () -> M3DagProjector.project(
                        null, "jdk-sample", M3DagProjectionFormat.AIRFLOW_PYTHON));
        assertThrows(
                NullPointerException.class,
                () -> M3DagProjector.project(dag, "jdk-sample", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3DagProjection(
                        M3DagProjectionFormat.CAMEL_JAVA,
                        "not-a-root",
                        "content"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3DagProjection(
                        M3DagProjectionFormat.CAMEL_JAVA,
                        "0".repeat(64),
                        "   "));
    }

    @Test
    void projectionFileNamesAreStable() {
        assertEquals(
                "M3BackportRoutes.java",
                M3DagProjectionFormat.CAMEL_JAVA.fileName());
        assertEquals(
                "m3_backport_dag.py",
                M3DagProjectionFormat.AIRFLOW_PYTHON.fileName());
        assertEquals(
                "m3_backport_rules.drl",
                M3DagProjectionFormat.DROOLS_DRL.fileName());
    }

    private static M3RecipeDag sampleDag() {
        return M3PacketDagComposer.compose(
                new M3BackportPacket(
                        "jdk-sample",
                        List.of(
                                atom("java", M3EditScope.FILE, false, "recipe.Java", List.of()),
                                atom("text", M3EditScope.FILE, false, "recipe.Text", List.of()),
                                atom(
                                        "join",
                                        M3EditScope.PACKAGE,
                                        true,
                                        "recipe.Join",
                                        List.of("java", "text")))));
    }

    private static M3RecipeAtom atom(
            String id,
            M3EditScope scope,
            boolean approved,
            String workRef,
            List<String> dependencies) {
        return new M3RecipeAtom(id, scope, approved, workRef, dependencies);
    }
}
