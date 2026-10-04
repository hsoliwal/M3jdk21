// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Restores the authoritative JDK 22-27 release/JEP denominator as independent FILE atoms.
 *
 * <p>Each child is a one-target, hash-pinned text recipe. The composite joins those independently
 * replayable leaves at MODULE scope so catalogue, queue, verification and status cannot converge
 * against a mutually-consistent but incomplete denominator.</p>
 */
public final class M3ReleaseJepDenominatorRecipe extends Recipe {
    public static final int JEP_DENOMINATOR = 85;

    private static final List<String> CRATES = List.of(
            "release-jep-denominator-authority",
            "release-jep-denominator-authority-code",
            "release-jep-denominator-authority-test",
            "release-jep-denominator-catalogue",
            "release-jep-denominator-queue",
            "release-jep-denominator-status-code",
            "release-jep-denominator-status-report",
            "release-jep-denominator-status-test",
            "release-jep-denominator-verifier");

    @Override public String getDisplayName() {
        return "Converge authoritative release JEP denominator";
    }

    @Override public String getDescription() {
        return "Joins nine independently hash-pinned FILE atoms so official release authority, "
                + "catalogue, queue, verifier and status reporting converge to the same denominator.";
    }

    @Override public Set<String> getTags() {
        return Set.of("m3", "jdk21", "inventory", "jep", "recipe-first",
                "file-atoms", "module-join", "fail-closed", "candidate-only");
    }

    @Override public int maxCycles() { return 1; }

    @Override public List<Recipe> getRecipeList() {
        return CRATES.stream()
                .map(M3Jdk21HashPinnedTextSnapshotRecipe::new)
                .map(Recipe.class::cast)
                .toList();
    }

    public static List<String> crates() { return CRATES; }
}
