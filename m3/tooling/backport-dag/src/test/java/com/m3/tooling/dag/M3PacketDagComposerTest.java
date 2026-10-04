// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3EditScope;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3PacketDagComposerTest {
    @Test
    void independentFileAtomsFanOutAndRejoinBeforeJUnit() {
        M3BackportPacket packet =
                new M3BackportPacket(
                        "jdk-x",
                        List.of(
                                atom("java", M3EditScope.FILE, false, "recipe.Java", List.of()),
                                atom("text", M3EditScope.FILE, false, "recipe.Text", List.of())));

        M3RecipeDag dag = M3PacketDagComposer.compose(packet);

        assertEquals(14, dag.size());
        assertThrows(IllegalArgumentException.class, () -> dag.require("recipe-crate"));
        assertEquals(
                List.of("baseline-convergence"),
                dag.require("packet-java").dependsOn());
        assertEquals(
                List.of("baseline-convergence"),
                dag.require("packet-text").dependsOn());
        assertEquals(
                List.of("packet-java", "packet-text"),
                dag.require("recipe-junit").dependsOn());
        assertEquals(
                List.of("packet-java", "packet-text"),
                dag.layers().get(5).stream().map(M3DagNode::id).toList());
        assertEquals("promote", dag.topologicalOrder().getLast().id());
        assertEquals(M3EditScope.FILE, dag.require("promote").scope());
    }

    @Test
    void packetDependenciesFormLaterLayersAndTerminalFanIn() {
        M3BackportPacket packet =
                new M3BackportPacket(
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

        M3RecipeDag dag = M3PacketDagComposer.compose(packet);

        assertEquals(
                List.of("packet-a", "packet-b"),
                dag.layers().get(4).stream().map(M3DagNode::id).toList());
        assertEquals(
                List.of("packet-join"),
                dag.layers().get(6).stream().map(M3DagNode::id).toList());
        assertEquals(
                List.of("packet-join"),
                dag.require("recipe-junit").dependsOn());
        assertEquals(M3EditScope.PACKAGE, dag.require("packet-join").scope());
        assertEquals(M3EditScope.PACKAGE, dag.require("promote").scope());
    }

    @Test
    void broaderRootAtomRequiresExplicitPromotionApproval() {
        M3BackportPacket rejected =
                new M3BackportPacket(
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
                () -> M3PacketDagComposer.compose(rejected));

        M3BackportPacket accepted =
                new M3BackportPacket(
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
                M3PacketDagComposer.compose(accepted)
                        .require("packet-package")
                        .scope());
    }

    @Test
    void packetCyclesAreRejectedByCanonicalDagKernel() {
        M3BackportPacket cyclic =
                new M3BackportPacket(
                        "jdk-cycle",
                        List.of(
                                atom("a", M3EditScope.FILE, false, "recipe.A", List.of("b")),
                                atom("b", M3EditScope.FILE, false, "recipe.B", List.of("a"))));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3PacketDagComposer.compose(cyclic));
    }

    @Test
    void composedDagPreservesCanonicalVerificationTail() {
        M3RecipeDag dag =
                M3PacketDagComposer.compose(
                        new M3BackportPacket(
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
                        .map(M3DagNode::id)
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
        M3BackportPacket packet =
                new M3BackportPacket(
                        "jdk-evidence",
                        List.of(
                                atom(
                                        "java",
                                        M3EditScope.FILE,
                                        false,
                                        "recipe.Java",
                                        List.of())));
        M3BackportPacketEvidence evidence =
                new M3BackportPacketEvidence(
                        "jdk-evidence",
                        List.of(
                                new M3RecipeAtomEvidence(
                                        "java",
                                        "contract/java",
                                        "docs/java.md",
                                        "Strategy",
                                        "ConcreteStrategy.Java",
                                        "JavaRecipeTest",
                                        true)));

        assertEquals(
                "packet-java",
                M3PacketDagComposer.compose(packet, evidence)
                        .layers().get(5).getFirst().id());

        M3BackportPacketEvidence missing =
                new M3BackportPacketEvidence(
                        "jdk-evidence",
                        List.of(
                                new M3RecipeAtomEvidence(
                                        "other",
                                        "contract/other",
                                        "docs/other.md",
                                        "Adapter",
                                        "Adapter.Other",
                                        "OtherRecipeTest",
                                        true)));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3PacketDagComposer.compose(packet, missing));
        assertThrows(
                NullPointerException.class,
                () -> M3PacketDagComposer.compose(packet, null));
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
