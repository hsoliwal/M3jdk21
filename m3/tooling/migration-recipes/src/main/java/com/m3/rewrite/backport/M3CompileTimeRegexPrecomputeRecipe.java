// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/**
 * M3JDK-owned installer for the compile-time constant-regex precompute port
 * ({@code m3/ports/compiletime}): the class-file inventory, the M3CTRX01 image codec, the
 * fail-open gate, the harvester and the differential driver.
 *
 * <p>Source mutation is delegated to the hash-pinned snapshot recipe; this wrapper only gives the
 * crate a stable Maven/OpenRewrite identity. Stock {@code java.util.regex} stays the oracle.</p>
 */
public final class M3CompileTimeRegexPrecomputeRecipe extends Recipe {
    public static final String CRATE = "m3-compiletime-regex-precompute";

    @Override
    public String getDisplayName() {
        return "M3 compile-time constant-regex precompute";
    }

    @Override
    public String getDescription() {
        return "Installs the M3 compile-time String inventory scanner and the constant-regex "
                + "requirement image, gate and harvester from an exact hash-pinned Java crate.";
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
