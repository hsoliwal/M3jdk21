// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** Source-sealed receiver for the batch bootstrap of JEPs lacking recipe evidence. */
public final class M3NoRecipeJepBootstrapReceiverRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Receive no-recipe JEP batch bootstrap";
    }

    @Override
    public String getDescription() {
        return "Replays the exact implementation-authority ledger, validator, tests, docs, and "
                + "matrix workflow that generate FILE-sized recipe evidence without editing JDK "
                + "product source or granting compatibility/promotion authority.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "backport", "openjdk", "recipe-first",
                "batch", "candidate-only", "fail-closed");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk21-no-recipe-jep-bootstrap"));
    }
}
