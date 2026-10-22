// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.nebula.m3.rewrite.convergence.NebulaM3FileConvergenceRecipeDag.Phase;
import org.junit.jupiter.api.Test;

/** Adversarial JUnit proof for a scheduler-neutral, FILE-scoped recipe dependency DAG. */
final class NebulaM3DagWavePlannerTest {
    @Test
    void existingManifestStillProducesTheSameFiveSerialWaves() {
        List<NebulaM3RecipeDagManifest.Node> canonical = NebulaM3RecipeDagManifest.nodes();
        List<List<NebulaM3RecipeDagManifest.Node>> waves =
                NebulaM3RecipeDagManifest.readinessWaves();

        assertEquals(canonical.size(), waves.size());
        for (int index = 0; index < canonical.size(); index++) {
            assertEquals(List.of(canonical.get(index)), waves.get(index));
        }
        assertTrue(NebulaM3RecipeDagManifest.root().matches("[0-9a-f]{64}"));
        assertFalse(NebulaM3RecipeDagManifest.orchestratorMutationAuthority());
        assertFalse(NebulaM3RecipeDagManifest.orchestratorPromotionAuthority());
    }

    @Test
    void independentSamePhaseAtomsShareOneReadinessWaveOnly() {
        List<NebulaM3RecipeDagManifest.Node> candidate = branched();
        List<List<NebulaM3RecipeDagManifest.Node>> waves =
                NebulaM3DagWavePlanner.plan(candidate);

        assertEquals(List.of(1, 2, 1, 1, 1),
                waves.stream().map(List::size).toList());
        assertEquals(
                List.of("atom-a", "atom-b"),
                waves.get(1).stream()
                        .map(NebulaM3RecipeDagManifest.Node::id).toList());
        assertTrue(waves.stream().flatMap(List::stream).allMatch(
                node -> "FILE".equals(node.scope())));
        assertThrows(UnsupportedOperationException.class, waves::clear);
        assertThrows(UnsupportedOperationException.class, () -> waves.get(1).clear());
    }

