// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import org.openrewrite.Recipe;

/**
 * One source-sealed M3 recipe that converges the existing recipe laboratory with bounded
 * combinatorial mastery, regex/string decoy fixtures and permanent counterexample atoms.
 */
public final class M3RecipeMasteryConvergenceRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Converge M3 recipe mastery laboratory";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed canonical mastery lab/replay/capture and whole-spectrum "
                + "postimages from exact versioned Java/POM preimages.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-convergence-v6"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-lab-integrity-v2"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-schedule-integrity-v1"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-precompute-integrity-v1"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-combinator-integrity-v1"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-signals-integrity-v1"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-signals-integrity-v2"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-native-integrity-v1"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-replay-integrity-v1"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-counterexample-replay-proof-v1"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-exact-failure-v1"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-repeat-stability-v1"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-repeat-stability-v2"),
                new M3HashPinnedJavaSnapshotRecipe("atom-pattern-whole-spectrum-v3"),
                new M3HashPinnedJavaSnapshotRecipe("atom-pattern-whole-spectrum-v4"),
                new M3HashPinnedJavaSnapshotRecipe("recipe-mastery-convergence-bootstrap-v16"),
                new M3HashPinnedPomSnapshotRecipe("atom-pattern-whole-spectrum-v2"));
    }
}
