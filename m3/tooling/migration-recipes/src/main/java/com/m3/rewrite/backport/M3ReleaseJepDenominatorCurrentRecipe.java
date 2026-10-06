// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** Exact current-master correction from the stale 82-row JEP denominator to the 85-row authority. */
public final class M3ReleaseJepDenominatorCurrentRecipe extends Recipe {
    public static final String CRATE = "release-jep-denominator-current";

    @Override
    public String getDisplayName() {
        return "Reconcile released JEP denominator to current authority";
    }

    @Override
    public String getDescription() {
        return "Replays the exact current-master control-plane transition from the stale 82-row "
                + "JEP catalogue to the fail-closed 85-row JDK22-27 release authority.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "denominator",
                "release-authority",
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
