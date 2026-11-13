// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.rewrite.scope.M3EditScope;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PacketDagComposerTest {
    @Test
    void independentFileAtomsFanOutAndRejoinBeforeJUnit() {
        BackportPacket packet =
                new BackportPacket(
                        "jdk-x",
                        List.of(
                                atom("java", M3EditScope.FILE, false, "recipe.Java", List.of()),
                                atom("text", M3EditScope.FILE, false, "recipe.Text", List.of())));

        RecipeDag dag = PacketDagComposer.compose(packet);

        assertEquals(18, dag.size());
        assertThrows(IllegalArgumentException.class, () -> dag.require("recipe-crate"));
        assertEquals(
                List.of("a3-preparation"),
                dag.require("packet-java").dependsOn());
        assertEquals(
                List.of("a3-preparation"),
                dag.require("packet-text").dependsOn());
        assertEquals(
                List.of("packet-java", "packet-text"),
                dag.require("recipe-junit").dependsOn());
        assertEquals(
                List.of("packet-java", "packet-text"),
                layerContaining(dag, "packet-java").stream().map(DagNode::id).toList());
        assertTrue(
                layerIndex(dag, "a3-preparation")
                        < layerIndex(dag, "packet-java"));
        assertEquals("promote", dag.topologicalOrder().getLast().id());
        assertEquals(M3EditScope.FILE, dag.require("promote").scope());
    }

    @Test
    void packetDependenciesFormLaterLayersAndTerminalFanIn() {
        BackportPacket packet =
                new BackportPacket(
                        "jdk-y",
                        List.of(
                                atom("a", M3EditScope.FILE, false, "recipe.A", List.of()),
                                atom("b", M3EditScope.FILE, false, "recipe.B", List.of()),
                                atom(
                                        "join",
                                        M3EditScope.PACKAGE,
                                        true,
                                        "recipe.Join",
                                        List.of("a", "b"))));

        RecipeDag dag = PacketDagComposer.compose(packet);

        assertEquals(
                List.of("packet-a", "packet-b"),
                layerContaining(dag, "packet-a").stream().map(DagNode::id).toList());
        assertEquals(
                List.of("packet-join"),
                layerContaining(dag, "packet-join").stream().map(DagNode::id).toList());
        assertTrue(layerIndex(dag, "packet-a") < layerIndex(dag, "packet-join"));
        assertEquals(
                List.of("packet-join"),
                dag.require("recipe-junit").dependsOn());
        assertEquals(M3EditScope.PACKAGE, dag.require("packet-join").scope());
        assertEquals(M3EditScope.PACKAGE, dag.require("promote").scope());
    }

    @Test
    void broaderRootAtomRequiresExplicitPromotionApproval() {
        BackportPacket rejected =
                new BackportPacket(
                        "jdk-z",
                        List.of(
                                atom(
                                        "package",
                                        M3EditScope.PACKAGE,
                                        false,
                                        "recipe.Package",
                                        List.of())));

        assertThrows(
                IllegalArgumentException.class,
                () -> PacketDagComposer.compose(rejected));

        BackportPacket accepted =
                new BackportPacket(
                        "jdk-z",
                        List.of(
                                atom(
                                        "package",
                                        M3EditScope.PACKAGE,
                                        true,
                                        "recipe.Package",
                                        List.of())));

        assertEquals(
                M3EditScope.PACKAGE,
                PacketDagComposer.compose(accepted)
                        .require("packet-package")
                        .scope());
    }

    @Test
    void packetCyclesAreRejectedByCanonicalDagKernel() {
        BackportPacket cyclic =
                new BackportPacket(
                        "jdk-cycle",
                        List.of(
                                atom("a", M3EditScope.FILE, false, "recipe.A", List.of("b")),
                                atom("b", M3EditScope.FILE, false, "recipe.B", List.of("a"))));

        assertThrows(
                IllegalArgumentException.class,
                () -> PacketDagComposer.compose(cyclic));
    }

    @Test
    void composedDagPreservesCanonicalVerificationTail() {
        RecipeDag dag =
                PacketDagComposer.compose(
                        new BackportPacket(
                                "jdk-tail",
                                List.of(
                                        atom(
                                                "a",
                                                M3EditScope.FILE,
                                                false,
                                                "recipe.A",
                                                List.of()))));

        assertEquals(
                List.of(
                        "recipe-junit",
                        "diff",
                        "lint",
                        "compile",
                        "jtreg",
                        "runtime",
                        "promote"),
                dag.topologicalOrder().stream()
                        .map(DagNode::id)
                        .filter(
                                id ->
                                        List.of(
                                                        "recipe-junit",
                                                        "diff",
                                                        "lint",
                                                        "compile",
                                                        "jtreg",
                                                        "runtime",
                                                        "promote")
                                                .contains(id))
                        .toList());
        assertTrue(dag.require("promote").serialPromotion());
    }

    @Test
    void evidenceBoundCompositionRequiresCompleteSemanticProof() {
        BackportPacket packet =
                new BackportPacket(
                        "jdk-evidence",
                        List.of(
                                atom(
                                        "java",
                                        M3EditScope.FILE,
                                        false,
                                        "recipe.Java",
                                        List.of())));
        BackportPacketEvidence evidence =
                new BackportPacketEvidence(
                        "jdk-evidence",
                        List.of(
                                new RecipeAtomEvidence(
                                        "java",
                                        "contract/java",
                                        "docs/java.md",
                                        "Strategy",
                                        "ConcreteStrategy.Java",
                                        "JavaRecipeTest",
                                        true)));

        RecipeDag evidenceDag = PacketDagComposer.compose(packet, evidence);
        assertEquals(
                "packet-java",
                layerContaining(evidenceDag, "packet-java").getFirst().id());
        assertTrue(
                layerIndex(evidenceDag, "a3-preparation")
                        < layerIndex(evidenceDag, "packet-java"));

        BackportPacketEvidence missing =
                new BackportPacketEvidence(
                        "jdk-evidence",
                        List.of(
                                new RecipeAtomEvidence(
                                        "other",
                                        "contract/other",
                                        "docs/other.md",
                                        "Adapter",
                                        "Adapter.Other",
                                        "OtherRecipeTest",
                                        true)));
        assertThrows(
                IllegalArgumentException.class,
                () -> PacketDagComposer.compose(packet, missing));
        assertThrows(
                NullPointerException.class,
                () -> PacketDagComposer.compose(packet, null));
    }

    private static List<DagNode> layerContaining(RecipeDag dag, String id) {
        return dag.layers().stream()
                .filter(layer -> layer.stream().anyMatch(node -> node.id().equals(id)))
                .findFirst()
                .orElseThrow();
    }

    private static int layerIndex(RecipeDag dag, String id) {
        for (int index = 0; index < dag.layers().size(); index++) {
            if (dag.layers().get(index).stream().anyMatch(node -> node.id().equals(id))) {
                return index;
            }
        }
        throw new IllegalArgumentException("missing DAG node: " + id);
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
