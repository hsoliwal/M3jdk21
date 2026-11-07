// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Cyclic dependencies in the greedy pass of {@link M3IndexPrecomputePlanner} (more than
 * {@link M3IndexPrecomputePlanner#EXACT_LIMIT} candidates), recipe planner-cycle-fix-20261107. Contract, taken from the
 * exact search: a capability is admitted only with all of its dependencies, so a dependency cycle is one
 * dependency-closed group. Before the recipe the greedy closure recursed without end on a cycle (StackOverflowError).
 */
//FLLDCIM: TOP - regression of recipe m3/ports/precompute/recipe/planner-cycle-fix-20261107 (fails with StackOverflowError before it)
final class M3IndexPrecomputePlannerCycleTest {
    private static final M3IndexPrecomputeCapability[] CAPS = M3IndexPrecomputeCapability.values();
    private static final int GREEDY = M3IndexPrecomputePlanner.EXACT_LIMIT + 1;
    private static final int CORPUS = 800;

    @Test
    void greedyClosesATwoCycleAsOneGroup() {
        M3IndexPrecomputeRecommendation result = recommend(cycle(0, 1), new boolean[GREEDY], 100);
        assertEquals(GREEDY, result.capabilities().size());
        assertEquals(GREEDY, result.retainedBytes());
        assertEquals(GREEDY, result.score());
        assertFalse(result.exactSearch());
    }

    @Test
    void greedyLeavesOutACycleWhoseGroupDoesNotFit() {
        M3IndexPrecomputeRecommendation result = recommend(cycle(0, 1, 2), new boolean[GREEDY], 2);
        assertTrue(result.capabilities().stream().noneMatch(Set.of(CAPS[0], CAPS[1], CAPS[2])::contains));
        assertEquals(2, result.retainedBytes());
        assertEquals(2, result.score());
    }

    @Test
    void mandatoryCycleMemberBringsItsWholeGroup() {
        boolean[] mandatory = new boolean[GREEDY];
        mandatory[0] = true;
        M3IndexPrecomputeRecommendation result = recommend(cycle(0, 1), mandatory, 2);
        assertEquals(Set.of(CAPS[0], CAPS[1]), result.capabilities());
        assertEquals(2, result.retainedBytes());
    }

    @Test
    void mandatoryCycleOverBudgetIsRefused() {
        boolean[] mandatory = new boolean[GREEDY];
        mandatory[0] = true;
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> recommend(cycle(0, 1), mandatory, 1));
        assertEquals("mandatory precompute capabilities exceed byte budget", refused.getMessage());
    }

    @Test
    void cyclicCorpusMatchesTheReferenceWithoutOverflow() throws Exception {
        long[] counts = M3IndexPrecomputePlannerDump.run(M3IndexPrecomputePlannerDump.class.getPackageName(), "M3",
                "M3_FROZEN_LONG_TUPLE_V1:", CORPUS, true, M3IndexPrecomputePlannerDump.discard());
        assertEquals(0, counts[3], "stack overflows");
        assertTrue(counts[4] > 0, "cyclic catalogues on the brute-force path");
        assertTrue(counts[5] > 0, "cyclic catalogues on the greedy path");
    }

    /** GREEDY unit candidates (1 byte, reuse 1, weight 1); members[i] depends on members[i + 1], the last on the first. */
    private static int[][] cycle(int... members) {
        int[][] deps = new int[GREEDY][0];
        for (int i = 0; i < members.length; i++) {
            deps[members[i]] = new int[] {members[(i + 1) % members.length]};
        }
        return deps;
    }

    private static M3IndexPrecomputeRecommendation recommend(int[][] deps, boolean[] mandatory, long budget) {
        List<M3IndexPrecomputeCandidate> candidates = new ArrayList<>();
        M3IndexPrecomputeWorkload.Builder weights = M3IndexPrecomputeWorkload.builder().buildPenalty(0);
        for (int j = 0; j < GREEDY; j++) {
            Set<M3IndexPrecomputeCapability> dependencies = EnumSet.noneOf(M3IndexPrecomputeCapability.class);
            for (int d : deps[j]) {
                dependencies.add(CAPS[d]);
            }
            candidates.add(new M3IndexPrecomputeCandidate(CAPS[j], 1, 0, 1, dependencies, mandatory[j]));
            weights.weight(CAPS[j], 1);
        }
        return new M3IndexPrecomputePlanner().recommend(candidates, budget, weights.build());
    }
}
