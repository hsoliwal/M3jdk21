// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.rewrite.scope.M3EditScope;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class DagProjectorTest {
    @Test
    void allProjectionsBindTheSameCanonicalSemanticRoot() {
        RecipeDag dag = sampleDag();

        Map<DagProjectionFormat, DagProjection> projections =
                DagProjector.all(dag, "jdk-sample");

        assertEquals(3, projections.size());
        String root = DagSemanticRoot.of(dag);
        assertTrue(root.matches("[0-9a-f]{64}"));
        for (DagProjection projection : projections.values()) {
            assertEquals(root, projection.dagRoot());
            assertTrue(projection.content().contains(root));
            assertFalse(projection.content().isBlank());
        }
    }

    @Test
    void airflowProjectionPreservesExactDirectEdges() {
        RecipeDag dag = sampleDag();
        String source =
                DagProjector.project(
                                dag,
                                "jdk-sample",
                                DagProjectionFormat.AIRFLOW_PYTHON)
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
        RecipeDag dag = sampleDag();
        String source =
                DagProjector.project(
                                dag,
                                "jdk-sample",
                                DagProjectionFormat.CAMEL_JAVA)
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
        RecipeDag dag = sampleDag();
        String source =
                DagProjector.project(
                                dag,
                                "jdk-sample",
                                DagProjectionFormat.DROOLS_DRL)
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
        RecipeDag first = sampleDag();
        RecipeDag changedWork =
                PacketDagComposer.compose(
                        new BackportPacket(
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
                DagSemanticRoot.of(first),
                DagSemanticRoot.of(changedWork));
        assertEquals(
                DagSemanticRoot.of(first),
                DagSemanticRoot.of(sampleDag()));
    }

    @Test
    void workReferencesAreEscapedWithoutChangingDagIdentity() {
        RecipeDag dag =
                PacketDagComposer.compose(
                        new BackportPacket(
                                "jdk-escape",
                                List.of(
                                        atom(
                                                "java",
                                                M3EditScope.FILE,
                                                false,
                                                "recipe.\"Quoted\"\\Path",
                                                List.of()))));

        String camel =
                DagProjector.project(
                                dag,
                                "jdk-escape",
                                DagProjectionFormat.CAMEL_JAVA)
                        .content();
        String airflow =
                DagProjector.project(
                                dag,
                                "jdk-escape",
                                DagProjectionFormat.AIRFLOW_PYTHON)
                        .content();
        String drools =
                DagProjector.project(
                                dag,
                                "jdk-escape",
                                DagProjectionFormat.DROOLS_DRL)
                        .content();

        assertTrue(camel.contains("recipe.\\\"Quoted\\\"\\\\Path"));
        assertTrue(airflow.contains("recipe.\"Quoted\"\\\\Path"));
        assertTrue(drools.contains("workRef=recipe.\"Quoted\"\\Path"));
        assertEquals(
                DagSemanticRoot.of(dag),
                DagProjector.project(
                                dag,
                                "jdk-escape",
                                DagProjectionFormat.CAMEL_JAVA)
                        .dagRoot());
    }


    @Test
    void invalidProjectionInputsFailClosed() {
        RecipeDag dag = sampleDag();

        assertThrows(
                IllegalArgumentException.class,
                () -> DagProjector.project(
                        dag, "bad id!", DagProjectionFormat.AIRFLOW_PYTHON));
        assertThrows(
                NullPointerException.class,
                () -> DagProjector.project(
                        null, "jdk-sample", DagProjectionFormat.AIRFLOW_PYTHON));
        assertThrows(
                NullPointerException.class,
                () -> DagProjector.project(dag, "jdk-sample", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DagProjection(
                        DagProjectionFormat.CAMEL_JAVA,
                        "not-a-root",
                        "content"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DagProjection(
                        DagProjectionFormat.CAMEL_JAVA,
                        "0".repeat(64),
                        "   "));
    }

    @Test
    void projectionFileNamesAreStable() {
        assertEquals(
                "M3BackportRoutes.java",
                DagProjectionFormat.CAMEL_JAVA.fileName());
        assertEquals(
                "m3_backport_dag.py",
                DagProjectionFormat.AIRFLOW_PYTHON.fileName());
        assertEquals(
                "m3_backport_rules.drl",
                DagProjectionFormat.DROOLS_DRL.fileName());
    }

    private static RecipeDag sampleDag() {
        return PacketDagComposer.compose(
                new BackportPacket(
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

    private static RecipeAtom atom(
            String id,
            M3EditScope scope,
            boolean approved,
            String workRef,
            List<String> dependencies) {
        return new RecipeAtom(id, scope, approved, workRef, dependencies);
    }
}
