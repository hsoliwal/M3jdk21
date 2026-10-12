// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21 update-line backport of JDK-8359706 with its mandatory macOS repair.
 *
 * <p>The primary donor is the OpenJDK 21 update-line backport commit
 * {@code 3da873b1aa23c64e47a5da3352b7b48e8c2c74cd}, itself a backport of mainline
 * {@code b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e}. The packet also includes
 * JDK-8380236 from {@code f696cb16462f0201a10540ca36049472a79eba94}, which replaces the
 * macOS signal-path assertion with a precondition and restores the build.
 *
 * <p>HotSpot/platform C++ and headers are exact hash-pinned text atoms. The focused jtreg test
 * remains a structured Java LST atom. M3JDK21's Java 21 semantics and public Java APIs are
 * unchanged.
 */
public final class M3Jdk8359706BackportRecipe extends Recipe {
    public static final String JDK21U_COMMIT =
            "3da873b1aa23c64e47a5da3352b7b48e8c2c74cd";
    public static final String MAINLINE_COMMIT =
            "b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e";
    public static final String MACOS_REPAIR_COMMIT =
            "f696cb16462f0201a10540ca36049472a79eba94";
    public static final String MACOS_REPAIR_MAINLINE =
            "3a109f49feb19f313632be6a2aa24ba7d9b7269b";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8359706 open file descriptor diagnostics";
    }

    @Override
    public String getDescription() {
        return "Replays the OpenJDK 21 update-line VM.info/hs_err open-file-descriptor "
                + "diagnostics plus the mandatory macOS repair from exact M3JDK21 preimages.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jdk-8359706",
                "jdk-8380236",
                "serviceability",
                "hotspot",
                "vm-info",
                "hash-pinned",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk21u-open-fds-8359706-text"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk21u-open-fds-8359706-java"));
    }
}
