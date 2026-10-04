// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Converges the authoritative JDK 22-27 released-JEP denominator as independent FILE atoms.
 *
 * <p>Ten one-target hash-pinned text leaves join at MODULE scope. Six targets are additive on
 * current master; four are exact replacements. Catalogue, queue, pass ledger, verifier and status
 * are therefore unable to converge independently to contradictory denominators.</p>
 */
public final class M3ReleaseJepDenominatorRecipe extends Recipe {
    public static final int JEP_DENOMINATOR = 85;

    private static final List<String> CRATES = List.of(
            "release-jep-denominator-authority",
            "release-jep-denominator-authority-code",
            "release-jep-denominator-authority-test",
            "release-jep-denominator-passes",
            "release-jep-denominator-catalogue",
            "release-jep-denominator-queue",
            "release-jep-denominator-status-code",
            "release-jep-denominator-status-report",
            "release-jep-denominator-status-test",
            "release-jep-denominator-verifier");

    @Override
    public String getDisplayName() {
        return "Converge authoritative released JEP denominator";
    }

    @Override
    public String getDescription() {
        return "Joins ten independently hash-pinned FILE atoms so official release authority, "
                + "catalogue, pass/work queue, verifier and status reporting converge to 85 JEPs.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "inventory", "jep", "recipe-first",
                "file-atoms", "module-join", "fail-closed", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return CRATES.stream()
                .map(M3Jdk21HashPinnedTextSnapshotRecipe::new)
                .map(Recipe.class::cast)
                .toList();
    }

    public static List<String> crates() {
        return CRATES;
    }
}
