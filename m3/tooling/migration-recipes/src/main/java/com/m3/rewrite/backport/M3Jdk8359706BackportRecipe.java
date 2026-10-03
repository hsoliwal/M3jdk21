// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible cumulative backport of JDK-8359706 plus required macOS fix JDK-8380236.
 */
public final class M3Jdk8359706BackportRecipe extends Recipe {
    public static final String PRIMARY_COMMIT =
            "b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e";
    public static final String MACOS_FIX_COMMIT =
            "3a109f49feb19f313632be6a2aa24ba7d9b7269b";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8359706 open file descriptor diagnostics";
    }

    @Override
    public String getDescription() {
        return "Replays the dependency-closed Java-21-compatible open-file-descriptor diagnostics "
                + "packet including the immediate macOS build fix.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "backport", "jdk-8359706", "jdk-8380236",
                "hotspot", "serviceability", "diagnostics", "hash-pinned", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe("jdk27-open-fd-8359706-java"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-open-fd-8359706-text"));
    }
}