    @Test
    void partialPhaseBarrierCannotSchedulePatternizationBeforeAllAtoms() {
        var candidate = branched();
        candidate.set(3, node(3, "pattern", Phase.PATTERNIZATION, List.of("atom-a")));
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(candidate));
    }

    @Test
    void missingForwardSelfCyclicAndDuplicateDependenciesFailClosed() {
        var canonical = NebulaM3RecipeDagManifest.nodes();
        for (List<String> dependencies : List.of(
                List.of("missing"),
                List.of(canonical.get(1).id()),
                List.of(canonical.get(3).id()),
                List.of(canonical.get(0).id(), canonical.get(0).id()))) {
            var changed = new ArrayList<>(canonical);
            var old = changed.get(1);
            changed.set(1, copy(old, old.ordinal(), old.id(), old.phase(),
                    old.scope(), old.authority(), dependencies));
            assertThrows(IllegalArgumentException.class,
                    () -> NebulaM3DagWavePlanner.plan(changed));
        }
    }

    @Test
    void outOfOrderMissingOrForgedPhaseFailsClosed() {
        var canonical = NebulaM3RecipeDagManifest.nodes();

        var missing = new ArrayList<>(canonical);
        missing.remove(missing.size() - 1);
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(missing));

        var reordered = new ArrayList<>(canonical);
        var old = reordered.get(1);
        reordered.set(1, copy(old, old.ordinal(), old.id(), Phase.DOCUMENTATION,
                old.scope(), old.authority(), old.dependsOn()));
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(reordered));

        var badRoot = new ArrayList<>(canonical);
        old = badRoot.get(0);
        badRoot.set(0, copy(old, old.ordinal(), old.id(), Phase.ATOMIZATION,
                old.scope(), "CANDIDATE_ONLY", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(badRoot));
    }

    @Test
    void authorityAndScopeCannotBePromotedBySchedulingMetadata() {
        var canonical = NebulaM3RecipeDagManifest.nodes();
        for (String wrongScope : List.of("PACKAGE", "MODULE", "LIBRARY_API")) {
            var changed = new ArrayList<>(canonical);
            var old = changed.get(2);
            changed.set(2, copy(old, old.ordinal(), old.id(), old.phase(),
                    wrongScope, old.authority(), old.dependsOn()));
            assertThrows(IllegalArgumentException.class,
                    () -> NebulaM3DagWavePlanner.plan(changed));
        }
        var wrongAuthority = new ArrayList<>(canonical);
        var old = wrongAuthority.get(2);
        wrongAuthority.set(2, copy(old, old.ordinal(), old.id(), old.phase(),
                old.scope(), "READ_ONLY", old.dependsOn()));
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(wrongAuthority));
    }

    @Test
    void ordinalIdentityAndDuplicateIdsCannotBypassReview() {
        var canonical = NebulaM3RecipeDagManifest.nodes();
        var wrongOrdinal = new ArrayList<>(canonical);
        var old = wrongOrdinal.get(2);
        wrongOrdinal.set(2, copy(old, 8, old.id(), old.phase(),
                old.scope(), old.authority(), old.dependsOn()));
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(wrongOrdinal));

        var duplicate = new ArrayList<>(canonical);
        old = duplicate.get(2);
        duplicate.set(2, copy(old, old.ordinal(), canonical.get(1).id(), old.phase(),
                old.scope(), old.authority(), old.dependsOn()));
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(duplicate));

        var invalidId = new ArrayList<>(canonical);
        old = invalidId.get(2);
        invalidId.set(2, copy(old, old.ordinal(), "escaped/path", old.phase(),
                old.scope(), old.authority(), old.dependsOn()));
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(invalidId));
    }

    @Test
    void additionalInventoryRootCannotForgeCompletion() {
        var candidate = branched();
        candidate.set(1, node(1, "another-inventory", Phase.INVENTORY, List.of("inventory")));
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(candidate));
    }

    @Test
    void laterPhaseCannotRevertToAnEarlierPhase() {
        var candidate = branched();
        candidate.set(4, node(4, "reverted-atom", Phase.ATOMIZATION,
                List.of("pattern")));
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(candidate));
    }

    @Test
    void missingFinalPhaseIsRejectedEvenWhenNodeBudgetIsLargeEnough() {
        var candidate = branched();
        candidate.remove(candidate.size() - 1);
        assertEquals(5, candidate.size());
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(candidate));
    }

    @Test
    void samePhaseDependenciesCanForceSerialReadinessWithoutChangingScope() {
        var candidate = branched();
        candidate.set(2, node(2, "atom-b", Phase.ATOMIZATION,
                List.of("inventory", "atom-a")));
        List<List<NebulaM3RecipeDagManifest.Node>> waves =
                NebulaM3DagWavePlanner.plan(candidate);
        assertEquals(List.of(1, 1, 1, 1, 1, 1),
                waves.stream().map(List::size).toList());
        assertTrue(waves.stream().flatMap(List::stream)
                .allMatch(node -> "FILE".equals(node.scope())));
    }

    @Test
    void oversizedPlanAndNullInputFailClosed() {
        List<NebulaM3RecipeDagManifest.Node> tooLarge = new ArrayList<>();
        for (int i = 0; i < 4097; i++) {
            tooLarge.add(node(i, "atom-" + i, Phase.INVENTORY, List.of()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> NebulaM3DagWavePlanner.plan(tooLarge));
        assertThrows(NullPointerException.class,
                () -> NebulaM3DagWavePlanner.plan(null));
    }

    private static List<NebulaM3RecipeDagManifest.Node> branched() {
        return new ArrayList<>(List.of(
                node(0, "inventory", Phase.INVENTORY, List.of()),
                node(1, "atom-a", Phase.ATOMIZATION, List.of("inventory")),
                node(2, "atom-b", Phase.ATOMIZATION, List.of("inventory")),
                node(3, "pattern", Phase.PATTERNIZATION, List.of("atom-a", "atom-b")),
                node(4, "documentation", Phase.DOCUMENTATION, List.of("pattern")),
                node(5, "fixed-point", Phase.PROVEN_FILE_CONVERGENCE,
                        List.of("documentation"))));
    }

    private static NebulaM3RecipeDagManifest.Node node(
            int ordinal, String id, Phase phase, List<String> dependencies) {
        return new NebulaM3RecipeDagManifest.Node(ordinal, id, phase,
                "org.eclipse.nebula.m3.fixture.TestRecipe", "FILE",
                phase.mutating() ? "CANDIDATE_ONLY" : "READ_ONLY", dependencies);
    }

    private static NebulaM3RecipeDagManifest.Node copy(
            NebulaM3RecipeDagManifest.Node old, int ordinal, String id,
            Phase phase, String scope, String authority, List<String> dependencies) {
        return new NebulaM3RecipeDagManifest.Node(
                ordinal, id, phase, old.recipeClass(), scope, authority, dependencies);
    }
}
