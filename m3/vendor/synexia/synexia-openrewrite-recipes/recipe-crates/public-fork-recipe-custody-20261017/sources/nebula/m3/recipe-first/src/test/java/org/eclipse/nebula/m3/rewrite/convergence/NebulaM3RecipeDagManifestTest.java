// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class NebulaM3RecipeDagManifestTest {
    @Test
    void manifestMirrorsTheCanonicalRecipeAtomOrder() {
        var atoms = NebulaM3FileConvergenceRecipeDag.atoms();
        var nodes = NebulaM3RecipeDagManifest.nodes();

        assertEquals(atoms.size(), nodes.size());
        assertEquals(
                atoms.stream().map(NebulaM3FileConvergenceRecipeDag.Atom::id).toList(),
                nodes.stream().map(NebulaM3RecipeDagManifest.Node::id).toList());
        assertEquals(
                atoms.stream().map(NebulaM3FileConvergenceRecipeDag.Atom::phase).toList(),
                nodes.stream().map(NebulaM3RecipeDagManifest.Node::phase).toList());
        assertEquals(
                atoms.stream().map(atom -> atom.recipe().getClass().getName()).toList(),
                nodes.stream().map(NebulaM3RecipeDagManifest.Node::recipeClass).toList());
    }

    @Test
    void dependenciesFormDeterministicPhaseBarriers() {
        var nodes = NebulaM3RecipeDagManifest.nodes();

        assertTrue(nodes.getFirst().dependsOn().isEmpty());
        for (NebulaM3RecipeDagManifest.Node node : nodes) {
            int phase = node.phase().ordinal();
            if (phase == 0) {
                assertTrue(node.dependsOn().isEmpty());
                continue;
            }
            var expected =
                    nodes.stream()
                            .filter(
                                    candidate ->
                                            candidate.phase().ordinal() == phase - 1)
                            .map(NebulaM3RecipeDagManifest.Node::id)
                            .toList();
            assertEquals(expected, node.dependsOn(), node.id());
        }
        assertTrue(nodes.stream().allMatch(node -> "FILE".equals(node.scope())));
        assertEquals("READ_ONLY", nodes.getFirst().authority());
        assertTrue(
                nodes.stream()
                        .skip(1)
                        .allMatch(node -> "CANDIDATE_ONLY".equals(node.authority())));
    }

    @Test
    void orchestrationTargetsNeverGainMutationOrPromotionAuthority() {
        assertEquals(
                Set.of(
                        NebulaM3RecipeDagManifest.Orchestrator.MAVEN_OPENREWRITE,
                        NebulaM3RecipeDagManifest.Orchestrator.CAMEL,
                        NebulaM3RecipeDagManifest.Orchestrator.AIRFLOW,
                        NebulaM3RecipeDagManifest.Orchestrator.DROOLS),
                NebulaM3RecipeDagManifest.orchestrationTargets());
        assertFalse(NebulaM3RecipeDagManifest.orchestratorMutationAuthority());
        assertFalse(NebulaM3RecipeDagManifest.orchestratorPromotionAuthority());
    }

    @Test
    void manifestIsContentAddressedAndStable() {
        String tsv = NebulaM3RecipeDagManifest.tsv();

        assertTrue(tsv.startsWith("ordinal\tid\tphase\trecipe\tscope\tauthority\tdependsOn\n"));
        assertTrue(tsv.contains("pure-int-atomization"));
        assertTrue(tsv.contains("svgloader-length-convergence"));
        assertTrue(NebulaM3RecipeDagManifest.root().matches("[0-9a-f]{64}"));
        assertEquals(NebulaM3RecipeDagManifest.root(), NebulaM3RecipeDagManifest.root());
    }
}
