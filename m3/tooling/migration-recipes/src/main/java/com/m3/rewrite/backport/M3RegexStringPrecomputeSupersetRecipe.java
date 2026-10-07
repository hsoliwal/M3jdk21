// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/**
 * M3JDK-owned installer for the regex/String trial-precompute superset.
 *
 * <p>The actual source mutation is delegated to the existing hash-pinned snapshot recipe. This
 * wrapper only gives the task a stable Maven/OpenRewrite recipe identity.</p>
 */
public final class M3RegexStringPrecomputeSupersetRecipe extends Recipe {
    public static final String CRATE = "m3-regex-string-precompute-v2";

    @Override
    public String getDisplayName() {
        return "M3 regex String precompute superset";
    }

    @Override
    public String getDescription() {
        return "Installs M3-owned regex/String static fact packets and bounded hard-case trial "
                + "ranking from an exact hash-pinned Java recipe crate.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedSnapshotRecipe(CRATE));
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    public boolean bootstrapPromotionAuthority() {
        return false;
    }

    public boolean semanticAuthority() {
        return false;
    }
}
