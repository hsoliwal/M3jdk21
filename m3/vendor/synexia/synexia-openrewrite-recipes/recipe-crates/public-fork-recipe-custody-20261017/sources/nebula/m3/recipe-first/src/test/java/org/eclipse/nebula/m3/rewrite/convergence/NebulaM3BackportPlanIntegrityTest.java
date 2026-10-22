// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Contract and adversarial proof for the bounded cross-repository backport DAG atom. */
final class NebulaM3BackportPlanIntegrityTest {
    @Test
    void canonicalPlanIsValidatedImmutableAndStillContentAddressed() {
        List<NebulaM3BackportPlan.Step> validated =
                NebulaM3BackportPlan.validate(NebulaM3BackportPlan.steps());

        assertEquals(NebulaM3BackportPlan.steps(), validated);
        assertEquals(NebulaM3BackportPlan.Phase.values().length, validated.size());
        assertThrows(UnsupportedOperationException.class, () -> validated.clear());
        assertTrue(NebulaM3BackportPlan.root().matches("[0-9a-f]{64}"));
        assertFalse(NebulaM3BackportPlan.directSourceMutationAuthority());
        assertFalse(NebulaM3BackportPlan.promotionAuthority());
    }

    @Test
    void missingAndExtraPhasesFailClosed() {
        var missing = new ArrayList<>(NebulaM3BackportPlan.steps());
        missing.remove(3);
        assertThrows(IllegalArgumentException.class, () -> NebulaM3BackportPlan.validate(missing));

        var extra = new ArrayList<>(NebulaM3BackportPlan.steps());
        extra.add(extra.getLast());
        assertThrows(IllegalArgumentException.class, () -> NebulaM3BackportPlan.validate(extra));
    }

    @Test
    void reorderAndWrongOrdinalCannotPretendToBeCanonical() {
        var reordered = new ArrayList<>(NebulaM3BackportPlan.steps());
        Collections.swap(reordered, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> NebulaM3BackportPlan.validate(reordered));

        var changed = replace(4, old -> step(
                old, 8, old.id(), old.phase(), old.repository(),
                old.maximumScope(), old.dependsOn()));
        assertThrows(IllegalArgumentException.class, () -> NebulaM3BackportPlan.validate(changed));
    }

    @Test
    void duplicateStepIdCannotRebindDependencies() {
        var changed = replace(2, old -> step(
                old, old.ordinal(), NebulaM3BackportPlan.steps().get(1).id(),
                old.phase(), old.repository(), old.maximumScope(), old.dependsOn()));
        assertThrows(IllegalArgumentException.class, () -> NebulaM3BackportPlan.validate(changed));
    }

    @Test
    void selfCycleForwardAndSkippedDependencyFailClosed() {
        for (String dependency : List.of(
                NebulaM3BackportPlan.steps().get(3).id(),
                NebulaM3BackportPlan.steps().get(7).id(),
                NebulaM3BackportPlan.steps().get(0).id())) {
            var changed = replace(3, old -> step(
                    old, old.ordinal(), old.id(), old.phase(), old.repository(),
                    old.maximumScope(), List.of(dependency)));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> NebulaM3BackportPlan.validate(changed));
        }
    }

    @Test
    void nebulaAndSynexiaAndJdkOwnershipCannotBeSwapped() {
        var changed = replace(1, old -> step(
                old, old.ordinal(), old.id(), old.phase(), "hsoliwal/M3jdk21",
                old.maximumScope(), old.dependsOn()));
        assertThrows(IllegalArgumentException.class, () -> NebulaM3BackportPlan.validate(changed));
    }

    @Test
    void libraryScopeCannotArriveBeforeFileReplayAndProof() {
        var early = replace(5, old -> step(
                old, old.ordinal(), old.id(), old.phase(), old.repository(),
                NebulaM3RefactorScope.LIBRARY_API, old.dependsOn()));
        assertThrows(IllegalArgumentException.class, () -> NebulaM3BackportPlan.validate(early));

        var missingFinal = replace(7, old -> step(
                old, old.ordinal(), old.id(), old.phase(), old.repository(),
                NebulaM3RefactorScope.FILE, old.dependsOn()));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaM3BackportPlan.validate(missingFinal));
    }

    @Test
    void recordItselfRejectsMutationAndPromotionAuthority() {
        var prior = NebulaM3BackportPlan.steps().get(0);
        assertThrows(IllegalArgumentException.class, () -> new NebulaM3BackportPlan.Step(
                prior.ordinal(), prior.id(), prior.phase(), prior.repository(),
                prior.action(), prior.maximumScope(), prior.dependsOn(), true, false));
        assertThrows(IllegalArgumentException.class, () -> new NebulaM3BackportPlan.Step(
                prior.ordinal(), prior.id(), prior.phase(), prior.repository(),
                prior.action(), prior.maximumScope(), prior.dependsOn(), false, true));
    }

    private static List<NebulaM3BackportPlan.Step> replace(
            int index, java.util.function.UnaryOperator<NebulaM3BackportPlan.Step> transform) {
        var copy = new ArrayList<>(NebulaM3BackportPlan.steps());
        copy.set(index, transform.apply(copy.get(index)));
        return copy;
    }

    private static NebulaM3BackportPlan.Step step(
            NebulaM3BackportPlan.Step old,
            int ordinal,
            String id,
            NebulaM3BackportPlan.Phase phase,
            String repository,
            NebulaM3RefactorScope scope,
            List<String> dependencies) {
        return new NebulaM3BackportPlan.Step(
                ordinal, id, phase, repository, old.action(), scope, dependencies, false, false);
    }
}
