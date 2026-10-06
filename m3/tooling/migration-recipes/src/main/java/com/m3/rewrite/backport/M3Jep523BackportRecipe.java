// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible backport candidate for JEP 523 / JDK-8383856.
 *
 * <p>Upstream authority is openjdk/jdk commit
 * {@code 86637704fd01493ddb57e455dbd1e35e4798237d}. The product change is one HotSpot C++
 * FILE atom. M3JDK21 adds a separate structured jtreg atom adapting its existing Java-21
 * default-GC regression to the new ergonomic policy while proving explicit SerialGC still wins.
 *
 * <p>This recipe is candidate-only. It does not authorize product promotion without HotSpot build,
 * focused jtreg and fallback/no-G1 configuration proof.</p>
 */
public final class M3Jep523BackportRecipe extends Recipe {
    public static final String JEP = "523";
    public static final String ISSUE = "JDK-8383856";
    public static final String UPSTREAM_COMMIT =
            "86637704fd01493ddb57e455dbd1e35e4798237d";

    @Override
    public String getDisplayName() {
        return "Backport JEP 523 G1 default policy";
    }

    @Override
    public String getDescription() {
        return "Replays the one-file HotSpot JEP 523 product atom and Java-21-adapted default-GC "
                + "jtreg atom from exact current-tree preimages.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-523",
                "jdk-8383856",
                "hotspot",
                "gc",
                "g1",
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
                        "jdk27-jep523-g1-default-text"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk27-jep523-g1-default-java"));
    }
}
