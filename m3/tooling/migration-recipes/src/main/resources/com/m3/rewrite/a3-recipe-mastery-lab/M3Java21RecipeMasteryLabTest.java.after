// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

class M3Java21RecipeMasteryLabTest {
    @Test
    void canonicalDagCompilesPreservesBehaviorAndIgnoresCodeLookingDecoys() {
        Map<String, String> before = M3RecipeMasteryLab.corpus();
        Map<String, M3RecipeMasteryLab.Outcome> baseline = M3RecipeMasteryLab.execute(before);

        Map<String, String> after =
                M3RecipeMasteryLab.applyCanonical(
                        new M3Java21ConvergenceRecipe(), before);

        assertEquals(baseline, M3RecipeMasteryLab.execute(after));
        assertEquals(24, M3RecipeMasteryLab.markerCount(after, "M3-IOP: PURE_INT_EXPRESSION"));
        assertEquals(24, M3RecipeMasteryLab.markerCount(after, "M3-ATOM: m3$pureIntAtom"));
        assertEquals(
                0,
                M3RecipeMasteryLab.markerCount(
                        Map.of(
                                "division", after.get("src/main/java/mastery/RejectDivision.java"),
                                "call", after.get("src/main/java/mastery/RejectCall.java"),
                                "extra", after.get("src/main/java/mastery/RejectExtraStatement.java"),
                                "visibility", after.get("src/main/java/mastery/RejectVisibility.java")),
                        "M3-ATOM: m3$pureIntAtom"));
        assertEquals(
                after,
                M3RecipeMasteryLab.applyCanonical(
                        new M3Java21ConvergenceRecipe(), after));
    }

    @Test
    void everyMutationPassPermutationConvergesToCanonicalBehaviorAndSource() {
        Map<String, String> before = M3RecipeMasteryLab.corpus();
        Map<String, M3RecipeMasteryLab.Outcome> baseline = M3RecipeMasteryLab.execute(before);
        Map<String, String> canonical =
                M3RecipeMasteryLab.applyCanonical(
                        new M3Java21ConvergenceRecipe(), before);

        for (List<Recipe> schedule : permutations()) {
            M3RecipeMasteryLab.Convergence convergence =
                    M3RecipeMasteryLab.converge(schedule, before);

            assertTrue(
                    convergence.cycles() <= 3,
                    () -> "slow convergence schedule=" + names(schedule));
            assertEquals(
                    baseline,
                    M3RecipeMasteryLab.execute(convergence.sources()),
                    () -> "behavior drift schedule=" + names(schedule));
            assertEquals(
                    canonical,
                    convergence.sources(),
                    () -> "source divergence schedule=" + names(schedule));

            M3RecipeMasteryLab.Convergence replay =
                    M3RecipeMasteryLab.converge(schedule, convergence.sources());
            assertEquals(0, replay.cycles(), () -> "not fixed point schedule=" + names(schedule));
            assertEquals(convergence.sources(), replay.sources());
        }
    }

    @Test
    void allNonEmptyMutationPassSubsetsRemainCompilerSafeAndMonotone() {
        Map<String, String> before = M3RecipeMasteryLab.corpus();
        Map<String, M3RecipeMasteryLab.Outcome> baseline = M3RecipeMasteryLab.execute(before);
        List<Recipe> passes = mutationPasses();

        for (int mask = 1; mask < (1 << passes.size()); mask++) {
            ArrayList<Recipe> schedule = new ArrayList<>();
            for (int index = 0; index < passes.size(); index++) {
                if ((mask & (1 << index)) != 0) {
                    schedule.add(passes.get(index));
                }
            }

            M3RecipeMasteryLab.Convergence convergence =
                    M3RecipeMasteryLab.converge(schedule, before);
            assertEquals(
                    baseline,
                    M3RecipeMasteryLab.execute(convergence.sources()),
                    () -> "subset changed behavior schedule=" + names(schedule));
            assertTrue(convergence.cycles() <= 2, () -> "subset failed to stabilize");
            assertFalse(convergence.sources().isEmpty());
        }
    }

    @Test
    void compilerGateRejectsDeliberateBrokenCandidate() {
        Map<String, String> broken = new java.util.LinkedHashMap<>(M3RecipeMasteryLab.corpus());
        String path = "src/main/java/mastery/F000.java";
        broken.put(
                path,
                broken.get(path).replace(
                        "static int call(int a, int b) { return target(a, b); }",
                        "static int call(int a, int b) { return missing(a, b); }"));

        AssertionError failure =
                assertThrows(AssertionError.class, () -> M3RecipeMasteryLab.execute(broken));
        assertTrue(failure.getMessage().contains("Java compiler rejected mastery corpus"));
    }

    @Test
    void regexAndTextBlockSignalsAreStableAcrossAllRecipeSchedules() {
        Map<String, String> before = M3RecipeMasteryLab.corpus();
        Map<String, M3RecipeMasteryLab.Outcome> baseline = M3RecipeMasteryLab.execute(before);

        int expectedRegex =
                baseline.values().stream().mapToInt(M3RecipeMasteryLab.Outcome::regexScore).sum();
        assertEquals(24, expectedRegex);

        for (List<Recipe> schedule : permutations()) {
            Map<String, M3RecipeMasteryLab.Outcome> after =
                    M3RecipeMasteryLab.execute(
                            M3RecipeMasteryLab.converge(schedule, before).sources());
            assertEquals(
                    expectedRegex,
                    after.values().stream()
                            .mapToInt(M3RecipeMasteryLab.Outcome::regexScore)
                            .sum(),
                    () -> "regex/text-block signal drift schedule=" + names(schedule));
        }
    }

    private static List<Recipe> mutationPasses() {
        return List.of(
                new M3AtomizePureIntReturnRecipe(),
                new M3PatternizePureIntAtomRecipe(),
                new M3DocumentPureIntAtomRecipe());
    }

    private static List<List<Recipe>> permutations() {
        List<Recipe> passes = mutationPasses();
        ArrayList<List<Recipe>> result = new ArrayList<>();
        for (int a = 0; a < passes.size(); a++) {
            for (int b = 0; b < passes.size(); b++) {
                if (b == a) continue;
                for (int c = 0; c < passes.size(); c++) {
                    if (c == a || c == b) continue;
                    result.add(List.of(passes.get(a), passes.get(b), passes.get(c)));
                }
            }
        }
        assertEquals(6, result.size());
        return List.copyOf(result);
    }

    private static String names(List<Recipe> recipes) {
        return recipes.stream()
                .map(recipe -> recipe.getClass().getSimpleName())
                .reduce((left, right) -> left + "->" + right)
                .orElse("<empty>");
    }
}
