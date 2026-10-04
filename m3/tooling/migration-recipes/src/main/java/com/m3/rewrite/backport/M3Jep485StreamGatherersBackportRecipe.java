// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible backport of final JEP 485, Stream Gatherers.
 *
 * <p>The reviewed donor state is OpenJDK JDK 24 GA ({@code jdk-24+36}). The packet imports the
 * final, non-preview Gatherer API and implementation, the cumulative GA fixes, focused jtreg, and
 * only the stream-pipeline integration required by that API. JDK24's {@code MhUtil} dependency is
 * adapted locally because that unrelated helper does not exist in the Java 21 target.</p>
 */
public final class M3Jep485StreamGatherersBackportRecipe extends Recipe {
    public static final String DONOR_TAG = "jdk-24+36";
    public static final String IMPLEMENTATION_COMMIT =
            "33b26f79a986d015abdcd84b89842adc0a4bde64";
    public static final String GRADUATION_COMMIT =
            "ef0dc2518e7636cc8a9ca580613ff5edeb4c19fd";
    public static final String MAP_CONCURRENT_FIX =
            "450636ae28b84ded083b6861c6cba85fbf87e16e";

    @Override
    public String getDisplayName() {
        return "Backport final Stream Gatherers to M3JDK21";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed JDK24-GA Stream Gatherers API, stream-pipeline integration "
                + "and GA tests from exact current-master preimages.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-485",
                "stream-gatherers",
                "java-base",
                "hash-pinned",
                "library-api",
                "jdk24-ga",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe("jdk24-jep485-stream-gatherers"));
    }
}
