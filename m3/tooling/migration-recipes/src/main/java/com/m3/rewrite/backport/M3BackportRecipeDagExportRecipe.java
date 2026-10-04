// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Installs the complete candidate-only backport queue DAG exporter as independent FILE atoms.
 *
 * <p>Four leaves are additive and one updates the exact recipe catalogue preimage. The composite
 * has MULTI_MODULE control-plane scope only; it never edits JDK product sources and never grants
 * apply or promotion authority.</p>
 */
public final class M3BackportRecipeDagExportRecipe extends Recipe {
    private static final List<String> CRATES = List.of(
            "queue-dag-export-script",
            "queue-dag-export-test",
            "queue-dag-export-doc",
            "queue-dag-export-workflow",
            "queue-dag-export-catalogue");

    @Override
    public String getDisplayName() {
        return "Install complete M3JDK21 backport queue DAG export";
    }

    @Override
    public String getDescription() {
        return "Joins five hash-pinned control-plane FILE atoms that export the complete backport "
                + "queue as deterministic candidate-only recipe DAG evidence.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "backport", "recipe-dag", "queue",
                "openrewrite", "candidate-only", "control-plane", "multi-module");
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

    public boolean productSourceMutationAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }
}
