// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible serviceability backport of JDK-8359706 with JDK-8380236 folded in.
 *
 * <p>Primary donor: {@code openjdk/jdk@b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e}.
 * macOS build repair: {@code openjdk/jdk@3a109f49feb19f313632be6a2aa24ba7d9b7269b}.
 * Eight FILE atoms remain independently replayable; this composite is the MODULE semantic join.</p>
 */
public final class M3Jdk8359706OpenFdCountBackportRecipe extends Recipe {
    public static final String PRIMARY_UPSTREAM_COMMIT =
            "b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e";
    public static final String MACOS_FOLLOWUP_COMMIT =
            "3a109f49feb19f313632be6a2aa24ba7d9b7269b";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8359706 open file descriptor diagnostics";
    }

    @Override
    public String getDescription() {
        return "Composes eight hash-pinned FILE atoms adding VM.info/hs_err open-file-descriptor "
                + "diagnostics while preserving Java 21 language, API, ABI and unsupported-platform behavior.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "backport", "jdk-8359706", "jdk-8380236",
                "hotspot", "serviceability", "diagnostics", "hash-pinned",
                "file-atoms", "dag-composable", "module-scope", "candidate-adapted");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-open-fd-8359706-aix"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-open-fd-8359706-bsd-cpp"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-open-fd-8359706-bsd-hpp"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-open-fd-8359706-linux"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-open-fd-8359706-windows"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-open-fd-8359706-os-hpp"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-open-fd-8359706-vmerror"),
                new M3Jdk21HashPinnedSnapshotRecipe("jdk27-open-fd-8359706-jtreg"));
    }
}
