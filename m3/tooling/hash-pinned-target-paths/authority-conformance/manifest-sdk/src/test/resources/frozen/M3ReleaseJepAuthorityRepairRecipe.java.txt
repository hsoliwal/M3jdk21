// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** Source-sealed replay for the current released-JEP catalogue repair. */
public final class M3ReleaseJepAuthorityRepairRecipe extends Recipe {
    public static final String CRATE = "release-jep-authority-repair-20261006";

    @Override
    public String getDisplayName() {
        return "Repair current released JEP catalogue";
    }

    @Override
    public String getDescription() {
        return "Replays the exact current-master catalogue/status repair from hash-pinned text.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "denominator",
                "hash-pinned",
                "multi-module",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }
}
